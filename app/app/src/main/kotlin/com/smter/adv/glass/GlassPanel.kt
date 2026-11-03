package com.smter.adv.glass

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.smter.adv.ui.panel.PanelHeightState

/**
 * The docked glass pane: the one place that assembles the library's refraction, highlight, inner
 * shadow, blur and tone — and the one place that decides which of those the platform can run.
 *
 * It is a deep module — callers hand it the recorded background layer, the panel height state, the
 * appearance spec and, for screenshots only, an optional engine override. No layout, interaction or
 * environment decisions leak out of here, and no caller needs to know which library effect does what.
 */
@Composable
fun GlassPanel(
    backdrop: Backdrop,
    heightState: PanelHeightState,
    look: GlassLook,
    modifier: Modifier = Modifier,
    engine: GlassEngine? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val density = LocalDensity.current.density
    // The environment gate lives here: nobody outside this module asks the platform what it can do.
    val tier = GlassEngine.resolve(Build.VERSION.SDK_INT, engine)
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .fillMaxHeight(heightState.fraction),
    ) {
        val size = Size(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat())
        val pixels = GlassLookMath.pixels(look, size.width, size.height, density)
        val geometry = GlassLookMath.geometry(look, size.width, size.height, density)
        // The pane's silhouette: rounded on top, square where it runs off the bottom edge. The rim
        // lens refracts around this very shape, so the shader's rounded-rect SDF coincides with the
        // visible edge; a different (e.g. larger) corner set would push the band off the edge and
        // flatten the straight sides into a blur.
        val paneShape = remember(geometry.cornerRadiusDp) {
            RoundedCornerShape(
                topStart = geometry.cornerRadiusDp.dp,
                topEnd = geometry.cornerRadiusDp.dp,
                bottomEnd = 0.dp,
                bottomStart = 0.dp,
            )
        }
        val tint = Color.White.copy(alpha = GlassLookMath.tintAlpha(look, tier))
        val scrim = Brush.verticalGradient(
            0f to Color.Transparent,
            1f to Color.Black.copy(alpha = geometry.scrimAlpha),
            startY = (size.height - geometry.scrimHeightPx).coerceAtLeast(0f),
            endY = size.height,
        )
        Box(Modifier.fillMaxSize()) {
            when (tier) {
                GlassEngine.FULL, GlassEngine.BLUR -> Box(
                    Modifier
                        .fillMaxSize()
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { paneShape },
                            effects = {
                                blur(radius = pixels.blurRadiusPx)
                                // Below API 33 the library drops this call itself; keeping it out of
                                // the fallback tier as well makes the intent explicit.
                                if (tier == GlassEngine.FULL) {
                                    lens(
                                        refractionHeight = pixels.refractionHeightPx,
                                        refractionAmount = pixels.refractionAmountPx,
                                        chromaticAberration = look.variant == GlassVariant.DISPERSION,
                                    )
                                }
                            },
                            highlight = {
                                Highlight(width = geometry.highlightWidthDp.dp, alpha = geometry.highlightAlpha)
                            },
                            shadow = null,
                            innerShadow = {
                                InnerShadow(radius = geometry.innerShadowRadiusDp.dp, alpha = geometry.innerShadowAlpha)
                            },
                        ),
                )

                GlassEngine.OPAQUE -> OpaqueRim(look, geometry, paneShape)
            }
            // Tone and scrim, drawn with the lens outline so the refraction band bends the same
            // silhouette the pane shows.
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(paneShape)
                    .background(tint)
                    .background(scrim),
            )
            Box(Modifier.fillMaxSize()) { content() }
        }
    }
}

/**
 * API 26–30: no runtime shader and no render effects, so neither the lens nor the library's own
 * highlight / inner shadow can run. The pane falls back to a near-opaque tinted surface with a
 * hand-drawn top highlight and rim shadow, which keeps it a coherent pane instead of a bare window.
 */
@Composable
private fun OpaqueRim(look: GlassLook, geometry: GlassPanelGeometry, shape: Shape) {
    Box(
        Modifier
            .fillMaxSize()
            .clip(shape)
            .background(Color(look.fallbackBaseColor)),
    ) {
        // Rim shadow: a dark vignette hugging the silhouette where the glass meets its frame.
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = geometry.innerShadowAlpha),
                        ),
                    ),
                ),
        )
        // Top highlight: the pane catches the light along its top edge.
        Box(
            Modifier
                .fillMaxWidth()
                .height((geometry.highlightWidthDp * 4f).dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = geometry.highlightAlpha),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
    }
}
