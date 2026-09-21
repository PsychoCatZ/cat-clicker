package dev.psychocat.catclicker.game.engine

import dev.psychocat.catclicker.game.data.FurniturePoint
import dev.psychocat.catclicker.game.data.SceneLayout
import dev.psychocat.catclicker.game.minigames.RoundStatus
import dev.psychocat.catclicker.game.minigames.sliding.SlidingDifficulty
import dev.psychocat.catclicker.game.minigames.sliding.SlidingRound
import dev.psychocat.catclicker.game.model.GameAction
import dev.psychocat.catclicker.game.model.GameMode
import dev.psychocat.catclicker.game.model.GameState
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.contentOrNull
import kotlin.math.abs
import kotlin.math.max
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Replays sessions recorded from the web reducer (`node tools/android/export_engine_golden.mjs`)
 * and checks the Kotlin engine ends up in the same state.
 */
class EngineParityTest {
    private val scenarios = Json.parseToJsonElement(
        javaClass.getResourceAsStream("/golden/engine.json")!!.bufferedReader(Charsets.UTF_8).readText(),
    ).jsonObject.getValue("scenarios").jsonArray

    private fun action(json: JsonObject): GameAction? {
        fun string(key: String) = json.getValue(key).jsonPrimitive.content
        return when (val type = string("type")) {
            "click" -> GameAction.Click
            "tick" -> GameAction.Tick(json.getValue("seconds").jsonPrimitive.double)
            "toggleLights" -> GameAction.ToggleLights
            "dismissOfflineReport" -> GameAction.DismissOfflineReport
            "buyResource" -> GameAction.BuyResource(string("id"))
            "buyUpgrade" -> GameAction.BuyUpgrade(string("id"))
            "placeFurniture" -> {
                val layout = SceneLayout.entries.firstOrNull { it.key == string("layout") }
                // The web reducer ignores unknown layouts, so the step changes nothing.
                if (layout == null) null
                else GameAction.PlaceFurniture(string("id"), layout, json.getValue("x").jsonPrimitive.double, json.getValue("y").jsonPrimitive.double)
            }
            "buyFood" -> GameAction.BuyFood(string("id"))
            "buyCat" -> GameAction.BuyCat(string("id"))
            "selectCat" -> GameAction.SelectCat(string("id"))
            "visitRoom" -> GameAction.VisitRoom(json.getValue("roomId").jsonPrimitive.int)
            "enterNextRoom" -> GameAction.EnterNextRoom
            "dismissFinal" -> GameAction.DismissFinal
            "showFinal" -> GameAction.ShowFinal
            "startExpert" -> GameAction.StartExpert
            "reset" -> GameAction.Reset
            "startSliding" -> GameAction.StartSliding(
                seed = json.getValue("seed").jsonPrimitive.double.toLong(),
                difficulty = SlidingDifficulty.fromKey(string("difficulty"))!!,
                catId = json["catId"]?.jsonPrimitive?.contentOrNull,
            )
            "slidingMove" -> GameAction.SlidingMove(json.getValue("tileId").jsonPrimitive.int)
            "slidingReshuffle" -> GameAction.SlidingReshuffle
            "settleSliding" -> GameAction.SettleMiniGame
            else -> error("Unknown action $type")
        }
    }

    private fun apply(state: GameState, step: JsonObject): GameState = when {
        "a" in step -> action(step.getValue("a").jsonObject)?.let { GameEngine.reduce(state, it) } ?: state
        "fund" in step -> state.copy(
            rooms = state.rooms.mapIndexed { index, room ->
                if (index == state.currentRoom - 1) room.copy(fish = step.getValue("fund").jsonPrimitive.double) else room
            },
        )
        "offline" in step -> GameEngine.applyOfflineProgress(state, step.getValue("offline").jsonPrimitive.double)
        else -> error("Unknown step")
    }

    private fun assertClose(expected: Double, actual: Double, label: String) {
        val tolerance = 1e-9 * max(1.0, abs(expected))
        assertTrue(abs(expected - actual) <= tolerance, "$label: expected $expected but was $actual")
    }

    private fun assertPoint(expected: JsonElement?, actual: FurniturePoint?, label: String) {
        if (expected == null || expected is JsonNull) {
            assertEquals(null, actual, label)
            return
        }
        assertClose(expected.jsonObject.getValue("x").jsonPrimitive.double, actual!!.x, "$label.x")
        assertClose(expected.jsonObject.getValue("y").jsonPrimitive.double, actual.y, "$label.y")
    }

