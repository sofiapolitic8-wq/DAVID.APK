package io.david.mobile.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.R as LucideR
import io.david.mobile.data.VoiceParticipant
import io.david.mobile.data.VoiceRoom

/*
 * Voice rooms, redesigned (OM, 2026-09-30: "kisi professional app ka ho").
 * The pieces every voice page shares: the room's art (official rooms wear the
 * real Dvd mark, the curvy W, never a typed "W"), live bars, the room card,
 * the room-code field with its "Ask the creator" line, participant tiles and
 * the call dock. Dvd iOS draws the same (`DvdVoiceKit.swift`).
 */

/** A room's picture: the Dvd mark on ink for official rooms, else the creator's avatar. */
@Composable
internal fun VoiceRoomArt(room: VoiceRoom, size: Dp) {
    if (room.managedPublic) {
        Box(
            Modifier.size(size).clip(dvdRounded(size * 0.30f)).background(Dvd.Ink),
            contentAlignment = Alignment.Center,
        ) { DvdMark(size = size * 0.60f, ink = Dvd.OnInk, unfilled = Color.Transparent) }
    } else {
        DvdAvatar(room.creator.avatarUrl, room.creator.avatarKey, room.creator.displayName.ifBlank { room.name }, size)
    }
}

/** Three bars that dance while a room is live; still bars otherwise. */
@Composable
internal fun VoiceLiveBars(colour: Color, playing: Boolean, height: Dp = 12.dp) {
    val loop = rememberInfiniteTransition(label = "voice-bars")
    val phases = listOf(0, 180, 360).map { offset ->
        loop.animateFloat(
            initialValue = 0.3f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(520, delayMillis = offset, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "bar",
        )
    }
    Row(Modifier.height(height), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
        phases.forEachIndexed { index, phase ->
            val level = if (playing) phase.value else listOf(0.45f, 0.8f, 0.6f)[index]
            Box(Modifier.width(3.dp).height(height * level).clip(CircleShape).background(colour))
        }
    }
}

/** "● 3 in" on green for a live room, "Quiet" otherwise. */
@Composable
internal fun VoiceLivePill(room: VoiceRoom) {
    if (room.active) {
        Row(
            Modifier.clip(CircleShape).background(Dvd.Live.copy(alpha = 0.13f)).padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            VoiceLiveBars(Dvd.Live, playing = true, height = 11.dp)
            Text("${room.activeCount} in", fontFamily = Dvd.Body, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Dvd.Live)
        }
    } else {
        Text("Quiet", fontFamily = Dvd.Body, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Dvd.Quiet)
    }
}

/** A room in the list: art, name, who and how to get in, and whether it is live. */
@Composable
internal fun VoiceRoomCard(room: VoiceRoom, onOpen: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    // Player rooms ask a newcomer for the code; a closed room takes no one.
    val locked = !room.managedPublic && !room.mine && !room.member
    Row(
        Modifier
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .fillMaxWidth()
            .scale(pressScale(pressed))
            .clip(dvdRounded(20.dp))
            .background(Dvd.Card)
            .border(1.dp, if (room.active) Dvd.Live.copy(alpha = 0.35f) else Dvd.Rule, dvdRounded(20.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onOpen)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        VoiceRoomArt(room, 48.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(room.name, fontFamily = Dvd.Body, fontWeight = FontWeight.Bold, fontSize = 15.5.sp, color = Dvd.Ink,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                if (locked) Icon(painterResource(LucideR.drawable.lucide_ic_lock), null, Modifier.size(12.dp), tint = Dvd.Quiet)
                Text(
                    when {
                        room.managedPublic -> "Official · open to everyone"
                        room.mine -> "Your room · ${if (room.gate == "open") "open" else "closed"}"
                        room.gate != "open" -> "by ${room.creator.displayName} · closed right now"
                        locked -> "by ${room.creator.displayName} · code needed"
                        else -> "by ${room.creator.displayName} · you're a member"
                    },
                    fontFamily = Dvd.Body, fontSize = 12.5.sp, color = Dvd.Quiet, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
        VoiceLivePill(room)
    }
}

/** A section title for the voice pages, with an optional quiet note on the right. */
@Composable
internal fun VoiceSectionTitle(title: String, note: String = "") {
    Row(Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 22.dp, bottom = 6.dp), verticalAlignment = Alignment.Bottom) {
        Text(title, fontFamily = Dvd.Body, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Dvd.Ink, modifier = Modifier.weight(1f))
        if (note.isNotEmpty()) Text(note, fontFamily = Dvd.Body, fontSize = 12.sp, color = Dvd.Quiet)
    }
}

/**
 * The private room's code: eight boxes filled as it is typed, then the way
 * to get one ("Don't have a code? Ask <creator> for it.", the name opens
 * their profile).
 */
@Composable
internal fun VoiceRoomCodeField(
    code: String,
    onCode: (String) -> Unit,
    creator: String,
    error: String,
    onAskCreator: () -> Unit,
    onDone: () -> Unit,
) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Enter the room code", fontFamily = Dvd.Body, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Dvd.Ink)
        Spacer(Modifier.height(4.dp))
        Text("This room is private. Its code has 8 letters and numbers.", fontFamily = Dvd.Body, fontSize = 12.5.sp,
            color = Dvd.Quiet, textAlign = TextAlign.Center)
        Spacer(Modifier.height(14.dp))
        BasicTextField(
            value = code,
            onValueChange = { typed -> onCode(typed.filter { it.isLetterOrDigit() }.uppercase().take(8)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                keyboardType = KeyboardType.Ascii,
                autoCorrectEnabled = false,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            decorationBox = { inner -> Box {
                // The real field stays in the tree (focus, keyboard) but unseen; the boxes show the code.
                Box(Modifier.size(1.dp).alpha(0f)) { inner() }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (index in 0 until 8) {
                        val filled = index < code.length
                        val current = index == code.length
                        Box(
                            Modifier.size(width = 36.dp, height = 46.dp).clip(dvdRounded(11.dp))
                                .background(if (filled) Dvd.Card else Dvd.Well)
                                .border(
                                    if (current || error.isNotEmpty()) 1.5.dp else 1.dp,
                                    when {
                                        error.isNotEmpty() -> Dvd.Blood
                                        current -> Dvd.Ink
                                        else -> Dvd.Rule
                                    },
                                    dvdRounded(11.dp),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(code.getOrNull(index)?.toString() ?: "", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
                                fontSize = 19.sp, color = Dvd.Ink)
                        }
                    }
                }
            } },
        )
        if (error.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(error, fontFamily = Dvd.Body, fontSize = 12.sp, color = Dvd.Blood, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(12.dp))
        val who = creator.ifBlank { "the room's creator" }
        Text(
            buildAnnotatedString {
                append("Don't have a code? Ask ")
                withStyle(SpanStyle(color = Dvd.Link, fontWeight = FontWeight.Bold)) { append(who) }
                append(" for it.")
            },
            fontFamily = Dvd.Body, fontSize = 13.sp, color = Dvd.Mute, textAlign = TextAlign.Center,
            modifier = Modifier.clip(CircleShape)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onAskCreator)
                .padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

/** One person in the call: a round avatar with a live ring while they speak, a muted badge, the name. */
@Composable
internal fun VoiceParticipantTile(
    participant: VoiceParticipant,
    speaking: Boolean,
    you: Boolean,
    onTap: () -> Unit,
) {
    val loop = rememberInfiniteTransition(label = "speaking")
    val pulse by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(900, easing = LinearEasing)), label = "pulse")
    Column(
        Modifier.clip(dvdRounded(18.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onTap)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(84.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier.size(84.dp).drawBehind {
                    if (!speaking) return@drawBehind
                    // A soft ring that keeps leaving the avatar while they talk.
                    drawCircle(Dvd.Live.copy(alpha = 0.35f * (1f - pulse)), radius = size.minDimension / 2 * (0.82f + 0.18f * pulse))
                    drawCircle(Dvd.Live, radius = size.minDimension / 2 * 0.84f, style = Stroke(width = 3.dp.toPx()))
                },
            )
            DvdAvatar(participant.avatarUrl, participant.avatarKey, participant.displayName, 64.dp, corner = 32.dp)
            if (participant.muted) {
                Box(
                    Modifier.align(Alignment.BottomEnd).offset(x = (-6).dp, y = (-6).dp).size(24.dp).clip(CircleShape)
                        .background(Dvd.Paper).padding(2.dp).clip(CircleShape).background(Dvd.Blood),
                    contentAlignment = Alignment.Center,
                ) { Icon(painterResource(LucideR.drawable.lucide_ic_mic_off), null, Modifier.size(12.dp), tint = Color.White) }
            }
        }
        Text(
            participant.displayName + if (you) " (you)" else "",
            fontFamily = Dvd.Body, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, color = Dvd.Ink,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 104.dp),
        )
        Text(
            when { speaking -> "Speaking"; participant.muted -> "Muted"; else -> "Listening" },
            fontFamily = Dvd.Body, fontSize = 11.sp, color = if (speaking) Dvd.Live else Dvd.Quiet,
        )
    }
}

/** A round call control: filled while "on", quiet well otherwise; red for Leave. */
@Composable
internal fun VoiceDockButton(icon: Int, label: String, on: Boolean, danger: Boolean = false, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val back = when {
        danger -> Color(0xFFE5484D)
        on -> Dvd.Ink
        else -> Dvd.Well
    }
    val ink = if (danger || on) Color.White else Dvd.Ink
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            Modifier.size(56.dp).scale(pressScale(pressed)).clip(CircleShape).background(back)
                .clickable(interactionSource = interaction, indication = null, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) { Icon(painterResource(icon), label, Modifier.size(22.dp), tint = if (danger || on) ink else Dvd.Ink) }
        Text(label, fontFamily = Dvd.Body, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = Dvd.Mute)
    }
}

/**
 * Joining or creating, as a small sheet at the bottom instead of a full-screen
 * seven-step wall: the room's name, one plain line for where it is, a thin
 * progress, Cancel. On failure: what went wrong, Retry and Cancel.
 */
@Composable
internal fun VoiceJoinSheet(
    title: String,
    stages: List<String>,
    stage: Int,
    failure: String,
    bottom: Dp,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.28f)), contentAlignment = Alignment.BottomCenter) {
        Column(
            Modifier.padding(start = 12.dp, end = 12.dp, bottom = bottom + 12.dp).fillMaxWidth().widthIn(max = 460.dp)
                .clip(dvdRounded(26.dp)).background(Dvd.Card).border(1.dp, Dvd.Rule, dvdRounded(26.dp))
                .padding(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(Modifier.size(46.dp).clip(dvdRounded(14.dp)).background(Dvd.Ink), contentAlignment = Alignment.Center) {
                    if (failure.isEmpty()) VoiceLiveBars(Dvd.OnInk, playing = true, height = 18.dp)
                    else Icon(painterResource(LucideR.drawable.lucide_ic_mic_off), null, Modifier.size(20.dp), tint = Dvd.OnInk)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        if (failure.isEmpty()) stages.getOrElse(stage) { stages.last() } else "Couldn't get you in",
                        fontFamily = Dvd.Body, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Dvd.Ink,
                    )
                    Text(if (failure.isEmpty()) title else failure, fontFamily = Dvd.Body, fontSize = 12.5.sp,
                        color = if (failure.isEmpty()) Dvd.Quiet else Dvd.Blood, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
            }
            if (failure.isEmpty()) {
                Spacer(Modifier.height(16.dp))
                // Fills stage by stage, gliding between them.
                val done by androidx.compose.animation.core.animateFloatAsState(
                    ((stage + 1).toFloat() / stages.size.coerceAtLeast(1)).coerceIn(0.2f, 1f), tween(420, easing = FastOutSlowInEasing),
                    label = "join-progress",
                )
                Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(Dvd.Well)) {
                    Box(Modifier.fillMaxWidth(done).height(4.dp).clip(CircleShape).background(Dvd.Live))
                }
                Spacer(Modifier.height(14.dp))
                Text("Cancel", fontFamily = Dvd.Body, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp, color = Dvd.Link,
                    modifier = Modifier.align(Alignment.End).clickable(interactionSource = remember { MutableInteractionSource() },
                        indication = null, onClick = onCancel).padding(6.dp))
            } else {
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.weight(1f)) { PaperOutlineButton(label = "Cancel", onClick = onCancel) }
                    Box(Modifier.weight(1f)) { PaperPrimaryButton(label = "Try again", onClick = onRetry) }
                }
            }
        }
    }
}

/** The room screen's facts in one quiet row: who can enter, how many, how many calls. */
@Composable
internal fun VoiceFacts(room: VoiceRoom) {
    Row(
        Modifier.padding(horizontal = 20.dp).fillMaxWidth().clip(dvdRounded(18.dp)).background(Dvd.Card)
            .border(1.dp, Dvd.Rule, dvdRounded(18.dp)).heightIn(min = 64.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val facts = listOf(
            (when {
                room.managedPublic -> "Open"
                room.gate != "open" -> "Closed"
                else -> "Code"
            }) to "Access",
            "${room.activeCount}/${room.capacity}" to "Inside",
            room.lifetimeCalls.toString() to "Calls",
        )
        facts.forEachIndexed { index, (value, label) ->
            if (index > 0) Box(Modifier.width(1.dp).height(30.dp).background(Dvd.Rule))
            Column(Modifier.weight(1f).padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(value, fontFamily = Dvd.Body, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Dvd.Ink)
                Text(label, fontFamily = Dvd.Body, fontSize = 11.sp, color = Dvd.Quiet)
            }
        }
    }
}
