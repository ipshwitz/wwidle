package com.wyrmwhelp.idlehoard.ui.game

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wyrmwhelp.idlehoard.domain.model.CreatureLair
import com.wyrmwhelp.idlehoard.domain.model.OwnedLair
import com.wyrmwhelp.idlehoard.ui.common.FantasyPalette
import com.wyrmwhelp.idlehoard.ui.format.CycleTimeFormat
import com.wyrmwhelp.idlehoard.ui.format.GoldFormat

/**
 * Fixed duration for the fill bar's [animateFloatAsState] — deliberately
 * *not* tied to `GameEngine.TICK_INTERVAL_MS` the way it originally was.
 * Tracking the tick rate exactly meant the bar re-synced to a fresh target
 * almost immediately every tick, so a fast-resetting target (a heavily
 * Speed-boosted lair) produced visible bounce instead of being smoothed out.
 *
 * Started at 150ms, which fixed the bounce but overcorrected: a tween is
 * always chasing a moving target, so it lags the raw value by roughly its
 * own duration the whole time the bar is filling — with a 150ms lag and a
 * production cycle only a few hundred milliseconds long (a middling
 * Speed-boosted lair, not fast enough to hit
 * `GameEngine.PROGRESS_SOLID_THRESHOLD_SECONDS`), the animated value never
 * caught up before the reset snapped it back down, so the bar visibly fell
 * short of ever looking "full." Dropped to 60ms, then to 20ms (v0.21.6,
 * alongside `GameEngine.TICK_INTERVAL_MS` dropping from 33ms to 8ms for the
 * same underlying reason — see that constant's doc) — short enough that
 * even a lair whose cycle is only a few tens of milliseconds long (comfortably
 * above `GameEngine.PROGRESS_SOLID_THRESHOLD_SECONDS`, so it's still really
 * animating rather than showing solid) can visibly climb most of the way to
 * full before resetting, while still being long enough (~2.5x the lowered
 * tick interval) to smooth the now much finer per-tick quantization steps.
 */
private const val PROGRESS_ANIMATION_DURATION_MS = 20

/** Fixed overall card height (v0.22.0 redesign) — see [LairCard]'s class doc for why this replaced content-driven sizing. */
private val CARD_HEIGHT = 96.dp

/** Fixed width of the owned-count panel in the bottom row. */
private val OWNED_BOX_WIDTH = 56.dp

/**
 * A muted brick-red standing in for "can't afford this" on the buy button —
 * deliberately not a stock bright red, which would clash with the warm
 * wood/parchment/gold palette everywhere else in this chrome.
 */
private val unaffordableLight = Color(0xFFA8564A)
private val unaffordableDark = Color(0xFF6E332B)

