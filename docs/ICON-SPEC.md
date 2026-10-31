# 应用图标资源规格（2026-10-31 v2 定稿替换）

源文件：`design/v2/` 定稿套件（1024×1024 图标 + 1280×720 横幅）。
Android **不支持直接使用 SVG**，全部按密度栅格化后落盘；渲染用 Edge headless，缩放用 LANCZOS。

| 文件 | 用途 |
| --- | --- |
| `icon-v2.svg` | 圆角方卡片图标（1024，22.6% 圆角，透明角外区） |
| `icon-v2-square.svg` | 满幅方形图标（Play 商店图源） |
| `foreground-v2.svg` | 纯三角前景（无卡片底板，透明底） |
| `banner-v2.svg` | 1280×720 TV 横幅（三角 + 文字标） |
| `mono-v2.svg` | 单色三角（主题图标形状参考） |

## 一、图形规范

- 卡片底：纯色 `#0C1219`（**不做渐变**，青色/橙色不参与背景）。
- 播放三角：45° 折面分割，亮面 `#FFF0CE → #FFB45E`，暗面 `#FF8F3C → #D9590F`，折线从左上顶点切至斜边。
- 三角内叠一层 `#FFF` 0.22→0 的斜向高光（`sheen`），模拟卡纸受光，**不加阴影、不加描边、不加外发光**。
- 三角外接框 460×548（1024 画布），占卡片 45%×54%。

## 二、图标尺寸与格式

### 1. 传统启动图标（API < 26，以及部分第三方桌面）

| 目录 | 尺寸 | 格式 |
| --- | --- | --- |
| mipmap-mdpi | 48 × 48 | PNG（RGBA） |
| mipmap-hdpi | 72 × 72 | PNG（RGBA） |
| mipmap-xhdpi | 96 × 96 | PNG（RGBA） |
| mipmap-xxhdpi | 144 × 144 | PNG（RGBA） |
| mipmap-xxxhdpi | 192 × 192 | PNG（RGBA） |

`ic_launcher.png` 用 22.6% 圆角方（squircle，圆角由 SVG 内 clipPath 裁出，四角透明）；
`ic_launcher_round.webp` 用正圆裁切，同尺寸同密度。
圆形图标必须用 **无损 WebP**：有损 WebP 会在透明斜边渗出暗色描边，圆看起来会带方角。

### 2. 自适应图标（API 26+，`mipmap-anydpi-v26/ic_launcher(_round).xml`）

| 图层 | 画布 | 尺寸（mdpi→xxxhdpi） | 格式 |
| --- | --- | --- | --- |
| foreground | 108 × 108 dp | 108 / 162 / 216 / 324 / 432 px | PNG（RGBA，透明底） |
| background | — | 纯色 `@color/ic_launcher_background` = `#0C1219` | color |
| monochrome | 108 × 108 dp | 矢量 | VectorDrawable（Android 13+ 主题图标） |

- 108dp 画布中，可见区只有中心 72dp，安全圆直径 66dp。
- 前景三角按 **46dp 高**（约 39dp 宽）居中，对角约 60dp，完整落在安全圆内 —— 圆形 / 圆角方 / 水滴 / 方形任何遮罩都不会切到尖角。
- 前景层只放三角，**不带卡片底板**；底色由 `<background>` 提供，这样系统遮罩才能铺满到边缘，不会出现「小卡片套在大卡片里」的双层边。
- `<monochrome>` 只在 API 33+ 生效，低版本系统忽略该节点，因此可放在同一个 `anydpi-v26` 文件里。系统会用壁纸取色重绘，路径只能是纯白实心。

### 3. Google Play 商店图

`app/src/main/ic_launcher-playstore.png`：**512 × 512 px，32-bit PNG，完全不透明**。
满幅、无圆角（圆角由商店裁切），用 `icon-v2-square.svg` 渲染。

### 4. Android TV 横幅 banner

