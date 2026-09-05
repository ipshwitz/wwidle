package com.wyrmwhelp.idlehoard.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private const val COIN_COUNT = 12
private const val BURST_DURATION_MS = 650

private val COIN_FILL = Color(0xFFFFD700)
private val COIN_RIM = Color(0xFFB8860B)

private class CoinSpec(val angleRad: Float, val speedDp: Float, val radiusDp: Float)

/**
 * A one-shot radial burst of small gold coins, fired each time [trigger]
 * changes — an incrementing counter rather than a boolean, so a second
 * plunder fires its own burst even if the previous one hasn't finished
 * animating (each new value replays the whole effect via [key], which tears
 * down and re-launches the coroutine underneath). Renders nothing until the
 * first trigger, and nothing but plain `Canvas` draws otherwise — no pointer
 * input, so it never blocks clicks on whatever it's layered over (place it
 * as the topmost child of the card's `Box` so it draws over the content).
 */
@Composable
fun CoinBurstOverlay(trigger: Int, modifier: Modifier = Modifier) {
    if (trigger <= 0) return

    key(trigger) {
        val coins = remember {
            List(COIN_COUNT) {
                CoinSpec(
                    angleRad = Random.nextFloat() * (2f * Math.PI.toFloat()),
                    speedDp = Random.nextFloat() * 40f + 40f,
                    radiusDp = Random.nextFloat() * 3f + 4f,
                )
            }
        }
        val progress = remember { Animatable(0f) }
        LaunchedEffect(Unit) {
            progress.animateTo(1f, animationSpec = tween(BURST_DURATION_MS, easing = LinearEasing))
        }

        if (progress.value < 1f) {
            Canvas(modifier = modifier) {
                val t = progress.value
                val alpha = (1f - t).coerceIn(0f, 1f)
                val center = Offset(size.width / 2f, size.height / 2f)
                val gravityPx = 60.dp.toPx()
                coins.forEach { coin ->
                    val dx = cos(coin.angleRad) * coin.speedDp.dp.toPx() * t
                    val dy = sin(coin.angleRad) * coin.speedDp.dp.toPx() * t + gravityPx * t * t
                    val pos = center + Offset(dx, dy)
                    val radiusPx = coin.radiusDp.dp.toPx()
                    drawCircle(color = COIN_FILL, radius = radiusPx, center = pos, alpha = alpha)
                    drawCircle(
                        color = COIN_RIM,
                        radius = radiusPx * 0.85f,
                        center = pos,
                        alpha = alpha,
                        style = Stroke(width = radiusPx * 0.25f),
                    )
                }
            }
        }
    }
}
