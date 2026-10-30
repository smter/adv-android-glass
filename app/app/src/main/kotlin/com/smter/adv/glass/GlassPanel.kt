package com.smter.adv.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
 * shadow, blur and tone.
 *
 * It is a deep module — callers hand it the recorded background layer, the panel height state and
 * the appearance spec, and get a pane that touches the bottom edge. No layout, interaction or
 * environment decisions leak out of here, and no caller needs to know which library effect does what.
 */
@Composable
fun GlassPanel(
    backdrop: Backdrop,
    heightState: PanelHeightState,
    look: GlassLook,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val density = LocalDensity.current.density
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
        val tint = Color.White.copy(alpha = geometry.tintAlpha)
        val scrim = Brush.verticalGradient(
            0f to Color.Transparent,
            1f to Color.Black.copy(alpha = geometry.scrimAlpha),
            startY = (size.height - geometry.scrimHeightPx).coerceAtLeast(0f),
            endY = size.height,
        )
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { paneShape },
                        effects = {
                            blur(radius = pixels.blurRadiusPx)
                            lens(
                                refractionHeight = pixels.refractionHeightPx,
                                refractionAmount = pixels.refractionAmountPx,
                                chromaticAberration = look.variant == GlassVariant.DISPERSION,
                            )
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
