package com.smter.adv.ui.panel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PanelHeightStateTest {

    @Test
    fun startsAtTheCollapsedAnchorAndClampsToTheAnchors() {
        val state = PanelHeightState()
        assertEquals(PanelHeightState.COLLAPSED, state.fraction, 0f)

        state.dragBy(10f)
        assertEquals(PanelHeightState.EXPANDED, state.fraction, 0f)

        state.dragBy(-10f)
        assertEquals(PanelHeightState.COLLAPSED, state.fraction, 0f)
    }

    @Test
    fun explicitInitialFractionIsClamped() {
        assertEquals(PanelHeightState.EXPANDED, PanelHeightState(initialFraction = 5f).fraction, 0f)
        assertEquals(PanelHeightState.COLLAPSED, PanelHeightState(initialFraction = -5f).fraction, 0f)
    }

    @Test
    fun nearestAnchorPicksTheCloserOne() {
        val state = PanelHeightState()
        assertEquals(PanelHeightState.COLLAPSED, state.nearestAnchor(), 0f)

        state.jumpTo(0.51f)
        assertEquals(PanelHeightState.EXPANDED, state.nearestAnchor(), 0f)
    }

    @Test
    fun progressRunsFromCollapsedToExpanded() {
        val state = PanelHeightState(initialFraction = PanelHeightState.COLLAPSED)
        assertEquals(0f, state.progress, 0.001f)
        state.jumpTo(PanelHeightState.EXPANDED)
        assertEquals(1f, state.progress, 0.001f)
    }

    @Test
    fun upwardDragGrowsTheSheetUntilItIsExpanded() {
        val state = PanelHeightState()
        assertTrue(state.takesOverscroll(dragUp = true, listAtTop = false))

        state.jumpTo(PanelHeightState.EXPANDED)
        // Fully expanded: the article owns upward drags so it can be read.
        assertFalse(state.takesOverscroll(dragUp = true, listAtTop = false))
    }

    @Test
    fun downwardDragOnlyShrinksFromTheTopOfAnExpandedSheet() {
        val state = PanelHeightState(initialFraction = PanelHeightState.EXPANDED)
        assertTrue(state.takesOverscroll(dragUp = false, listAtTop = true))
        // Mid-article: pulling down reads the previous lines, it does not move the sheet.
        assertFalse(state.takesOverscroll(dragUp = false, listAtTop = false))

        state.jumpTo(PanelHeightState.COLLAPSED)
        assertFalse(state.takesOverscroll(dragUp = false, listAtTop = true))
    }
}
