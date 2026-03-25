# 异常处理体系

> 创建日期：2026-03-25
> 最后更新：2026-03-25

---

## 1. 异常处理概述

本项目采用 **全局异常处理器统一兜底** 的策略，核心原则：

- **Controller 层禁止 try-catch**：Controller 只做「接收参数 → 调 Service → 返回结果」，异常自动上抛给全局处理器。
- **Service 层抛 `BusinessException`**：业务校验失败时主动抛出，携带 `ResultCode` 和用户友好的 message。
- **`GlobalExceptionHandler`（`@RestControllerAdvice`）统一兜底**：捕获所有异常，返回统一的 `R<T>` 格式响应，同时将未预期的异常异步写入 `admin_error_log` 表。

数据流向：

```
Controller → Service 抛出 BusinessException
                ↓
       GlobalExceptionHandler 捕获
                ↓
        ┌───────┴───────┐
        ↓               ↓
   返回 R<T> 响应    异步写入 admin_error_log（仅未预期异常）
```

---

## 2. 统一响应格式 R\<T\>

所有接口统一返回 `R<T>` 包装类，结构如下：

```json
{
  "code": 200,
  "message": "OK",
  "data": { }
}
```

### 工厂方法

| 方法 | 用途 | 示例输出 |
|------|------|----------|
| `R.ok()` | 无数据成功 | `{code:200, message:"OK", data:null}` |
| `R.ok(data)` | 带数据成功 | `{code:200, message:"OK", data:...}` |
| `R.ok(message, data)` | 自定义消息成功 | `{code:200, message:"自定义", data:...}` |
| `R.error(ResultCode)` | 枚举错误 | `{code:400, message:"参数错误", data:null}` |
| `R.error(ResultCode, message)` | 枚举码 + 自定义消息 | `{code:400, message:"用户名已存在", data:null}` |
| `R.error(code, message)` | 自定义码 + 消息 | `{code:423, message:"账户已锁定", data:null}` |

### ResultCode 枚举

| 枚举值 | code | 默认 message |
|--------|------|--------------|
| `SUCCESS` | 200 | OK |
| `PARAM_ERROR` | 400 | 参数错误 |
| `UNAUTHORIZED` | 401 | 未认证 |
| `FORBIDDEN` | 403 | 无权限 |
| `NOT_FOUND` | 404 | 资源不存在 |
| `ACCOUNT_LOCKED` | 423 | 账户已被锁定 |
| `INTERNAL_SERVER_ERROR` | 500 | 服务器内部错误 |

> **注意**：HTTP 状态码始终为 200，业务错误码体现在响应体的 `code` 字段中。

---

## 3. BusinessException

`BusinessException` 继承 `RuntimeException`，是唯一允许 message 透传给前端的异常类型。

### 类定义

```java
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    // 使用 ResultCode 枚举的默认消息
    public BusinessException(ResultCode resultCode) { ... }

    // 自定义错误码 + 消息
    public BusinessException(int code, String message) { ... }

    // 使用 ResultCode 的 code + 自定义消息
    public BusinessException(ResultCode resultCode, String message) { ... }
}
```

### 使用原则

- **message 必须是用户友好的安全文案**，由开发者主动编写，可安全透传给前端。
- 禁止将内部信息（ID、SQL、堆栈）放入 message。

### 示例

```java
// 参数校验
throw new BusinessException(ResultCode.PARAM_ERROR, "用户名已存在");

// 资源不存在
throw new BusinessException(ResultCode.NOT_FOUND, "角色不存在");

// 账户锁定
throw new BusinessException(ResultCode.ACCOUNT_LOCKED, "登录失败次数过多，账户已被锁定30分钟");
```

---

## 4. GlobalExceptionHandler

`@RestControllerAdvice` 全局异常处理器，按异常类型分别处理：

