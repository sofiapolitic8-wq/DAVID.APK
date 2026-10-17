package io.david.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.david.mobile.data.NotificationKind
import io.david.mobile.data.DvdNotification
import io.david.mobile.data.eventWhen
import io.david.mobile.data.hasStarted
import kotlinx.coroutines.delay

/**
 * The panel behind the bell on Home.
 *
 * One list, newest first, and every entry reads at a glance because the kind
 * decides the shape rather than a caption: an event carries a schedule and a
 * server, a chat message carries whoever sent it, an update carries what
 * changed. Tapping a card marks it read; the kind-specific action underneath
 * — Join, View, whatever it is — does the rest.
 */
@Composable
fun NotificationsScreen(
    notifications: List<DvdNotification>,
    insetTop: Dp,
    insetBottom: Dp,
    showRootTabs: Boolean = false,
    /** Alerts is a page opened from Home's bell (OM, 2026-10-04): Back closes it. */
    onBack: (() -> Unit)? = null,
    onMarkAllRead: () -> Unit,
    onOpenNotification: (String) -> Unit,
    onSetNotificationRead: (String, Boolean) -> Unit,
    onDeleteNotification: (String) -> Unit,
    onJoinEvent: (String) -> Unit,
    onOpenUpdate: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onOpenBackup: () -> Unit,
    onTabSocial: (androidx.compose.ui.geometry.Rect) -> Unit,
    onTabPlay: (androidx.compose.ui.geometry.Rect) -> Unit,
    onTabSkin: (androidx.compose.ui.geometry.Rect) -> Unit,
    onTabSettings: (androidx.compose.ui.geometry.Rect) -> Unit,
    highlightId: String? = null,
    onHighlightConsumed: () -> Unit = {},
    onRefresh: (done: () -> Unit) -> Unit = { it() },
) {
    var pulling by remember { mutableStateOf(false) }
    LaunchedEffect(pulling) { if (pulling) onRefresh { pulling = false } }
    val listState = rememberLazyListState()
    var selectedNotification by remember { mutableStateOf<DvdNotification?>(null) }
    LaunchedEffect(highlightId, notifications) {
        val index = notifications.indexOfFirst { it.id == highlightId }
        if (index >= 0) {
            listState.animateScrollToItem(index)
            delay(1200)
            onHighlightConsumed()
        }
    }
    Box(modifier = Modifier.fillMaxSize().background(Dvd.Paper)) {
    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        // Dvd iOS › Alerts: the header scrolls with the inbox; no rule under it.
        val clearance = LocalRootTabClearance.current
        IosRefreshable(pulling, { pulling = true }, Modifier.weight(1f).fillMaxWidth()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = insetTop),
                ) {
                    if (onBack != null) {
                        item {
                            Box(Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 16.dp)) {
                                Row(
                                    Modifier.align(Alignment.CenterStart)
                                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onBack),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    IosIcon(IosGlyph.CHEVRON_LEFT, Dvd.Link, size = 16.dp, semibold = true)
                                    Text("Back", fontFamily = Dvd.Body, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Dvd.Link)
                                }
                            }
                        }
                    }
                    item {
                        IosScreenHeader(kicker = "Inbox", title = "Alerts") {
                            Text(
                                text = "Read all",
                                fontFamily = Dvd.Body,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = Dvd.Link,
                                modifier = Modifier.clickable(onClick = onMarkAllRead),
                            )
                        }
                    }
                    if (notifications.isEmpty()) {
                        item {
                            IosPaperCard { IosEmptyPanel(title = "All caught up", note = "Nothing new right now.") }
                        }
                    }
                    itemsIndexed(notifications, key = { _, item -> item.id }) { _, notification ->
                        Box(Modifier.padding(horizontal = 16.dp).animateItem()) {
                        NotificationCard(
                            notification = notification,
                            highlighted = notification.id == highlightId,
                            onTap = { onOpenNotification(notification.id) },
                            onLongPress = { selectedNotification = notification },
                            onJoinEvent = onJoinEvent,
                            onOpenUpdate = onOpenUpdate,
                            onOpenLeaderboard = onOpenLeaderboard,
                            onOpenBackup = onOpenBackup,
                        )
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                    item { Spacer(Modifier.height(clearance)) }
                }
        }

        if (showRootTabs) {
            RootTabs(
                selected = RootTab.PLAY,
                insetBottom = insetBottom,
                unreadNotifications = notifications.count { !it.read },
                onNotifications = {},
                onSocial = onTabSocial,
                onPlay = onTabPlay,
                onSkin = onTabSkin,
                onSettings = onTabSettings,
            )
        }
    }

        selectedNotification?.let { notification ->
            IosActionSheet(
                title = notification.title,
                actions = listOf(
                    IosSheetAction(if (notification.read) "Mark unread" else "Mark as read") {
                        onSetNotificationRead(notification.id, !notification.read)
                    },
                    IosSheetAction("Delete notification", destructive = true) { onDeleteNotification(notification.id) },
                ),
                insetBottom = insetBottom,
                onDismiss = { selectedNotification = null },
            )
        }
    }
}