    private fun assertSliding(expected: JsonElement?, state: GameState, label: String) {
        val round = state.activeGame
        if (expected == null || expected is JsonNull) {
            assertEquals(null, round, "$label active game")
            return
        }
        val web = expected.jsonObject
        val actual = round as? SlidingRound ?: error("$label: expected a sliding round but was $round")
        assertEquals(web.getValue("id").jsonPrimitive.content, actual.id, "$label sliding id")
        assertEquals(web.getValue("roomId").jsonPrimitive.int, actual.roomId, "$label sliding room")
        assertEquals(web.getValue("mode").jsonPrimitive.content == "expert", actual.mode == GameMode.EXPERT, "$label sliding mode")
        assertEquals(web.getValue("difficulty").jsonPrimitive.content, actual.difficulty.key, "$label sliding difficulty")
        assertEquals(web.getValue("catId").jsonPrimitive.content, actual.catId, "$label sliding cat")
        assertEquals(
            web.getValue("tiles").jsonArray.map { if (it is JsonNull) null else it.jsonPrimitive.int },
            actual.tiles, "$label sliding tiles",
        )
        assertEquals(web.getValue("moves").jsonPrimitive.int, actual.moves, "$label sliding moves")
        assertEquals(web.getValue("score").jsonPrimitive.int, actual.score, "$label sliding score")
        assertEquals(web.getValue("rngState").jsonPrimitive.long, actual.rngState, "$label sliding rng")
        assertEquals(web.getValue("status").jsonPrimitive.content, if (actual.status == RoundStatus.FINISHED) "finished" else "playing", "$label sliding status")
        assertEquals(web["lastEvent"]?.takeIf { it !is JsonNull }?.jsonPrimitive?.content, actual.lastEvent?.key, "$label sliding event")
    }

    private fun assertSnapshot(expected: JsonObject, state: GameState, label: String) {
        assertEquals(expected.getValue("mode").jsonPrimitive.content == "expert", state.mode == GameMode.EXPERT, "$label mode")
        assertEquals(expected.getValue("currentRoom").jsonPrimitive.int, state.currentRoom, "$label currentRoom")
        assertEquals(expected.getValue("unlockedRoom").jsonPrimitive.int, state.unlockedRoom, "$label unlockedRoom")
        assertEquals(expected.getValue("finalDismissed").jsonPrimitive.boolean, state.finalDismissed, "$label finalDismissed")

        val report = expected["offlineReport"]
        if (report == null || report is JsonNull) {
            assertEquals(null, state.offlineReport, "$label offlineReport")
        } else {
            val actual = state.offlineReport ?: error("$label: missing offline report")
            assertClose(report.jsonObject.getValue("elapsedSeconds").jsonPrimitive.double, actual.elapsedSeconds, "$label report.elapsed")
            assertClose(report.jsonObject.getValue("creditedSeconds").jsonPrimitive.double, actual.creditedSeconds, "$label report.credited")
            assertClose(report.jsonObject.getValue("fishEarned").jsonPrimitive.double, actual.fishEarned, "$label report.fish")
            assertClose(report.jsonObject.getValue("hungerSpent").jsonPrimitive.double, actual.hungerSpent, "$label report.hunger")
        }

        assertSliding(expected["sliding"], state, label)

        val rooms = expected.getValue("rooms").jsonArray
        assertEquals(rooms.size, state.rooms.size)
        rooms.forEachIndexed { index, item ->
            val web = item.jsonObject
            val room = state.rooms[index]
            val where = "$label room ${index + 1}"
            assertClose(web.getValue("fish").jsonPrimitive.double, room.fish, "$where fish")
            assertClose(web.getValue("hunger").jsonPrimitive.double, room.hunger, "$where hunger")
            assertClose(web.getValue("caviarSeconds").jsonPrimitive.double, room.caviarSeconds, "$where caviar")
            assertEquals(web.getValue("lightsOff").jsonPrimitive.boolean, room.lightsOff, "$where lights")
            assertEquals(web.getValue("selectedCat").jsonPrimitive.content, room.selectedCat, "$where selectedCat")
            assertEquals(web.getValue("boughtUpgrades").jsonArray.map { it.jsonPrimitive.content }, room.boughtUpgrades, "$where upgrades")
            assertEquals(web.getValue("unlockedCats").jsonArray.map { it.jsonPrimitive.content }, room.unlockedCats, "$where cats")
            assertEquals(
                web.getValue("resourceLevels").jsonObject.mapValues { it.value.jsonPrimitive.int },
                room.resourceLevels, "$where resource levels",
            )
            val positions = web.getValue("furniturePositions").jsonObject
            assertEquals(positions.keys, room.furniturePositions.keys, "$where furniture ids")
            positions.forEach { (id, value) ->
                val actual = room.furniturePositions.getValue(id)
                assertPoint(value.jsonObject["desktop"], actual.desktop, "$where $id desktop")
                assertPoint(value.jsonObject["mobile"], actual.mobile, "$where $id mobile")
            }
        }
    }

    @Test
    fun kotlinEngineReplaysEveryRecordedWebSession() {
        assertTrue(scenarios.size >= 9, "golden file looks incomplete")
        for (scenario in scenarios) {
            val name = scenario.jsonObject.getValue("name").jsonPrimitive.content
            var state = GameState.initial()
            var checked = 0
            scenario.jsonObject.getValue("steps").jsonArray.forEachIndexed { index, item ->
                val step = item.jsonObject
                state = apply(state, step)
                val snap = step["snap"]
                if (snap != null) {
                    assertSnapshot(snap.jsonObject, state, "[$name step $index ${step - "snap"}]")
                    checked += 1
                }
            }
            assertTrue(checked > 0, "$name: nothing was compared")
        }
    }
}
