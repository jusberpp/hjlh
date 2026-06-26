# E学慧通学习认证管理端

基于 Vue 3、Vite、Element Plus、Pinia 和 Vue Router 实现，页面结构及交互习惯按若依 Vue3 管理端组织。

## 已实现

- 学员信息管理：筛选、单个新增、编辑、授权课程多选、主课程、授权手机号、Excel 模板批量导入校验、查询结果导出。
- 课程管理：按年级/学科/状态筛选、新增、编辑、启停、受控删除、小鹅通链接或课程 ID 映射、讲师姓名与头像上传、多组上课时间配置。
- 课程资料管理：课程入口、讲义/作业多文件上传、每个文件单独设置开放时间、二次编辑、受控删除。
- 物理作业：题目数量、每题小题分、提交截止时间、提交进度展示和小题分数据导出。
- 提分宝管理：从已对学生显示的作业中单选关联、已上传过提分宝的作业禁止重复选择、ZIP 解析校验、确认上传和逻辑删除。
- 登录鉴权：JWT 登录、登录态持久化、路由守卫、退出登录。

## 本地运行

```bash
npm install
npm run dev
```

默认地址：`http://localhost:5178`

## 前后端联调

管理端已对接 `backend` Spring Boot 后端，数据来自正式接口而非页面内存。

### 1. 启动后端

后端支持本地零依赖启动（H2 内存库 + 内存令牌存储）：

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

服务地址 `http://localhost:8081`，初始管理员 `admin / Admin123!`。

如需 MySQL/Redis 生产形态，参考 `backend/README.md` 使用 Docker Compose。

### 2. 启动前端

```bash
cd admin-web
npm run dev
```

`vite.config.js` 已将 `/login`、`/getInfo`、`/study`、`/course`、`/auth`、`/common` 等路径代理到 `http://localhost:8081`，无需额外配置。

### 3. 验证联调

启动后访问 `http://localhost:5178`，使用 `admin / Admin123!` 登录后即可操作全部管理页面。

端到端冒烟脚本覆盖登录、课程、学员、资料、提分宝和旧 H5 兼容全链路：

```bash
python3 scripts/admin_backend_smoke.py --base http://localhost:5178
```

## 目录结构

```text
src/
├── api/                      接口模块
│   ├── login.js              登录、获取用户信息
│   ├── study/student.js      学员 CRUD、Excel 导入导出
│   ├── course/course.js      课程 CRUD、选项、讲师头像、启停
│   ├── course/material.js    资料上传、批量新增、编辑、小题分导出
│   └── course/treasure.js    提分宝作业选项、解析、确认、删除
├── utils/
│   ├── request.js            fetch 封装：JWT 注入、错误处理、文件下载/上传
│   └── format.js             文件大小、扩展名等格式化
├── store/auth.js             Pinia 鉴权状态：token 持久化、用户信息、权限
├── router/index.js           路由 + 登录守卫
├── layout/AdminLayout.vue    侧边栏 + 顶栏 + 退出登录
├── views/
│   ├── login/index.vue       登录页
│   ├── student/index.vue     学员信息管理
│   ├── course/index.vue      课程管理
│   └── material/index.vue    课程资料 + 提分宝
└── styles/index.css          全局样式
```

## 接口约定

完整接口约定见：[学习认证系统后台接口文档](docs/学习认证系统后台接口文档.md)，实施口径以 [接口与需求冲突清单](../specs/admin-web-backend/api-reconciliation.md) 和后端实际 Controller 为准。

- 普通响应 `{ code, msg, data }`，分页响应 `{ code, msg, total, rows }`，`code === 200` 为成功。
- 后端 `LocalDateTime` 统一使用 ISO 格式（`yyyy-MM-ddTHH:mm:ss`），前端 `utils/request.js` 提供 `toBackendDateTime` / `toDisplayDateTime` 互转。
- 鉴权：管理端 `Authorization: Bearer {jwt}`；旧 H5 请求头 `token`，两套链路独立。
- 权限标识：`study:student:*`、`course:course:*`、`course:material:*`、`course:treasure:*`。

## 构建产物

```bash
npm run build     # 输出到 dist/
npm run preview   # 预览构建产物
```
