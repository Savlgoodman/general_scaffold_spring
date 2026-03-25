# 定时任务调度系统

> 创建日期：2026-03-25
> 最后更新：2026-03-25
> 影响范围：后端（DynamicTaskScheduler/TaskExecutorService/TaskLogService/LogCleanService/TaskController）、前端（TaskCenter）

## 一、架构概览

```
DynamicTaskScheduler (SchedulingConfigurer)
    │
    │  启动时从 admin_task_config 加载所有 enabled 任务
    │  每次触发前重新读 DB 获取最新 Cron（动态更新）
    ↓
TaskExecutorService (统一执行入口)
    │
    │  taskName → Service 方法映射
    │  自动记录执行日志到 admin_task_log
    ↓
业务 Service（实际执行逻辑）
    ├── LogCleanService.cleanApiLogs()
    ├── LogCleanService.cleanOperationLogs()
    ├── LogCleanService.cleanLoginLogs()
    ├── LogCleanService.cleanErrorLogs()
    ├── FileService.scanOrphanFiles()
    └── FileService.emptyRecycleBin()
```

**核心原则**：业务逻辑在 Service 层，Task/Controller 只做调用方。同一个 Service 方法可以被定时调度、手动触发、前端按钮三种方式调用。

## 二、动态调度机制

### 为什么不用 @Scheduled

`@Scheduled` 注解的 Cron 表达式在编译时固定，运行时无法修改。本系统使用 Spring 的 `SchedulingConfigurer` 接口实现动态调度。

### DynamicTaskScheduler

```java
@Component
public class DynamicTaskScheduler implements SchedulingConfigurer {
    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        for (AdminTaskConfig task : enabledTasks) {
            registrar.addTriggerTask(
                () -> taskExecutorService.execute(task.getTaskName()),
                triggerContext -> {
                    // 每次触发前从 DB 读最新 Cron
                    AdminTaskConfig latest = reload(task.getTaskName());
                    if (latest == null || latest.getEnabled() != 1) return null;
                    return new CronTrigger(latest.getCronExpression())
                        .nextExecution(triggerContext);
                }
            );
        }
    }
}
```

**动态行为**：
- 修改 Cron 表达式 → 下次触发即用新 Cron，**无需重启**
- 停用任务（enabled=0）→ trigger 返回 null，任务不再执行
- 无效 Cron → 捕获异常并记录日志，不影响其他任务

## 三、数据模型

### admin_task_config 表（任务配置）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGSERIAL | 主键 |
| task_name | VARCHAR(100) | 任务标识（英文，唯一），如 `api-log-cleanup` |
| task_label | VARCHAR(100) | 显示名（中文），如 `API日志清理` |
| task_group | VARCHAR(50) | 分组：log / file / system |
| cron_expression | VARCHAR(50) | Cron 表达式（5 字段标准格式） |
| enabled | INTEGER | 1=启用，0=停用 |
| description | VARCHAR(255) | 任务描述 |
| last_run_time | TIMESTAMP | 上次执行时间（自动更新） |
| last_run_status | VARCHAR(20) | 上次执行状态：success / failed |

### admin_task_log 表（执行日志）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGSERIAL | 主键 |
| task_name | VARCHAR(100) | 任务名 |
| task_group | VARCHAR(50) | 任务分组 |
| status | VARCHAR(20) | 执行状态：success / failed / running |
| message | TEXT | 错误信息（失败时） |
| duration_ms | BIGINT | 执行耗时（毫秒） |
| detail | TEXT | 执行结果描述（如"清理 API 日志 1234 条"） |

## 四、内置任务清单

