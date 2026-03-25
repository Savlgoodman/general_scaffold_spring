# RBAC 权限管理系统

- **创建日期**：2026-03-25
- **最后更新日期**：2026-03-25

---

## 1. 权限模型概述

系统采用经典的三层 RBAC（Role-Based Access Control）模型，核心关系链路为：

```
用户(AdminUser) ──多对多──> 角色(AdminRole) ──多对多──> 权限(AdminPermission)
```

### 关联关系

| 关联 | 中间表 | 说明 |
|------|--------|------|
| 用户 - 角色 | `admin_user_role` | 一个用户可拥有多个角色，一个角色可分配给多个用户 |
| 角色 - 权限 | `admin_role_permission` | 一个角色可关联多个权限，支持 GRANT/DENY 双向效果 |
| 角色 - 菜单 | `admin_role_menu` | 控制角色可见的前端菜单项 |
| 用户 - 权限覆写 | `admin_user_permission_override` | 针对个别用户增加（GRANT）或撤销（DENY）特定权限，优先级高于角色权限 |

### 权限计算优先级

```
用户权限覆写（最高）> 角色权限（按 priority 排序，DENY 优先）> 默认拒绝
```

---

## 2. 数据模型

所有实体继承 `BaseEntity`，包含以下公共字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | `Long` | 主键，自增 |
| `createTime` | `LocalDateTime` | 创建时间（自动填充） |
| `updateTime` | `LocalDateTime` | 更新时间（自动填充） |
| `isDeleted` | `Integer` | 逻辑删除标记（0-未删除，1-已删除），由 `@TableLogic` 自动处理 |

### 2.1 AdminUser（管理员用户）

表名：`admin_user`

| 字段 | 类型 | 说明 |
|------|------|------|
| `username` | `String` | 用户名 |
| `password` | `String` | 密码（BCrypt 加密） |
| `nickname` | `String` | 昵称 |
| `email` | `String` | 邮箱 |
| `phone` | `String` | 手机号 |
| `avatar` | `String` | 头像 URL |
| `status` | `Integer` | 状态（1-正常，0-禁用） |
| `isSuperuser` | `Integer` | 是否超级管理员（1-是，0-否） |

### 2.2 AdminRole（角色）

表名：`admin_role`

| 字段 | 类型 | 说明 |
|------|------|------|
| `name` | `String` | 角色名称 |
| `code` | `String` | 角色编码 |
| `description` | `String` | 角色描述 |
| `status` | `Integer` | 状态（1-正常，0-禁用） |
| `sort` | `Integer` | 排序 |

### 2.3 AdminPermission（权限）

表名：`admin_permission`

| 字段 | 类型 | 说明 |
|------|------|------|
| `name` | `String` | 权限名称 |
| `code` | `String` | 权限标识，如 `system:user:list` |
| `type` | `String` | 权限类型（`api`-接口，`permission`-权限） |
| `method` | `String` | HTTP 方法（GET/POST/PUT/DELETE） |
| `path` | `String` | 接口路径，如 `/api/admin/users/**` |
| `description` | `String` | 权限描述 |
| `parentId` | `Long` | 父级 ID |
| `groupKey` | `String` | 分组标识，如 `admin_users` |
| `groupName` | `String` | 分组名称，如 "用户管理" |
| `isGroup` | `Integer` | 是否组权限（1-是，0-否） |
| `status` | `Integer` | 状态（1-启用，0-禁用） |
| `sort` | `Integer` | 排序 |

权限支持分组结构：`isGroup=1` 的记录作为组权限（通常对应 `/**` 通配符路径），其下包含多个子权限（`isGroup=0`，相同 `groupKey`）。组权限被 GRANT 时，其下所有子权限自动生效。

### 2.4 关联表

#### admin_user_role（用户-角色）

| 字段 | 类型 | 说明 |
|------|------|------|
| `userId` | `Long` | 用户 ID |
| `roleId` | `Long` | 角色 ID |

#### admin_role_permission（角色-权限）

| 字段 | 类型 | 说明 |
|------|------|------|
| `roleId` | `Long` | 角色 ID |
| `permissionId` | `Long` | 权限 ID |
| `effect` | `String` | 生效方式（`GRANT`-允许，`DENY`-拒绝） |
| `priority` | `Integer` | 优先级（0-100，越大越优先） |

#### admin_role_menu（角色-菜单）

| 字段 | 类型 | 说明 |
|------|------|------|
| `roleId` | `Long` | 角色 ID |
| `menuId` | `Long` | 菜单 ID |

