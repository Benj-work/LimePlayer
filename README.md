# 青柠播放器（Lime Player）

参考 `10.139.95.4` 设备上爱奇艺 App 的界面语言实现的本地视频播放器。片库临时读取
`/sdcard/ExoPlayerLibrary/`（含子目录），UI 走爱奇艺那套「深色底 + 品牌绿 + 五列海报墙 + 详情页 + 全屏播放器」的结构，
但品牌、包名、图标均为自研（`com.lime.player`，青柠播放器），不使用爱奇艺任何素材与商标。

## 交付物

| 文件 | 说明 |
| --- | --- |
| `青柠播放器.apk` | 已签名 release 包（v1.0），可直接安装 |
| `screenshots/01-首页.png` | 首页（榜单行 + 横版推荐位）实机截图 |
| `screenshots/02-片库.png` | 电影频道（竖版卡 + 副标题）实机截图 |
| `screenshots/03-详情页.png` | 详情页实机截图 |
| `screenshots/04-播放器.png` | 全屏播放器（控制层展开）实机截图 |

安装与授权（首次）：

```bash
adb -s 10.139.95.4:5555 install -r 青柠播放器.apk
adb -s 10.139.95.4:5555 shell appops set com.lime.player MANAGE_EXTERNAL_STORAGE allow
adb -s 10.139.95.4:5555 shell am start -n com.lime.player/.ui.LibraryActivity
```

## 界面还原要点

参照对象是设备上爱奇艺的首页 `com.qiyi.video.speaker/com.qiyi.video.speaker.activity.WelcomeActivity`，
用 `uiautomator dump` 取控件几何、再用像素行统计定字号、定色值：

| 元素 | 爱奇艺实测（1920×1200） | 本实现 |
| --- | --- | --- |
| 顶栏 | 0–120 px，底色 `#202C3C` | 80 dp，`#202C3C` |
| 导航按钮 | 实心胶囊 `#343F4D`，80 px 高、圆角=半高、左右内边距约 39 dp、间距 36 px | 53 dp 高胶囊 `#343F4D`，左右 38 dp，间距 24 dp |
| 导航文字 | 24 sp，灰 `#A4A9B0`（实测墨迹 68×33 px） | 24 sp（墨迹 67×34 px） |
| 筛选按钮 | 同款胶囊，168×80 px | 174×80 px |
| 频道栏 | 120–248 px，同底色，首项文字 x≈79 | 85 dp，首项文字 x≈83 |
| 频道文字 | 约 30 sp，选中绿 `#0BBE06` + 下划线 | 29 sp / 选中 31 sp 加粗绿 + 下划线 |
| 内容底色 | `#131F30`（深藏蓝，非纯黑） | `#131F30` |
| 第一排卡片 | 5 列，344×458 px，间距 32 | 5 列 344×458 px 等比 |
| 卡片标题 | 约 24 sp（墨迹高 34 px） | 24 sp（墨迹高 34 px，一致） |
| 排名角标 | 封面左上，约 42 sp 斜体 | 42 sp 粗斜体 + 阴影 |
| 画质角标 | 封面右下胶囊，高 42 px | 19 sp 胶囊，由真实分辨率推导（4K/1080P/720P） |
| 第二排 | 4 列横版卡 444×353 px，标题 + 剧情式副标题 | 4 列 438×353 px，标题 + 类型副标题 |
| 详情/播放器 | — | 标题 52 sp、按钮 68 dp、播放器标题 24 sp、控制按钮 64 dp |
| 观看历史页 | 标题居中 32 sp、右上「编辑」、4 列卡 450×~250 px、卡间距 24 px | 标题 32 sp、编辑 24 sp、4 列 444×250 px、间距 24 px |
| 我的收藏页 | 爱奇艺无此页，沿用历史页框架 | 同上，去掉进度条，副标题改「类型 · 规格」 |

## 功能

