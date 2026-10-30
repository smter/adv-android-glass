package com.smter.adv.ui

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.smter.adv.glass.GlassLook
import com.smter.adv.glass.GlassLookMath.stronger
import com.smter.adv.glass.GlassLookMath.weaker
import com.smter.adv.glass.GlassPanel
import com.smter.adv.glass.GlassVariant
import com.smter.adv.ui.panel.PanelHeightState
import com.smter.adv.ui.panel.ReadingPanel
import com.smter.adv.wallpaper.WallpaperRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Single-screen demo:
 *  - full-bleed wallpaper,
 *  - a bottom glass reading panel docked to the screen edge, 30% tall and growing to 70% while
 *    reading,
 *  - a small control strip at the top to switch the rim optics and the wallpaper.
 *
 * Layer order (bottom → top): wallpaper layer (the backdrop the glass samples) → tap-outside
 * catcher → glass panel + text → control strip. The catcher only covers the area the panel does
 * not, so it never steals a tap from the panel's own controls.
 */
@Composable
fun DemoScreen(launch: LaunchState = LaunchState.None) {
    val context = LocalContext.current
    val repository = remember { WallpaperRepository(context) }
    val scope = rememberCoroutineScope()
    val backdrop = rememberLayerBackdrop()

    val boot = remember {
        PanelHeightState(initialFraction = launch.panelFraction ?: PanelHeightState.COLLAPSED)
    }
    val heightState = remember(boot) { boot }
    var look by remember {
        mutableStateOf(GlassLook(variant = launch.variant ?: GlassLook().variant))
    }
    var wallpaper by remember { mutableStateOf<Bitmap?>(null) }
    var showControls by remember { mutableStateOf(!launch.isDebugLaunch) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            withContext(Dispatchers.IO) { repository.adopt(uri) }
            wallpaper = withContext(Dispatchers.Default) {
                repository.loadCover(context.resources.displayMetrics.widthPixels, context.resources.displayMetrics.heightPixels)
            }
        }
    }

    LaunchedEffect(Unit) {
        wallpaper = withContext(Dispatchers.Default) {
            repository.loadCover(context.resources.displayMetrics.widthPixels, context.resources.displayMetrics.heightPixels)
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val screenH = maxHeight
        val fraction = heightState.fraction

        // The artwork layer. The glass samples this exact layer, so its pixels inside the panel and
        // just outside the rim can never drift apart.
        Box(
            Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop),
        ) {
            wallpaper?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "背景壁纸",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        // Tap on the uncovered part of the wallpaper collapses the panel. It stops exactly where the
        // panel starts, so the panel's own tap gesture is never shadowed.
        Box(
            Modifier
                .fillMaxWidth()
                .height((screenH * (1f - fraction)).coerceAtLeast(0.dp))
                .align(Alignment.TopCenter)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {
                    scope.launch { heightState.settle(PanelHeightState.COLLAPSED) }
                },
        )

        GlassPanel(
            backdrop = backdrop,
            heightState = heightState,
            look = look,
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            ReadingPanel(
                heightState = heightState,
                modifier = Modifier.fillMaxSize(),
                onTapPanel = {
                    scope.launch {
                        heightState.settle(
                            if (heightState.isExpanded) PanelHeightState.COLLAPSED else PanelHeightState.EXPANDED,
                        )
                    }
                },
            )
        }

        if (showControls) {
            ControlStrip(
                look = look,
                onLook = { look = it },
                onPick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                onResetWallpaper = {
                    scope.launch {
                        withContext(Dispatchers.IO) { repository.reset() }
                        wallpaper = withContext(Dispatchers.Default) {
                            repository.loadCover(
                                context.resources.displayMetrics.widthPixels,
                                context.resources.displayMetrics.heightPixels,
                            )
                        }
                    }
                },
                onHide = { showControls = false },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 12.dp, end = 12.dp, bottom = 16.dp),
            )
        } else {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0x33FFFFFF))
                    .clickable { showControls = true },
            )
        }
    }
}

@Composable
private fun ControlStrip(
    look: GlassLook,
    onLook: (GlassLook) -> Unit,
    onPick: () -> Unit,
    onResetWallpaper: () -> Unit,
    onHide: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0x66000000))
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(18.dp))
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Chip("透镜", look.variant == GlassVariant.LENS) {
                onLook(look.copy(variant = GlassVariant.LENS))
            }
            Chip("色散", look.variant == GlassVariant.DISPERSION) {
                onLook(look.copy(variant = GlassVariant.DISPERSION))
            }
            Chip("折射 −", false) { onLook(look.weaker()) }
            Chip("折射 +", false) { onLook(look.stronger()) }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Chip("换图", false, onPick)
            Chip("还原", false, onResetWallpaper)
            Chip("收纳", false, onHide)
        }
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        style = TextStyle(
            fontFamily = FontFamily.Serif,
            fontSize = 13.sp,
            color = if (selected) Color.White else Color(0xB3FFFFFF),
        ),
        modifier = Modifier
            .padding(end = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Color(0x6614E0FF) else Color(0x1AFFFFFF))
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 6.dp),
    )
}