#### admin_user_permission_override（用户权限覆写）

| 字段 | 类型 | 说明 |
|------|------|------|
| `userId` | `Long` | 用户 ID |
| `permissionId` | `Long` | 权限 ID |
| `effect` | `String` | 生效方式（`GRANT`-允许，`DENY`-拒绝） |

---

## 3. 鉴权流程

鉴权由 `PermissionAuthorizationFilter` 实现，继承 `OncePerRequestFilter`，在 Spring Security 过滤器链中位于 `JwtAuthenticationFilter` 之后执行。

### 过滤器链顺序

```
请求 → JwtAuthenticationFilter（身份认证）→ PermissionAuthorizationFilter（权限授权）→ Controller
```

### 鉴权步骤

```
1. 检查公开路径白名单（SecurityConstants.PUBLIC_PATHS）
   ├── 匹配 → 跳过鉴权，直接放行
   └── 不匹配 → 继续

2. 获取 SecurityContext 中的 Authentication
   ├── 未认证 → 返回 401 Unauthorized
   └── 已认证 → 提取 userId

3. 调用 rbacService.checkPermission(userId, path, method)
   ├── 超级管理员（isSuperuser=1）→ 直接放行
   ├── 查找匹配的权限记录（路径 + HTTP 方法）
   │   ├── 检查用户权限覆写（override）→ 覆写存在则直接以覆写 effect 为准
   │   ├── 检查角色权限 → 按 priority 降序、DENY 优先排序
   │   └── 无匹配权限 → 拒绝
   └── 无权限 → 返回 403 Forbidden
```

### 公开路径白名单

白名单统一定义在 `SecurityConstants.PUBLIC_PATHS`，`SecurityConfig` 和 `PermissionAuthorizationFilter` 共用同一份，避免重复维护：

```java
public static final String[] PUBLIC_PATHS = {
    "/health",
    "/api/admin/auth/**",
    "/swagger-ui/**",
    "/swagger-ui.html",
    "/api-docs/**",
    "/v3/api-docs/**",
    "/webjars/**",
    "/openapi.json",
    "/error",
    "/api/admin/system-config/public"
};
```

### 通配符路径匹配

`PermissionServiceImpl.matchPattern()` 实现了路径匹配逻辑：

- `/**` 双通配符：匹配前缀路径本身及所有子路径。例如 `/api/admin/users/**` 匹配 `/api/admin/users`、`/api/admin/users/123`、`/api/admin/users/123/roles` 等
- `*` 单通配符：匹配单层路径段
- 精确匹配：无通配符时要求路径完全一致

---

## 4. 权限同步

### 同步机制

权限列表来源于后端 OpenAPI 规范，通过 Python 脚本自动同步到数据库，避免手动维护。

### 同步脚本

脚本路径：`admin-backend/script/sync_permissions_from_openapi.py`

工作流程：

1. 读取 `application-{profile}.yml` 获取数据库连接配置
2. 请求后端 OpenAPI spec（`/api-docs`）
3. 解析所有 API 端点，提取 path、method、tag 等信息
4. 以 `psycopg2` 直连 PostgreSQL，对比 `admin_permission` 表现有数据
5. 新增缺失的权限记录，更新已有记录的描述信息

### 命名规范

- `groupKey` 格式统一为下划线分隔（如 `admin_users`），不使用点分隔
- `groupName` 使用中文名称（如 "用户管理"），对应 Controller 的 `@Tag` 描述

---

## 5. 前端页面

### 5.1 用户管理（UserManagement.tsx）

路径：`admin-frontend/src/pages/system/UserManagement.tsx`

功能：
- 用户列表（分页、搜索）
- 创建 / 编辑用户
- 角色分配对话框（多选 checkbox，调用 `RBACService.syncUserRoles`）
- 权限覆写对话框（`UserPermissionDialog.tsx`，可对单个用户设置 GRANT/DENY 覆写）
- 批量删除

### 5.2 角色管理（RoleManagement.tsx）

路径：`admin-frontend/src/pages/system/RoleManagement.tsx`

功能：
- 角色列表（CRUD）
- 创建 / 编辑角色
- 权限同步对话框（`RolePermissionDialog.tsx`，按 groupKey 分组展示 checkbox，支持组权限一键全选）
- 菜单同步对话框（`RoleMenuDialog.tsx`，树形 checkbox 选择可见菜单）

### 5.3 权限管理（PermissionManagement.tsx）

