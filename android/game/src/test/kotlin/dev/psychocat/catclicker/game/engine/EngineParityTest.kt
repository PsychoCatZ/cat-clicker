package dev.psychocat.catclicker.game.engine

import dev.psychocat.catclicker.game.data.FurniturePoint
import dev.psychocat.catclicker.game.data.SceneLayout
import dev.psychocat.catclicker.game.minigames.RoundStatus
import dev.psychocat.catclicker.game.minigames.mahjong.MahjongDifficulty
import dev.psychocat.catclicker.game.minigames.mahjong.MahjongRound
import dev.psychocat.catclicker.game.minigames.match3.Match3Round
import dev.psychocat.catclicker.game.minigames.pairs.PairsRound
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
            "startMahjong" -> GameAction.StartMahjong(
                seed = json.getValue("seed").jsonPrimitive.double.toLong(),
                difficulty = MahjongDifficulty.fromKey(string("difficulty"))!!,
            )
            "mahjongSelect" -> GameAction.MahjongSelect(json.getValue("tileId").jsonPrimitive.int)
            "mahjongHint" -> GameAction.MahjongHint
            "mahjongShuffle" -> GameAction.MahjongShuffle
            "settleMahjong" -> GameAction.SettleMiniGame
            "startMatch3" -> GameAction.StartMatch3(json.getValue("seed").jsonPrimitive.double.toLong())
            "match3Swap" -> GameAction.Match3Swap(json.getValue("first").jsonPrimitive.int, json.getValue("second").jsonPrimitive.int)
            "settleMatch3" -> GameAction.SettleMiniGame
            "startPairs" -> GameAction.StartPairs(
                seed = json.getValue("seed").jsonPrimitive.double.toLong(),
                cardCount = json.getValue("cardCount").jsonPrimitive.int,
            )
            "pairsReveal" -> GameAction.PairsReveal(json.getValue("cardId").jsonPrimitive.int)
            "pairsHideMismatch" -> GameAction.PairsHideMismatch
            "settlePairs" -> GameAction.SettleMiniGame
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

    private fun assertActiveGame(expected: JsonObject, state: GameState, label: String) {
        val sliding = expected["sliding"]?.takeIf { it !is JsonNull }
        val pairs = expected["pairs"]?.takeIf { it !is JsonNull }
        val match3 = expected["match3"]?.takeIf { it !is JsonNull }
        val mahjong = expected["mahjong"]?.takeIf { it !is JsonNull }
        when {
            sliding != null -> assertSliding(sliding, state, label)
            pairs != null -> assertPairs(pairs.jsonObject, state, label)
            match3 != null -> assertMatch3(match3.jsonObject, state, label)
            mahjong != null -> assertMahjong(mahjong.jsonObject, state, label)
            else -> assertEquals(null, state.activeGame, "$label active game")
        }
    }

    private fun assertMahjong(web: JsonObject, state: GameState, label: String) {
        val actual = state.activeGame as? MahjongRound ?: error("$label: expected a mahjong round but was ${state.activeGame}")
        assertEquals(web.getValue("id").jsonPrimitive.content, actual.id, "$label mahjong id")
        assertEquals(web.getValue("roomId").jsonPrimitive.int, actual.roomId, "$label mahjong room")
        assertEquals(web.getValue("difficulty").jsonPrimitive.content, actual.difficulty.key, "$label mahjong difficulty")
        assertEquals(
            web.getValue("tiles").jsonArray.map {
                val tile = it.jsonObject
                Triple(tile.getValue("id").jsonPrimitive.int, tile.getValue("catId").jsonPrimitive.content, tile.getValue("removed").jsonPrimitive.boolean)
            },
            actual.tiles.map { Triple(it.id, it.catId, it.removed) }, "$label mahjong tiles",
        )
        assertEquals(web.getValue("selectedId").takeIf { it !is JsonNull }?.jsonPrimitive?.int, actual.selectedId, "$label mahjong selected")
        assertEquals(web.getValue("hintedIds").jsonArray.map { it.jsonPrimitive.int }, actual.hintedIds, "$label mahjong hinted")
        assertEquals(web.getValue("score").jsonPrimitive.int, actual.score, "$label mahjong score")
        assertEquals(web.getValue("pairsFound").jsonPrimitive.int, actual.pairsFound, "$label mahjong pairs")
        assertEquals(web.getValue("hintsUsed").jsonPrimitive.int, actual.hintsUsed, "$label mahjong hints")
        assertEquals(web.getValue("shuffles").jsonPrimitive.int, actual.shuffles, "$label mahjong shuffles")
        assertEquals(web.getValue("rngState").jsonPrimitive.long, actual.rngState, "$label mahjong rng")
        assertEquals(web.getValue("status").jsonPrimitive.content, if (actual.status == RoundStatus.FINISHED) "finished" else "playing", "$label mahjong status")
        assertEquals(web["lastEvent"]?.takeIf { it !is JsonNull }?.jsonPrimitive?.content, actual.lastEvent?.key, "$label mahjong event")
    }

    private fun assertMatch3(web: JsonObject, state: GameState, label: String) {
        val actual = state.activeGame as? Match3Round ?: error("$label: expected a match-3 round but was ${state.activeGame}")
        assertEquals(web.getValue("id").jsonPrimitive.content, actual.id, "$label match3 id")
        assertEquals(web.getValue("roomId").jsonPrimitive.int, actual.roomId, "$label match3 room")
        assertEquals(
            web.getValue("board").jsonArray.map { it.jsonObject.getValue("id").jsonPrimitive.int to it.jsonObject.getValue("catId").jsonPrimitive.content },
            actual.board.map { it.id to it.catId }, "$label match3 board",
        )
        assertEquals(web.getValue("movesLeft").jsonPrimitive.int, actual.movesLeft, "$label match3 movesLeft")
        assertEquals(web.getValue("score").jsonPrimitive.int, actual.score, "$label match3 score")
        assertEquals(web.getValue("maxCombo").jsonPrimitive.int, actual.maxCombo, "$label match3 maxCombo")
        assertEquals(web.getValue("rngState").jsonPrimitive.long, actual.rngState, "$label match3 rng")
        assertEquals(web.getValue("nextTileId").jsonPrimitive.int, actual.nextTileId, "$label match3 nextTileId")
        assertEquals(web.getValue("status").jsonPrimitive.content, if (actual.status == RoundStatus.FINISHED) "finished" else "playing", "$label match3 status")
        assertEquals(web.getValue("lastGain").jsonPrimitive.int, actual.lastGain, "$label match3 lastGain")
        assertEquals(web.getValue("lastCombo").jsonPrimitive.int, actual.lastCombo, "$label match3 lastCombo")
        assertEquals(web.getValue("shuffled").jsonPrimitive.boolean, actual.shuffled, "$label match3 shuffled")
    }

    private fun assertPairs(web: JsonObject, state: GameState, label: String) {
        val actual = state.activeGame as? PairsRound ?: error("$label: expected a pairs round but was ${state.activeGame}")
        assertEquals(web.getValue("id").jsonPrimitive.content, actual.id, "$label pairs id")
        assertEquals(web.getValue("roomId").jsonPrimitive.int, actual.roomId, "$label pairs room")
        assertEquals(web.getValue("mode").jsonPrimitive.content == "expert", actual.mode == GameMode.EXPERT, "$label pairs mode")
        assertEquals(web.getValue("cardCount").jsonPrimitive.int, actual.cardCount, "$label pairs size")
        assertEquals(
            web.getValue("cards").jsonArray.map {
                val card = it.jsonObject
                Triple(card.getValue("id").jsonPrimitive.int, card.getValue("catId").jsonPrimitive.content, card.getValue("matched").jsonPrimitive.boolean)
            },
            actual.cards.map { Triple(it.id, it.catId, it.matched) }, "$label pairs cards",
        )
        assertEquals(web.getValue("revealed").jsonArray.map { it.jsonPrimitive.int }, actual.revealed, "$label pairs revealed")
        assertEquals(web.getValue("attempts").jsonPrimitive.int, actual.attempts, "$label pairs attempts")
        assertEquals(web.getValue("matches").jsonPrimitive.int, actual.matches, "$label pairs matches")
        assertEquals(web.getValue("status").jsonPrimitive.content, if (actual.status == RoundStatus.FINISHED) "finished" else "playing", "$label pairs status")
        val lastMatch = web["lastMatch"]?.takeIf { it !is JsonNull }?.jsonPrimitive?.boolean
        assertEquals(lastMatch, actual.lastMatch, "$label pairs lastMatch")
        assertEquals(web.getValue("rngState").jsonPrimitive.long, actual.rngState, "$label pairs rng")
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

        assertActiveGame(expected, state, label)

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
        assertTrue(scenarios.size >= 26, "golden file looks incomplete")
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
