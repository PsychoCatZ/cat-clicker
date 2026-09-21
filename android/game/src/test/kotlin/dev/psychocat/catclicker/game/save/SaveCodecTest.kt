package dev.psychocat.catclicker.game.save

import dev.psychocat.catclicker.game.data.SceneLayout
import dev.psychocat.catclicker.game.engine.GameEngine
import dev.psychocat.catclicker.game.model.GameAction
import dev.psychocat.catclicker.game.model.GameMode
import dev.psychocat.catclicker.game.model.GameState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SaveCodecTest {
    private fun GameState.act(action: GameAction) = GameEngine.reduce(this, action)

    private fun funded(state: GameState, amount: Double = 1e9) = state.copy(
        rooms = state.rooms.mapIndexed { i, room -> if (i == state.currentRoom - 1) room.copy(fish = amount) else room },
    )

    private fun playedState(): GameState {
        var state = funded(GameState.initial())
        state = state.act(GameAction.BuyUpgrade("room-1-basic-1")).act(GameAction.BuyCat("basic-02"))
        state = state.act(GameAction.BuyResource("fish")).act(GameAction.BuyFood("caviar"))
        state = state.act(GameAction.PlaceFurniture("room-1-basic-1", SceneLayout.DESKTOP, 33.3, 80.0))
        state = state.act(GameAction.PlaceFurniture("room-1-basic-1", SceneLayout.MOBILE, 44.4, 70.0))
        state = state.act(GameAction.Tick(1.5)).act(GameAction.ToggleLights)
        return state
    }

    @Test
    fun roundTripKeepsEverything() {
        val state = playedState()
        val loaded = assertNotNull(SaveCodec.decode(SaveCodec.encode(state, 1234L)))
        assertEquals(state, loaded.state)
        assertEquals(1234L, loaded.savedAtMillis)
    }

    @Test
    fun expertModeAndUnlockedRoomsSurvive() {
        var state = GameState.initial(GameMode.EXPERT)
        state = state.copy(unlockedRoom = 3, currentRoom = 2, finalDismissed = true)
        val loaded = assertNotNull(SaveCodec.decode(SaveCodec.encode(state, 1L)))
        assertEquals(state, loaded.state)
    }

    @Test
    fun offlineReportIsNeverStored() {
        val state = playedState().let { GameEngine.applyOfflineProgress(it.copy(rooms = it.rooms.map { r -> r.copy(lightsOff = false) }), 7200.0) }
        assertNotNull(state.offlineReport)
        assertNull(assertNotNull(SaveCodec.decode(SaveCodec.encode(state, 1L))).state.offlineReport)
    }

    @Test
    fun garbageIsRejected() {
        assertNull(SaveCodec.decode(""))
        assertNull(SaveCodec.decode("not json"))
        assertNull(SaveCodec.decode("[1,2,3]"))
        assertNull(SaveCodec.decode("""{"rooms":"oops"}"""))
        assertNull(SaveCodec.decode("""{"schemaVersion":99}"""))
    }

    @Test
    fun emptyObjectBecomesAFreshGame() {
        val loaded = assertNotNull(SaveCodec.decode("{}"))
        assertEquals(GameState.initial(), loaded.state)
    }

    @Test
    fun brokenValuesAreRepaired() {
        val text = """
            {"schemaVersion":1,"savedAt":-5,"mode":"weird","currentRoom":9,"unlockedRoom":2,
             "rooms":[{"fish":-3,"hunger":500,"caviarSeconds":9999,
                       "resourceLevels":{"fish":5000,"nonsense":4},
                       "boughtUpgrades":["room-1-basic-1","room-1-basic-1","room-2-basic-1","junk"],
                       "furniturePositions":{"room-1-basic-1":{"desktop":{"x":500,"y":1},"mobile":{"x":10,"y":90}},
                                             "room-1-basic-2":{"desktop":{"x":1,"y":1}}},
                       "unlockedCats":["rare-01","basic-03","basic-03"],"selectedCat":"basic-05"}]}
        """.trimIndent()
        val loaded = assertNotNull(SaveCodec.decode(text))
        val state = loaded.state
        assertEquals(0L, loaded.savedAtMillis)
        assertEquals(GameMode.NORMAL, state.mode)
        assertEquals(2, state.unlockedRoom)
        assertEquals(2, state.currentRoom, "current room is clamped to the unlocked ones")
        val room = state.rooms[0]
        assertEquals(0.0, room.fish)
        assertEquals(100.0, room.hunger)
        assertEquals(60.0, room.caviarSeconds)
        assertEquals(1000, room.resourceLevels["fish"])
        assertEquals(listOf("room-1-basic-1"), room.boughtUpgrades)
        assertEquals(setOf("room-1-basic-1"), room.furniturePositions.keys)
        val position = room.furniturePositions.getValue("room-1-basic-1")
        assertNull(position.desktop, "out-of-range point is dropped")
        assertEquals(10.0, position.mobile?.x)
        assertEquals(listOf("basic-01", "basic-03"), room.unlockedCats)
        assertEquals("basic-01", room.selectedCat, "selected cat must be an unlocked one")
        assertTrue(state.rooms.drop(1).all { it.fish == 0.0 && it.hunger == 100.0 }, "missing rooms start fresh")
    }
}