路径：`admin-frontend/src/pages/system/PermissionManagement.tsx`

功能：
- 按 `groupKey` 分组展示所有 API 权限
- 显示每条权限的 HTTP 方法（GET/POST/PUT/DELETE）、路径、名称
- 从 OpenAPI 同步权限（触发后端同步接口）

---

## 6. 超级管理员特权

`isSuperuser=1` 的用户拥有以下特权：

| 特权 | 实现位置 | 说明 |
|------|----------|------|
| 跳过权限检查 | `RBACServiceImpl.checkPermission()` | 超级管理员直接返回 `true`，不查询权限表 |
| 拥有全部权限 | `RBACServiceImpl.getUserPermissionIds()` | 返回所有活跃权限 ID |
| 权限总览显示全部 GRANT | `RBACServiceImpl.getUserPermissionOverview()` | 来源标记为 `SUPER_USER` |
| 开发者模式开关 | 前端 Header 右上角 | 仅超级管理员可见 |
| 开发者模式下显示全部路由 | `AppSidebar.tsx` | 开发者模式 ON 时，侧边栏渲染 `appRoutes` 中所有前端路由（扁平列表），不受后端菜单数据库控制 |

---

## 7. 关键文件清单

### 后端

| 文件 | 说明 |
|------|------|
| `admin-backend/src/.../model/entity/AdminUser.java` | 用户实体 |
| `admin-backend/src/.../model/entity/AdminRole.java` | 角色实体 |
| `admin-backend/src/.../model/entity/AdminPermission.java` | 权限实体 |
| `admin-backend/src/.../model/entity/AdminUserRole.java` | 用户-角色关联实体 |
| `admin-backend/src/.../model/entity/AdminRolePermission.java` | 角色-权限关联实体（含 effect/priority） |
| `admin-backend/src/.../model/entity/AdminRoleMenu.java` | 角色-菜单关联实体 |
| `admin-backend/src/.../model/entity/AdminUserPermissionOverride.java` | 用户权限覆写实体 |
| `admin-backend/src/.../common/BaseEntity.java` | 实体基类（id, createTime, updateTime, isDeleted） |
| `admin-backend/src/.../common/SecurityConstants.java` | 安全常量（公开路径白名单） |
| `admin-backend/src/.../config/SecurityConfig.java` | Spring Security 配置（过滤器链编排） |
| `admin-backend/src/.../security/JwtAuthenticationFilter.java` | JWT 身份认证过滤器 |
| `admin-backend/src/.../security/PermissionAuthorizationFilter.java` | 权限授权过滤器 |
| `admin-backend/src/.../service/RBACService.java` | RBAC 服务接口 |
| `admin-backend/src/.../service/impl/RBACServiceImpl.java` | RBAC 服务实现（权限检查、角色/权限同步、用户覆写） |
| `admin-backend/src/.../service/PermissionService.java` | 权限服务接口 |
| `admin-backend/src/.../service/impl/PermissionServiceImpl.java` | 权限服务实现（路径匹配、分组查询） |
| `admin-backend/src/.../controller/AdminUserController.java` | 用户管理接口 |
| `admin-backend/src/.../controller/AdminUserPermissionController.java` | 用户权限覆写接口 |
| `admin-backend/src/.../controller/RoleController.java` | 角色管理接口 |
| `admin-backend/src/.../controller/PermissionController.java` | 权限管理接口 |
| `admin-backend/script/sync_permissions_from_openapi.py` | 权限同步脚本 |

> 上表中 `src/.../` 代表 `src/main/java/com/scaffold/admin/`

### 前端

| 文件 | 说明 |
|------|------|
| `admin-frontend/src/pages/system/UserManagement.tsx` | 用户管理页面 |
| `admin-frontend/src/pages/system/RoleManagement.tsx` | 角色管理页面 |
| `admin-frontend/src/pages/system/PermissionManagement.tsx` | 权限管理页面 |
| `admin-frontend/src/pages/system/components/RolePermissionDialog.tsx` | 角色权限同步对话框 |
| `admin-frontend/src/pages/system/components/RoleMenuDialog.tsx` | 角色菜单同步对话框 |
| `admin-frontend/src/pages/system/components/UserPermissionDialog.tsx` | 用户权限覆写对话框 |
| `admin-frontend/src/routes.tsx` | 前端路由定义（开发者模式菜单数据源） |
| `admin-frontend/src/components/layout/AppSidebar.tsx` | 侧边栏（开发者模式/普通模式菜单渲染） |
