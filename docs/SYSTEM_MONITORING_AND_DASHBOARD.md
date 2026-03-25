# 监控与仪表盘系统

- **创建日期**：2026-03-25
- **最后更新**：2026-03-25

---

## 1. 监控与仪表盘概述

系统提供三大运维管理能力：

| 模块 | 功能 | 面向角色 |
|------|------|----------|
| **系统监控** | 实时查看 CPU、内存、JVM、磁盘、Redis、数据库连接池等运行指标 | 运维 / 超级管理员 |
| **仪表盘统计** | 用户概览、登录趋势、API 性能、错误趋势、公告通知 | 所有管理员（按权限自适应） |
| **在线用户管理** | 查看当前在线会话、强制踢人下线 | 超级管理员 |

三者共同构成管理后台的运维视角，帮助管理员掌握系统健康状况和用户活跃情况。

---

## 2. 系统监控

### 2.1 后端接口

**Controller**: `SystemController`

| 方法 | 路径 | operationId | 返回类型 |
|------|------|-------------|----------|
| GET | `/api/admin/system/monitor` | `getSystemInfo` | `R<SystemInfoVO>` |

**`SystemInfoVO` 数据结构**：

#### CPU（`SystemCpuInfo`）

| 字段 | 类型 | 说明 |
|------|------|------|
| `coreCount` | `int` | CPU 核心数 |
| `systemUsage` | `double` | 系统使用率（%） |
| `userUsage` | `double` | 用户使用率（%） |
| `idle` | `double` | 空闲率（%） |

#### 内存（`SystemMemoryInfo`）

| 字段 | 类型 | 说明 |
|------|------|------|
| `total` | `long` | 总内存（字节） |
| `used` | `long` | 已用内存（字节） |
| `available` | `long` | 可用内存（字节） |
| `usageRate` | `double` | 使用率（%） |

#### JVM（`SystemJvmInfo`）

| 字段 | 类型 | 说明 |
|------|------|------|
| `maxMemory` | `long` | 最大内存（字节） |
| `totalMemory` | `long` | 已分配内存（字节） |
| `usedMemory` | `long` | 已用内存（字节） |
| `freeMemory` | `long` | 空闲内存（字节） |
| `usageRate` | `double` | 使用率（%） |
| `javaVersion` | `String` | Java 版本 |
| `uptime` | `String` | 运行时长 |

#### 磁盘（`SystemDiskInfo`，数组）

| 字段 | 类型 | 说明 |
|------|------|------|
| `mount` | `String` | 盘符 / 挂载点 |
| `fsType` | `String` | 文件系统类型 |
| `total` | `long` | 总空间（字节） |
| `used` | `long` | 已用空间（字节） |
| `available` | `long` | 可用空间（字节） |
| `usageRate` | `double` | 使用率（%） |

#### Redis（`SystemRedisInfo`）

| 字段 | 类型 | 说明 |
|------|------|------|
| `version` | `String` | Redis 版本 |
| `usedMemory` | `String` | 已用内存 |
| `connectedClients` | `String` | 连接数 |
| `uptimeDays` | `String` | 运行天数 |
| `keyCount` | `String` | Key 数量 |

#### 数据库连接池（`SystemDbPoolInfo`）

| 字段 | 类型 | 说明 |
|------|------|------|
| `activeConnections` | `int` | 活跃连接数 |
| `idleConnections` | `int` | 空闲连接数 |
| `totalConnections` | `int` | 总连接数 |
| `threadsAwaitingConnection` | `int` | 等待线程数 |

#### 服务器（`SystemServerInfo`）

| 字段 | 类型 | 说明 |
|------|------|------|
| `osName` | `String` | 操作系统名称 |
| `osArch` | `String` | 系统架构 |
| `hostName` | `String` | 主机名 |
| `ip` | `String` | IP 地址 |

### 2.2 前端页面

**页面组件**：`SystemMonitor.tsx`

**布局**：6 大卡片，采用 `grid-cols-1 md:grid-cols-2 xl:grid-cols-3` 响应式网格：

| 卡片 | 图标 | 内容 |
|------|------|------|
| CPU | `Cpu` | 总使用率（大字）+ 核心数 Badge + 进度条 + 用户/系统/空闲明细 |
| 内存 | `MemoryStick` | 使用率 + 总量 Badge + 进度条 + 已用/可用 |
| JVM | `Activity` | 使用率 + Java 版本 Badge + 进度条 + 已用/已分配/最大/运行时长 |
| 磁盘 | `HardDrive` | 每个分区独立展示：挂载点 + 使用率 + 进度条 + 已用/总量 |
| Redis | `Database` | 版本 / 已用内存 / 连接数 / 运行天数 / Key 统计 |
| 服务器 & 连接池 | `Server` | 操作系统 / 架构 / 主机名 / IP + 分隔线 + 活跃/空闲/总/等待连接数 |

