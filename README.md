<div align="center">

<img src="app/src/main/ic_launcher-playstore.png" width="104" alt="NewTV" />

# NewTV

**基于 FongMi / CatVod / motao123 二次开发的 Android TV 影音聚合客户端**

在保留原生播放器内核与扩展能力的基础上，对界面进行了美化，并精简了一般用户不常用的入口。

[![Release](https://img.shields.io/github/v/release/ctowl2018/webtv?style=flat-square&label=release&color=0f766e)](https://github.com/ctowl2018/webtv/releases)
[![License](https://img.shields.io/github/license/ctowl2018/webtv?style=flat-square&label=license&color=0f766e)](LICENSE.md)
[![Stars](https://img.shields.io/github/stars/ctowl2018/webtv?style=flat-square&label=stars&color=444444)](https://github.com/ctowl2018/webtv/stargazers)
[![Last commit](https://img.shields.io/github/last-commit/ctowl2018/webtv?style=flat-square&label=last%20commit)](https://github.com/ctowl2018/webtv/commits/main)
[![Android](https://img.shields.io/badge/Android-7.0%2B-3ddc84?style=flat-square&logo=android&logoColor=white)](#下载与安装)
[![Android TV](https://img.shields.io/badge/Android%20TV-Leanback-4285f4?style=flat-square&logo=android&logoColor=white)](#下载与安装)
[![Java](https://img.shields.io/badge/Java-21-ed8b00?style=flat-square&logo=openjdk&logoColor=white)](#技术栈)

</div>

---

## 目录

| 板块 | 内容 |
| :-- | :-- |
| [**项目简介**](#项目简介) | 定位、边界、适合谁 |
| [**界面预览**](#界面预览) | Leanback（电视端）实机截图 |
| [**与上游的主要差异**](#与上游的主要差异) | 本次二次开发做了什么 |
| [**核心能力**](#核心能力) | 播放 / 配置 / WebHome / 安全 / 发布 |
| [**下载与安装**](#下载与安装) | 安装包与选型 |
| [**快速上手**](#快速上手) | 三步跑起来 |
| [**使用示例**](#使用示例) | 配置、WebHome、局域网、遥控 |
| [**架构概览**](#架构概览) | 分层与主链路 |
| [**工程结构**](#工程结构) | 目录与模块职责 |
| [**技术栈**](#技术栈) | 选型与版本 |
| [**构建与开发**](#构建与开发) | 环境、命令、变体矩阵 |
| [**常见问题**](#常见问题) | 高频疑问速查 |
| [**贡献指南**](#贡献指南) | 如何提交改动 |
| [**开源协议**](#开源协议) | GPL-3.0 与合规声明 |

---

## 项目简介

**NewTV** 是 **motao123/webtv**（源自 **FongMi / CatVod** 生态）的二次开发版本，应用 ID 为 `com.fongmi.android.newtv`。同一套代码同时产出**手机端**与**电视端**，但重心放在 **Android TV（Leanback）** 的影音体验上；内容完全由用户自带。

<table>
<tr><th align="left">它做</th><th align="left">它不做</th></tr>
<tr valign="top"><td>

- 导入点播 / 直播配置（URL、文件、assets、局域网同步）
- WebHome：给站点配独立网页首页，并开放原生能力给网页
- 配置、历史、收藏、登录态在手机与电视间一键同步
- 局域网管理页 + 可自托管的公网遥控（服务端源码随仓库给出，设置页入口已精简）

</td><td>

- **不内置任何影视内容**
- **不预置任何点播 / 直播 / 弹幕默认接口**（全部由用户自行配置）
- 不分发、不托管、不推荐任何 JSON / 站源
- 不提供公共遥控服务器（服务端随源码给出，需自行部署）

</td></tr>
</table>

**适合谁**：已有自己的 TVBox / FongMi / CatVod 配置源；想在电视端用更美观的界面浏览；想把手机与电视的配置和进度同步起来。

---

## 界面预览

以下为 **Android TV（Leanback）** 版本实机截图，出自本仓库当前代码：

| 启动欢迎页 | 首页 |
| :--: | :--: |
| <img src="docs/images/leanback-splash.jpg" alt="启动欢迎页" width="420" /> | <img src="docs/images/leanback-home.jpg" alt="首页" width="420" /> |

| 分类导航页 | 搜索页 |
| :--: | :--: |
| <img src="docs/images/leanback-category.jpg" alt="分类导航页" width="420" /> | <img src="docs/images/leanback-search.jpg" alt="搜索页" width="420" /> |

| 搜索结果页 | 收藏页 |
| :--: | :--: |
| <img src="docs/images/leanback-search-result.jpg" alt="搜索结果页" width="420" /> | <img src="docs/images/leanback-keep.jpg" alt="收藏页" width="420" /> |

| 播放详情页 | 播放全屏页 |
| :--: | :--: |
| <img src="docs/images/leanback-vod-detail.jpg" alt="播放详情页" width="420" /> | <img src="docs/images/leanback-player.jpg" alt="播放全屏页" width="420" /> |

> 截图中的站点名称、源站标签等敏感信息均已做马赛克处理。

---

## 与上游的主要差异

本节说明本仓库（`ctowl2018/webtv`）相对 `motao123/webtv` 上游所做的具体改动，便于你判断是否需要跟进上游更新：

- **界面美化（UI 重做）**
  - 首页分类导航改为与分类页一致的分页切换，导航常驻、焦点逻辑统一。
  - 海报选中态只保留片名条反馈（160ms 过渡），控制条渐变加深、放大溢出安全区重做。
  - 弹幕样式、顶部品牌栏与时间格式（统一 `HH:mm` + 加粗无衬线）整体对齐。
- **设置精简**：设置菜单面向普通用户做了瘦身——
  - 移除「关于」项；「版本」项仅保留版本号展示，屏蔽应用内更新检查入口。
  - 移除**公网远程遥控**、**播放记录同步开关**、**无痕模式**、**DoH 切换**等高级/小众开关。
  - 「增强功能」子页移除**调试日志**、**CSP 预热**、**站点注入**、**Git 云盘**、**站点健康排序**等开发者向条目。
  - 保留仍具实用价值的：网盘检测、管理页面、代理设置、家庭过滤、登录态学习、一键同步、播放记录同步的详细配置。
- **不预置内置源/接口**：点播、直播、弹幕均不在代码里内置任何默认地址，首次启动留空，由用户在设置中填写自己的合法配置源。
- **运行时不带 Python**：构建中已剥离 Chaquopy/Python 运行时（见[技术栈](#技术栈)与[工程结构](#工程结构)），所有变体均不含 Python 类型直播源支持。
- **仅简体中文**：移除了英文 / 繁体中文资源，任何系统语言下均走中文界面。

除上述差异外，播放内核、扩展运行时（QuickJS / Jar / HTTP 站源）、WebHome、局域网管理、公网遥控等能力与上游保持一致（其中公网遥控的服务端源码仍在仓库中，仅设置页不再提供入口）。

---

## 核心能力

### 播放与内核

| 能力 | 说明 |
| :-- | :-- |
| 多内核 | EXO / IJK / MPV 三选一，默认 EXO；精简版仅 EXO + IJK |
| 播放连续性 | 换源、换集、解析播放后保持进度与倍速一致 |
| 控制增强 | 播放中重播、内核热切换、0.1x–5x 倍速微调、章节选择、错误阶段提示 |
| 字幕与解码 | 字幕样式高级设置、双字幕、音频直通、Dolby Vision 输出策略 |
| 弹幕 | 总开关与播放页状态强一致，关闭后不会被换集 / 云搜隐式打开 |
| 直播 | 失败自动换源、自定义 EPG、时移、分组 |
| 预加载 | 按内核配置线程 / 缓存 / 时长，自动预加载下一集 |
| 投屏 | 手机端可投屏；电视端可作 DLNA Renderer 接收投屏 |

### 配置与同步

| 能力 | 说明 |
| :-- | :-- |
| 导入增强 | 导入前预检，**失败不覆盖**当前配置；支持 URL / 文件 / assets |
| 配置管理 | 查看当前与历史配置、来源类型、最后使用时间；删除前提示关联影响 |
| 一键同步 | 同步配置与站源、WebHome、搜索记录、继续观看、收藏、设置、登录态 |
| 登录态学习 | 学习 Cookie / 登录态文件路径并跨设备迁移；可预览候选文件内容 |
| 进度与收藏 | 强化恢复路径，配置缺失时提供回退 |
| 搜索相关性 | 结果按关键词匹配度过滤与排序 |

### WebHome 与扩展

| 能力 | 说明 |
| :-- | :-- |
| 网页首页 | 每个站点可配独立网页首页，支持透明背景 |
| Native SDK | 网页通过 `window.fm` 调用受信任范围内的原生播放 / 搜索 / 请求 / 缓存 |
| 内嵌播放 | 网页可把剧集数据直接交给播放器，无需站点接口即可开播 |
| 开发套件 | [`webhome-devkit/`](webhome-devkit/README.md) 提供示例、模板与 AI skills |

### 安全与管理

| 能力 | 说明 |
| :-- | :-- |
| 本地接口加固 | 写操作端点强制 token（含回环请求）、DNS rebinding 防护 |
| SSRF 防护 | 网关禁用自动重定向并逐跳校验；拒绝私有 / 回环 / 链路本地地址 |
| 其它加固 | 路径遍历、XXE、DoS 防护；CORS 白名单收紧到精确端口 |
| 家庭过滤 | 按关键词屏蔽客厅不宜内容，并隐藏指定来源项 |
| 网盘检测 | 校验网盘分享链接有效性，WebHome 与本地 HTTP 均可调用 |
| 语言 | 仅简体中文 |

### 发布与更新

> 以下能力属于底层实现；本仓库设置页已**屏蔽「版本」项的应用内更新检查入口**，普通用户请直接下载新版 APK 覆盖安装（同签名不丢数据）。

| 能力 | 说明 |
| :-- | :-- |
| 多源更新 | GitHub Release / 自建镜像多源切换 |
| 断点续传 | 中断保留进度，同源最多重试 3 次，耗尽后换源并按 HTTP Range 续传 |
| 多重校验 | 体积 → SHA-256 → 包名 → versionCode/versionName → 签名指纹 |
| 安装兜底 | 自动安装失败时导出 APK 到 Downloads 手动安装 |

---

## 下载与安装

最新版本 **v5.17.2** · 一次发版产出 **10 个安装包**（手机 / 电视 × 完整 / 精简 × 三种架构）。

| 设备 | 完整版 | 精简版 |
| :-- | :-- | :-- |
| Android TV · 不确定架构 | `leanback-universal.apk` | `leanback-lite-universal.apk` |
| Android TV · 新电视盒子 | `leanback-arm64_v8a.apk` | `leanback-lite-arm64_v8a.apk` |
| Android TV · 老盒子 | `leanback-armeabi_v7a.apk` | — |
| Android 手机 · 不确定架构 | `mobile-universal.apk` | `mobile-lite-universal.apk` |
| Android 手机 · 新设备 | `mobile-arm64_v8a.apk` | `mobile-lite-arm64_v8a.apk` |
| Android 手机 · 老设备 | `mobile-armeabi_v7a.apk` | — |

> 上表是产物标识；实际发布时文件名会附带版本号（如 `leanback-arm64_v8a-5.17.2.apk`），下载时以实际文件名为准。

**选型建议**

- **不确定 CPU 架构** → 选对应设备的 `universal`。
- **存储紧张 / 只要基础播放** → 选 `lite`：去掉 MPV 内核（约 37MB），体积更小。
- **所有变体均不含 Python 运行时**：本仓库已剥离 Chaquopy，因此不再区分「是否支持 Python 类型直播源」，统一不支持。

> 精简版与完整版包名相同，可互相覆盖安装；应用内更新清单独立分发，不会跨形态「升级」。

下载渠道：

| 渠道 | 地址 |
| :-- | :-- |
| GitHub Releases（主） | <https://github.com/ctowl2018/webtv/releases> |

---

## 快速上手

```text
① 下载对应 APK 安装          Android 7.0（API 24）及以上
② 设置 → 填入你的配置 URL     或选择本地配置文件 / 局域网同步导入
③ 开始浏览、搜索、收藏、播放
```

常用入口：

```text
设置 → 增强功能      网盘检测 · 管理页面 · 代理设置 · 家庭过滤 · 登录态学习 · 一键同步 · 播放记录同步
设置 → 播放器设置    内核、倍速、字幕、投屏等播放偏好
设置 → 弹幕设置      弹幕开关、样式、密度、速度、位置
```

> **首次启动会弹出「远程依赖确认」**——这是 App 请求你确认加载配置中的远程 jar，属于安全机制。**只信任你自己配置来源的内容**。

---

## 使用示例

### 例 1 · 给站点指定 WebHome 首页

在点播 JSON 的站点条目里加 `homePage` 字段（别名 `home_page` / `webHome` / `web_home`）。注意它**不是** `ext`——`ext` 是爬虫扩展参数。

```json
{
  "sites": [
    {
      "key": "mysite",
      "name": "我的站点",
      "type": 3,
      "api": "csp_MySpider",
      "homePage": "https://example.com/home/index.html"
    }
  ]
}
```

其中 `type: 3` 表示该站点是 Spider 类型（`api` 以 `csp_` 开头）。

### 例 2 · 网页调用原生能力

<details>
<summary><b>展开 <code>window.fm</code> 能力表</b></summary>

| 方法 | 说明 |
| :-- | :-- |
| `fm.req(url, options)` | 原生请求，绕过普通浏览器 CORS 限制 |
| `fm.res(url, options)` | 生成本地资源网关地址 |
| `fm.play(url, title, options)` | 播放直链或 `push://` 地址 |
| `fm.vod(siteKey, vodId, title, pic)` | 打开原生详情 / 播放链路 |
| `fm.search(keyword, { direct })` | 调用原生搜索 |
| `fm.openLive()` / `fm.openKeep()` / `fm.openSetting()` | 打开原生页面 |
| `fm.history()` | 读取最近观看记录 |
| `fm.config()` | 获取当前配置与家庭过滤状态 |
| `fm.site()` | 获取当前站点信息 |
| `fm.cache` | 本地缓存能力 |
| `fm.back()` / `fm.reload()` | 处理返回与刷新 |

</details>

把网页上的一集交给 App 播放：

```js
async function playEpisode(vodId, title, pic, episodeUrl) {
  // 推荐：走原生详情 / 播放链路，可使用换源、历史、弹幕等能力
  await window.fm.vod('mysite', vodId, title, pic);

  // 也可直接播放直链
  // await window.fm.play(episodeUrl, title);
}
```

### 例 3 · 局域网管理页

从 App 的「**设置 → 增强功能 → 管理页面**」取得地址，在同一局域网的手机或电脑浏览器中打开。

- 文件管理、一键同步、调试日志、配置管理
- 查看播放状态与进度，发送播放 / 暂停 / 停止 / 上下集 / 循环 / 重播
- 远端设备需**单独配对**——发现设备 ≠ 获得控制权限

> ⚠️ 端口由 App 动态分配，**请使用 App 显示的完整地址**，不要固定假定为 9978。管理地址与配对凭据属敏感信息，请勿公开分享。

### 例 4 · 公网遥控（自托管）

> 本仓库设置页已精简，**不再提供公网遥控的图形入口**；如需使用，可参考下述方案自行接入 `serverless/webtv-remote-go` 与 `remote` 模块。

与局域网管理是**两套独立连接**：电视**主动连接**你部署的 HTTPS 中转服务，**不需要、也不应把本地管理端口暴露到公网**。

```text
部署中转服务（见 serverless/webtv-remote-go）
  → 设备端填写服务地址并启用（默认关闭）
  → 被控设备生成限时配对码
  → 网页控制台输入配对码完成绑定
  → 执行搜索 / 推送播放地址 / 播放控制 / 读取状态
```

连接优先 WebSocket，并提供 HTTP 轮询回退；配对码仅可用一次，不再使用时请**撤销设备授权**。

---

## 架构概览

```mermaid
flowchart TB
    UI["界面层<br/>mobile · leanback"]

    subgraph APP["业务层 :app"]
        CFG["api<br/>配置与站源"]
        PLY["player<br/>EXO / IJK / MPV"]
        SRV["server<br/>本地 HTTP"]
        RMT["remote<br/>公网遥控"]
        WBH["web<br/>WebHome 桥"]
    end

    subgraph RT["扩展运行时"]
        JS["quickjs<br/>JS Spider"]
        JAR["Jar / HTTP 站源"]
    end

    UI --> CFG
    UI --> PLY
    UI --> SRV
    UI --> RMT
    UI --> WBH
    CFG --> JS
    CFG --> JAR
```

**四条主链路**

1. **播放**：配置解析站点 → 选择内核 → `player` 封装 EXO / IJK / MPV → 播放页与 OSD
2. **扩展**：`api` 按站点 `type` 分派 Spider（Jar / JS）或 HTTP 站源，失败可重试
3. **同步**：本地 HTTP `server` 承载管理页，与另一台设备配对后交换配置与壳子状态
4. **遥控**：`remote` 主动连自托管中转，经配对授权后执行受限命令

---

## 工程结构

```text
TV/
├── app/                        Android 主应用
│   ├── src/main/               手机 / 电视共用业务逻辑（Java）
│   │   └── java/com/fongmi/android/tv/
│   │       ├── api/            配置与站源加载
│   │       ├── bean/           数据模型
│   │       ├── db/             Room 数据库
│   │       ├── dlna/           DLNA 投屏
│   │       ├── playback/       播放状态与续播
│   │       ├── player/         EXO / IJK / MPV 封装（最大子包）
│   │       ├── remote/         公网遥控
│   │       ├── server/         本地 HTTP 服务与管理页接口
│   │       ├── setting/        设置项
│   │       ├── ui/             页面与控件
│   │       ├── update/         应用内更新与校验
│   │       ├── utils/          工具类（含依赖信任校验）
│   │       └── web/            WebHome 与原生桥接
│   ├── src/mobile/             手机端 UI 与清单
│   ├── src/leanback/           电视端 UI 与清单
│   ├── src/full/ src/lite/     完整版 / 精简版编译差异
│   ├── schemas/                Room schema
│   └── proguard-rules*.pro     混淆规则（含 ViewBinding 保护）
├── catvod/                     CatVod 抽象层与 Spider 接口
├── quickjs/                    JavaScript 运行时
├── docs/                       下载页（index.html）与图标规范
│   └── images/                 README 用到的实机截图（站点名已打码）
├── webhome-devkit/             WebHome 开发套件（skills / templates / examples）
├── serverless/webtv-remote-go/ 自托管公网遥控中转（Go）
├── scripts/                    构建与发布脚本
├── third_party/                本地 Maven、依赖锁定与补丁源码
└── .github/workflows/          GitHub 侧 CI · 发版 · EPG 同步 · Pages 部署
```

> **Python 运行时已剥离**：本仓库不再构建 `:chaquo` 模块，所有变体的 `FEATURE_PYTHON` 固定为 `false`（见 `settings.gradle` 与 `app/build.gradle`）。因此 `chaquo/` 目录不再参与打包。

---

## 技术栈

| 分类 | 选型 |
| :-- | :-- |
| 语言 | **Java 21**（core library desugaring） |
| 构建 | Gradle Wrapper + AGP + Version Catalog（`gradle/libs.versions.toml`） |
| 平台 | `minSdk 24`（Android 7.0）· `targetSdk 37` · `compileSdk 37` |
| 播放内核 | Media3 / ExoPlayer（内置定制版）、IJK、MPV |
| 扩展运行时 | QuickJS（JS Spider）· JAR · HTTP API（**不含 Python**） |
| 数据与网络 | Room · OkHttp · NanoHTTPD · JGit · jUPnP（DLNA） |
| UI | ViewBinding · Material · Leanback（电视）· Lottie · Glide |
| 其它 | EventBus · NewPipeExtractor · TensorFlow Lite · zxing · biometric |
| 附属服务 | Go 编写的自托管公网遥控中转 |
| 语言 | **仅简体中文**（已移除 en / zh-rTW 资源） |

**Gradle 模块**

| 模块 | 职责 |
| :-- | :-- |
| `:app` | Android 主应用（`main` 共用 + `mobile` / `leanback` 两套 UI） |
| `:catvod` | CatVod 抽象层：Spider 接口、OkHttp、代理工具 |
| `:quickjs` | JavaScript Spider 运行时 |

---

## 构建与开发

**环境要求**

| 依赖 | 版本 |
| :-- | :-- |
| JDK | **21**（`gradle/gradle-daemon-jvm.properties` 已声明工具链 21） |
| Android SDK | `platforms;android-37`、`build-tools;37.0.0` |
| Gradle | **9.6.1**（使用仓库内置 `gradlew`，无需单独安装） |
| AGP | **9.2.1**（见 `gradle/libs.versions.toml`） |

> 本仓库已剥离 Python 运行时，**不再需要宿主机安装 Python** 即可编译。

**常用命令**

```bash
# 编译（与本地自检一致）
./gradlew :app:compileMobileFullArm64_v8aDebugJavaWithJavac \
          :app:compileLeanbackFullArm64_v8aDebugJavaWithJavac \
          :catvod:compileDebugJavaWithJavac

# 打包单个变体调试包（末尾 Release 可换 Debug 做本地调试）
./gradlew :app:assembleLeanbackFullArm64_v8aDebug
./gradlew :app:assembleMobileFullArm64_v8aDebug

# 打包（一次产出全部安装包）
./gradlew :app:assembleLeanbackFullUniversalRelease
./gradlew :app:assembleLeanbackFullArm64_v8aRelease
./gradlew :app:assembleLeanbackFullArmeabi_v7aRelease
./gradlew :app:assembleLeanbackLiteUniversalRelease
./gradlew :app:assembleLeanbackLiteArm64_v8aRelease
./gradlew :app:assembleMobileFullUniversalRelease
./gradlew :app:assembleMobileFullArm64_v8aRelease
./gradlew :app:assembleMobileFullArmeabi_v7aRelease
./gradlew :app:assembleMobileLiteUniversalRelease
./gradlew :app:assembleMobileLiteArm64_v8aRelease

# 单元测试（与 CI 保持一致）
./gradlew :app:testLeanbackFullArm64_v8aDebugUnitTest \
          :app:testMobileFullArm64_v8aDebugUnitTest

# 只跑编译不打包，改动较大时更快
./gradlew :app:compileLeanbackFullArm64_v8aDebugJavaWithJavac \
          :app:compileMobileFullArm64_v8aDebugJavaWithJavac
```

产物路径：`app/build/outputs/apk/<变体>/release/<包名>.apk`
测试报告：`app/build/reports/tests/`

**变体矩阵**：`mode`(mobile/leanback) × `edition`(full/lite) × `abi`(universal/arm64_v8a/armeabi_v7a)，任务名规则 `assemble` + Mode + Edition + Abi + `Release`。`lite` 不产出 `armeabi_v7a`，因此 **6 + 4 = 10 个包**。

**签名**：Release 构建必须提供签名，否则在配置阶段直接中止。

```properties
# local.properties（不入库）
sdk.dir=/path/to/Android/Sdk
storeFile=/absolute/path/to/release.keystore
keyAlias=your_key_alias
storePassword=your_store_password
```

**CI / 发版**

本仓库的 `.github/workflows/` 沿用上游的 GitHub Actions 配置（`ci.yml` 编译 + 单测、`android-release.yml` 发版、`pages.yml` 部署下载站、`epg-sync.yml` 同步 EPG 等）。Fork 后需在仓库 **Settings → Actions** 中手动启用工作流，并提交 `v*` 标签触发发行构建。

---

## 常见问题

<details>
<summary><b>展开高频疑问</b></summary>

**装完是空的，没有影视内容？**
设计如此。本仓库是播放器壳子，不内置内容、不分发 JSON。请在设置中导入你自己的合法配置源。

**全新安装后首页一片空白？**
配置来源尚未导入或加载失败。请到「设置」导入你的配置 URL / 文件，并确认网络可访问该来源。

**精简版（lite）和完整版有什么区别？**
精简版去掉 MPV 内核（约 37MB），保留 EXO/IJK 双内核、QuickJS/JS 源与全部协议能力，体积更小。**两者均不含 Python 运行时**，因此 Python 类型直播源都不可用。

**本地 Gradle 提示 JDK 版本不对？**
本项目要求 **JDK 21**。若系统是 JDK 8/11/17，请让 `JAVA_HOME` 指向 JDK 21 后再执行 `gradlew`。

**Release 构建报 `Release signing is required for release builds`？**
缺少签名配置。按上文「签名」在 `local.properties` 填好 `storeFile` / `keyAlias` / `storePassword`。

**本地单元测试跑不过，但 CI 是绿的？**
本地环境容易因测试类未被加入 worker classpath 而报 `ClassNotFoundException`。**以 CI 结果为准**。

**单元测试覆盖了什么？**
`app/src/test/java/androidx/media3/mpvplayer/` 下集中放置 MPV 内核的策略类测试（HLS 缓存与预载门控、倍速、缓冲进度、清晰度切换、OSD 策略等），均为纯 JVM 单元测试，无需设备。运行方式见[构建与开发](#构建与开发)。

**应用内更新一直失败 / 找不到更新入口？**
本仓库设置页已移除「版本」项的更新检查入口，请直接下载新版 APK 手动覆盖安装（同签名不丢数据）。

**局域网管理页打不开 / 端口不是 9978？**
端口由 App 动态分配，请以「设置 → 增强功能 → 管理页面」显示的完整地址为准，并确保设备在同一局域网。

**怎么自己开发 WebHome 首页或注入脚本？**
先用 [`webhome-devkit/`](webhome-devkit/README.md) 的模板与示例起步。

</details>

---

## 贡献指南

**流程**

```bash
git clone https://github.com/ctowl2018/webtv.git && cd webtv
git checkout -b fix/your-topic

# 改完先本地编译，再推分支让 CI 验证
git commit -m "fix: 一句话说清动机"
git push origin fix/your-topic
# 然后在 GitHub 上向 main 提 Pull Request
```

**约定**

- **提交信息**：沿用 Conventional Commits 前缀（`fix:` / `feat:` / `docs:` / `chore:` / `build:`），标题写清「为什么」。
- **CI 是准入门槛**：`ci.yml` 的编译与单测必须通过。
- **不要提交**：`local.properties`、keystore、`debug/` 下的调试产物（均已在 `.gitignore` 中）。
- **改动前先看上游**：本项目是 FongMi / CatVod / motao123 的二次开发，涉及与上游行为不一致的改动，请在 PR 中说明理由。
- **发版由维护者执行**：合并到 `main` 后由维护者打 `v*` 标签触发发行构建。

**提交前自检**

```bash
./gradlew :app:compileLeanbackFullArm64_v8aDebugJavaWithJavac \
          :app:compileMobileFullArm64_v8aDebugJavaWithJavac
```

---

## 开源协议

本项目采用 **[GNU General Public License v3.0](LICENSE.md)**（GPL-3.0）。你可以自由使用、修改与分发，但**衍生作品必须以相同协议开源**并保留版权声明——这正是本项目作为 FongMi / CatVod / motao123 二次开发所遵循的方式。

提交 Pull Request 即表示你同意你的贡献按同一协议授权。

**合规声明**

> 本仓库只提供技术实现和播放器壳子能力：**不内置影视内容、不预置任何点播 / 直播 / 弹幕接口、不维护站源、不分发 JSON**。
> 所有内容来源均由用户自行配置，请确保合法合规，并遵守所在地区的法律法规。

---

<div align="center">

**文档** ·
[开发套件](webhome-devkit/README.md) ·
[更新日志](CHANGELOG.md)

<sub>基于 <a href="https://github.com/FongMi/TV">FongMi / CatVod</a> · <a href="https://github.com/motao123/webtv">motao123/webtv</a> 生态二次开发</sub>

</div>