`android:banner` 必须是 **位图**，不能是 adaptive-icon（自适应图标是正方形，塞进 16:9 槽位会被拉伸）。

| 目录 | 尺寸 | 格式 |
| --- | --- | --- |
| mipmap-mdpi | 160 × 90 | PNG |
| mipmap-hdpi | 240 × 135 | PNG |
| mipmap-xhdpi（基准） | 320 × 180 | PNG |
| mipmap-xxhdpi | 480 × 270 | PNG |
| mipmap-xxxhdpi | 640 × 360 | PNG |

### 5. 应用内 logo 与启动图

| 资源 | 用途 | 尺寸 | 格式 |
| --- | --- | --- | --- |
| `drawable-nodpi/ic_logo.png` | 首页 / 影视页标题栏图标，Glide 兜底图 | 288 × 288 | PNG（RGBA，**仅三角、透明底**） |
| `drawable-*/logo_word.png` | 首页标题栏字标「影视 Videos」 | 墨迹高 25dp（25/38/50/76/101 px，即原 21dp 的 1.2 倍） | PNG（RGBA，透明底） |
| `drawable-nodpi/startup_logo.png` | 启动窗口背景（`startup_window_background.xml`，`gravity=fill`） | 1920 × 1080 | PNG（不透明，16:9，组合标按半尺寸绘制） |
| `drawable-nodpi/splash_mark.png` | 应用内启动闪屏层的透明底组合标（显示宽度 = 屏宽 26%） | 1011 × 186 | PNG（RGBA） |

- `ic_logo` 去掉卡片底板：标题栏底色 `#0E1116` 与卡片底 `#0C1219` 只差 2 级，带底板会看成一块「发暗的方块」；只留三角在 53dp 下更干净，三角放大到画布 78% 保证辨识度。
- 启动图与启动窗口底色同为 `#0C1219`，`Theme.Splash` 期间不会出现底色与图片边界跳色。
- 字标排版：中文用 Noto Sans SC Black（wght 900）+ 8.5% 字距，西文用 Arial Black 收到中文的 0.8 高度，
  两者按**墨迹底边**对齐；中文是主体，Videos 作为配套。这与横幅 `banner-v2.svg` 的「类 Arial Black」调性一致。

### 6. Android 12+ 系统启动屏（Leanback）

`leanback/res/values-v31/styles.xml` 覆盖 `Theme.Splash`：

- `android:windowSplashScreenBackground` = `@color/startup_background`（`#0C1219`）
- `android:windowSplashScreenAnimatedIcon` = `@drawable/ic_splash_blank`（透明块）
- 仍保留 `android:windowBackground` 指向 layer-list，供 API < 31 使用

原因：Android 12 起系统用 SplashScreen 生成启动窗口，只取 `windowBackground` 里的底色、**不绘制**其中的位图，
且图标槽是方形、放不下横向的「图标 + 影视 Videos」。因此系统层只保留同色底，组合标改由
`activity_home.xml` 的 `splashLayer`（`splash_mark.png`，宽度 = 屏宽 26%）绘制，
首帧后停留 900ms 再 320ms 淡出，与系统启动窗口无缝衔接。

### 7. 移动端

**本次不改动移动端**（只以 Leanback 变体交付）。`mobile/res/values/styles.xml` 的 `Theme.Splash` 保持原样。
若后续要让移动端跟随 v2 图标，可把 `windowSplashScreenAnimatedIcon` 改成 `@mipmap/ic_launcher`，
并补 `<item name="windowSplashScreenBackground">@color/ic_launcher_background</item>`。

### 8. 首页标题栏的品牌组合（Leanback）

`leanback/res/layout/activity_home.xml` 的工具栏：

```
[ CustomLogoBar：logo(42dp) + logo_word(20dp) ]  [ CustomTitleView：源名称 ]  …  [搜索][收藏][设置][时钟]
```

