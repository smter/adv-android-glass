package com.smter.adv.glass

import android.graphics.ColorMatrix

/** CSS filter maths ported from the values used by liquid-glass.js. */
object GlassColorMatrix {

    /** contrast(1.2) expressed as the CSS contrast matrix (scale around mid-grey). */
    fun contrast(c: Float): ColorMatrix = ColorMatrix(
        floatArrayOf(
            c, 0f, 0f, 0f, (1f - c) * 128f,
            0f, c, 0f, 0f, (1f - c) * 128f,
            0f, 0f, c, 0f, (1f - c) * 128f,
            0f, 0f, 0f, 1f, 0f,
        )
    )

    /** brightness(1.05) - a plain gain. */
    fun brightness(b: Float): ColorMatrix = ColorMatrix(
        floatArrayOf(
            b, 0f, 0f, 0f, 0f,
            0f, b, 0f, 0f, 0f,
            0f, 0f, b, 0f, 0f,
            0f, 0f, 0f, 1f, 0f,
        )
    )

    /** saturate(1.1) using the Rec.709 luminance weights, as the CSS filter does. */
    fun saturate(s: Float): ColorMatrix {
        val inv = 1f - s
        val r = 0.2126f * inv
        val g = 0.7152f * inv
        val b = 0.0722f * inv
        return ColorMatrix(
            floatArrayOf(
                r + s, g, b, 0f, 0f,
                r, g + s, b, 0f, 0f,
                r, g, b + s, 0f, 0f,
                0f, 0f, 0f, 1f, 0f,
            )
        )
    }

    /** Chain used by the upstream backdrop-filter: contrast -> brightness -> saturate. */
    fun glassFilter(): ColorMatrix {
        val result = ColorMatrix()
        result.set(contrast(1.2f))
        result.preConcat(brightness(1.05f))
        result.preConcat(saturate(1.1f))
        return result
    }
}
