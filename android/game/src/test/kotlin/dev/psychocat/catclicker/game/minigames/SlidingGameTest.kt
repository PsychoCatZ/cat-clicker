package dev.psychocat.catclicker.game.minigames

import dev.psychocat.catclicker.game.data.Cats
import dev.psychocat.catclicker.game.engine.GameEngine
import dev.psychocat.catclicker.game.minigames.sliding.SlidingBoard
import dev.psychocat.catclicker.game.minigames.sliding.SlidingDifficulty
import dev.psychocat.catclicker.game.minigames.sliding.SlidingEvent
import dev.psychocat.catclicker.game.minigames.sliding.SlidingGame
import dev.psychocat.catclicker.game.minigames.sliding.SlidingRound
import dev.psychocat.catclicker.game.model.GameAction
import dev.psychocat.catclicker.game.model.GameMode
import dev.psychocat.catclicker.game.model.GameState
import dev.psychocat.catclicker.game.save.SaveCodec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** Kotlin counterpart of tools/check_sliding.mjs plus the engine wiring of the round. */
class SlidingGameTest {
    private fun catIds(room: Int) = Cats.forRoom(room).map { it.id }

    @Test
    fun shufflesAreSolvableNotSolvedAndReversible() {
        for (difficulty in SlidingDifficulty.entries) {
            val size = difficulty.size
            for (seed in 1L..50L) {
                val shuffled = SlidingBoard.shuffle(size, seed, difficulty.shuffleMoves)
                assertEquals(size * size, shuffled.tiles.size)
                assertTrue(SlidingBoard.isSolvable(shuffled.tiles, size))
                assertTrue(!SlidingBoard.isSolved(shuffled.tiles))
                assertEquals(size * size, shuffled.tiles.toSet().size)

                var restored = shuffled.tiles
                for (tileId in shuffled.history.reversed()) {
                    restored = assertNotNull(SlidingBoard.move(restored, size, tileId), "reverse move stays legal")
                }
                assertTrue(SlidingBoard.isSolved(restored), "$difficulty shuffle reverses to the goal")
            }
        }
    }

    @Test
    fun roundsUseTheRoomCatsAndStaySolvable() {
        for (room in 1..5) {
            val ids = catIds(room)
            for (difficulty in SlidingDifficulty.entries) {
                val round = SlidingGame.create(room, GameMode.NORMAL, ids, difficulty, 20260921L + room, ids[3])
                assertEquals(ids[3], round.catId)
                assertEquals(difficulty.size, round.size)
                assertTrue(SlidingBoard.isSolvable(round.tiles, round.size))
            }
            val random = SlidingGame.create(room, GameMode.NORMAL, ids, SlidingDifficulty.EASY, 12345L)
            assertTrue(random.catId in ids)
        }
    }

    @Test
    fun illegalMovesAreIgnoredAndLegalOnesCount() {
        val ids = catIds(1)
        val round = SlidingGame.create(1, GameMode.NORMAL, ids, SlidingDifficulty.EASY, 98765L, ids[1])
        val movable = SlidingBoard.movableTileIds(round.tiles, round.size)
        val blocked = round.tiles.filterNotNull().first { it !in movable }
        assertSame(round, SlidingGame.move(round, blocked))
        val moved = SlidingGame.move(round, movable[0])
        assertEquals(1, moved.moves)
        assertNotEquals(round.tiles, moved.tiles)
    }

    @Test
    fun lastMoveFinishesTheRoundWithScore() {
        val ids = catIds(1)
        val start = SlidingGame.create(1, GameMode.NORMAL, ids, SlidingDifficulty.EASY, 98765L, ids[1])
        val goal = SlidingBoard.solved(3)
        val lastTile = 3 * 3 - 2
        val almost: SlidingRound = start.copy(
            tiles = SlidingBoard.move(goal, 3, lastTile)!!, moves = 0, score = 0,
            status = RoundStatus.PLAYING, lastEvent = null,
        )
        val done = SlidingGame.move(almost, lastTile)
        assertEquals(RoundStatus.FINISHED, done.status)
        assertEquals(1, done.moves)
        assertEquals(SlidingGame.completionScore(SlidingDifficulty.EASY, 1), done.score)
        assertEquals(SlidingEvent.COMPLETED, done.lastEvent)
        assertSame(done, SlidingGame.move(done, 0), "finished rounds ignore moves")
        assertSame(done, SlidingGame.reshuffle(done), "finished rounds cannot be reshuffled")
    }

    @Test
    fun reshuffleKeepsTheCatAndResetsMoves() {
        val ids = catIds(1)
        val round = SlidingGame.create(1, GameMode.NORMAL, ids, SlidingDifficulty.NORMAL, 33333L, ids[2])
        val afterMove = SlidingGame.move(round, SlidingBoard.movableTileIds(round.tiles, round.size)[0])
        val again = SlidingGame.reshuffle(afterMove)
        assertEquals(0, again.moves)
        assertEquals(round.catId, again.catId)
        assertTrue(SlidingBoard.isSolvable(again.tiles, again.size))
        assertTrue(!SlidingBoard.isSolved(again.tiles))
    }

