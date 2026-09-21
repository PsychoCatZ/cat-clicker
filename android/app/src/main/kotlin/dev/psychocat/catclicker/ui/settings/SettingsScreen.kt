package dev.psychocat.catclicker.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import dev.psychocat.catclicker.ui.components.WipeConfirmDialogs
import dev.psychocat.catclicker.ui.components.WipeTexts

/**
 * Settings live on their own screen, away from the game. Resetting progress needs two separate confirmations,
 * and in both dialogs the safe answer is the prominent one.
 */
/** Testing shortcuts, handed to the settings screen only in debuggable builds (never in a release build). */
class DebugTools(val grantFish: () -> Unit, val completeRoom: () -> Unit)

@Composable
fun SettingsScreen(onBack: () -> Unit, onResetConfirmed: () -> Unit, modifier: Modifier = Modifier, debug: DebugTools? = null) {
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

        if (debug != null) {
            ItemCard(Modifier.fillMaxWidth()) {
                Text("Для проверки", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Этот раздел виден только в отладочной сборке, из Android Studio. В обычной версии его нет.",
                    style = MaterialTheme.typography.bodySmall,
                )
                BigButton("Добавить 1 000 000 рыбок", debug.grantFish, modifier = Modifier.fillMaxWidth(), style = ButtonStyle.Tonal)
                BigButton("Купить всё в этой комнате", debug.completeRoom, modifier = Modifier.fillMaxWidth(), style = ButtonStyle.Tonal)
            }
        }
    }

    WipeConfirmDialogs(
        step = resetStep,
        onStep = { resetStep = it },
        texts = WipeTexts(
            firstTitle = "Сбросить весь прогресс?",
            firstText = "Все комнаты, коты, рыбки, ресурсы и улучшения будут удалены. Игра начнётся с самого начала.",
            secondTitle = "Вы точно уверены?",
            secondText = "Это последний вопрос. После нажатия «Да, удалить всё» прогресс пропадёт навсегда.",
        ),
        onConfirmed = onResetConfirmed,
    )
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