/**
 * One lair in the list. Redesigned in v0.22.0 from a single card whose whole
 * background doubled as its own progress bar to a fixed-height
 * ([CARD_HEIGHT]) card split into two rows, matching a layout piloted first
 * as an HTML mockup and reskinned here with this app's existing
 * [FantasyPalette] wood/parchment/gold tones and [rarityColor] ramp rather
 * than copied wholesale:
 * - **Top row**: the lair name and challenge rating on one truncating line
 *   (`"Kobold Warren (1/8 CR)"`, built as a single [buildAnnotatedString] so
 *   they truncate together rather than as two independent `Text`s), then a
 *   dedicated progress-bar track below it — a dark inset groove, an animated
 *   rarity-gradient fill, a soft gloss strip along the top edge, and the
 *   income/cycle-time line overlaid centered on top of the bar itself
 *   (previously a separate line elsewhere in the card). An unclaimed lair
 *   (`owned.count == 0`) shows the track empty with a "Claim to begin"
 *   prompt instead of a real fraction.
 * - **Bottom row**: the [BuyButton] (gold gradient when affordable, the
 *   muted [unaffordableLight]/[unaffordableDark] brick tone when not) takes
 *   most of the width; a fixed-width [OwnedBox] panel to its right replaces
 *   the old "Owned: N" text line.
 *
 * The monster name (e.g. "Kobold") that used to sit next to the challenge
 * rating is dropped — the lair name already implies it in every case so
 * far, and there wasn't room for it alongside a real progress bar and the
 * income/cycle-time line it now overlays.
 *
 * [coinBurstTrigger] is hoisted up to `LairRow` (not local `remember` state
 * here) so the creature avatar next to this card — a separate container —
 * can fire the same burst as tapping the card itself; both go through the
 * same counter, and `LairRow` is what actually decides when to bump it now
 * (on `OwnedLair.completedLoads` changing, not on the tap — see that file).
 *
 * [progress] is read straight from `GameEngine.lairProgress` (via
 * `GameViewModel`/`GameScreen`/`LairRow`) instead of being derived here from
 * raw `OwnedLair.cycleProgressSeconds`/`effectiveProductionSeconds` the way
 * it used to be — a heavily Speed-boosted lair can complete one or more full
 * cycles inside a single engine tick, and deriving the fraction per
 * composable from a value that can wrap around between samples read as the
 * fill bar "bouncing" instead of animating. The engine now does that
 * derivation once per tick and reports a flat 1f (a continuously solid bar)
 * once a lair's cycle gets too fast to sample meaningfully — see
 * `GameEngine.PROGRESS_SOLID_THRESHOLD_SECONDS`. The animation's own tween
 * duration ([PROGRESS_ANIMATION_DURATION_MS]) is fixed and deliberately
 * decoupled from the engine's tick rate, so rapid resets get smoothed
 * rather than tracked frame-for-frame — *except* the reset itself:
 * [progress] only ever increases within a cycle and resets exactly once on
 * completion (never any other kind of decrease), so a lower [progress] than
 * last recomposition is unambiguously "a new cycle just started," not a
 * value worth tweening smoothly down to. Tweening it anyway made the bar
 * visibly slide backward into its next cycle instead of snapping to empty
 * and refilling — and, since that backward slide ate into the tween's own
 * window, also cut the *next* forward tween short enough that a moderately
 * fast lair's bar rarely looked like it actually reached full before
 * resetting again. A remembered `previousProgress` (per card, since this is
 * `remember`ed inside the composable) tracks last recomposition's raw value
 * so the reset can `snap()` instead.
 *
 * [productionSeconds] is this lair's current actual cycle time (after Speed
 * Boost and milestone stacking) — shown centered on the progress bar as
 * `"${gp} gp / ${cycle time}"` via [CycleTimeFormat] instead of the old flat
 * "gp/cycle" label, so the player can actually see how fast a lair is
 * collecting rather than just its per-cycle payout.
 */
