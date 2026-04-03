# 开发流程指南

> 创建日期：2026-03-25
> 最后更新：2026-04-03

## 适用范围

本文档描述本项目的标准开发流程，重点覆盖：

- 前后端协作节奏
- 后端 OpenAPI 注解规范
- 前端 generated API 调用规范
- 页面路由注册方式
- 常见开发检查清单

如果你只是第一次把项目跑起来，先看根目录 `QUICK_START.md`。

## 前后端协作开发流程

### 核心流程：后端先行，API 生成驱动前端

1. **后端开发接口** — 编写 Controller / Service / VO / DTO，确保所有字段标注 `@Schema` 注解
2. **后端编译验证** — 执行 `mvn compile`，确认接口和模型定义无误
3. **用户生成前端 API** — 等待用户运行 `npm run generate:api`，不要自行执行该命令
4. **前端对接** — 使用 `src/api/generated/` 中生成的函数和类型直接接入页面

## 后端开发规范

### 1. 数据库变更

新增表结构或字段时，在 `admin-backend/src/main/resources/db/migration/` 下新增 Flyway 脚本：

```text
V{版本号}__{描述}.sql
```

约定：

- 管理后台业务表统一使用 `admin_` 前缀
- 基础表字段通常包含 `id`、`create_time`、`update_time`、`is_deleted`
- 启动后端时 Flyway 会自动执行未运行的迁移脚本

### 2. Entity / DTO / VO

要求：

- Entity 继承 `BaseEntity`
- Entity 使用 `@TableName`
- DTO / VO / Entity 的业务字段都要补 `@Schema(description = "...")`
- DTO 使用 JSR-303 校验注解时，Controller 入参同步加 `@Valid`

特别注意：

- **内部类命名必须唯一**
- 不同 VO 中的内部类不能重名，例如不要同时出现多个 `Summary`、`GroupSection`
- 命名规范建议为 `{父类前缀}{内部类名}`，例如 `UserPermGroupSection`

### 3. Mapper / Service

要求：

- Mapper 继承 `BaseMapper<T>`
- Service 实现类优先使用 `@RequiredArgsConstructor` + `final` 注入
- 所有 CUD（增删改）类 Service 方法，按需标注 `@OperationLog`

操作审计示例：

```java
@OperationLog(module = "用户管理", type = OperationType.CREATE)
public AdminUser createUser(CreateAdminUserDTO dto) { ... }

@OperationLog(module = "角色管理", type = OperationType.DELETE, description = "批量删除")
public void deleteRoles(List<Long> ids) { ... }
```

逻辑删除注意事项：

- 删除数据时使用 `mapper.delete()`、`mapper.deleteById()`、`mapper.deleteBatchIds()`
- 不要手动 `setIsDeleted(1)` 再 `updateById()`
- 不要用 `LambdaUpdateWrapper.set(...isDeleted, 1)` 模拟删除

### 4. Controller 与 OpenAPI

Controller 必须补齐 OpenAPI 注解，否则会直接影响 orval 生成结果。

要求：

- Controller 类必须有 `@Tag(name = "英文tag名", description = "...")`
- Controller 方法必须有 `@Operation(operationId = "...", summary = "...")`
- DTO / VO 字段必须有 `@Schema(description = "...")`

`operationId` 命名规范：

- 格式：`{动词}{资源}`
- 示例：`listRoles`、`getUserDetail`、`syncRolePermissions`

还要确认：

- `application.yml` 中保留 `springdoc.default-produces-media-type: application/json`
- 否则 orval 可能把返回类型生成为 `Blob`

## 前端对接规范

### 1. generated API 使用规则

- **必须使用 generated endpoint 函数**
- 不手写新的 API 文件
- generated 代码采用工厂模式

示例：

```ts
const rolesApi = getRoles()
const res = await rolesApi.listRoles(params)

if (res.code === 200 && res.data) {
  // use res.data
}
```

注意：

- 返回值是 `R<T>` 包装：`{ code, message, data }`
- 判断成功时使用 `res.code === 200 && res.data`
- generated 类型字段默认都是 optional，使用时注意 `?? fallback` 或 `!` 断言
- 自定义 axios 实例位于 `src/api/custom-instance.ts`

### 2. API 生成规则

后端接口完成并通过编译后，由用户执行：

```bash
cd admin-frontend
npm run generate:api
```

说明：

- orval 读取 `http://localhost:8080/api-docs`
- 生成代码目录为 `src/api/generated/`
- **不要自行运行该命令，等待用户执行**

### 3. operationId 变更后的前端处理

后端修改 `operationId` 后，前端重新生成 API，函数名通常会变化。

常见变化：

- `list` → `listRoles`
- `getDetail` → `getRoleDetail`
- `_delete` → `deleteRole`

处理方式：

1. 全局搜索旧函数名
2. 替换为新生成的函数名
3. 执行 `npx tsc --noEmit` 检查类型错误

## 前端页面与路由注册

### 单一数据源

受保护页面路由统一注册在 `admin-frontend/src/routes.tsx` 的 `appRoutes` 数组中。

新增页面步骤：

1. 在 `src/pages/` 下创建页面组件
2. 在 `src/routes.tsx` 的 `appRoutes` 数组中添加一条路由
3. 完成后 React Router 和开发者模式菜单会自动生效
4. 如需普通角色可见，再到后端菜单表配置并分配权限

示例：

```tsx
export const appRoutes: RouteConfig[] = [
  { path: "/new-page", title: "新页面", icon: "Globe", element: <NewPage /> },
]
```

### 菜单显示规则

- 超级管理员 + 开发者模式开启：侧边栏显示 `appRoutes` 中全部路由
- 超级管理员关闭开发者模式，或普通用户登录：侧边栏按后端返回的菜单树渲染

## 推荐开发顺序

当你新增一个完整功能时，推荐按下面顺序推进：

1. Flyway 脚本变更数据库
2. Entity / Mapper 建模
3. DTO / VO 建模并补齐 `@Schema`
4. Service 实现业务逻辑，CUD 补 `@OperationLog`
5. Controller 暴露接口并补齐 `@Tag` / `@Operation`
6. 执行 `mvn compile`
7. 等待用户执行 `npm run generate:api`
8. 前端页面接入 generated API
9. 在 `src/routes.tsx` 注册页面
10. 执行前端类型检查或构建验证

## 检查清单

### 后端检查

- Flyway 脚本版本号递增
- DTO / VO / Entity 字段已补 `@Schema`
- Controller 已补 `@Tag` 和 `@Operation(operationId = ...)`
- CUD Service 已补 `@OperationLog`
- 删除逻辑使用 MyBatis-Plus `delete` 方法
- `mvn compile` 通过

### 前端检查

- 只使用 `src/api/generated/` 中的函数和类型
- 对 optional 字段做了空值处理
- 页面已注册到 `src/routes.tsx`
- 如函数名变更，已同步替换旧调用
- `npx tsc --noEmit` 或构建检查通过
