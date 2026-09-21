package dev.psychocat.catclicker.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import dev.psychocat.catclicker.game.data.Furniture
import dev.psychocat.catclicker.game.data.SceneLayout
import dev.psychocat.catclicker.game.model.GameMode
import dev.psychocat.catclicker.ui.components.WipeConfirmDialogs
import dev.psychocat.catclicker.ui.components.WipeTexts
import dev.psychocat.catclicker.ui.room.FinalDialog
import dev.psychocat.catclicker.ui.room.FurnitureEdit
import dev.psychocat.catclicker.ui.room.FurniturePanel
import dev.psychocat.catclicker.ui.room.SceneFurniture
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.psychocat.catclicker.game.data.Cats
import dev.psychocat.catclicker.game.data.Items
import dev.psychocat.catclicker.game.data.Rooms
import dev.psychocat.catclicker.game.data.Upgrades
import dev.psychocat.catclicker.game.engine.Economy
import dev.psychocat.catclicker.game.format.Numbers
import dev.psychocat.catclicker.game.model.GameAction
import dev.psychocat.catclicker.game.model.GameState
import dev.psychocat.catclicker.ui.components.BigButton
import dev.psychocat.catclicker.ui.components.ButtonStyle
import dev.psychocat.catclicker.ui.room.FishHud
import dev.psychocat.catclicker.ui.room.HungerPanel
import dev.psychocat.catclicker.ui.room.ModeLabel
import dev.psychocat.catclicker.ui.room.RatesHud
import dev.psychocat.catclicker.ui.room.Hud
import dev.psychocat.catclicker.ui.room.RoomChips
import dev.psychocat.catclicker.ui.room.RoomScene
import dev.psychocat.catclicker.ui.room.SCENE_ASPECT_LANDSCAPE
import dev.psychocat.catclicker.ui.room.SCENE_ASPECT_PORTRAIT
import dev.psychocat.catclicker.ui.room.ShopTab
import dev.psychocat.catclicker.ui.room.ShopTabs
import dev.psychocat.catclicker.ui.settings.DebugTools
import dev.psychocat.catclicker.ui.settings.SettingsScreen
import dev.psychocat.catclicker.ui.shop.catsShop
import dev.psychocat.catclicker.ui.shop.foodShop
import dev.psychocat.catclicker.ui.shop.miniGamesPlaceholder
import dev.psychocat.catclicker.ui.shop.resourcesShop
import dev.psychocat.catclicker.ui.shop.upgradesShop
import dev.psychocat.catclicker.ui.theme.Brown
import dev.psychocat.catclicker.ui.theme.Cream
import kotlinx.coroutines.delay

private data class Notice(val text: String, val id: Long)

/**
 * The whole game UI. [dispatch] returns true when the action changed the game, which is how purchases that were
 * rejected (not enough fish, already owned) stay silent.
 */
