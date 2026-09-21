package dev.psychocat.catclicker.ui.room

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.psychocat.catclicker.assets.gameImage
import dev.psychocat.catclicker.game.data.Rooms
import dev.psychocat.catclicker.game.engine.Economy
import dev.psychocat.catclicker.game.format.Numbers
import dev.psychocat.catclicker.game.model.GameMode
import dev.psychocat.catclicker.game.model.GameState
import dev.psychocat.catclicker.ui.components.BigButton
import dev.psychocat.catclicker.ui.components.ButtonStyle
import dev.psychocat.catclicker.ui.theme.CreamCard
import dev.psychocat.catclicker.ui.theme.CardBorder
import dev.psychocat.catclicker.ui.theme.FishBorder
import dev.psychocat.catclicker.ui.theme.FishCard
import dev.psychocat.catclicker.ui.theme.HungerFill
import dev.psychocat.catclicker.ui.theme.HungerTrack
import kotlin.math.ceil

/** Landscape layout: everything in one block. */
@Composable
fun Hud(state: GameState, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ModeLabel(state)
        FishHud(state)
        RatesHud(state)
    }
}

/** The fish counter alone: small enough to stay pinned to the top of the screen while the player shops. */
@Composable
fun FishHud(state: GameState, modifier: Modifier = Modifier) {
    StatCard(
        label = "Рыбки комнаты", value = Numbers.format(state.progress.fish), modifier = modifier.fillMaxWidth(),
        emphasized = true, icon = { Image(gameImage("resource_01"), contentDescription = null, modifier = Modifier.size(40.dp)) },
    )
}

/** Income per tap and per second. The reset button of the web version deliberately does not live here. */
@Composable
fun RatesHud(state: GameState, modifier: Modifier = Modifier) {
    val income = Economy.fishPerSecond(state) + Economy.safetyIncome(state)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        StatCard(label = "За клик", value = "+" + Numbers.format(Economy.currentClickReward(state)), modifier = Modifier.weight(1f), large = false)
        StatCard(label = "В секунду", value = Numbers.format(income), modifier = Modifier.weight(1f), large = false)
    }
}

@Composable
fun ModeLabel(state: GameState, modifier: Modifier = Modifier) {
    Text(
        (if (state.mode == GameMode.EXPERT) "Режим «Эксперт»" else "Обычный режим") + " · " + Rooms.byId(state.currentRoom).name,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier,
    )
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
    large: Boolean = true,
    icon: (@Composable () -> Unit)? = null,
) {
    Surface(
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(16.dp),
        color = if (emphasized) FishCard else CreamCard,
        border = BorderStroke(1.dp, if (emphasized) FishBorder else CardBorder),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            icon?.invoke()
            Column(verticalArrangement = Arrangement.Center) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                Text(value, style = if (large) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium, maxLines = 1)
            }
        }
    }
}

/** Satiety bar, the light switch and the caviar boost. */
@Composable
fun HungerPanel(state: GameState, onToggleLights: () -> Unit, modifier: Modifier = Modifier) {
    val progress = state.progress
    val minutes = ceil(progress.hunger / 100 * Economy.hungerDuration(state.mode) / 60).toInt()
    val status = when {
        progress.hunger <= 0 -> "Кот голоден"
        progress.lightsOff -> "${ceil(progress.hunger).toInt()}% · пауза"
        else -> "${ceil(progress.hunger).toInt()}% · ~$minutes мин"
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = CreamCard,
        border = BorderStroke(1.dp, CardBorder),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Сытость кота", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Text(status, style = MaterialTheme.typography.titleSmall)
            }
            Box(
                modifier = Modifier.fillMaxWidth().height(22.dp).clip(RoundedCornerShape(11.dp)).background(HungerTrack)
                    .semantics {
                        contentDescription = "Сытость кота"
                        progressBarRangeInfo = ProgressBarRangeInfo(progress.hunger.toFloat().coerceIn(0f, 100f), 0f..100f)
                    },
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth((progress.hunger / 100).toFloat().coerceIn(0f, 1f)).fillMaxHeight()
                        .background(HungerFill),
                )
            }
            BigButton(
                text = if (progress.lightsOff) "Включить свет" else "Выключить свет",
                onClick = onToggleLights,
                enabled = progress.hunger > 0,
                style = ButtonStyle.Tonal,
                modifier = Modifier.fillMaxWidth(),
            )
            if (progress.hunger <= 0) {
                Text("Сначала покормите кота: раздел «Корм».", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (progress.caviarSeconds > 0) {
                Text(
                    "Икра: рыбок за клик ×2 · ещё ${ceil(progress.caviarSeconds).toInt()} с",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
