package dev.psychocat.catclicker.ui.minigames

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.psychocat.catclicker.assets.gameImage
import dev.psychocat.catclicker.game.data.Cats
import dev.psychocat.catclicker.game.data.Room
import dev.psychocat.catclicker.game.minigames.mahjong.MahjongDifficulty
import dev.psychocat.catclicker.game.minigames.mahjong.MahjongLayouts
import dev.psychocat.catclicker.game.minigames.pairs.PairsGame
import dev.psychocat.catclicker.game.minigames.sliding.SlidingDifficulty
import dev.psychocat.catclicker.ui.components.BigButton
import dev.psychocat.catclicker.ui.components.CardArt
import dev.psychocat.catclicker.ui.components.ItemCard
import dev.psychocat.catclicker.ui.components.SectionHeading
import dev.psychocat.catclicker.ui.theme.CardBorder
import dev.psychocat.catclicker.ui.theme.CreamCard
import dev.psychocat.catclicker.ui.theme.FishBorder
import dev.psychocat.catclicker.ui.theme.FishCard

/** The "Мини-игры" section of the shop. Games appear here one by one as they are ported. */
fun LazyListScope.miniGamesShop(
    room: Room,
    selectedCatId: String,
    onStartMatch3: () -> Unit,
    onStartMahjong: (MahjongDifficulty) -> Unit,
    onStartPairs: (Int) -> Unit,
    onStartSliding: (SlidingDifficulty, String) -> Unit,
) {
    item(key = "minigames-heading") {
        SectionHeading(
            "Спокойные игры · ${room.name}",
            "Мини-игры",
            "Без таймера и без влияния на открытие комнат. Очки после игры превращаются в рыбки этой комнаты.",
        )
    }
    item(key = "minigames-match3") {
        Match3HubCard(room, onStartMatch3)
    }
    item(key = "minigames-pairs") {
        PairsHubCard(room, onStartPairs)
    }
    item(key = "minigames-mahjong") {
        MahjongHubCard(room, onStartMahjong)
    }
    item(key = "minigames-sliding") {
        SlidingHubCard(room, selectedCatId, onStartSliding)
    }
}

