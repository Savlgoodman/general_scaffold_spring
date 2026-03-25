# 系统架构与代码规范总览

- **创建日期**：2026-03-25
- **最后更新日期**：2026-03-25

---

## 1. 技术栈总览

### 1.1 后端技术栈

| 技术 | 版本/说明 |
|------|-----------|
| Java | 17 |
| Spring Boot | 3.x |
| MyBatis-Plus | ORM + 分页拦截器 + 逻辑删除 |
| PostgreSQL | 主数据库 |
| Redis | Lettuce 连接池，用于 Token 黑名单/验证码/会话/配置缓存 |
| MinIO | 对象存储（文件上传、头像管理） |
| SpringDoc | OpenAPI 3.0 文档生成 |
| Flyway | 数据库版本迁移 |
| Lombok | 样板代码消除（`@Data`、`@RequiredArgsConstructor` 等） |
| BCrypt | 密码加密 |

### 1.2 前端技术栈

| 技术 | 版本/说明 |
|------|-----------|
| React | 18 + TypeScript |
| Vite | 构建工具，端口 3000，API 代理到 `localhost:8080` |
| shadcn/ui | new-york 风格组件库，统一圆角 12px |
| Tailwind CSS | 原子化 CSS 框架 |
| Zustand | 轻量状态管理，支持 localStorage 持久化 |
| Recharts | 图表库 |
| Orval | 从 OpenAPI spec 自动生成 API 代码 |
| React Router | v6 路由管理 |
| Lucide React | 图标库 |
| Axios | HTTP 客户端（带 Token 注入和 401 自动刷新） |

---

## 2. 后端架构

### 2.1 MVC 严格分层

```
Controller  →  Service(接口+impl)  →  Mapper  →  数据库
```

| 层 | 职责 | 禁止事项 |
|----|------|----------|
| `controller/` | 接收请求 → 调 Service → 返回结果 | 禁止业务逻辑、禁止 try-catch |
| `service/` | 所有业务逻辑、事务管理（接口 + impl） | 禁止直接操作 Controller 层逻辑 |
| `mapper/` | MyBatis-Plus `BaseMapper`，数据访问 | — |
| `util/` | 纯算法、无状态工具类 | 禁止直接操作数据库 |
| `common/` | 枚举、常量、全局异常等共享组件 | — |

### 2.2 依赖注入规范

**构造器注入 + `@RequiredArgsConstructor`（强制）**：

```java
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {
    private final AdminRoleMapper adminRoleMapper;       // final + 构造器注入
    private final AdminRolePermissionMapper rolePermMapper;
}
```

- 所有依赖字段必须声明为 `final`
- 禁止 `@Autowired` 字段注入
- Util 工具类不使用 DI（无状态纯函数）

### 2.3 统一响应格式 `R<T>`

所有接口统一返回 `R<T>` 包装：

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

- 成功：`R.ok(data)`
- 失败：`R.error(ResultCode.XXX)` 或 `R.error(code, message)`
- 状态码枚举 `ResultCode`：SUCCESS(200)、PARAM_ERROR(400)、UNAUTHORIZED(401)、FORBIDDEN(403)、NOT_FOUND(404)、ACCOUNT_LOCKED(423)、INTERNAL_SERVER_ERROR(500)

### 2.4 异常处理链路

```
Service 抛 BusinessException → GlobalExceptionHandler 统一捕获 → R<T> 响应
```

- **Controller 层禁止 try-catch**，异常自动上抛给全局处理器
- Service 层通过 `throw new BusinessException(ResultCode.XXX, "用户友好的消息")` 报错
- `GlobalExceptionHandler`（`@RestControllerAdvice`）已覆盖：`BusinessException`、`@Valid` 校验异常、JSON 解析失败、405、404 等
- 安全原则：异常 message **永远不直接透传**给前端，`BusinessException` 的 message 是开发者主动编写的安全文案

### 2.5 MyBatis-Plus 约定

**BaseEntity 基类**（所有实体继承）：

