# 基础开发流程指南

> 创建日期：2026-03-25
> 最后更新：2026-03-25

## 概述

本文档描述从新增数据库表到前后端完整对接的标准开发流程。适用于在本脚手架上新增业务功能模块的场景。

---

## 完整流程总览

```
1. Flyway 建表
    ↓
2. 创建实体 Entity
    ↓
3. 创建 Mapper
    ↓
4. 创建 DTO / VO
    ↓
5. 创建 Service 接口 + 实现
    ↓
6. 创建 Controller（含 OpenAPI 注解）
    ↓
7. mvn compile 验证
    ↓
8. npm run generate:api（前端生成 API）
    ↓
9. 前端页面开发 + 对接 API
    ↓
10. 路由注册 + 提交代码
```

---

## Step 1: Flyway 数据库迁移

在 `admin-backend/src/main/resources/db/migration/` 下新建 SQL 文件。

### 命名规则

```
V{版本号}__{描述}.sql
```

- 版本号递增（查看已有文件确定下一个版本号）
- 双下划线 `__` 分隔版本号和描述
- 描述用英文下划线连接

### 示例

```sql
-- V12__create_admin_article_table.sql

CREATE TABLE admin_article (
    id          BIGSERIAL PRIMARY KEY,
    title       VARCHAR(200)  NOT NULL,
    content     TEXT,
    status      INTEGER       NOT NULL DEFAULT 0,
    author_id   BIGINT,
    create_time TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted  INTEGER       NOT NULL DEFAULT 0
);

COMMENT ON TABLE admin_article IS '文章表';
COMMENT ON COLUMN admin_article.title IS '标题';
COMMENT ON COLUMN admin_article.status IS '状态（0-草稿 1-已发布）';
```

### 注意事项

- 所有管理后台表以 `admin_` 为前缀
- 必须包含 `id`、`create_time`、`update_time`、`is_deleted` 四个基础字段
- `is_deleted` 用于逻辑删除（0=未删除，1=已删除）
- 启动后端时 Flyway 自动执行未运行的迁移脚本

---

## Step 2: 创建实体 Entity

在 `model/entity/` 下创建实体类，继承 `BaseEntity`。

```java
package com.scaffold.admin.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.admin.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "文章")
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("admin_article")
public class AdminArticle extends BaseEntity {

    @Schema(description = "标题")
    private String title;

    @Schema(description = "内容")
    private String content;

    @Schema(description = "状态（0-草稿 1-已发布）")
    private Integer status;

    @Schema(description = "作者ID")
    private Long authorId;
}
```

### 要点

- 继承 `BaseEntity`（自动获得 id、createTime、updateTime、isDeleted）
- `@TableName` 对应数据库表名
- `@Data` + `@EqualsAndHashCode(callSuper = true)` 标准 Lombok 组合
- 每个字段必须有 `@Schema(description = "...")` 注解

---

## Step 3: 创建 Mapper

在 `mapper/` 下创建 Mapper 接口。

```java
package com.scaffold.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scaffold.admin.model.entity.AdminArticle;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AdminArticleMapper extends BaseMapper<AdminArticle> {
}
```

继承 `BaseMapper<T>` 后自动获得：`insert`、`selectById`、`selectList`、`selectPage`、`selectCount`、`updateById`、`deleteById`、`deleteBatchIds` 等方法。

如需自定义 SQL，在 `resources/mapper/` 下创建同名 XML 文件。

---

## Step 4: 创建 DTO / VO

### DTO（请求参数）

```java
// model/dto/CreateArticleDTO.java
@Schema(description = "创建文章请求")
@Data
public class CreateArticleDTO {

    @Schema(description = "标题")
    @NotBlank(message = "标题不能为空")
    private String title;

    @Schema(description = "内容")
    private String content;
}
```

### VO（响应对象）

```java
// model/vo/ArticleVO.java
@Schema(description = "文章信息")
@Data
public class ArticleVO {

    @Schema(description = "文章ID")
    private Long id;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "内容")
    private String content;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
```

