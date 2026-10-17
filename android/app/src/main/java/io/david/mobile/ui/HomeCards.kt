package io.david.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * Home's cards, shared with Dvd Desktop (synced by its build.gradle.kts):
 * moved here unchanged from HomeScreen.kt (IosArenaCard, NearOriginalRow) and
 * SkinScreen.kt (PaperSwitch), 2026-10-02.
 */

/** Dvd iOS Play's arena block: live dot, arena code, players, endpoint, load bar, Enter lobby and the globe. */
@Composable
internal fun IosArenaCard(
    title: String,
    live: Boolean,
    players: Int,
    endpoint: String,
    ready: Boolean,
    onEnter: () -> Unit,
    onPickServer: (Rect) -> Unit,
    /** Beside the title (Android: the arena's country flag). */
    titleAccessory: @Composable () -> Unit = {},
) {
    Column(Modifier.padding(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(6.dp).clip(DvdCapsule).background(if (live) Dvd.Live else Dvd.Quiet))
            Spacer(Modifier.width(6.dp))
            Text(
                text = if (live) "LIVE ARENA" else "ARENA DIRECTORY",
                fontFamily = Dvd.Body,
                fontWeight = FontWeight.Bold,
                fontSize = 10.5.sp,
                letterSpacing = 0.8.sp,
                color = if (live) Dvd.Live else Dvd.Quiet,
            )
        }
        Row(Modifier.padding(top = 11.dp), verticalAlignment = Alignment.Bottom) {
            Text(
                text = title,
                fontFamily = Dvd.Body,
                fontWeight = FontWeight.Bold,
                fontSize = 21.sp,
                color = Dvd.Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            titleAccessory()
            Spacer(Modifier.weight(1f))
            if (ready) {
                Text("$players players", fontFamily = Dvd.Body, fontSize = 11.5.sp, color = Dvd.Quiet)
            }
        }
        Text(
            text = endpoint.ifBlank { "Choose a live arena or enter an address." },
            fontFamily = Dvd.Body,
            fontSize = 12.5.sp,
            color = Dvd.Mute,
            modifier = Modifier.padding(top = 3.dp),
        )
        Box(
            Modifier
                .padding(top = 13.dp)
                .fillMaxWidth()
                .height(4.dp)
                .clip(DvdCapsule)
                .background(Dvd.Track),
        ) {
            val load = (players / 2000f).coerceIn(0f, 1f)
            if (load > 0f) Box(Modifier.fillMaxWidth(load).height(4.dp).clip(DvdCapsule).background(Dvd.Ink))
        }
        Row(Modifier.padding(top = 16.dp)) {
            val interaction = remember { MutableInteractionSource() }
            val pressed by interaction.collectIsPressedAsState()
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .scale(pressScale(pressed, ready))
                    .clip(dvdRounded(11.dp))
                    .background(if (ready) Dvd.Ink else Dvd.Ink.copy(alpha = 0.35f))
                    .clickable(interactionSource = interaction, indication = null, enabled = ready, onClick = onEnter),
                contentAlignment = Alignment.Center,
            ) {
                Text("Enter lobby", fontFamily = Dvd.Body, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Dvd.OnInk)
            }
            Spacer(Modifier.width(9.dp))
            var bounds by remember { mutableStateOf(Rect.Zero) }
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .onGloballyPositioned { bounds = it.boundsInRoot() }
                    .clip(dvdRounded(11.dp))
                    .border(1.dp, Dvd.Rule, dvdRounded(11.dp))
                    .clickable { onPickServer(bounds) },
                contentAlignment = Alignment.Center,
            ) {
                IosIcon(IosGlyph.GLOBE, Dvd.Mute, size = 19.dp, semibold = true)
            }
        }
    }
}

/** Home › Near Original: a loadout row with a switch instead of a chevron. */
@Composable
internal fun NearOriginalRow(on: Boolean, onToggle: (Boolean) -> Unit) {
    Column {
        Box(Modifier.fillMaxWidth().height(1.dp).background(Dvd.RowRule))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IosLoadoutIcon(IosGlyph.ARROW_CLOCKWISE)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Near Original", fontFamily = Dvd.Body, fontSize = 15.5.sp, color = Dvd.Ink, maxLines = 1)
                Text(
                    "Slither's own HUD, joystick and arrow. Only on-screen buttons stay movable.",
                    fontFamily = Dvd.Body, fontSize = 12.sp, lineHeight = 15.sp, color = Dvd.Quiet,
                )
            }
            Spacer(Modifier.width(10.dp))
            PaperSwitch(on = on, onToggle = onToggle)
        }
    }
}

@Composable
internal fun PaperSwitch(on: Boolean, onToggle: (Boolean) -> Unit) {
    LiquidSwitch(on = on, onToggle = onToggle)
}

