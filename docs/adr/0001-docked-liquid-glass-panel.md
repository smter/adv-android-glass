# ADR-0001: Rebuild the docked glass panel on a third-party liquid-glass library

- **Status:** Accepted
- **Date:** 2026-10-30
- **Deciders:** adv-android-glass maintainers
- **Supersedes:** none (first decision recorded for the glass rendering area)
- **Related:** issue #1 (spec), sub-issues #2 / #3 / #4

## Context

The demo's hand-rolled liquid-glass implementation failed to converge across several
repair rounds and never looked like liquid glass:

- The AGSL backend **crashed on launch** (the process exited and only the home screen
  remained).
- The CPU backend ran, but the measured refraction displacement of the whole panel was
  **about 1 px** — the background inside and outside the pane lined up almost
  pixel-for-pixel. Physically the pane refracted nothing; it was a stroked transparent
  rectangle.
- The panel's **top edge was a hard horizontal cut**, so a visible seam and
  misalignment appeared between the pane and the wallpaper above it (see
  `s1.png`, `emu_03_lens_cpu_70.png`).
- The "prism DIAMOND" variant's constant five-face offset was a pseudo-optical
  approximation; visually it read as a purple arc.

The user's only real requirement: **a demo that looks like Apple's Liquid Glass.** The
implementation details and engineering effort matter far less than the final look. The
previous attempts — porting `shuding/liquid-glass`'s SVG displacement filter to AGSL and
a Kotlin geometry port — were abandoned once it was clear they would not converge.

The correct optical model was already identified in diagnosis: rounded-rectangle SDF with
a `circleMap` spherical refraction displacing along the contour normal, where the interior
region (`-sd >= refractionHeight`) returns the original pixel unchanged, so the inside and
outside of the pane align with no seam. Per-corner independent radii (`float4 cornerRadii`)
allow a bottom-docked pane whose bottom corners differ from its top corners.

## Decision

### Delegate the optics to `io.github.kyant0:backdrop`