- **焦点从源名称移到品牌组合**：`CustomLogoBar`（`ui/custom/CustomLogoBar.java`）承接原 `CustomTitleView` 的焦点逻辑——
  点击弹源列表、长按重载配置、上键刷新当前源。`CustomTitleView` 退化为纯文本（`focusable=false`）。
- **选中态不用焦点框**：改为整组慢速呼吸，`alpha` 在 1.0 ↔ 0.3 之间往返（单程 1200ms，`AccelerateDecelerate`），
  下限固定 0.3，保证最暗时字标仍可辨认；失焦立即恢复 1.0。
- **源名称只露 5 秒**：`HomeActivity` 用 `mEnterTime` 记进入首页的时刻，`onResume` 开窗口；
  `setTitle()` 按 `SOURCE_NAME_DURATION - elapsed` 计算剩余时长，超时则 `INVISIBLE`（保留占位，右侧图标不位移）。
  用户主动切源（`setSite`）会重置窗口，新源名称照样露 5 秒。没有配置源时标题直接留白，避免与品牌字标重复。
- 源名称样式：`16sp`、`@color/home_source_name`（`#9BA7B6`），弱于品牌字标，形成「品牌主 / 源名辅」的层级。

### 8.1 工具栏右侧三个入口图标（2026-10 更换）

首页标题栏与共用品牌栏使用同一套图标，均为 24dp 视口、1.9 描边、白色、圆头/圆角连接：

| 用途 | 资源 | 形状 |
| --- | --- | --- |
| 搜索 | `drawable/ic_toolbar_search.xml` | 放大镜：大镜片（描边中心 r7.5、外径 16.9）+ 右下 27° 开口 + 45° 短手柄 |
| 收藏 | `drawable/ic_toolbar_star.xml` | 五角星描边，内外半径比 0.43 |
| 设置 | `drawable/ic_toolbar_setting.xml` | 六角螺母：平顶六边形 + 内圈（孔/边 ≈ 0.42） |

- 三个图形的包围盒统一收敛到约 17dp（渲染后 36–38px @2x），垂直中心对齐到同一基线，
  替换后不会出现“某个图标偏小/偏高”的问题；槽位仍是 40dp + 8dp 内边距（`style/HomeToolbarIcon`）。
- 旧的 `ic_action_search` / `ic_detail_keep_off` / `ic_action_setting` 仍保留给其他页面使用，未删除。

## 三、本次改动（v2 替换）

覆盖写入：

- `main/res/mipmap-{m,h,x,xx,xxx}dpi/ic_launcher.png`、`ic_launcher_round.webp`
- `main/res/drawable-{m,h,x,xx,xxx}dpi/ic_launcher_foreground.png`
- `main/res/ic_launcher-playstore.png`、`main/ic_launcher-playstore.png`
- `main/res/drawable-nodpi/ic_logo.png`
- `main/res/drawable-nodpi/startup_logo.jpg`、`leanback/res/drawable-nodpi/startup_logo.jpg`
- `leanback/res/mipmap-*/ic_banner.png`

改写：

- `main/res/drawable/ic_launcher_monochrome.xml` —— 换成与彩色图标同一条三角轮廓（原为「光圈环 + 三角」，与新图标不符）。
- `main/res/values/colors.xml` —— `ic_launcher_background` `#0B1017` → `#0C1219`；`startup_background` `#060A0F` → `#0C1219`。
- 新增 `leanback/res/values-v31/styles.xml` —— Android 12+ 启动屏底色与图标显式接管（见 6）。

**作用范围：仅 Leanback 变体**。`mobile/` 源集本次未做任何改动（含 `mobile/res/values/styles.xml`、`mobile/res/drawable/ic_site_block.xml`）。
图标位图仍放在共享的 `main/res/` 下（历史位置不变），Leanback 构建读取同一份资源。

未改动（本来就指向上述资源，无需改引用）：