### 注意事项

- 所有字段必须有 `@Schema(description = "...")` — 这是前端 API 生成的依据
- VO 内部类命名加父类前缀（如 `ArticleVOAuthorInfo`），避免 orval 合并同名类
- DTO 使用 `@Valid` + JSR-303 校验注解（`@NotBlank`、`@NotNull`、`@Size` 等）

---

## Step 5: 创建 Service

### 接口

```java
// service/ArticleService.java
public interface ArticleService {
    PageResult<ArticleVO> listArticles(int pageNum, int pageSize, String keyword);
    ArticleVO getArticleDetail(Long id);
    void createArticle(CreateArticleDTO dto);
    void updateArticle(Long id, UpdateArticleDTO dto);
    void deleteArticle(Long id);
}
```

### 实现

```java
// service/impl/ArticleServiceImpl.java
@Service
@RequiredArgsConstructor
public class ArticleServiceImpl implements ArticleService {

    private final AdminArticleMapper adminArticleMapper;

    @Override
    public PageResult<ArticleVO> listArticles(int pageNum, int pageSize, String keyword) {
        LambdaQueryWrapper<AdminArticle> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(AdminArticle::getTitle, keyword);
        }
        wrapper.orderByDesc(AdminArticle::getCreateTime);

        Page<AdminArticle> page = adminArticleMapper.selectPage(
                new Page<>(pageNum, pageSize), wrapper);

        List<ArticleVO> records = page.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());

        return new PageResult<>(page.getTotal(), records, page.getCurrent(), page.getSize());
    }

    @Override
    @OperationLog(module = "文章管理", type = OperationType.CREATE)
    public void createArticle(CreateArticleDTO dto) {
        AdminArticle article = new AdminArticle();
        article.setTitle(dto.getTitle());
        article.setContent(dto.getContent());
        article.setStatus(0); // 草稿
        article.setAuthorId(SecurityUtils.getCurrentUserId());
        adminArticleMapper.insert(article);
    }

    @Override
    @OperationLog(module = "文章管理", type = OperationType.DELETE)
    public void deleteArticle(Long id) {
        // MyBatis-Plus 自动处理逻辑删除
        adminArticleMapper.deleteById(id);
    }

    private ArticleVO toVO(AdminArticle entity) { /* 字段映射 */ }
}
```

### 要点

- `@RequiredArgsConstructor` + `final` 字段注入
- CUD 方法标注 `@OperationLog` 审计注解
- 业务校验失败抛 `BusinessException`
- 删除用 `mapper.deleteById()`（不要 `setIsDeleted(1)` + `updateById()`）

---

## Step 6: 创建 Controller

```java
// controller/ArticleController.java
@Tag(name = "articles", description = "文章管理")
@RestController
@RequestMapping("/api/admin/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    @Operation(operationId = "listArticles", summary = "文章列表")
    @GetMapping
    public R<PageResult<ArticleVO>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword) {
        return R.ok(articleService.listArticles(pageNum, pageSize, keyword));
    }

    @Operation(operationId = "getArticleDetail", summary = "文章详情")
    @GetMapping("/{id}")
    public R<ArticleVO> detail(@PathVariable Long id) {
        return R.ok(articleService.getArticleDetail(id));
    }

    @Operation(operationId = "createArticle", summary = "创建文章")
    @PostMapping
    public R<Void> create(@RequestBody @Valid CreateArticleDTO dto) {
        articleService.createArticle(dto);
        return R.ok();
    }

    @Operation(operationId = "updateArticle", summary = "更新文章")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody @Valid UpdateArticleDTO dto) {
        articleService.updateArticle(id, dto);
        return R.ok();
    }

    @Operation(operationId = "deleteArticle", summary = "删除文章")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        articleService.deleteArticle(id);
        return R.ok();
    }
}
```

### OpenAPI 注解要点

