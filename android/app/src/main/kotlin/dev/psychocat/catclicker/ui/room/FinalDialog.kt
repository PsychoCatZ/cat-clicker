package dev.psychocat.catclicker.ui.room

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.psychocat.catclicker.assets.gameImage
import dev.psychocat.catclicker.ui.components.BigButton
import dev.psychocat.catclicker.ui.components.ButtonStyle

/**
 * Shown once all 25 cats are saved and all rooms are furnished. Back or "return" only hides it; the game goes on
 * and the player can reopen it from the bottom of the main screen. "Expert" is offered only in normal mode.
 */
@Composable
fun FinalDialog(expertAvailable: Boolean, onStartExpert: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(12.dp),
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFFFFF9ED),
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = gameImage("ui_final"),
                    contentDescription = "Котификация началась! Все коты собраны",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)),
                )
                Text(
                    "Все пять комнат обустроены, все 25 котов спасены.",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )
                if (expertAvailable) {
                    BigButton("Начать режим «Эксперт»", onStartExpert, modifier = Modifier.fillMaxWidth())
                }
                BigButton("Вернуться в комнаты", onDismiss, modifier = Modifier.fillMaxWidth(), style = ButtonStyle.Tonal)
            }
        }
    }
}
