# Adv — 液态玻璃沉浸阅读面板（Android）

包名 `com.smter.adv`。一张壁纸 + 底部一片"液态玻璃"面板：面板初始占屏幕底部 **30%**，
上滑阅读时逐级上浮到 **70%**，点面板展开、点外部或在列表顶部下滑收回。

玻璃效果是把 [shuding/liquid-glass](https://github.com/shuding/liquid-glass) 的 SVG
位移滤镜技术移植到 Android（AGSL + Kotlin 几何移植），两种变体可实时切换：

| 变体 | 来源 | 当前实现 |
| --- | --- | --- |
| 透镜 LENS | `liquid-glass.js` | SDF + 双 smoothStep 位移场 + CSS 同款 contrast/brightness/saturate 链 |
| 棱镜 DIAMOND | `liquid-diamond.js` | 解析式棱面折射近似（非原版光线追踪），保留青辉与棱线 |

渲染后端也能切换：**AGSL**（API 33+，GPU 内求值）与 **CPU**（位图位移，兼容 API 26–32）。

## 运行

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
cd app
.\gradlew assembleDebug          # 或 android build
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb shell am start -n com.smter.adv/.MainActivity
```

需要 JDK 17+（本机 `JAVA_HOME` 指向 JDK 8，需显式覆盖）。

## 代码结构

```
app/app/src/main/kotlin/com/smter/adv/
├── MainActivity.kt
├── glass/
│   ├── LensGeometry.kt        # liquid-glass.js 的 roundedRectSDF/smoothStep 逐行移植
│   ├── LiquidGlassShaders.kt  # AGSL 程序（透镜 / 棱镜）
│   ├── AgslGlassPainter.kt    # AGSL 绘制封装（BitmapShader 作为采样输入）
│   ├── LiquidGlassRenderer.kt # CPU 位移渲染（无 Compose 依赖，可单测）
│   ├── GlassBand.kt           # Compose 侧的光带绘制
│   └── ColorMatrix.kt         # CSS 滤镜链等价矩阵
├── ui/
│   ├── DemoScreen.kt          # 主界面 + 顶部调试条（变体/后端/换图/还原/收纳）
│   ├── panel/PanelHeightState.kt   # 30% ↔ 70% 双锚点 + 弹簧回落
│   └── panel/ReadingPanel.kt       # 宋体正文与嵌套滚动联动
├── wallpaper/WallpaperRepository.kt # 内置壁纸 + 图片选择器（落盘持久化）
└── content/ReadingSample.kt   # 演示文本（原创）
```

## 字体

字体文件不打包：正文声明 `FontFamily.Serif`，繁体/简体中文由系统回退到
`NotoSerifCJK`（AOSP `fonts.xml` 中 `fallbackFor="serif"` 条目）。

## 已知问题

见 [Issues](https://github.com/smter/adv-android-glass/issues)。面板玻璃目前渲染不对（边缘出现
缸形/模糊叠影），详见 issue 说明。

## 许可

本项目代码 MIT；移植来源与第三方声明见 [app/THIRD_PARTY_NOTICES.md](app/THIRD_PARTY_NOTICES.md)。
