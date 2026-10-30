package com.smter.adv.ui.panel

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smter.adv.content.ReadingSample

/** Typography for the immersive body: system serif family → NotoSerifCJK for Chinese. */
val ReadingBodyStyle = TextStyle(
    fontFamily = FontFamily.Serif,
    fontSize = 16.sp,
    lineHeight = 30.sp,
    letterSpacing = 0.4.sp,
    color = Color(0xF2FFFFFF),
)

/** Vertical share of the panel per pixel of overscroll: one firm swipe ≈ one anchor step. */
private const val OVERSCROLL_PER_PANEL_FRACTION = 0.0016f

/**
 * The scrollable article on top of the glass.
 *
 * Gestures: the article scrolls first. While the list sits at its very top:
 *  - dragging up (finger moves up, content would scroll down) grows the panel,
 *  - dragging down shrinks it.
 *
 * Only a tap toggles the anchors; drags are left to the list, so text can always scroll.
 */
@Composable
fun ReadingPanel(
    heightState: PanelHeightState,
    modifier: Modifier = Modifier,
    onTapPanel: () -> Unit,
) {
    val scroll = rememberScrollState()

    val connection = remember(heightState, scroll) {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput || available.y == 0f) return Offset.Zero
                // Overscroll only counts while the list is pinned at its top edge.
                val atTop = scroll.value <= 0
                if (!atTop) return Offset.Zero
                val overscroll = available.y // negative while the finger moves up
                heightState.dragBy(-overscroll * OVERSCROLL_PER_PANEL_FRACTION)
                return Offset(0f, overscroll)
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (heightState.fraction != heightState.collapsed && heightState.fraction != heightState.expanded) {
                    heightState.settle(velocity = available.y / 40f)
                }
                return Velocity.Zero
            }
        }
    }

    Box(modifier.fillMaxSize().nestedScroll(connection)) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(horizontal = 28.dp)
                .padding(top = 38.dp, bottom = 44.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(
                text = ReadingSample.TITLE,
                style = TextStyle(
                    fontFamily = FontFamily.Serif,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp,
                    color = Color.White,
                ),
            )
            Text(
                text = "一则关于彗星、楼顶与旧保温杯的短记",
                style = TextStyle(
                    fontFamily = FontFamily.Serif,
                    fontSize = 13.sp,
                    letterSpacing = 2.sp,
                    color = Color(0xB3FFFFFF),
                ),
            )
            Spacer(Modifier.height(6.dp))
            ReadingSample.paragraphs.forEach { paragraph ->
                Text(text = paragraph, style = ReadingBodyStyle)
            }
            Spacer(Modifier.height(24.dp))
        }
        // Tap toggle. This sits over the text but only reacts to a press-and-release *without*
        // movement: a drag is left to the scroller, which is why it lives in the pane not on top.
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(onTapPanel) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        val up = waitForUpOrCancellation()
                        if (up != null && !up.isConsumed) onTapPanel()
                    }
                },
        )
    }
}