- **首页**：青柠品牌栏（搜索 / 历史 / 收藏 / 设置）、频道 Tab（推荐·电影·剧集·纪录片·全部）、筛选排序（默认 / 评分 / 年份 / 名称 / 时长）、长按收藏。
  - 「推荐」= 爱奇艺式双段结构：第一排 5 张带排名角标的竖版海报卡，第二排 4 张 16:9 横版卡（标题 + 类型副标题）；
  - 其余频道为 5 列竖版卡，卡片带「8.5分 · 2025」副标题。
- **搜索**：点顶栏「搜索」，整条顶栏被一条搜索框自右向左滑入覆盖（260 ms 减速插值），光标自动落入输入框并弹出键盘；
  边输边过滤，右侧 × 清空，按「取消」或返回键收回——收回报文会一并清空条件、恢复完整列表。
- **观看历史**：独立页面（对齐爱奇艺 `PlayerRecordActivity`）——居中标题「观看历史」、右上「编辑」、四列 16:9 卡片，
  封面底部绿色进度条按真实观看比例填充，副标题为「第 N 集 · 已看 53%」；点卡片按记录续播；
  编辑态露出删除角标，可单条删除或「清空全部」。
- **我的收藏**：与观看历史同一套页面框架——居中标题「我的收藏」、右上「编辑」、四列 16:9 卡片，
  副标题为「剧情 / 喜剧 / 动画 · 1080P」；按片库顺序排列，点卡片进详情页，
  编辑态可「取消收藏」单条或「清空全部」，空态提示「在片库长按海报即可收藏」。
- **详情页**：背景剧照 + 渐变遮罩、评分、`类型 · 画质 · 时长`、「继续观看 00:12:34」、剧情简介、导演/主演、剧集横排选集、收藏按钮。
- **播放器**（Media3 ExoPlayer 1.3.1）：
  - 单击显隐控制层（4 秒自动隐藏，暂停时常驻），双击左右 ±10 秒，单击暂停显示中央大按钮；
  - 进度条可拖动，`当前时间 / 总时长`；
  - 倍速面板（0.75×–2×）、画面比例三档（适应 / 铺满 / 裁剪）、选集面板（当前集打勾）、下一集；
  - 断点续播（每 5 秒落盘）、播完自动记录、退出保存；
  - 全屏沉浸（隐藏状态栏/导航栏），横屏锁定，屏幕常亮。

## 片库约定

```
/sdcard/ExoPlayerLibrary/
├── 哪吒.mp4              影片
├── 哪吒.nfo              Kodi/Emby 风格元数据（<movie>）
├── 哪吒.jpg              同名海报（也可 poster.jpg / folder.jpg）
├── 生命奇观.S01E01.广西溶洞.mp4   剧集（正则识别 SxxEyy）
├── 生命奇观.S01E01.广西溶洞.nfo   <episodedetails>，title 作为分集名
└── 生命奇观.S01E01.广西溶洞.jpg
```

- 视频格式：mp4 / mkv / mov / avi / ts / webm / flv / m4v / wmv / rmvb。
- 文件名 `S01E02`（大小写、`.`/`_`/空格分隔均可）会自动归并为同一部剧；无 `.nfo` 时标题取自文件名。
- 缺失海报时显示占位图；`.nfo` 缺失时评分/简介留空，不影响播放。
- 画质与时长由 `MediaMetadataRetriever` 现场读取，不依赖 `.nfo`。

### 详情页横版背景

详情页头图优先用横版剧照，命名 `<视频文件名>-fanart.jpg`，与视频放在同一目录：

```
哪吒.mp4
哪吒.jpg            竖版海报（片库卡片用）
哪吒-fanart.jpg     横版剧照（详情页头图用）
```

查找顺序：`-fanart` → `.fanart` / `-backdrop` / `.backdrop` / `-thumb` → 目录下的 `fanart.jpg`；
都没有则回落到竖版海报。剧集取该季第一集的 fanart。换图不需要重新打包，直接覆盖文件并重进 App 即可。