@Composable
private fun MahjongHubCard(room: Room, onStart: (MahjongDifficulty) -> Unit) {
    val roomCats = Cats.forRoom(room.id)
    var difficulty by rememberSaveable { mutableStateOf(MahjongDifficulty.NORMAL) }
    val layout = MahjongLayouts.of(difficulty)

    ItemCard(Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
            roomCats.forEach { cat ->
                Surface(
                    modifier = Modifier.weight(1f).heightIn(min = 70.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = catTint(cat),
                    border = BorderStroke(2.dp, Color(0xFFEFB86D)),
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(2.dp)) {
                        Image(gameImage(cat.image), contentDescription = null, modifier = Modifier.size(58.dp))
                    }
                }
            }
        }
        Text("${layout.tileCount} ФИШЕК · ${layout.tileCount / 2} ПАР", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text("Кошачий маджонг", style = MaterialTheme.typography.titleLarge)
        Text(
            "Снимайте одинаковых свободных котиков со слоёв. Без таймера, штрафов и спешки.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text("Сложность", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            MahjongDifficulty.entries.forEach { option ->
                val active = option == difficulty
                Surface(
                    modifier = Modifier.weight(1f).heightIn(min = 64.dp)
                        .selectable(selected = active, role = Role.RadioButton) { difficulty = option },
                    shape = RoundedCornerShape(14.dp),
                    color = if (active) FishCard else CreamCard,
                    border = BorderStroke(if (active) 3.dp else 1.dp, if (active) MaterialTheme.colorScheme.primary else CardBorder),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.padding(6.dp)) {
                        Text(option.title, style = MaterialTheme.typography.labelMedium, maxLines = 1, textAlign = TextAlign.Center)
                        Text("${MahjongLayouts.of(option).tileCount}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        BigButton("Играть · ${difficulty.title.lowercase()}", { onStart(difficulty) }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun Match3HubCard(room: Room, onStart: () -> Unit) {
    val roomCats = Cats.forRoom(room.id)
    ItemCard(Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
            roomCats.forEach { cat ->
                Surface(
                    modifier = Modifier.weight(1f).heightIn(min = 60.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = FishCard,
                    border = BorderStroke(1.dp, FishBorder),
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(2.dp)) {
                        Image(gameImage(cat.image), contentDescription = null, modifier = Modifier.size(54.dp))
                    }
                }
            }
        }
        Text("20 ХОДОВ", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text("Котики в ряд", style = MaterialTheme.typography.titleLarge)
        Text(
            "Меняйте соседних котиков местами и собирайте линии из трёх и больше.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "• Без ограничения времени.\n• Ошибочная перестановка не расходует ход.\n• Все очки превратятся в рыбки этой комнаты.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        BigButton("Играть в «три в ряд»", onStart, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun PairsHubCard(room: Room, onStart: (Int) -> Unit) {
    val roomCats = Cats.forRoom(room.id)
    var cardCount by rememberSaveable { mutableStateOf(10) }

    ItemCard(Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            roomCats.take(3).forEach { cat ->
                Surface(
                    modifier = Modifier.weight(1f).heightIn(min = 90.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = FishCard,
                    border = BorderStroke(2.dp, FishBorder),
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(4.dp)) {
                        Image(gameImage(cat.image), contentDescription = null, modifier = Modifier.size(76.dp))
                    }
                }
            }
            Surface(
                modifier = Modifier.weight(1f).heightIn(min = 90.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primary,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("?", color = Color.White, style = MaterialTheme.typography.displayLarge)
                }
            }
        }
        Text("${cardCount / 2} ПАР", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text("Найди пару", style = MaterialTheme.typography.titleLarge)
        Text(
            "Открывайте по две карточки и запоминайте, где спрятались одинаковые котики.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text("Размер поля", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            PairsGame.CARD_COUNTS.forEach { option ->
                val active = option == cardCount
                Surface(
                    modifier = Modifier.weight(1f).heightIn(min = 64.dp)
                        .selectable(selected = active, role = Role.RadioButton) { cardCount = option },
                    shape = RoundedCornerShape(14.dp),
                    color = if (active) FishCard else CreamCard,
                    border = BorderStroke(if (active) 3.dp else 1.dp, if (active) MaterialTheme.colorScheme.primary else CardBorder),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.padding(6.dp)) {
                        Text("$option", style = MaterialTheme.typography.titleSmall)
                        Text("карточек", style = MaterialTheme.typography.bodySmall, maxLines = 1)
                    }
                }
            }
        }
        BigButton("Играть · $cardCount карточек", { onStart(cardCount) }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun SlidingHubCard(room: Room, selectedCatId: String, onStart: (SlidingDifficulty, String) -> Unit) {
    val roomCats = Cats.forRoom(room.id)
    var difficulty by rememberSaveable { mutableStateOf(SlidingDifficulty.EASY) }
    var chosenCatId by rememberSaveable(room.id) { mutableStateOf(selectedCatId) }
    val cat = roomCats.firstOrNull { it.id == chosenCatId } ?: roomCats.firstOrNull { it.id == selectedCatId } ?: roomCats.first()

    ItemCard(Modifier.fillMaxWidth()) {
        CardArt(cat.image, cat.name, height = 170.dp)
        Text("${difficulty.size}×${difficulty.size} · ОДИН КОТИК", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text("Кошачьи пятнашки", style = MaterialTheme.typography.titleLarge)
        Text(
            "Передвигайте соседние плитки в пустую клетку и восстановите изображение котика.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text("Выберите котика", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            roomCats.forEach { option ->
                val active = option.id == cat.id
                Surface(
                    modifier = Modifier.weight(1f).heightIn(min = 64.dp)
                        .selectable(selected = active, role = Role.RadioButton) { chosenCatId = option.id },
                    shape = RoundedCornerShape(14.dp),
                    color = if (active) FishCard else CreamCard,
                    border = BorderStroke(if (active) 3.dp else 1.dp, if (active) MaterialTheme.colorScheme.primary else CardBorder),
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(4.dp)) {
                        Image(gameImage(option.image), contentDescription = option.name, modifier = Modifier.size(52.dp))
                    }
                }
            }
        }

        Text("Сложность", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            SlidingDifficulty.entries.forEach { option ->
                val active = option == difficulty
                Surface(
                    modifier = Modifier.weight(1f).heightIn(min = 64.dp)
                        .selectable(selected = active, role = Role.RadioButton) { difficulty = option },
                    shape = RoundedCornerShape(14.dp),
                    color = if (active) FishCard else CreamCard,
                    border = BorderStroke(if (active) 3.dp else 1.dp, if (active) MaterialTheme.colorScheme.primary else CardBorder),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.padding(6.dp)) {
                        Text(option.title, style = MaterialTheme.typography.labelMedium, maxLines = 1, textAlign = TextAlign.Center)
                        Text("${option.size}×${option.size}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        BigButton("Играть · ${difficulty.size}×${difficulty.size}", { onStart(difficulty, cat.id) }, modifier = Modifier.fillMaxWidth())
    }
}
