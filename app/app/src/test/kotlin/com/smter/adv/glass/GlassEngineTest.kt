package com.smter.adv.glass

import com.smter.adv.glass.GlassLookMath.tintAlpha
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GlassEngineTest {

    @Test
    fun platformTiersFollowTheRuntimeShaderAndRenderEffectFloors() {
        assertEquals(GlassEngine.FULL, GlassEngine.forSdk(36))
        assertEquals(GlassEngine.FULL, GlassEngine.forSdk(33))
        assertEquals(GlassEngine.BLUR, GlassEngine.forSdk(32))
        assertEquals(GlassEngine.BLUR, GlassEngine.forSdk(31))
        assertEquals(GlassEngine.OPAQUE, GlassEngine.forSdk(30))
        assertEquals(GlassEngine.OPAQUE, GlassEngine.forSdk(26))
    }

    @Test
    fun forcedTierWinsSoFallbacksCanBeCapturedOnACapableDevice() {
        assertEquals(GlassEngine.BLUR, GlassEngine.resolve(36, GlassEngine.BLUR))
        assertEquals(GlassEngine.OPAQUE, GlassEngine.resolve(36, GlassEngine.OPAQUE))
        assertEquals(GlassEngine.FULL, GlassEngine.resolve(26, GlassEngine.FULL))
    }

    @Test
    fun noOverrideLetsThePlatformDecide() {
        assertEquals(GlassEngine.FULL, GlassEngine.resolve(36, null))
        assertEquals(GlassEngine.OPAQUE, GlassEngine.resolve(26, null))
    }

    @Test
    fun engineExtraParsesTheDocumentedValues() {
        assertEquals(GlassEngine.FULL, GlassEngine.parse("full"))
        assertEquals(GlassEngine.BLUR, GlassEngine.parse("blur"))
        assertEquals(GlassEngine.BLUR, GlassEngine.parse("fallback"))
        assertEquals(GlassEngine.OPAQUE, GlassEngine.parse("OPAQUE"))
        assertEquals(GlassEngine.BLUR, GlassEngine.parse("  blur  "))
        assertNull(GlassEngine.parse(null))
        assertNull(GlassEngine.parse("nonsense"))
    }

    @Test
    fun fallbackTiersRaiseTheTintSoTheBodyTextStaysReadable() {
        val look = GlassLook()
        assertEquals(look.tintAlpha, tintAlpha(look, GlassEngine.FULL), 0f)
        assertTrue(tintAlpha(look, GlassEngine.BLUR) >= GlassLookMath.FALLBACK_TINT_ALPHA)
        assertTrue(tintAlpha(look, GlassEngine.OPAQUE) >= GlassLookMath.FALLBACK_TINT_ALPHA)
    }

    @Test
    fun opaqueBackingColourActuallyHidesTheWallpaper() {
        val alpha = (GlassLook().fallbackBaseColor ushr 24).toInt()
        assertEquals(0xFF, alpha)
    }
}
