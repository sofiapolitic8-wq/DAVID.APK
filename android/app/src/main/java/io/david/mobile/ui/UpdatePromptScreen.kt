package io.david.mobile.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.david.mobile.data.UpdateState
import io.david.mobile.data.BackupState

/**
 * The update prompt.
 *
 * Dvd looks for a newer version every time it starts, quietly, and says
 * nothing unless it finds one. When it does, this is the whole conversation:
 * what the new version is called, how big it is, and two answers. Choosing to
 * update turns the same card into the progress of the download — the box never
 * moves and nothing else on screen changes, so the update happens *here*
 * rather than by throwing the player into some other screen.
 *
 * Where the build comes from is never mentioned, because it is not the
 * player's problem: they are told there is a new version of Dvd, and that is
 * all this says.
 */
@Composable
fun UpdatePrompt(
    state: UpdateState,
    backup: BackupState,
    updateStarted: Boolean,
    insetTop: Dp,
    insetBottom: Dp,
    onLater: () -> Unit,
    onUpdate: () -> Unit,
    onChooseFolder: () -> Unit,
    beta: Boolean = false,
    onBetaSettings: () -> Unit = {},
) {
    // The card arrives rather than appears: a short spring on scale and a fade,
    // the way a phone raises an alert.
    var arrived by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { arrived = true }
    val springing by animateFloatAsState(
        targetValue = if (arrived) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 340f),
        label = "entrance",
    )
    // A spring passes its target and comes back; alpha may not. The overshoot
    // is welcome in the scale, where it reads as weight, and nowhere else.
    val entrance = springing.coerceIn(0f, 1f)

    val downloading = state.status == STATUS_DOWNLOADING || state.status == STATUS_VERIFYING
    val installing = state.status == STATUS_INSTALLING
    val failed = state.failed
    val choosingFolder = updateStarted && backup.selectingFolder
    val backingUp = updateStarted && backup.saving
    val backupFailed = updateStarted && backup.failed && !downloading && !installing
    val preparingBackup = updateStarted && state.available &&
        !choosingFolder && !backingUp && !backup.saved && !backupFailed

    val shape = dvdRounded(16.dp)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC1E1C1A).copy(alpha = 0.80f * entrance))
            .padding(top = insetTop, bottom = insetBottom)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .heightIn(max = 620.dp)
                .scale(0.94f + 0.06f * springing)
                .alpha(entrance)
                .clip(shape)
                .background(Dvd.Card)
                .border(1.dp, Dvd.Rule, shape)
                .padding(horizontal = 22.dp, vertical = 24.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = when {
                    failed -> "UPDATE"
                    backupFailed -> "UPDATE PAUSED"
                    choosingFolder -> "BEFORE UPDATE"
                    backingUp || preparingBackup -> "PROTECTING DVD"
                    installing -> "ALMOST THERE"
                    downloading -> "UPDATING"
                    backup.saved -> "BACKUP READY"
                    beta -> "BETA UPDATE AVAILABLE"
                    else -> "UPDATE AVAILABLE"
                },
                fontFamily = Dvd.Body,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.5.sp,
                letterSpacing = 0.92.sp,
                color = if (failed || backupFailed) Dvd.Badge else Dvd.Quiet,
            )
            Spacer(Modifier.height(10.dp))

            Text(
                text = when {
                    failed -> "Update failed"
                    backupFailed -> "Backup failed"
                    choosingFolder -> "Choose a david.io folder"
                    backingUp -> backup.title.ifEmpty { "Backing up" }
                    preparingBackup -> "Preparing backup"
                    installing -> "Restarting"
                    downloading -> "Downloading"
                    backup.saved -> "Backup saved"
                    beta -> "david.io ${state.version} beta"
                    else -> "david.io ${state.version}"
                },
                fontFamily = Dvd.Body,
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
                lineHeight = 31.sp,
                letterSpacing = (-0.4).sp,
                color = if (failed || backupFailed) Dvd.Badge else Dvd.Ink,
            )

            Spacer(Modifier.height(10.dp))
            Text(
                text = when {
                    failed -> state.detail.ifEmpty { "It can be tried again from Settings." }
                    backupFailed -> backup.detail.ifEmpty { "The update did not start." }
                    choosingFolder -> backup.detail
                    backingUp -> backup.detail
                    preparingBackup -> "Capturing everything that belongs to this david.io before downloading."
                    installing -> "david.io closes and comes back on the new version."
                    downloading -> state.detail
                    backup.saved -> backup.detail
                    else -> state.detail
                },
                fontFamily = Dvd.Body,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                color = Dvd.Mute,
            )

            val offering = !failed && !backupFailed && !choosingFolder && !backingUp &&
                !preparingBackup && !downloading && !installing && !backup.saved
            if (beta && offering) {
                // A beta says what it is, what that means, and how to stop them.
                Spacer(Modifier.height(16.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(dvdRounded(12.dp))
                        .background(Dvd.Well)
                        .padding(14.dp),
                ) {
                    Text(
                        text = "This is a beta update",
                        fontFamily = Dvd.Body,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Dvd.Ink,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Beta updates are early builds: you get new features before everyone else, " +
                            "but they can have rough edges or bugs. Stable updates come later, for everyone.",
                        fontFamily = Dvd.Body,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        color = Dvd.Mute,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Don't want beta updates? Turn them off in Settings \u203A Backup \u203A",
                        fontFamily = Dvd.Body,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        color = Dvd.Link,
                        modifier = Modifier.clickable(onClick = onBetaSettings),
                    )
                }
            }

            if (backingUp || preparingBackup || downloading || installing) {
                Spacer(Modifier.height(20.dp))
                ProgressTrack(
                    fraction = if (backingUp) backup.count / 100f else state.progress / 100f,
                    // The install has no percentage to report, so the track
                    // sits full rather than pretending to still be moving.
                    complete = installing,
                )
            }

            if (backup.saved && (downloading || installing) && backup.detail.isNotEmpty()) {
                Spacer(Modifier.height(18.dp))
                Text(
                    text = "BACKUP STORED",
                    fontFamily = Dvd.Body,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    letterSpacing = 0.8.sp,
                    color = Dvd.Quiet,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = backup.detail,
                    fontFamily = Dvd.Body,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = Dvd.Mute,
                )
            }

            if (choosingFolder) {
                Spacer(Modifier.height(22.dp))
                PaperPrimaryButton(label = "OK", onClick = onChooseFolder)
            } else if (!backingUp && !preparingBackup && !downloading && !installing) {
                Spacer(Modifier.height(22.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.weight(1f)) {
                        PaperOutlineButton(
                            label = if (failed) "Close" else "Later",
                            onClick = onLater,
                        )
                    }
                    if (!failed) {
                        Box(Modifier.weight(1f)) {
                            PaperPrimaryButton(
                                label = if (backupFailed) "Try again" else "Update",
                                onClick = onUpdate,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** The bar that fills as the download arrives. */
@Composable
private fun ProgressTrack(fraction: Float, complete: Boolean) {
    val settling by animateFloatAsState(
        targetValue = if (complete) 1f else fraction.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = 1f, stiffness = 180f),
        label = "progress",
    )
    // fillMaxWidth refuses anything outside 0..1, and an animation is allowed
    // to be outside it for a frame.
    val filled = settling.coerceIn(0f, 1f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(dvdRounded(99.dp))
            .background(Dvd.Track),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(filled)
                .height(4.dp)
                .clip(dvdRounded(99.dp))
                .background(Dvd.Ink)
        )
    }
}

@Composable
private fun QuietAction(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(modifier = modifier) {
        PaperOutlineButton(label = label, onClick = onClick)
    }
}

/** A post-update restore stays on one card from consent through its final report. */
@Composable
fun BackupRestorePrompt(
    state: BackupState,
    insetTop: Dp,
    insetBottom: Dp,
    onLater: () -> Unit,
    onRestore: () -> Unit,
    onClose: () -> Unit,
) {
    val restoring = state.restoring
    val finished = state.restored || state.partial || state.failed
    val shape = dvdRounded(16.dp)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC1E1C1A))
            .padding(top = insetTop, bottom = insetBottom)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .heightIn(max = 620.dp)
                .clip(shape)
                .background(Dvd.Card)
                .border(1.dp, Dvd.Rule, shape)
                .padding(horizontal = 22.dp, vertical = 24.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = when {
                    restoring -> "RESTORING"
                    state.partial -> "RECOVERY REPORT"
                    finished -> "RESTORE REPORT"
                    else -> "BACKUP FOUND"
                },
                fontFamily = Dvd.Body,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.5.sp,
                letterSpacing = 0.92.sp,
                color = Dvd.Quiet,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = when {
                    state.partial -> "Some data could not be restored"
                    state.failed -> "Restore failed"
                    state.restored -> "Everything possible was restored"
                    restoring -> state.title.ifEmpty { "Restoring backup" }
                    else -> "Restore your david.io?"
                },
                fontFamily = Dvd.Body,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                lineHeight = 29.sp,
                color = if (state.partial || state.failed) Dvd.Badge else Dvd.Ink,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = if (state.restoreOffer) {
                    "A backup contains settings that are not applied in this build. " +
                        "Restore it now? You can later restore it from Settings › Backup.\n\n" +
                        state.detail
                } else {
                    state.detail
                },
                fontFamily = Dvd.Body,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                color = Dvd.Mute,
            )
            if (restoring) {
                Spacer(Modifier.height(20.dp))
                ProgressTrack(state.count / 100f, complete = false)
            }
            Spacer(Modifier.height(24.dp))
            when {
                state.restoreOffer -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.weight(1f)) { PaperOutlineButton("Later", onClick = onLater) }
                    Box(Modifier.weight(1f)) { PaperPrimaryButton("Yes", onClick = onRestore) }
                }
                finished -> PaperPrimaryButton("OK", onClick = onClose)
            }
        }
    }
}

/* The updater's states, named here so this file reads without a lookup. They
   match the constants the activity emits. */
private const val STATUS_DOWNLOADING = 4
private const val STATUS_VERIFYING = 5
private const val STATUS_INSTALLING = 7