当前片库 7 部作品的横版图已随片库放好，来源与分辨率见 [`backdrops/SOURCES.md`](backdrops/SOURCES.md)。

头图高度按图片自身比例算：`h = clamp(屏宽 × 图高 / 图宽, 700dp, 一屏高)`，渐变遮罩与头图等高。
这样 16:9 的剧照正好铺满、上下都不裁切，更宽的图只裁两侧，遮罩末端与页面底色重合，
不会出现「图被顶到上面、下面露出背景」的接缝。

## 源码结构

```
app/
├── app/src/main/java/com/lime/player/
│   ├── data/  MediaEntry · NfoParser · LibraryRepository（扫描/归并/分类）
│   ├── ui/    LibraryActivity · DetailActivity · PlayerActivity
│   │          HistoryActivity · FavoritesActivity
│   │          MediaCardAdapter · EpisodeAdapter · HistoryAdapter · FavoritesAdapter
│   ├── service/ VoicePlayerService（AIDL 服务端）· PlaybackCommandBus（Binder→主线程通道）
│   └── util/  Prefs（进度/历史/收藏）· ImageLoader（海报缓存）· Ui（dp/时间格式化）
├── app/src/main/res/layout|drawable|values/   爱奇艺风格布局、矢量图标、深色主题
├── app/libs/PlayerController-release.aar      语音助手 AIDL 契约 SDK
└── app/build.gradle（Media3 1.3.1、compileSdk 34、minSdk 28）
```

## 语音助手 AIDL 集成（PlayerController SDK）

播放器作为 **AIDL 服务端**，语音助手作为客户端，通过 `com.huawei.hmt.playercontroller.PLAYER_SERVICE`
隐式发现、显式绑定（SDK 内部完成 resolve + setComponent）。

### 依赖与注册

- SDK 放在 `app/libs/PlayerController-release.aar`，`build.gradle` 以 `implementation files(...)` 引入；
  SDK 自身 `minSdkVersion=28`，因此本工程 minSdk 由 23 提升到 **28**。
- 服务端实现：`service/VoicePlayerService extends BasePlayerService`，清单注册：

```xml
<service android:name=".service.VoicePlayerService" android:exported="true">
    <intent-filter>
        <action android:name="com.huawei.hmt.playercontroller.PLAYER_SERVICE"/>
    </intent-filter>
</service>
```

- oneway 方法在 **Binder 线程**回调；服务里统一转主线程（`PlaybackCommandBus`）后再操作播放器/UI。

### 三条指令的落地行为

| 指令 | 当前实现 | 说明 |
|---|---|---|
| `play(mediaId, positionMs)` | 解析片源 → `startActivity(PlayerActivity)`，`startMs` 优先进度 | 已有播放页时走 `onNewIntent` → 原地换片（不重建页面） |
| `skip(1)` 片头 | `seekTo(0)` | 暂无真实片头区间，等价回到开头 |
| `skip(2)` 片尾 | 有下一集则切下一集，否则跳到结尾 | 同上，无真实片尾区间 |
| `skip(3/4)` 广告/前情回顾 | 仅回执 toast | 本地片源没有广告与前情回顾 |
| `focusActor(actorId)` | 回执 toast（片源标注了该演员时提示「缺少演员时间轴」） | 没有演员出场时间轴，暂不跳转 |

### mediaId 解析规则

按顺序尝试：精确 id（`movie:/path`、`ep:/path`、`series:标题#S1`）→ 文件路径 → 片名（完全/包含）→ 文件名片段。
**都不匹配时回退到片库第一部**并 toast 说明，保证「接口调通 + 能拉起播放」。测试 APK 固定发 `media-A`，
所以联调时会看到「未匹配「media-A」，播放默认片源：浪浪山小妖怪」——这是预期行为。

### 后台拉起播放器

语音助手在前台、播放器在后台时，Android 12 禁止后台应用直接 `startActivity`。
本工程声明 `SYSTEM_ALERT_WINDOW` 并需要在装机后放开 appops（`tools/build-install.ps1` 已包含）：

