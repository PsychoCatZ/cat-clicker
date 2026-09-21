package dev.psychocat.catclicker.game.engine

import dev.psychocat.catclicker.game.data.Cats
import dev.psychocat.catclicker.game.data.SceneLayout
import dev.psychocat.catclicker.game.data.Upgrades
import dev.psychocat.catclicker.game.model.GameAction
import dev.psychocat.catclicker.game.model.GameMode
import dev.psychocat.catclicker.game.model.GameState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** Readable rules of the game, mirroring the assertions of tools/check_game.mjs. */
class GameEngineTest {
    private fun funded(state: GameState = GameState.initial(), amount: Double = 1e9) = state.copy(
        rooms = state.rooms.mapIndexed { i, room -> if (i == state.currentRoom - 1) room.copy(fish = amount) else room },
    )

    private fun GameState.act(action: GameAction) = GameEngine.reduce(this, action)

    @Test
    fun clickEarnsOneFishAtTheStart() {
        val state = GameState.initial().act(GameAction.Click)
        assertEquals(1.0, state.progress.fish)
    }

    @Test
    fun eachResourceLevelAddsAFixedClickBonus() {
        var state = funded(amount = 15.0).act(GameAction.BuyResource("fish"))
        assertEquals(2.0, Economy.clickPower(state))
        state = funded(state, Economy.resourceCost(state, "fish")).act(GameAction.BuyResource("fish"))
        assertEquals(3.0, Economy.clickPower(state))
    }

    @Test
    fun advancedUpgradeNeedsAllBasicOnes() {
        val state = funded().act(GameAction.BuyUpgrade("room-1-advanced-1"))
        assertTrue(state.progress.boughtUpgrades.isEmpty())
    }

    @Test
    fun upgradeGivesPassiveIncomeBeforeItIsPlaced() {
        val state = funded().act(GameAction.BuyUpgrade("room-1-basic-1"))
        assertEquals(1.0, Economy.fishPerSecond(state))
        assertTrue(state.progress.furniturePositions.isEmpty(), "new purchases wait in the tray")
    }

    @Test
    fun sleepingCatEarnsNoClicksAndLightsOffPausesHunger() {
        var state = funded().act(GameAction.BuyUpgrade("room-1-basic-1")).act(GameAction.ToggleLights)
        assertSame(state, state.act(GameAction.Click), "sleeping cat does not earn clicks")
        val before = state.progress
        state = state.act(GameAction.Tick(1.0))
        assertEquals(before.hunger, state.progress.hunger)
        assertEquals(before.fish + 1, state.progress.fish, "passive income continues in the dark")
        state = state.act(GameAction.ToggleLights).act(GameAction.Tick(1.0))
        assertTrue(state.progress.hunger < before.hunger)
    }

    @Test
    fun emptyHungerCannotBeWokenByLights() {
        val state = GameState.initial().let { it.copy(rooms = it.rooms.mapIndexed { i, r -> if (i == 0) r.copy(hunger = 0.0) else r }) }
        assertSame(state, state.act(GameAction.Click))
        assertSame(state, state.act(GameAction.ToggleLights))
    }

    @Test
    fun mouseFallbackTakesAboutHalfAnHour() {
        var state = GameState.initial().let { it.copy(rooms = it.rooms.mapIndexed { i, r -> if (i == 0) r.copy(hunger = 0.0) else r }) }
        repeat(1800) { state = state.act(GameAction.Tick(1.0)) }
        assertTrue(kotlin.math.abs(state.progress.fish - 60.0) < 0.1, "fish was ${state.progress.fish}")
        state = state.act(GameAction.BuyFood("mouse"))
        assertTrue(state.progress.hunger >= 25)
    }

    @Test
    fun caviarDoublesClicksForAMinute() {
        val state = funded().act(GameAction.BuyFood("caviar"))
        assertEquals(60.0, state.progress.caviarSeconds)
        assertEquals(2.0, Economy.currentClickReward(state))
    }