/** A quiet paper card; unread state is visible without washing out its text. */
@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun NotificationCardShell(
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    highlighted: Boolean,
    unread: Boolean,
    content: @Composable ColumnScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val shape = dvdRounded(17.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (highlighted) Dvd.Hover else Dvd.Card.copy(alpha = 0.92f))
            .border(1.dp, Dvd.Rule, shape)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(16.dp),
        content = content,
    )
}

/**
 * Dvd iOS's alert card: unread dot, the kind in tracked caps, the date and a
 * ⋯ menu; the title; the body rendered as full Markdown; any detail rows. The
 * same card for every kind — tapping still opens what the alert is about.
 */
@Composable
private fun NotificationCard(
    notification: DvdNotification,
    highlighted: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onJoinEvent: (String) -> Unit,
    onOpenUpdate: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onOpenBackup: () -> Unit,
) {
    NotificationCardShell(
        onClick = onTap,
        onLongClick = onLongPress,
        highlighted = highlighted,
        unread = !notification.read,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).clip(DvdCapsule).background(if (notification.read) Color.Transparent else Dvd.Live))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = alertLabel(notification.kind),
                    fontFamily = Dvd.Body,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.5.sp,
                    letterSpacing = 1.sp,
                    color = Dvd.Live,
                )
                Spacer(Modifier.weight(1f))
                Text(notification.createdAt.take(10), fontFamily = Dvd.Body, fontSize = 10.5.sp, color = Dvd.Quiet)
                Box(
                    Modifier
                        .size(28.dp)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onLongPress),
                    contentAlignment = Alignment.Center,
                ) { IosIcon(IosGlyph.ELLIPSIS, Dvd.Quiet, size = 18.dp) }
            }
            Text(notification.title, fontFamily = Dvd.Body, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Dvd.Ink)
            if (notification.body.isNotBlank()) {
                DvdMarkdown(source = notification.body, baseSize = 12.5.sp)
            }
            alertMeta(notification).forEach { (key, value) ->
                Row {
                    Text(key, fontFamily = Dvd.Body, fontSize = 11.5.sp, color = Dvd.Quiet)
                    Spacer(Modifier.weight(1f))
                    Text(value, fontFamily = Dvd.Body, fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp, color = Dvd.Ink)
                }
            }
            alertActionTitle(notification)?.let { action ->
                Text("$action ›", fontFamily = Dvd.Body, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, color = Dvd.Link)
            }
        }
    }
}

/** The kind in tracked caps, as Dvd iOS words it (`DvdAlertRouting.label`). */
private fun alertLabel(kind: NotificationKind): String = when (kind) {
    NotificationKind.TRAIL_LIKE -> "BEAD ON YOUR TRAIL"
    NotificationKind.TRAIL_REPLY -> "TRAIL REPLY"
    NotificationKind.SUPPORT -> "FROM DVD"
    else -> kind.name.replace('_', ' ')
}

/** Where a tap on the card leads, when it leads somewhere (`DvdAlertRouting.actionTitle`). */
private fun alertActionTitle(n: DvdNotification): String? = when (n.kind) {
    NotificationKind.TRAIL_LIKE, NotificationKind.TRAIL_REPLY -> if (n.trailId.isNullOrBlank()) null else "View trail"
    NotificationKind.SUPPORT -> "See your reports"
    NotificationKind.FOLLOW -> if (n.actorId.isNullOrBlank()) null else "View profile"
    else -> null
}

