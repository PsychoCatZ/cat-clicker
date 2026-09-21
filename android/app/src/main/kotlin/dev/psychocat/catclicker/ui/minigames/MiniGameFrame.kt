package dev.psychocat.catclicker.ui.minigames

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.psychocat.catclicker.assets.gameImage
import dev.psychocat.catclicker.game.data.Cat
import dev.psychocat.catclicker.game.data.Cats
import dev.psychocat.catclicker.game.data.Room
import dev.psychocat.catclicker.ui.components.BigButton
import dev.psychocat.catclicker.ui.components.ButtonStyle
import dev.psychocat.catclicker.ui.theme.CardBorder
import dev.psychocat.catclicker.ui.theme.CreamCard

/**
 * The full-screen "focus" look shared by all mini-games: the room's puzzle backdrop and one calm card on top of it,
 * with no shop, no navigation and no timers. [content] learns whether the screen is wide and how much room it has.
 */
@Composable
fun MiniGameFrame(room: Room, content: @Composable (landscape: Boolean, width: Dp, height: Dp) -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = gameImage(room.match3Image),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(modifier = Modifier.fillMaxSize().background(Color(0x55000000)))
        BoxWithConstraints(modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(6.dp)) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(24.dp),
                color = CreamCard.copy(alpha = 0.96f),
                border = BorderStroke(1.dp, CardBorder),
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    content(maxWidth > maxHeight, maxWidth, maxHeight)
                }
            }
        }
    }
}

/** Title of a mini-game with the "back" button on the right. */
@Composable
fun MiniGameTitle(room: Room, title: String, onExit: () -> Unit, exitLabel: String = "Вернуться") {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                room.name.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(title, style = MaterialTheme.typography.titleLarge)
        }
        BigButton(exitLabel, onExit, style = ButtonStyle.Tonal)
    }
}

/** A small "label / value" tile of the round summary. */
@Composable
fun MiniStat(label: String, value: String, modifier: Modifier = Modifier, icon: (@Composable () -> Unit)? = null) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFFFF4E0),
        border = BorderStroke(1.dp, CardBorder),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                icon?.invoke()
                Text(value, style = MaterialTheme.typography.titleSmall, maxLines = 2)
            }
        }
    }
}

/**
 * A soft background colour per cat of the room, in the order of the room list. Several rooms have cats that look
 * alike (room 5: all black, told apart by their caps), so colour gives a second, much easier way to tell them apart.
 */
private val CatTints = listOf(
    Color(0xFFFFD9D9), // pink
    Color(0xFFD5E6FF), // blue
    Color(0xFFD8F1CC), // green
    Color(0xFFFFEDB0), // yellow
    Color(0xFFE5D8FF), // lavender
)

fun catTint(cat: Cat): Color {
    val index = Cats.forRoom(cat.roomId).indexOfFirst { it.id == cat.id }.coerceAtLeast(0)
    return CatTints[index % CatTints.size]
}
