package com.wyrmwhelp.idlehoard.ui.menu

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wyrmwhelp.idlehoard.R

/** One entry in the floating menu. [imageRes] is null for sections without art yet. */
data class MenuItem(val label: String, val imageRes: Int? = null)

/** The floating menu's sections, in display order (top of the stack first). */
val floatingMenuItems: List<MenuItem> = listOf(
    MenuItem("Help & Social", R.drawable.menu_help_social),
    MenuItem("Unlocks", R.drawable.menu_unlocks),
    MenuItem("Upgrades", R.drawable.menu_upgrades),
    MenuItem("Stewards", R.drawable.menu_stewards),
    MenuItem("Level Up", R.drawable.menu_level_up),
    MenuItem("Shop"),
    MenuItem("Settings"),
)

/**
 * Wooden sign art is 1626x536 (tightly cropped to the sign shape itself, no
 * padding — the original exports had transparent margins baked into the
 * canvas, which silently threw off every size derived from this ratio until
 * they were recropped) — used to size each plank (and, in
 * `SectionOverlayCard`, each section header) without distorting it.
 */
const val SIGN_ASPECT_RATIO = 1626f / 536f

/**
 * A hamburger-style toggle fixed at the bottom of the screen that expands
 * upward into a vertical stack of tappable sections — evoking the wooden
 * trail signpost in the background art. The toggle is a plain
 * (transparent-background) `IconButton`, not a Material `FloatingActionButton`
 * — a FAB always draws its own solid container/shadow, which would show as a
 * box behind the chest art instead of letting the art float directly on the
 * background. It swaps between `closed_chest`/`open_chest` art depending on
 * [expanded]. Each `floatingMenuItems` entry with an [MenuItem.imageRes] renders
 * as that wooden-sign art directly (no extra container — the sign image already
 * is one); entries without art yet (currently Shop and Settings) fall back
 * to a plain labeled surface until their own art exists.
 */
@Composable
fun FloatingMenu(onItemSelected: (String) -> Unit, modifier: Modifier = Modifier) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { expanded = false },
                    ),
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
                exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.padding(bottom = 12.dp),
                ) {
                    floatingMenuItems.forEach { item ->
                        MenuItemPlank(
                            item = item,
                            onClick = {
                                expanded = false
                                onItemSelected(item.label)
                            },
                        )
                    }
                }
            }

            IconButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.size(72.dp),
            ) {
                Image(
                    painter = painterResource(
                        if (expanded) R.drawable.open_chest else R.drawable.closed_chest,
                    ),
                    contentDescription = if (expanded) "Close menu" else "Open menu",
                    modifier = Modifier.size(64.dp),
                )
            }
        }
    }
}

@Composable
private fun MenuItemPlank(item: MenuItem, onClick: () -> Unit) {
    if (item.imageRes != null) {
        Image(
            painter = painterResource(item.imageRes),
            contentDescription = item.label,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .width(200.dp)
                .aspectRatio(SIGN_ASPECT_RATIO)
                .clickable(onClick = onClick),
        )
    } else {
        Surface(
            onClick = onClick,
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            tonalElevation = 4.dp,
            shadowElevation = 4.dp,
            modifier = Modifier.widthIn(min = 170.dp),
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = item.label, fontWeight = FontWeight.Bold)
            }
        }
    }
}
