package com.smter.adv.ui

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smter.adv.glass.GlassBand
import com.smter.adv.glass.GlassVariant
import com.smter.adv.ui.panel.PanelHeightState
import com.smter.adv.ui.panel.ReadingPanel
import com.smter.adv.ui.settings.GlassEngine
import com.smter.adv.ui.settings.GlassSettings
import com.smter.adv.wallpaper.WallpaperRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Single-screen demo:
 *  - full-bleed wallpaper,
 *  - bottom glass reading panel that starts at 30% and rises to 70% while scrolling,
 *  - a small control strip on top to A/B the two glass variants and the two rendering runtimes.
 */
@Composable
fun DemoScreen() {
    val context = LocalContext.current
    val repository = remember { WallpaperRepository(context) }
    val scope = rememberCoroutineScope()
    val heightState = remember { PanelHeightState() }
    var settings by remember { mutableStateOf(GlassSettings()) }
    var wallpaper by remember { mutableStateOf<Bitmap?>(null) }
    var showControls by remember { mutableStateOf(true) }

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
        val screenW = maxWidth
        val wallpaperTopPx = 0f

        // Backdrop. This is the content the glass refracts.
        wallpaper?.let { bitmap ->
            androidx.compose.foundation.Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "背景壁纸",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Slight vignette so white serif text stays legible over the brightest parts of the art.
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.55f to Color.Transparent,
                        1f to Color(0x66000000),
                    )
                ),
        )

        // Tap outside the panel → collapse.
        Box(
            Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null,
                ) {
                    scope.launch { heightState.settle(0.30f) }
                },
        )

        // Glass + text panel.
        val fraction = heightState.fraction
        Box(
            Modifier
                .fillMaxWidth()
                .height(screenH * fraction)
                .align(Alignment.BottomCenter),
        ) {
            wallpaper?.let {
                GlassBand(
                    wallpaper = it,
                    wallpaperTopPx = wallpaperTopPx,
                    panelFraction = fraction,
                    settings = settings,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            ReadingPanel(
                heightState = heightState,
                modifier = Modifier.fillMaxSize(),
                onTapPanel = {
                    scope.launch {
                        if (heightState.isExpanded) heightState.settle(0.30f) else heightState.settle(0.70f)
                    }
                },
            )
        }

        if (showControls) {
            ControlStrip(
                settings = settings,
                onSettings = { settings = it },
                onPick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                onResetWallpaper = {
                    scope.launch {
                        withContext(Dispatchers.IO) { repository.reset() }
                        wallpaper = withContext(Dispatchers.Default) {
                            repository.loadCover(context.resources.displayMetrics.widthPixels, context.resources.displayMetrics.heightPixels)
                        }
                    }
                },
                onHide = { showControls = false },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 8.dp, start = 12.dp, end = 12.dp),
            )
        } else {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(12.dp)
                    .size(28.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0x33FFFFFF))
                    .clickable { showControls = true },
            )
        }
    }
}

@Composable
private fun ControlStrip(
    settings: GlassSettings,
    onSettings: (GlassSettings) -> Unit,
    onPick: () -> Unit,
    onResetWallpaper: () -> Unit,
    onHide: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0x59000000))
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(18.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Chip("透镜", settings.variant == GlassVariant.LENS) {
                onSettings(settings.copy(variant = GlassVariant.LENS))
            }
            Chip("棱镜", settings.variant == GlassVariant.DIAMOND) {
                onSettings(settings.copy(variant = GlassVariant.DIAMOND))
            }
            Chip(if (settings.engine == GlassEngine.AGSL) "AGSL" else "CPU", true) {
                onSettings(settings.copy(engine = if (settings.engine == GlassEngine.AGSL) GlassEngine.CPU else GlassEngine.AGSL))
            }
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
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}
