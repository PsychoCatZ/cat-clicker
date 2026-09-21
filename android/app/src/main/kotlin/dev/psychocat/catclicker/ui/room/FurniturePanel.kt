package dev.psychocat.catclicker.ui.room

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.psychocat.catclicker.assets.gameImage
import dev.psychocat.catclicker.game.format.Numbers
import dev.psychocat.catclicker.ui.components.BigButton
import dev.psychocat.catclicker.ui.components.ButtonStyle
import dev.psychocat.catclicker.ui.theme.CardBorder
import dev.psychocat.catclicker.ui.theme.CreamCard
import dev.psychocat.catclicker.ui.theme.FishCard

/**
 * Bought furniture: shows what is still waiting for a place and switches the "arrange" mode on and off.
 * Choosing an item in the tray also switches the mode on, so one tap is enough to start.
 */
@Composable
fun FurniturePanel(
    items: List<SceneFurniture>,
    editing: Boolean,
    selectedId: String?,
    onToggleEditing: () -> Unit,
    onSelect: (String) -> Unit,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val chosenItem = items.firstOrNull { editing && it.upgrade.id == selectedId }
    val waiting = items.count { it.point == null }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = CreamCard,
        border = BorderStroke(1.dp, CardBorder),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Обставить комнату", style = MaterialTheme.typography.titleSmall)
                    Text(
                        if (waiting > 0) Numbers.itemsWaiting(waiting) else "Все купленные предметы размещены",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                BigButton(if (editing) "Готово" else "Расставить", onToggleEditing)
            }
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items.forEach { item ->
                    val chosen = editing && selectedId == item.upgrade.id
                    Surface(
                        modifier = Modifier.width(220.dp).heightIn(min = 84.dp)
                            .selectable(selected = chosen, role = Role.Button) { onSelect(item.upgrade.id) },
                        shape = RoundedCornerShape(16.dp),
                        color = if (chosen) FishCard else CreamCard,
                        border = BorderStroke(if (chosen) 3.dp else 1.dp, if (chosen) MaterialTheme.colorScheme.primary else CardBorder),
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Image(gameImage(item.upgrade.image), contentDescription = null, modifier = Modifier.size(64.dp))
                            Column {
                                Text(item.upgrade.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 2)
                                Text(
                                    if (item.point != null) "В комнате" else "Ждёт места",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
            if (chosenItem != null && chosenItem.point != null) {
                BigButton(
                    "Убрать «${chosenItem.upgrade.name}» из комнаты",
                    { onRemove(chosenItem.upgrade.id) },
                    modifier = Modifier.fillMaxWidth(),
                    style = ButtonStyle.Tonal,
                )
            }
            if (editing) {
                Text(
                    "Выберите предмет и коснитесь места в комнате или перетащите его. Кнопка «Убрать» возвращает предмет в список, доход от него сохраняется. Кот не собирает рыбок, пока вы расставляете.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
