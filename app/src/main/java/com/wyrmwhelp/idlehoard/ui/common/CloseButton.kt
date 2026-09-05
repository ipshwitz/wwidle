package com.wyrmwhelp.idlehoard.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wyrmwhelp.idlehoard.R

/**
 * The app's standard close control: crossed swords in a wooden ring
 * (`x.png`), not a Material icon glyph — use this for every "close this
 * overlay/dialog" affordance going forward (e.g. `SectionOverlayCard`) so it
 * stays consistent rather than mixing in `Icons.Default.Close` again. A plain
 * clickable `Image`, not `IconButton` — `IconButton` clips its content to its
 * own fixed 40dp touch-target box, which crops any [size] larger than that
 * (needed when a caller sizes this to match a header sign).
 */
@Composable
fun CloseButton(onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = 32.dp) {
    Image(
        painter = painterResource(R.drawable.x),
        contentDescription = "Close",
        modifier = modifier
            .size(size)
            .clickable(onClick = onClick),
    )
}