/** The detail rows iOS lists under an alert, from whatever the alert carries. */
private fun alertMeta(n: DvdNotification): List<Pair<String, String>> = buildList {
    // The trail and support alerts already say who, in their own words.
    val saysWho = n.kind == NotificationKind.TRAIL_LIKE || n.kind == NotificationKind.TRAIL_REPLY || n.kind == NotificationKind.SUPPORT
    if (!saysWho) n.actorName?.takeIf { it.isNotBlank() }?.let { add("From" to it) }
    n.eventOrganizer?.takeIf { it.isNotBlank() }?.let { add("Organizer" to it) }
    n.startsAt?.takeIf { it.isNotBlank() }?.let { add("Starts" to it.replace('T', ' ').take(16)) }
    n.serverAddress?.takeIf { it.isNotBlank() }?.let { add("Server" to it) }
    n.version?.takeIf { it.isNotBlank() }?.let { add("Version" to it) }
    n.value?.let { add("Value" to it.toString()) }
}.sortedBy { it.first }

/** The two account-local actions revealed by holding any card. */
@Composable
private fun NotificationActionsPopup(
    notification: DvdNotification,
    insetBottom: Dp,
    onDismiss: () -> Unit,
    onSetRead: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    val dismissInteraction = remember { MutableInteractionSource() }
    val cardInteraction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f))
            .clickable(
                interactionSource = dismissInteraction,
                indication = null,
                onClick = onDismiss,
            ),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = Dvd.Gutter)
                .padding(bottom = insetBottom + Dvd.Gap)
                .fillMaxWidth()
                .clip(dvdRounded(15.dp))
                .background(Dvd.Card)
                .border(1.dp, Dvd.Rule, dvdRounded(15.dp))
                .clickable(
                    interactionSource = cardInteraction,
                    indication = null,
                    onClick = {},
                )
                .padding(horizontal = 20.dp, vertical = 18.dp),
        ) {
            Text(
                text = "ALERT",
                fontFamily = Dvd.Body,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.5.sp,
                letterSpacing = 0.92.sp,
                color = Dvd.Quiet,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = notification.title,
                fontFamily = Dvd.Body,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Dvd.Ink,
                maxLines = 2,
            )
            Spacer(Modifier.height(16.dp))
            DvdRule()
            NotificationPopupAction(
                label = if (notification.read) "Mark as unread" else "Mark as read",
                color = Dvd.Ink,
            ) { onSetRead(!notification.read) }
            DvdRule()
            NotificationPopupAction("Delete notification", Dvd.Blood, onDelete)
        }
    }
}

@Composable
private fun NotificationPopupAction(label: String, color: Color, onClick: () -> Unit) {
    Text(
        text = label,
        fontFamily = Dvd.Body,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        color = color,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
    )
}

/** The small-caps tag every card opens with, plus the unread dot and the clock. */
@Composable
private fun NotificationHeader(tag: String, tagColor: Color, timestamp: String, unread: Boolean) {
    // Dvd iOS: unread dot, the kind in tracked caps, the date on the right.
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(7.dp).clip(DvdCapsule).background(if (unread) Dvd.Live else Color.Transparent))
        Spacer(Modifier.width(8.dp))
        Text(
            text = tag.uppercase(),
            fontFamily = Dvd.Body,
            fontWeight = FontWeight.Bold,
            fontSize = 9.5.sp,
            letterSpacing = 1.sp,
            color = Dvd.Live,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = timestamp,
            fontFamily = Dvd.Body,
            fontSize = 10.5.sp,
            color = Dvd.Quiet,
        )
    }
}

@Composable
private fun UnreadDot() {
    Box(
        modifier = Modifier
            .size(7.dp)
            .clip(dvdRounded(999.dp))
            .background(Dvd.Live),
    )
}

@Composable
private fun FeatureCardBody(n: DvdNotification) {
    NotificationHeader("New Feature", Dvd.Live, n.timestamp, !n.read)
    Spacer(Modifier.height(8.dp))
    Text(
        text = n.title,
        fontFamily = Dvd.Body,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        color = Dvd.Ink,
    )
    Spacer(Modifier.height(8.dp))
    DvdMarkdown(n.body, baseSize = 14.sp)
}

