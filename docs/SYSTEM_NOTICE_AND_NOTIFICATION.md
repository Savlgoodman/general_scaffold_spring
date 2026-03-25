# 通知公告系统

- **创建日期**：2026-03-25
- **最后更新日期**：2026-03-25

---

## 1. 通知公告概述

系统内置的公告通知管理模块，支持管理员发布通知和公告，涵盖完整的 **草稿 -> 发布 -> 撤回** 生命周期管理。前端在多处消费展示已发布公告，包括管理后台列表页、Dashboard 公告板、Header 通知轮播、Header Bell 通知弹窗。

### 生命周期状态流转

```
创建 ──> draft(草稿)
           │
           ├── 编辑（仅草稿状态允许）
           ├── 删除
           │
           v
       published(已发布) ──> withdrawn(已撤回)
           │                       │
           ├── 置顶/取消置顶        ├── 删除
           ├── 删除                 └── (不可重新发布)
           v
       前端各消费场景展示
```

### 公告类型

| 值 | 含义 | 说明 |
|----|------|------|
| `notice` | 通知 | 常规通知，内容选填 |
| `announcement` | 公告 | 正式公告，内容必填 |

---

## 2. 数据模型

### 实体类 `AdminNotice`

继承 `BaseEntity`（提供 `id`, `createTime`, `updateTime`, `isDeleted` 四个基础字段）。

| 字段 | 类型 | 说明 |
|------|------|------|
| `title` | `String` | 公告标题 |
| `content` | `String` | 公告内容（Markdown 格式） |
| `type` | `String` | 公告类型：`notice`（通知）/ `announcement`（公告） |
| `status` | `String` | 状态：`draft`（草稿）/ `published`（已发布）/ `withdrawn`（已撤回） |
| `isTop` | `Integer` | 是否置顶：0-否 / 1-是 |
| `publishTime` | `LocalDateTime` | 发布时间（发布时自动设置） |
| `publisherId` | `Long` | 发布者用户 ID |
| `publisherName` | `String` | 发布者用户名 |

### 数据库表 `admin_notice`

由 Flyway 迁移脚本创建：
- `V1__init_schema.sql` — 创建表结构及索引（`idx_admin_notice_status`, `idx_admin_notice_deleted`）
- `V9__add_publisher_to_admin_notice.sql` — 新增 `publisher_id` 和 `publisher_name` 字段

```sql
CREATE TABLE admin_notice (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    content TEXT,
    type VARCHAR(20) DEFAULT 'notice',
    status VARCHAR(20) DEFAULT 'draft',
    is_top INTEGER DEFAULT 0,
    publish_time TIMESTAMP,
    publisher_id BIGINT,
    publisher_name VARCHAR(100),
    is_deleted INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

---

## 3. 后端接口

### Controller: `NoticeController`

路径前缀：`/api/admin/notices`

| 方法 | 路径 | operationId | 说明 |
|------|------|-------------|------|
| `GET` | `/` | `listNotices` | 分页列表（支持 `keyword`, `status`, `type` 筛选） |
| `GET` | `/{id}` | `getNoticeDetail` | 获取公告详情 |
| `POST` | `/` | `createNotice` | 创建公告（默认草稿状态） |
| `PUT` | `/{id}` | `updateNotice` | 编辑公告（仅草稿状态） |
| `PUT` | `/{id}/publish` | `publishNotice` | 发布公告 |
| `PUT` | `/{id}/withdraw` | `withdrawNotice` | 撤回公告 |
| `PUT` | `/{id}/top` | `toggleNoticeTop` | 切换置顶状态 |
| `DELETE` | `/{id}` | `deleteNotice` | 删除公告 |

### DTO

**`CreateNoticeDTO`**：
- `title`（`@NotBlank`） — 公告标题
- `content` — 公告内容（公告类型必填，通知类型选填）
- `type` — 公告类型，默认 `notice`

**`UpdateNoticeDTO`**：
- `title` — 公告标题（选填）
- `content` — 公告内容（选填）
- `type` — 公告类型（选填）

### Service 层业务规则

- **创建**：默认状态 `draft`，`isTop` 为 0；类型为 `announcement` 时内容不能为空
- **编辑**：仅 `draft` 状态允许编辑，否则抛出 `BusinessException`
- **发布**：仅 `draft` 状态可发布，发布时自动记录 `publishTime`、`publisherId`、`publisherName`（通过 `SecurityUtils.getCurrentUser()` 获取）
- **撤回**：仅 `published` 状态可撤回
- **置顶**：切换 `isTop` 在 0/1 之间
- **排序**：列表按 `isTop DESC` -> `publishTime DESC` -> `createTime DESC` 排序

### 操作审计

所有 CUD 操作均标注 `@OperationLog` 注解：

```java
@OperationLog(module = "通知公告", type = OperationType.CREATE)    // create
@OperationLog(module = "通知公告", type = OperationType.UPDATE)    // update
@OperationLog(module = "通知公告", type = OperationType.DELETE)    // delete
@OperationLog(module = "通知公告", type = OperationType.UPDATE, description = "发布公告")   // publish
@OperationLog(module = "通知公告", type = OperationType.UPDATE, description = "撤回公告")   // withdraw
@OperationLog(module = "通知公告", type = OperationType.UPDATE, description = "切换置顶")   // toggleTop
```

---

## 4. 前端管理页面（NoticeManagement.tsx）

管理后台的公告 CRUD 页面，路径 `/system/notices`。

### 功能列表

- **公告列表**：分页展示，支持关键词搜索和状态筛选
- **创建/编辑对话框**：标题输入 + Markdown 编辑器（`@uiw/react-md-editor`，通过 `lazy()` 按需加载）
- **状态操作**：发布、撤回
- **置顶切换**：切换公告置顶状态
- **删除**：确认对话框后删除

### 状态 Badge 配色

| 状态 | 标签 | Badge variant |
|------|------|---------------|
| `draft` | 草稿 | `secondary`（灰色） |
| `published` | 已发布 | `default`（主色） |
| `withdrawn` | 已撤回 | `destructive`（红色） |

### 类型标签

| 值 | 显示 |
|----|------|
| `notice` | 通知 |
| `announcement` | 公告 |

### API 调用方式

```typescript
import { getNotices } from '@/api/generated/notices/notices'