**进度条颜色规则**（`ProgressBar` 组件）：

| 使用率 | 颜色 |
|--------|------|
| > 90% | 红色（`bg-red-500`） |
| > 70% | 琥珀色（`bg-amber-500`） |
| <= 70% | 主题色（`bg-primary`） |

**其他特性**：
- 右上角手动刷新按钮，加载时图标旋转
- 磁盘卡片支持多分区动态渲染（`info.disks?.map()`）
- 字节单位自动换算（`formatBytes` 工具函数：B → KB → MB → GB → TB）
- 首次加载显示 `CardGroupSkeleton` 骨架屏

---

## 3. 仪表盘统计

### 3.1 后端接口

**Controller**: `StatisticsController`，路径前缀 `/api/admin/statistics`

| 接口 | 方法 | operationId | 返回类型 | 说明 |
|------|------|-------------|----------|------|
| `/overview` | GET | `getStatOverview` | `R<StatOverviewVO>` | 概览统计 |
| `/login-trend` | GET | `getStatLoginTrend` | `R<List<StatLoginTrendVO>>` | 7 天登录趋势 |
| `/api-stats` | GET | `getStatApiStats` | `R<StatApiStatsVO>` | API 请求统计 |
| `/recent-logins` | GET | `getStatRecentLogins` | `R<List<StatRecentLoginVO>>` | 最近 N 条登录记录 |
| `/error-trend` | GET | `getStatErrorTrend` | `R<List<StatErrorTrendVO>>` | 7 天错误趋势 |

#### StatOverviewVO 字段

| 字段 | 类型 | 说明 |
|------|------|------|
| `totalUsers` | `Long` | 用户总数 |
| `activeUsers` | `Long` | 活跃用户数（status=1） |
| `totalRoles` | `Long` | 角色总数 |
| `todayLoginSuccess` | `Long` | 今日登录成功数 |
| `todayLoginFailed` | `Long` | 今日登录失败数 |
| `onlineUsers` | `Integer` | 当前在线用户数 |
| `todayErrors` | `Long` | 今日错误数 |
| `publishedNotices` | `Long` | 已发布公告数 |

#### StatLoginTrendVO 字段

| 字段 | 类型 | 说明 |
|------|------|------|
| `date` | `String` | 日期（MM-dd 格式） |
| `successCount` | `Long` | 成功登录次数 |
| `failedCount` | `Long` | 失败登录次数 |

#### StatApiStatsVO 字段

| 字段 | 类型 | 说明 |
|------|------|------|
| `todayRequests` | `Long` | 今日请求总数 |
| `avgResponseTime` | `Double` | 平均响应时间（ms） |
| `errorRate` | `Double` | 错误率（%，responseCode >= 400） |
| `slowEndpoints` | `List<StatApiStatsSlowEndpoint>` | 慢接口 Top 5 |

`StatApiStatsSlowEndpoint` 子结构：

| 字段 | 类型 | 说明 |
|------|------|------|
| `method` | `String` | 请求方法（GET/POST/...） |
| `path` | `String` | 请求路径 |
| `avgDuration` | `Double` | 平均耗时（ms） |
| `count` | `Long` | 请求次数 |

#### StatRecentLoginVO 字段

| 字段 | 类型 | 说明 |
|------|------|------|
| `username` | `String` | 用户名 |
| `status` | `String` | 登录状态（success / failed） |
| `ip` | `String` | IP 地址 |
| `createTime` | `LocalDateTime` | 登录时间 |

#### StatErrorTrendVO 字段

| 字段 | 类型 | 说明 |
|------|------|------|
| `date` | `String` | 日期（MM-dd 格式） |
| `count` | `Long` | 错误数量 |

### 3.2 前端页面

**页面组件**：`Dashboard.tsx`

#### 通用 hook：`useStatData<T>`

每个面板组件内部使用 `useStatData` hook 独立请求数据：

- 调用传入的 `fetcher` 函数获取数据
- 返回 `{ data, visible, loading, refresh }`
- 请求成功（`code === 200`）时设置数据
- 请求返回 403 时自动将 `visible` 设为 `false`，面板静默隐藏
- 网络异常且 HTTP 状态码为 403 时同样隐藏

