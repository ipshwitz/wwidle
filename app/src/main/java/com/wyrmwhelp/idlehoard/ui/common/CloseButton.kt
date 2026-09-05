package com.wyrmwhelp.idlehoard.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.wyrmwhelp.idlehoard.R

/**
 * The app's standard close control: crossed swords in a wooden ring
 * (`x.png`), not a Material icon glyph — use this for every "close this
 * overlay/dialog" affordance going forward (e.g. `SectionOverlayCard`) so it
 * stays consistent rather than mixing in `Icons.Default.Close` again.
 */
@Composable
fun CloseButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, modifier = modifier) {
        Image(
            painter = painterResource(R.drawable.x),
            contentDescription = "Close",
            modifier = Modifier.size(32.dp),
        )
    }
}
