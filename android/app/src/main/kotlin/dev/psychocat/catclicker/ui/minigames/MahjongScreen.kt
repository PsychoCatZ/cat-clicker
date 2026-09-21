package dev.psychocat.catclicker.ui.minigames

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.psychocat.catclicker.assets.gameImage
import dev.psychocat.catclicker.game.data.Cat
import dev.psychocat.catclicker.game.data.Cats
import dev.psychocat.catclicker.game.data.Room
import dev.psychocat.catclicker.game.format.Numbers
import dev.psychocat.catclicker.game.minigames.RoundStatus
import dev.psychocat.catclicker.game.minigames.mahjong.MahjongBoard
import dev.psychocat.catclicker.game.minigames.mahjong.MahjongEvent
import dev.psychocat.catclicker.game.minigames.mahjong.MahjongGame
import dev.psychocat.catclicker.game.minigames.mahjong.MahjongRound
import dev.psychocat.catclicker.game.minigames.mahjong.MahjongTile
import dev.psychocat.catclicker.game.model.GameMode
import dev.psychocat.catclicker.ui.components.BigButton
import dev.psychocat.catclicker.ui.components.ButtonStyle

/**
 * "Cat mahjong": remove pairs of equal free tiles. A tile is free when nothing lies on it and its left or right
 * side is open. No timer, no penalties, hints are free; a shuffle is offered only in a dead end.
 */
@Composable
fun MahjongScreen(
    room: Room,
    round: MahjongRound,
    onSelect: (Int) -> Unit,
    onHint: () -> Unit,
    onShuffle: () -> Unit,
    onPlayAgain: () -> Unit,
    onExit: () -> Unit,
) {
    val layout = round.layout
    val catsById = Cats.forRoom(round.roomId).associateBy { it.id }
    val availablePairs = MahjongBoard.findPairs(round.tiles)
    val activeCount = round.tiles.count { !it.removed }
    val playing = round.status == RoundStatus.PLAYING
    val reward = MahjongGame.fishReward(round.score, round.roomId, round.mode)
    val message = when {
        round.lastEvent == MahjongEvent.MISMATCH -> "Эти котики разные. Выбран новый свободный котик."
        round.lastEvent == MahjongEvent.MATCH -> "Пара найдена! Открылись новые фишки."
        round.lastEvent == MahjongEvent.HINT -> "Подсказка мягко подсветила доступную пару."
        round.lastEvent == MahjongEvent.SHUFFLED -> "Фишки перемешаны — поле снова проходимо."
        round.selectedId != null -> "Теперь выберите такую же свободную фишку."
        availablePairs.isEmpty() && activeCount > 0 -> "Доступных пар нет. Можно спокойно перемешать фишки."
        else -> "Выберите две одинаковые свободные фишки."
    }

    MiniGameFrame(room) { landscape, _, _ ->
        val summary: @Composable () -> Unit = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${round.difficulty.title} · ${layout.tileCount} фишек", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiniStat("Пары", "${round.pairsFound} / ${layout.tileCount / 2}", Modifier.weight(1f))
                    MiniStat("Очки", Numbers.format(round.score.toDouble()), Modifier.weight(1f))
                    MiniStat(
                        "Награда", Numbers.format(reward), Modifier.weight(1f),
                        icon = { Image(gameImage("resource_01"), contentDescription = null, modifier = Modifier.size(22.dp)) },
                    )
                }
            }
        }
        val controls: @Composable () -> Unit = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    BigButton(
                        "Подсказка", onHint, modifier = Modifier.weight(1f),
                        enabled = playing && availablePairs.isNotEmpty(), style = ButtonStyle.Tonal,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                    )
                    BigButton(
                        "Перемешать", onShuffle, modifier = Modifier.weight(1f),
                        enabled = playing && availablePairs.isEmpty() && activeCount > 0, style = ButtonStyle.Tonal,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                    )
                }
                Text(
                    "Подсказки: ${round.hintsUsed} · Перемешивания: ${round.shuffles}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        val notes: @Composable () -> Unit = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Свободная фишка не перекрыта сверху, а слева или справа от неё есть выход. Тёмные фишки пока заблокированы.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    if (round.mode == GameMode.EXPERT) "Режим «Эксперт»: котификация увеличена." else "Ошибки не штрафуются, таймера нет, подсказки бесплатны.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (landscape) {
            Row(modifier = Modifier.fillMaxSize().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(modifier = Modifier.weight(1.4f).verticalScroll(rememberScrollState())) {
                    MahjongBoardView(round, catsById, playing, onSelect, Modifier.fillMaxWidth())
                }
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    MiniGameTitle(room, "Кошачий маджонг", onExit)
                    summary()
                    Text(message, style = MaterialTheme.typography.titleSmall)
                    controls()
                    notes()
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 6.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(modifier = Modifier.padding(horizontal = 6.dp)) { MiniGameTitle(room, "Кошачий маджонг", onExit) }
                Box(modifier = Modifier.padding(horizontal = 6.dp)) { summary() }
                Box(modifier = Modifier.padding(horizontal = 6.dp)) { controls() }
                Text(message, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(horizontal = 6.dp))
                MahjongBoardView(round, catsById, playing, onSelect, Modifier.fillMaxWidth())
                Box(modifier = Modifier.padding(horizontal = 6.dp)) { notes() }
            }
        }
    }

    if (round.status == RoundStatus.FINISHED) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Все коты найдены!") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Найдено пар: ${round.pairsFound}")
                    Text("Набрано очков: ${Numbers.format(round.score.toDouble())}")
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Image(gameImage("resource_01"), contentDescription = null, modifier = Modifier.size(28.dp))
                        Text("Котифицировано: ${Numbers.format(reward)} рыбок", style = MaterialTheme.typography.titleSmall)
                    }
                }
            },
            confirmButton = { BigButton("Сыграть ещё", onPlayAgain) },
            dismissButton = { BigButton("Вернуться", onExit, style = ButtonStyle.Tonal) },
        )
    }
}

