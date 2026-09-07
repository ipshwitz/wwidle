package com.wyrmwhelp.idlehoard.ui.helpsocial

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wyrmwhelp.idlehoard.ui.common.FantasyPalette

/** Every "Help & Social" link, in display order (top of the list first). */
enum class SocialPlatform { FACEBOOK, INSTAGRAM, X, TIKTOK, WHATNOT, WEBSITE, EMAIL }

/** One row's worth of data — [url] is what [HelpSocialContent]'s [onOpenLink] is called with. */
data class SocialLink(
    val platform: SocialPlatform,
    val label: String,
    val handle: String,
    val url: String,
)

/** The game's real social/contact presence — kept as one list so adding a platform is one line. */
val SOCIAL_LINKS: List<SocialLink> = listOf(
    SocialLink(SocialPlatform.FACEBOOK, "Facebook", "@wyrmandwhelp", "https://www.facebook.com/wyrmandwhelp"),
    SocialLink(SocialPlatform.INSTAGRAM, "Instagram", "@wyrmandwhelp", "https://www.instagram.com/wyrmandwhelp"),
    SocialLink(SocialPlatform.X, "X", "@wyrmandwhelp", "https://x.com/wyrmandwhelp"),
    SocialLink(SocialPlatform.TIKTOK, "TikTok", "@wyrmandwhelp", "https://www.tiktok.com/@wyrmandwhelp"),
    SocialLink(SocialPlatform.WHATNOT, "Whatnot", "@wyrmandwhelp", "https://www.whatnot.com/user/wyrmandwhelp"),
    SocialLink(SocialPlatform.WEBSITE, "Website", "wyrmandwhelp.com", "https://www.wyrmandwhelp.com"),
    SocialLink(SocialPlatform.EMAIL, "Email Us", "support@wyrmandwhelp.com", "mailto:support@wyrmandwhelp.com"),
)

/**
 * The "Help & Social" section's real content — a list of the game's actual
 * social accounts, website, and support email ([SOCIAL_LINKS]), each row
 * showing a hand-drawn brand-styled icon (see [SocialIcon]) rather than a
 * generic link glyph. Pure display plus one callback, same pattern as
 * `StewardsContent`/`ShopContent`: tapping a row calls [onOpenLink] with
 * that link's raw URL (an `https://` link or an `mailto:` one for email) —
 * `MainActivity` is what actually fires the `Intent`, since resolving one
 * needs a `Context` this composable has no reason to hold.
 */
@Composable
fun HelpSocialContent(
    onOpenLink: (String) -> Unit,
    modifier: Modifier = Modifier,
    palette: FantasyPalette = FantasyPalette.Default,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            IntroCard(palette = palette)
            Spacer(Modifier.height(4.dp))
        }
        items(SOCIAL_LINKS, key = { it.platform }) { link ->
            SocialLinkRow(link = link, onClick = { onOpenLink(link.url) }, palette = palette)
        }
    }
}

/** A translucent parchment card matching `LairCard`'s base treatment, not a Material `Surface`. */
@Composable
private fun ParchmentCard(
    palette: FantasyPalette,
    modifier: Modifier = Modifier,
    borderColor: Color = palette.woodDark.copy(alpha = 0.5f),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.verticalGradient(
                    listOf(palette.parchmentShade.copy(alpha = 0.8f), palette.parchment.copy(alpha = 0.8f)),
                ),
            )
            .border(1.5.dp, borderColor, RoundedCornerShape(10.dp))
            .padding(12.dp),
        content = content,
    )
}

@Composable
private fun IntroCard(palette: FantasyPalette, modifier: Modifier = Modifier) {
    ParchmentCard(palette = palette, modifier = modifier) {
        Text(
            text = "Help & Social",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Serif, color = palette.ink),
        )
        Text(
            text = "Follow the hoard, catch our streams, or reach out if something's broken.",
            style = MaterialTheme.typography.bodySmall,
            color = palette.ink.copy(alpha = 0.8f),
        )
    }
}

/** One social/contact link: a brand-styled [SocialIcon], its label/handle, the whole row tappable. */
@Composable
private fun SocialLinkRow(link: SocialLink, onClick: () -> Unit, palette: FantasyPalette, modifier: Modifier = Modifier) {
    ParchmentCard(palette = palette, modifier = modifier.clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SocialIcon(platform = link.platform, modifier = Modifier.size(44.dp))
            Column {
                Text(
                    text = link.label,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Serif, color = palette.ink),
                )
                Text(
                    text = link.handle,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.ink.copy(alpha = 0.7f),
                )
            }
        }
    }
}

/**
 * A brand-colored circular icon per [SocialPlatform] — hand-drawn via
 * `Canvas` (this project's stated style, "Canvas for animation, no sprite
 * pack") rather than a new drawable asset. Facebook/X/Whatnot render as
 * simple bold letterforms, which is literally accurate for two of the
 * three (X's real logo is just a stylized "X"; Facebook's is a lowercase
 * "f" in a circle) rather than an approximation. Instagram and TikTok get
 * real hand-drawn `Path` glyphs (a camera outline, a musical note with the
 * signature color-offset silhouette) since those two are recognized by
 * shape more than by letter. Website/Email use the app's own
 * [FantasyPalette] wood/gold tones instead of a brand color, since
 * they're first-party links, not other platforms.
 */
