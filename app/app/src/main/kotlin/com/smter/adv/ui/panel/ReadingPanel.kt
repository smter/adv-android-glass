package com.smter.adv.ui.panel

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
 * Gestures: an upward drag grows the panel until it is fully expanded; after that the article
 * scrolls normally. Pulling down at the very top of the article shrinks the panel again. A tap
 * toggles the anchors.
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
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput || available.y == 0f) return Offset.Zero
                // available.y is a scroll delta: negative while the finger moves up (the content
                // would scroll forward), positive while it moves down.
                if (!heightState.takesOverscroll(dragUp = available.y < 0f, listAtTop = scroll.value <= 0)) {
                    return Offset.Zero
                }
                heightState.dragBy(-available.y * OVERSCROLL_PER_PANEL_FRACTION)
                return available
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (heightState.fraction != heightState.collapsed && heightState.fraction != heightState.expanded) {
                    heightState.settle(velocity = available.y / 40f)
                }
                return Velocity.Zero
            }
        }
    }

    Box(
        modifier
            .fillMaxSize()
            .nestedScroll(connection)
            // Tap-to-toggle lives on the container rather than on an overlay above it: an overlay
            // would shadow the scrollable underneath, so the article could never scroll.
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onTapPanel,
            ),
    ) {
        Column(
            Modifier
                // fillMaxSize() would pin the column to the viewport height and leave the scroll
                // range at zero; the column must wrap its content so the article can scroll.
                .fillMaxWidth()
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
    }
}
