package dev.psychocat.catclicker.ui.minigames

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.psychocat.catclicker.assets.gameImage
import dev.psychocat.catclicker.game.data.Cat
import dev.psychocat.catclicker.game.data.Cats
import dev.psychocat.catclicker.game.data.Room
import dev.psychocat.catclicker.game.format.Numbers
import dev.psychocat.catclicker.game.minigames.RoundStatus
import dev.psychocat.catclicker.game.minigames.match3.MATCH3_SIZE
import dev.psychocat.catclicker.game.minigames.match3.Match3Board
import dev.psychocat.catclicker.game.minigames.match3.Match3Game
import dev.psychocat.catclicker.game.minigames.match3.Match3Round
import dev.psychocat.catclicker.game.model.GameMode
import dev.psychocat.catclicker.ui.components.BigButton
import dev.psychocat.catclicker.ui.components.ButtonStyle
import dev.psychocat.catclicker.ui.theme.CardBorder

/**
 * "Cats in a row": pick a cat, then a neighbour; if the swap makes a line of three or more the move counts.
 * A swap that makes no line is refused without costing a move. 20 moves, no timer.
 */
@Composable
fun Match3Screen(
    room: Room,
    round: Match3Round,
    onSwap: (Int, Int) -> Unit,
    onPlayAgain: () -> Unit,
    onExit: () -> Unit,
) {
    var selected by rememberSaveable { mutableStateOf<Int?>(null) }
    var message by rememberSaveable { mutableStateOf("Выберите котика, затем коснитесь соседнего.") }
    val reward = Match3Game.fishReward(round.score, round.roomId, round.mode)
    val catsById = Cats.forRoom(round.roomId).associateBy { it.id }

    // A finished turn always clears the selection and tells the player what they got.
    LaunchedEffect(round.id, round.score, round.lastGain, round.lastCombo, round.shuffled) {
        selected = null
        if (round.lastGain > 0) {
            val combo = if (round.lastCombo > 1) " Комбо ×${round.lastCombo}!" else ""
            val shuffle = if (round.shuffled) " Поле мягко перемешано: все ходы снова доступны." else ""
            message = "Получено ${Numbers.format(round.lastGain.toDouble())} очков.$combo$shuffle"
        }
    }

    fun chooseTile(index: Int) {
        if (round.status != RoundStatus.PLAYING) return
        val current = selected
        when {
            current == null -> {
                selected = index
                message = "Теперь выберите соседнего котика."
            }
            current == index -> {
                selected = null
                message = "Выбор отменён. Можно выбрать другого котика."
            }
            !Match3Board.areAdjacent(current, index) -> {
                selected = index
                message = "Выбран новый котик. Теперь коснитесь соседнего."
            }
            !Match3Board.swapCreatesMatch(round.board, current, index) -> {
                selected = null
                message = "Ряд не получился, но ход не потрачен. Попробуйте другую пару."
            }
            else -> {
                onSwap(current, index)
                selected = null
            }
        }
    }

    MiniGameFrame(room) { landscape, width, height ->
        val stats: @Composable () -> Unit = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniStat("Ходы", "${round.movesLeft}", Modifier.weight(1f))
                MiniStat("Очки", Numbers.format(round.score.toDouble()), Modifier.weight(1f))
                MiniStat(
                    "Награда", Numbers.format(reward), Modifier.weight(1f),
                    icon = { Image(gameImage("resource_01"), contentDescription = null, modifier = Modifier.size(22.dp)) },
                )
            }
        }
        val notes: @Composable () -> Unit = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Рамка показывает выбранного котика. Играйте без спешки — таймера нет. Незавершённый раунд сохранится.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    if (round.mode == GameMode.EXPERT) "Режим «Эксперт»: награда увеличена." else "Мини-игра необязательна и не влияет на открытие комнат.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (landscape) {
            val boardSide = minOf(height - 24.dp, width * 0.55f)
            Row(modifier = Modifier.fillMaxSize().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Match3BoardView(round, catsById, selected, Modifier.size(boardSide).align(Alignment.CenterVertically), ::chooseTile)
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    MiniGameTitle(room, "Котики в ряд", onExit, exitLabel = "Закончить")
                    stats()
                    Text(message, style = MaterialTheme.typography.titleSmall)
                    notes()
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 6.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MiniGameTitle(room, "Котики в ряд", onExit, exitLabel = "Закончить")
                stats()
                Text(message, style = MaterialTheme.typography.titleSmall)
                Match3BoardView(round, catsById, selected, Modifier.fillMaxWidth(), ::chooseTile)
                notes()
            }
        }
    }

    if (round.status == RoundStatus.FINISHED) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Отличная работа!") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Набрано ${Numbers.format(round.score.toDouble())} очков.")
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

/** The 7x7 board. Every tile keeps its id while it falls, so it slides to its new cell instead of jumping. */
@Composable
private fun Match3BoardView(
    round: Match3Round,
    cats: Map<String, Cat>,
    selected: Int?,
    modifier: Modifier,
    onTap: (Int) -> Unit,
) {
    BoxWithConstraints(
        modifier = modifier.aspectRatio(1f)
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFFF6E0BF), Color(0xFFD9AE84))))
            .border(BorderStroke(4.dp, Color(0xFF87533D)), RoundedCornerShape(18.dp))
            .padding(4.dp)
            .semantics { contentDescription = "Поле семь на семь" },
    ) {
        val cell = maxWidth / MATCH3_SIZE
        round.board.forEachIndexed { index, tile ->
            key(tile.id) {
                val cat = cats[tile.catId]
                val x by animateDpAsState(cell * (index % MATCH3_SIZE), tween(220), label = "tileX")
                val y by animateDpAsState(cell * (index / MATCH3_SIZE), tween(220), label = "tileY")
                val isSelected = selected == index
                Box(
                    modifier = Modifier
                        .offset(x = x, y = y)
                        .size(cell)
                        .padding(2.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (cat != null) catTint(cat) else Color(0xFFFFF4E0))
                        .border(
                            if (isSelected) BorderStroke(4.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, CardBorder),
                            RoundedCornerShape(10.dp),
                        )
                        .semantics {
                            contentDescription = "${cat?.name ?: "Котик"}, ряд ${index / MATCH3_SIZE + 1}, столбец ${index % MATCH3_SIZE + 1}" +
                                if (isSelected) ", выбран" else ""
                        }
                        .clickable(role = Role.Button) { onTap(index) },
                ) {
                    if (cat != null) {
                        Image(
                            painter = gameImage(cat.image),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize().padding(2.dp),
                        )
                    }
                }
            }
        }
    }
}