@Composable
private fun SocialIcon(platform: SocialPlatform, modifier: Modifier = Modifier, palette: FantasyPalette = FantasyPalette.Default) {
    when (platform) {
        SocialPlatform.FACEBOOK -> LetterIcon(text = "f", background = SolidBrush(Color(0xFF1877F2)), modifier = modifier)
        SocialPlatform.X -> LetterIcon(text = "X", background = SolidBrush(Color(0xFF000000)), modifier = modifier)
        SocialPlatform.WHATNOT -> LetterIcon(text = "W", background = SolidBrush(Color(0xFF6C4FF6)), modifier = modifier)
        SocialPlatform.INSTAGRAM -> Box(modifier) { InstagramIcon(modifier = Modifier.size(44.dp)) }
        SocialPlatform.TIKTOK -> Box(modifier) { TikTokIcon(modifier = Modifier.size(44.dp)) }
        SocialPlatform.WEBSITE -> GlobeIcon(palette = palette, modifier = modifier)
        SocialPlatform.EMAIL -> EnvelopeIcon(palette = palette, modifier = modifier)
    }
}

/** A flat color, wrapped so [LetterIcon] can take either a solid color or (in principle) a gradient. */
private fun SolidBrush(color: Color): Brush = Brush.linearGradient(listOf(color, color))

/** A bold single-letter monogram on a colored circle — used where the platform's real logo is a letterform. */
@Composable
private fun LetterIcon(text: String, background: Brush, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.size(44.dp)) {
            drawCircle(brush = background, radius = size.minDimension / 2f)
        }
        Text(
            text = text,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

/** Instagram's rounded-square camera outline with a lens circle and shutter dot, on the real brand gradient. */
@Composable
private fun InstagramIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFFFEDA75), Color(0xFFFA7E1E), Color(0xFFD62976), Color(0xFF962FBF), Color(0xFF4F5BD5)),
            ),
            radius = radius,
            center = center,
        )
        val bodySize = radius * 1.1f
        val topLeft = Offset(center.x - bodySize / 2f, center.y - bodySize / 2f)
        drawRoundRect(
            color = Color.White,
            topLeft = topLeft,
            size = Size(bodySize, bodySize),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(bodySize * 0.28f),
            style = Stroke(width = radius * 0.14f),
        )
        drawCircle(color = Color.White, radius = radius * 0.32f, center = center, style = Stroke(width = radius * 0.14f))
        drawCircle(color = Color.White, radius = radius * 0.07f, center = Offset(topLeft.x + bodySize * 0.82f, topLeft.y + bodySize * 0.18f))
    }
}

/** TikTok's musical-note glyph with the signature cyan/magenta offset shadow, on a black disc. */
@Composable
private fun TikTokIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(color = Color.Black, radius = radius, center = center)

        val noteWidth = radius * 0.34f
        val noteHeight = radius * 1.05f
        val noteTop = Offset(center.x + radius * 0.08f, center.y - noteHeight / 2f)

        fun notePath(offset: Offset): Path = Path().apply {
            val x = noteTop.x + offset.x
            val y = noteTop.y + offset.y
            moveTo(x, y)
            lineTo(x, y + noteHeight * 0.72f)
            addOval(androidx.compose.ui.geometry.Rect(x - noteWidth, y + noteHeight * 0.58f, x + noteWidth * 0.1f, y + noteHeight))
            moveTo(x, y)
            cubicTo(x, y, x + noteWidth * 1.6f, y + noteHeight * 0.08f, x + noteWidth * 1.6f, y + noteHeight * 0.42f)
            lineTo(x + noteWidth * 1.1f, y + noteHeight * 0.42f)
            cubicTo(x + noteWidth * 1.1f, y + noteHeight * 0.22f, x, y + noteHeight * 0.15f, x, y)
            close()
        }

        drawPath(notePath(Offset(-radius * 0.09f, radius * 0.06f)), color = Color(0xFF25F4EE))
        drawPath(notePath(Offset(radius * 0.09f, -radius * 0.06f)), color = Color(0xFFFE2C55))
        drawPath(notePath(Offset.Zero), color = Color.White)
    }
}

/** A simple carved globe — meridian lines over a ringed circle — for the game's own website link. */
@Composable
private fun GlobeIcon(palette: FantasyPalette, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(44.dp)) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(brush = Brush.radialGradient(listOf(palette.woodLight, palette.woodDark), center = center, radius = radius), radius = radius, center = center)
        val strokeWidth = radius * 0.08f
        drawCircle(color = palette.parchment, radius = radius * 0.68f, center = center, style = Stroke(width = strokeWidth))
        drawOval(
            color = palette.parchment,
            topLeft = Offset(center.x - radius * 0.3f, center.y - radius * 0.68f),
            size = Size(radius * 0.6f, radius * 1.36f),
            style = Stroke(width = strokeWidth * 0.7f),
        )
        drawLine(
            color = palette.parchment,
            start = Offset(center.x - radius * 0.68f, center.y),
            end = Offset(center.x + radius * 0.68f, center.y),
            strokeWidth = strokeWidth * 0.7f,
            cap = StrokeCap.Round,
        )
    }
}

/** A simple carved envelope for the support-email link. */
@Composable
private fun EnvelopeIcon(palette: FantasyPalette, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(44.dp)) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(brush = Brush.radialGradient(listOf(palette.woodLight, palette.woodDark), center = center, radius = radius), radius = radius, center = center)

        val envelopeWidth = radius * 1.3f
        val envelopeHeight = radius * 0.95f
        val topLeft = Offset(center.x - envelopeWidth / 2f, center.y - envelopeHeight / 2f)
        val strokeWidth = radius * 0.09f
        drawRoundRect(
            color = palette.parchment,
            topLeft = topLeft,
            size = Size(envelopeWidth, envelopeHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius * 0.14f),
            style = Stroke(width = strokeWidth),
        )
        val flap = Path().apply {
            moveTo(topLeft.x, topLeft.y)
            lineTo(center.x, center.y + envelopeHeight * 0.08f)
            lineTo(topLeft.x + envelopeWidth, topLeft.y)
        }
        drawPath(flap, color = palette.parchment, style = Stroke(width = strokeWidth))
    }
}
