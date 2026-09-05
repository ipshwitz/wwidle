package com.wyrmwhelp.idlehoard.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.wyrmwhelp.idlehoard.R

/**
 * A backdrop image behind a 50%-opacity white overlay (so it stays
 * atmospheric without competing with content) — the app's shared visual
 * treatment for any full-bleed background. Defaults to the fantasy landscape
 * art used by `GameScreen`; pass a different [imageRes] for other surfaces
 * (e.g. `SectionOverlayCard` uses the wooden-wall art instead).
 */
@Composable
fun AppBackground(
    modifier: Modifier = Modifier,
    imageRes: Int = R.drawable.main_bg,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Image(
            painter = painterResource(imageRes),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White.copy(alpha = 0.5f)),
        )
        content()
    }
}