| 字段 | 说明 | 策略 |
|------|------|------|
| `id` | 主键 | `@TableId(type = IdType.AUTO)` |
| `createTime` | 创建时间 | `@TableField(fill = FieldFill.INSERT)` 自动填充 |
| `updateTime` | 更新时间 | `@TableField(fill = FieldFill.INSERT_UPDATE)` 自动填充 |
| `isDeleted` | 逻辑删除标记 | `@TableLogic` |

**逻辑删除规则**：

- 删除操作使用 `mapper.delete()` / `mapper.deleteById()` / `mapper.deleteBatchIds()`，MyBatis-Plus 自动处理
- **禁止** `setIsDeleted(1)` + `updateById()`（`updateById()` 会忽略 `@TableLogic` 字段的 SET）
- **禁止** `LambdaUpdateWrapper.set(::getIsDeleted, 1)` + `mapper.update()`
- 查询时 MyBatis-Plus 已自动加 `is_deleted=0` 条件，无需手动添加

**分页**：PostgreSQL 分页拦截器，由 `MyBatisPlusConfig` 配置。

**时间填充**：`MetaObjectHandler` 自动填充 `createTime` 和 `updateTime`。

### 2.6 Redis Key 设计

`RedisKeys` 枚举统一管理所有 Key 前缀，通过 `key()` 方法构建完整 Key：

```java
// 枚举定义
CAPTCHA("captcha", "验证码"),
TOKEN_BLACKLIST("token:blacklist", "Token黑名单"),
LOGIN_FAIL("login:fail", "登录失败计数"),
USER_REFRESH_TOKEN("user:refresh_token", "用户当前有效的Refresh Token"),
ONLINE_SESSION("online:session", "用户在线会话"),
SYSTEM_CONFIG("system:config", "系统配置");

// 使用示例
String key = RedisKeys.CAPTCHA.key(uuid);  // → "captcha:{uuid}"
```

### 2.7 OpenAPI 注解规范

前端 API 代码生成（Orval）依赖后端注解的准确性：

| 位置 | 注解 | 要求 |
|------|------|------|
| Controller 类 | `@Tag(name = "英文tag名", description = "...")` | name 必须英文 |
| Controller 方法 | `@Operation(operationId = "listUsers", summary = "...")` | operationId 必须唯一，命名格式 `{动词}{资源}` |
| DTO/VO 字段 | `@Schema(description = "...")` | 所有字段必须标注 |
| VO 内部类 | 命名加父类前缀 | 如 `UserPermGroupSection`，避免 Orval 合并同名类 |

### 2.8 安全白名单

**单一数据源：`SecurityConstants.PUBLIC_PATHS`**

`SecurityConfig`（Spring Security `permitAll`）和 `PermissionAuthorizationFilter`（权限跳过）共用此数组，禁止各自维护白名单。

### 2.9 获取当前用户

**统一使用 `SecurityUtils` 工具类**（`util/SecurityUtils.java`）：

```java
AdminUserDetails user = SecurityUtils.getCurrentUser();    // 当前用户详情
Long userId = SecurityUtils.getCurrentUserId();             // 当前用户 ID
boolean isSuperuser = SecurityUtils.isSuperuser();          // 是否超管
```

禁止直接操作 `SecurityContextHolder` 手动获取。

### 2.10 操作审计日志

所有 **CUD（增删改）** 类 Service 方法必须标注 `@OperationLog` 注解：

```java
@OperationLog(module = "用户管理", type = OperationType.CREATE)
public AdminUser createUser(CreateAdminUserDTO dto) { ... }

@OperationLog(module = "角色管理", type = OperationType.DELETE, description = "批量删除")
public void deleteRoles(List<Long> ids) { ... }
```

- `module`：业务模块名（中文），如 "用户管理"、"角色管理"、"菜单管理"
- `type`：`OperationType.CREATE` / `UPDATE` / `DELETE`
- `description`：可选补充说明
- 纯查询接口（GET/列表/详情）不需要标注

