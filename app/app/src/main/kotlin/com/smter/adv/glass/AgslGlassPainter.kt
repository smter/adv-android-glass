package com.smter.adv.glass

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * Draws the glass band with an AGSL program, mirroring the browser's feDisplacementMap.
 *
 * The wallpaper band that sits behind the panel is uploaded once as a BitmapShader and handed to
 * the program through [RuntimeShader.setInputShader]; the program evaluates the lens field per
 * fragment exactly like the SVG filter did, and hands the sampled colour back.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class AgslGlassPainter {

    private var runtimeShader: RuntimeShader? = null
    private var input: BitmapShader? = null
    private var source: Bitmap? = null
    private var program: String = ""
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun bind(variant: GlassVariant) {
        val wanted = when (variant) {
            GlassVariant.LENS -> LiquidGlassShaders.LENS
            GlassVariant.DIAMOND -> LiquidGlassShaders.DIAMOND
        }
        if (wanted != program) {
            runtimeShader = RuntimeShader(wanted)
            program = wanted
        }
    }

    /**
     * @param band wallpaper pixels that sit exactly behind the panel (same pixel space as [box]).
     */
    fun draw(canvas: Canvas, band: Bitmap, box: Box, params: GlassParams) {
        val program = runtimeShader ?: return
        if (source !== band || input == null) {
            input = BitmapShader(band, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            source = band
        }
        val content = input ?: return
        val shape = LensShape.scaledToAspect(box.height / box.width)
        val max = LensGeometry.maxDisplacement(shape).coerceAtLeast(0.0001f)

        program.setFloatUniform("uPanelSize", box.width, box.height)
        program.setFloatUniform("uLayerOrigin", 0f, 0f)
        program.setFloatUniform("uLayerScale", 1f, 1f)
        program.setFloatUniform("uDisplacement", params.displacementPx / max)
        program.setFloatUniform("uCornerPx", params.cornerRadiusPx, params.cornerRadiusPx)
        program.setFloatUniform("uHalfWidth", shape.halfWidthU)
        program.setFloatUniform("uHalfHeight", shape.halfHeightU)
        program.setFloatUniform("uCornerRadius", shape.cornerRadiusU)
        program.setFloatUniform("uFeather", shape.featherU)
        program.setFloatUniform("uRamp", shape.rampU)
        if (params.variant == GlassVariant.DIAMOND) {
            program.setFloatUniform("uApex", box.width * 0.5f, box.height * 0.22f)
            program.setFloatUniform("uPrismW", box.width * 0.34f)
            program.setFloatUniform("uPrismH", box.height * 0.62f)
            program.setFloatUniform("uTime", (System.nanoTime() / 1_000_000_000f) % 1000f)
        }
        program.setInputShader("content", content)
        paint.shader = program
        canvas.save()
        canvas.translate(box.left, box.top)
        canvas.drawRect(0f, 0f, box.width, box.height, paint)
        canvas.restore()
        paint.shader = null
    }

    data class Box(val left: Float, val top: Float, val width: Float, val height: Float)
}
