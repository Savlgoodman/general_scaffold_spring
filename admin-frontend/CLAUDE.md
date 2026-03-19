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
- CSS 变量主题，支持亮色/暗色/暖色模式
- 后端 API 前缀: `/api/admin/*`，JWT Bearer Token 认证

## 项目结构
```
src/
├── components/
│   ├── ui/           # shadcn/ui 组件
│   ├── layout/       # 布局组件（侧边栏、顶栏等）
│   └── providers/    # Provider 组件
├── hooks/            # 自定义 Hooks
├── lib/utils.ts      # cn() 工具函数
├── pages/            # 页面组件
├── store/            # Zustand 状态管理
├── index.css         # 全局样式 & Tailwind 指令
├── App.tsx           # 路由配置
└── main.tsx          # 入口
```

## 主题系统

### CSS 变量说明

shadcn/ui 使用 CSS 变量实现主题，所有颜色定义在 `src/index.css` 中：

```css
:root {
  --background: 背景色
  --foreground: 文字色
  --primary: 主色调（按钮等）
  --primary-foreground: 主色调文字
  --secondary: 次要色调
  --secondary-foreground: 次要色调文字
  --muted: 弱化背景
  --muted-foreground: 弱化文字
  --accent: 强调色
  --accent-foreground: 强调色文字
  --destructive: 危险/错误色
  --destructive-foreground: 危险色文字
  --border: 边框色
  --input: 输入框边框
  --ring: 聚焦环颜色
  --radius: 圆角大小
  --sidebar-*: 侧边栏专用颜色
}
```

### 添加新主题

在 `src/index.css` 的 `.dark` 后添加新的主题类：

```css
.warm {
  --background: 36 33% 96%;      /* 米白背景 */
  --foreground: 224 71% 4%;      /* 深海军蓝文字 */
  --primary: 35 24% 42%;         /* 暖棕色主色 */
  --primary-foreground: 0 0% 100%;
  --secondary: 30 20% 90%;
  --secondary-foreground: 224 71% 4%;
  --muted: 30 15% 88%;
  --muted-foreground: 30 12% 35%;
  --accent: 30 20% 90%;
  --accent-foreground: 224 71% 4%;
  --destructive: 4 90% 58%;       /* 珊瑚红 */
  --destructive-foreground: 0 0% 100%;
  --border: 30 15% 82%;
  --input: 30 15% 82%;
  --ring: 35 24% 42%;
  /* 侧边栏变量同理 */
}
```

### 颜色格式

使用 HSL 格式：`色相 饱和度% 明度%`
- 亮色主题：明度值较高
- 暗色主题：明度值较低
- 暖色主题：色相在 30-40（暖棕色调）

### 主题切换实现

1. **Store**: `src/store/theme.ts` - 使用 Zustand 管理主题状态
2. **Provider**: `src/components/providers/ThemeProvider.tsx` - 应用主题到 DOM
3. **切换逻辑**: 在 `html` 元素上添加/移除 class

```typescript
// 主题类型
type Theme = 'light' | 'dark' | 'warm' | 'system'

// 应用主题
const applyTheme = (theme: 'light' | 'dark' | 'warm') => {
  document.documentElement.classList.remove('light', 'dark', 'warm')
  document.documentElement.classList.add(theme)
}
```

### 注意事项

- 使用语义化颜色变量（`bg-primary` 而非 `bg-blue-500`）
- 不要在组件中硬编码颜色
- 图表颜色需要在 `ChartContainer` 的 `config` 中单独配置

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