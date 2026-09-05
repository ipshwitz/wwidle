package com.wyrmwhelp.idlehoard.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wyrmwhelp.idlehoard.R
import com.wyrmwhelp.idlehoard.ui.menu.SIGN_ASPECT_RATIO
import com.wyrmwhelp.idlehoard.ui.menu.floatingMenuItems

/** Fixed size for the overlapping sign header — see [SectionOverlayCard]. */
private val SIGN_HEADER_WIDTH = 240.dp

/**
 * Derived (not hardcoded) from [SIGN_ASPECT_RATIO] so it can't silently drift
 * out of sync with the actual art the way a copy-pasted constant did when the
 * sign images were recropped — needed as a concrete `Dp` (not just an
 * `aspectRatio` modifier) so the card surface below can reserve exactly half
 * of it as a top inset.
 */
private val SIGN_HEADER_HEIGHT = SIGN_HEADER_WIDTH / SIGN_ASPECT_RATIO

/**
 * A card that slides up from the bottom to cover 92% of the screen height,
 * with a scrim behind it and a close button in its top-right corner — used
 * for every `FloatingMenu` section instead of navigating to a separate full
 * screen, so the game underneath is never actually left. [title] drives both
 * visibility (non-null = shown) and content; the last non-null value is
 * retained while animating out so the card doesn't go blank mid-exit.
 *
 * The header reuses whichever `floatingMenuItems` entry matches [title] — the
 * same wooden-sign art shown on that item in the menu — and straddles the
 * card's top edge like a hanging plaque: the card surface is inset from the
 * top by half the sign's height, and the sign sits at the very top of the
 * (taller) surrounding box, so its top half reads as "outside" the card over
 * the scrim and its bottom half overlaps the card surface. The sign is
 * anchored top-start (not centered) so it doesn't compete with the
 * `CloseButton` anchored top-end — same straddle, opposite corners. Every
 * section has its own sign now; a future section added before its own art
 * exists falls back to a plain bold title with no overlap.
 * The card itself uses [AppBackground] with the wooden-wall art (a tavern
 * interior) instead of `GameScreen`'s landscape, behind the same 50%-white
 * overlay treatment.
 *
 * Content padding inside the surface must account for the sign's overlap on
 * top of its own breathing room (`SIGN_HEADER_HEIGHT / 2 + 20.dp`, not a
 * flat guess) — using less than the actual overlap renders content partly
 * hidden underneath the sign, which is the bug this class previously had.
 *
 * [content] defaults to the "Coming soon…" placeholder every section used to
 * show unconditionally; a caller with something real to display (so far,
 * just Unlocks — see `MainActivity`'s `WyrmWhelpApp`) passes its own content
 * instead. Runs inside the same padded `Column` so it still clears the sign
 * header the same way everything else here does.
 */
@Composable
fun SectionOverlayCard(
    title: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit = { ComingSoonPlaceholder() },
) {
    var lastTitle by remember { mutableStateOf(title) }
    if (title != null) lastTitle = title
    val headerImageRes = floatingMenuItems.firstOrNull { it.label == lastTitle }?.imageRes

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = title != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss,
                    ),
            )
        }

        AnimatedVisibility(
            visible = title != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.92f),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Surface(
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    color = Color.Transparent,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .padding(top = if (headerImageRes != null) SIGN_HEADER_HEIGHT / 2 else 0.dp)
                        .fillMaxSize(),
                ) {
                    AppBackground(imageRes = R.drawable.woodenwall_1) {
                        // The sign overlaps SIGN_HEADER_HEIGHT/2 *into* this surface (see
                        // class doc) — content must clear that plus its own breathing room,
                        // not just a flat "looks about right" value, or it renders half
                        // hidden under the sign.
                        val contentTopPadding = if (headerImageRes != null) {
                            SIGN_HEADER_HEIGHT / 2 + 20.dp
                        } else {
                            64.dp
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(
                                    top = contentTopPadding,
                                    start = 20.dp,
                                    end = 20.dp,
                                    bottom = 24.dp,
                                ),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            if (headerImageRes == null) {
                                Text(
                                    text = lastTitle.orEmpty(),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                Spacer(Modifier.height(16.dp))
                            }
                            content()
                        }
                    }
                }

                if (headerImageRes != null) {
                    Image(
                        painter = painterResource(headerImageRes),
                        contentDescription = lastTitle,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = 8.dp)
                            .width(SIGN_HEADER_WIDTH)
                            .aspectRatio(SIGN_ASPECT_RATIO),
                    )
                }

                // Sized and positioned like the sign header: same height, aligned to
                // the very top of this (taller) Box so it straddles the card edge the
                // same way — top half over the scrim, bottom half over the surface.
                CloseButton(
                    onClick = onDismiss,
                    size = SIGN_HEADER_HEIGHT,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 8.dp),
                )
            }
        }
    }
}

/** The default body for a section with no real content yet — see [SectionOverlayCard]'s [content] param. */
@Composable
fun ComingSoonPlaceholder() {
    Text(text = "Coming soon…", style = MaterialTheme.typography.bodyLarge)
}