| 异常类型 | HTTP 状态 | body 中 code | 处理方式 |
|----------|-----------|-------------|----------|
| `BusinessException` | 200 | 自定义 | 透传 `code` + `message` |
| `MethodArgumentNotValidException` | 200 | 400 | 提取所有字段校验错误，逗号拼接 |
| `BindException` | 200 | 400 | 同上 |
| `MissingServletRequestParameterException` | 200 | 400 | `"缺少必填参数: {name}"` |
| `HttpMessageNotReadableException` | 200 | 400 | `"请求体格式错误"` |
| `HttpRequestMethodNotSupportedException` | 200 | 405 | `"不支持的请求方法: {method}"` |
| `NoHandlerFoundException` | 200 | 404 | `"接口不存在"` |
| `IllegalArgumentException` | 200 | 400 | `"参数错误"`（不透传 message） |
| `Exception`（兜底） | 200 | 500 | `"系统内部错误，请稍后重试"` |

### 异常日志入库

兜底的 `Exception` 处理器会将异常信息异步写入 `admin_error_log` 表，包含以下字段：

| 字段 | 说明 |
|------|------|
| `level` | 严重级别（`ERROR` / `CRITICAL`） |
| `exception_class` | 异常类全限定名 |
| `exception_message` | 异常消息（截断至 500 字符） |
| `stack_trace` | 堆栈信息（截断至 4000 字符） |
| `request_path` | 请求 URI |
| `request_method` | HTTP 方法 |
| `request_params` | 查询参数 |
| `user_id` | 当前用户 ID（通过 `SecurityUtils` 获取） |
| `ip` | 客户端 IP（通过 `IpUtils` 获取） |

严重级别判定规则：
- `OutOfMemoryError` / `StackOverflowError` 作为 cause → `CRITICAL`
- 其他 → `ERROR`

日志写入通过 `LogWriteService.writeErrorLog()` 异步执行（`@Async("logExecutor")`），写入失败仅记录控制台日志，不影响业务响应。

---

## 5. 安全原则

### Spring Boot 错误端点配置

```yaml
server:
  error:
    include-stacktrace: never
    include-message: never
    include-binding-errors: never
```

以上配置确保 Spring Boot 默认 `/error` 端点不会泄露任何内部信息。

### 异常信息隔离策略

| 信息流向 | 策略 |
|----------|------|
| → 前端响应 | 只返回 `R<T>` 中开发者编写的安全文案 |
| → 服务端日志 | `log.warn` / `log.error` 记录完整异常信息 |
| → admin_error_log 表 | 异步写入异常类名、消息、堆栈（供后台排查） |

### 核心规则

1. **异常 message 永远不直接透传给前端** —— `BusinessException` 的 message 是开发者主动编写的安全文案，属于「受控透传」；其他异常一律返回通用错误提示。
2. **内部信息仅存在于服务端** —— 堆栈、SQL 错误、内部 ID 等信息只出现在服务端日志和 `admin_error_log` 表中。
3. **禁止在异常中拼接敏感数据** —— 如 `throw new IllegalArgumentException("用户不存在: " + userId)` 是错误做法。

---

## 6. Controller 规范

Controller 层的职责是 **接收参数 → 调 Service → 返回结果**，不处理任何异常。

```java
// ✅ 正确：直接调用 Service，异常由全局处理器兜底
@PostMapping
public R<Void> create(@RequestBody @Valid CreateUserDTO dto) {
    userService.createUser(dto);
    return R.ok();
}

@GetMapping("/{id}")
public R<UserVO> getDetail(@PathVariable Long id) {
    return R.ok(userService.getUserDetail(id));
}
```

```java
// ❌ 错误：Controller 中 try-catch
@PostMapping
public R<Void> create(@RequestBody @Valid CreateUserDTO dto) {
    try {
        userService.createUser(dto);
        return R.ok();
    } catch (Exception e) {
        return R.error(ResultCode.INTERNAL_SERVER_ERROR, e.getMessage());
        // 问题1: e.getMessage() 可能包含 SQL 错误等内部信息
        // 问题2: 绕过了全局异常处理器的日志入库逻辑
    }
}
```

