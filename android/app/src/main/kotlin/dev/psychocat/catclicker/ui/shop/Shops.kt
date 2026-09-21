package dev.psychocat.catclicker.ui.shop

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.psychocat.catclicker.game.data.Cats
import dev.psychocat.catclicker.game.data.Items
import dev.psychocat.catclicker.game.data.Upgrade
import dev.psychocat.catclicker.game.data.UpgradeTier
import dev.psychocat.catclicker.game.data.Upgrades
import dev.psychocat.catclicker.game.engine.Economy
import dev.psychocat.catclicker.game.format.Numbers
import dev.psychocat.catclicker.game.model.GameState
import dev.psychocat.catclicker.ui.components.BigButton
import dev.psychocat.catclicker.ui.components.ButtonStyle
import dev.psychocat.catclicker.ui.components.CardArt
import dev.psychocat.catclicker.ui.components.CardTexts
import dev.psychocat.catclicker.ui.components.ItemCard
import dev.psychocat.catclicker.ui.components.PriceButton
import dev.psychocat.catclicker.ui.components.SectionHeading
import dev.psychocat.catclicker.ui.components.StatusLabel
import dev.psychocat.catclicker.ui.components.cardGrid

/** Upgrades of the current room: 5 basic ones, then 5 advanced ones once all basic ones are bought. */
fun LazyListScope.upgradesShop(state: GameState, columns: Int, onBuy: (String) -> Unit) {
    val progress = state.progress
    val advancedOpen = Economy.basicUpgradesBought(state)
    val all = Upgrades.forRoom(state.currentRoom)
    val basic = all.filter { it.tier == UpgradeTier.BASIC }
    val advanced = all.filter { it.tier == UpgradeTier.ADVANCED }

    item(key = "upgrades-heading") {
        SectionHeading("Обставьте комнату", "Улучшения", "Купите 5 обычных, чтобы открыть продвинутые. Все 10 нужны для перехода.")
    }
    item(key = "upgrades-basic-title") {
        TierTitle("Обычные · ${basic.count { it.id in progress.boughtUpgrades }}/5")
    }
    cardGrid(basic, columns, { it.id }) { upgrade, modifier ->
        UpgradeCard(state, upgrade, locked = false, onBuy, modifier)
    }
    item(key = "upgrades-advanced-title") {
        Column {
            TierTitle("Продвинутые · ${advanced.count { it.id in progress.boughtUpgrades }}/5")
            Text(
                "Продвинутый предмет заменяет обычный в комнате. Доход обоих сохраняется.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    cardGrid(advanced, columns, { it.id }) { upgrade, modifier ->
        UpgradeCard(state, upgrade, locked = !advancedOpen, onBuy, modifier)
    }
}

@Composable
private fun TierTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun UpgradeCard(
    state: GameState,
    upgrade: Upgrade,
    locked: Boolean,
    onBuy: (String) -> Unit,
    modifier: Modifier,
) {
    val progress = state.progress
    val owned = upgrade.id in progress.boughtUpgrades
    val cost = Economy.upgradeCost(state, upgrade)
    ItemCard(modifier) {
        CardArt(upgrade.image, upgrade.name)
        CardTexts(
            badge = (if (upgrade.tier == UpgradeTier.ADVANCED) "Продвинутое" else "Обычное") + " · место ${upgrade.slot + 1}",
            title = upgrade.name,
            body = "+${Numbers.format(upgrade.income.toDouble())} рыбок в секунду",
        )
        Spacer(Modifier.weight(1f))
        when {
            owned -> StatusLabel("Куплено")
            locked -> BigButton("Сначала обычные", onClick = {}, enabled = false, style = ButtonStyle.Tonal, modifier = Modifier.fillMaxWidth())
            else -> PriceButton("Купить", Numbers.format(cost), enabled = progress.fish >= cost, onClick = { onBuy(upgrade.id) })
        }
    }
}

/** Click-income resources: every level adds the same bonus per tap, the price grows. */
fun LazyListScope.resourcesShop(state: GameState, columns: Int, onBuy: (String) -> Unit) {
    val progress = state.progress
    item(key = "resources-heading") {
        SectionHeading("Доход за нажатие", "Ресурсы", "Каждая покупка добавляет столько же рыбок за клик. Цена следующего уровня растёт.")
    }
    cardGrid(Items.resources, columns, { it.id }) { item, modifier ->
        val level = progress.resourceLevels[item.id] ?: 0
        val cost = Economy.resourceCost(state, item.id)
        ItemCard(modifier) {
            CardArt(item.image, item.name)
            CardTexts(
                badge = "Уровень $level",
                title = item.name,
                body = "+${item.bonus} за клик при каждой покупке · сейчас +${item.bonus * level}",
            )
            Spacer(Modifier.weight(1f))
            PriceButton("Улучшить", Numbers.format(cost), enabled = progress.fish >= cost, onClick = { onBuy(item.id) })
        }
    }
}

/** Food: wakes the cat when the satiety bar is empty. */
fun LazyListScope.foodShop(state: GameState, columns: Int, onBuy: (String) -> Unit) {
    val progress = state.progress
    item(key = "food-heading") {
        Column {
            SectionHeading("Разбудите кота", "Корм", "Когда шкала пуста, клики не работают. Рыбки в секунду продолжают поступать.")
            if (Economy.safetyIncome(state) > 0) {
                Text(
                    "Даже без улучшений рыбки медленно копятся. На мышку хватит примерно через 30 минут.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
    cardGrid(Items.foods, columns, { it.id }) { item, modifier ->
        val cost = Economy.foodCost(state, item.baseCost)
        val full = progress.hunger >= 100 && item.boostSeconds == null
        ItemCard(modifier) {
            CardArt(item.image, item.name)
            CardTexts(
                badge = "Сытость +${item.restore}%",
                title = item.name,
                body = if (item.boostSeconds != null) "Доход за клик ×2 на 1 минуту" else "Восстанавливает шкалу голода",
            )
            Spacer(Modifier.weight(1f))
            PriceButton("Купить", Numbers.format(cost), enabled = progress.fish >= cost && !full, onClick = { onBuy(item.id) })
        }
    }
}

/** The five cats of the current room: buy, then choose which one sits in the room. */
fun LazyListScope.catsShop(state: GameState, columns: Int, onBuy: (String) -> Unit, onSelect: (String) -> Unit) {
    val progress = state.progress
    item(key = "cats-heading") {
        SectionHeading("Спасите всех пятерых", "Коты комнаты", "Купленные коты навсегда остаются в своей комнате. Любого можно выбрать.")
    }
    cardGrid(Cats.forRoom(state.currentRoom), columns, { it.id }) { cat, modifier ->
        val unlocked = cat.id in progress.unlockedCats
        val selected = progress.selectedCat == cat.id
        val cost = Economy.catCost(state, cat.baseCost)
        ItemCard(modifier, highlighted = selected) {
            CardArt(cat.image, cat.name, height = 150.dp)
            CardTexts(
                badge = null,
                title = cat.name,
                body = if (selected) "Сейчас в комнате" else if (unlocked) "В коллекции" else "Ждёт спасения",
            )
            Spacer(Modifier.weight(1f))
            when {
                selected -> StatusLabel("Выбран")
                unlocked -> BigButton("Выбрать", onClick = { onSelect(cat.id) }, modifier = Modifier.fillMaxWidth())
                else -> PriceButton("Открыть", Numbers.format(cost), enabled = progress.fish >= cost, onClick = { onBuy(cat.id) })
            }
        }
    }
}

fun LazyListScope.miniGamesPlaceholder() {
    item(key = "minigames-placeholder") {
        SectionHeading("Спокойные игры без таймера", "Мини-игры", "Здесь появятся «Три в ряд», «Найди пару», «Кошачий маджонг» и «Кошачьи пятнашки». Пока они готовятся.")
    }
}

private fun Modifier.fillMaxWidth(): Modifier = this.then(Modifier.fillMaxWidth())