@Composable
private fun EventCardBody(n: DvdNotification, onJoin: (String) -> Unit) {
    var clock by remember(n.id) { mutableLongStateOf(System.currentTimeMillis()) }
    // A live countdown on Play (OM, 2026-10-01): every second, computed from the
    // start time each tick, never a cached label.
    LaunchedEffect(n.id) {
        while (true) {
            delay(1_000L - System.currentTimeMillis() % 1_000L)
            clock = System.currentTimeMillis()
        }
    }
    val started = n.hasStarted(java.time.Instant.ofEpochMilli(clock))
    NotificationHeader("Battledome Event", Dvd.Live, n.timestamp, !n.read)
    Spacer(Modifier.height(8.dp))
    Text(
        text = n.title,
        fontFamily = Dvd.Body,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        color = Dvd.Ink,
    )
    if (n.body.isNotBlank()) {
        Spacer(Modifier.height(8.dp))
        ExpandableBlock { DvdMarkdown(n.body, baseSize = 14.sp) }
    }
    Spacer(Modifier.height(12.dp))
    DvdRule()
    Spacer(Modifier.height(12.dp))
    eventWhen(n.startsAt).takeIf { it.isNotEmpty() }?.let { InfoRow("When", it) }
    n.eventOrganizer?.let { InfoRow("By", it) }
    n.serverAddress?.let { InfoRow("Server", it) }
    Spacer(Modifier.height(14.dp))

    InviteActions(
        id = n.id,
        address = n.serverAddress,
        joinLabel = if (started) "Play" else eventCountdown(n.startsAt, clock),
        joinFilled = started,
        onJoin = onJoin,
    )
}

@Composable
private fun UpdateCardBody(n: DvdNotification, onOpenUpdate: () -> Unit) {
    NotificationHeader("Update Available", Dvd.Live, n.timestamp, !n.read)
    Spacer(Modifier.height(8.dp))
    Text(
        text = n.title,
        fontFamily = Dvd.Body,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        color = Dvd.Ink,
    )
    n.version?.takeIf { it.isNotBlank() }?.let {
        Spacer(Modifier.height(6.dp))
        DvdLabel("Version $it")
    }
    if (n.body.isNotBlank()) {
        Spacer(Modifier.height(8.dp))
        ExpandableBlock { DvdMarkdown(n.body, baseSize = 14.sp) }
    }
    Spacer(Modifier.height(12.dp))
    NotificationOutlineAction(label = "View updates", onClick = onOpenUpdate)
}

@Composable
private fun InviteCardBody(n: DvdNotification, onJoin: (String) -> Unit) {
    NotificationHeader("Server Invite", Dvd.Live, n.timestamp, !n.read)
    Spacer(Modifier.height(8.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        DvdAvatar(
            url = "",
            avatarKey = "",
            initial = n.actorName ?: "?",
            size = 30.dp,
            corner = 11.dp,
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = n.actorName ?: "Someone",
            fontFamily = Dvd.Body,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Dvd.Ink,
        )
    }
    if (n.body.isNotBlank()) {
        Spacer(Modifier.height(8.dp))
        DvdMarkdown(n.body, baseSize = 14.sp)
    }
    n.serverAddress?.let {
        Spacer(Modifier.height(12.dp))
        InfoRow("Server", it)
    }
    Spacer(Modifier.height(14.dp))
    InviteActions(n.id, n.serverAddress, onJoin = onJoin)
}

@Composable
private fun FollowCardBody(n: DvdNotification) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        DvdAvatar(
            url = "",
            avatarKey = "",
            initial = n.actorName ?: "?",
            size = 34.dp,
            corner = 12.dp,
        )
        Spacer(Modifier.width(11.dp))
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Dvd.Ink)) {
                    append(n.actorName ?: "Someone")
                }
                append("  " + n.body)
            },
            fontFamily = Dvd.Body,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            color = Dvd.Ink,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            if (!n.read) {
                UnreadDot()
                Spacer(Modifier.height(4.dp))
            }
            Text(
                text = n.timestamp,
                fontFamily = Dvd.Body,
                fontSize = 10.sp,
                color = Dvd.Quiet,
            )
        }
    }
}

@Composable
private fun AchievementCardBody(n: DvdNotification) {
    NotificationHeader("Milestone", Dvd.Live, n.timestamp, !n.read)
    Spacer(Modifier.height(8.dp))
    Text(
        text = n.title,
        fontFamily = Dvd.Body,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        color = Dvd.Ink,
    )
    Spacer(Modifier.height(6.dp))
    DvdMarkdown(n.body, baseSize = 14.sp)
}

@Composable
private fun NoticeCardBody(n: DvdNotification) {
    NotificationHeader("Notice", Dvd.Blood, n.timestamp, !n.read)
    Spacer(Modifier.height(8.dp))
    Text(
        text = n.title,
        fontFamily = Dvd.Body,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        color = Dvd.Ink,
    )
    if (n.body.isNotBlank()) {
        Spacer(Modifier.height(6.dp))
        DvdMarkdown(n.body, baseSize = 14.sp)
    }
}

