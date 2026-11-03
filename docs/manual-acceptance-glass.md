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
| `engine`   | `full` / `blur` / `opaque`           | Forces a rendering tier: `blur` is the API 31–32 ladder (blur + tint + rim), `opaque` the API 26–30 one (opaque tinted pane). `fallback` is an alias for `blur`. Either fallback can be captured on a capable device. |

When any of these extras is present the launch is treated as a debug launch and the
on-screen control strip is hidden, so the captured frame shows only the demo.

Examples:

```powershell
# 70% panel, plain lens
adb shell am start -n com.smter.adv/.MainActivity --es panel 0.7 --es variant lens

# 70% panel, dispersion variant
adb shell am start -n com.smter.adv/.MainActivity --es panel 0.7 --es variant dispersion

# Force each fallback tier on a capable device
adb shell am start -n com.smter.adv/.MainActivity --es panel 0.7 --es engine blur
adb shell am start -n com.smter.adv/.MainActivity --es panel 0.7 --es engine opaque
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
# Capture on the device and pull: PowerShell 5.1's ">" re-encodes the stream as text and
# corrupts the PNG (PowerShell 7's "adb exec-out ... > x.png" is fine).
adb shell screencap -p /sdcard/adv-shot.png
adb pull /sdcard/adv-shot.png "$state.png"
```

Repeat with the extras you need for each criterion (`--es panel 0.3`, `--es variant
dispersion`, `--es engine blur`, `--es engine opaque`). Capture two frames for the resize criteria — one at
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
9. **Fallbacks are coherent.** Forcing either fallback tier (`--es engine blur` / `--es engine opaque`) renders a
   coherent tinted panel — not a blank or black region.

## Recording the result

For each checklist run, keep the captured PNG(s) and note pass/fail per item against the
extras used. A regression is any item that passed on a prior run and no longer does; attach
the two frames (before/after) when reporting it.

## Last run — 2026-11-03

Environment: `medium_phone` AVD (API 36, host GPU, AGSL path), `debug` variant, bundled
wallpaper plus a high-contrast checkerboard image pushed through the photo picker so the
refraction band is unambiguous.

| # | Item | Result | Evidence |
| - | ---- | ------ | -------- |
| 1 | Inward-bending rim band | ✅ pass | [rim-refraction.jpg](images/acceptance/rim-refraction.jpg) |
| 2 | Interior vs. outside aligned | ✅ pass — no seam, no offset copy | [70-lens.jpg](images/acceptance/70-lens.jpg) |
| 3 | Docked silhouette, rounded top | ✅ pass — panel top at y=720 of 2400 (70%) | [70-lens.jpg](images/acceptance/70-lens.jpg), [30-lens.jpg](images/acceptance/30-lens.jpg) |
| 4 | Highlight and inner shadow | ✅ pass | [rim-refraction.jpg](images/acceptance/rim-refraction.jpg) |
| 5 | Band tracks the rim while resizing | ✅ pass — three frames of one 30% → 70% drag | [resize-tracking.jpg](images/acceptance/resize-tracking.jpg) |
| 6 | Text legibility | ✅ pass | [70-lens.jpg](images/acceptance/70-lens.jpg) |
| 7 | Interactions fire exactly once | ✅ pass — title y: 861 → 1821 (pull down) → 861 (tap panel) → 1821 (tap outside) | measured with `android layout` |
| 8 | No launch crash (AGSL path) | ✅ pass — every pinned launch kept its process alive | — |
| 9 | Fallbacks coherent | ✅ pass — both forced tiers render tinted panes, not blank/black | [70-blur.jpg](images/acceptance/70-blur.jpg), [70-opaque.jpg](images/acceptance/70-opaque.jpg) |

Supporting captures: [variant-compare.jpg](images/acceptance/variant-compare.jpg) (lens vs.
dispersion — colour fringing at the rim, height unchanged) and
[wallpaper-checkerboard.jpg](images/acceptance/wallpaper-checkerboard.jpg) (glass re-refracts a
new wallpaper).

### Fixed during this run

Two interaction defects surfaced while walking items 5 and 7 and were fixed in the same
change:

- The tap-to-toggle used a full-size overlay above the article. It shadowed the scrollable, so
  the article never scrolled and every swipe was read as a tap, toggling the sheet. The tap now
  belongs to the container.
- The drag mapping contradicted the documented gesture: dragging up *shrank* the sheet. The
  sheet now grows on an upward drag until it is expanded, the article then scrolls normally,
  and pulling down at the article's very top collapses it again.

### Not covered by this run

- The API 31–32 and API 26–30 tiers were reached by forcing `engine`, not by running on a real
  device of either version. Their rendering is verified; their platform behaviour (no runtime
  shader / no render effects at all) is not.
- Motion was sampled at discrete frames, not recorded: "tracks the rim" is judged from three
  frames of one gesture.