/**
 * The stack of tiles. Positions are the layout's half-tile units scaled to the board; every layer is shifted a little
 * up and to the right so the pile looks three-dimensional, and a higher layer is drawn above a lower one.
 */
@Composable
private fun MahjongBoardView(
    round: MahjongRound,
    cats: Map<String, Cat>,
    playing: Boolean,
    onSelect: (Int) -> Unit,
    modifier: Modifier,
) {
    val layout = round.layout
    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(layout.width.toFloat() / (layout.height * 1.08f))
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.radialGradient(listOf(Color(0xFF9A6849), Color(0xFF6B4632))))
            .border(BorderStroke(3.dp, Color(0xFF89563E)), RoundedCornerShape(20.dp))
            .padding(6.dp)
            .semantics { contentDescription = "Поле маджонга: ${round.difficulty.title.lowercase()} уровень, ${layout.tileCount} фишек" },
    ) {
        val tileWidth = maxWidth * 2 / layout.width
        val tileHeight = maxHeight * 2 / layout.height
        for (tile in round.tiles) {
            if (tile.removed) continue
            val free = MahjongBoard.isFree(round.tiles, tile.id)
            val slot = tile.slot
            MahjongTileView(
                tile = tile,
                cat = cats[tile.catId],
                free = free,
                selected = round.selectedId == tile.id,
                hinted = tile.id in round.hintedIds,
                enabled = free && playing,
                modifier = Modifier
                    .offset(
                        x = maxWidth * slot.x / layout.width + 4.dp * slot.z,
                        y = maxHeight * slot.y / layout.height - 5.dp * slot.z,
                    )
                    .size(tileWidth, tileHeight)
                    .zIndex((slot.z * 100 + slot.y * 2 + slot.x).toFloat()),
                onClick = { onSelect(tile.id) },
            )
        }
    }
}

@Composable
private fun MahjongTileView(
    tile: MahjongTile,
    cat: Cat?,
    free: Boolean,
    selected: Boolean,
    hinted: Boolean,
    enabled: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(9.dp)
    val tint = if (cat != null) catTint(cat) else Color(0xFFFFF4E0)
    val face = when {
        selected -> Color(0xFFFFE3A8)
        hinted -> Color(0xFFFFF0B8)
        free -> tint
        else -> lerp(tint, Color(0xFF5B4A3F), 0.5f)
    }
    Box(
        modifier = modifier
            .semantics {
                contentDescription = (cat?.name ?: "Котик") + if (free) ", свободная фишка" else ", заблокированная фишка"
            }
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
    ) {
        // The thickness of the tile: a darker copy shifted down and to the right.
        Box(
            modifier = Modifier.fillMaxSize().padding(start = 3.dp, top = 4.dp).clip(shape).background(Color(0xFF8F674D)),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(end = 3.dp, bottom = 4.dp)
                .clip(shape)
                .background(face)
                .border(
                    when {
                        selected -> BorderStroke(4.dp, Color(0xFFAD4D31))
                        hinted -> BorderStroke(4.dp, Color(0xFFD99521))
                        free -> BorderStroke(1.5.dp, Color(0xFFEFB86D))
                        else -> BorderStroke(1.dp, Color(0xFF7A6455))
                    },
                    shape,
                ),
        ) {
            if (cat != null) {
                Image(
                    painter = gameImage(cat.image),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(2.dp).alpha(if (free) 1f else 0.55f),
                )
            }
        }
    }
}
