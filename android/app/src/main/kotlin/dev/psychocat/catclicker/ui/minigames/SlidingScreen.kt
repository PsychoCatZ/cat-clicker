package dev.psychocat.catclicker.ui.minigames

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.psychocat.catclicker.assets.LocalAssets
import dev.psychocat.catclicker.assets.gameImage
import dev.psychocat.catclicker.game.data.Cats
import dev.psychocat.catclicker.game.data.Room
import dev.psychocat.catclicker.game.format.Numbers
import dev.psychocat.catclicker.game.minigames.RoundStatus
import dev.psychocat.catclicker.game.minigames.sliding.SlidingBoard
import dev.psychocat.catclicker.game.minigames.sliding.SlidingEvent
import dev.psychocat.catclicker.game.minigames.sliding.SlidingGame
import dev.psychocat.catclicker.game.minigames.sliding.SlidingRound
import dev.psychocat.catclicker.game.model.GameMode
import dev.psychocat.catclicker.ui.components.BigButton
import dev.psychocat.catclicker.ui.components.ButtonStyle
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * "Cat sliding puzzle": slide tiles next to the gap until the picture of a cat is whole again.
 * No timer, no move limit, no penalties; leaving early simply pays the reward earned so far (none until solved).
 */
@Composable
fun SlidingScreen(
    room: Room,
    round: SlidingRound,
    onMove: (Int) -> Unit,
    onReshuffle: () -> Unit,
    onPlayAgain: () -> Unit,
    onExit: () -> Unit,
) {
    var showOriginal by rememberSaveable { mutableStateOf(false) }
    val cat = Cats.find(round.catId)
    val reward = SlidingGame.fishReward(round.score, round.roomId, round.mode)
    val playing = round.status == RoundStatus.PLAYING
    val message = when (round.lastEvent) {
        SlidingEvent.SHUFFLED -> "Картинка перемешана допустимыми ходами и точно решаема."
        SlidingEvent.MOVED -> "Передвигайте соседние плитки в пустую клетку."
        else -> "Соберите цельное изображение котика."
    }

    MiniGameFrame(room) { landscape, width, height ->
        val controls: @Composable () -> Unit = {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                BigButton("Образец", { showOriginal = true }, modifier = Modifier.weight(1f), style = ButtonStyle.Tonal, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp))
                BigButton("Перемешать", onReshuffle, modifier = Modifier.weight(1f), enabled = playing, style = ButtonStyle.Tonal, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp))
            }
        }
        val summary: @Composable () -> Unit = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "${round.difficulty.title} · ${round.size}×${round.size} · ${cat?.name ?: "Котик"}",
                    style = MaterialTheme.typography.titleSmall,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiniStat("Ходы", Numbers.format(round.moves.toDouble()), Modifier.weight(1f))
                    MiniStat(
                        "Рыбки сейчас", Numbers.format(reward), Modifier.weight(1f),
                        icon = { Image(gameImage("resource_01"), contentDescription = null, modifier = Modifier.size(24.dp)) },
                    )
                }
            }
        }
        val notes: @Composable () -> Unit = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(message, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Нажимайте на плитку рядом с пустой клеткой. Число ходов ничем не ограничено.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    if (round.mode == GameMode.EXPERT) "Режим «Эксперт»: котификация увеличена." else "Таймера и штрафов нет — собирайте картинку в своём темпе.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (landscape) {
            val boardSide = minOf(height - 24.dp, width * 0.5f)
            Row(modifier = Modifier.fillMaxSize().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SlidingBoardView(round, onMove, Modifier.size(boardSide).align(Alignment.CenterVertically))
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    MiniGameTitle(room, "Кошачьи пятнашки", onExit)
                    summary()
                    controls()
                    notes()
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MiniGameTitle(room, "Кошачьи пятнашки", onExit)
                summary()
                SlidingBoardView(round, onMove, Modifier.fillMaxWidth())
                controls()
                notes()
            }
        }
    }

    if (showOriginal) {
        AlertDialog(
            onDismissRequest = { showOriginal = false },
            title = { Text("Образец: ${cat?.name ?: "Котик"}") },
            text = {
                if (cat != null) {
                    Image(
                        painter = gameImage(cat.image),
                        contentDescription = "Оригинальное изображение: ${cat.name}",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                    )
                }
            },
            confirmButton = { BigButton("Продолжить", { showOriginal = false }) },
        )
    }

    if (round.status == RoundStatus.FINISHED) {
        AlertDialog(
            // Deliberately not dismissible by a stray tap: the player chooses what to do next.
            onDismissRequest = {},
            title = { Text("Картинка собрана!") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Сложность: ${round.difficulty.title} · ${round.size}×${round.size}")
                    Text("Количество ходов: ${Numbers.format(round.moves.toDouble())}")
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

/** The square board. Each tile is a piece of the cat picture (fitted whole into the square, like the web version). */
@Composable
private fun SlidingBoardView(round: SlidingRound, onMove: (Int) -> Unit, modifier: Modifier = Modifier) {
    val cat = Cats.find(round.catId)
    val picture: ImageBitmap? = cat?.let { ImageBitmap.imageResource(LocalAssets.current.drawable(it.image)) }
    val movable = SlidingBoard.movableTileIds(round.tiles, round.size).toSet()
    val playing = round.status == RoundStatus.PLAYING
    val gridSize = round.size
    val emptyIndex = round.tiles.indexOf(null)

    BoxWithConstraints(
        modifier = modifier.aspectRatio(1f)
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.radialGradient(listOf(Color(0xFFF6E0BF), Color(0xFFC99B70))))
            .border(BorderStroke(4.dp, Color(0xFF87533D)), RoundedCornerShape(18.dp))
            .padding(4.dp),
    ) {
        val cell: Dp = maxWidth / gridSize

        // The gap.
        Box(
            modifier = Modifier
                .offset(x = cell * (emptyIndex % gridSize), y = cell * (emptyIndex / gridSize))
                .size(cell)
                .padding(3.dp)
                .background(Color(0x668B5C43), RoundedCornerShape(8.dp)),
        )

        for (tileId in 0 until gridSize * gridSize - 1) {
            val position = round.tiles.indexOf(tileId)
            val x by animateDpAsState(cell * (position % gridSize), tween(150), label = "tileX")
            val y by animateDpAsState(cell * (position / gridSize), tween(150), label = "tileY")
            val canMove = tileId in movable && playing
            SlidingTile(
                tileId = tileId,
                gridSize = gridSize,
                picture = picture,
                canMove = canMove,
                modifier = Modifier.offset(x = x, y = y).size(cell),
                onClick = { onMove(tileId) },
            )
        }
    }
}

@Composable
private fun SlidingTile(
    tileId: Int,
    gridSize: Int,
    picture: ImageBitmap?,
    canMove: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val column = tileId % gridSize
    val row = tileId / gridSize
    Canvas(
        modifier = modifier
            .semantics { contentDescription = "Фрагмент ${tileId + 1}" + if (canMove) ", можно передвинуть" else "" }
            .clickable(enabled = canMove, role = Role.Button, onClick = onClick),
    ) {
        val inset = 2.dp.toPx()
        val radius = CornerRadius(8.dp.toPx())
        val frame = RoundRect(inset, inset, size.width - inset, size.height - inset, radius)
        clipPath(Path().apply { addRoundRect(frame) }) {
            drawRect(Color(0xFFF4DFBF))
            if (picture != null) {
                // The whole picture is fitted into the board square; this tile shows its own slice of it.
                val boardSide = size.width * gridSize
                val scale = min(boardSide / picture.width, boardSide / picture.height)
                val fittedWidth = picture.width * scale
                val fittedHeight = picture.height * scale
                val originX = (boardSide - fittedWidth) / 2 - column * size.width
                val originY = (boardSide - fittedHeight) / 2 - row * size.height
                drawImage(
                    image = picture,
                    dstOffset = IntOffset(originX.roundToInt(), originY.roundToInt()),
                    dstSize = IntSize(fittedWidth.roundToInt(), fittedHeight.roundToInt()),
                    filterQuality = FilterQuality.Medium,
                )
            }
        }
        drawRoundRect(
            color = if (canMove) Color(0xFFE8A95C) else Color(0xFFF7E7CE),
            topLeft = Offset(inset, inset),
            size = Size(size.width - 2 * inset, size.height - 2 * inset),
            cornerRadius = radius,
            style = Stroke(width = if (canMove) 3.dp.toPx() else 1.5.dp.toPx()),
        )
    }
}
