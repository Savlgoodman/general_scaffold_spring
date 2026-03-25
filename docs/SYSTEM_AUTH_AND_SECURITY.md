# 认证与安全体系

> 创建日期：2026-03-25
> 最后更新：2026-03-25

本文档描述系统的完整认证与安全架构，覆盖后端（Spring Security + JWT）和前端（React + Zustand）两端实现。

---

## 1. 认证体系概述

系统采用 **JWT 双 Token 机制**，实现无状态认证 + Redis 增强：

| Token 类型 | 有效期 | 用途 |
|------------|--------|------|
| Access Token | 5 分钟（300,000ms） | 接口鉴权，短生命周期降低泄露风险 |
| Refresh Token | 7 天（604,800,000ms） | 续签 Access Token，长生命周期减少重新登录 |

核心设计原则：

- **无状态**：JWT 自包含用户信息（`userId`、`username`、`tokenType`），服务端不存储 Access Token
- **Redis 增强**：Token 黑名单、Refresh Token 存储、在线会话管理、登录失败计数均依赖 Redis
- **Refresh Token Rotation**：每次刷新时旧 Refresh Token 立即拉黑，生成全新双 Token，防止 Token 被盗用后持续使用
- **HMAC-SHA256 签名**：使用 `com.auth0.jwt` 库，密钥通过 `jwt.secret` 配置

---

## 2. 登录流程

### 2.1 后端流程

**入口**：`AuthController.login()` → `AuthServiceImpl.login()`

```
请求 POST /api/admin/auth/login
Body: { username, password, captchaKey, captchaCode }
```

执行步骤：

1. **账号锁定检查** — 读取 Redis `login:fail:{username}` 计数，达到上限（默认 5 次，可通过系统配置 `login_max_retry` 调整）则拒绝登录，锁定时长默认 30 分钟（可通过 `login_lock_duration` 配置）
2. **验证码校验** — 调用 `AuthCaptchaUtil.verify()` 比对 Redis `captcha:{uuid}` 中存储的验证码，校验后立即删除（一次性使用）
3. **用户查询** — 通过 `username` 查询 `admin_user` 表（MyBatis-Plus 自动过滤 `is_deleted=0`）
4. **状态检查** — 用户不存在或 `status != 1`（被禁用）则拒绝
5. **密码比对** — `BCryptPasswordEncoder.matches()` 比对密码哈希
6. **登录失败处理** — 验证码错误、用户不存在、密码错误均会增加失败计数（`increment`），首次失败时设置 TTL
7. **生成双 Token** — `JwtTokenProvider.generateAccessToken()` + `generateRefreshToken()`
8. **存储 Refresh Token** — 写入 Redis `user:refresh_token:{userId}`，TTL 7 天
9. **创建在线会话** — 写入 Redis `online:session:{userId}`，包含用户信息、IP、UA、登录时间，TTL 6 分钟
10. **记录登录日志** — 写入 `admin_login_log` 表（成功/失败/禁用）
11. **构建响应** — 返回 `LoginVO`（双 Token + UserVO + 菜单树）

### 2.2 前端流程

**入口**：`Login.tsx`

1. 页面加载时调用 `getCaptcha` 获取图形验证码（Base64 图片 + `captchaKey`）
2. 用户提交表单 → 调用 generated `login` 接口
3. 登录成功 → 调用 `useAuthStore.getState().setLoginData(accessToken, refreshToken, user, menus)` 存储认证数据
4. 跳转至 `/`（首页）

---

## 3. Token 刷新机制

### 3.1 后端：Refresh Token Rotation

**入口**：`AuthServiceImpl.refreshToken()`

```
请求 POST /api/admin/auth/refresh
Body: { refreshToken }
```

执行步骤：

1. 验证 Refresh Token 签名 + 确认类型为 `refresh`
2. 检查黑名单 — 已拉黑则拒绝
3. 比对存储 — Redis 中 `user:refresh_token:{userId}` 必须与请求中的 Token 完全一致（防止已轮换的旧 Token 被重用）
4. 检查用户状态 — 用户被禁用则拒绝
5. **拉黑旧 Refresh Token** — `addToBlacklist()`，TTL 为 Token 剩余有效期
6. 生成新的双 Token
7. 存储新 Refresh Token 到 Redis
8. **更新在线会话** — 重置 `online:session:{userId}` 的 TTL 为 6 分钟，更新 `accessToken` 和 `lastActiveTime`（心跳效果）
9. 返回新的 `LoginVO`