@Composable
private fun BroadcastCardBody(n: DvdNotification) {
    NotificationHeader("Announcement", Dvd.Quiet, n.timestamp, !n.read)
    Spacer(Modifier.height(8.dp))
    Text(
        text = n.title,
        fontFamily = Dvd.Body,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        color = Dvd.Ink,
    )
    if (n.body.isNotBlank()) {
        Spacer(Modifier.height(6.dp))
        DvdMarkdown(n.body, baseSize = 14.sp)
    }
}

@Composable
private fun RankCardBody(n: DvdNotification, onOpenLeaderboard: () -> Unit) {
    NotificationHeader("Leaderboard", Dvd.Live, n.timestamp, !n.read)
    Spacer(Modifier.height(8.dp))
    Text(
        text = n.title,
        fontFamily = Dvd.Body,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        color = Dvd.Ink,
    )
    Spacer(Modifier.height(6.dp))
    DvdMarkdown(n.body, baseSize = 14.sp)
    Spacer(Modifier.height(12.dp))
    NotificationOutlineAction(label = "View leaderboard", onClick = onOpenLeaderboard)
}

@Composable
private fun BackupCardBody(n: DvdNotification, onOpenBackup: () -> Unit) {
    NotificationHeader("Backup", Dvd.Quiet, n.timestamp, !n.read)
    Spacer(Modifier.height(8.dp))
    Text(
        text = n.title,
        fontFamily = Dvd.Body,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        color = Dvd.Ink,
    )
    Spacer(Modifier.height(6.dp))
    DvdMarkdown(n.body, baseSize = 14.sp)
    Spacer(Modifier.height(12.dp))
    NotificationOutlineAction(label = "View backup", onClick = onOpenBackup)
}

/** Secondary card action with paper contrast, never the legacy glass treatment. */
@Composable
private fun NotificationOutlineAction(
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val shape = dvdRounded(12.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .scale(pressScale(pressed))
            .clip(shape)
            .background(if (enabled) Dvd.Paper else Dvd.Well)
            .border(1.dp, Dvd.Ink.copy(alpha = if (enabled) 0.24f else 0.08f), shape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontFamily = Dvd.Body,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = if (enabled) Dvd.Ink else Dvd.Quiet.copy(alpha = 0.55f),
        )
    }
}

@Composable
private fun InviteActions(
    id: String,
    address: String?,
    joinLabel: String = "Join Now",
    joinFilled: Boolean = true,
    onJoin: (String) -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    var copied by remember(id) { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(1500)
            copied = false
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        NotificationOutlineAction(
            label = if (copied) "Copied" else "Copy IP",
            modifier = Modifier.weight(1f),
            enabled = !address.isNullOrBlank(),
        ) {
            address?.let {
                clipboard.setText(AnnotatedString(it))
                copied = true
            }
        }
        if (joinFilled) {
            DvdPrimaryAction(
                label = joinLabel,
                modifier = Modifier.weight(1.2f).height(50.dp),
            ) { address?.let(onJoin) }
        } else {
            NotificationOutlineAction(
                label = joinLabel,
                modifier = Modifier.weight(1.2f),
                enabled = !address.isNullOrBlank(),
            ) { address?.let(onJoin) }
        }
    }
}

private fun eventCountdown(startsAt: String?, nowMs: Long): String {
    val instant = startsAt ?: return "Play"
    return runCatching {
        val left = ((java.time.Instant.parse(instant).toEpochMilli() - nowMs).coerceAtLeast(0L) + 999L) / 1_000L
        val days = left / 86_400
        val hours = (left % 86_400) / 3_600
        val minutes = (left % 3_600) / 60
        val seconds = left % 60
        when {
            days > 0 -> "Starts in ${days}d ${hours}h"
            hours > 0 -> "Starts in %d:%02d:%02d".format(hours, minutes, seconds)
            else -> "Starts in %02d:%02d".format(minutes, seconds)
        }
    }.getOrDefault("Play")
}

/** A label/value pair, the same shape an event's rundown always takes. */
@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Text(
            text = label.uppercase(),
            fontFamily = Dvd.Body,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            letterSpacing = 1.2.sp,
            color = Dvd.Quiet,
            modifier = Modifier.width(78.dp),
        )
        Text(
            text = value,
            fontFamily = Dvd.Body,
            fontWeight = FontWeight.Bold,
            fontSize = 12.5.sp,
            color = Dvd.Ink,
        )
    }
}