| 注解位置 | 注解 | 必填 | 说明 |
|----------|------|------|------|
| 类 | `@Tag(name = "英文tag")` | 是 | orval 按 tag 分目录生成 |
| 方法 | `@Operation(operationId = "...")` | 是 | 决定前端函数名 |
| 方法 | `@Operation(summary = "...")` | 是 | 接口简述 |
| 参数 | `@Schema(description = "...")` | 是 | 字段描述 |

**operationId 命名规范：** `{动词}{资源}`（如 `listArticles`、`getArticleDetail`、`createArticle`、`deleteArticle`）

---

## Step 7: 后端编译验证

```bash
mvn compile
```

确认无编译错误后，启动后端：

```bash
mvn spring-boot:run
```

可访问 Swagger UI 验证接口：`http://localhost:8080/swagger-ui.html`

---

## Step 8: 前端生成 API

```bash
cd admin-frontend
npm run generate:api
```

Orval 读取后端 OpenAPI spec（`http://localhost:8080/api-docs`），自动在 `src/api/generated/` 下生成：

```
src/api/generated/
├── articles/
│   └── articles.ts        # 工厂函数 getArticles()
└── model/
    ├── articleVO.ts        # 类型定义
    ├── createArticleDTO.ts
    └── ...
```

---

## Step 9: 前端页面开发

### 创建页面组件

```tsx
// src/pages/system/ArticleManagement.tsx
import { getArticles } from "@/api/generated/articles/articles"
import type { ArticleVO } from "@/api/generated/model"

const articlesApi = getArticles()

function ArticleManagement() {
  const [data, setData] = useState<ArticleVO[]>([])

  const fetchList = useCallback(async () => {
    const res = await articlesApi.listArticles({ pageNum: 1, pageSize: 10 })
    if (res.code === 200 && res.data) {
      setData(res.data.records ?? [])
    }
  }, [])

  useEffect(() => { fetchList() }, [fetchList])

  return (
    <div className="space-y-6 max-w-7xl mx-auto">
      {/* 页面内容 */}
    </div>
  )
}
```

### API 调用模式

```tsx
// 实例化工厂
const articlesApi = getArticles()

// 调用（返回 R<T> 包装）
const res = await articlesApi.listArticles({ pageNum, pageSize, keyword })
if (res.code === 200 && res.data) {
  // res.data 是 PageResult<ArticleVO>
  setData(res.data.records ?? [])
  setTotal(res.data.total ?? 0)
}
```

**注意：** generated 类型所有字段都是 optional，使用时需 `?? fallback` 或 `!` 断言。

---

## Step 10: 路由注册

在 `src/routes.tsx` 的 `appRoutes` 数组中添加一条：

```tsx
import ArticleManagement from "@/pages/system/ArticleManagement"

export const appRoutes: RouteConfig[] = [
  // ... 已有路由
  { path: "/system/article", title: "文章管理", icon: "FileText", element: <ArticleManagement /> },
]
```

完成后：
- React Router 路由自动注册
- 开发者模式侧边栏菜单自动出现
- 如需普通用户可见，需在 `admin_menu` 表插入对应菜单记录并分配给角色

---

## 权限注册（可选）

后端编译并启动后，运行权限同步脚本：

```bash
cd admin-backend
python script/sync_permissions_from_openapi.py
```

脚本自动读取 OpenAPI spec，将新增的 API 端点注册到 `admin_permission` 表。之后可在角色管理中分配给对应角色。

---

## 检查清单

| 步骤 | 确认项 |
|------|--------|
| 建表 | Flyway 脚本版本号递增、包含基础字段 |
| 实体 | 继承 BaseEntity、@TableName、@Schema |
| Mapper | 继承 BaseMapper、@Mapper |
| DTO/VO | 所有字段 @Schema、VO 内部类加前缀 |
| Service | @RequiredArgsConstructor、CUD 加 @OperationLog |
| Controller | @Tag、@Operation(operationId)、返回 R<T> |
| 编译 | mvn compile 无错误 |
| API 生成 | npm run generate:api 成功 |
| 前端 | 使用 generated 函数、处理 optional 字段 |
| 路由 | routes.tsx 注册、开发者模式可见 |