- `main/AndroidManifest.xml`：`android:icon="@mipmap/ic_launcher"`、`android:roundIcon="@mipmap/ic_launcher_round"`
- `leanback/AndroidManifest.xml`：`android:banner="@mipmap/ic_banner"`
- `leanback/res/layout/activity_home.xml`（48dp）、`mobile/res/layout/fragment_vod.xml`（36dp）：`@drawable/ic_logo`
- `main/java/.../utils/ImgUtil.java`：Glide `error(R.drawable.ic_logo)`
- `main/res/drawable/startup_window_background.xml`：`@drawable/startup_logo`

## 四、重新生成

```bash
python design/gen_icons_v2.py "D:/WorkBuddy/TV/webtv/app/src"   # 应用图标（各密度）
python design/gen_brand.py                                      # 字标 logo_word + 启动图 startup_logo / splash_mark
```

`gen_icons_v2.py` 内置 v2 几何常量（`TRI_PATH` / `TRI_BBOX`）与两处可调参数：

- `FG_TRI_DP = 46.0` —— 自适应前景三角高度（dp/108dp 画布）
- `LOGO_TRI_RATIO = 0.78` —— 应用内 `ic_logo` 三角占画布比例

`gen_brand.py` 的可调参数（文件头部）：

- `WORD_DP = 21.0` —— 首页字标墨迹高度（dp）
- `SPLASH_TRI_PX = 186` / `SPLASH_WORD_PX = 122` —— 启动画面三角与字标高度（px @1920×1080）
- `build_word()` 的 `cn_track`（中文 8.5% 字距）、`lat_ratio`（西文 0.8）、`gap_ratio`（中英文间距 0.38）

改图标只需替换 `design/v2/*.svg`，也可同步修改这些常量后重跑。
替换前的旧资源快照：`design/icon-backup-before-v2.tar.gz`。

## 五、播放器控制条（Leanback，2026-10-31）

### 1. 按钮行（仅 Leanback，`leanback/res/layout/view_control_vod_action.xml`）

- 槽位 `ControlSlot`：50×40dp，左右各 3dp → 相邻间距 6dp（更紧凑）。
- 顺序：上一集、下一集、弹幕、字幕、音轨、倍速、片头、片尾、播放设置（螺母图标）。
- 原“…”更多按钮已移除，其入口由螺母图标承担（`ic_control_panel.xml`，参考图比例：六边形 15.6×18dp、
  中心圆孔外径 ≈ 宽度 41%、描边 1.55dp）；菜单项分别由播放页“换源”与设置面板承接。

### 2. 片头 / 片尾（`ic_control_opening.xml` / `ic_control_ending.xml`）

- 参考图实测：总高 70px 基准下，竖条 6×70、空心三角 38×52、描边 6、尖端距竖条 2px，圆头圆角。
- 换算 24dp 画布：竖条高 19dp、三角 10.3×14dp、描边 1.6dp，视觉外框 14×19dp。
- 按钮为“图标 + 文字”：`ControlIconTextSlot`（wrap_content，padding 10/12dp，drawablePadding 6dp，13sp）；
  默认文案 `play_skip_op` / `play_skip_ed`（跳过片头 / 跳过片尾），设定偏移后显示时间（如 `00:57`）。

### 3. 进度条聚焦态（`CustomSeekView`）

- 不再画焦点框，也不缩放 `DefaultTimeBar`（纵向缩放会把内部圆形滑块拉成椭圆）。
- 聚焦时：已播放段/滑块/左右时间文字变 `@color/control_focus`（#FF9F1A），
  并在轨道下方淡入 10dp 高的 `shape_control_seek_focus_glow`（#33FF9F1A）表达“变粗”。

### 4. 控制条背景（`leanback/res/drawable/shape_controller.xml`）

