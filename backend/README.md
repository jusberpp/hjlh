# E学慧通后端

面向 `admin-web` 与现有学生认证 H5 的正式 Spring Boot 后端。管理端采用若依风格的 JWT、权限标识、`AjaxResult`/`TableDataInfo` 响应和组织数据范围；旧 H5 继续使用名为 `token` 的请求头。

## 技术基线

- Java 17
- Spring Boot 3.5.15
- Spring Security + JWT
- Spring JDBC
- MySQL 8
- Redis 7
- Flyway
- Apache POI
- 本地鉴权文件存储，可替换为 COS/OSS/MinIO

## 本地验证

本机无需预装 Maven：

```bash
./mvnw test
./mvnw package
```

启动生产形态需要 MySQL 和 Redis。安装 Docker 后可运行：

```bash
docker compose up --build
```

服务地址：`http://localhost:8081`

- Swagger UI：`http://localhost:8081/swagger-ui.html`
- 健康检查：`http://localhost:8081/actuator/health`

## 初始管理员

仅用于首次开发启动：

```text
用户名：admin
密码：Admin123!
```

初始密码已经使用 BCrypt 保存。部署前仍必须修改密码，并替换 `JWT_SECRET`、`PHONE_HASH_SECRET` 和数据库密码。

## 配置

复制 `.env.example` 并按部署环境注入变量。核心配置：

- `DB_URL`、`DB_USERNAME`、`DB_PASSWORD`
- `REDIS_HOST`、`REDIS_PORT`、`REDIS_PASSWORD`
- `JWT_SECRET`
- `PHONE_HASH_SECRET`
- `FILE_STORAGE_ROOT`、`FILE_TEMP_ROOT`
- `XIAOE_MODE`

`XIAOE_MODE=stub` 只用于开发和联调，不会调用真实小鹅通。生产切换前必须根据现网小鹅通 API/SDK 补充正式适配器和凭据。

## 数据库迁移

Flyway 启动时自动执行：

- `V1__initial_schema.sql`：课程、学员、多课程授权、资料、作业、小题分、提分宝批次和学生个人文件、导入与审计表。
- `V2__dicts_and_permissions.sql`：年级、学科、资料类型、课程状态字典，以及管理菜单和按钮权限。

关键数据库兜底：

- 同组织学号唯一。
- 有效手机号唯一。
- 同组织年级与学科课程唯一。
- 一个学员课程组合唯一。
- 一个作业只能存在一个有效提分宝批次。

## 鉴权

- 管理端：`Authorization: Bearer {jwt}`。
- 旧 H5：请求头 `token: {studentToken}`，默认 Redis 保存 30 分钟。
- 测试环境：`STUDENT_TOKEN_STORE=memory`，不依赖 Redis。
- 所有资料和个人提分宝文件均由后端鉴权下载，不支持 `bypassToken=true`。

## 已实现接口

接口路径以 `specs/admin-web-backend/api-reconciliation.md` 为准：

- `/study/student/**`
- `/course/course/**`
- `/course/material/**`
- `/course/treasure/**`
- `/auth/schools`
- `/auth/grades`
- `/auth/verify-student`
- `/auth/bind-phone`
- `/auth/bind-result`
- `/auth/files/{fileId}/download`
- `/auth/treasure-files/{fileId}/download`

OpenAPI 由运行时自动生成。

## 文件安全

- 文件名仅用于展示，存储键使用随机 ID。
- 临时上传令牌、提分宝解析令牌一次性消费。
- 校验扩展名、文件头、大小和路径。
- ZIP 限制条目数量、解压总大小，并阻止路径穿越。
- 提分宝确认时保存批次与每个学生的个人文件，学生只能下载属于自己的文件。
- 当前本地存储适合单机部署；多实例生产环境应替换为 COS/OSS/MinIO 实现。

## 回滚

业务表默认使用逻辑删除。数据库结构回滚应先备份，再按 Flyway 版本编写显式回滚脚本；不要直接删除已经在线使用的迁移文件。