### 3.2 前端：自动刷新 + 并发加锁

**实现文件**：`admin-frontend/src/api/custom-instance.ts`

响应拦截器处理 401 错误：

1. **非 401 错误** — 直接抛出
2. **刷新接口本身 401** — 直接登出（`/api/admin/auth/refresh` 返回 401 说明 Refresh Token 也失效）
3. **已重试过的请求** — 直接登出（防止无限循环）
4. **无 Refresh Token** — 直接登出
5. **正在刷新中** — 将原请求加入 `pendingRequests` 队列，等待刷新完成后用新 Token 重试
6. **发起刷新** — 设置 `isRefreshing = true`，调用 refresh 接口：
   - 成功：更新 auth store → 重试原请求 → 通知队列中所有等待请求
   - 失败：通知队列失败 → 登出 → 跳转 `/login?expired=1`
7. **finally** — 重置 `isRefreshing = false`

**并发安全**：多个请求同时收到 401 时，只有第一个发起 refresh，其余排队等待新 Token。

### 3.3 心跳机制

前端每次 Access Token 过期（5 分钟）→ 自动 refresh → 后端重置在线会话 TTL（6 分钟）。只要用户保持页面活跃（有 API 调用），会话就不会过期。用户离开页面超过约 6 分钟，在线会话自动消失。

---

## 4. 登出流程

### 4.1 后端

**入口**：`AuthServiceImpl.logout()`

```
请求 POST /api/admin/auth/logout
Header: Authorization: Bearer {accessToken}
```

执行步骤：

1. 提取 Bearer Token
2. 将 Access Token 加入黑名单
3. 从 Access Token 解析 `userId`
4. 读取并拉黑对应的 Refresh Token
5. 删除 Redis 中存储的 Refresh Token（`user:refresh_token:{userId}`）
6. 删除在线会话（`online:session:{userId}`）

### 4.2 前端

1. 调用 `logout` API
2. 调用 `useAuthStore.getState().logout()` 清空状态（`accessToken`、`refreshToken`、`user`、`menus` 置空，`isAuthenticated` 置 false）
3. Zustand persist 中间件自动同步 `localStorage`
4. 跳转登录页

---

## 5. 安全过滤器链

Spring Security 过滤器链按以下顺序执行：

```
请求 → JwtAuthenticationFilter → UsernamePasswordAuthenticationFilter → PermissionAuthorizationFilter → Controller
```

### 5.1 JwtAuthenticationFilter

**位置**：在 `UsernamePasswordAuthenticationFilter` 之前

**文件**：`admin-backend/src/main/java/com/scaffold/admin/security/JwtAuthenticationFilter.java`

处理流程：

1. 从 `Authorization` 请求头提取 Bearer Token
2. 检查 Token 是否在黑名单（Redis `token:blacklist:{token}`）→ 在黑名单则跳过
3. **一次性验证** Token 签名（`verifyToken()` 返回 `DecodedJWT`）
4. 确认 Token 类型为 `access`（拒绝 Refresh Token 用于 API 认证）
5. 从 JWT 提取 `userId` → `adminUserService.loadUserById()` 加载用户详情
6. 构建 `UsernamePasswordAuthenticationToken` → 设置到 `SecurityContextHolder`
7. 异常不中断请求链，仅记录日志后继续 `filterChain.doFilter()`

### 5.2 PermissionAuthorizationFilter

**位置**：在 `JwtAuthenticationFilter` 之后

**文件**：`admin-backend/src/main/java/com/scaffold/admin/security/PermissionAuthorizationFilter.java`

处理流程：

1. **公开路径检查** — 匹配 `SecurityConstants.PUBLIC_PATHS` 则跳过（支持 `/**` 通配符前缀匹配）
2. **认证检查** — `SecurityContext` 无认证信息 → 返回 401
3. **提取用户 ID** — 支持 `AdminUserDetails`、`AdminUser`、`Map` 三种 principal 类型
4. **RBAC 权限校验** — 调用 `rbacService.checkPermission(userId, path, method)` → 无权限返回 403