- 原 82% 纯色 → 线性渐变：`angle=90`（自下而上由深到浅），底边 `#59000000`（黑 35%）→ 顶部 `#00000000`。
- 组件尺寸/内边距不变（shape 无 padding）。若要“顶部最深、底部透明”，`angle` 改 270 并互换两色。

### 5. 启动画面居中

- `activity_home.xml` 的 `splashLayer` 内组合标由 `top|start`+24dp 边距改为 `center`；
  标题栏的 `logoBar` 仍在左上角 24dp，闪屏整层 320ms 淡出。
- 冷启动最初约 1s 是系统启动屏（v31 主题的 `windowSplashScreenAnimatedIcon` 为透明块），
  这段时间只有底色、没有标识；应用首帧后组合标才出现（约 1.2s）。如需这段也有标识，
  可把 `windowSplashScreenAnimatedIcon` 换成 `@drawable/ic_launcher_foreground`。

## 六、控制条与页面品牌栏（2026-10-31 第二次调整）

### 1. 片头 / 片尾按钮位置
- 按钮行顺序：上集、下集、弹幕、字幕、音轨、倍速、播放设置、**[弹性留白]**、跳过片头、跳过片尾。
- `view_control_vod_action.xml` 用 `Space(weight=1)` 把这两个按钮推到最右端、右端对齐；留白不可聚焦。
- `ControlIconTextSlot` 更紧凑：内边距 6/8dp、图标与文字 4dp、左右外边距 2dp，文字 `textStyle=bold`。
- 焦点链右端改为 `ending.setNextFocusRightId(View.NO_ID)`（原先停在 panelBtn 上）。

### 2. 进度条聚焦态
- 强调色 `control_focus` 由 #FF9F1A 改为 **#FFFFFF**，光带 `control_focus_glow` 改为 **#33FFFFFF**；
  聚焦时仍会淡入 10dp 光带，滑块依旧正圆（不缩放控件）。

### 3. 控制条渐变
- `shape_controller.xml`：`angle=90`（底→顶），底边 **#80000000（黑 50%）** → 顶部透明。

### 4. 弹窗背景统一
- 新增 `main/res/drawable/shape_dialog_glass.xml`：上沿 22dp 圆角、`#B3`（≈70% 不透明）暗色玻璃、1dp 高光边。
- `BaseBottomSheetDialog#setBehavior` 对非透明弹窗统一套用；`ControlDialog` 去掉紫色渐变面板与白色 sheen，
  改用同一张背景 —— 弹幕/字幕/音轨/播放设置观感一致。

### 5. 首页源名称
- 取消 5 秒自动淡出：`setTitle()` 直接常显，删除 `SOURCE_NAME_DURATION` / `hideSourceName` / `mEnterTime`。

### 6. 共用品牌栏 `view_brand_toolbar.xml`
- 分类页（VodActivity）、搜索页（SearchActivity）、搜索结果页（CollectActivity）、播放详情页（VideoActivity）
  顶部统一插入该 include：42dp 图标 + 10dp 间距 + 20dp 字标，左/上 24dp，右侧搜索/收藏/设置 + 24sp 时钟，
  与首页标题栏同尺寸同位置；组合标不聚焦、不响应点击（无源切换）。
  组合标尺寸取原 53/12/25 的 80%（42/10/20），2026-10 起首页与品牌栏同步缩放。
- 中间的 `brandTitle` 使用首页源名称的规范（14dp 左间距、16sp、`@color/home_source_name`、单行 marquee），
  默认 INVISIBLE 占位；搜索结果页把「“xx”的搜索结果」写进这里，原来单独一行已删除（含 `setHeader` 调用）。
- 详情页：`video` 改为 `layout_below="@id/brand"`（marginTop 8dp），`name` 改为 `alignTop="@id/video"`；
  进入全屏播放时品牌栏 `GONE`，退出时恢复。
- 时钟由 `BrandToolbar` 统一托管，跟随 Activity 的 onResume/onStart 启停。
