package com.wyrmwhelp.idlehoard.ui.common

import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.wyrmwhelp.idlehoard.R

/**
 * Shown full-screen while `GameViewModel`'s initial load (local save,
 * sign-in, cloud-save merge, offline earnings) is still in flight — see
 * `GameViewModel.isLoading`. `loading_video.mp4` (`res/raw/`, copied from
 * `/assets/loading-video.mp4` — an older dragon and a baby dragon counting
 * Gold and Gems, 416x752/~9:16, 6s, 24fps) plays on loop and fills the
 * screen naturally since its aspect ratio already matches a phone's.
 *
 * Plain `VideoView` (`android.widget`) via `AndroidView` rather than pulling
 * in a Media3/ExoPlayer dependency for a single looping 6-second clip —
 * `VideoView` already handles playback/looping/aspect-fit on its own with
 * no new library. Muted regardless of whether the source has an audio
 * track (it does, going by its bitrate) — a loading screen playing
 * unexpected sound is jarring, and there's no music/SFX anywhere else in
 * the app yet to make an exception for.
 */
@Composable
fun LoadingScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.BottomCenter,
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                VideoView(context).apply {
                    setVideoURI(Uri.parse("android.resource://${context.packageName}/${R.raw.loading_video}"))
                    setOnPreparedListener { mediaPlayer ->
                        mediaPlayer.isLooping = true
                        mediaPlayer.setVolume(0f, 0f)
                    }
                    start()
                }
            },
        )
        Text(
            text = "Loading your hoard…",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = FontFamily.Serif,
                color = Color.White,
                shadow = Shadow(Color.Black.copy(alpha = 0.6f), blurRadius = 6f),
            ),
            modifier = Modifier.padding(bottom = 56.dp),
        )
    }
}
