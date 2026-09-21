package dev.psychocat.catclicker.ui.room

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.psychocat.catclicker.assets.gameImage
import dev.psychocat.catclicker.game.data.Rooms
import dev.psychocat.catclicker.ui.theme.CardBorder
import dev.psychocat.catclicker.ui.theme.CreamCard
import dev.psychocat.catclicker.ui.theme.Sand

enum class ShopTab(val title: String, val icon: String) {
    UPGRADES("Улучшения", "ui_02"),
    RESOURCES("Ресурсы", "resource_02"),
    FOOD("Корм", "food_02"),
    CATS("Коты", "ui_03"),
    MINIGAMES("Мини-игры", "ui_04"),
}

/** The five rooms. Locked rooms are visible but dimmed, so the player can see what is ahead. */
@Composable
fun RoomChips(currentRoom: Int, unlockedRoom: Int, onVisit: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Rooms.all.forEach { room ->
            val unlocked = room.id <= unlockedRoom
            val selected = room.id == currentRoom
            Surface(
                modifier = Modifier.widthIn(min = 140.dp).heightIn(min = 64.dp)
                    .selectable(selected = selected, enabled = unlocked, role = Role.Tab) { onVisit(room.id) },
                shape = RoundedCornerShape(16.dp),
                color = if (selected) MaterialTheme.colorScheme.primary else if (unlocked) CreamCard else Sand,
                contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                border = BorderStroke(1.dp, CardBorder),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text("Комната ${room.id}", style = MaterialTheme.typography.labelMedium)
                    Text(
                        if (unlocked) room.name else "Закрыта",
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/** Section switcher: big labelled buttons in rows of [columns], nothing is hidden behind scrolling. */
@Composable
fun ShopTabs(selected: ShopTab, columns: Int, onSelect: (ShopTab) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ShopTab.entries.chunked(columns).forEach { rowTabs ->
            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowTabs.forEach { tab ->
                    val active = tab == selected
                    Surface(
                        modifier = Modifier.weight(1f).fillMaxHeight().heightIn(min = 84.dp)
                            .selectable(selected = active, role = Role.Tab) { onSelect(tab) },
                        shape = RoundedCornerShape(16.dp),
                        color = if (active) CreamCard else Sand,
                        border = BorderStroke(if (active) 3.dp else 1.dp, if (active) MaterialTheme.colorScheme.primary else CardBorder),
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Image(gameImage(tab.icon), contentDescription = null, modifier = Modifier.size(38.dp))
                            Text(tab.title, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, maxLines = 1)
                        }
                    }
                }
                repeat(columns - rowTabs.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