    @Test
    fun scoreAndRewardRules() {
        assertEquals(900, SlidingGame.completionScore(SlidingDifficulty.EASY, 1000), "many moves never reduce the base")
        assertTrue(SlidingGame.completionScore(SlidingDifficulty.NORMAL, 1) > SlidingGame.completionScore(SlidingDifficulty.EASY, 1))
        assertTrue(SlidingGame.completionScore(SlidingDifficulty.HARD, 1) > SlidingGame.completionScore(SlidingDifficulty.NORMAL, 1))
        assertEquals(30.0, SlidingGame.fishReward(900, 1, GameMode.NORMAL))
        assertEquals(67.0, SlidingGame.fishReward(900, 2, GameMode.EXPERT))
    }

    @Test
    fun onlyOneRoundAtATime() {
        val state = GameEngine.reduce(GameState.initial(), GameAction.StartSliding(1L, SlidingDifficulty.EASY))
        assertNotNull(state.activeGame)
        assertSame(state, GameEngine.reduce(state, GameAction.StartSliding(2L, SlidingDifficulty.HARD)))
    }

    @Test
    fun settlingPaysTheRoomWhereTheRoundStarted() {
        var state = GameEngine.reduce(GameState.initial(), GameAction.StartSliding(555L, SlidingDifficulty.EASY))
        val round = state.activeGame as SlidingRound
        val history = SlidingBoard.shuffle(3, 555L, SlidingDifficulty.EASY.shuffleMoves).history
        for (tileId in history.reversed()) state = GameEngine.reduce(state, GameAction.SlidingMove(tileId))
        val finished = state.activeGame as SlidingRound
        assertEquals(RoundStatus.FINISHED, finished.status)
        val reward = SlidingGame.fishReward(finished.score, 1, GameMode.NORMAL)
        assertTrue(reward > 0)
        assertEquals(round.roomId, 1)

        state = GameEngine.reduce(state, GameAction.SettleMiniGame)
        assertNull(state.activeGame)
        assertEquals(reward, state.rooms[0].fish)
    }

    @Test
    fun leavingEarlyPaysNothing() {
        var state = GameEngine.reduce(GameState.initial(), GameAction.StartSliding(9L, SlidingDifficulty.HARD))
        state = GameEngine.reduce(state, GameAction.SettleMiniGame)
        assertNull(state.activeGame)
        assertEquals(0.0, state.rooms[0].fish)
    }

    @Test
    fun activeRoundSurvivesSaveAndLoad() {
        var state = GameEngine.reduce(GameState.initial(), GameAction.StartSliding(31L, SlidingDifficulty.NORMAL, "basic-03"))
        val round = state.activeGame as SlidingRound
        state = GameEngine.reduce(state, GameAction.SlidingMove(SlidingBoard.movableTileIds(round.tiles, round.size)[0]))
        val loaded = assertNotNull(SaveCodec.decode(SaveCodec.encode(state, 1L)))
        assertEquals(state.activeGame, loaded.state.activeGame)
    }

    @Test
    fun brokenSavedRoundsAreDropped() {
        val state = GameEngine.reduce(GameState.initial(), GameAction.StartSliding(31L, SlidingDifficulty.EASY))
        val text = SaveCodec.encode(state, 1L)
        // An unsolvable board (two tiles swapped) must not be restored.
        val round = state.activeGame as SlidingRound
        val broken = round.tiles.toMutableList().also {
            val a = it.indexOfFirst { t -> t == 0 }
            val b = it.indexOfFirst { t -> t == 1 }
            it[a] = 1
            it[b] = 0
        }
        val tampered = text.replace(round.tiles.joinToString(",") { it?.toString() ?: "null" }, broken.joinToString(",") { it?.toString() ?: "null" })
        assertNotEquals(text, tampered)
        assertNull(SaveCodec.decode(tampered)!!.state.activeGame)
        // A round of another room is not restored either.
        assertNull(SaveCodec.decode(text.replace("\"currentRoom\":1", "\"currentRoom\":2").replace("\"unlockedRoom\":1", "\"unlockedRoom\":2"))!!.state.activeGame)
    }

    @Test
    fun visitingAnotherRoomSettlesTheRound() {
        var state = GameState.initial().copy(unlockedRoom = 2)
        state = GameEngine.reduce(state, GameAction.StartSliding(3L, SlidingDifficulty.EASY))
        state = GameEngine.reduce(state, GameAction.VisitRoom(2))
        assertNull(state.activeGame)
        assertEquals(2, state.currentRoom)
    }
}
