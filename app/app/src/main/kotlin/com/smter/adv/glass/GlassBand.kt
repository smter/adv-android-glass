package com.smter.adv.glass

import android.graphics.Bitmap
import android.graphics.RectF
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import com.smter.adv.ui.settings.GlassEngine
import com.smter.adv.ui.settings.GlassSettings
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Paints the glass band that fills the reading panel.
 *
 * Both back ends read the wallpaper band sitting directly behind the panel, so the refracted
 * content is exactly what the browser's backdrop-filter would have seen.
 */
@Composable
fun GlassBand(
    wallpaper: Bitmap,
    wallpaperTopPx: Float,
    panelFraction: Float,
    settings: GlassSettings,
    modifier: Modifier = Modifier,
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val params = remember(settings, density) {
        GlassParams(
            variant = settings.variant,
            tint = ((settings.tintAlpha * 255f).roundToInt() shl 24) or 0x00FFFFFF,
            displacementPx = with(density) { settings.displacementDp.dp.toPx() },
        )
    }
    val engine = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && settings.engine == GlassEngine.AGSL) {
        GlassEngine.AGSL
    } else {
        GlassEngine.CPU
    }
    val agsl = remember { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) AgslGlassPainter() else null }
    val cpu = remember { LiquidGlassRenderer() }
    var cachedBand by remember { mutableStateOf<Bitmap?>(null) }
    var cachedKey by remember { mutableStateOf("") }

    Canvas(modifier) {
        val bandHeight = (size.height * panelFraction).roundToInt().coerceAtLeast(1)
        val srcTop = (wallpaperTopPx + size.height - bandHeight).roundToInt().coerceIn(0, wallpaper.height - 1)
        val key = size.width.roundToInt().toString() + "x" + size.height.roundToInt() + "@" + srcTop
        if (cachedKey != key || cachedBand == null) {
            val srcWidth = size.width.roundToInt().coerceAtMost(wallpaper.width)
            val srcHeight = bandHeight.coerceAtMost(wallpaper.height - srcTop)
            cachedBand?.recycle()
            cachedBand = Bitmap.createBitmap(wallpaper, 0, srcTop, srcWidth, srcHeight)
            cachedKey = key
        }
        val band = cachedBand ?: return@Canvas
        if (engine == GlassEngine.AGSL && agsl != null) {
            drawAgsl(agsl, band, size, params)
        } else {
            drawCpu(cpu, band, size, params)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun DrawScope.drawAgsl(painter: AgslGlassPainter, band: Bitmap, size: Size, params: GlassParams) {
    painter.bind(params.variant)
    painter.draw(
        drawContext.canvas.nativeCanvas,
        band,
        AgslGlassPainter.Box(0f, 0f, size.width, size.height),
        params,
    )
}

private fun DrawScope.drawCpu(renderer: LiquidGlassRenderer, band: Bitmap, size: Size, params: GlassParams) {
    val warped = renderer.warpBand(band, size.width.roundToInt(), size.height.roundToInt(), 0f, 0f, params)
    drawImage(warped.asImageBitmap())
    warped.recycle()
    renderer.drawFinish(drawContext.canvas.nativeCanvas, RectF(0f, 0f, size.width, size.height), params)
}
