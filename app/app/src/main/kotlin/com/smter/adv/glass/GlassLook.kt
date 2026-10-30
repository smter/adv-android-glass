package com.smter.adv.glass

/**
 * Appearance specification of the docked glass panel.
 *
 * Every knob is a plain density-independent number: no Compose, no Android, no I/O. [GlassLookMath]
 * is the only place that turns these into the pixel values the library's shaders consume, so the
 * "dp became px and vanished" class of bug is caught by a JVM test instead of by an eye.
 */
data class GlassLook(
    val variant: GlassVariant = GlassVariant.LENS,
    /** Radius of the two top corners in dp. The bottom corners stay square: the pane runs off-screen. */
    val cornerRadiusDp: Float = 28f,
    /** Width of the refracting band along the rim, in dp. */
    val refractionHeightDp: Float = 30f,
    /** How far the rim pulls the background towards the edge, in dp. */
    val refractionAmountDp: Float = 18f,
    /** Demo strength knob, multiplied onto both refraction values. */
    val strengthScale: Float = 1f,
    /** Soft blur under the glass body, in dp. */
    val blurRadiusDp: Float = 16f,
    /** Milky white tint laid over the refracted background. */
    val tintAlpha: Float = 0.10f,
    /** Height of the bottom scrim that keeps the last lines readable, in dp. */
    val scrimHeightDp: Float = 160f,
    val scrimAlpha: Float = 0.55f,
    /** Width of the top highlight band, in dp. */
    val highlightWidthDp: Float = 1f,
    val highlightAlpha: Float = 0.7f,
    /** Rim inner shadow, in dp. */
    val innerShadowRadiusDp: Float = 24f,
    val innerShadowAlpha: Float = 0.55f,
)

/** What the rim does optically. Dispersion maps to the library's chromatic aberration toggle. */
enum class GlassVariant { LENS, DISPERSION }

/** Corner radii in pixels, in [topStart], [topEnd], [bottomStart], [bottomEnd] order. */
data class CornerRadiiPx(
    val topStart: Float,
    val topEnd: Float,
    val bottomStart: Float,
    val bottomEnd: Float,
)

/** Everything the panel hands to the shaders, in pixels. */
data class GlassPanelPixels(
    /** The visible pane silhouette: rounded on top, square at the bottom edge. */
    val cornerRadii: CornerRadiiPx,
    val refractionHeightPx: Float,
    val refractionAmountPx: Float,
    /**
     * Corner radii of the rounded rect the rim lens refracts around, in px. This must equal
     * [cornerRadii]: the shader derives its SDF from this shape, so any mismatch between the lens
     * shape and the visible pane pushes the refraction band off the edge and flattens the sides.
     */
    val lensCornerRadii: CornerRadiiPx,
    val blurRadiusPx: Float,
    val scrimHeightPx: Float,
)

object GlassLookMath {

    /**
     * The one density → pixel conversion in the app. Clamps rather than trusts: a nonsense look still
     * comes out as a pane that pulls the rim, never as a flat rectangle.
     */
    fun pixels(look: GlassLook, widthPx: Float, heightPx: Float, density: Float): GlassPanelPixels {
        val shortSide = minOf(widthPx, heightPx).coerceAtLeast(0f)
        val cap = shortSide / 2f
        val topRadius = (look.cornerRadiusDp * density).coerceIn(0f, cap)
        val strength = clampStrength(look.strengthScale)
        val refractionHeight = (look.refractionHeightDp * strength * density)
            .coerceAtLeast(MIN_HEIGHT_PX)
        val refractionAmount = (look.refractionAmountDp * strength * density)
            .coerceAtLeast(MIN_AMOUNT_PX)
        // The lens refracts around the very silhouette the pane draws: rounded on top, square at the
        // bottom edge. One corner set, shared by both, so the band can never drift off the edge.
        val silhouette = CornerRadiiPx(
            topStart = topRadius,
            topEnd = topRadius,
            bottomStart = 0f,
            bottomEnd = 0f,
        )
        return GlassPanelPixels(
            cornerRadii = silhouette,
            refractionHeightPx = refractionHeight,
            refractionAmountPx = refractionAmount,
            lensCornerRadii = silhouette,
            blurRadiusPx = (look.blurRadiusDp * density).coerceAtLeast(0f),
            scrimHeightPx = (look.scrimHeightDp * density).coerceAtLeast(0f),
        )
    }

    /**
     * Stronger: one step up the demo's strength ladder, saturated at [MAX_STRENGTH]. Weaker: one step
     * down, never below [MIN_STRENGTH]. The knob's own limits live here so the UI only names intent.
     */
    fun GlassLook.stronger() = copy(strengthScale = (strengthScale + STRENGTH_STEP).coerceAtMost(MAX_STRENGTH))

    fun GlassLook.weaker() = copy(strengthScale = (strengthScale - STRENGTH_STEP).coerceAtLeast(MIN_STRENGTH))

    private fun clampStrength(strength: Float) = strength.coerceIn(MIN_STRENGTH, MAX_STRENGTH)

    /** Below this the demo knob stops shrinking the effect into a flat pane. */
    const val MIN_STRENGTH = 0.40f
    const val MAX_STRENGTH = 2.5f
    const val STRENGTH_STEP = 0.25f
    const val MIN_HEIGHT_PX = 8f
    const val MIN_AMOUNT_PX = 6f
}

/** Exact pixel geometry of a panel, mirroring [GlassLookMath.pixels] where Compose needs dp again. */
data class GlassPanelGeometry(
    val cornerRadiusDp: Float,
    val scrimHeightPx: Float,
    val tintAlpha: Float,
    val scrimAlpha: Float,
    val highlightWidthDp: Float,
    val highlightAlpha: Float,
    val innerShadowRadiusDp: Float,
    val innerShadowAlpha: Float,
)

fun GlassLookMath.geometry(look: GlassLook, widthPx: Float, heightPx: Float, density: Float): GlassPanelGeometry {
    val px = pixels(look, widthPx, heightPx, density)
    return GlassPanelGeometry(
        cornerRadiusDp = if (density > 0f) px.cornerRadii.topStart / density else 0f,
        scrimHeightPx = px.scrimHeightPx,
        tintAlpha = look.tintAlpha,
        scrimAlpha = look.scrimAlpha,
        highlightWidthDp = look.highlightWidthDp,
        highlightAlpha = look.highlightAlpha,
        innerShadowRadiusDp = look.innerShadowRadiusDp,
        innerShadowAlpha = look.innerShadowAlpha,
    )
}