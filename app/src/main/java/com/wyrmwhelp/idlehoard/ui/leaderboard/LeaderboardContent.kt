package com.wyrmwhelp.idlehoard.ui.leaderboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wyrmwhelp.idlehoard.domain.model.LeaderboardEntry
import com.wyrmwhelp.idlehoard.domain.model.LeaderboardPeriod
import com.wyrmwhelp.idlehoard.ui.common.FantasyPalette
import com.wyrmwhelp.idlehoard.ui.format.GoldFormat

/**
 * The "Leaderboard" section's real content — three ranked boards (all-time,
 * weekly, monthly gold earned; see `domain/model/Leaderboard.kt`), backed
 * by the `leaderboard_rankings` table `SQL/004_create_leaderboards.sql`
 * refreshes once an hour server-side — the app never ranks anything
 * itself, it just reads whatever that job last computed (so a number here
 * can lag reality by up to an hour; there's no live-updating attempted).
 *
 * Every player with a `profiles` username appears on a board — since
 * v0.50.0 that's genuinely everyone, guest included (see
 * `SQL/006_auto_generate_usernames.sql`'s trigger, which auto-assigns a
 * placeholder `AnonymousNNNNNN` name the instant an account exists, no
 * longer just players who explicitly set a real one). [GuestNoteCard] used
 * to explain why a guest would never see themselves here at all; now it
 * just nudges them to pick a real name via sign-in instead, since they're
 * already competing under the auto-assigned one.
 *
 * Pure display plus callbacks — [entries]/[currentUserEntry]/[isLoading]/
 * [errorMessage] are `GameViewModel.loadLeaderboard`'s state, passed in by
 * `MainActivity`'s `WyrmWhelpApp`, which triggers that load whenever this
 * section opens or [onSelectPeriod] switches tabs.
 */
@Composable
fun LeaderboardContent(
    isSignedIn: Boolean,
    period: LeaderboardPeriod,
    entries: List<LeaderboardEntry>,
    currentUserEntry: LeaderboardEntry?,
    isLoading: Boolean,
    errorMessage: String?,
    onSelectPeriod: (LeaderboardPeriod) -> Unit,
    modifier: Modifier = Modifier,
    palette: FantasyPalette = FantasyPalette.Default,
) {
    Column(modifier = modifier.fillMaxSize()) {
        if (!isSignedIn) {
            GuestNoteCard(palette = palette, modifier = Modifier.padding(bottom = 8.dp))
        }
        LeaderboardTabRow(selected = period, onSelect = onSelectPeriod, palette = palette)
        Spacer(Modifier.height(8.dp))
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = palette.goldDeep)
                errorMessage != null -> Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.ink,
                    modifier = Modifier.align(Alignment.Center),
                )
                entries.isEmpty() -> Text(
                    text = "No one's on this board yet — be the first to earn some gold!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.ink.copy(alpha = 0.8f),
                    modifier = Modifier.align(Alignment.Center),
                )
                else -> LeaderboardList(entries = entries, currentUserEntry = currentUserEntry, palette = palette)
            }
        }
    }
}

@Composable
private fun LeaderboardList(
    entries: List<LeaderboardEntry>,
    currentUserEntry: LeaderboardEntry?,
    palette: FantasyPalette,
    modifier: Modifier = Modifier,
) {
    // The current player's own rank is shown pinned below the list only
    // when it isn't already visible inside it (i.e. they're outside the
    // top N fetched) — a signed-in player ranked, say, #12 in a top-50 list
    // already sees themselves highlighted in place, no separate row needed.
    val ownRowAlreadyVisible = entries.any { it.isCurrentUser }
    LazyColumn(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items(entries, key = { it.rank }) { entry ->
            LeaderboardRow(entry = entry, palette = palette)
        }
        if (currentUserEntry != null && !ownRowAlreadyVisible) {
            item {
                Spacer(Modifier.height(4.dp))
                HorizontalDivider(thickness = 1.dp, color = palette.woodDark.copy(alpha = 0.35f))
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Your rank",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Serif, color = palette.ink),
                )
                Spacer(Modifier.height(4.dp))
            }
            item { LeaderboardRow(entry = currentUserEntry, palette = palette) }
        }
    }
}

/** One ranked row: `#rank`, username, gold earned — highlighted when it's the current player's own. */
@Composable
private fun LeaderboardRow(entry: LeaderboardEntry, palette: FantasyPalette, modifier: Modifier = Modifier) {
    ParchmentCard(
        palette = palette,
        modifier = modifier,
        borderColor = if (entry.isCurrentUser) palette.goldDeep else palette.woodDark.copy(alpha = 0.5f),
        background = if (entry.isCurrentUser) {
            Brush.verticalGradient(listOf(palette.goldBright.copy(alpha = 0.35f), palette.goldDeep.copy(alpha = 0.2f)))
        } else {
            Brush.verticalGradient(listOf(palette.parchmentShade.copy(alpha = 0.8f), palette.parchment.copy(alpha = 0.8f)))
        },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "#${entry.rank}",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium.copy(color = palette.goldDeep),
                modifier = Modifier.width(40.dp),
            )
            Text(
                text = entry.username,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Serif, color = palette.ink),
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${GoldFormat.format(entry.goldEarned)} gp",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.ink.copy(alpha = 0.85f),
            )
        }
    }
}

@Composable
private fun GuestNoteCard(palette: FantasyPalette, modifier: Modifier = Modifier) {
    ParchmentCard(palette = palette, modifier = modifier) {
        Text(
            text = "You're competing under an auto-assigned guest name. Sign in and choose a " +
                "real username (Settings → Account) to make it your own.",
            style = MaterialTheme.typography.bodySmall,
            color = palette.ink.copy(alpha = 0.8f),
        )
    }
}

@Composable
private fun LeaderboardTabRow(
    selected: LeaderboardPeriod,
    onSelect: (LeaderboardPeriod) -> Unit,
    palette: FantasyPalette,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        LeaderboardPeriod.entries.forEach { entry ->
            LeaderboardTabButton(
                text = entry.label,
                selected = entry == selected,
                onClick = { onSelect(entry) },
                palette = palette,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Same shape as `ShopContent.kt`'s private `ShopTabButton`/`UpgradesContent.kt`'s `UpgradeTabButton`, duplicated per this project's established per-file-duplication convention for small private UI helpers. */
@Composable
private fun LeaderboardTabButton(text: String, selected: Boolean, onClick: () -> Unit, palette: FantasyPalette, modifier: Modifier = Modifier) {
    val shape = CutCornerShape(6.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    if (selected) listOf(palette.goldBright, palette.goldDeep) else listOf(palette.woodLight, palette.woodDark),
                ),
            )
            .border(1.dp, palette.woodDark, shape)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (selected) palette.ink else palette.parchment,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

/** Same shape as `StewardsContent.kt`'s private `ParchmentCard`, plus a swappable [background] so [LeaderboardRow] can tint its own-row highlight. */
@Composable
private fun ParchmentCard(
    palette: FantasyPalette,
    modifier: Modifier = Modifier,
    borderColor: Color = palette.woodDark.copy(alpha = 0.5f),
    background: Brush = Brush.verticalGradient(listOf(palette.parchmentShade.copy(alpha = 0.8f), palette.parchment.copy(alpha = 0.8f))),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .border(1.5.dp, borderColor, RoundedCornerShape(10.dp))
            .padding(12.dp),
        content = content,
    )
}
