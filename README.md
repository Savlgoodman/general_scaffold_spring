# General Scaffold Spring

通用后台管理系统脚手架，基于 Spring Boot + React 前后端分离架构，开箱即用的 RBAC 权限管理系统。

作者：成都大学 - **罗钦文** Kevin Roo

感谢 Claude Opus 4.6 的大力协作！

## 技术栈

### 后端

- **Java 17** + **Spring Boot 3**
- **MyBatis-Plus** — ORM 框架，自动分页、逻辑删除
- **PostgreSQL** — 主数据库
- **Redis** — Token 黑名单、验证码、在线会话、系统配置缓存
- **Spring Security** + **JWT** — 认证与授权（Access Token + Refresh Token）
- **Flyway** — 数据库版本迁移
- **SpringDoc OpenAPI** — API 文档自动生成
- **MinIO** — 对象存储（头像、文件）

### 前端

- **React 18** + **TypeScript**
- **Vite** — 构建工具
- **shadcn/ui** (new-york) + **Tailwind CSS** — UI 组件库
- **Zustand** — 状态管理（auth/theme/preferences/site-config）
- **Axios** + **Orval** — HTTP 请求，从 OpenAPI 自动生成 API 代码
- **Recharts** — 图表（仪表盘统计）
- **@dnd-kit** — 拖拽排序（菜单管理）
- **React Router v6** — 路由

## 项目结构

```
general_scaffold_spring/
├── admin-backend/          # Spring Boot 后端
│   ├── src/main/java/com/scaffold/admin/
│   │   ├── controller/     # REST 接口（16 个 Controller）
│   │   ├── service/        # 业务逻辑（接口 + impl）
│   │   ├── mapper/         # MyBatis-Plus 数据访问
│   │   ├── model/          # entity / dto / vo / enums
│   │   ├── security/       # JWT + 过滤器链
│   │   ├── aspect/         # AOP 切面（日志）
│   │   ├── handler/        # 全局异常处理
│   │   ├── config/         # 配置类（11 个）
│   │   ├── common/         # 基础组件（R/ResultCode/BaseEntity）
│   │   ├── util/           # 工具类
│   │   ├── annotation/     # 自定义注解
│   │   └── task/           # 定时任务
│   └── src/main/resources/
│       ├── db/migration/   # Flyway 迁移脚本（11 个）
│       └── application.yml
├── admin-frontend/         # React 前端
│   ├── src/
│   │   ├── api/generated/  # Orval 自动生成的 API 代码（19 个模块）
│   │   ├── components/     # 通用组件（ui/layout/auth/skeletons）
│   │   ├── pages/          # 页面组件（16 个页面）
│   │   ├── store/          # Zustand 状态管理（4 个 Store）
│   │   ├── routes.tsx      # 路由配置（单一数据源）
│   │   └── App.tsx
│   └── package.json
├── docs/                   # 项目文档（详见 docs/README.md）
├── CLAUDE.md               # AI 开发指导
└── README.md
```

## 核心功能

### 认证与安全

- JWT 双 Token 机制（Access Token 5 分钟 + Refresh Token 7 天）
- Refresh Token Rotation — 每次刷新旧 token 立即拉黑
- 前端 401 自动刷新（并发加锁，用户无感知）
- 验证码登录、登录失败锁定（可配置次数和时长）
- Token 黑名单（登出即失效）
- 在线用户管理 + 强制踢人下线

### RBAC 权限管理

- **用户管理** — CRUD、角色分配、权限覆盖（grant/revoke）
- **角色管理** — CRUD、权限分配、菜单分配
- **权限管理** — 按组分类的 API 权限，同步脚本自动生成
- **菜单管理** — 树形菜单 CRUD、拖拽排序、角色-菜单关联
- **超级管理员** — 跳过所有权限检查，支持开发者模式

### 动态菜单

- 登录时后端返回用户可见菜单树，前端动态渲染侧边栏
- 超级管理员「开发者模式」显示所有前端路由页面
- 5 种侧边栏渲染模式（默认/紧凑/扁平/折叠分组/图标）

### 通知公告

- 公告 CRUD + 草稿/发布/撤回生命周期
- Markdown 编辑器 + 实时预览
- Dashboard 公告板 + Header 通知轮播 + Bell 通知弹窗
- 已读管理 + 偏好设置控制

### 日志体系