### 2.11 日志体系

四类日志，全部通过 `LogWriteService` + `@Async("logExecutor")` 异步写入数据库：

| 日志类型 | 实现方式 | 数据库表 |
|----------|----------|----------|
| API 请求日志 | `ApiLogAspect` AOP 切面 | `admin_api_log` |
| 登录日志 | `AuthServiceImpl.recordLoginLog()` | `admin_login_log` |
| 操作审计日志 | `@OperationLog` + `OperationLogAspect` | `admin_operation_log` |
| 系统异常日志 | `GlobalExceptionHandler` 兜底写入 | `admin_error_log` |

异步线程池配置：`AsyncConfig` → `logExecutor`（core=2, max=5, queue=500）。日志写入失败只记录控制台 error，不影响业务流程。

### 2.12 数据库迁移

Flyway 管理数据库版本：

- 脚本路径：`src/main/resources/db/migration/`
- 命名格式：`V{版本}__{描述}.sql`
- 所有表结构变更必须通过迁移脚本完成
- 数据库表名统一以 `admin_` 为前缀（如 `admin_user`、`admin_role`）

---

## 3. 前端架构

### 3.1 shadcn/ui 组件化

- 28+ shadcn 组件安装在 `src/components/ui/`
- 采用 **new-york** 风格，统一圆角 12px（`--radius: 0.75rem`）
- 不硬编码颜色值，使用语义化 CSS 变量（`bg-primary`、`text-muted-foreground`）
- 添加新组件：`npx shadcn@latest add <component>`

### 3.2 CSS 变量主题系统

**三套主题**：默认暖色 / light / dark

- 颜色使用 HSL 格式定义在 `src/index.css`
- 核心变量：`--background`、`--foreground`、`--primary`、`--secondary`、`--muted`、`--accent`、`--destructive`、`--border`
- 图表专用变量：`--chart-1` 到 `--chart-5`
- 侧边栏专用变量：`--sidebar-*`

**主题切换实现**：

| 文件 | 职责 |
|------|------|
| `src/index.css` | CSS 变量定义（`:root` / `.dark` / `.warm`） |
| `tailwind.config.js` | CSS 变量映射到 Tailwind 工具类 |
| `src/store/theme.ts` | Zustand 状态管理，持久化到 localStorage |
| `src/components/providers/ThemeProvider.tsx` | 键盘快捷键 + 系统主题监听 |
| `src/components/layout/Header.tsx` | 主题切换下拉菜单 |

```tsx
// 正确：使用语义化颜色
<div className="bg-primary text-primary-foreground" />

// 错误：硬编码颜色
<div className="bg-blue-500" />
```

### 3.3 Zustand Store 设计

| Store | 文件 | 用途 | 持久化 |
|-------|------|------|--------|
| `useAuthStore` | `store/auth.ts` | tokens、user、menus、devMode | localStorage |
| `useThemeStore` | `store/theme.ts` | 主题选择 + resolved theme | localStorage |
| `usePreferencesStore` | `store/preferences.ts` | 通知偏好、侧边栏样式、已读通知 | localStorage |
| `useSiteConfigStore` | `store/site-config.ts` | 站点品牌配置 | localStorage |

所有 Store 均使用 `zustand/middleware` 的 `persist` 中间件，数据持久化到 localStorage。

### 3.4 API 生成与调用（工厂模式）

**生成流程**：

1. 后端开发接口 + 标注 `@Schema`/`@Operation` 注解
2. 后端 `mvn compile` 验证
3. 用户运行 `npm run generate:api`（Orval 从 OpenAPI spec 生成）
4. 前端使用 `src/api/generated/` 中的函数和类型

**调用方式**：

```typescript
// 工厂函数获取 API 实例
const api = getAdminUsers();

// 调用接口
const res = await api.listUsers(params);

// 返回 R<T> 包装，字段全 optional
if (res.code === 200 && res.data) {
  // 使用 res.data，注意 ?? fallback 或 ! 断言
}
```

