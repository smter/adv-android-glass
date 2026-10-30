package com.smter.adv.glass

import com.smter.adv.glass.GlassLookMath.pixels
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** A medium phone panel, as the emulator draws it: 1080 px wide, ~16% of the screen tall, xxhdpi. */
private const val PANEL_W = 1080f
private const val PANEL_H = 390f
private const val DENSITY = 2.625f

class GlassLookTest {

    @Test
    fun dockedPaneHasRoundedSymmetricTopCornersAndSquareBottom() {
        val radii = pixels(GlassLook(), PANEL_W, PANEL_H, DENSITY).cornerRadii
        assertEquals(radii.topStart, radii.topEnd, 0f)
        assertTrue(radii.topStart > 0f)
        assertEquals(0f, radii.bottomStart, 0f)
        assertEquals(0f, radii.bottomEnd, 0f)
    }

    @Test
    fun topRadiusIsDensityScaled() {
        val radii = pixels(GlassLook(cornerRadiusDp = 28f), PANEL_W, PANEL_H, DENSITY).cornerRadii
        assertEquals(73.5f, radii.topStart, 0.01f)
    }

    @Test
    fun cornerRadiiNeverExceedHalfTheShortSide() {
        val onAWidePane = pixels(GlassLook(cornerRadiusDp = 28f), PANEL_W, PANEL_H, DENSITY)
        assertEquals(73.5f, onAWidePane.cornerRadii.topStart, 0.01f)

        val onANarrowPane = pixels(GlassLook(cornerRadiusDp = 28f), widthPx = 40f, heightPx = PANEL_H, density = 3f)
        assertEquals(20f, onANarrowPane.cornerRadii.topStart, 0.01f)
    }

    @Test
    fun lensRefractsAroundTheSameSilhouetteThePaneShows() {
        // If the shader's rounded-rect SDF does not coincide with the visible pane shape, the straight
        // sides flatten into a blur and the corners drift. The lens radii must therefore equal the
        // pane's own radii: rounded on top, square at the bottom where the pane runs off-screen.
        val px = pixels(GlassLook(cornerRadiusDp = 28f), PANEL_W, PANEL_H, DENSITY)
        assertEquals(px.cornerRadii.topStart, px.lensCornerRadii.topStart, 0f)
        assertEquals(px.cornerRadii.topEnd, px.lensCornerRadii.topEnd, 0f)
        assertEquals(0f, px.lensCornerRadii.bottomStart, 0f)
        assertEquals(0f, px.lensCornerRadii.bottomEnd, 0f)
    }

    @Test
    fun lensSilhouetteIsDensityScaled() {
        val mdpi = pixels(GlassLook(cornerRadiusDp = 28f), PANEL_W, PANEL_H, density = 1f)
        assertEquals(28f, mdpi.lensCornerRadii.topStart, 0.001f)
        val xxxhdpi = pixels(GlassLook(cornerRadiusDp = 28f), PANEL_W, PANEL_H, density = 3f)
        assertEquals(84f, xxxhdpi.lensCornerRadii.topStart, 0.001f)
    }

    @Test
    fun refractionIsExpressedInPixelsAndScalesWithDensity() {
        val look = GlassLook(refractionHeightDp = 20f, refractionAmountDp = 12f, strengthScale = 1f)

        val mdpi = pixels(look, PANEL_W, PANEL_H, density = 1f)
        assertEquals(20f, mdpi.refractionHeightPx, 0.001f)
        assertEquals(12f, mdpi.refractionAmountPx, 0.001f)

        val xxxhdpi = pixels(look, PANEL_W, PANEL_H, density = 3f)
        assertEquals(60f, xxxhdpi.refractionHeightPx, 0.001f)
        assertEquals(36f, xxxhdpi.refractionAmountPx, 0.001f)
    }

    @Test
    fun blurAndBottomScrimFollowDensity() {
        val px = pixels(GlassLook(blurRadiusDp = 16f, scrimHeightDp = 160f), PANEL_W, PANEL_H, DENSITY)
        assertEquals(42f, px.blurRadiusPx, 0.01f)
        assertEquals(420f, px.scrimHeightPx, 0.01f)
    }

    @Test
    fun zeroStrengthStillPullsTheRimVisibly() {
        val px = pixels(GlassLook(strengthScale = 0f), PANEL_W, PANEL_H, DENSITY)
        assertTrue(px.refractionHeightPx > 20f)
        assertTrue(px.refractionAmountPx > 12f)
    }

    @Test
    fun nonsenseLookIsClampedInsteadOfFlatteningThePane() {
        val look = GlassLook(refractionHeightDp = -20f, refractionAmountDp = -5f, strengthScale = -1f)
        val px = pixels(look, PANEL_W, PANEL_H, DENSITY)
        assertTrue(px.refractionHeightPx >= GlassLookMath.MIN_HEIGHT_PX)
        assertTrue(px.refractionAmountPx >= GlassLookMath.MIN_AMOUNT_PX)
    }
}