@Composable
fun GameScreen(state: GameState, dispatch: (GameAction) -> Boolean, modifier: Modifier = Modifier, debug: DebugTools? = null) {
    var tab by rememberSaveable { mutableStateOf(ShopTab.UPGRADES) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var editing by rememberSaveable { mutableStateOf(false) }
    var selectedFurniture by rememberSaveable { mutableStateOf<String?>(null) }
    var expertStep by rememberSaveable { mutableIntStateOf(0) }
    var notice by remember { mutableStateOf<Notice?>(null) }
    fun say(text: String) {
        notice = Notice(text, System.nanoTime())
    }

    LaunchedEffect(notice?.id) {
        if (notice != null) {
            delay(2500)
            notice = null
        }
    }
    BackHandler(enabled = showSettings) { showSettings = false }
    // Arranging furniture belongs to one room; leaving it (or resetting) ends the mode.
    LaunchedEffect(state.currentRoom, state.mode) {
        editing = false
        selectedFurniture = null
    }
    val finished = state.currentRoom == Rooms.count && Economy.roomComplete(state)

    val actions = GameActions(
        onCatTap = { dispatch(GameAction.Click) },
        onSleepingTap = {
            say(if (state.progress.hunger <= 0) "Кот спит. Купите корм в разделе «Корм»" else "Кот спит. Включите свет")
        },
        onToggleLights = { dispatch(GameAction.ToggleLights) },
        onVisitRoom = { roomId ->
            if (dispatch(GameAction.VisitRoom(roomId))) tab = ShopTab.UPGRADES
        },
        onDoor = {
            if (dispatch(GameAction.EnterNextRoom)) {
                tab = ShopTab.UPGRADES
                say("Новая комната открыта")
            }
        },
        onBuyUpgrade = { id ->
            val item = Upgrades.find(state.currentRoom, id)
            if (item != null && dispatch(GameAction.BuyUpgrade(id))) say("Улучшение куплено: ${item.name}. Предмет ждёт места под комнатой")
        },
        onBuyResource = { id ->
            val item = Items.resource(id)
            if (item != null && dispatch(GameAction.BuyResource(id))) say("${item.name}: доход за клик увеличен")
        },
        onBuyFood = { id ->
            val item = Items.food(id)
            if (item != null && dispatch(GameAction.BuyFood(id))) say("${item.name}: кот сыт")
        },
        onBuyCat = { id ->
            val item = Cats.find(id)
            if (item != null && dispatch(GameAction.BuyCat(id))) say("Кот открыт: ${item.name}")
        },
        onSelectCat = { id -> dispatch(GameAction.SelectCat(id)) },
        onPlaceFurniture = { id, layout, x, y -> dispatch(GameAction.PlaceFurniture(id, layout, x, y)) },
        onShowFinal = { dispatch(GameAction.ShowFinal) },
    )

    Box(modifier = modifier.fillMaxSize()) {
        if (showSettings) {
            SettingsScreen(
                onBack = { showSettings = false },
                onResetConfirmed = {
                    dispatch(GameAction.Reset)
                    showSettings = false
                    tab = ShopTab.UPGRADES
                    say("Прогресс сброшен")
                },
                modifier = Modifier.safeDrawingPadding(),
                debug = debug,
            )
        } else {
            GameBody(
                state = state,
                tab = tab,
                onTab = { tab = it },
                actions = actions,
                editing = editing,
                onEditing = { editing = it },
                selectedFurniture = selectedFurniture,
                onSelectFurniture = { selectedFurniture = it },
                finished = finished,
                onOpenSettings = { showSettings = true },
            )
        }

        notice?.let { current ->
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).safeDrawingPadding().padding(16.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite },
                shape = RoundedCornerShape(16.dp),
                color = Brown,
                contentColor = Color.White,
                shadowElevation = 8.dp,
            ) {
                Text(current.text, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp))
            }
        }

        state.offlineReport?.let { report ->
            AlertDialog(
                onDismissRequest = { dispatch(GameAction.DismissOfflineReport) },
                title = { Text("Котики ждали вас ${Numbers.formatAway(report.elapsedSeconds)}") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Пассивный доход: +${Numbers.format(report.fishEarned)} рыбок")
                        Text("Сытость: −${"%.1f".format(report.hungerSpent).replace('.', ',')}%")
                        if (report.elapsedSeconds > report.creditedSeconds) {
                            Text("Доход и расход сытости учтены максимум за 8 часов.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                },
                confirmButton = { BigButton("Понятно", { dispatch(GameAction.DismissOfflineReport) }) },
            )
        }

        if (finished && !state.finalDismissed) {
            FinalDialog(
                expertAvailable = state.mode == GameMode.NORMAL,
                onStartExpert = { expertStep = 1 },
                onDismiss = { dispatch(GameAction.DismissFinal) },
            )
        }
        WipeConfirmDialogs(
            step = expertStep,
            onStep = { expertStep = it },
            texts = WipeTexts(
                firstTitle = "Начать режим «Эксперт»?",
                firstText = "Все комнаты, коты, рыбки, ресурсы и улучшения будут сброшены. В режиме «Эксперт» цены выше, а кот голодает быстрее.",
                secondTitle = "Вы точно хотите начать заново?",
                secondText = "Текущий прогресс пропадёт. После нажатия «Да, начать» вернуть его нельзя.",
                keepFirst = "Нет, вернуться",
                wipe = "Да, начать",
            ),
            onConfirmed = {
                if (dispatch(GameAction.StartExpert)) {
                    tab = ShopTab.UPGRADES
                    say("Режим «Эксперт» начался")
                }
            },
        )
    }
}

private class GameActions(
    val onCatTap: () -> Unit,
    val onSleepingTap: () -> Unit,
    val onToggleLights: () -> Unit,
    val onVisitRoom: (Int) -> Unit,
    val onDoor: () -> Unit,
    val onBuyUpgrade: (String) -> Unit,
    val onBuyResource: (String) -> Unit,
    val onBuyFood: (String) -> Unit,
    val onBuyCat: (String) -> Unit,
    val onSelectCat: (String) -> Unit,
    val onPlaceFurniture: (String, SceneLayout, Double, Double) -> Unit,
    val onShowFinal: () -> Unit,
)

@Composable
private fun GameBody(
    state: GameState,
    tab: ShopTab,
    onTab: (ShopTab) -> Unit,
    actions: GameActions,
    editing: Boolean,
    onEditing: (Boolean) -> Unit,
    selectedFurniture: String?,
    onSelectFurniture: (String?) -> Unit,
    finished: Boolean,
    onOpenSettings: () -> Unit,
) {
    val room = Rooms.byId(state.currentRoom)
    val cat = Cats.find(state.progress.selectedCat)
    val showDoor = Economy.roomComplete(state) && state.currentRoom < Rooms.count

    BoxWithConstraints(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
        val landscape = maxWidth > maxHeight
        // Portrait phones use the web version mobile furniture layout, landscape and tablets the desktop one.
        val layout = if (landscape) SceneLayout.DESKTOP else SceneLayout.MOBILE
        val furniture = Furniture.visible(state.currentRoom, state.progress.boughtUpgrades).map { upgrade ->
            SceneFurniture(upgrade, Furniture.positionForLayout(state.progress.furniturePositions[upgrade.id], layout))
        }
        val arranging = editing && furniture.isNotEmpty()
        val selectedId = selectedFurniture?.takeIf { id -> furniture.any { it.upgrade.id == id } }

        fun toggleEditing() {
            if (arranging) {
                onEditing(false)
                onSelectFurniture(null)
            } else {
                onEditing(true)
                onSelectFurniture((furniture.firstOrNull { it.point == null } ?: furniture.firstOrNull())?.upgrade?.id)
            }
        }

        @Composable
        fun Scene(aspect: Float) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                RoomScene(
                    room = room, cat = cat, progress = state.progress, clickReward = Economy.currentClickReward(state),
                    aspectRatio = aspect, layout = layout, furniture = furniture,
                    edit = if (arranging) FurnitureEdit(selectedId) { id, x, y -> actions.onPlaceFurniture(id, layout, x, y) } else null,
                    onCatTap = actions.onCatTap, onSleepingTap = actions.onSleepingTap,
                )
                if (showDoor) {
                    BigButton("Перейти в следующую комнату", actions.onDoor, modifier = Modifier.fillMaxWidth())
                }
            }
        }

        @Composable
        fun Furnishing() {
            if (furniture.isNotEmpty()) {
                FurniturePanel(
                    items = furniture,
                    editing = arranging,
                    selectedId = selectedId,
                    onToggleEditing = ::toggleEditing,
                    onSelect = { id ->
                        onEditing(true)
                        onSelectFurniture(id)
                    },
                )
            }
        }

        if (landscape) {
            val paneWidth = maxWidth * 0.46f
            val wide = paneWidth >= 560.dp
            Row(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.weight(1.17f).verticalScroll(rememberScrollState()).padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Hud(state)
                    Scene(SCENE_ASPECT_LANDSCAPE)
                    Furnishing()
                    HungerPanel(state, actions.onToggleLights)
                }
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item { RoomChips(state.currentRoom, state.unlockedRoom, actions.onVisitRoom) }
                    item { ShopTabs(tab, columns = if (wide) 5 else 3, onSelect = onTab) }
                    shopContent(tab, state, columns = if (wide) 3 else 2, actions)
                    settingsEntry(finished && state.finalDismissed, actions.onShowFinal, onOpenSettings)
                }
            }
        } else {
            val tablet = maxWidth >= 600.dp
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                stickyHeader {
                    // Keeps the fish counter in view while buying; the background is drawn a little wider than the
                    // item so scrolling cards never show through at the side margins.
                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .drawBehind {
                                val gutter = 12.dp.toPx()
                                drawRect(Cream, topLeft = Offset(-gutter, 0f), size = Size(size.width + 2 * gutter, size.height))
                            }
                            .padding(vertical = 8.dp),
                    ) { FishHud(state) }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        ModeLabel(state)
                        RatesHud(state)
                    }
                }
                item { RoomChips(state.currentRoom, state.unlockedRoom, actions.onVisitRoom) }
                item { Scene(SCENE_ASPECT_PORTRAIT) }
                item { Furnishing() }
                item { HungerPanel(state, actions.onToggleLights) }
                item { ShopTabs(tab, columns = if (tablet) 5 else 3, onSelect = onTab) }
                shopContent(tab, state, columns = if (tablet) 3 else 2, actions)
                settingsEntry(finished && state.finalDismissed, actions.onShowFinal, onOpenSettings)
            }
        }
    }
}

