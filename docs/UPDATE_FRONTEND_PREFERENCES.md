# 前端偏好设置 + 通知已读管理

> 创建日期：2026-03-24
> 影响范围：仅前端，不涉及后端接口

## 一、背景

当前系统缺少一个**前端侧的用户偏好设置**页面。主题切换分散在 Header 下拉菜单中，通知轮播无法关闭，通知没有已读/未读状态。需要集中管理这些前端行为。

### 与现有 SystemSettings 的区别

| | 系统设置 (`/system/settings`) | 前端偏好 (`/settings`) |
|---|---|---|
| 数据来源 | 后端 `admin_system_config` 表 | 前端 localStorage |
| 影响范围 | 所有用户（全局配置） | 仅当前浏览器 |
| 权限要求 | 管理员 | 所有登录用户 |
| 示例 | 站点名称、登录失败上限 | 主题、通知显示、布局偏好 |

## 二、功能设计

### 2.1 偏好设置项

| 分组 | 设置项　　　　　　　　| 类型 | 默认值 | 说明　　　　　　　　　　　　　　　　　　　　　　 |
| ------| -----------------------| ------| --------| --------------------------------------------------|
| 外观 | 主题　　　　　　　　　| 单选 | light　| light/dark/warm/system，复用现有 `useThemeStore` |
| 外观 | 侧边栏默认折叠　　　　| 开关 | false　| 页面加载时侧边栏是否折叠　　　　　　　　　　　　 |
| 通知 | 显示 Header 通知轮播　| 开关 | true　 | 关闭后 Header 不显示 NoticeMarquee　　　　　　　 |
| 通知 | 通知轮播速度　　　　　| 单选 | normal | slow(8s)/normal(5s)/fast(3s)　　　　　　　　　　 |
| 通知 | 显示 Dashboard 公告区 | 开关 | true　 | 关闭后 Dashboard 不渲染公告卡片　　　　　　　　　|

### 2.2 通知已读管理

**存储方式**：localStorage `notice-read-ids` 键，存储 `Set<number>` 序列化后的 JSON 组。

**交互逻辑**：
- Header Bell 按钮显示未读通知数量角标
- 点击 Bell 按钮弹出通知列表 Popover
- 列表中未读通知高亮显示，已读通知灰显
- 点击某条通知标记为已读（将 id 加入 localStorage）
- 提供"全部已读"按钮

**与 NoticeMarquee 的关系**：
- 轮播所有已发布通知（不区分已读未读）
- 但如果用户关闭了 Header 通知轮播，可以通过 Bell 按钮查看

### 2.3 路由与入口

- **路由**：`/settings`（加入 `ProtectedRoute` 白名单，所有登录用户可访问）
- **入口**：Header 用户下拉菜单中已有"设置"菜单项，链接到 `/settings`

## 三、技术方案

### 3.1 新建 Zustand Store

**新建** `store/preferences.ts`：

```typescript
interface PreferencesState {
  showHeaderNotice: boolean       // 显示 Header 通知轮播
  showDashboardNotice: boolean    // 显示 Dashboard 公告区
  noticeSpeed: 'slow' | 'normal' | 'fast'
  sidebarCollapsed: boolean       // 侧边栏默认折叠

  // 已读通知
  readNoticeIds: number[]

  // Actions
  setShowHeaderNotice: (v: boolean) => void
  setShowDashboardNotice: (v: boolean) => void
  setNoticeSpeed: (v: 'slow' | 'normal' | 'fast') => void
  setSidebarCollapsed: (v: boolean) => void
  markNoticeRead: (id: number) => void
  markAllNoticesRead: (ids: number[]) => void
  isNoticeRead: (id: number) => boolean
}
```

使用 `zustand/persist` 持久化到 localStorage key `preferences-storage`。

### 3.2 消费方改造

| 组件　　　　　　　　　　　　　| 改造点　　　　　　　　　　　　　　　　　　　　　　　 |
| -------------------------------| ------------------------------------------------------|
| `Header.tsx` — NoticeMarquee　| 读取 `showHeaderNotice`，为 false 时不渲染　　　　　 |
| `Header.tsx` — Bell 按钮　　　| 读取 `readNoticeIds`，显示未读角标，点击弹出通知列表 |
| `Header.tsx` — NoticeMarquee　| 读取 `noticeSpeed`，映射为轮播间隔毫秒　　　　　　　 |
| `Dashboard.tsx` — NoticeBoard | 读取 `showDashboardNotice`，为 false 时不渲染　　　　|
| `Header.tsx` — "设置"菜单项　 | 导航到 `/settings`　　　　　　　　　　　　　　　　　 |
| `ProtectedRoute.tsx`　　　　　| 白名单增加 `/settings`　　　　　　　　　　　　　　　 |

### 3.3 通知 Popover

在 Header 的 Bell 按钮上挂载 `Popover`：
- 展示最新 20 条已发布通知（所有类型）
- 未读通知左侧有蓝色圆点
- 点击标记已读
- 底部"全部已读"按钮
- 空状态提示

## 四、文件清单

### 新建

| 文件 | 说明 |
|------|------|
| `store/preferences.ts` | 前端偏好 Zustand Store |
| `pages/Settings.tsx` | 前端偏好设置页面 |

### 修改

| 文件 | 改动 |
|------|------|
| `components/layout/Header.tsx` | NoticeMarquee 条件渲染 + Bell 通知 Popover + 设置导航 |
| `pages/Dashboard.tsx` | NoticeBoard 条件渲染 |
| `components/auth/ProtectedRoute.tsx` | 白名单增加 `/settings` |
| `routes.tsx` | 注册 `/settings` 路由 |

## 五、实施顺序

1. 新建 `store/preferences.ts`
2. 新建 `pages/Settings.tsx` 设置页面
3. 改造 Header（条件渲染 + Bell Popover + 设置导航）
4. 改造 Dashboard（条件渲染）
5. 更新路由和白名单

## 六、注意事项

- 主题设置复用现有 `useThemeStore`，不重复存储
- localStorage key 命名遵循 `xxx-storage` 模式（与现有 `theme-storage`、`auth-storage` 一致）
- 已读通知 id 列表可能增长，定期清理超过 30 天的 id（可选）
- 偏好设置页和个人中心页是两个独立页面（`/settings` vs `/profile`）
