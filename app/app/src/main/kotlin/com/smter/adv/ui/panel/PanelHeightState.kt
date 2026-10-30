package com.smter.adv.ui.panel

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import kotlin.math.abs

/**
 * Height of the reading panel as a share of the screen height.
 *
 * Two settle positions exist — 30% (collapsed) and 70% (expanded). Every gesture writes a
 * continuous value; release settles onto the nearest anchor by position and velocity.
 */
@Stable
class PanelHeightState(
    collapsedFraction: Float = COLLAPSED,
    expandedFraction: Float = EXPANDED,
    initialFraction: Float = collapsedFraction,
) {
    var fraction by mutableFloatStateOf(initialFraction.coerceIn(collapsedFraction, expandedFraction))
        private set

    val collapsed: Float = collapsedFraction
    val expanded: Float = expandedFraction

    val isExpanded: Boolean get() = fraction > (collapsed + expanded) / 2f

    /** 0 at the collapsed anchor, 1 at the expanded anchor. */
    val progress: Float get() = ((fraction - collapsed) / (expanded - collapsed)).coerceIn(0f, 1f)

    /** Positive delta grows the panel. Clamped to the anchors, so a drag never escapes them. */
    fun dragBy(delta: Float) {
        fraction = (fraction + delta).coerceIn(collapsed, expanded)
    }

    fun jumpTo(target: Float) {
        fraction = target.coerceIn(collapsed, expanded)
    }

    /** Animated settle onto an anchor. */
    suspend fun settle(target: Float = nearestAnchor(), velocity: Float = 0f) {
        animate(
            initialValue = fraction,
            targetValue = target.coerceIn(collapsed, expanded),
            initialVelocity = velocity,
            animationSpec = SettleSpec,
        ) { value, _ -> fraction = value }
    }

    fun nearestAnchor(): Float =
        if (abs(fraction - collapsed) <= abs(fraction - expanded)) collapsed else expanded

    companion object {
        const val COLLAPSED = 0.30f
        const val EXPANDED = 0.70f

        val SettleSpec: AnimationSpec<Float> = spring(dampingRatio = 0.86f, stiffness = Spring.StiffnessMediumLow)
    }
}