@Composable
fun LairCard(
    lair: CreatureLair,
    owned: OwnedLair,
    goldPieces: Double,
    buyQuantity: BuyQuantity,
    globalIncomeMultiplier: Double,
    progress: Float,
    productionSeconds: Double,
    coinBurstTrigger: Int,
    onClaim: () -> Unit,
    onStartLoad: () -> Unit,
    modifier: Modifier = Modifier,
    palette: FantasyPalette = FantasyPalette.Default,
    profitBoostMultiplier: Double = 1.0,
) {
    // coerceAtLeast(1): MAX resolves to 0 when even one more unit isn't
    // affordable — falling back to a 1-unit preview keeps the button showing
    // a real cost (and staying correctly disabled) instead of a "x0" label.
    val claimQuantity = buyQuantity.resolve(lair, owned.count, goldPieces).coerceAtLeast(1)
    val claimCost = lair.costForUnits(owned.count, claimQuantity)
    val canClaim = goldPieces >= claimCost
    var previousProgress by remember { mutableFloatStateOf(progress) }
    val isReset = progress < previousProgress
    previousProgress = progress
    val animatedFillFraction by animateFloatAsState(
        targetValue = if (owned.count > 0) progress else 0f,
        animationSpec = if (isReset) snap() else tween(durationMillis = PROGRESS_ANIMATION_DURATION_MS),
        label = "lairFill",
    )
    val rarity = rarityColor(lair.tier)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(CARD_HEIGHT)
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.verticalGradient(
                    listOf(palette.parchmentShade, palette.parchment),
                ),
            )
            .border(1.5.dp, rarity.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
            .clickable(
                enabled = owned.count > 0 && !owned.hasSteward && !owned.isLoading,
                onClick = onStartLoad,
            ),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = palette.ink)) {
                            append(lair.name)
                        }
                        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = palette.ink.copy(alpha = 0.6f))) {
                            append(" (${lair.challengeRating} CR)")
                        }
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Serif,
                        fontSize = 13.sp,
                        lineHeight = 15.sp,
                        shadow = Shadow(Color.Black.copy(alpha = 0.2f), Offset(0.5f, 0.5f), blurRadius = 0.5f),
                    ),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(18.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(palette.woodDark.copy(alpha = 0.35f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = animatedFillFraction)
                            .background(Brush.horizontalGradient(listOf(rarity.copy(alpha = 0.65f), rarity))),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .background(
                                    Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.35f), Color.Transparent)),
                                ),
                        )
                    }
                    Text(
                        text = if (owned.count > 0) {
                            "${GoldFormat.format(lair.incomePerCycle(owned.count, globalIncomeMultiplier, profitBoostMultiplier))} gp / ${CycleTimeFormat.format(productionSeconds)}"
                        } else {
                            "Claim to begin"
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            lineHeight = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.parchment,
                            shadow = Shadow(Color.Black.copy(alpha = 0.55f), Offset.Zero, blurRadius = 1f),
                        ),
                        modifier = Modifier.align(Alignment.Center).padding(horizontal = 4.dp),
                    )
                }
            }

            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                BuyButton(
                    text = if (owned.count == 0) {
                        if (claimQuantity == 1) "Claim" else "Claim x$claimQuantity"
                    } else {
                        "+$claimQuantity"
                    },
                    price = "${GoldFormat.format(claimCost)} gp",
                    affordable = canClaim,
                    onClick = onClaim,
                    palette = palette,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
                OwnedBox(
                    count = owned.count,
                    palette = palette,
                    modifier = Modifier.width(OWNED_BOX_WIDTH).fillMaxHeight(),
                )
            }
        }

        CoinBurstOverlay(trigger = coinBurstTrigger, modifier = Modifier.matchParentSize())
    }
}

/**
 * The bottom-left buy action — a gold gradient when [affordable], the muted
 * brick tone when not, with a two-line label (quantity on top, price below)
 * instead of the single-line `WoodenButton` pill this replaced. Kept private
 * to this file since nothing else needs a button shaped quite like this one.
 */
@Composable
private fun BuyButton(
    text: String,
    price: String,
    affordable: Boolean,
    onClick: () -> Unit,
    palette: FantasyPalette,
    modifier: Modifier = Modifier,
) {
    val textColor = if (affordable) palette.ink else palette.parchment
    Column(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    if (affordable) {
                        listOf(palette.goldBright, palette.goldDeep)
                    } else {
                        listOf(unaffordableLight, unaffordableDark)
                    },
                ),
            )
            .clickable(enabled = affordable, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold, color = textColor),
        )
        Text(
            text = price,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = textColor),
        )
    }
}

/** The bottom-right owned-count panel — a recessed wood-toned box replacing the old "Owned: N" text line. */
@Composable
private fun OwnedBox(count: Int, palette: FantasyPalette, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(palette.woodDark.copy(alpha = 0.28f))
            .border(width = 1.5.dp, color = palette.woodDark.copy(alpha = 0.4f)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold, color = palette.ink),
        )
        Text(
            text = "owned",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                fontStyle = FontStyle.Italic,
                color = palette.ink.copy(alpha = 0.6f),
            ),
        )
    }
}

/**
 * A five-band "rarity" color ramp across the catalog's tiers (green → blue →
 * purple → orange → gold), so the lair list reads as a visible power curve at
 * a glance instead of a wall of identical cards.
 */
internal fun rarityColor(tier: Int): Color = when {
    tier <= 2 -> Color(0xFF4CAF50)
    tier <= 5 -> Color(0xFF2196F3)
    tier <= 8 -> Color(0xFF9C27B0)
    tier <= 11 -> Color(0xFFFF9800)
    else -> Color(0xFFFFC107)
}
