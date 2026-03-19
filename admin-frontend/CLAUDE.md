# Admin Frontend - 项目记忆

## 项目概述
后台管理系统前端脚手架，配合 Spring Boot 后端实现 RBAC 权限管理。

## 技术栈
- **框架**: React 18 + TypeScript
- **构建**: Vite（端口 3000，API 代理到 localhost:8080）
- **UI**: shadcn/ui（new-york 风格）+ Tailwind CSS
- **路由**: React Router v6
- **状态**: Zustand
- **HTTP**: Axios
- **图标**: Lucide React

## 项目约定
- 路径别名: `@/*` → `src/*`
- shadcn 组件存放: `src/components/ui/`
- 页面组件存放: `src/pages/`
- CSS 变量主题，支持亮色/暗色模式
- 后端 API 前缀: `/api/admin/*`，JWT Bearer Token 认证

## 项目结构
```
src/
├── components/ui/    # shadcn/ui 组件
├── lib/utils.ts      # cn() 工具函数
├── pages/            # 页面组件
├── index.css         # 全局样式 & Tailwind 指令
├── App.tsx           # 路由配置
└── main.tsx          # 入口
```

## 添加 shadcn 组件
```bash
npx shadcn@latest add <component>
```

## 后端 API 响应格式
```json
{ "code": 200, "message": "success", "data": { } }
```

## 开发计划（6 阶段）
1. ✅ 基础搭建（Vite + Tailwind + shadcn + 路由）
2. ⬜ 认证模块（登录、Token 管理、路由守卫）
3. ⬜ 布局框架（侧边栏 + 顶栏 + 动态菜单）
4. ⬜ 系统管理页面（用户/角色/菜单/权限 CRUD）
5. ⬜ 日志与监控
6. ⬜ 业务功能（通知公告、个人中心）



## Git 提交规范

### 自动提交原则

- 每次完成一个独立的修改后，必须立即提交代码（不等用户要求）
- 每个 commit 只做一件事（原子化提交）
- 提交前确保测试通过（如有对应测试）

### Conventional Commits 规范

按修改类型使用对应前缀：

| 类型       | 说明               | 示例                                                |
| ---------- | ------------------ | --------------------------------------------------- |
| `feat`     | 新功能             | `feat: add industries endpoint`                     |
| `fix`      | Bug 修复           | `fix: correct field mapping in allocation response` |
| `refactor` | 重构（不改变行为） | `refactor: extract status computation to helper`    |
| `docs`     | 文档更新           | `docs: update API.md for new endpoints`             |
| `test`     | 测试相关           | `test: add tests for allocation results`            |
| `chore`    | 构建/依赖/配置     | `chore: update dependencies`                        |

### 类型隔离原则

- `fix` 不改变 API 接口
- `feat` 有明确边界，一个 commit 对应一个功能点
- `refactor` 不改变外部行为
- `docs` 独立于代码变更（如果代码和文档同时改，拆分为两个 commit）

### Commit Message 格式

```
<type>: <简短描述>
```

- 描述使用中文、不加句号
- 保持简洁（一行 50 字符以内为佳）