### 5.3 SecurityConfig

**文件**：`admin-backend/src/main/java/com/scaffold/admin/config/SecurityConfig.java`

关键配置：

- **CSRF 禁用** — 纯 API 服务，前后端分离无需 CSRF
- **Session 策略** — `STATELESS`，不创建 HTTP Session
- **公开路径** — `SecurityConstants.PUBLIC_PATHS` 定义的路径 `permitAll()`
- **其他请求** — `anyRequest().authenticated()`
- **CORS** — 启用（由 Spring 默认配置处理）

### 5.4 公开路径白名单

**单一数据源**：`admin-backend/src/main/java/com/scaffold/admin/common/SecurityConstants.java`

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

`SecurityConfig` 和 `PermissionAuthorizationFilter` 共用此数组，避免白名单不一致。

---

## 6. 在线用户管理

### 6.1 在线检测机制

基于 **Refresh 心跳** 实现：

- 前端 Access Token 每 5 分钟过期 → 自动调用 `/api/admin/auth/refresh`
- 后端 `refreshToken()` 中调用 `updateOnlineSession()` → 重置 Redis 会话 TTL 为 6 分钟
- 用户无活跃请求超过 6 分钟 → Redis Key 自动过期 → 用户从在线列表消失

### 6.2 在线用户列表

**后端**：`OnlineUserController.listOnlineUsers()` → `OnlineUserServiceImpl.listOnlineUsers()`

- 通过 `KEYS online:session:*` 扫描所有在线会话
- 反序列化 `OnlineSessionData` → 转换为 `OnlineUserVO`
- 按登录时间降序排列

### 6.3 强制下线（踢人）

**后端**：`OnlineUserController.forceOffline()` → `OnlineUserServiceImpl.forceOffline()`

执行步骤：

1. 校验不能踢自己
2. 读取目标用户的在线会话
3. 拉黑该用户的 Access Token
4. 拉黑该用户的 Refresh Token + 删除 Redis 存储
5. 删除在线会话

被踢用户下次发起请求时，Access Token 在黑名单中 → JwtAuthenticationFilter 不设置认证 → PermissionAuthorizationFilter 返回 401 → 前端自动 refresh → Refresh Token 也在黑名单 → 登出跳转登录页。

### 6.4 前端页面

**文件**：`admin-frontend/src/pages/monitor/OnlineUsers.tsx`

- 展示在线用户列表（用户名、昵称、IP、UA、登录时间、最后活跃时间）
- 提供踢人按钮 + 二次确认对话框

---

## 7. Redis Key 设计

所有 Key 通过 `RedisKeys` 枚举统一管理，避免硬编码字符串。

| Key 模式 | 枚举值 | 用途 | TTL |
|----------|--------|------|-----|
| `captcha:{uuid}` | `CAPTCHA` | 图形验证码存储 | 5 分钟 |
| `login:fail:{username}` | `LOGIN_FAIL` | 登录失败计数 | 锁定时长（默认 30 分钟，可配置） |
| `token:blacklist:{token}` | `TOKEN_BLACKLIST` | Token 黑名单 | Token 剩余有效期 |
| `user:refresh_token:{userId}` | `USER_REFRESH_TOKEN` | 当前有效的 Refresh Token | 7 天 |
| `online:session:{userId}` | `ONLINE_SESSION` | 在线会话数据 | 6 分钟（每次 refresh 滚动重置） |
| `system:config` | `SYSTEM_CONFIG` | 系统配置缓存 | 视具体实现 |

**Key 构建方法**：`RedisKeys.LOGIN_FAIL.key("admin")` → `"login:fail:admin"`

---

## 8. 前端认证架构

### 8.1 Auth Store

**文件**：`admin-frontend/src/store/auth.ts`

基于 Zustand + `persist` 中间件（`localStorage` key: `auth-storage`）：

| 字段 | 类型 | 说明 |
|------|------|------|
| `accessToken` | `string \| null` | 当前 Access Token |
| `refreshToken` | `string \| null` | 当前 Refresh Token |
| `user` | `UserVO \| null` | 当前用户信息 |
| `menus` | `MenuVO[]` | 用户可见菜单树 |
| `devMode` | `boolean` | 开发者模式开关（默认 true） |
| `isAuthenticated` | `boolean` | 是否已认证 |

