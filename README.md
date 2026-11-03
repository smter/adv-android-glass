# Adv — 液态玻璃沉浸阅读面板（Android）

包名 `com.smter.adv`。一张壁纸 + 底部一片"液态玻璃"面板：面板初始占屏幕底部 **30%**，
上滑展开到 **70%** 开始阅读，在正文顶部下拉收回；点面板在 30% ↔ 70% 之间切换，点面板外收回。

玻璃是一块**贴着屏幕底边**的圆角面板：顶边两角圆角、底边直接跑出屏幕，边缘有可见的向内弯曲
的折射带，面板内部与紧邻其外的壁纸逐像素对齐，并带顶部高光、内阴影、模糊与色调。

光学实现来自 [Kyant0/AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass)
（`io.github.kyant0:backdrop` 1.0.6，Apache-2.0）：库提供圆角矩形 SDF 折射、色散、高光、
内阴影与模糊；本仓库只负责贴底轮廓、外观参数与平台降级。第三方声明见
[app/THIRD_PARTY_NOTICES.md](app/THIRD_PARTY_NOTICES.md)。

| 变体 | 控制条 | 效果 |
| --- | --- | --- |
| 透镜 `lens` | 透镜 | 纯净折射 |
| 色散 `dispersion` | 色散 | 打开库的色差开关，边缘出现彩色色散 |

渲染按平台能力分三档，判定收在玻璃模块内部（`GlassEngine`），调用点不判断系统版本：

| 档位 | 环境 | 表现 |
| --- | --- | --- |
| `full` | API 33+ | 运行时着色器折射 + 模糊 + 高光 + 内阴影 |
| `blur` | API 31–32 | 无运行时着色器：模糊 + 加重色调 + 高光 + 内阴影 |
| `opaque` | API 26–30 | 无渲染特效：不透明有色面板 + 自绘高光与内阴影 |

## 运行

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
cd app
.\gradlew assembleDebug          # 或 android build
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb shell am start -n com.smter.adv/.MainActivity
```

需要 JDK 17+（本机 `JAVA_HOME` 指向 JDK 8，需显式覆盖）。

## 截图回归循环

启动参数把面板锁进已知状态，无需点击 UI 即可截图（带任一参数即视为调试启动，控制条隐藏，
保证画面里只有 demo 本身）：

| 参数 | 取值 | 作用 |
| --- | --- | --- |
| `panel` | 比例，如 `0.7` / `0.3` | 锁定面板高度 |
| `variant` | `lens` / `dispersion` | 选择折射变体（`diamond` / `prism` 也映射到色散） |
| `engine` | `full` / `blur` / `opaque` | 强制渲染档位；`fallback` 是 `blur` 的别名 |

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
cd app
.\gradlew assembleDebug --console=plain
adb install -r app\build\outputs\apk\debug\app-debug.apk

adb shell am force-stop com.smter.adv
adb shell am start -n com.smter.adv/.MainActivity --es panel 0.7 --es variant lens
Start-Sleep -Seconds 8   # 首帧偏慢：背景层每帧抓取，冷启动要等首帧画出来
adb shell screencap -p /sdcard/adv-shot.png
adb pull /sdcard/adv-shot.png emu_lens_70.png
# 用 `adb exec-out screencap -p > x.png` 也可以，但仅限 PowerShell 7；Windows PowerShell
# 5.1 的 `>` 会按文本重新编码，PNG 会被写坏。
```

一条流程的便捷入口：

```powershell
powershell -File scripts/screenshot.ps1 -State 70lens
powershell -File scripts/screenshot.ps1 -State 70opaque -SkipBuild
```

可用状态：`30lens`、`70lens`、`70dispersion`、`70blur`、`70opaque`。完整验收流程见
[docs/manual-acceptance-glass.md](docs/manual-acceptance-glass.md)。

## 代码结构

```
app/app/src/main/kotlin/com/smter/adv/
├── MainActivity.kt
├── glass/
│   ├── GlassEngine.kt         # 33+ / 31–32 / 26–30 三档渲染等级与强制覆盖
│   ├── GlassLook.kt           # 纯外观规格；唯一的 dp→px 换算与钳制
│   └── GlassPanel.kt          # 深模块：装配折射/高光/内阴影/模糊/色调，含环境判定
├── ui/
│   ├── DemoScreen.kt          # 主界面 + 控制条（变体/折射强度/换图/还原/收纳）
│   ├── LaunchState.kt         # 截图回归用的启动参数
│   ├── panel/PanelHeightState.kt   # 30% ↔ 70% 双锚点、手势归属与弹簧回落
│   ├── panel/ReadingPanel.kt       # 宋体正文与嵌套滚动联动
│   └── theme/
├── wallpaper/WallpaperRepository.kt # 内置壁纸 + 图片选择器（落盘持久化）
└── content/ReadingSample.kt   # 演示文本（原创）
```

## 测试

纯 JVM 单元测试，不需要设备或模拟器：

```powershell
cd app
.\gradlew testDebugUnitTest
```

- `GlassLookTest` / `GlassLookKnobTest`：贴底轮廓、四角半径上限、dp→px 与密度、强度阶梯
- `GlassEngineTest`：三档判定、强制覆盖与回退档的色调下限
- `PanelHeightStateTest`：锚点夹取、最近锚点、手势归属（上滑展开 / 顶部下拉收回）

## 已知问题

- 31–32 与 26–30 两档是用 `--es engine` 在 API 36 模拟器上强制触发验证的，尚无真实的低版本
  设备实测。
- 折射带在低细节壁纸上不明显；用高对比图片（控制条"换图"）最容易看清。

## 字体

字体文件不打包：正文声明 `FontFamily.Serif`，繁体/简体中文由系统回退到
`NotoSerifCJK`（AOSP `fonts.xml` 中 `fallbackFor="serif"` 条目）。

## 许可

本项目代码 MIT；第三方组件与参考来源见 [app/THIRD_PARTY_NOTICES.md](app/THIRD_PARTY_NOTICES.md)。