private fun LazyListScope.shopContent(tab: ShopTab, state: GameState, columns: Int, actions: GameActions) {
    when (tab) {
        ShopTab.UPGRADES -> upgradesShop(state, columns, actions.onBuyUpgrade)
        ShopTab.RESOURCES -> resourcesShop(state, columns, actions.onBuyResource)
        ShopTab.FOOD -> foodShop(state, columns, actions.onBuyFood)
        ShopTab.CATS -> catsShop(state, columns, actions.onBuyCat, actions.onSelectCat)
        ShopTab.MINIGAMES -> miniGamesPlaceholder()
    }
}

/** Far from the shops and at the very bottom, so nobody opens the settings by accident. */
private fun LazyListScope.settingsEntry(showFinalButton: Boolean, onShowFinal: () -> Unit, onOpenSettings: () -> Unit) {
    item(key = "settings-entry") {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 24.dp, bottom = 32.dp)) {
            if (showFinalButton) {
                BigButton("Все коты спасены · Финал", onShowFinal, modifier = Modifier.fillMaxWidth())
            }
            Text(
                "Котокликер работает без интернета и сохраняется сам.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            BigButton("Настройки", onOpenSettings, modifier = Modifier.fillMaxWidth(), style = ButtonStyle.Tonal)
        }
    }
}