const noticesApi = getNotices()

// 分页查询
const res = await noticesApi.listNotices({ pageNum, pageSize, keyword, status })

// 创建
await noticesApi.createNotice(dto)

// 发布
await noticesApi.publishNotice(id)

// 撤回
await noticesApi.withdrawNotice(id)
```

---

## 5. 前端消费场景

### 5.1 Dashboard 公告板

> **状态**：规划中，尚未实现

- `NoticeBoard` 组件，嵌入 Dashboard 页面
- 获取最新 8 条已发布公告（`status=published`）
- 置顶公告显示 `Pin` 图标，排在列表最前
- 点击公告标题弹出详情 Dialog，内容通过 Markdown 渲染展示
- 可通过偏好设置中的 `showDashboardNotice` 控制显隐

### 5.2 Header 通知轮播

> **状态**：规划中，尚未实现

- `NoticeMarquee` 组件，嵌入顶部 Header 区域
- 横向滚动展示最新已发布公告的标题
- 滚动速度可配置：

| 速度选项 | 对应时长 |
|----------|----------|
| `slow` | 8 秒 |
| `normal` | 5 秒（默认） |
| `fast` | 3 秒 |

- 可通过偏好设置中的 `showHeaderNotice` 控制显隐

### 5.3 Header Bell 通知弹窗

> **状态**：规划中，尚未实现

- Header 右侧 Bell 图标按钮
- 角标显示未读通知数量
- 点击弹出 `Popover` 通知列表
- 已读管理：`readNoticeIds` 存储在 `localStorage`（通过 preferences store）
- 点击单条通知弹出详情 Dialog

---

## 6. 偏好设置集成

通知相关偏好在 `src/store/preferences.ts` 中管理，使用 Zustand + `persist` 中间件持久化到 `localStorage`（key: `preferences-storage`）。

### 通知相关状态

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `showHeaderNotice` | `boolean` | `true` | 控制 Header 通知轮播显隐 |
| `noticeSpeed` | `'slow' \| 'normal' \| 'fast'` | `'normal'` | 轮播滚动速度 |
| `readNoticeIds` | `number[]` | `[]` | 已读通知 ID 列表 |

### 速度映射常量

```typescript
export const NOTICE_SPEED_MAP: Record<NoticeSpeed, number> = {
  slow: 8000,    // 8 秒
  normal: 5000,  // 5 秒
  fast: 3000,    // 3 秒
}
```

### 已读管理方法

| 方法 | 说明 |
|------|------|
| `markNoticeRead(id)` | 标记单条通知为已读 |
| `markAllNoticesRead(ids)` | 批量标记已读 |
| `isNoticeRead(id)` | 判断通知是否已读 |

---

## 7. 关键文件清单

### 后端

| 文件 | 说明 |
|------|------|
| `admin-backend/src/main/java/com/scaffold/admin/model/entity/AdminNotice.java` | 实体类 |
| `admin-backend/src/main/java/com/scaffold/admin/model/dto/CreateNoticeDTO.java` | 创建 DTO |
| `admin-backend/src/main/java/com/scaffold/admin/model/dto/UpdateNoticeDTO.java` | 更新 DTO |
| `admin-backend/src/main/java/com/scaffold/admin/controller/NoticeController.java` | 控制器 |
| `admin-backend/src/main/java/com/scaffold/admin/service/NoticeService.java` | 服务接口 |
| `admin-backend/src/main/java/com/scaffold/admin/service/impl/NoticeServiceImpl.java` | 服务实现 |
| `admin-backend/src/main/java/com/scaffold/admin/mapper/AdminNoticeMapper.java` | 数据访问层 |
| `admin-backend/src/main/resources/db/migration/V1__init_schema.sql` | 建表迁移（含 admin_notice） |
| `admin-backend/src/main/resources/db/migration/V9__add_publisher_to_admin_notice.sql` | 新增发布者字段迁移 |

### 前端

| 文件 | 说明 |
|------|------|
| `admin-frontend/src/pages/system/NoticeManagement.tsx` | 公告管理页面 |
| `admin-frontend/src/api/generated/notices/notices.ts` | orval 生成的 API 函数 |
| `admin-frontend/src/store/preferences.ts` | 偏好设置 store（含通知偏好和已读管理） |

### 前端（规划中，待实现）

| 文件 | 说明 |
|------|------|
| `admin-frontend/src/components/NoticeBoard.tsx` | Dashboard 公告板组件 |
| `admin-frontend/src/components/layout/NoticeMarquee.tsx` | Header 通知轮播组件 |
| Header Bell 通知弹窗 | 嵌入 `Header.tsx` 的通知弹窗 |
