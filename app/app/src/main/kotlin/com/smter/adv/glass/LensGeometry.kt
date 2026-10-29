/*
 * Kotlin port of the geometry used by https://github.com/shuding/liquid-glass (MIT, (c) 2025 Shu Ding).
 * See THIRD_PARTY_NOTICES.md for the upstream licence text.
 */
package com.smter.adv.glass

/** Tunables that describe the pill the glass sits on, expressed in shares of the panel box. */
data class LensShape(
    /** Half-extent in normalised u (0..1) space. Upstream authored 0.3 for a 300px wide box. */
    val halfWidthU: Float = UPSTREAM_HALF_WIDTH_U,
    /** Half-extent in normalised v space; scaled by the panel aspect so px/px stays square. */
    val halfHeightU: Float = UPSTREAM_HALF_HEIGHT_U,
    /** Corner radius of the super-ellipse, in normalised u space. */
    val cornerRadiusU: Float = UPSTREAM_CORNER_RADIUS_U,
    /** Feathering subtracted from the SDF before the double smooth-step (upstream 0.15). */
    val featherU: Float = UPSTREAM_FEATHER_U,
    /** Edge of the bend ramp used by the second smooth-step (upstream 0.8). */
    val rampU: Float = UPSTREAM_RAMP_U,
) {
    companion object {
        // Exact constants from liquid-glass.js (Shader fragment for the 300x200 pill).
        const val UPSTREAM_HALF_WIDTH_U = 0.3f
        const val UPSTREAM_HALF_HEIGHT_U = 0.2f
        const val UPSTREAM_CORNER_RADIUS_U = 0.6f
        const val UPSTREAM_FEATHER_U = 0.15f
        const val UPSTREAM_RAMP_U = 0.8f

        /** The upstream recipe, scaled anisotropically by the panel aspect. */
        fun scaledToAspect(aspect: Float): LensShape = LensShape(
            halfWidthU = UPSTREAM_HALF_WIDTH_U,
            halfHeightU = UPSTREAM_HALF_HEIGHT_U * aspect,
            cornerRadiusU = UPSTREAM_CORNER_RADIUS_U,
            featherU = UPSTREAM_FEATHER_U,
            rampU = UPSTREAM_RAMP_U,
        )
    }
}

object LensGeometry {
    fun smoothStep(a: Float, b: Float, t: Float): Float {
        val x = ((t - a) / (b - a)).coerceIn(0f, 1f)
        return x * x * (3f - 2f * x)
    }

    /** Rounded-rectangle signed distance in normalised space, as implemented upstream. */
    fun roundedRectSdf(x: Float, y: Float, halfWidth: Float, halfHeight: Float, radius: Float): Float {
        val qx = kotlin.math.abs(x) - halfWidth + radius
        val qy = kotlin.math.abs(y) - halfHeight + radius
        val mx = if (qx > 0f) qx else 0f
        val my = if (qy > 0f) qy else 0f
        return minOf(kotlin.math.max(qx, qy), 0f) + kotlin.math.sqrt(mx * mx + my * my) - radius
    }

    /**
     * Displacement factor at a normalised point, exactly following the upstream fragment:
     * double smooth-step on (d - feather) then remap to [0,1]. 0 = fully displaced to the centre,
     * 1 = untouched.
     */
    fun scaleAt(u: Float, v: Float, shape: LensShape): Float {
        val ix = u - 0.5f
        val iy = v - 0.5f
        val d = roundedRectSdf(ix, iy, shape.halfWidthU, shape.halfHeightU, shape.cornerRadiusU)
        return smoothStep(0f, 1f, smoothStep(shape.rampU, 0f, d - shape.featherU))
    }

    /**
     * How far (in unit-space) the sample is pulled toward the centre: 1 - scale. The browser turns
     * this into pixels through feDisplacementMap's scale attribute.
     */
    fun displacementAt(u: Float, v: Float, shape: LensShape): Float = 1f - scaleAt(u, v, shape)

    /** Largest displacement magnitude on a grid, used as the displacement-map normalisation. */
    fun maxDisplacement(shape: LensShape, steps: Int = 64): Float {
        var max = 0f
        for (j in 0..steps) for (i in 0..steps) {
            val d = displacementAt(i.toFloat() / steps, j.toFloat() / steps, shape)
            if (d > max) max = d
        }
        return if (max > 0f) max else 1f
    }
}
