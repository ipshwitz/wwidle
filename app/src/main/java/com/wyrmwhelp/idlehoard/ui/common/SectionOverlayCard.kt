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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.wyrmwhelp.idlehoard.ui.menu.SIGN_ASPECT_RATIO
import com.wyrmwhelp.idlehoard.ui.menu.floatingMenuItems

/** Fixed size for the overlapping sign header — see [SectionOverlayCard]. */
private val SIGN_HEADER_WIDTH = 240.dp

/** ≈ SIGN_HEADER_WIDTH / SIGN_ASPECT_RATIO (1672:941), as a fixed value so the
 * card surface below can reserve exactly half of it as a top inset. */
private val SIGN_HEADER_HEIGHT = 135.dp

/**
 * A card that slides up from the bottom to cover most of the screen, with a
 * scrim behind it and a close button in its top-right corner — used for every
 * `FloatingMenu` section instead of navigating to a separate full screen, so
 * the game underneath is never actually left. [title] drives both visibility
 * (non-null = shown) and content; the last non-null value is retained while
 * animating out so the card doesn't go blank mid-exit.
 *
 * The header reuses whichever `floatingMenuItems` entry matches [title] — the
 * same wooden-sign art shown on that item in the menu — and straddles the
 * card's top edge like a hanging plaque: the card surface is inset from the
 * top by half the sign's height, and the sign sits at the very top of the
 * (taller) surrounding box, so its top half reads as "outside" the card over
 * the scrim and its bottom half overlaps the card surface. Sections without
 * art yet (e.g. Settings) fall back to a plain bold title with no overlap.
 */
@Composable
fun SectionOverlayCard(title: String?, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
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
                .fillMaxHeight(0.85f),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Surface(
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .padding(top = if (headerImageRes != null) SIGN_HEADER_HEIGHT / 2 else 0.dp)
                        .fillMaxSize(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                top = if (headerImageRes != null) 24.dp else 56.dp,
                                start = 16.dp,
                                end = 16.dp,
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
                        Text(
                            text = "Coming soon…",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }

                if (headerImageRes != null) {
                    Image(
                        painter = painterResource(headerImageRes),
                        contentDescription = lastTitle,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .width(SIGN_HEADER_WIDTH)
                            .aspectRatio(SIGN_ASPECT_RATIO),
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }
        }
    }
}
