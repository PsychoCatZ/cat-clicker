package dev.psychocat.catclicker.ui.minigames

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.psychocat.catclicker.assets.gameImage
import dev.psychocat.catclicker.game.data.Cat
import dev.psychocat.catclicker.game.data.Cats
import dev.psychocat.catclicker.game.data.Room
import dev.psychocat.catclicker.game.format.Numbers
import dev.psychocat.catclicker.game.minigames.RoundStatus
import dev.psychocat.catclicker.game.minigames.pairs.PairCard
import dev.psychocat.catclicker.game.minigames.pairs.PairsGame
import dev.psychocat.catclicker.game.minigames.pairs.PairsRound
import dev.psychocat.catclicker.game.model.GameMode
import dev.psychocat.catclicker.ui.components.BigButton
import dev.psychocat.catclicker.ui.components.ButtonStyle
import dev.psychocat.catclicker.ui.theme.Success
import kotlinx.coroutines.delay

/**
 * "Find the pair": open two cards, find the two identical cats. No timer for the player: the only pause is the
 * short moment two different cards stay face up so the player can memorise them, and a tap ends it sooner.
 */
@Composable
fun PairsScreen(
    room: Room,
    round: PairsRound,
    onReveal: (Int) -> Unit,
    onHideMismatch: () -> Unit,
    onPlayAgain: () -> Unit,
    onExit: () -> Unit,
) {
    val reward = PairsGame.fishReward(round)
    val catsById = Cats.forRoom(round.roomId).associateBy { it.id }

    // Two different cards turn back by themselves; the same effect also restarts after the app was reopened
    // in the middle of a mismatch, because the state is saved with the cards face up.
    LaunchedEffect(round.revealed, round.lastMatch) {
        if (round.showingMismatch) {
            delay(PairsGame.MISMATCH_SHOW_MILLIS)
            onHideMismatch()
        }
    }

    val message = when {
        round.lastMatch == true -> "Пара найдена! Продолжайте."
        round.showingMismatch -> "Не совпали — запомните карточки. Коснитесь, чтобы продолжить."
        round.revealed.size == 1 -> "Теперь откройте вторую карточку."
        else -> "Откройте две карточки и найдите одинаковых котиков."
    }

    fun tap(cardId: Int) {
        if (round.showingMismatch) onHideMismatch() else onReveal(cardId)
    }

    MiniGameFrame(room) { landscape, _, _ ->
        val stats: @Composable () -> Unit = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniStat("Найдено", "${round.matches} / ${round.totalPairs}", Modifier.weight(1f))
                MiniStat("Попытки", Numbers.format(round.attempts.toDouble()), Modifier.weight(1f))
                MiniStat(
                    "Награда", Numbers.format(reward), Modifier.weight(1f),
                    icon = { Image(gameImage("resource_01"), contentDescription = null, modifier = Modifier.size(22.dp)) },
                )
            }
        }
        val notes: @Composable () -> Unit = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(message, style = MaterialTheme.typography.titleSmall)
                Text(
                    "${round.totalPairs} пар, без таймера. Незавершённый раунд сохранится автоматически.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    if (round.mode == GameMode.EXPERT) "Режим «Эксперт»: награда увеличена." else "Мини-игра необязательна и не влияет на открытие комнат.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (landscape) {
            Row(modifier = Modifier.fillMaxSize().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(modifier = Modifier.weight(1.5f).verticalScroll(rememberScrollState())) {
                    PairsBoard(round, catsById, columns = if (round.cardCount == 10) 5 else 8, onTap = ::tap)
                }
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    MiniGameTitle(room, "Найди пару", onExit, exitLabel = "Закончить")
                    stats()
                    notes()
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MiniGameTitle(room, "Найди пару", onExit, exitLabel = "Закончить")
                stats()
                Text(message, style = MaterialTheme.typography.titleSmall)
                PairsBoard(round, catsById, columns = 4, onTap = ::tap)
                Text(
                    "${round.totalPairs} пар, без таймера. Незавершённый раунд сохранится автоматически.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (round.status == RoundStatus.FINISHED) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Прекрасная память!") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Все пары найдены. Понадобилось попыток: ${Numbers.format(round.attempts.toDouble())}.")
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Image(gameImage("resource_01"), contentDescription = null, modifier = Modifier.size(28.dp))
                        Text("Награда: ${Numbers.format(reward)} рыбок", style = MaterialTheme.typography.titleSmall)
                    }
                }
            },
            confirmButton = { BigButton("Сыграть ещё", onPlayAgain) },
            dismissButton = { BigButton("Получить рыбки", onExit, style = ButtonStyle.Tonal) },
        )
    }
}

/** Rows of cards; a shorter last row is centred. */
@Composable
private fun PairsBoard(round: PairsRound, cats: Map<String, Cat>, columns: Int, onTap: (Int) -> Unit) {
    val gap = 8.dp
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val cardWidth: Dp = (maxWidth - gap * (columns - 1)) / columns
        Column(
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Поле из ${round.cardCount} карточек" },
            verticalArrangement = Arrangement.spacedBy(gap),
        ) {
            round.cards.chunked(columns).forEachIndexed { rowIndex, rowCards ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(gap, Alignment.CenterHorizontally),
                ) {
                    rowCards.forEachIndexed { columnIndex, card ->
                        val number = rowIndex * columns + columnIndex + 1
                        val visible = card.matched || card.id in round.revealed
                        PairCardView(card, cats[card.catId], number, visible, Modifier.width(cardWidth)) { onTap(card.id) }
                    }
                }
            }
        }
    }
}

@Composable
private fun PairCardView(card: PairCard, cat: Cat?, number: Int, visible: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val rotation by animateFloatAsState(if (visible) 180f else 0f, tween(320), label = "flip")
    val faceUp = rotation > 90f
    Box(
        modifier = modifier
            .aspectRatio(0.78f)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 14f * density
            }
            .semantics {
                contentDescription = if (visible) "${cat?.name ?: "Котик"}, карточка $number" else "Закрытая карточка $number"
            }
            .clickable(enabled = !card.matched, role = Role.Button, onClick = onClick),
    ) {
        if (!faceUp) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primary,
                border = BorderStroke(3.dp, Color(0xFFE8A95C)),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("?", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }
            }
        } else {
            // The face is drawn mirrored by the flip, so it is turned once more to read correctly.
            Surface(
                modifier = Modifier.fillMaxSize().graphicsLayer { rotationY = 180f },
                shape = RoundedCornerShape(14.dp),
                color = if (cat != null) catTint(cat) else Color(0xFFFFF4E0),
                border = BorderStroke(if (card.matched) 4.dp else 3.dp, if (card.matched) Success else Color(0xFFE8A95C)),
            ) {
                if (cat != null) {
                    Image(
                        painter = gameImage(cat.image),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize().padding(4.dp),
                    )
                }
            }
        }
    }
}