---

## 7. Service 规范

Service 层负责业务逻辑，校验失败时抛出 `BusinessException`。

```java
// ✅ 正确：抛 BusinessException，message 是安全文案
public AdminUser createUser(CreateAdminUserDTO dto) {
    if (userMapper.existsByUsername(dto.getUsername())) {
        throw new BusinessException(ResultCode.PARAM_ERROR, "用户名已存在");
    }
    // ...
}

public RoleVO getRoleDetail(Long id) {
    AdminRole role = roleMapper.selectById(id);
    if (role == null) {
        throw new BusinessException(ResultCode.NOT_FOUND, "角色不存在");
    }
    // ...
}
```

```java
// ❌ 错误：抛 IllegalArgumentException，message 含内部信息
public RoleVO getRoleDetail(Long id) {
    AdminRole role = roleMapper.selectById(id);
    if (role == null) {
        throw new IllegalArgumentException("用户不存在: " + id);
        // GlobalExceptionHandler 会拦截，但 id 暴露了内部标识
    }
    // ...
}
```

```java
// ❌ 错误：Service 内 catch 后返回 null，调用方不知道发生了什么
public RoleVO getRoleDetail(Long id) {
    try {
        return roleMapper.selectById(id);
    } catch (Exception e) {
        log.error("查询失败", e);
        return null;  // 吞掉异常，调用方无法感知错误
    }
}
```

---

## 8. 前端错误处理

### 8.1 Axios 拦截器（`custom-instance.ts`）

**请求拦截器**：自动注入 `Authorization: Bearer {accessToken}` 请求头。

**响应拦截器**：处理 401 状态码的 token 刷新逻辑：

1. 收到 401 响应 → 使用 `refreshToken` 调用 `/api/admin/auth/refresh` 接口
2. 刷新成功 → 更新本地 token，重试原请求
3. 刷新失败（refresh token 也过期）→ 清除登录状态，跳转 `/login?expired=1`

**并发控制**：多个请求同时 401 时，只发一次刷新请求，其余排队等待新 token 后重试。

### 8.2 页面级错误处理

页面调用 API 后根据 `res.code` 判断：

```typescript
const res = await someApi.someMethod(params)
if (res.code === 200 && res.data) {
    // 成功处理
} else {
    toast.error(res.message)  // 显示后端返回的安全文案
}
```

### 8.3 Dashboard 403 处理

Dashboard 页面中，某些面板可能因权限不足返回 403。前端对 403 响应自动隐藏对应面板，而非显示错误提示。

---

## 9. 关键文件清单

### 后端

| 文件 | 说明 |
|------|------|
| `admin-backend/src/main/java/com/scaffold/admin/common/R.java` | 统一响应包装类 |
| `admin-backend/src/main/java/com/scaffold/admin/common/ResultCode.java` | 响应状态码枚举 |
| `admin-backend/src/main/java/com/scaffold/admin/common/BusinessException.java` | 业务异常类 |
| `admin-backend/src/main/java/com/scaffold/admin/handler/GlobalExceptionHandler.java` | 全局异常处理器 |
| `admin-backend/src/main/java/com/scaffold/admin/model/entity/AdminErrorLog.java` | 异常日志实体 |
| `admin-backend/src/main/java/com/scaffold/admin/service/LogWriteService.java` | 异步日志写入服务 |
| `admin-backend/src/main/java/com/scaffold/admin/util/SecurityUtils.java` | 当前用户工具类 |
| `admin-backend/src/main/java/com/scaffold/admin/util/IpUtils.java` | IP 提取工具类 |
| `admin-backend/src/main/resources/application.yml` | `server.error.*` 安全配置 |

### 前端

| 文件 | 说明 |
|------|------|
| `admin-frontend/src/api/custom-instance.ts` | Axios 实例、token 刷新、401 处理 |
