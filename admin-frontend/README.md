# Admin Frontend

后台管理系统前端脚手架，基于 React + shadcn/ui + Tailwind CSS 构建，配合 Spring Boot 后端实现完整的 RBAC 权限管理系统。

## 技术栈

| 职责 | 技术选型 | 说明 |
|------|---------|------|
| 框架 | React 18 | 前端核心框架 |
| 构建工具 | Vite | 快速开发与构建 |
| 语言 | TypeScript | 类型安全 |
| UI 组件库 | shadcn/ui | 基于 Radix UI 的可定制组件 |
| 样式 | Tailwind CSS | 原子化 CSS 框架 |
| 路由 | React Router v6 | 前端路由管理 |
| 状态管理 | Zustand | 轻量级全局状态 |
| HTTP 客户端 | Axios | API 请求（自动代理到后端 8080 端口） |
| 图标 | Lucide React | 图标库 |

## 项目结构

```
src/
├── assets/              # 静态资源
├── components/
│   ├── ui/              # shadcn/ui 组件（自动生成）
│   └── layout/          # 布局组件（侧边栏、顶栏等）
├── hooks/               # 自定义 Hooks
├── lib/
│   ├── utils.ts         # cn() 工具函数
│   └── api.ts           # Axios 实例与拦截器
├── pages/               # 页面组件
│   ├── Login.tsx        # 登录页
│   ├── Dashboard.tsx    # 仪表盘
│   ├── system/          # 系统管理模块
│   │   ├── User.tsx     # 用户管理
│   │   ├── Role.tsx     # 角色管理
│   │   ├── Menu.tsx     # 菜单管理
│   │   └── Permission.tsx # 权限管理
│   └── log/             # 日志模块
│       ├── ApiLog.tsx
│       ├── LoginLog.tsx
│       ├── OperationLog.tsx
│       └── ErrorLog.tsx
├── store/               # Zustand 状态存储
│   ├── auth.ts          # 认证状态（token、用户信息）
│   └── app.ts           # 应用状态（侧边栏、主题等）
├── types/               # TypeScript 类型定义
├── router/              # 路由配置
├── App.tsx
├── main.tsx
└── index.css            # 全局样式 & Tailwind 指令
```

## 前端开发计划

### 阶段一：基础搭建 ✅
- [x] Vite + React + TypeScript 项目初始化
- [x] Tailwind CSS 配置
- [x] shadcn/ui 初始化
- [x] 路由基础配置（React Router）

### 阶段二：认证模块
- [ ] 登录页（验证码 + 用户名密码）
- [ ] Axios 请求/响应拦截器（Token 自动刷新）
- [ ] Zustand 认证状态管理
- [ ] 路由守卫（未登录跳转、权限校验）

### 阶段三：布局框架
- [ ] 主布局（侧边栏 + 顶栏 + 内容区）
- [ ] 动态菜单渲染（根据后端菜单接口）
- [ ] 面包屑导航
- [ ] 暗色/亮色主题切换

### 阶段四：系统管理页面
- [ ] 用户管理（CRUD + 分配角色）
- [ ] 角色管理（CRUD + 分配权限/菜单）
- [ ] 菜单管理（树形结构 CRUD）
- [ ] 权限管理（列表 + 同步）

### 阶段五：日志与监控
- [ ] API 日志列表
- [ ] 登录日志列表
- [ ] 操作审计日志列表
- [ ] 异常日志列表
- [ ] 系统监控仪表盘

### 阶段六：业务功能
- [ ] 通知公告管理
- [ ] 个人中心
- [ ] 数据可视化图表

## 开发指南

### 启动开发服务器

```bash
npm install
npm run dev
```

前端运行在 `http://localhost:3000`，API 请求自动代理到 `http://localhost:8080`。

### 添加 shadcn/ui 组件

```bash
npx shadcn@latest add button
npx shadcn@latest add card
npx shadcn@latest add table
```

### 构建

```bash
npm run build
```

## 后端 API 约定

- 基础路径：`/api/admin/*`
- 认证方式：JWT Bearer Token
- 响应格式：
```json
{
  "code": 200,
  "message": "success",
  "data": { }
}
```
- 前端通过 OpenAPI Schema 自动生成接口调用代码