```powershell
adb shell appops set com.lime.player SYSTEM_ALERT_WINDOW allow
```

### 联调步骤（assistantTest-debug.apk）

1. 安装并启动 `com.huawei.hmt.assistanttest`，标题栏出现「已连接播放器服务」即绑定成功。
2. 点「播放：片源A + 60秒」→ 播放器被拉起并从 60 秒开始播放。
3. 点「跳过片头」→ 播放器回到 00:00；点「跳过广告」/「只看演员X」→ 提示 toast。
4. 日志：`adb logcat -s VoicePlayerService VoiceBus`，可看到 Binder 线程收到指令、主线程分发的全过程。

逐条验证结果见 `screenshots/08-语音助手联调.png` 与下表。

### 联调记录（设备 10.139.95.4，Android 12）

| 项 | 结果 |
|---|---|
| 服务发现 | `cmd package query-services -a com.huawei.hmt.playercontroller.PLAYER_SERVICE` → `com.lime.player.service.VoicePlayerService` |
| 绑定 | 测试 App 顶部显示「已连接播放器服务」 |
| play 冷启动 | 进程由 `Start proc … for service {…VoicePlayerService}` 拉起，无需先开 App |
| play 带时间点 | 传入 60000 ms，播放页初始进度 01:0x（此前 00:00 起播） |
| play 二次 | 同一窗口实例收到 `onNewIntent`，原地换片，无页面重建 |
| skip 后台 | Binder 线程 `onSkip type=1` → 主线程 `PlayerActivity.onSkip pos=106745` → 播放位置变 00:00 |
| skip 广告 | 收到 `onSkip type=3`，仅 toast「当前片源没有广告」 |
| focusActor | 收到 `onFocusActor actorId=actor-X`，仅 toast 回执 |
| toast 可见性 | SystemUI 出现 `Toast` 窗口（`pkg=com.lime.player`） |

## 本地构建

```powershell
$env:JAVA_HOME='D:\demo_player_workspace\tools\jdk\jdk-17.0.13+11'
$env:ANDROID_HOME='D:\demo_player_workspace\tools\sdk'
$env:TEMP='D:\demo_player_workspace\tmp'; $env:TMP=$env:TEMP
D:\demo_player_workspace\tools\gradle\gradle-8.7\bin\gradle.bat -p D:\demo_player_workspace\app `
  -g D:\demo_player_workspace\gh assembleRelease --no-daemon
```

也可以直接用 Gradle Wrapper（首次会自动下载 Gradle 8.7）：

```bash
cd app
./gradlew assembleRelease
```

签名使用仓库内 `app/limeplayer.jks`（别名 `lime`，口令 `limeplayer`，**仅供 demo，勿用于正式发布**）。
`app/local.properties` 里的 `sdk.dir` 是本机路径，已在 `.gitignore` 中排除，构建前按自己的 SDK 位置生成即可。

## 设备适配注意

正常状态下布局按满屏 1920×1200 排布，播放器控制层贴在底部（控制条底边 y≈1179，留 14 dp 内边距）。

排查记录：曾出现底部约 100 px 区域点击无效。当时 `dumpsys window windows` 里能看到系统悬浮窗
`com.smarthome.centerapp`（`SYSTEM_ALERT_WINDOW`，frame = `[0,1100][1920,1200]`）拦截了该区域的触摸。
那是设备「禁用语音助手」模式的提示层，退出该模式后窗口消失，触摸恢复正常——不需要、也不应该为此把 UI 上移。
因此相关偏移已全部撤销（之前为绕过它加的 `marginBottom=70dp`、首页 104 dp / 详情页 120 dp 底部内边距都已还原为满屏布局）。

另外左侧 36 px 是 systemui 的手势边条窗口（`[0,0][36,1200]`），页面左边距 24 dp = 36 px 正好从它右侧开始，无需额外处理。
