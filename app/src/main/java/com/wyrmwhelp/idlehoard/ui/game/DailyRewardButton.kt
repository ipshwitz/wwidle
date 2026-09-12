package com.wyrmwhelp.idlehoard.ui.game

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.wyrmwhelp.idlehoard.R

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
 * hand-illustrated hanging rune-scroll "calendar" banner) — [canClaim]
 * swaps which of the two square 1144x1144 images shows, same "two art
 * states, not a dim/undim treatment" idea `QuickAdBoostButton` uses a
 * single dimmed alpha for instead, since these are genuinely two distinct
 * pieces of art rather than one icon faded. Whole-square `Image` (no
 * circular crop) since the banner's own silhouette — pointed rod tips
 * extending to both edges, tapering tassels top and bottom — doesn't read
 * as a circle the way a portrait does; matches how
 * `media_play.png`/`new_notification.png` are shown elsewhere in this app.
 *
 * **No day-number badge** — v0.51.0 originally overlaid a small gold
 * circle with the current day number, but per explicit follow-up
 * ("we are using the glow effect to make it clear they need to click on
 * it, let's get rid of that number completely") that's gone as of
 * v0.52.3: `calendar_state_new`'s glow (see the Assets section — fixed in
 * v0.52.2 to actually render) is the sole "something's ready" signal now,
 * and the day count is still available inside [DailyRewardDialog] itself.
 *
 * Always tappable regardless of [canClaim], same "stays tappable, just
 * shows the current state" convention as [QuickAdBoostButton] — tapping
 * while already claimed today still opens [DailyRewardDialog], just in its
 * "come back tomorrow" state rather than a claimable preview.
 */
@Composable
fun DailyRewardButton(
    canClaim: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.size(72.dp).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(if (canClaim) R.drawable.calendar_state_new else R.drawable.calendar_state_normal),
            contentDescription = if (canClaim) "Daily Reward ready to claim" else "Daily Reward",
            modifier = Modifier.size(64.dp),
        )
    }
}
