package dev.psychocat.catclicker.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

/** Words of the two questions asked before something irreversible. */
class WipeTexts(
    val firstTitle: String,
    val firstText: String,
    val secondTitle: String,
    val secondText: String,
    val keepFirst: String = "Нет, не сбрасывать",
    val keepSecond: String = "Нет, оставить всё",
    val wipe: String = "Да, удалить всё",
)

/**
 * Two questions in a row before progress is wiped. [step]: 0 = nothing shown, 1 = first question, 2 = second.
 * In both dialogs the safe answer is the prominent button on the right, the destructive one is plain,
 * and tapping outside or pressing Back means "no".
 */
@Composable
fun WipeConfirmDialogs(step: Int, onStep: (Int) -> Unit, texts: WipeTexts, onConfirmed: () -> Unit) {
    when (step) {
        1 -> AlertDialog(
            onDismissRequest = { onStep(0) },
            title = { Text(texts.firstTitle) },
            text = { Text(texts.firstText) },
            confirmButton = { BigButton(texts.keepFirst, { onStep(0) }) },
            dismissButton = { BigButton("Продолжить…", { onStep(2) }, style = ButtonStyle.Outlined) },
        )

        2 -> AlertDialog(
            onDismissRequest = { onStep(0) },
            title = { Text(texts.secondTitle) },
            text = { Text(texts.secondText) },
            confirmButton = { BigButton(texts.keepSecond, { onStep(0) }) },
            dismissButton = { BigButton(texts.wipe, { onStep(0); onConfirmed() }, style = ButtonStyle.Danger) },
        )
    }
}
