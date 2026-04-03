# 快速上手

> 创建日期：2026-04-03
> 最后更新：2026-04-03

## 默认登录账号

- 管理员账号：`admin`
- 管理员密码：`admin123`

## 文档入口

- 更详细的开发流程参考 `./docs/GUIDE_DEVELOPMENT_WORKFLOW.md`
- AI 协作开发参考根目录 `AGENTS.md` 或 `CLAUDE.md`

## 环境要求

- Java 17+
- Maven 3.9+
- Node.js 18+
- PostgreSQL 14+
- Redis 6+
- MinIO（可选；如需使用文件上传、头像、文件中心等功能时配置）

## 一次性初始化

### 1. 创建数据库

先创建一个空数据库，名称可自定义，后续与 `application-dev.yml` 保持一致即可。示例：

```sql
CREATE DATABASE scaffold_spring_dev;
```

### 2. 导入初始化 SQL

项目根目录已提供初始化脚本 `./init.sql`，将其导入刚创建的数据库。

常见做法示例：

```bash
psql -U postgres -d scaffold_spring_dev -f init.sql
```

如果你使用 Navicat、DataGrip、pgAdmin 等图形化工具，也可以直接执行 `init.sql`。

### 3. 复制后端配置模板

将后端配置模板复制为开发环境配置：

```bash
cd admin-backend/src/main/resources
cp application-template.yml application-dev.yml
```

Windows PowerShell 可用：

```powershell
Copy-Item application-template.yml application-dev.yml
```

### 4. 填写 `application-dev.yml`

文件位置：`admin-backend/src/main/resources/application-dev.yml`

需要至少填写以下配置：

```yml
database:
  host: "127.0.0.1"
  port: 5432
  username: "postgres"
  password: "your-password"
  name: "scaffold_spring_dev"

redis:
  host: "127.0.0.1"
  port: 6379
  password: ""
  db: 0
  max_connections: 50

minio:
  endpoint: "http://localhost:9000"
  access-key: "minioadmin"
  secret-key: "minioadmin"
  bucket-name: "admin-uploads"

jwt:
  secret: "please-change-to-a-random-long-secret"
  access-expiration: 300000
  refresh-expiration: 604800000
```

说明：

- `database`：填写 PostgreSQL 连接信息
- `redis`：填写 Redis 连接信息
- `jwt.secret`：必须改成你自己的随机长字符串
- `minio`：若本地暂不使用文件相关功能，可先保留默认值；实际使用相关功能前再补齐可用配置

## 启动项目

### 1. 启动后端

```bash
cd admin-backend
mvn compile
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

启动后可访问：

- Swagger UI：`http://localhost:8080/swagger-ui.html`
- OpenAPI：`http://localhost:8080/api-docs`

### 2. 启动前端

```bash
cd admin-frontend
npm install
npm run dev
```

前端默认地址：

- `http://localhost:3000`

前端开发服务器会将 `/api` 请求代理到 `http://localhost:8080`。

## 首次联调建议

1. 先确认后端能正常访问 `http://localhost:8080/swagger-ui.html`
2. 再启动前端并打开 `http://localhost:3000`
3. 使用 `admin / admin123` 登录验证初始化是否成功

## 关于前后端协作

本项目采用“后端先行，API 生成驱动前端”的方式协作：

1. 后端先开发接口，并补齐 OpenAPI 注解
2. 后端执行 `mvn compile` 验证
3. 由用户手动执行 `npm run generate:api`
4. 前端使用 `src/api/generated/` 中生成的函数和类型接入

注意：不要手写新的前端 API 文件，优先使用 generated endpoint 函数。
