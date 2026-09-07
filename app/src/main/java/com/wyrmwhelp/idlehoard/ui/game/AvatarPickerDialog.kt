package com.wyrmwhelp.idlehoard.ui.game

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.wyrmwhelp.idlehoard.R
import com.wyrmwhelp.idlehoard.domain.model.AVATAR_CATALOG
import com.wyrmwhelp.idlehoard.domain.model.AvatarGender
import com.wyrmwhelp.idlehoard.domain.model.AvatarOption
import com.wyrmwhelp.idlehoard.ui.common.CloseButton
import com.wyrmwhelp.idlehoard.ui.common.FantasyPalette
import com.wyrmwhelp.idlehoard.ui.common.avatarDrawableRes

/**
 * The avatar picker opened by tapping `GameHeader`'s `MedallionEmblem` — a
 * plain [Dialog] (same parchment-scroll chrome as `WelcomeBackDialog`) with
 * a scrollable grid of all 26 [AVATAR_CATALOG] portraits (see
 * `domain/model/Avatar.kt`), grouped by gender, plus a "Default" row to
 * revert to the placeholder shield. Tapping any tile selects it and closes
 * the dialog immediately — there's no separate "confirm" step, matching how
 * every other one-tap choice in this app behaves (e.g. `BuyQuantity`
 * cycling, a `FloatingMenu` item).
 */
@Composable
fun AvatarPickerDialog(
    selectedAvatarId: String?,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
    palette: FantasyPalette = FantasyPalette.Default,
) {
    val femaleOptions = AVATAR_CATALOG.filter { it.gender == AvatarGender.FEMALE }
    val maleOptions = AVATAR_CATALOG.filter { it.gender == AvatarGender.MALE }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .widthIn(min = 280.dp, max = 360.dp)
                .shadow(8.dp, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.verticalGradient(listOf(palette.parchmentShade, palette.parchment)))
                .border(2.dp, palette.woodDark, RoundedCornerShape(16.dp))
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Choose Your Avatar",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Serif, color = palette.ink),
                    fontWeight = FontWeight.Bold,
                )
                CloseButton(onClick = onDismiss, size = 28.dp)
            }
            Spacer(Modifier.height(8.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    DefaultAvatarRow(
                        selected = selectedAvatarId == null,
                        onClick = { onSelect(null) },
                        palette = palette,
                    )
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    GroupLabel(text = "Female", palette = palette)
                }
                items(femaleOptions, key = { it.id }) { option ->
                    AvatarTile(
                        option = option,
                        selected = option.id == selectedAvatarId,
                        onClick = { onSelect(option.id) },
                        palette = palette,
                    )
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    GroupLabel(text = "Male", palette = palette)
                }
                items(maleOptions, key = { it.id }) { option ->
                    AvatarTile(
                        option = option,
                        selected = option.id == selectedAvatarId,
                        onClick = { onSelect(option.id) },
                        palette = palette,
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupLabel(text: String, palette: FantasyPalette) {
    Text(
        text = text,
        modifier = Modifier.padding(top = 2.dp),
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Serif, color = palette.ink),
    )
}

/** One selectable portrait: a circular crop plus its class name, ringed gold when [selected]. */
@Composable
private fun AvatarTile(
    option: AvatarOption,
    selected: Boolean,
    onClick: () -> Unit,
    palette: FantasyPalette,
) {
    val portraitRes = avatarDrawableRes(option.id)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .fillMaxWidth()
                .clip(CircleShape)
                .border(
                    width = if (selected) 3.dp else 1.dp,
                    brush = if (selected) {
                        Brush.sweepGradient(listOf(palette.goldDeep, palette.goldBright, palette.goldDeep))
                    } else {
                        SolidColor(palette.woodDark.copy(alpha = 0.4f))
                    },
                    shape = CircleShape,
                ),
        ) {
            if (portraitRes != null) {
                Image(
                    painter = painterResource(portraitRes),
                    contentDescription = option.className,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Text(
            text = option.className,
            modifier = Modifier.padding(top = 2.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelSmall.copy(color = palette.ink),
        )
    }
}

/** The "revert to the placeholder shield" option — a full-width row rather than a grid tile since there's no portrait to show. */
@Composable
private fun DefaultAvatarRow(selected: Boolean, onClick: () -> Unit, palette: FantasyPalette) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) palette.goldBright.copy(alpha = 0.35f) else palette.parchmentShade.copy(alpha = 0.5f))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) palette.goldDeep else palette.woodDark.copy(alpha = 0.4f),
                shape = RoundedCornerShape(8.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(palette.woodLight, palette.woodDark))),
            contentAlignment = Alignment.Center,
        ) {
            Image(painter = painterResource(R.drawable.x), contentDescription = null, modifier = Modifier.size(18.dp))
        }
        Text(
            text = "Default (no avatar)",
            style = MaterialTheme.typography.bodyMedium.copy(color = palette.ink),
        )
    }
}
