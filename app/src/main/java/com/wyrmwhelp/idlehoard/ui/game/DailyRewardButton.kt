package com.wyrmwhelp.idlehoard.ui.game

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wyrmwhelp.idlehoard.R
import com.wyrmwhelp.idlehoard.ui.common.FantasyPalette

/**
 * The Daily Reward system's persistent quick-access icon — fixed bottom-start
 * in the main game screen (`GameScreen`), the mirror image of
 * [QuickAdBoostButton] on bottom-end, with `FloatingMenu`'s chest toggle
 * anchoring the center: "Floating Calendar Icon (left), Menu (center), Ads
 * (right)," per explicit design. Same 72.dp touch target / 24.dp bottom
 * inset as those two so all three read as one matched row of round icon
 * buttons — `GameScreen` applies that matching bottom padding via this
 * composable's [modifier].
 *
 * Uses the real `calendar_state_normal`/`calendar_state_new` art (a
 * hand-illustrated hanging rune-scroll "calendar" banner — its 4x7 grid of
 * rune squares happens to echo the 28-day cycle) — [canClaim] swaps which
 * of the two square 1144x1144 images shows, same "two art states, not a
 * dim/undim treatment" idea `QuickAdBoostButton` uses a single dimmed alpha
 * for instead, since these are genuinely two distinct pieces of art rather
 * than one icon faded. Whole-square `Image` (no circular crop) since the
 * banner's own silhouette — pointed rod tips extending to both edges,
 * tapering tassels top and bottom — doesn't read as a circle the way a
 * portrait does; matches how `media_play.png`/`new_notification.png` are
 * shown elsewhere in this app. `calendar_state_new.png`'s solid yellow
 * backdrop (vs. `calendar_state_normal.png`'s genuinely transparent one)
 * is used as supplied rather than second-guessed — same standing
 * convention this file's own Assets section already applies to every art
 * asset that arrives slightly different from expectation — and doubles as
 * a glow effect for the claimable state, which reads well here.
 *
 * A small gold [DayBadge] overlaps the bottom-end corner with the current
 * day number — the art itself doesn't encode a specific day, so this is
 * still needed for the same "which day am I on" glance the earlier
 * text-placeholder version showed inline.
 *
 * Always tappable regardless of [canClaim], same "stays tappable, just
 * shows the current state" convention as [QuickAdBoostButton] — tapping
 * while already claimed today still opens [DailyRewardDialog], just in its
 * "come back tomorrow" state rather than a claimable preview.
 */
@Composable
fun DailyRewardButton(
    canClaim: Boolean,
    day: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    palette: FantasyPalette = FantasyPalette.Default,
) {
    Box(
        modifier = modifier.size(72.dp).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(modifier = Modifier.size(64.dp), contentAlignment = Alignment.BottomEnd) {
            Image(
                painter = painterResource(if (canClaim) R.drawable.calendar_state_new else R.drawable.calendar_state_normal),
                contentDescription = if (canClaim) "Daily Reward ready to claim" else "Daily Reward",
                modifier = Modifier.size(64.dp),
            )
            DayBadge(day = day, colors = palette, modifier = Modifier.offset(x = 4.dp, y = 4.dp))
        }
    }
}

/** Small gold day-number badge overlapping the calendar icon's corner — same shape as `QuickAdBoostButton`'s private `SlotBadge`, duplicated per this project's per-file-duplication convention. */
@Composable
private fun DayBadge(day: Int, colors: FantasyPalette, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(22.dp)
            .background(colors.goldBright, CircleShape)
            .border(1.dp, colors.woodDark, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = day.toString(), color = colors.ink, fontWeight = FontWeight.Bold, fontSize = 10.sp)
    }
}
