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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smter.adv.content.ReadingSample
import kotlinx.coroutines.launch

/** Typography for the immersive body: system serif family → NotoSerifCJK for Chinese. */
val ReadingBodyStyle = TextStyle(
    fontFamily = FontFamily.Serif,
    fontSize = 16.sp,
    lineHeight = 30.sp,
    letterSpacing = 0.4.sp,
    color = Color(0xF2FFFFFF),
)

/** Vertical share of the panel per pixel of overscroll: one full swipe ≈ one anchor step. */
private const val OVERSCROLL_PER_PANEL_FRACTION = 0.0009f

/**
 * The scrollable article. Scroll gestures are consumed by the text first; whatever remains when the
 * text cannot scroll further is handed to [heightState]:
 *  - pulling up at the top of the article expands the panel (up to 70%),
 *  - pulling down at the top of the article collapses it (back to 30%).
 */
@Composable
fun ReadingPanel(
    heightState: PanelHeightState,
    modifier: Modifier = Modifier,
    onTapPanel: () -> Unit,
) {
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    var dragStartedAtTop by remember { mutableStateOf(false) }

    val connection = remember(heightState) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // Pre-scroll runs before the list sees the delta.
                if (source == NestedScrollSource.UserInput) {
                    dragStartedAtTop = scroll.value <= 0
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (available.y == 0f) return Offset.Zero
                val overscroll = available.y // negative = finger moved up (reveal more)
                // Upward overscroll at the top expands; downward overscroll from an expanded panel collapses.
                if (overscroll < 0f && scroll.value <= 0) {
                    heightState.dragBy(-overscroll * OVERSCROLL_PER_PANEL_FRACTION)
                    return Offset(0f, overscroll)
                }
                if (overscroll > 0f && scroll.value <= 0 && heightState.isExpanded) {
                    heightState.dragBy(-overscroll * OVERSCROLL_PER_PANEL_FRACTION)
                    return Offset(0f, overscroll)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                return Offset.Zero.let { _ -> Velocity.Zero }
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (heightState.fraction != heightState.collapsed && heightState.fraction != heightState.expanded) {
                    heightState.settle(velocity = available.y / 16f)
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
        // Tap on the panel body toggles the anchor; text tapping does not scroll-steal because it
        // only reacts to taps, not drags.
        Box(
            Modifier.fillMaxSize().pointerInput(heightState) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val up = waitForUpOrCancellation()
                    if (up != null) onTapPanel()
                }
            },
        )
    }
}
