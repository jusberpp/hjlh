# 公众号 H5 落地页

Vue 3 + Vite + Vant 的移动端 H5 工程。仓库里可以沉淀多个业务互不相关的 H5 项目，但每次只选择一个当前活动 H5 项目进行本地预览和打包发布。

## 技术栈

- Vue 3
- Vite
- TypeScript
- Vant
- Pinia
- SCSS
- Tailwind CSS 4
- PostCSS `px` 转 `vw`

## 常用命令

```bash
npm install
npm run dev
npm run build
npm run build:test
npm run preview
```

## H5 项目组织

每个 H5 项目独立放在 `src/pages/<projectName>/` 下，项目目录名统一使用驼峰命名，例如 `studentAuth`、`gaokaoLive`。项目代码、项目数据、项目专属样式和项目专属素材放在一起。一个 H5 项目内部可以只有单页，也可以有多个路由页：

```txt
src/pages/<projectName>/
  index.vue
  data.ts
  routes.ts        # 内部需要多页面时维护
  styles.css       # 有项目专属样式时维护
  views/
  components/
  assets/
```

当前活动 H5 项目由 `virtual:h5-entry` 根据 `curProjectName` 指向具体页面目录。目前默认项目在 `vite.config.ts` 中配置为：

- 默认项目：`summerTifenbao`
- `src/pages/summerTifenbao/index.vue`：暑期课程提分宝文件发布与下载操作页，包含学生端物理提交流程和运营后台关键页面
- 暑期课程学生端按实际时间展示下一次未开始课次和已到显示时间的资料；物理作业截止后禁止提交，截止前提交后直接进入提分宝生成中页面
- H5 手动填写手机号时需通过短信验证码校验，成功后由后端同步写入学员信息中的授权手机号
- 构建或运行时可通过 `curProjectName=<projectName>` 指定当前 H5 项目，`projectName` 对应 `src/pages/<projectName>/`
- `src/pages/studentAuth/index.vue`：学生认证 H5 项目入口壳
- `src/pages/studentAuth/entry.ts`：学生认证 H5 项目入口配置
- `src/pages/studentAuth/routes.ts`：学生认证 H5 项目内部路由配置
- `src/pages/studentAuth/views/`：认证、确认报名信息、绑定手机号、开通成功等路由页
- `src/pages/studentAuth/components/`：当前 H5 项目自己的步骤组件和页面组件
- `src/pages/studentAuth/data.ts`：当前 H5 项目自己的静态配置
- `src/pages/studentAuth/store.ts`：当前 H5 项目自己的页面状态
- `src/pages/studentAuth/assets/`：当前 H5 项目自己的图片素材

不要把所有 H5 项目做成一个大配置生成器。后续 H5 结构差异大时，直接新增独立项目目录，复用稳定的公共能力即可。

## 项目级文件

- `src/pages/index.ts`：当前要预览和发布的 H5 项目入口配置
- `src/router/index.ts`：项目级路由创建入口，应只挂载当前活动 H5 项目的路由
- `vite.config.ts`：读取 `curProjectName` 并把 `virtual:h5-entry` 指向当前 H5 项目入口
- `src/styles/`：全局样式和 Tailwind 入口
- `src/shared/`：跨 H5 项目稳定复用的组件和 composable
- `src/utils/`：跨 H5 项目稳定复用的工具函数和请求封装
- `src/stores/`：真正跨 H5 项目共享的全局状态；单个 H5 项目的状态优先放在该项目目录内
- `src/wechat/`：微信 JS-SDK 能力预留

## 新增 H5 项目流程

1. 在 `src/pages/<projectName>/` 新增 H5 项目目录，目录名使用驼峰命名。
2. 新增 `src/pages/<projectName>/entry.ts`，导出该 H5 项目的入口配置。
3. 把项目专属页面、组件、样式、数据、素材放到该项目目录内。
4. 如果该 H5 项目内部需要多个页面，为该项目维护自己的路由配置，例如 `src/pages/<projectName>/routes.ts`。
5. 使用 `curProjectName=<projectName> npm run build` 验证该项目类型检查和构建。

`entry.ts` 维护单个 H5 项目的入口信息，例如：

```ts
import type { H5PageEntry } from "../types";
import routes from "./routes";

const entry: H5PageEntry = {
  name: "gaokaoLive",
  title: "2026 高考直播公开课",
  loadComponent: () => import("./index.vue"),
  routes,
};

export default entry;
```

这里的 `name` 用来标识当前 H5 项目，`title` 是默认浏览器标题，`loadComponent` 指向当前项目入口，`routes` 指向当前项目内部路由配置。如果该 H5 项目只有单页，可以只配置根路径到 `loadComponent`。