Stop implementing refraction ourselves. Use the mature third-party library
[`io.github.kyant0:backdrop`](https://github.com/Kyant0/AndroidLiquidGlass)
(Apache-2.0). It already ships the correct shader model (rounded-rect SDF +
`circleMap` refraction along the contour normal, interior passthrough) plus per-corner
radii, and provides `Lens` (refraction), `Highlight`, `InnerShadow`, `Blur` and
`ColorFilter` — covering the remaining glass finish that the old `GlassFinish` hand-rolled
fallbacks used to approximate.

**Version `1.0.6` is pinned, required, and verified by building the project.**

- `2.0.1` transitively pulls Compose `1.12.0`, which demands `compileSdk 37` and AGP
  `9.1.0`. This project runs `compileSdk 36` with AGP `9.0.1` (see
  [`libs.versions.toml`](../app/gradle/libs.versions.toml)), so the newer line fails the
  build.
- `1.0.6` resolves against the existing Compose BOM `2026.03.01` and builds clean.
- `1.0.6` exposes the same
  `lens(refractionHeight, refractionAmount, depthEffect, chromaticAberration)` and
  `rememberLayerBackdrop()` surface as `2.0.1` (verified by inspecting the artifact), so
  no capability is lost by pinning.

Upgrading to the `2.0.x` line is **out of scope**; it requires an AGP + `compileSdk 37`
migration first.

### Shape decision: docked bottom panel

The panel is pinned to the bottom edge of the screen, inset horizontally, with **rounded
top corners and square (zero-radius) bottom corners**. The bottom edge reaches the screen
edge, so the two bottom corners take no normal radius — they are set to `0` so the pane
reads as running off-screen. This is what produces the docked system-sheet silhouette
(30% / 70% anchors).

This shape decision is what `GlassLookMath`'s **corner-radii computation** encodes, and it
is asserted by a unit test: the top pair is rounded, the bottom pair is zero, and the
radii never exceed the panel's half-min-dimension.

### Delete the hand-rolled glass modules

Every module that the library subsumes is removed outright, with no compatibility layer
and nothing left dormant. Each was an unfinished abstraction whose complexity did not need
to reappear elsewhere:

- the hand-written refraction stack — the geometry maths, the AGSL programs, the AGSL
  painter, and the CPU reference renderer (`LensGeometry`, `LiquidGlassShaders`,
  `AgslGlassPainter`, `LiquidGlassRenderer`);
- the faceted "diamond" optics approximation;
- the normalised upstream geometry port (already dead code);
- the finish / colour-matrix / crop-scratch helpers (`ColorMatrix`);
- the panel band composable (`GlassBand`);
- the debug trace helper.

**Retained** (genuine deep modules — deleting them would scatter logic across call sites):

- the panel height state (30% / 70% anchors, spring settle, velocity-aware nearest-anchor
  choice) — `ui/panel/PanelHeightState.kt`;
- the reading panel's scroll + overscroll → height gesture wiring —
  `ui/panel/ReadingPanel.kt`;
- the wallpaper repository (built-in art + picker + persistence) —
  `wallpaper/WallpaperRepository.kt`;
- the debug launch-extras holder that drives the screenshot regression loop —
  `ui/LaunchState.kt`.

### New deep module: the glass panel

A single composable — `glass/GlassPanel.kt` — accepts the backdrop, the height state and
the look; everything else is internal. It **accepts** the layer backdrop rather than
creating it, and **returns** nothing — it draws. It owns, internally:

- assembly of the library's lens, highlight, inner shadow, blur and colour filter;
- the layer-backdrop wiring and shape;
- the API-gate fallback path;
- the conversion from the look's density-independent values into the pixel units the
  library's shader expects.

A small immutable value module — `glass/GlassLook.kt` — describes appearance only
(corner radius, refraction height/amount, tint, blur radius, chromatic-dispersion toggle).
It exposes exactly two computations and nothing else: **corner radii for a given panel
size** (the docked-bottom rule) and **lens parameters for a given density** (the one
dp→px conversion, with a minimum enforced so a too-small value cannot silently produce a
flat pane). See "Testing seam" below.

### Backdrop capture via `LayerBackdrop`

The wallpaper is drawn once, full-bleed, inside a `GraphicsLayer`; the glass panel reads
that layer as its backdrop (`rememberLayerBackdrop` / `LayerBackdrop`), which snapshots the
layer every frame. There is **no manual cropping, no translated copy, no cached band**.
This is what guarantees the refracted content follows the panel as it moves and resizes,
so the refraction band never shows a stale backdrop.

### Three-tier degradation ladder

The API gate lives **inside the glass panel module**, not at call sites:

- **API 33+** — full AGSL refraction (the library's lens shader).
- **API 31–32** — no runtime shader; the panel degrades to **blur + tint + highlight** via
  render effects.
- **API 26–30** — no render effects either; the panel degrades to an **opaque tinted
  surface** with a highlight and an inner shadow.

The "engine" launch extra selects between the full-refraction path and the forced fallback
path, so the fallback can be exercised on a capable device.

### Testing seam: `GlassLookMath` as a pure JVM module

The glass look module is a pure value-to-value module with no Compose, no Android and no
I/O, so it runs as plain JVM JUnit on `src/test/kotlin` with no device or emulator
(`junit:junit:4.13.2` is already declared as `testImplementation`).

- **Glass look → corner radii** — asserts the docked silhouette: top pair rounded, bottom
  pair zero, radii clamped to the half-min-dimension.
- **Glass look → lens parameters** — asserts refraction height and amount are in pixel
  units, scale with density, comfortably exceed the "effectively zero" floor the previous
  implementation fell under, and that a nonsensical look (zero or negative height/amount)
  is clamped rather than emitting a flat pane.

This is the highest available seam: the refractive geometry and pixel displacement maths
now live inside the third-party library, and testing those would test someone else's
implementation rather than ours. Visual acceptance stays **manual** (see
[`docs/manual-acceptance-glass.md`](../manual-acceptance-glass.md)) — the emulator's GPU
path is not a faithful oracle.

## Consequences

**Positive**

- The panel visibly bends the wallpaper at its rim, and the interior lines up
  pixel-perfectly with the wallpaper outside it — no seam, no offset copy.
- The panel is docked to the bottom edge with a symmetric, intentional silhouette.
- The correct optical model is maintained upstream instead of by us.
- Deleting the old modules removes the temptation for the next person to patch the wrong
  thing.
- The dp→px conversion and docked-corner rule are covered by fast JVM tests, so the
  "7 dp became 16 px and vanished" class of bug is caught by a test, not by an eye.

**Negative / trade-offs**

- We depend on a third-party library's public surface and pinned version.
- Staying on `1.0.6` defers the AGP 9.1 / `compileSdk 37` migration required by `2.0.x`.
- Visual quality is accepted manually, not by an automated image-diff gate.

**Neutral**

- The library is Apache-2.0; the third-party notices file must gain an entry for it. The
  project's own MIT claim is unaffected.

## Alternatives considered

- **Keep repairing the hand-rolled implementation.** Rejected: it shipped "effectively
  zero displacement" as "CPU backend fully correct", modelled the panel as a hard-cut
  rectangular banner (making any rim displacement produce a seam by construction), and a
  compiler scoping failure was chased with probe shaders. It does not converge.
- **Port the upstream pyramid ray tracer for the "diamond" variant.** Rejected: the
  library offers chromatic dispersion as a toggle; a faithful ray-tracer port is out of
  scope.
- **Upgrade to `2.0.x` immediately.** Rejected for now: requires AGP 9.1 + `compileSdk 37`
  migration; `1.0.6` provides the same optics API.
- **Keep a CPU reference renderer for tests.** Rejected: it was the least reliable part of
  the old build, blocked the screenshot loop with its slowness, and the library has no CPU
  path. The pure-JVM `GlassLookMath` seam replaces it.

## Notes for the implementer

- Working directory: the repo root; Gradle root is `app/`.
- Build: set `JAVA_HOME` to the Android Studio JBR (JDK 21), then run the Gradle wrapper's
  `assembleDebug --console=plain` from `app/`.
- Emulator: a `medium_phone` AVD (API 36) uses the host GPU, so AGSL renders faithfully
  there.
- When a build looks up-to-date but the APK did not change, suspect Kotlin incremental
  compilation over constants and run a clean build.
