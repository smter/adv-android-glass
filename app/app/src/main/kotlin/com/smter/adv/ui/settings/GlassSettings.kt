package com.smter.adv.ui.settings

import androidx.compose.runtime.Immutable
import com.smter.adv.glass.GlassVariant

/** Which runtime the glass pass runs on. AGSL needs API 33 and is the default on capable devices. */
enum class GlassEngine { AGSL, CPU }

/** User-facing knobs exposed in the demo bar. */
@Immutable
data class GlassSettings(
    val variant: GlassVariant = GlassVariant.LENS,
    val engine: GlassEngine = GlassEngine.AGSL,
    /** Panel tint alpha, 0f fully clear → 0.35f milky. */
    val tintAlpha: Float = 0.10f,
    /** Rim strength in px, mapped onto the upstream feDisplacementMap scale. */
    val displacementDp: Float = 10f,
)