- **API 请求日志** — AOP 切面自动记录所有请求/响应
- **登录日志** — 成功/失败/锁定/禁用
- **操作审计日志** — `@OperationLog` 注解自动记录增删改
- **系统异常日志** — 未处理异常自动入库
- 全部异步写入，不影响接口性能
- API 日志自动清理（默认 30 天）

### 监控与仪表盘

- **系统监控** — CPU/内存/JVM/磁盘/Redis/数据库连接池实时数据
- **仪表盘** — 概览统计、登录趋势、API 统计、错误趋势、最近登录
- **权限自适应** — 各面板按用户权限自动显隐（403 静默隐藏）
- **在线用户** — 实时列表 + 强制下线

### 系统配置

- KV 配置表 + Redis 缓存
- 分组管理（站点品牌/安全策略/外观设置）
- 支持图片上传（Logo/Favicon/背景图）

### 文件存储

- MinIO 对象存储集成
- 文件上传/下载/删除
- 桶存储管理页面（文件浏览/预览/分类）
- 头像上传 + 裁剪

### 前端体验

- 骨架屏加载（所有页面和对话框，禁止旋转图标）
- 亮色/暗色/暖色主题切换（CSS 变量驱动）
- 偏好设置（主题/通知/侧边栏样式）
- 全局异常处理 + Toast 提示
- 个人中心（头像裁剪/信息编辑/密码修改）

## 快速开始

### 环境要求

- Java 17+
- Node.js 18+
- PostgreSQL 14+
- Redis 6+
- MinIO（可选，文件存储功能）

### 后端启动

```bash
cd admin-backend

# 配置数据库和 Redis 连接
cp src/main/resources/application-template.yml src/main/resources/application-dev.yml
# 编辑 application-dev.yml 填写数据库和 Redis 连接信息

# 启动（自动执行 Flyway 迁移）
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

默认管理员账号：`admin` / `admin123`

### 前端启动

```bash
cd admin-frontend
npm install

# 生成 API 代码（需要后端已启动）
npm run generate:api

# 启动开发服务器
npm run dev
```

访问 http://localhost:3000

### API 文档

后端启动后访问 http://localhost:8080/swagger-ui.html

## 开发流程

详见 [docs/GUIDE_DEVELOPMENT_WORKFLOW.md](./docs/GUIDE_DEVELOPMENT_WORKFLOW.md)

**简要流程：**

1. **Flyway 建表** — `db/migration/V{N}__xxx.sql`
2. **创建实体** — 继承 `BaseEntity`，标注 `@Schema`
3. **创建 Mapper** — 继承 `BaseMapper<T>`
4. **创建 DTO/VO** — 所有字段标注 `@Schema`
5. **创建 Service** — `@RequiredArgsConstructor`，CUD 加 `@OperationLog`
6. **创建 Controller** — `@Tag` + `@Operation(operationId=...)`，返回 `R<T>`
7. **编译验证** — `mvn compile`
8. **生成前端 API** — `npm run generate:api`
9. **前端页面开发** — 使用 `src/api/generated/` 中的函数和类型
10. **路由注册** — `src/routes.tsx` 添加一条

## 项目文档

所有文档位于 [docs/](./docs/) 目录，按类型分类：

| 类型 | 文档 |
|------|------|
| **SYSTEM** | [架构与代码规范](./docs/SYSTEM_ARCHITECTURE_AND_CONVENTIONS.md) · [认证安全](./docs/SYSTEM_AUTH_AND_SECURITY.md) · [RBAC 权限](./docs/SYSTEM_RBAC_PERMISSION.md) · [菜单管理](./docs/SYSTEM_MENU_MANAGEMENT.md) · [日志审计](./docs/SYSTEM_LOGGING_AND_AUDIT.md) · [异常处理](./docs/SYSTEM_EXCEPTION_HANDLING.md) · [通知公告](./docs/SYSTEM_NOTICE_AND_NOTIFICATION.md) · [监控仪表盘](./docs/SYSTEM_MONITORING_AND_DASHBOARD.md) |
| **GUIDE** | [基础开发流程](./docs/GUIDE_DEVELOPMENT_WORKFLOW.md) |
| **DESIGN** | [中间件重构](./docs/DESIGN_BACKEND_MIDDLEWARE_REFACTOR.md) · [日志实现](./docs/DESIGN_LOGGING_SYSTEM_IMPLEMENTATION.md) · [在线会话](./docs/DESIGN_ONLINE_USER_SESSION_MANAGEMENT.md) · [系统配置](./docs/DESIGN_SYSTEM_CONFIG.md) |

完整索引见 [docs/README.md](./docs/README.md)。

## License

MIT