#### 6 个独立面板组件

| 组件 | 数据来源 | 展示形式 |
|------|----------|----------|
| `OverviewCards` | `getStatOverview` | 4 列统计卡片（用户总数、在线用户、今日登录、今日错误），每张卡片包含主数值 + 图标 + 副标题 |
| `NoticeBoard` | `listNotices`（公告接口） | 公告列表 + 点击弹窗查看 Markdown 详情，置顶公告带 Pin 图标 |
| `LoginTrendChart` | `getStatLoginTrend` | Recharts `AreaChart` 面积图，双色渐变（成功/失败），占 2/3 宽度 |
| `ApiStatsCard` | `getStatApiStats` | 数值列表（请求总数、平均响应、错误率）+ 慢接口 Top 5 列表，占 1/3 宽度 |
| `ErrorTrendChart` | `getStatErrorTrend` | Recharts `BarChart` 柱状图，圆角柱体，占 2/3 宽度 |
| `RecentLoginsCard` | `getStatRecentLogins` | 可滚动列表（最大高度 320px），每条含头像、用户名、IP、状态 Badge、相对时间 |

**页面布局结构**：

```
OverviewCards           — 4 列网格
NoticeBoard             — 全宽
LoginTrendChart + ApiStatsCard   — 3 列网格（2:1）
ErrorTrendChart + RecentLoginsCard — 3 列网格（2:1）
```

### 3.3 权限自适应机制

仪表盘采用"每个面板独立请求、403 静默隐藏"的设计，无需前端维护权限映射：

1. **每个面板组件独立调用各自的 API**，互不影响
2. **后端是唯一权限来源**：后端根据当前用户角色判断是否有权访问该统计接口
3. **403 自动隐藏**：`useStatData` hook 捕获 403 响应后将 `visible` 设为 `false`，面板从页面消失
4. **超级管理员**看到所有面板
5. **普通管理员**只看到有权限的面板，无权限的面板不显示（无报错提示）
6. **降级体验**：即使所有统计接口无权限，页面仍可正常渲染（只是没有面板）

---

## 4. 在线用户管理

### 4.1 后端接口

**Controller**: `OnlineUserController`，路径前缀 `/api/admin/monitor/online-users`

| 方法 | 路径 | operationId | 返回类型 | 说明 |
|------|------|-------------|----------|------|
| GET | `/api/admin/monitor/online-users` | `listOnlineUsers` | `R<List<OnlineUserVO>>` | 在线用户列表 |
| DELETE | `/api/admin/monitor/online-users/{userId}` | `forceUserOffline` | `R<Void>` | 强制用户下线 |

#### OnlineUserVO 字段

| 字段 | 类型 | 说明 |
|------|------|------|
| `userId` | `Long` | 用户 ID |
| `username` | `String` | 用户名 |
| `nickname` | `String` | 昵称 |
| `avatar` | `String` | 头像 URL |
| `loginIp` | `String` | 登录 IP |
| `userAgent` | `String` | 浏览器 User-Agent |
| `loginTime` | `LocalDateTime` | 登录时间 |
| `lastActiveTime` | `LocalDateTime` | 最后活跃时间 |

### 4.2 在线会话存储

在线用户列表通过扫描 Redis key 实现：

- **Key 模式**：`online:session:{userId}`（通过 `RedisKeys.ONLINE_SESSION` 枚举管理）
- **Value 类型**：`OnlineSessionData`（包含 `accessToken`、`userId`、`username` 等完整会话信息）
- **列表查询**：`redisTemplate.keys("online:session:*")` 扫描所有在线会话，逐个读取并转换为 `OnlineUserVO`
- **排序**：按登录时间降序

### 4.3 强制下线（踢人）流程

`OnlineUserServiceImpl.forceOffline(userId)` 执行以下步骤：

1. **校验**：不能踢自己（`SecurityUtils.getCurrentUserId()` 与目标 `userId` 比对）
2. **读取会话**：从 Redis 获取 `online:session:{userId}` 对应的 `OnlineSessionData`
3. **拉黑 Access Token**：调用 `jwtTokenProvider.addToBlacklist(session.getAccessToken())`
4. **拉黑 Refresh Token**：读取 `user:refresh_token:{userId}` 中存储的 refresh token 并加入黑名单，同时删除该 key
5. **删除在线会话**：删除 `online:session:{userId}` key
6. **记录日志**：`log.info` 记录管理员 ID 和被踢用户 ID

