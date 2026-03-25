# 日志与审计体系

> 创建日期：2026-03-25
> 最后更新：2026-03-25

---

## 1. 日志体系概述

系统包含 4 类日志，覆盖 API 请求追踪、用户登录审计、业务操作审计和系统异常记录。其中 API 日志、操作审计日志和异常日志通过 `LogWriteService` + `@Async("logExecutor")` 异步写入数据库，登录日志在 `AuthServiceImpl` 中同步写入。

| 日志类型 | 实现方式 | 数据库表 | 写入方式 |
|----------|----------|----------|----------|
| API 请求日志 | `ApiLogAspect` AOP 切面 | `admin_api_log` | 异步（`LogWriteService.writeApiLog`） |
| 登录日志 | `AuthServiceImpl.recordLoginLog()` | `admin_login_log` | 同步（`loginLogMapper.insert`） |
| 操作审计日志 | `@OperationLog` + `OperationLogAspect` | `admin_operation_log` | 异步（`LogWriteService.writeOperationLog`） |
| 系统异常日志 | `GlobalExceptionHandler` 兜底异常时写入 | `admin_error_log` | 异步（`LogWriteService.writeErrorLog`） |

---

## 2. 异步基础设施

### 2.1 线程池配置

`AsyncConfig` 声明了专用于日志写入的线程池 `logExecutor`：

| 参数 | 值 | 说明 |
|------|-----|------|
| `corePoolSize` | 2 | 常驻线程数 |
| `maxPoolSize` | 5 | 最大线程数 |
| `queueCapacity` | 500 | 等待队列容量 |
| `threadNamePrefix` | `log-async-` | 线程名前缀，便于日志排查 |
| `rejectedExecutionHandler` | `CallerRunsPolicy` | 队列满时由调用者线程执行，不丢弃 |

文件：`admin-backend/src/main/java/com/scaffold/admin/config/AsyncConfig.java`

### 2.2 LogWriteService

`LogWriteService` 提供三个 `@Async("logExecutor")` 方法，统一封装日志入库操作：

- `writeApiLog(AdminApiLog)` -- API 请求日志
- `writeErrorLog(AdminErrorLog)` -- 系统异常日志
- `writeOperationLog(AdminOperationLog)` -- 操作审计日志

**容错策略**：所有写入操作包裹在 try-catch 中，写入失败仅记录控制台 `log.error`，不抛出异常，不影响业务请求。

文件：`admin-backend/src/main/java/com/scaffold/admin/service/LogWriteService.java`

---

## 3. API 请求日志

### 3.1 切面实现

`ApiLogAspect` 使用 `@Around("within(@RestController *)")` 拦截所有 `@RestController` 标注的 Controller 方法。

### 3.2 记录字段

| 字段 | 说明 | 来源 |
|------|------|------|
| `userId` | 当前用户 ID | `SecurityUtils.getCurrentUser()` |
| `username` | 当前用户名 | `SecurityUtils.getCurrentUser()` |
| `method` | HTTP 方法 | `request.getMethod()` |
| `path` | 请求路径 | `request.getRequestURI()` |
| `queryParams` | 查询参数 | `request.getQueryString()` |
| `requestBody` | 请求体 JSON | 从 `@RequestBody` 参数提取 |
| `responseCode` | 响应业务码 | `R.getCode()` |
| `responseBody` | 响应体 JSON（可配置） | 仅 `storeResponseBody=true` 时记录 |
| `durationMs` | 请求耗时（毫秒） | `System.currentTimeMillis()` 差值 |
| `ip` | 客户端 IP | `IpUtils.getClientIp(request)` |
| `userAgent` | 浏览器标识 | `request.getHeader("User-Agent")` |

### 3.3 跳过规则

以下路径前缀的请求不记录日志：

- `/api/admin/auth/` -- 认证端点（登录/注册/刷新 token 等）
- `/swagger-ui` -- Swagger UI 页面
- `/api-docs`、`/v3/api-docs` -- OpenAPI 文档
- `/health` -- 健康检查

### 3.4 配置项

| 配置键 | 默认值 | 说明 |
|--------|--------|------|
| `app.log.store-response-body` | `false` | 是否存储响应体。生产环境建议关闭以节省存储空间 |

### 3.5 安全限制