    @Test
    fun floorAndWallItemsStayOnTheirSurface() {
        var state = funded().act(GameAction.BuyUpgrade("room-1-basic-1"))
        state = state.act(GameAction.PlaceFurniture("room-1-basic-1", SceneLayout.DESKTOP, 50.0, 10.0))
        assertTrue(state.progress.furniturePositions.getValue("room-1-basic-1").desktop!!.y >= 57)
    }

    @Test
    fun placedFurnitureCanBeTakenOutAndPlacedAgain() {
        var state = funded().act(GameAction.BuyUpgrade("room-1-basic-1"))
        state = state.act(GameAction.PlaceFurniture("room-1-basic-1", SceneLayout.DESKTOP, 30.0, 80.0))
        state = state.act(GameAction.PlaceFurniture("room-1-basic-1", SceneLayout.MOBILE, 40.0, 80.0))
        assertTrue(state.progress.furniturePositions.containsKey("room-1-basic-1"))

        val removed = state.act(GameAction.RemoveFurniture("room-1-basic-1"))
        assertTrue(removed.progress.furniturePositions.isEmpty(), "the item is back in the tray for both layouts")
        assertEquals(1.0, Economy.fishPerSecond(removed), "removing furniture never changes income")
        assertSame(removed, removed.act(GameAction.RemoveFurniture("room-1-basic-1")), "nothing to remove")

        val again = removed.act(GameAction.PlaceFurniture("room-1-basic-1", SceneLayout.DESKTOP, 50.0, 80.0))
        assertNotNull(again.progress.furniturePositions["room-1-basic-1"])
    }

    @Test
    fun unownedFurnitureCannotBePlaced() {
        val state = funded()
        assertSame(state, state.act(GameAction.PlaceFurniture("room-1-advanced-2", SceneLayout.DESKTOP, 30.0, 30.0)))
    }

    @Test
    fun expertModeRaisesPricesAndSpeedsUpHunger() {
        val normal = GameState.initial()
        val expert = GameState.initial(GameMode.EXPERT)
        assertEquals(Economy.catCost(normal, 150) * 2.5, Economy.catCost(expert, 150))
        assertTrue(Economy.hungerDuration(expert.mode) < Economy.hungerDuration(normal.mode))
    }

    @Test
    fun offlineIncomeIsCappedAtEightHours() {
        val state = funded().act(GameAction.BuyUpgrade("room-1-basic-1"))
        val away = GameEngine.applyOfflineProgress(state, 100_000.0)
        val report = assertNotNull(away.offlineReport)
        assertEquals(8.0 * 3600, report.creditedSeconds)
        assertEquals(state.progress.fish + 8 * 3600, away.progress.fish)
    }

    @Test
    fun shortAbsenceIsNotReported() {
        val state = funded().act(GameAction.BuyUpgrade("room-1-basic-1"))
        assertNull(GameEngine.applyOfflineProgress(state, 30.0).offlineReport)
        assertSame(state, GameEngine.applyOfflineProgress(state, 0.5))
    }

    @Test
    fun completingAllFiveRoomsOffersTheFinalAndExpert() {
        var state = GameState.initial()
        for (room in 1..5) {
            for (cat in Cats.forRoom(room)) state = funded(state, 1e13).act(GameAction.BuyCat(cat.id))
            for (upgrade in Upgrades.forRoom(room)) state = funded(state, 1e13).act(GameAction.BuyUpgrade(upgrade.id))
            state = state.act(GameAction.EnterNextRoom)
        }
        assertEquals(5, state.currentRoom)
        assertTrue(Economy.roomComplete(state))
        assertEquals(false, state.finalDismissed)
        val expert = state.act(GameAction.StartExpert)
        assertEquals(GameMode.EXPERT, expert.mode)
        assertEquals(1, expert.currentRoom)
        assertTrue(expert.progress.boughtUpgrades.isEmpty())
    }

    @Test
    fun cannotVisitLockedRooms() {
        val state = GameState.initial()
        assertSame(state, state.act(GameAction.VisitRoom(2)))
    }
}
