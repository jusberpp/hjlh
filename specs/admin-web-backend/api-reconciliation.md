# 接口与需求冲突清单

## 1. 必须在实现前统一的差异

| 主题 | 当前实际情况 | 实施口径 |
| --- | --- | --- |
| 学生认证路径 | H5 已调用 `/auth/*`；后台接口文档新增 `/h5/*` | 本期保留 `/auth/*`，不直接替换；新版 `/h5/*` 后续版本化实现 |
| 鉴权头 | 管理端为 `Authorization: Bearer`；旧 H5 为请求头 `token` | 两套安全链并存，分别校验，禁止混用 |
| 学员 ID | 管理端 `studentId` 是数据库主键；旧 H5 `studentId` 是学号，`id` 才是主键 | 使用独立 DTO 明确转换 |
| 课程数量 | 管理端支持多课程；旧 H5 响应为单课程 | 授权关系增加唯一主课程 |
| 年级/学科值 | 管理端文档使用枚举；旧 H5 使用“高一”等中文 | 数据库存枚举，旧 DTO 映射中文 |
| 课程分层 | 旧 H5 使用 `learningLevels` 查课程；管理端页面没有该字段 | 学员保留可空分层字段；旧逻辑优先主课程，分层仅兼容历史数据 |
| 课程删除 | 页面提示关联将失效 | 后端禁止删除有关联课程，建议停用或先解除关系 |
| 资料时间字段 | 管理端 `openTime`；H5 文档部分使用 `displayTime` | 数据库统一 `open_time`，各 DTO 做字段映射 |
| 提分宝数据 | 管理端文档只定义 ZIP 批次 | 必须增加每个学生的个人文件明细 |
| 提分宝发布 | PRD 要求确认发布；当前页面只有确认上传 | MVP 确认即发布，内部保留发布状态 |
| 小题分 | 页面满分输入最小值为 1；PRD 要求学生可得 0 分和半分 | 满分可为正数，学生得分必须支持 0 和 decimal |
| 错误码 | `42212`、`42213` 在文档中重复 | 后端建立唯一错误码枚举，输出最终对照表 |
| 手机号唯一 | 管理接口文档写“由业务决定”；旧认证文档明确检查占用 | MVP 采用一个手机号一个有效学员 |
| 文件访问 | 文档示例存在公开 `fileUrl` | 正式接口返回受控下载入口或短期签名地址，不返回永久公开路径 |

## 2. 管理端接口实施清单

### 学员

- `GET /study/student/list`
- `GET /study/student/{studentId}`
- `POST /study/student`
- `PUT /study/student`
- `POST /study/student/import-template`
- `POST /study/student/import/validate`
- `POST /study/student/import/confirm`
- `POST /study/student/export`

### 课程

- `GET /course/course/options`
- `GET /course/course/list`
- `GET /course/course/{courseId}`
- `POST /course/course/lecturer-avatar`
- `POST /course/course`
- `PUT /course/course`
- `PUT /course/course/status`
- `DELETE /course/course/{courseIds}`

### 资料

- `GET /course/material/course-list`
- `GET /course/material/list`
- `POST /course/material/upload`
- `POST /course/material/batch`
- `GET /course/material/{materialId}`
- `PUT /course/material`
- `DELETE /course/material/{materialIds}`
- `POST /course/material/{materialId}/submission/export`

### 提分宝

- `GET /course/treasure/homework-options`
- `GET /course/treasure/list`
- `POST /course/treasure/parse`
- `POST /course/treasure`
- `DELETE /course/treasure/{batchIds}`

## 3. 旧 H5 回归清单

- `GET /auth/schools`
- `GET /auth/grades`
- `POST /auth/verify-student`
- `POST /auth/bind-phone`
- `GET /auth/bind-result`
- `GET /auth/files/{fileId}/download`

回归时必须使用 `h5-project/src/pages/studentAuth/api/index.ts` 的实际类型，而不是只参考新版 H5 章节。

## 4. 后续能力，不阻塞 Admin Web MVP

- `/h5/student/home`
- `/h5/auth/phone/code`
- `/h5/student/phone/bind`
- `/h5/course/{courseId}/materials`
- `/h5/homework/{homeworkId}`
- `/h5/homework/{homeworkId}/submit`
- `/h5/homework/{homeworkId}/treasure-status`
- 答案提交后解锁、小题分学生录入、预计发布时间、提分宝单文件替换和下载记录页面。

这些接口可复用本期数据模型，但应在独立规格中补齐状态机和前端流程后再实现。

