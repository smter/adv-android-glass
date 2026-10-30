package com.smter.adv.glass

import com.smter.adv.glass.GlassLookMath.stronger
import com.smter.adv.glass.GlassLookMath.weaker
import org.junit.Assert.assertEquals
import org.junit.Test

class GlassLookKnobTest {

    @Test
    fun steppingStrengthChangesOnlyStrength() {
        val look = GlassLook(strengthScale = 1f, variant = GlassVariant.DISPERSION)
        val up = look.stronger()
        assertEquals(1f + GlassLookMath.STRENGTH_STEP, up.strengthScale, 0.001f)
        assertEquals(GlassVariant.DISPERSION, up.variant)

        val down = up.weaker()
        assertEquals(1f, down.strengthScale, 0.001f)
    }

    @Test
    fun strengthStaysInsideTheDemoLadder() {
        val saturated = GlassLook(strengthScale = GlassLookMath.MAX_STRENGTH)
        assertEquals(GlassLookMath.MAX_STRENGTH, saturated.stronger().strengthScale, 0f)

        val collapsed = GlassLook(strengthScale = GlassLookMath.MIN_STRENGTH)
        assertEquals(GlassLookMath.MIN_STRENGTH, collapsed.weaker().strengthScale, 0f)
    }
}