被踢用户的下一次请求会因 access token 在黑名单中而收到 401 响应，refresh token 同样失效，无法自动续期。

该方法标注了 `@OperationLog(module = "在线用户管理", type = OperationType.DELETE, description = "强制用户下线")`，操作会被审计日志记录。

### 4.4 前端页面

**页面组件**：`OnlineUsers.tsx`

**表格列**：

| 列 | 内容 |
|----|------|
| 头像 | Avatar 组件，支持图片 + 首字母 fallback |
| 用户名 | 用户名文本，本人显示"本人" Badge |
| 昵称 | 昵称或 `-` |
| 登录 IP | 等宽字体展示 |
| 登录时间 | `yyyy-MM-dd HH:mm:ss` 格式 |
| 最后活跃 | 同上 |
| 操作 | 查看详情（Eye 图标）+ 强制下线（LogOut 图标） |

**交互特性**：

- **自动刷新**：每 30 秒自动拉取在线用户列表（`setInterval`）
- **手动刷新**：右上角刷新按钮
- **不能踢自己**：前端禁用下线按钮（`isSelf` 判断）+ 后端二次校验
- **踢人确认对话框**：展示用户名和昵称，需确认操作
- **踢人成功后弹出禁用确认**：询问是否同时禁用该用户账户（调用 `updateUser` 设置 `status: 0`），防止用户重新登录
- **用户详情弹窗**：展示头像、用户名、ID、登录 IP、登录时间、最后活跃时间、完整 User-Agent 信息
- **骨架屏加载**：首次加载时使用 `TableSkeleton` 组件

---

## 5. 关键文件清单

### 后端

| 文件 | 说明 |
|------|------|
| `admin-backend/src/main/java/com/scaffold/admin/controller/SystemController.java` | 系统监控接口 |
| `admin-backend/src/main/java/com/scaffold/admin/controller/StatisticsController.java` | 仪表盘统计接口 |
| `admin-backend/src/main/java/com/scaffold/admin/controller/OnlineUserController.java` | 在线用户管理接口 |
| `admin-backend/src/main/java/com/scaffold/admin/service/SystemMonitorService.java` | 系统监控服务 |
| `admin-backend/src/main/java/com/scaffold/admin/service/StatisticsService.java` | 统计服务接口 |
| `admin-backend/src/main/java/com/scaffold/admin/service/OnlineUserService.java` | 在线用户服务接口 |
| `admin-backend/src/main/java/com/scaffold/admin/service/impl/OnlineUserServiceImpl.java` | 在线用户服务实现（Redis 会话扫描 + 踢人逻辑） |
| `admin-backend/src/main/java/com/scaffold/admin/model/vo/SystemInfoVO.java` | 系统监控数据 VO（含 7 个内部类） |
| `admin-backend/src/main/java/com/scaffold/admin/model/vo/StatOverviewVO.java` | 概览统计 VO |
| `admin-backend/src/main/java/com/scaffold/admin/model/vo/StatLoginTrendVO.java` | 登录趋势 VO |
| `admin-backend/src/main/java/com/scaffold/admin/model/vo/StatApiStatsVO.java` | API 统计 VO（含慢接口内部类） |
| `admin-backend/src/main/java/com/scaffold/admin/model/vo/StatRecentLoginVO.java` | 最近登录 VO |
| `admin-backend/src/main/java/com/scaffold/admin/model/vo/StatErrorTrendVO.java` | 错误趋势 VO |
| `admin-backend/src/main/java/com/scaffold/admin/model/vo/OnlineUserVO.java` | 在线用户 VO |
| `admin-backend/src/main/java/com/scaffold/admin/model/vo/OnlineSessionData.java` | Redis 会话存储数据结构 |

### 前端

| 文件 | 说明 |
|------|------|
| `admin-frontend/src/pages/system/SystemMonitor.tsx` | 系统监控页面（6 卡片分区） |
| `admin-frontend/src/pages/Dashboard.tsx` | 仪表盘页面（6 个独立面板组件） |
| `admin-frontend/src/pages/monitor/OnlineUsers.tsx` | 在线用户管理页面 |
| `admin-frontend/src/api/generated/system-monitor/` | 系统监控 generated API |
| `admin-frontend/src/api/generated/statistics/` | 统计 generated API |
| `admin-frontend/src/api/generated/online-users/` | 在线用户 generated API |
| `admin-frontend/src/components/skeletons.tsx` | 骨架屏组件（TableSkeleton / CardGroupSkeleton） |