| taskName | 显示名 | 分组 | 默认 Cron | 说明 |
|----------|--------|------|-----------|------|
| `api-log-cleanup` | API日志清理 | log | `0 0 3 * * ?` | 删除 N 天前的 API 请求日志 |
| `operation-log-cleanup` | 操作日志清理 | log | `0 30 3 * * ?` | 删除 N 天前的操作审计日志 |
| `login-log-cleanup` | 登录日志清理 | log | `0 0 4 * * ?` | 删除 N 天前的登录日志 |
| `error-log-cleanup` | 异常日志清理 | log | `0 30 4 * * ?` | 删除 N 天前的异常日志 |
| `orphan-file-scan` | 孤儿文件扫描 | file | `0 0 2 * * ?` | 扫描无引用文件移入回收站 |
| `recycle-bin-cleanup` | 回收站清空 | file | `0 0 5 * * SUN` | 彻底删除超过保留天数的回收站文件 |

## 五、TaskExecutorService — 统一执行入口

所有任务执行（无论定时还是手动）都通过此 Service，统一记录执行日志。

### 任务注册表

```java
taskRegistry = Map.of(
    "api-log-cleanup",       logCleanService::cleanApiLogs,
    "operation-log-cleanup", logCleanService::cleanOperationLogs,
    "login-log-cleanup",     logCleanService::cleanLoginLogs,
    "error-log-cleanup",     logCleanService::cleanErrorLogs,
    "orphan-file-scan",      fileService::scanOrphanFiles,
    "recycle-bin-cleanup",   fileService::emptyRecycleBin
);
```

### 执行流程

```
execute(taskName)
    ↓
taskLogService.startTask(taskName, group)  → 写入 running 日志
    ↓
taskRegistry.get(taskName).get()           → 执行业务逻辑
    ↓
成功 → taskLogService.finishTask(logId, duration, detail)
失败 → taskLogService.failTask(logId, duration, errorMsg)
    ↓
更新 admin_task_config.lastRunTime / lastRunStatus
```

## 六、后端接口

### TaskController — `/api/admin/tasks`

| 方法 | 路径 | operationId | 说明 |
|------|------|-------------|------|
| GET | `/configs` | `listTaskConfigs` | 所有任务配置列表 |
| PUT | `/configs/{id}` | `updateTaskConfig` | 修改 Cron / 启用状态（实时生效） |
| POST | `/{taskName}/run` | `runTaskManually` | 手动触发执行 |
| GET | `/logs` | `listTaskLogs` | 执行日志（分页 + 按任务名/状态筛选） |
| GET | `/logs/{id}` | `getTaskLogDetail` | 执行日志详情 |

## 七、前端联动

### 调度中心页面（TaskCenter）

**Tab 1 — 任务配置**：
- 表格：任务名、分组、Cron、启用 Switch、上次执行时间/状态
- 操作：编辑 Cron（弹窗修改，保存即生效）、启用/停用 Switch、立即执行

**Tab 2 — 执行日志**：
- 表格：任务名、状态 Badge、耗时、结果摘要、执行时间
- 筛选：按任务名、按状态

### 其他页面联动

| 页面 | 联动方式 |
|------|----------|
| 文件中心 | "清空回收站" 按钮 → `DELETE /api/admin/files/recycle-bin` |
| 文件中心 | 跳转调度中心查看清理/扫描执行记录 |
| 调度中心 | "立即执行" → `POST /api/admin/tasks/{taskName}/run` |

## 八、配置项

```yaml
app:
  log:
    api-log-retention-days: 30          # API 日志保留天数
    operation-log-retention-days: 90    # 操作日志保留天数
    login-log-retention-days: 90        # 登录日志保留天数
    error-log-retention-days: 60        # 异常日志保留天数
  file:
    recycle-bin-retention-days: 7       # 回收站文件保留天数
```

## 九、扩展新任务

添加新的定时任务只需 3 步：

1. **Service 层写业务逻辑**（返回 `String` 描述执行结果）
2. **DynamicTaskScheduler 注册表中添加映射**
3. **Flyway 脚本或手动往 `admin_task_config` 插入一行配置**

无需重启，新任务会被 `DynamicTaskScheduler` 自动加载。
