# Manual acceptance: the liquid-glass look

The glass optics now live inside the third-party library
([`io.github.kyant0:backdrop`](https://github.com/Kyant0/AndroidLiquidGlass), pinned
`1.0.6`; see [ADR-0001](adr/0001-docked-liquid-glass-panel.md)). Re-testing the shader's
output pixel-by-pixel would couple us to rendering we do not own, and the emulator's GPU
path is not a faithful oracle. **Visual acceptance is therefore manual**, judged against
the explicit criteria below rather than by vibes.

Screenshot on a capable device or emulator (AGSL path, API 33+) with the state pinned by
launcher extras, then walk the checklist.

## Prerequisites

- A device / emulator on **API 33+** with a working GPU path — the `medium_phone` AVD
  (API 36) uses the host GPU and renders AGSL faithfully.
- A build of the `debug` variant installed.
- The built-in wallpaper (or a known-good wallpaper) so criteria are repeatable.

## Forcing a known state with launcher extras

The demo reads these **debug-only** launch extras (see
[`LaunchState.kt`](../app/app/src/main/kotlin/com/smter/adv/ui/LaunchState.kt)). Pass them
with `--es` (extra **string**); `am start` re-reads them once per launch and keeps them
for the life of that launch:

| Extra      | Values                              | Effect                                                                 |
| ---------- | ----------------------------------- | ---------------------------------------------------------------------- |
| `panel`    | a fraction, e.g. `0.7` or `0.3`      | Pins the panel height. `0.7` = expanded (70%), `0.3` = collapsed (30%). |
| `variant`  | `lens` or `dispersion`               | `lens` = plain refraction; `dispersion` = chromatic-dispersion variant. (`diamond` / `prism` also map to `dispersion`.) |
| `engine`   | `full` or `fallback`                 | Selects the full-refraction path or the forced fallback path, so the fallback can be exercised on a capable device. |

When any of these extras is present the launch is treated as a debug launch and the
on-screen control strip is hidden, so the captured frame shows only the demo.

Examples:

```powershell
# 70% panel, plain lens
adb shell am start -n com.smter.adv/.MainActivity --es panel 0.7 --es variant lens

# 70% panel, dispersion variant
adb shell am start -n com.smter.adv/.MainActivity --es panel 0.7 --es variant dispersion

# Force the fallback path on a capable device
adb shell am start -n com.smter.adv/.MainActivity --es panel 0.7 --es engine fallback
```

## Screenshot loop

Install, force-stop, start with the extras that pin the state, wait a moment for the first
frame (the backdrop is captured per-frame), then screencap:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
cd app
.\gradlew assembleDebug --console=plain
adb install -r app\build\outputs\apk\debug\app-debug.apk

$state = 'emu_lens_70'
adb shell am force-stop com.smter.adv
adb shell am start -n com.smter.adv/.MainActivity --es panel 0.7 --es variant lens
Start-Sleep -Seconds 3
adb exec-out screencap -p > "$state.png"
```

Repeat with the extras you need for each criterion (`--es panel 0.3`, `--es variant
dispersion`, `--es engine fallback`). Capture two frames for the resize criteria — one at
`0.3` and one at `0.7` — or record a screen capture of the drag.

If a build looks up-to-date but the APK did not change, suspect Kotlin incremental
compilation over constants and run a clean build before re-capturing.

## The checklist

Pinned state noted per item. Tick each only when the captured frame / observed behaviour
clearly satisfies it.

1. **Inward-bending rim band (70%).** At `--es panel 0.7`, the rim shows a visible,
   *inward-bending* refraction band — not a flat edge.
2. **Pixel alignment inside vs. outside (70%).** At `--es panel 0.7`, sampling just inside
   the rim against just outside it shows *displaced* content; sampling the panel's
   *interior* against the wallpaper just outside shows *identical* content (no seam, no
   offset copy of the background).
3. **Docked silhouette.** The panel touches the bottom screen edge; its top corners are
   rounded; the silhouette is symmetric.
4. **Highlight and inner shadow.** A top highlight and a rim inner shadow are both visible.
5. **Band tracks the rim while resizing.** Dragging from 30% → 70% keeps the refraction
   band attached to the moving rim (no snap, no stale band).
6. **Text legibility.** Body text is readable over the busiest region of the built-in
   wallpaper.
7. **Interactions fire exactly once.** Tapping the panel, tapping outside the panel, and
   overscroll each change the height exactly once per action.
8. **No launch crash (AGSL path).** The app does not crash on launch in the AGSL
   (full-refraction) path.
9. **Fallback is coherent.** Forcing the fallback path (`--es engine fallback`) renders a
   coherent tinted panel — not a blank or black region.

## Recording the result

For each checklist run, keep the captured PNG(s) and note pass/fail per item against the
extras used. A regression is any item that passed on a prior run and no longer does; attach
the two frames (before/after) when reporting it.
