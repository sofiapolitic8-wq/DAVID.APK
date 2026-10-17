package io.david.mobile.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.david.mobile.R
import io.david.mobile.data.ApiPlayer
import java.text.NumberFormat
import java.util.Locale

/** Which board is showing. The server ranks; this only asks which way. */
enum class LeaderboardSort(val api: String, val label: String) {
    SCORE("score", "Score"),
    KILLS("kills", "Kills"),
}

/**
 * Spec page 03 — Social › Leaderboard.
 *
 * You are pinned in Your standing. The list is the rest of the board. Back
 * is ‹ Social (or Play if this opened from the other tab). No tab bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    players: List<ApiPlayer>,
    sort: LeaderboardSort,
    loading: Boolean,
    error: String,
    offline: Boolean,
    updatedAt: Long,
    meId: String,
    backLabel: String = "Social",
    insetTop: Dp,
    insetBottom: Dp,
    onSortChange: (LeaderboardSort) -> Unit,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
    onOpenPlayer: (String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val ranks = remember(players) { players.withIndex().associate { (index, p) -> p.id to index + 1 } }
    val me = players.firstOrNull { it.id == meId }
    val term = query.trim().lowercase()
    val visible = remember(players, term, meId) {
        val rest = players.filter { it.id != meId }
        if (term.isEmpty()) rest else rest.filter { matches(it, term) }
    }
    val standing = me?.takeIf { term.isEmpty() || matches(it, term) }
    val header = if (sort == LeaderboardSort.KILLS) "Top by kills" else "Top by score"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Dvd.Paper),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Dvd.Paper.copy(alpha = 0.94f))
                .padding(top = insetTop)
                .padding(start = 14.dp, end = 14.dp, bottom = 12.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "‹ $backLabel",
                    fontFamily = Dvd.Body,
                    fontSize = 16.sp,
                    color = Dvd.Link,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .clickable(onClick = onBack)
                        .padding(horizontal = 6.dp, vertical = 8.dp),
                )
                Text(
                    text = "Leaderboard",
                    fontFamily = Dvd.Body,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = Dvd.Ink,
                )
            }
            Segmented(sort = sort, onSortChange = onSortChange)
            SearchField(query = query, onQuery = { query = it })
            if (offline && updatedAt > 0L) {
                Text("Offline · updated ${cachedAge(updatedAt)} ago", fontFamily = Dvd.Body, fontSize = 11.sp,
                    color = Dvd.Quiet, modifier = Modifier.padding(start = 4.dp, top = 8.dp))
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Dvd.Rule),
        )

        PullToRefreshBox(
            isRefreshing = loading,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize(),
        ) {
        when {
            error.isNotEmpty() && players.isEmpty() -> Notice(error, Dvd.Blood)
            loading && players.isEmpty() -> Notice("Loading the board…", Dvd.Quiet)
            players.isEmpty() -> Notice("Nobody has registered yet.", Dvd.Quiet)
            visible.isEmpty() && standing == null && term.isNotEmpty() ->
                Notice("Nobody here matches “$query”.", Dvd.Quiet)
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = insetBottom + 24.dp),
            ) {
                if (standing != null) {
                    item(key = "standing-label") {
                        SectionLabel("Your standing")
                    }
                    item(key = "standing") {
                        StandingCard(
                            rank = ranks[standing.id] ?: 0,
                            player = standing,
                            sort = sort,
                            onClick = { onOpenPlayer(standing.id) },
                        )
                    }
                }
                if (visible.isNotEmpty()) {
                    item(key = "top-label") {
                        SectionLabel(
                            header,
                            top = if (standing != null) 22.dp else 16.dp,
                        )
                    }
                    item(key = "board") {
                        BoardCard {
                            visible.forEachIndexed { index, player ->
                                BoardRow(
                                    rank = ranks[player.id] ?: 0,
                                    player = player,
                                    sort = sort,
                                    first = index == 0,
                                    onClick = { onOpenPlayer(player.id) },
                                )
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
        }
    }
}

private fun cachedAge(savedAt: Long): String {
    val minutes = ((System.currentTimeMillis() - savedAt).coerceAtLeast(0L) / 60_000L)
    return if (minutes < 1) "just now" else if (minutes < 60) "$minutes min" else if (minutes < 1440) "${minutes / 60} hr" else "${minutes / 1440} d"
}

@Composable
private fun Segmented(sort: LeaderboardSort, onSortChange: (LeaderboardSort) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .clip(dvdRounded(10.dp))
            .background(Dvd.Track)
            .padding(3.dp),
    ) {
        LeaderboardSort.entries.forEach { option ->
            val active = option == sort
            val interaction = remember { MutableInteractionSource() }
            val pressed by interaction.collectIsPressedAsState()
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(32.dp)
                    .scale(pressScale(pressed))
                    .clip(dvdRounded(8.dp))
                    .background(if (active) Dvd.Card else Color.Transparent)
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                    ) { onSortChange(option) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = option.label,
                    fontFamily = Dvd.Body,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                    fontSize = 13.5.sp,
                    color = if (active) Dvd.Ink else Dvd.Mute,
                )
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onQuery: (String) -> Unit) {
    val shape = dvdRounded(10.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 9.dp)
            .height(38.dp)
            .clip(shape)
            .background(Dvd.Card)
            .border(1.dp, Dvd.Rule, shape)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_search),
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            colorFilter = ColorFilter.tint(Dvd.TabIdle),
        )
        Spacer(Modifier.width(8.dp))
        BasicTextField(
            value = query,
            onValueChange = onQuery,
            singleLine = true,
            textStyle = TextStyle(
                fontFamily = Dvd.Body,
                fontSize = 14.5.sp,
                color = Dvd.Ink,
            ),
            cursorBrush = SolidColor(Dvd.Ink),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                if (query.isEmpty()) {
                    Text(
                        text = "Search a name or @username",
                        fontFamily = Dvd.Body,
                        fontSize = 14.5.sp,
                        color = Dvd.TabIdle,
                    )
                }
                inner()
            },
        )
    }
}

@Composable
private fun SectionLabel(text: String, top: Dp = 16.dp) {
    Text(
        text = text.uppercase(),
        fontFamily = Dvd.Body,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.5.sp,
        letterSpacing = 0.92.sp,
        color = Dvd.Quiet,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = top, bottom = 8.dp),
    )
}

@Composable
private fun StandingCard(
    rank: Int,
    player: ApiPlayer,
    sort: LeaderboardSort,
    onClick: () -> Unit,
) {
    val shape = dvdRounded(14.dp)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    BoardPerson(
        rank = rank,
        player = player,
        sort = sort,
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .scale(pressScale(pressed))
            .clip(shape)
            .background(Dvd.Card)
            .border(1.dp, Dvd.Rule, shape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
    )
}

@Composable
private fun BoardCard(content: @Composable () -> Unit) {
    val shape = dvdRounded(14.dp)
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clip(shape)
            .background(Dvd.Card)
            .border(1.dp, Dvd.Rule, shape),
    ) {
        content()
    }
}

@Composable
private fun BoardRow(
    rank: Int,
    player: ApiPlayer,
    sort: LeaderboardSort,
    first: Boolean,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Column {
        if (!first) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Dvd.RowRule),
            )
        }
        BoardPerson(
            rank = rank,
            player = player,
            sort = sort,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 60.dp)
                .scale(pressScale(pressed))
                .clickable(interactionSource = interaction, indication = null, onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 9.dp),
        )
    }
}

@Composable
private fun BoardPerson(
    rank: Int,
    player: ApiPlayer,
    sort: LeaderboardSort,
    modifier: Modifier,
) {
    val headline = if (sort == LeaderboardSort.KILLS) player.kills else player.highestScore
    val footnote = if (sort == LeaderboardSort.KILLS) {
        "${grouped(player.highestScore)} best"
    } else {
        "${grouped(player.kills)} kills"
    }
    val name = if (player.isDeleted) "Deleted player" else player.displayName.ifEmpty { "Unnamed" }
    val handle = when {
        player.isDeleted -> "account deleted"
        !player.username.isNullOrBlank() -> "@${player.username}"
        else -> "no username yet"
    }
    val nums = TextStyle(fontFeatureSettings = "tnum")
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = rank.toString(),
            fontFamily = Dvd.Body,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = Dvd.Quiet,
            style = nums,
            modifier = Modifier.width(22.dp),
        )
        Spacer(Modifier.width(11.dp))
        DvdAvatar(
            url = player.avatarUrl,
            avatarKey = player.avatarKey,
            initial = player.displayName,
            size = 34.dp,
            corner = 10.dp,
        )
        Spacer(Modifier.width(11.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                fontFamily = Dvd.Body,
                fontSize = 14.5.sp,
                color = if (player.isDeleted) Dvd.Quiet else Dvd.Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = handle,
                fontFamily = Dvd.Body,
                fontSize = 12.5.sp,
                color = Dvd.Quiet,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = grouped(headline),
                fontFamily = Dvd.Body,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = Dvd.Ink,
                style = nums,
            )
            Text(
                text = footnote,
                fontFamily = Dvd.Body,
                fontSize = 11.5.sp,
                color = Dvd.Quiet,
                style = nums,
            )
        }
    }
}

@Composable
private fun Notice(message: String, colour: Color) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            fontFamily = Dvd.Body,
            fontSize = 14.sp,
            color = colour,
        )
    }
}

private fun matches(player: ApiPlayer, term: String): Boolean =
    player.displayName.lowercase().contains(term) ||
        player.username.orEmpty().lowercase().contains(term) ||
        player.ingameName.orEmpty().lowercase().contains(term)

private fun grouped(value: Long): String =
    NumberFormat.getIntegerInstance(Locale.US).format(value)
