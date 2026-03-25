# 文件管理系统

> 创建日期：2026-03-25
> 最后更新：2026-03-25
> 影响范围：后端（FileService/FileController/MinIOConfig/MinioUtils）、前端（FileCenter/Profile）

## 一、架构概览

```
用户上传文件
    ↓
FileController (REST 接口)
    ↓
FileService (业务逻辑)
    ├── MinIO 存储（对象存储）
    └── admin_file 表（元数据记录）

访问控制：
    ├── avatars/* → 公开读（直链，头像长期有效）
    └── 其他目录 → 私有（presigned URL，5 分钟有效）
```

## 二、MinIO 存储策略

### 桶策略

| 目录 | 访问方式 | 说明 |
|------|----------|------|
| `avatars/` | 公开读 | 头像 URL 存在 `admin_user.avatar`，需长期有效 |
| 其他目录 | 私有 + presigned URL | 通过 `MinioUtils.getPresignedUrl()` 签发，5 分钟过期 |

### 文件命名规则

```
{目录}/{日期}/{分类前缀}-{UUID}.{扩展名}
示例：avatars/20260325/avatar-a1b2c3d4.png
示例：general/20260325/general-e5f6g7h8.pdf
```

### 文件分类

| category | 目录 | 上传限制 | 访问方式 |
|----------|------|----------|----------|
| `avatar` | `avatars/` | 图片 ≤2MB（jpg/png/gif/webp） | 公开直链 |
| `general` | `general/` | ≤50MB | presigned URL |
| `document` | `document/` | ≤50MB | presigned URL |
| `image` | `image/` | ≤50MB | presigned URL |

## 三、数据模型

### admin_file 表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGSERIAL | 主键 |
| file_name | VARCHAR(255) | 原始文件名 |
| object_name | VARCHAR(500) | MinIO 对象路径（唯一标识） |
| bucket_name | VARCHAR(100) | 桶名 |
| url | VARCHAR(1000) | 存储标识 URL（用于孤儿扫描比对） |
| size | BIGINT | 文件大小（字节） |
| content_type | VARCHAR(100) | MIME 类型 |
| category | VARCHAR(50) | 分类：avatar/general/document/image |
| uploader_id | BIGINT | 上传者 ID |
| uploader_name | VARCHAR(100) | 上传者用户名 |
| status | VARCHAR(20) | 状态：active/recycled/deleted |
| deleted_at | TIMESTAMP | 移入回收站时间 |

### 文件状态流转

```
active ──→ recycled ──→ deleted
  ↑            │
  └────────────┘
    (恢复)      (彻底删除)
```

- `active` → `recycled`：移入回收站（软删除，MinIO 文件保留）
- `recycled` → `active`：从回收站恢复
- `recycled` → `deleted`：彻底删除（MinIO 文件同步删除）

## 四、后端接口

### FileController — `/api/admin/files`

| 方法 | 路径 | operationId | 说明 |
|------|------|-------------|------|
| POST | `/upload` | `uploadFile` | 通用文件上传（≤50MB） |
| POST | `/upload/avatar` | `uploadAvatar` | 头像上传（图片 ≤2MB） |
| GET | `/` | `listFiles` | 分页查询（支持 bucket/category/status/keyword 筛选） |
| GET | `/{id}` | `getFileDetail` | 文件详情 |
| PUT | `/{id}/recycle` | `recycleFile` | 移入回收站 |
| PUT | `/{id}/restore` | `restoreFile` | 从回收站恢复 |
| DELETE | `/{id}` | `deleteFilePermanently` | 彻底删除（MinIO + DB） |
| GET | `/recycle-bin` | `listRecycleBin` | 回收站列表 |
| DELETE | `/recycle-bin` | `emptyRecycleBin` | 清空回收站（立即删除全部） |
| GET | `/buckets` | `listBuckets` | 桶列表 |

### AuthController — `/api/admin/auth`

| 方法 | 路径 | operationId | 说明 |
|------|------|-------------|------|
| POST | `/avatar` | `updateAvatar` | 上传头像并更新当前用户 avatar 字段 |

## 五、Presigned URL 机制

### MinioUtils 工具类

```java
// 默认 5 分钟有效
String url = minioUtils.getPresignedUrl(objectName);

// 自定义有效期
String url = minioUtils.getPresignedUrl(objectName, 600); // 10 分钟

// 判断是否公开目录
boolean isPublic = minioUtils.isPublicPath(objectName); // avatars/ → true
```

### URL 生成规则

| 场景 | URL 类型 | 说明 |
|------|----------|------|
| 文件列表（active + 非 avatar） | presigned URL | 每次查询动态生成，5 分钟过期 |
| 文件列表（active + avatar） | 公开直链 | DB 中存储的 `url` 直接返回 |
| 回收站文件 | null | 不生成任何 URL，无法访问 |
| 上传返回 | presigned 或直链 | avatar 返回直链，其他返回 presigned |

## 六、孤儿文件检测

扫描 `admin_file` 表中 `status=active` 但不被任何业务表引用的文件：

**扫描的引用源**：
- `admin_user.avatar` — 用户头像
- `admin_system_config.config_value` — 系统配置图片（site_logo/site_favicon/login_bg_image）

**检测结果**：孤儿文件自动标记为 `recycled`，等待回收站清理。

## 七、文件回收联动

以下场景会自动标记旧文件为 recycled：

| 场景 | 触发位置 | 逻辑 |
|------|----------|------|
| 用户更换头像 | `AdminUserServiceImpl.updateAvatar()` | 旧 avatar URL 对应的 admin_file 标记 recycled |
| 系统配置换图 | `SystemConfigServiceImpl.batchUpdate()` | logo/favicon/bg_image 旧 URL 对应文件标记 recycled |
| 手动移入回收站 | `FileController.recycle()` | 直接操作 |

## 八、前端页面

### FileCenter（文件中心）

**Tab 1 — 文件列表**：
- 表格：ID、文件名、分类、大小、上传者、上传时间
- 筛选：关键词搜索 + 分类下拉
- 操作：查看详情（预览图片+复制链接）、移入回收站
- 上传按钮

**Tab 2 — 回收站**：
- 表格：ID、文件名、分类、大小、删除时间
- 操作：恢复、彻底删除
- 全部还原、清空回收站按钮

### Profile（个人中心）

- 头像区域：点击弹出文件选择 → 裁剪弹窗（react-easy-crop）→ 上传

## 九、配置项

```yaml
minio:
  endpoint: ${minio.endpoint}
  access-key: ${minio.access-key}
  secret-key: ${minio.secret-key}
  bucket-name: ${minio.bucket-name:admin-uploads}

app:
  file:
    recycle-bin-retention-days: 7  # 定时清理回收站的保留天数
```

## 十、历史文件导入

MinIO 中已有但 `admin_file` 表中无记录的文件，通过 Python 脚本导入：

```bash
cd admin-backend
pip install -r script/requirements.txt
python script/import_minio_files.py --dry-run  # 预览
python script/import_minio_files.py             # 正式导入
```

脚本从 `application-dev.yml` 读取连接信息，按文件路径自动猜测分类。