请求体和响应体最大存储长度为 `65536` 字符（`MAX_BODY_LENGTH`），超出部分截断，防止异常大报文撑爆数据库字段。

文件：`admin-backend/src/main/java/com/scaffold/admin/aspect/ApiLogAspect.java`

---

## 4. 操作审计日志

### 4.1 注解定义

`@OperationLog` 注解标注在 Service 实现类的 CUD（增删改）方法上：

```java
@OperationLog(module = "用户管理", type = OperationType.CREATE)
public AdminUser createUser(CreateAdminUserDTO dto) { ... }

@OperationLog(module = "角色管理", type = OperationType.DELETE, description = "批量删除")
public void deleteRoles(List<Long> ids) { ... }
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `module` | `String` | 是 | 业务模块名（中文），如 "用户管理"、"角色管理"、"菜单管理" |
| `type` | `OperationType` | 是 | 操作类型枚举：`CREATE`（新增）、`UPDATE`（修改）、`DELETE`（删除） |
| `description` | `String` | 否 | 可选补充描述，如 "批量删除"、"同步角色菜单" |

文件：`admin-backend/src/main/java/com/scaffold/admin/annotation/OperationLog.java`

### 4.2 切面处理

`OperationLogAspect` 通过 `@Around("@annotation(operationLog)")` 拦截所有标注了 `@OperationLog` 的方法。

**记录字段**：

| 字段 | 说明 |
|------|------|
| `userId` / `username` | 当前操作用户 |
| `module` | 注解中的 `module` 值 |
| `operation` | 注解中的 `type.name()`（如 `CREATE`、`UPDATE`、`DELETE`） |
| `methodName` | 格式为 `类名.方法名`（如 `AdminUserServiceImpl.createUser`） |
| `requestParams` | 方法入参的 JSON 序列化 |
| `newData` | 方法返回值的 JSON 序列化 |
| `ip` | 客户端 IP |

**数据截断**：`requestParams` 和 `newData` 最大存储 `2000` 字符（`MAX_DATA_LENGTH`）。

### 4.3 标注原则

- 所有 CUD 类 Service 方法必须标注
- 纯查询接口（GET/列表/详情）不需要标注
- `module` 使用中文业务模块名，保持一致性

文件：`admin-backend/src/main/java/com/scaffold/admin/aspect/OperationLogAspect.java`

---

## 5. 登录日志

### 5.1 实现方式

登录日志在 `AuthServiceImpl.recordLoginLog()` 私有方法中直接调用 `loginLogMapper.insert()` 同步写入，未经过 `LogWriteService` 异步通道。写入失败时捕获异常并记录控制台 error，不影响登录流程。

### 5.2 记录字段

| 字段 | 说明 |
|------|------|
| `username` | 登录用户名 |
| `status` | 登录状态：`success` / `failed` / `locked` / `disabled` |
| `ip` | 客户端 IP |
| `userAgent` | 浏览器标识 |
| `message` | 描述信息，如 "登录成功"、"密码错误"、"验证码错误" |

### 5.3 触发场景

| 场景 | status | message |
|------|--------|---------|
| 验证码错误 | `failed` | 验证码错误 |
| 用户不存在 | `failed` | 用户不存在 |
| 账户被禁用 | `disabled` | 账户已被禁用 |
| 密码错误 | `failed` | 密码错误 |
| 登录成功 | `success` | 登录成功 |
| 注册成功 | `success` | 注册成功 |
| 系统异常 | `failed` | 系统异常 |

> 注意：`locked` 状态由 `checkAccountLocked()` 抛出 `BusinessException` 时隐式体现（此时不单独记录登录日志，因为异常在 `recordLoginLog` 调用之前抛出）。

文件：`admin-backend/src/main/java/com/scaffold/admin/service/impl/AuthServiceImpl.java`

---

## 6. 系统异常日志

### 6.1 触发条件

`GlobalExceptionHandler` 中的 `handleException(Exception e)` 兜底方法在捕获到未被其他 handler 匹配的异常时，调用 `writeErrorLog()` 将异常信息写入数据库。

**不触发异常日志的情况**：`BusinessException`、参数校验异常（`MethodArgumentNotValidException` / `BindException`）、`MissingServletRequestParameterException`、`HttpMessageNotReadableException`、`HttpRequestMethodNotSupportedException`、`NoHandlerFoundException`、`IllegalArgumentException` -- 这些异常有专门的 handler 处理，不写入异常日志表。

### 6.2 记录字段

| 字段 | 说明 |
|------|------|
| `level` | 异常级别：`ERROR`（默认）或 `CRITICAL`（`OutOfMemoryError` / `StackOverflowError` 的 cause） |
| `exceptionClass` | 异常类全限定名 |
| `exceptionMessage` | 异常消息（最长 500 字符） |
| `stackTrace` | 堆栈信息（最长 4000 字符） |
| `requestPath` | 请求路径 |
| `requestMethod` | HTTP 方法 |
| `requestParams` | 查询参数 |
| `userId` | 当前用户 ID |
| `ip` | 客户端 IP |

### 6.3 级别判定

| 级别 | 条件 |
|------|------|
| `CRITICAL` | 异常的 cause 是 `OutOfMemoryError` 或 `StackOverflowError` |
| `ERROR` | 其他所有未捕获异常（默认） |

文件：`admin-backend/src/main/java/com/scaffold/admin/handler/GlobalExceptionHandler.java`

---

## 7. 日志自动清理

`LogCleanupTask` 定时任务每天凌晨 3:00 执行（cron: `0 0 3 * * ?`），删除超过保留天数的 API 请求日志。

| 配置键 | 默认值 | 说明 |
|--------|--------|------|
| `app.log.api-log-retention-days` | `30` | API 日志保留天数 |

清理逻辑：使用 `LambdaQueryWrapper` 查询 `createTime < 当前时间 - 保留天数` 的记录，调用 `apiLogMapper.delete()` 批量删除。清理结果记录控制台 info 日志。

> 当前仅清理 `admin_api_log` 表。登录日志、操作审计日志和异常日志暂无自动清理机制。

文件：`admin-backend/src/main/java/com/scaffold/admin/task/LogCleanupTask.java`

---

## 8. 前端日志页面

4 个独立页面位于 `admin-frontend/src/pages/logs/`，统一使用 Card + Table 布局模式。

### 8.1 ApiLogPage

- **筛选条件**：关键词搜索（路径/用户名）、HTTP 方法下拉筛选
- **表格列**：方法（彩色 Badge）、路径、用户名、响应码、耗时、IP、时间
- **详情弹窗**：JSON 预览组件展示请求体/响应体
- **API 调用**：`getLogApi().listApiLogs(params)`

文件：`admin-frontend/src/pages/logs/ApiLogPage.tsx`

### 8.2 LoginLogPage

- **筛选条件**：关键词搜索、登录状态下拉筛选（成功/失败/锁定/禁用）
- **状态 Badge**：`success`=默认、`failed`=红色、`locked`=灰色、`disabled`=灰色
- **表格列**：用户名、状态、IP、User-Agent、消息、时间
- **API 调用**：`getLogLogin().listLoginLogs(params)`

文件：`admin-frontend/src/pages/logs/LoginLogPage.tsx`

### 8.3 OperationLogPage

- **筛选条件**：关键词搜索、操作类型下拉筛选（新增/修改/删除）
- **类型 Badge**：`CREATE`=绿色、`UPDATE`=黄色、`DELETE`=红色
- **表格列**：模块、类型、用户名、方法名、IP、时间
- **详情弹窗**：JSON 预览组件展示请求参数和操作后数据
- **API 调用**：`getLogOperation().listOperationLogs(params)`

文件：`admin-frontend/src/pages/logs/OperationLogPage.tsx`

### 8.4 ErrorLogPage

- **筛选条件**：关键词搜索、异常级别下拉筛选（WARNING/ERROR/CRITICAL）
- **级别 Badge**：`WARNING`=黄色、`ERROR`=红色、`CRITICAL`=深红色
- **表格列**：级别、异常类、请求路径、请求方法、用户 ID、IP、时间
- **详情弹窗**：JSON 预览组件展示异常消息和堆栈信息
- **API 调用**：`getLogError().listErrorLogs(params)`

文件：`admin-frontend/src/pages/logs/ErrorLogPage.tsx`

### 8.5 共同特点

- 分页：默认每页 20 条，支持翻页
- 搜索：输入框 + Search 按钮触发
- 刷新：RefreshCw 按钮手动刷新
- 详情：Eye 按钮打开 Dialog 弹窗查看完整数据
- 骨架屏：加载中使用 `TableSkeleton` 组件
- JSON 预览：共享 `JsonPreview` 组件（位于 `admin-frontend/src/pages/logs/components/JsonPreview.tsx`）

---

## 9. 关键文件清单

### 后端

| 文件 | 说明 |
|------|------|
| `admin-backend/src/main/java/com/scaffold/admin/config/AsyncConfig.java` | 异步线程池配置 |
| `admin-backend/src/main/java/com/scaffold/admin/service/LogWriteService.java` | 异步日志写入服务 |
| `admin-backend/src/main/java/com/scaffold/admin/aspect/ApiLogAspect.java` | API 请求日志切面 |
| `admin-backend/src/main/java/com/scaffold/admin/aspect/OperationLogAspect.java` | 操作审计日志切面 |
| `admin-backend/src/main/java/com/scaffold/admin/annotation/OperationLog.java` | 操作审计注解定义 |
| `admin-backend/src/main/java/com/scaffold/admin/model/enums/OperationType.java` | 操作类型枚举（CREATE/UPDATE/DELETE） |
| `admin-backend/src/main/java/com/scaffold/admin/handler/GlobalExceptionHandler.java` | 全局异常处理 + 异常日志写入 |
| `admin-backend/src/main/java/com/scaffold/admin/task/LogCleanupTask.java` | 日志定期清理任务 |
| `admin-backend/src/main/java/com/scaffold/admin/service/impl/AuthServiceImpl.java` | 登录日志记录 |
| `admin-backend/src/main/java/com/scaffold/admin/util/IpUtils.java` | IP 提取工具 |
| `admin-backend/src/main/java/com/scaffold/admin/util/SecurityUtils.java` | 当前用户信息工具 |

### 实体类

| 文件 | 对应表 |
|------|--------|
| `admin-backend/src/main/java/com/scaffold/admin/model/entity/AdminApiLog.java` | `admin_api_log` |
| `admin-backend/src/main/java/com/scaffold/admin/model/entity/AdminLoginLog.java` | `admin_login_log` |
| `admin-backend/src/main/java/com/scaffold/admin/model/entity/AdminOperationLog.java` | `admin_operation_log` |
| `admin-backend/src/main/java/com/scaffold/admin/model/entity/AdminErrorLog.java` | `admin_error_log` |

### Mapper

| 文件 | 说明 |
|------|------|
| `admin-backend/src/main/java/com/scaffold/admin/mapper/AdminApiLogMapper.java` | API 日志 Mapper |
| `admin-backend/src/main/java/com/scaffold/admin/mapper/AdminLoginLogMapper.java` | 登录日志 Mapper |
| `admin-backend/src/main/java/com/scaffold/admin/mapper/AdminOperationLogMapper.java` | 操作审计日志 Mapper |
| `admin-backend/src/main/java/com/scaffold/admin/mapper/AdminErrorLogMapper.java` | 异常日志 Mapper |

### Controller

| 文件 | 说明 |
|------|------|
| `admin-backend/src/main/java/com/scaffold/admin/controller/ApiLogController.java` | API 日志查询接口 |
| `admin-backend/src/main/java/com/scaffold/admin/controller/LoginLogController.java` | 登录日志查询接口 |
| `admin-backend/src/main/java/com/scaffold/admin/controller/OperationLogController.java` | 操作审计日志查询接口 |
| `admin-backend/src/main/java/com/scaffold/admin/controller/ErrorLogController.java` | 异常日志查询接口 |

### 前端

| 文件 | 说明 |
|------|------|
| `admin-frontend/src/pages/logs/ApiLogPage.tsx` | API 请求日志页面 |
| `admin-frontend/src/pages/logs/LoginLogPage.tsx` | 登录日志页面 |
| `admin-frontend/src/pages/logs/OperationLogPage.tsx` | 操作审计日志页面 |
| `admin-frontend/src/pages/logs/ErrorLogPage.tsx` | 系统异常日志页面 |
| `admin-frontend/src/pages/logs/components/JsonPreview.tsx` | JSON 预览组件 |
