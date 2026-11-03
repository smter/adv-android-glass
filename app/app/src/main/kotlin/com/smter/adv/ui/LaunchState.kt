package com.smter.adv.ui

import android.app.Activity
import com.smter.adv.glass.GlassEngine
import com.smter.adv.glass.GlassVariant

/**
 * Debug-only wiring for the screenshot regression loop: launcher extras put the demo into a known
 * state (panel height, variant, rendering tier) without anyone tapping the control strip.
 *
 * Extras are sticky for the life of the launch: the activity is recreated on configuration changes
 * and must not silently lose the state it was started with.
 */
class LaunchState private constructor(
    /** Explicit height override; null means "use the collapsed anchor". */
    val panelFraction: Float?,
    val variant: GlassVariant?,
    /** Forced rendering tier; null means "let the glass module read the platform". */
    val engine: GlassEngine?,
) {
    /** True when launch extras asked for a debug state; the control strip is hidden then. */
    val isDebugLaunch: Boolean get() = panelFraction != null || variant != null || engine != null

    companion object {
        /** No extras: the plain demo the user sees. */
        val None = LaunchState(panelFraction = null, variant = null, engine = null)

        private var bound: LaunchState? = null

        /**
         * Reads '--es panel 0.7 --es variant lens --es engine blur' style extras off the intent,
         * once per launch.
         */
        fun bind(activity: Activity): LaunchState = bound ?: read(activity).also { bound = it }

        private fun read(activity: Activity): LaunchState {
            val extras = activity.intent?.extras
            val variant = when (extras?.getString("variant")?.lowercase()) {
                "lens" -> GlassVariant.LENS
                "dispersion", "diamond", "prism" -> GlassVariant.DISPERSION
                else -> null
            }
            return LaunchState(
                panelFraction = extras?.getString("panel")?.toFloatOrNull(),
                variant = variant,
                engine = GlassEngine.parse(extras?.getString("engine")),
            )
        }

        /** Test seam: forgets the extras so the next launch re-reads its intent. */
        fun reset() {
            bound = null
        }
    }
}
