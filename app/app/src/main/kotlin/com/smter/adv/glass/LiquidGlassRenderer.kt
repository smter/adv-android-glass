package com.smter.adv.glass

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Shader

/** Which visual variant to paint. Mirrors the two upstream files. */
enum class GlassVariant { LENS, DIAMOND }

/** Everything that decides how the glass looks. */
data class GlassParams(
    val variant: GlassVariant = GlassVariant.LENS,
    /** Backdrop tint painted on top of the warped band. */
    val tint: Int = 0x14FFFFFF,
    val cornerRadiusPx: Float = 0f,
    /** Pixel pull at the strongest point of the rim (feDisplacementMap scale = max/2 upstream). */
    val displacementPx: Float = 24f,
)

/**
 * Compose-free painter for the glass band. Pure geometry + Bitmap work, so it can be unit tested
 * on the JVM (Robolectric not required for the field maths) and reused by screenshot tests.
 *
 * Paint order mirrors liquid-glass.js:
 *  1. displacement pass over the wallpaper pixels behind the pill,
 *  2. contrast(1.2) brightness(1.05) saturate(1.1),
 *  3. inset shadow + rim + tint.
 */
class LiquidGlassRenderer {

    /**
     * Warps the [width]x[height] region of [backdrop] whose top-left sits at ([originX], [originY]).
     * Returns a fresh bitmap that already carries the CSS colour chain.
     */
    fun warpBand(
        backdrop: Bitmap,
        width: Int,
        height: Int,
        originX: Float,
        originY: Float,
        params: GlassParams,
    ): Bitmap {
        val shape = LensShape.scaledToAspect(height.toFloat() / width.toFloat())
        val maxPull = LensGeometry.maxDisplacement(shape)
        val reach = if (params.displacementPx > 0f) params.displacementPx / maxPull else 0f
        val src = IntArray(backdrop.width * backdrop.height)
        backdrop.getPixels(src, 0, backdrop.width, 0, 0, backdrop.width, backdrop.height)
        val dst = IntArray(width * height)
        val cx = width * 0.5f
        val cy = height * 0.5f
        val facetGain = if (params.variant == GlassVariant.DIAMOND) 2.2f else 1f

        for (y in 0 until height) {
            val v = (y + 0.5f) / height
            for (x in 0 until width) {
                val u = (x + 0.5f) / width
                val scale = LensGeometry.scaleAt(u, v, shape)
                val pull = (1f - scale) * facetGain * reach
                val px = (cx + (x + 0.5f - cx) * (1f - pull) + originX).toInt()
                val py = (cy + (y + 0.5f - cy) * (1f - pull) + originY).toInt()
                val ix = px.coerceIn(0, backdrop.width - 1)
                val iy = py.coerceIn(0, backdrop.height - 1)
                dst[y * width + x] = src[iy * backdrop.width + ix]
            }
        }
        val warped = Bitmap.createBitmap(dst, width, height, Bitmap.Config.ARGB_8888)
        val filtered = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        Canvas(filtered).drawBitmap(warped, 0f, 0f, Paint().apply {
            colorFilter = ColorMatrixColorFilter(GlassColorMatrix.glassFilter())
        })
        warped.recycle()
        return filtered
    }

    /** Inset shadow, rim and tint drawn over the warped band in [panel]. */
    fun drawFinish(canvas: Canvas, panel: RectF, params: GlassParams) {
        val radiusIn = params.cornerRadiusPx / 2f
        val path = Path().apply { addRoundRect(panel, radiusIn, radiusIn, Path.Direction.CW) }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.shader = LinearGradient(
            0f, panel.top + panel.height() * 0.5f, 0f, panel.bottom,
            intArrayOf(Color.TRANSPARENT, 0x20000000), null, Shader.TileMode.CLAMP,
        )
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
        canvas.save()
        canvas.clipPath(path)
        canvas.restore()
        paint.shader = null
        paint.xfermode = null

        paint.color = params.tint
        canvas.drawPath(path, paint)
    }
}
