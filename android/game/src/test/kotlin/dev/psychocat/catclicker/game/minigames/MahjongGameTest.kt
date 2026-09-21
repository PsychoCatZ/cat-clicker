package dev.psychocat.catclicker.game.minigames

import dev.psychocat.catclicker.game.data.Cats
import dev.psychocat.catclicker.game.engine.GameEngine
import dev.psychocat.catclicker.game.minigames.mahjong.MahjongBoard
import dev.psychocat.catclicker.game.minigames.mahjong.MahjongDifficulty
import dev.psychocat.catclicker.game.minigames.mahjong.MahjongGame
import dev.psychocat.catclicker.game.minigames.mahjong.MahjongLayouts
import dev.psychocat.catclicker.game.minigames.mahjong.MahjongRound
import dev.psychocat.catclicker.game.model.GameAction
import dev.psychocat.catclicker.game.model.GameMode
import dev.psychocat.catclicker.game.model.GameState
import dev.psychocat.catclicker.game.save.SaveCodec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** Kotlin counterpart of tools/check_mahjong.mjs plus the engine wiring of the round. */
class MahjongGameTest {
    private fun catIds(room: Int) = Cats.forRoom(room).map { it.id }

    @Test
    fun layoutsAreClearableInPairs() {
        val expected = mapOf(MahjongDifficulty.EASY to 24, MahjongDifficulty.NORMAL to 40, MahjongDifficulty.HARD to 56)
        for (difficulty in MahjongDifficulty.entries) {
            val layout = MahjongLayouts.of(difficulty)
            assertEquals(expected.getValue(difficulty), layout.tileCount)
            assertEquals(layout.tileCount, layout.slots.map { it.id }.toSet().size)
            val sequence = assertNotNull(MahjongBoard.findRemovalSequence(layout.slots), "$difficulty has a geometric solution")
            assertEquals(layout.tileCount / 2, sequence.size)
        }
    }

    @Test
    fun dealtCatsAlwaysAllowAFullClear() {
        for (difficulty in MahjongDifficulty.entries) {
            val layout = MahjongLayouts.of(difficulty)
            val sequence = MahjongBoard.findRemovalSequence(layout.slots)!!
            for (room in 1..5) {
                for (seed in 1L..20L) {
                    var round = MahjongGame.create(room, GameMode.NORMAL, catIds(room), difficulty, seed)
                    assertTrue(round.tiles.all { it.catId in catIds(room) })
                    for ((first, second) in sequence) {
                        assertTrue(MahjongBoard.isFree(round.tiles, first))
                        assertTrue(MahjongBoard.isFree(round.tiles, second))
                        assertEquals(round.tiles.first { it.id == first }.catId, round.tiles.first { it.id == second }.catId)
                        round = MahjongGame.select(round, first)
                        round = MahjongGame.select(round, second)
                    }
                    assertEquals(RoundStatus.FINISHED, round.status)
                    assertEquals(layout.tileCount / 2, round.pairsFound)
                    assertEquals(
                        layout.tileCount / 2 * MahjongGame.PAIR_POINTS + MahjongGame.CLEAR_BONUS + MahjongGame.NO_HINT_BONUS + MahjongGame.NO_SHUFFLE_BONUS,
                        round.score,
                    )
                }
            }
        }
    }

    @Test
    fun blockedTilesCannotBeSelected() {
        val round = MahjongGame.create(1, GameMode.NORMAL, catIds(1), MahjongDifficulty.EASY, 20260921L)
        val blocked = round.tiles.first { !MahjongBoard.isFree(round.tiles, it.id) }
        assertSame(round, MahjongGame.select(round, blocked.id))
    }

    @Test
    fun hintsAreFree() {
        var round = MahjongGame.create(1, GameMode.NORMAL, catIds(1), MahjongDifficulty.EASY, 20260921L)
        round = MahjongGame.hint(round)
        assertEquals(1, round.hintsUsed)
        assertEquals(2, round.hintedIds.size)
        assertTrue(round.hintedIds.all { MahjongBoard.isFree(round.tiles, it) })
        assertEquals(round.tiles.first { it.id == round.hintedIds[0] }.catId, round.tiles.first { it.id == round.hintedIds[1] }.catId)
        assertEquals(0, round.score, "hints never cost points")
    }

    @Test
    fun aMismatchOnlyMovesTheSelection() {
        var round = MahjongGame.create(1, GameMode.NORMAL, catIds(1), MahjongDifficulty.EASY, 30303L)
        val free = MahjongBoard.freeTiles(round.tiles)
        val first = free.first { a -> free.any { it.id != a.id && it.catId != a.catId } }
        val second = free.first { it.id != first.id && it.catId != first.catId }
        round = MahjongGame.select(round, first.id)
        round = MahjongGame.select(round, second.id)
        assertEquals(second.id, round.selectedId)
        assertEquals(0, round.score)
        assertTrue(round.tiles.none { it.removed })
    }

