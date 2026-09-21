package dev.psychocat.catclicker.game.minigames.pairs

import dev.psychocat.catclicker.game.Balance
import dev.psychocat.catclicker.game.minigames.MiniGameRound
import dev.psychocat.catclicker.game.minigames.RoundStatus
import dev.psychocat.catclicker.game.model.GameMode
import dev.psychocat.catclicker.game.rng.Xorshift32
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

data class PairCard(val id: Int, val catId: String, val matched: Boolean)

/**
 * State of one "Find the pair" round. Port of `PairsRound` in src/game/pairs/types.ts.
 * [revealed] holds the ids of at most two face-up cards that are not matched yet.
 * After two different cards were opened ([lastMatch] == false) the board stays locked until [PairsGame.hideMismatch].
 */
data class PairsRound(
    val id: String,
    override val roomId: Int,
    override val mode: GameMode,
    val cardCount: Int,
    val cards: List<PairCard>,
    val revealed: List<Int>,
    val attempts: Int,
    val matches: Int,
    override val status: RoundStatus,
    val lastMatch: Boolean?,
    val rngState: Long,
) : MiniGameRound {
    val totalPairs: Int get() = cardCount / 2

    /** True while two different cards are face up and the player has to see them before they turn back. */
    val showingMismatch: Boolean get() = revealed.size == 2 && lastMatch == false
}

/** Port of src/game/pairs/{reducer,scoring}.ts. */
object PairsGame {
    /** Allowed board sizes: 5 pairs, 8 pairs, 10 pairs. */
    val CARD_COUNTS: List<Int> = listOf(10, 16, 20)

    /**
     * How long two different cards stay face up. The web version used 850 ms; the Android version gives the player
     * two seconds (agreed for an elderly player), and a tap turns the cards back sooner.
     */
    const val MISMATCH_SHOW_MILLIS = 2000L

    /** Cards per cat that a valid deck has, sorted; anything else in a save file is rejected. */
    fun expectedCatCounts(cardCount: Int): List<Int> = when (cardCount) {
        10 -> listOf(2, 2, 2, 2, 2)
        16 -> listOf(2, 2, 4, 4, 4)
        else -> listOf(4, 4, 4, 4, 4)
    }

    private fun modeKey(mode: GameMode) = if (mode == GameMode.EXPERT) "expert" else "normal"

    private class Shuffled(val cards: List<PairCard>, val rngState: Long)

    private fun shuffle(cards: List<PairCard>, seed: Long): Shuffled {
        val shuffled = cards.toMutableList()
        var rng = seed
        for (index in shuffled.size - 1 downTo 1) {
            val step = Xorshift32.step(rng)
            rng = step.state
            val target = floor(step.value * (index + 1)).toInt()
            val swap = shuffled[index]
            shuffled[index] = shuffled[target]
            shuffled[target] = swap
        }
        return Shuffled(shuffled, rng)
    }

    fun create(roomId: Int, mode: GameMode, catIds: List<String>, seed: Long, cardCount: Int = 10): PairsRound {
        require(catIds.size == 5) { "Pairs requires exactly five room cats" }
        require(cardCount in CARD_COUNTS) { "Unsupported card count $cardCount" }
        val rngState = Xorshift32.normalize(seed)
        val catOffset = (rngState % catIds.size).toInt()
        val deck = (0 until cardCount / 2).flatMap { pairIndex ->
            val catId = catIds[(pairIndex + catOffset) % catIds.size]
            listOf(PairCard(pairIndex * 2, catId, false), PairCard(pairIndex * 2 + 1, catId, false))
        }
        val shuffled = shuffle(deck, rngState)
        return PairsRound(
            id = "$roomId-${modeKey(mode)}-$rngState",
            roomId = roomId,
            mode = mode,
            cardCount = cardCount,
            cards = shuffled.cards,
            revealed = emptyList(),
            attempts = 0,
            matches = 0,
            status = RoundStatus.PLAYING,
            lastMatch = null,
            rngState = shuffled.rngState,
        )
    }

    /** Turns a card face up. Returns the same instance when the tap is not allowed (board locked, card matched...). */
    fun reveal(round: PairsRound, cardId: Int): PairsRound {
        if (round.status != RoundStatus.PLAYING || round.revealed.size >= 2 || cardId in round.revealed) return round
        val card = round.cards.firstOrNull { it.id == cardId } ?: return round
        if (card.matched) return round
        if (round.revealed.isEmpty()) return round.copy(revealed = listOf(cardId), lastMatch = null)

        val first = round.cards.firstOrNull { it.id == round.revealed[0] }
            ?: return round.copy(revealed = listOf(cardId), lastMatch = null)
        val attempts = round.attempts + 1
        if (first.catId != card.catId) {
            return round.copy(revealed = listOf(first.id, card.id), attempts = attempts, lastMatch = false)
        }
        val matches = round.matches + 1
        return round.copy(
            cards = round.cards.map { if (it.id == first.id || it.id == card.id) it.copy(matched = true) else it },
            revealed = emptyList(),
            attempts = attempts,
            matches = matches,
            status = if (matches == round.cardCount / 2) RoundStatus.FINISHED else RoundStatus.PLAYING,
            lastMatch = true,
        )
    }

    /** Turns the two mismatched cards face down again. */
    fun hideMismatch(round: PairsRound): PairsRound =
        if (round.showingMismatch) round.copy(revealed = emptyList(), lastMatch = null) else round

    /** 10 fish-points per pair, plus a bonus for a finished round with few attempts. */
    fun baseReward(matches: Int, attempts: Int, totalPairs: Int, finished: Boolean): Int {
        val safeTotal = max(1, totalPairs)
        val safeMatches = max(0, min(safeTotal, matches))
        val pairReward = safeMatches * 10
        val efficiencyBonus = if (finished) max(0, safeTotal * 3 - max(safeTotal, attempts)) * 2 else 0
        return pairReward + efficiencyBonus
    }

    fun fishReward(round: PairsRound): Double = fishReward(
        round.matches, round.attempts, round.totalPairs, round.roomId, round.mode, round.status == RoundStatus.FINISHED,
    )

    fun fishReward(matches: Int, attempts: Int, totalPairs: Int, roomId: Int, mode: GameMode, finished: Boolean): Double =
        floor(baseReward(matches, attempts, totalPairs, finished) * Balance.roomEconomyScale(roomId) * (if (mode == GameMode.EXPERT) 1.5 else 1.0))
}