关键方法：

- `setLoginData()` — 登录/刷新成功后一次性更新所有字段
- `logout()` — 清空所有认证状态
- `toggleDevMode()` — 切换开发者模式

### 8.2 路由守卫

**文件**：`admin-frontend/src/components/auth/ProtectedRoute.tsx`

判断逻辑：

1. `!isAuthenticated` → 重定向到 `/login`
2. 超级管理员 + 开发者模式 ON → 放行所有路由
3. 白名单路由（`/`、`/profile`）→ 放行
4. 检查 `menus` 中是否包含当前路径 → 包含则放行
5. 以上均不满足 → 显示 403 Forbidden 页面

### 8.3 Axios 拦截器

**文件**：`admin-frontend/src/api/custom-instance.ts`

- **请求拦截器**：从 auth store 读取 `accessToken`，注入 `Authorization: Bearer {token}` 请求头
- **响应拦截器**：401 自动刷新机制（详见第 3.2 节）
- **customInstance 函数**：orval 生成代码的适配层，剥离 `responseType: 'blob'`，直接返回 `response.data`

---

## 9. 关键文件清单

### 后端

| 文件路径 | 说明 |
|----------|------|
| `admin-backend/src/main/java/com/scaffold/admin/controller/AuthController.java` | 认证 Controller（登录/登出/刷新/验证码） |
| `admin-backend/src/main/java/com/scaffold/admin/service/impl/AuthServiceImpl.java` | 认证核心业务逻辑 |
| `admin-backend/src/main/java/com/scaffold/admin/security/JwtTokenProvider.java` | JWT 签发/验证/黑名单管理 |
| `admin-backend/src/main/java/com/scaffold/admin/security/JwtAuthenticationFilter.java` | JWT 认证过滤器 |
| `admin-backend/src/main/java/com/scaffold/admin/security/PermissionAuthorizationFilter.java` | RBAC 权限过滤器 |
| `admin-backend/src/main/java/com/scaffold/admin/config/SecurityConfig.java` | Spring Security 配置 |
| `admin-backend/src/main/java/com/scaffold/admin/common/SecurityConstants.java` | 公开路径白名单常量 |
| `admin-backend/src/main/java/com/scaffold/admin/common/RedisKeys.java` | Redis Key 枚举 |
| `admin-backend/src/main/java/com/scaffold/admin/controller/OnlineUserController.java` | 在线用户管理 Controller |
| `admin-backend/src/main/java/com/scaffold/admin/service/impl/OnlineUserServiceImpl.java` | 在线用户业务逻辑（列表/踢人） |
| `admin-backend/src/main/java/com/scaffold/admin/util/SecurityUtils.java` | 当前用户工具类 |
| `admin-backend/src/main/java/com/scaffold/admin/util/AuthCaptchaUtil.java` | 验证码生成/校验工具 |
| `admin-backend/src/main/java/com/scaffold/admin/model/vo/OnlineSessionData.java` | 在线会话 Redis 存储结构 |
| `admin-backend/src/main/java/com/scaffold/admin/model/vo/LoginVO.java` | 登录响应 VO |
| `admin-backend/src/main/java/com/scaffold/admin/model/dto/LoginDTO.java` | 登录请求 DTO |
| `admin-backend/src/main/java/com/scaffold/admin/model/dto/RefreshTokenDTO.java` | 刷新请求 DTO |
| `admin-backend/src/main/resources/application.yml` | JWT 密钥与过期时间配置 |

### 前端

| 文件路径 | 说明 |
|----------|------|
| `admin-frontend/src/store/auth.ts` | Zustand 认证状态管理 |
| `admin-frontend/src/api/custom-instance.ts` | Axios 实例 + Token 注入 + 401 自动刷新 |
| `admin-frontend/src/components/auth/ProtectedRoute.tsx` | 路由守卫组件 |
| `admin-frontend/src/pages/Login.tsx` | 登录页面 |
| `admin-frontend/src/pages/monitor/OnlineUsers.tsx` | 在线用户管理页面 |
| `admin-frontend/src/api/generated/auth/auth.ts` | orval 生成的认证 API 函数 |
| `admin-frontend/src/api/generated/online-users/online-users.ts` | orval 生成的在线用户 API 函数 |
