package com.smter.adv.wallpaper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.net.Uri
import com.smter.adv.R
import java.io.File

/**
 * Owns the backdrop picture shown behind the reading panel.
 *
 * The bundled artwork (ref/壁纸.jpg, shipped in res/drawable-nodpi) is the default. A picture picked
 * through the system photo picker is copied into app storage so it survives restarts without needing
 * a persistable grant.
 */
class WallpaperRepository(private val context: Context) {

    private val store: File by lazy { File(context.filesDir, "wallpaper").apply { mkdirs() } }

    fun current(): Uri {
        val picked = File(store, PICKED)
        return if (picked.exists()) Uri.fromFile(picked) else Uri.parse(BUNDLED)
    }

    /**
     * Decodes into a downscaled ARGB bitmap that exactly covers the screen, cropped like
     * ContentScale.Crop. Recentred to the bottom of the image so the sky and the city band both read.
     */
    fun loadCover(screenWidth: Int, screenHeight: Int): Bitmap {
        val uri = current()
        val options = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
        val decoded = runCatching {
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        }.getOrNull()
        val source = decoded ?: BitmapFactory.decodeResource(context.resources, R.drawable.wallpaper_reference)

        val scale = maxOf(screenWidth.toFloat() / source.width, screenHeight.toFloat() / source.height)
        val outW = (source.width * scale).toInt().coerceAtLeast(screenWidth)
        val outH = (source.height * scale).toInt().coerceAtLeast(screenHeight)
        val scaled = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
        Canvas(scaled).drawBitmap(
            source,
            Matrix().apply { setScale(scale, scale) },
            null,
        )
        // Crop: keep the width centred, anchor the vertical crop to the bottom third.
        val left = ((outW - screenWidth) / 2).coerceAtLeast(0)
        val top = ((outH - screenHeight) * 2 / 3).coerceIn(0, (outH - screenHeight).coerceAtLeast(0))
        val result = Bitmap.createBitmap(scaled, left, top, screenWidth, screenHeight)
        if (result !== scaled) scaled.recycle()
        source.recycle()
        return result
    }

    /** Copies a picked image into app storage and returns the new file. */
    fun adopt(uri: Uri): Boolean = runCatching {
        val target = File(store, PICKED)
        context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        }
        true
    }.getOrDefault(false)

    /** Drops the picked image, falling back to the bundled reference artwork. */
    fun reset() {
        File(store, PICKED).delete()
    }

    companion object {
        private const val PICKED = "picked.jpg"
        const val BUNDLED = "android.resource://com.smter.adv/drawable/wallpaper_reference"
    }
}