    @Test
    fun deadEndShuffleRestoresAPair() {
        val ids = catIds(1)
        var round = MahjongGame.create(1, GameMode.NORMAL, ids, MahjongDifficulty.EASY, 40404L)
        val sequence = MahjongBoard.findRemovalSequence(MahjongLayouts.of(MahjongDifficulty.EASY).slots)!!
        val removedIds = sequence.dropLast(2).flatMap { listOf(it.first, it.second) }.toSet()
        var tiles = round.tiles.map { if (it.id in removedIds) it.copy(removed = true) else it }
        // Make the few tiles left all different cats: no available pair.
        val lastFree = MahjongBoard.freeTiles(tiles)
        assertTrue(lastFree.size <= ids.size)
        val recolored = lastFree.mapIndexed { index, tile -> tile.id to ids[index] }.toMap()
        tiles = tiles.map { if (it.id in recolored) it.copy(catId = recolored.getValue(it.id)) else it }
        round = round.copy(tiles = tiles)
        assertTrue(MahjongBoard.findPairs(round.tiles).isEmpty())

        val shuffled = MahjongGame.shuffle(round, ids)
        assertEquals(1, shuffled.shuffles)
        assertTrue(MahjongBoard.findPairs(shuffled.tiles).isNotEmpty(), "shuffle restores an available pair")
        val rest = MahjongBoard.findRemovalSequence(shuffled.tiles.filter { !it.removed }.map { it.slot })!!
        for ((first, second) in rest) {
            assertEquals(shuffled.tiles.first { it.id == first }.catId, shuffled.tiles.first { it.id == second }.catId)
        }
        assertSame(shuffled, MahjongGame.shuffle(shuffled, ids), "shuffling is refused while a pair exists")
    }

    @Test
    fun scoringAndRewards() {
        assertEquals(900, MahjongGame.completionBonus(0, 0))
        assertEquals(700, MahjongGame.completionBonus(1, 0))
        assertEquals(700, MahjongGame.completionBonus(0, 1))
        assertEquals(70.0, MahjongGame.fishReward(2100, 1, GameMode.NORMAL))
        assertEquals(157.0, MahjongGame.fishReward(2100, 2, GameMode.EXPERT))
    }

    @Test
    fun engineStartsPlaysAndSettles() {
        var state = GameEngine.reduce(GameState.initial(), GameAction.StartMahjong(24680L, MahjongDifficulty.EASY))
        assertSame(state, GameEngine.reduce(state, GameAction.StartMahjong(1L, MahjongDifficulty.HARD)))
        val round = state.activeGame as MahjongRound
        val pair = MahjongBoard.findPairs(round.tiles).first()
        state = GameEngine.reduce(state, GameAction.MahjongSelect(pair.first))
        state = GameEngine.reduce(state, GameAction.MahjongSelect(pair.second))
        assertEquals(100, (state.activeGame as MahjongRound).score)
        val reward = MahjongGame.fishReward(100, 1, GameMode.NORMAL)
        state = GameEngine.reduce(state, GameAction.SettleMiniGame)
        assertNull(state.activeGame)
        assertEquals(reward, state.rooms[0].fish, "early exit still pays what was earned")
    }

    @Test
    fun roundSurvivesSaveAndLoad() {
        var state = GameEngine.reduce(GameState.initial(), GameAction.StartMahjong(99L, MahjongDifficulty.NORMAL))
        val round = state.activeGame as MahjongRound
        val pair = MahjongBoard.findPairs(round.tiles).first()
        state = GameEngine.reduce(state, GameAction.MahjongSelect(pair.first))
        state = GameEngine.reduce(state, GameAction.MahjongSelect(pair.second))
        state = GameEngine.reduce(state, GameAction.MahjongHint)
        val loaded = assertNotNull(SaveCodec.decode(SaveCodec.encode(state, 1L)))
        assertEquals(state.activeGame, loaded.state.activeGame)
    }

    @Test
    fun brokenSavedBoardIsDropped() {
        val state = GameEngine.reduce(GameState.initial(), GameAction.StartMahjong(99L, MahjongDifficulty.EASY))
        val round = state.activeGame as MahjongRound
        // Marking one tile as removed leaves an odd number of removed tiles: not a valid position.
        val target = round.tiles[0]
        val text = SaveCodec.encode(state, 1L)
        val tampered = text.replace(
            "\"id\":${target.id},\"catId\":\"${target.catId}\",\"removed\":false",
            "\"id\":${target.id},\"catId\":\"${target.catId}\",\"removed\":true",
        )
        assertTrue(tampered != text)
        assertNull(SaveCodec.decode(tampered)!!.state.activeGame)
    }
}
