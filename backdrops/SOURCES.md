# 详情页横版背景（fanart）来源清单

片库里每部作品的详情页背景，都是按片名从网络检索到的横版剧照，已随片库放在
`/sdcard/ExoPlayerLibrary/` 下，命名规则为 `<视频文件名>-fanart.jpg`。

| 作品 | 设备文件名 | 分辨率 | 比例 | 来源 |
| --- | --- | --- | --- | --- |
| 浪浪山小妖怪 | `浪浪山小妖怪-fanart.jpg` | 1920×811 | 2.37 | https://x0.ifengimg.com/ucms/2025_29/4D2A25B0776CB3E1A6091C24599A44D325DB1020_size703_w1920_h811.jpg |
| 哪吒之魔童闹海 | `哪吒-fanart.jpg` | 1280×720 | 1.78 | https://vpic-cover.puui.qpic.cn/y3575m4enq5/y3575m4enq5_1736233012_hz.jpg/1280 |
| 夏洛特烦恼 | `夏洛特烦恼-fanart.jpg` | 1280×720 | 1.78 | http://puui.qpic.cn/vpic_cover/m0925l1gmqf/m0925l1gmqf_hz.jpg/1280 |
| 年会不能停！ | `年会不能停-fanart.jpg` | 3000×2000 | 1.50 | https://image11.m1905.cn/uploadfile/2023/1205/20231205105055330334.jpg |
| 人在囧途 | `人在囧途-fanart.jpg` | 1908×1070 | 1.78 | https://materials.cdn.bcebos.com/images/127800150/75cb15f430e0062d58953b0b8c8f7cf6.jpeg |
| 龙门客栈 | `龙门客栈-fanart.jpg` | 1280×720 | 1.78 | https://puui.qpic.cn/vpic_cover/o331156bn2v/o331156bn2v_hz.jpg/1280 |
| 生命奇观（S01E01） | `生命奇观.S01E01.广西溶洞-fanart.jpg` | 1350×872 | 1.55 | https://pic1.zhimg.com/v2-32bd76ef13c8b8e1ef95028280632d98_r.jpg |

## 选取方式

检索条件为「片名 + 剧照」，并对结果做三道过滤后才采用：宽 ≥ 1000–1200 px、
宽高比 1.4–2.7（必须是横版）、能正常解码为图片。抓取脚本见 `tools/fetch-backdrop.ps1`。

## 需要你自己确认的一点

这些图片是**按片名检索 + 比例过滤**挑出来的，我这边没有图像识别能力，无法逐张确认画面内容
确实是该片剧照（不是同名的其它版本、也不是海报拼图）。打开详情页看一眼即可判断；
若要替换，把同名文件覆盖掉、再 `adb push` 到 `/sdcard/ExoPlayerLibrary/` 就行，不需要重新打包：

```bash
adb -s 10.139.95.4:5555 push 新的横版图.jpg "/sdcard/ExoPlayerLibrary/哪吒-fanart.jpg"
```

## 命名与回落规则

`LibraryRepository.resolveBackdrop()` 依次查找：

1. `<视频文件名>-fanart.jpg`（本清单用的）
2. `<视频文件名>.fanart.jpg` / `-backdrop` / `.backdrop` / `-thumb`
3. 同目录下的 `fanart.jpg`

都没有时，详情页回落到竖版海报。剧集（如「生命奇观」）取该季第一集的 fanart。

图片仅用于本机演示片库的界面效果，版权归原权利人所有。
