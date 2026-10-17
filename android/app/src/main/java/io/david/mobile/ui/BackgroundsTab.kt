package io.david.mobile.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The arena's floor.
 *
 * One entry per background, in the same order the engine holds them, because
 * the index is what is stored and what crosses the bridge. Kept as a plain list
 * here rather than read from native: these are packaged assets, so Compose can
 * open the same files the renderer does and show the real tile.
 */
data class ArenaBackground(val label: String, val asset: String?)

val ARENA_BACKGROUNDS = listOf(
    ArenaBackground("david.io", "textures/backgrounds/bg54.jpg"),
    ArenaBackground("None", null),
    ArenaBackground("Hex slate", "textures/backgrounds/bg45.jpg"),
    ArenaBackground("Flowers", "textures/backgrounds/dd.jpg"),
    ArenaBackground("Green lava", "textures/backgrounds/greenlava.jpg"),
    ArenaBackground("Grid", "textures/backgrounds/grid.jpg"),
    ArenaBackground("Mars", "textures/backgrounds/mars.jpg"),
    ArenaBackground("Metal grid", "textures/backgrounds/metalgrid.jpg"),
    ArenaBackground("Noise", "textures/backgrounds/noise.jpg"),
    ArenaBackground("Red lava", "textures/backgrounds/redlava.jpg"),
    // Same order as the engine (backgrounds.h 10-17).
    ArenaBackground("Black", null),
    ArenaBackground("Midnight", "textures/backgrounds/dvd_midnight.png"),
    ArenaBackground("Carbon", "textures/backgrounds/dvd_carbon.png"),
    ArenaBackground("Abyss", "textures/backgrounds/dvd_abyss.png"),
    ArenaBackground("Nebula", "textures/backgrounds/dvd_nebula.png"),
    ArenaBackground("Dot grid", "textures/backgrounds/dvd_dotgrid.png"),
    ArenaBackground("Contours", "textures/backgrounds/dvd_contours.png"),
    ArenaBackground("Scales", "textures/backgrounds/dvd_scales.png"),
)

@Composable
fun BackgroundsTab(selected: Int, onPick: (Int) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize().padding(horizontal = Dvd.Gutter, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(ARENA_BACKGROUNDS.size) { index ->
            BackgroundCell(
                background = ARENA_BACKGROUNDS[index],
                chosen = index == selected,
                onClick = { onPick(index) },
            )
        }
    }
}

@Composable
private fun BackgroundCell(
    background: ArenaBackground,
    chosen: Boolean,
    onClick: () -> Unit,
) {
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(dvdRounded(Dvd.Corner))
                .background(Dvd.Black)
                .border(
                    if (chosen) 2.dp else 1.dp,
                    if (chosen) Dvd.White else Dvd.Line,
                    dvdRounded(Dvd.Corner),
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            val tile = rememberAssetTile(background.asset)
            if (tile != null) {
                // Cropped rather than fitted: a tile shown whole tells you less
                // about a floor than a piece of it at the size you will see it.
                Image(
                    bitmap = tile,
                    contentDescription = background.label,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else if (background.asset == null) {
                Text(
                    background.label.uppercase(),
                    fontFamily = Dvd.Body,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    letterSpacing = 1.4.sp,
                    color = Dvd.Faint,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = background.label,
            fontFamily = Dvd.Body,
            fontWeight = if (chosen) FontWeight.Bold else FontWeight.Normal,
            fontSize = 10.sp,
            color = if (chosen) Dvd.White else Dvd.Faint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Decodes a packaged tile, downsampled.
 *
 * These are full-size arena floors — several are close to a megapixel — and a
 * grid of twenty at full resolution is both slow and pointless at this size, so
 * they are sampled down on the way in.
 */
@Composable
internal fun rememberAssetTile(asset: String?): ImageBitmap? {
    if (asset == null) return null
    val context = LocalContext.current
    var bitmap by remember(asset) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(asset) {
        bitmap = withContext(Dispatchers.IO) {
            runCatching {
                context.assets.open(asset).use { stream ->
                    BitmapFactory.decodeStream(
                        stream, null,
                        BitmapFactory.Options().apply { inSampleSize = 4 },
                    )?.asImageBitmap()
                }
            }.getOrNull()
        }
    }
    return bitmap
}