- **禁止手写 API 文件**，必须使用 generated endpoint 函数
- 自定义 Axios 实例在 `src/api/custom-instance.ts`，已配置 Token 注入和 401 自动退出

### 3.5 路由单一数据源

所有受保护页面的路由定义在 `src/routes.tsx` 的 `appRoutes` 数组中：

```typescript
export const appRoutes: RouteConfig[] = [
  { path: "/dashboard", title: "仪表盘", icon: "LayoutDashboard", element: <Dashboard /> },
  // ...
]
```

| 消费方 | 用途 |
|--------|------|
| `src/App.tsx` | 遍历 `appRoutes` 注册 React Router 路由 |
| `src/components/layout/AppSidebar.tsx` | 开发者模式下遍历渲染侧边栏菜单 |

**新增页面步骤**：

1. 在 `src/pages/` 下创建页面组件
2. 在 `src/routes.tsx` 的 `appRoutes` 中添加一条
3. 完成 -- 路由自动注册，开发者模式菜单自动出现
4. （可选）后端 `admin_menu` 表插入菜单记录，分配给角色后普通用户可见

### 3.6 菜单控制机制

| 条件 | 菜单来源 |
|------|----------|
| 超级管理员 + 开发者模式 ON | `appRoutes` 全部前端路由（扁平列表） |
| 超级管理员 + 开发者模式 OFF | 后端登录接口返回的菜单树 |
| 普通用户 | 后端登录接口返回的菜单树（`admin_menu` + `admin_role_menu`） |

开发者模式开关在 Header 右上角，仅超级管理员可见。

### 3.7 骨架屏规范

**所有 loading 状态必须使用骨架屏**，禁止旋转图标或纯文字"加载中"。

可用骨架组件（`src/components/skeletons.tsx`）：

| 组件 | 用途 |
|------|------|
| `TableSkeleton` | 表格页面 |
| `CardGroupSkeleton` | Card 分组页面 |
| `DialogGroupSkeleton` | 对话框内容 |

```tsx
{loading ? (
  <TableSkeleton rows={5} cols={6} />
) : data.length === 0 ? (
  <EmptyState />
) : (
  <ActualTable />
)}
```

### 3.8 页面布局模式

标准管理页面布局结构：

```tsx
<div className="space-y-6 max-w-7xl mx-auto">
  {/* 标题行 + 操作按钮 */}
  <div className="flex items-center justify-between">
    <h1>页面标题</h1>
    <Button>新增</Button>
  </div>

  {/* 主内容 */}
  <Card>
    {/* 搜索/筛选 */}
    {/* Table */}
    {/* 分页 */}
  </Card>

  {/* Dialog 对话框 */}
</div>
```

### 3.9 Token 自动刷新

`src/api/custom-instance.ts` 实现了 Access Token 自动刷新机制：

1. 请求返回 401 → 自动使用 Refresh Token 换取新 Token
2. 并发请求加锁，只刷新一次，其余排队等待
3. 刷新成功后自动重试原请求
4. Refresh Token 也失效时 → 弹出 toast 提示 → 跳转登录页

---

## 4. 前后端协作规范

### 4.1 API 开发对接流程

```
后端开发接口 + @Schema 注解
        ↓
mvn compile 验证通过
        ↓
用户运行 npm run generate:api（Orval 生成）
        ↓
前端使用 src/api/generated/ 中的函数和类型
```

**注意**：不要自行运行 `npm run generate:api`，等待用户执行。

### 4.2 operationId 变更后的前端对接

后端添加/修改 `operationId` 后，用户重新生成 API，前端函数名会变化：

- 全局搜索旧函数名并替换为新函数名
- 常见变化模式：`list` → `listRoles`、`getDetail` → `getRoleDetail`、`_delete` → `deleteRole`
- 修改后运行 `npx tsc --noEmit` 验证无类型错误

### 4.3 配置要点

- `application.yml` 必须配置 `springdoc.default-produces-media-type: application/json`，否则 Orval 会生成 Blob 返回类型
- Swagger UI：`http://localhost:8080/swagger-ui.html`
- OpenAPI JSON：`http://localhost:8080/api-docs`

