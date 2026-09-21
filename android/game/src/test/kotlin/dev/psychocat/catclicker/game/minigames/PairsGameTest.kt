package dev.psychocat.catclicker.game.minigames

import dev.psychocat.catclicker.game.data.Cats
import dev.psychocat.catclicker.game.engine.GameEngine
import dev.psychocat.catclicker.game.minigames.pairs.PairsGame
import dev.psychocat.catclicker.game.minigames.pairs.PairsRound
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

/** Kotlin counterpart of tools/check_pairs.mjs plus the engine wiring of the round. */
class PairsGameTest {
    private fun catIds(room: Int) = Cats.forRoom(room).map { it.id }

    @Test
    fun decksHaveTheRightShape() {
        for (room in 1..5) {
            val ids = catIds(room)
            for (cardCount in PairsGame.CARD_COUNTS) {
                for (seed in 1L..50L) {
                    val round = PairsGame.create(room, GameMode.NORMAL, ids, seed, cardCount)
                    assertEquals(cardCount, round.cards.size)
                    assertEquals(cardCount, round.cards.map { it.id }.toSet().size)
                    val counts = ids.map { id -> round.cards.count { it.catId == id } }.sorted()
                    assertEquals(PairsGame.expectedCatCounts(cardCount), counts)
                }
            }
        }
    }

    @Test
    fun mismatchLocksTheBoardUntilItIsHidden() {
        val ids = catIds(1)
        var round = PairsGame.create(1, GameMode.NORMAL, ids, 20260921L)
        val first = round.cards[0]
        val wrong = round.cards.first { it.catId != first.catId }
        round = PairsGame.reveal(round, first.id)
        round = PairsGame.reveal(round, wrong.id)
        assertEquals(1, round.attempts)
        assertEquals(false, round.lastMatch)
        assertTrue(round.showingMismatch)
        assertSame(round, PairsGame.reveal(round, round.cards[2].id), "board is locked while a mismatch is visible")
        round = PairsGame.hideMismatch(round)
        assertTrue(round.revealed.isEmpty())
        assertSame(round, PairsGame.hideMismatch(round))
    }

    @Test
    fun everySizeCanBeSolved() {
        for (cardCount in PairsGame.CARD_COUNTS) {
            var round = PairsGame.create(1, GameMode.NORMAL, catIds(1), cardCount * 101L, cardCount)
            while (round.status == RoundStatus.PLAYING) {
                val first = round.cards.first { !it.matched }
                val second = round.cards.first { !it.matched && it.id != first.id && it.catId == first.catId }
                round = PairsGame.reveal(round, first.id)
                round = PairsGame.reveal(round, second.id)
            }
            assertEquals(cardCount / 2, round.matches)
            assertTrue(round.cards.all { it.matched })
        }
    }

    @Test
    fun rewardRules() {
        assertEquals(70, PairsGame.baseReward(5, 5, 5, true))
        assertEquals(105.0, PairsGame.fishReward(5, 5, 5, 2, GameMode.NORMAL, true))
        assertEquals(157.0, PairsGame.fishReward(5, 5, 5, 2, GameMode.EXPERT, true))
        assertEquals(20, PairsGame.baseReward(2, 9, 5, false), "no efficiency bonus while playing")
    }

    @Test
    fun onlyOneRoundAndUnsupportedSizesAreIgnored() {
        val state = GameEngine.reduce(GameState.initial(), GameAction.StartPairs(1L, 16))
        assertNotNull(state.activeGame)
        assertSame(state, GameEngine.reduce(state, GameAction.StartPairs(2L, 10)))
        val fresh = GameState.initial()
        assertSame(fresh, GameEngine.reduce(fresh, GameAction.StartPairs(1L, 12)))
    }

    @Test
    fun leavingPaysTheMatchesSoFar() {
        var state = GameEngine.reduce(GameState.initial(), GameAction.StartPairs(4L, 10))
        var round = state.activeGame as PairsRound
        val first = round.cards[0]
        val second = round.cards.first { it.id != first.id && it.catId == first.catId }
        state = GameEngine.reduce(state, GameAction.PairsReveal(first.id))
        state = GameEngine.reduce(state, GameAction.PairsReveal(second.id))
        round = state.activeGame as PairsRound
        assertEquals(1, round.matches)
        state = GameEngine.reduce(state, GameAction.SettleMiniGame)
        assertNull(state.activeGame)
        assertEquals(10.0, state.rooms[0].fish, "one pair = 10 fish in room 1")
    }

    @Test
    fun roundSurvivesSaveAndLoadEvenMidMismatch() {
        var state = GameEngine.reduce(GameState.initial(), GameAction.StartPairs(77L, 16))
        val round = state.activeGame as PairsRound
        val first = round.cards[0]
        val wrong = round.cards.first { it.catId != first.catId }
        state = GameEngine.reduce(state, GameAction.PairsReveal(first.id))
        state = GameEngine.reduce(state, GameAction.PairsReveal(wrong.id))
        assertTrue((state.activeGame as PairsRound).showingMismatch)
        val loaded = assertNotNull(SaveCodec.decode(SaveCodec.encode(state, 1L)))
        assertEquals(state.activeGame, loaded.state.activeGame)
    }

    @Test
    fun brokenSavedDecksAreDropped() {
        val state = GameEngine.reduce(GameState.initial(), GameAction.StartPairs(77L, 10))
        val text = SaveCodec.encode(state, 1L)
        // A deck with a wrong cat distribution must not be restored.
        val round = state.activeGame as PairsRound
        val victim = round.cards[0].catId
        val other = round.cards.first { it.catId != victim }.catId
        val tampered = text.replaceFirst("\"catId\":\"$victim\"", "\"catId\":\"$other\"")
        assertNull(SaveCodec.decode(tampered)!!.state.activeGame)
        assertNotNull(SaveCodec.decode(text)!!.state.activeGame)
    }
}