## 打包与部署

默认打包 `studentAuth`：

```bash
npm run build
```

指定当前 H5 项目打包：

```bash
curProjectName=studentAuth npm run build
curProjectName=gaokaoLive npm run build
curProjectName=summerTifenbao npm run build
```

本地预览也可以指定项目：

```bash
curProjectName=studentAuth npm run dev
curProjectName=summerTifenbao npm run dev
```

生成服务器部署 tag：

```bash
curProjectName=studentAuth npm run tag
curProjectName=gaokaoLive npm run tag
```

也可以把项目名作为参数传给脚本：

```bash
npm run tag -- gaokaoLive
```

构建产物默认输出到 `dist/`。网站直接挂 `dist/` 目录即可，入口文件就是 `dist/index.html`。

构建时会根据 `curProjectName` 自动生成部署路径前缀，默认规则是项目名转 kebab-case：

```text
studentAuth -> /student-auth/
gaokaoLive -> /gaokao-live/
```

因此同一域名部署多个 H5 时，可以分别把构建产物挂到对应子路径。构建命令仍只需要指定当前项目：

```bash
curProjectName=studentAuth npm run build
curProjectName=gaokaoLive npm run build
```

如果线上实际子路径不等于默认规则，可以在自动化部署环境中注入 `H5_BASE_PATH` 覆盖，不需要手动修改 `vite.config.ts`：

```bash
curProjectName=studentAuth H5_BASE_PATH=/activity/student-auth/ npm run build
```

如果要切到另一个 H5 项目打包，不需要改代码，只需要通过 `curProjectName` 指定 `src/pages/<projectName>/` 目录名。构建产物应只服务当前活动 H5 项目；历史 H5 项目保留在仓库中，但不应默认进入当前项目路由。

## 打 Tag 流程

使用 `tag.sh` 脚本为 H5 项目打部署 tag，tag 格式为：`<环境>-H5project#<项目名>-<日期>-<版本号>`

### 使用方式

```bash
# 方式 1：通过 npm run 打 tag（需设置 curProjectName）
curProjectName=studentAuth npm run tag
curProjectName=gaokaoLive npm run tag

# 方式 2：直接传参给脚本
npm run tag -- studentAuth
npm run tag -- gaokaoLive

# 方式 3：直接运行脚本（推荐）
./tag.sh studentAuth
./tag.sh gaokaoLive
```

### 格式说明

- 环境：根据当前分支自动确定（test、uat 分支对应各自的环境，main 分支对应 prod）
- 项目名：驼峰格式（如 `studentAuth`、`gaokaoLive`）
- 日期：当天日期（YYYY-MM-DD 格式）
- 版本号：基于同前缀 tag 自动递增（从 00 开始）

示例 tag：`test-H5project#studentAuth-2026-06-12-00`

### 支持的分支

- `test` 分支 → test 环境 tag
- `uat` 分支 → uat 环境 tag  
- `main` 分支 → prod 环境 tag

### 前提条件

- 确保当前分支有提交（脚本会先 `git push`）
- 确保项目存在对应入口文件：`src/pages/<projectName>/entry.ts`

## 公共能力

- `src/shared/components/QrCodeBlock.vue`：二维码展示模块
- `src/shared/composables/useCountdown.ts`：倒计时状态
- `src/utils/request.ts`：HTTP 请求封装
- `src/utils/time.ts`：时间格式化和直播状态判断
- `src/wechat/index.ts`：微信 JS-SDK 分享配置入口

公共能力只放跨 H5 项目稳定复用的内容。单个 H5 项目的结构、文案、样式、素材、状态和一次性逻辑优先留在项目目录内。

不要为了判断是否可复用而默认扫描所有历史 H5 项目。只有在当前任务影响多个 H5 项目、用户明确要求抽取，或已经看到相同/高度相似实现时，再评估抽到公共目录。

公共能力不能包含具体活动文案、页面视觉或投放素材。

## 静态资源

- H5 项目专属图片、二维码、背景图：放在 `src/pages/<projectName>/assets/`
- 多个 H5 项目共用素材：放在 `src/shared/assets/`
- 需要固定公开 URL 的文件：放在 `public/`

## 样式约定

组件样式使用 SCSS，按 750px 设计稿原值书写 `px`，移动端适配由 PostCSS 在构建期将 `px` 转为 `vw`。

## 微信能力预留

`src/wechat/index.ts` 预留了 JS-SDK 分享配置入口。后续需要自定义分享、授权、支付等能力时，补一个后端签名接口，再在页面入口调用 `setupWechatShare` 即可。
