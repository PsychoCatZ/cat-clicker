package dev.psychocat.catclicker.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.psychocat.catclicker.ui.components.BigButton
import dev.psychocat.catclicker.ui.components.ButtonStyle
import dev.psychocat.catclicker.ui.components.ItemCard

/**
 * Settings live on their own screen, away from the game. Resetting progress needs two separate confirmations,
 * and in both dialogs the safe answer is the prominent one.
 */
@Composable
fun SettingsScreen(onBack: () -> Unit, onResetConfirmed: () -> Unit, modifier: Modifier = Modifier) {
    // 0 = no dialog, 1 = first question, 2 = second question. Survives rotation.
    var resetStep by rememberSaveable { mutableIntStateOf(0) }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        BigButton("← Назад к игре", onBack, modifier = Modifier.fillMaxWidth(), style = ButtonStyle.Tonal)
        Text("Настройки", style = MaterialTheme.typography.headlineMedium)

        ItemCard(Modifier.fillMaxWidth()) {
            Text("Об игре", style = MaterialTheme.typography.titleMedium)
            Text(
                "Котокликер работает без интернета. Прогресс сохраняется на этом устройстве сам, " +
                    "кнопки «Сохранить» нет. Можно спокойно закрывать игру в любой момент.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text("Версия ${versionName()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        ItemCard(Modifier.fillMaxWidth()) {
            Text("Начать игру заново", style = MaterialTheme.typography.titleMedium)
            Text(
                "Удаляет все комнаты, котов, рыбок, ресурсы и улучшения. Вернуть их будет нельзя. " +
                    "Перед сбросом мы дважды спросим вас.",
                style = MaterialTheme.typography.bodyMedium,
            )
            BigButton("Сбросить весь прогресс…", { resetStep = 1 }, modifier = Modifier.fillMaxWidth(), style = ButtonStyle.Outlined)
        }
    }

    when (resetStep) {
        1 -> AlertDialog(
            onDismissRequest = { resetStep = 0 },
            title = { Text("Сбросить весь прогресс?") },
            text = { Text("Все комнаты, коты, рыбки, ресурсы и улучшения будут удалены. Игра начнётся с самого начала.") },
            confirmButton = { BigButton("Нет, не сбрасывать", { resetStep = 0 }) },
            dismissButton = { BigButton("Продолжить…", { resetStep = 2 }, style = ButtonStyle.Outlined) },
        )

        2 -> AlertDialog(
            onDismissRequest = { resetStep = 0 },
            title = { Text("Вы точно уверены?") },
            text = { Text("Это последний вопрос. После нажатия «Да, удалить всё» прогресс пропадёт навсегда.") },
            confirmButton = { BigButton("Нет, оставить всё", { resetStep = 0 }) },
            dismissButton = {
                BigButton("Да, удалить всё", { resetStep = 0; onResetConfirmed() }, style = ButtonStyle.Danger)
            },
        )
    }
}

@Composable
private fun versionName(): String {
    val context = LocalContext.current
    return try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"
    } catch (e: Exception) {
        "?"
    }
}
