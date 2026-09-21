package dev.psychocat.catclicker.game.minigames

import dev.psychocat.catclicker.game.data.Cats
import dev.psychocat.catclicker.game.engine.GameEngine
import dev.psychocat.catclicker.game.minigames.match3.MATCH3_MOVES
import dev.psychocat.catclicker.game.minigames.match3.MATCH3_SIZE
import dev.psychocat.catclicker.game.minigames.match3.Match3Board
import dev.psychocat.catclicker.game.minigames.match3.Match3Game
import dev.psychocat.catclicker.game.minigames.match3.Match3Round
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

/** Kotlin counterpart of tools/check_match3.mjs plus the engine wiring of the round. */
class Match3GameTest {
    private fun catIds(room: Int) = Cats.forRoom(room).map { it.id }

    @Test
    fun startBoardsAreSettledAndPlayable() {
        for (room in 1..5) {
            for (seed in 1L..50L) {
                val round = Match3Game.create(room, GameMode.NORMAL, catIds(room), seed)
                assertEquals(MATCH3_SIZE * MATCH3_SIZE, round.board.size)
                assertTrue(Match3Board.findRuns(round.board).isEmpty(), "no lines at the start")
                assertNotNull(Match3Board.findPossibleSwap(round.board), "a move exists")
            }
        }
    }

    @Test
    fun invalidSwapDoesNotSpendAMove() {
        val ids = catIds(1)
        val round = Match3Game.create(1, GameMode.NORMAL, ids, 20260921L)
        val invalid = round.board.indices.firstNotNullOf { index ->
            listOf(index + 1, index + MATCH3_SIZE)
                .firstOrNull { Match3Board.areAdjacent(index, it) && !Match3Board.swapCreatesMatch(round.board, index, it) }
                ?.let { index to it }
        }
        val turn = Match3Game.play(round, invalid.first, invalid.second, ids)
        assertEquals(false, turn.accepted)
        assertSame(round, turn.round)
    }

    @Test
    fun aWholeRoundOfTwentyMoves() {
        val ids = catIds(1)
        var round = Match3Game.create(1, GameMode.NORMAL, ids, 20260921L)
        while (round.movesLeft > 0) {
            val move = assertNotNull(Match3Board.findPossibleSwap(round.board), "stable board remains playable")
            val turn = Match3Game.play(round, move.first, move.second, ids)
            assertTrue(turn.accepted)
            assertEquals(round.movesLeft - 1, turn.round.movesLeft)
            assertTrue(turn.round.score > round.score)
            assertTrue(Match3Board.findRuns(turn.round.board).isEmpty())
            assertNotNull(Match3Board.findPossibleSwap(turn.round.board))
            round = turn.round
        }
        assertEquals(RoundStatus.FINISHED, round.status)
        assertTrue(round.score > 0)
        assertEquals(MATCH3_MOVES, 20)
    }

    @Test
    fun scoringRules() {
        assertEquals(30, Match3Board.runPoints(3))
        assertEquals(60, Match3Board.runPoints(4))
        assertEquals(100, Match3Board.runPoints(5))
        assertEquals(180, Match3Board.runPoints(7))
        assertEquals(1.0, Match3Board.cascadeMultiplier(1))
        assertEquals(2.0, Match3Board.cascadeMultiplier(3))
        assertEquals(3.0, Match3Board.cascadeMultiplier(9))
    }

    @Test
    fun rewardRules() {
        assertEquals(20.0, Match3Game.fishReward(270, 2, GameMode.NORMAL))
        assertEquals(30.0, Match3Game.fishReward(600, 1, GameMode.NORMAL))
        assertEquals(82.0, Match3Game.fishReward(1200, 2, GameMode.NORMAL))
        assertEquals(123.0, Match3Game.fishReward(1200, 3, GameMode.NORMAL))
        assertEquals(405.0, Match3Game.fishReward(2500, 5, GameMode.NORMAL))
        assertEquals(607.0, Match3Game.fishReward(2500, 5, GameMode.EXPERT))
    }

    @Test
    fun engineStartsPlaysAndSettles() {
        var state = GameEngine.reduce(GameState.initial(), GameAction.StartMatch3(12345L))
        val round = state.activeGame as Match3Round
        assertSame(state, GameEngine.reduce(state, GameAction.StartMatch3(6L)), "only one round at a time")
        val move = Match3Board.findPossibleSwap(round.board)!!
        state = GameEngine.reduce(state, GameAction.Match3Swap(move.first, move.second))
        val played = state.activeGame as Match3Round
        assertEquals(MATCH3_MOVES - 1, played.movesLeft)
        val reward = Match3Game.fishReward(played.score, 1, GameMode.NORMAL)
        state = GameEngine.reduce(state, GameAction.SettleMiniGame)
        assertNull(state.activeGame)
        assertEquals(reward, state.rooms[0].fish)
    }

    @Test
    fun roundSurvivesSaveAndLoad() {
        var state = GameEngine.reduce(GameState.initial(), GameAction.StartMatch3(777L))
        val round = state.activeGame as Match3Round
        val move = Match3Board.findPossibleSwap(round.board)!!
        state = GameEngine.reduce(state, GameAction.Match3Swap(move.first, move.second))
        val loaded = assertNotNull(SaveCodec.decode(SaveCodec.encode(state, 1L)))
        assertEquals(state.activeGame, loaded.state.activeGame)
    }

    @Test
    fun boardWithReadyLinesIsNotRestored() {
        val state = GameEngine.reduce(GameState.initial(), GameAction.StartMatch3(777L))
        val round = state.activeGame as Match3Round
        // Give three neighbours in the first row the same cat: a ready line.
        val cat = round.board[0].catId
        val tampered = SaveCodec.encode(state, 1L)
            .replace("\"id\":${round.board[1].id},\"catId\":\"${round.board[1].catId}\"", "\"id\":${round.board[1].id},\"catId\":\"$cat\"")
            .replace("\"id\":${round.board[2].id},\"catId\":\"${round.board[2].catId}\"", "\"id\":${round.board[2].id},\"catId\":\"$cat\"")
        assertNull(SaveCodec.decode(tampered)!!.state.activeGame)
    }
}
