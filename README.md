# 青柠播放器（LimePlayer）

## 源码结构

```
LimePlayer/
├── app/                                    Gradle 工程根
│   ├── settings.gradle                     模块声明 + 仓库（aliyun 镜像 / google / mavenCentral）
│   ├── build.gradle                        AGP 8.5.2
│   ├── gradle.properties                   JVM 参数、AndroidX 开关
│   ├── gradlew / gradlew.bat               Gradle Wrapper（8.7）
│   ├── gradle/wrapper/                     wrapper 配置与 jar
│   ├── limeplayer.jks                      demo 签名密钥
│   └── app/
│       ├── build.gradle                    应用模块：compileSdk 34 / minSdk 28 / targetSdk 34、签名、依赖
│       ├── libs/PlayerController-release.aar   语音助手 AIDL 契约 SDK
│       └── src/main/
│           ├── AndroidManifest.xml         四个页面 + 语音控制 Service + 权限声明
│           ├── java/com/lime/player/
│           │   ├── data/
│           │   │   ├── MediaEntry.java         影片/剧集数据模型（标题、评分、时长、剧集列表、剧照路径等）
│           │   │   ├── NfoParser.java          读取 .nfo 元数据（标题、简介、导演、演员、类型、评分）
│           │   │   └── LibraryRepository.java  扫描片库、归并剧集、按频道过滤、id/路径查找
│           │   ├── ui/
│           │   │   ├── LibraryActivity.java    首页/片库：频道 Tab、筛选排序、滑动搜索、收藏入口
│           │   │   ├── DetailActivity.java     详情页：剧照头图、简介、选集、收藏
│           │   │   ├── PlayerActivity.java     播放器：控制层、快进退、倍速、画面比例、断点续播
│           │   │   ├── HistoryActivity.java    观看历史页
│           │   │   ├── FavoritesActivity.java  我的收藏页
│           │   │   ├── MediaCardAdapter.java   首页卡片（竖版海报卡 + 横版宽卡两种视图类型）
│           │   │   ├── EpisodeAdapter.java     详情页选集列表
│           │   │   ├── HistoryAdapter.java     历史页卡片（封面 + 进度条）
│           │   │   └── FavoritesAdapter.java   收藏页卡片
│           │   ├── service/
│           │   │   ├── VoicePlayerService.java AIDL 服务端，接收语音助手的 play/skip/focusActor
│           │   │   └── PlaybackCommandBus.java Binder 线程 → 主线程的命令转发通道
│           │   └── util/
│           │       ├── Prefs.java              播放进度、观看历史、收藏
│           │       ├── ImageLoader.java        海报/剧照解码与内存缓存
│           │       └── Ui.java                 dp 换算、时间格式化
│           └── res/
│               ├── layout/                  activity_library / activity_detail / activity_player
│               │                            activity_history / item_media_card / item_wide_card
│               │                            item_episode / item_history_card / item_tab
│               ├── drawable/                背景、胶囊、遮罩、进度条、矢量图标
│               ├── color/                   文字选择器
│               └── values/                  colors / dimens / strings / themes
└── README.md
```

## 构建指南

### 环境要求

| 项 | 版本 |
|---|---|
| JDK | 17 |
| Android SDK | compileSdk 34、build-tools 34.0.0 |
| Gradle | 8.7（仓库内已带 Wrapper，无需单独安装） |
| Android Gradle Plugin | 8.5.2 |
| minSdk / targetSdk | 28 / 34 |

### 准备 SDK 路径

在 `app/local.properties` 写入本机 SDK 位置（该文件已在 `.gitignore` 中，不进仓库）：

```properties
sdk.dir=D\:\\path\\to\\android-sdk
```

### 构建

使用 Wrapper（首次运行会自动下载 Gradle 8.7）：

```bash
cd app
./gradlew assembleRelease        # Windows: gradlew.bat assembleRelease
```

或使用本机已安装的 Gradle：

```bash
gradle -p app assembleRelease
```

产物：`app/app/build/outputs/apk/release/app-release.apk`。

调试包同理：`./gradlew assembleDebug`。

### 依赖

```gradle
implementation 'androidx.appcompat:appcompat:1.6.1'
implementation 'androidx.recyclerview:recyclerview:1.3.2'
implementation 'androidx.media3:media3-exoplayer:1.3.1'
implementation 'androidx.media3:media3-ui:1.3.1'
implementation 'androidx.media3:media3-datasource:1.3.1'
implementation files('libs/PlayerController-release.aar')   // 语音助手 AIDL SDK，自身要求 minSdk 28
```

### 签名

`app/limeplayer.jks`（别名 `lime`，store/key 口令均为 `limeplayer`），已在 `app/app/build.gradle` 的 `signingConfigs.release` 中配置，仅供 demo 使用，正式发布请替换为自己的密钥。

### 安装到设备

```bash
adb install -r app/app/build/outputs/apk/release/app-release.apk
adb shell appops set com.lime.player MANAGE_EXTERNAL_STORAGE allow
adb shell appops set com.lime.player SYSTEM_ALERT_WINDOW allow   # 语音助手从后台拉起播放器需要
```