---

## 5. Git 提交规范

### 5.1 Conventional Commits

| 类型 | 说明 | 示例 |
|------|------|------|
| `feat` | 新功能 | `feat: 新增角色批量删除接口` |
| `fix` | Bug 修复 | `fix: 修复登录失败日志被事务回滚的问题` |
| `refactor` | 重构（不改变行为） | `refactor: 提取状态计算为工具函数` |
| `docs` | 文档更新 | `docs: 更新架构文档` |
| `test` | 测试相关 | `test: 新增权限分配单元测试` |
| `chore` | 构建/依赖/配置 | `chore: 升级 Spring Boot 版本` |

### 5.2 提交原则

- **原子化提交**：一个 commit 只做一件事
- **中文描述**，不加句号，一行 50 字符以内
- **类型隔离**：`fix` 不改变 API、`feat` 一个 commit 对应一个功能点、`refactor` 不改变外部行为、`docs` 独立于代码变更

### 5.3 Commit Message 格式

```
<type>: <简短描述>
```

---

## 6. 关键目录结构

### 6.1 后端目录

```
admin-backend/src/main/java/com/scaffold/admin/
├── controller/        # REST 接口，路由前缀 /api/admin/*
├── service/           # 业务逻辑接口
├── service/impl/      # 业务逻辑实现
├── mapper/            # MyBatis-Plus BaseMapper 数据访问
├── model/entity/      # 数据库实体（继承 BaseEntity）
├── model/dto/         # 请求 DTO
├── model/vo/          # 响应 VO
├── model/enums/       # 枚举类
├── security/          # JWT 过滤器链、Token 提供者
├── aspect/            # AOP 切面（API 日志、操作审计）
├── handler/           # 全局异常处理器
├── config/            # 配置类（Redis、MyBatis-Plus、Security、CORS 等）
├── common/            # 基础组件（R、BaseEntity、BusinessException、RedisKeys 等）
├── util/              # 工具类（SecurityUtils、IpUtils、AuthCaptchaUtil）
├── annotation/        # 自定义注解（@OperationLog）
└── task/              # 定时任务（日志清理等）
```

### 6.2 前端目录

```
admin-frontend/src/
├── pages/             # 页面组件
│   ├── system/        # 系统管理（用户/角色/菜单/权限/通知/存储/监控/设置）
│   ├── logs/          # 日志页面（API/登录/操作/异常）
│   └── monitor/       # 监控页面（在线用户）
├── components/        # 公共组件
│   ├── ui/            # shadcn/ui 组件库
│   ├── layout/        # 布局组件（MainLayout、Header、AppSidebar）
│   ├── providers/     # Provider 组件（ThemeProvider）
│   ├── auth/          # 认证组件（ProtectedRoute）
│   └── skeletons.tsx  # 骨架屏组件
├── store/             # Zustand 状态管理（auth、theme、preferences、site-config）
├── api/
│   ├── generated/     # Orval 自动生成的 API 代码（禁止手动修改）
│   └── custom-instance.ts  # Axios 实例（Token 注入、401 刷新）
├── hooks/             # 自定义 Hooks
├── lib/               # 工具函数（utils.ts、icon-map.ts）
├── routes.tsx         # 路由单一数据源
├── App.tsx            # 路由注册
├── main.tsx           # 应用入口
└── index.css          # 全局样式 & CSS 变量主题
```

---

## 7. 环境与构建

### 7.1 后端

```bash
# 构建（跳过测试）
mvn clean package -DskipTests

# 运行（指定 profile）
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 仅编译（用于验证注解和代码正确性）
mvn compile
```

### 7.2 前端

```bash
# 安装依赖
npm install

# 开发模式（端口 3000，代理到 localhost:8080）
npm run dev

# 生成 API 代码（用户手动执行）
npm run generate:api

# TypeScript 类型检查
npx tsc --noEmit
```
