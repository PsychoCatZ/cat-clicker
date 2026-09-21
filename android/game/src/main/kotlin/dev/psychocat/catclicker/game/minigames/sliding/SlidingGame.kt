package dev.psychocat.catclicker.game.minigames.sliding

import dev.psychocat.catclicker.game.Balance
import dev.psychocat.catclicker.game.minigames.MiniGameRound
import dev.psychocat.catclicker.game.minigames.RoundStatus
import dev.psychocat.catclicker.game.model.GameMode
import dev.psychocat.catclicker.game.rng.Xorshift32
import kotlin.math.floor
import kotlin.math.max

enum class SlidingDifficulty(val key: String, val title: String, val size: Int, val shuffleMoves: Int) {
    EASY("easy", "Лёгкий", 3, 72),
    NORMAL("normal", "Обычный", 4, 144),
    HARD("hard", "Сложный", 5, 240),
    ;

    companion object {
        fun fromKey(key: String?): SlidingDifficulty? = entries.firstOrNull { it.key == key }
    }
}

enum class SlidingEvent(val key: String) {
    MOVED("moved"),
    SHUFFLED("shuffled"),
    COMPLETED("completed"),
    ;

    companion object {
        fun fromKey(key: String?): SlidingEvent? = entries.firstOrNull { it.key == key }
    }
}

/** State of one "Cat sliding puzzle" round. Port of `SlidingRound` in src/game/sliding/types.ts. */
data class SlidingRound(
    val id: String,
    override val roomId: Int,
    override val mode: GameMode,
    val difficulty: SlidingDifficulty,
    val size: Int,
    val catId: String,
    val tiles: List<Int?>,
    val moves: Int,
    val score: Int,
    val rngState: Long,
    override val status: RoundStatus,
    val lastEvent: SlidingEvent?,
) : MiniGameRound

/** Port of src/game/sliding/{reducer,scoring}.ts. */
object SlidingGame {
    private class ScoreRule(val base: Int, val bonusMoves: Int, val bonusPerMove: Int)

    private fun rule(difficulty: SlidingDifficulty) = when (difficulty) {
        SlidingDifficulty.EASY -> ScoreRule(900, 80, 4)
        SlidingDifficulty.NORMAL -> ScoreRule(2000, 180, 5)
        SlidingDifficulty.HARD -> ScoreRule(3600, 360, 6)
    }

    /** A fixed base for finishing plus a small bonus for few moves; many moves never reduce the base. */
    fun completionScore(difficulty: SlidingDifficulty, moves: Int): Int {
        val r = rule(difficulty)
        return r.base + max(0, r.bonusMoves - max(0, moves)) * r.bonusPerMove
    }

    fun fishReward(score: Int, roomId: Int, mode: GameMode): Double =
        floor(max(0, score) / 30.0 * Balance.roomEconomyScale(roomId) * (if (mode == GameMode.EXPERT) 1.5 else 1.0))

    private fun modeKey(mode: GameMode) = if (mode == GameMode.EXPERT) "expert" else "normal"

    fun create(
        roomId: Int,
        mode: GameMode,
        catIds: List<String>,
        difficulty: SlidingDifficulty,
        seed: Long,
        requestedCatId: String? = null,
    ): SlidingRound {
        require(catIds.size == 5) { "Sliding puzzle requires exactly five room cats" }
        val initialSeed = Xorshift32.normalize(seed)
        val catId = if (requestedCatId != null && requestedCatId in catIds) requestedCatId else catIds[(initialSeed % catIds.size).toInt()]
        val shuffled = SlidingBoard.shuffle(difficulty.size, initialSeed, difficulty.shuffleMoves)
        return SlidingRound(
            id = "$roomId-${modeKey(mode)}-${difficulty.key}-$catId-$initialSeed",
            roomId = roomId,
            mode = mode,
            difficulty = difficulty,
            size = difficulty.size,
            catId = catId,
            tiles = shuffled.tiles,
            moves = 0,
            score = 0,
            rngState = shuffled.rngState,
            status = RoundStatus.PLAYING,
            lastEvent = SlidingEvent.SHUFFLED,
        )
    }

    /** Slides [tileId]; returns the same instance when the move is not allowed. */
    fun move(round: SlidingRound, tileId: Int): SlidingRound {
        if (round.status != RoundStatus.PLAYING) return round
        val tiles = SlidingBoard.move(round.tiles, round.size, tileId) ?: return round
        val moves = round.moves + 1
        val finished = SlidingBoard.isSolved(tiles)
        return round.copy(
            tiles = tiles,
            moves = moves,
            score = if (finished) completionScore(round.difficulty, moves) else 0,
            status = if (finished) RoundStatus.FINISHED else RoundStatus.PLAYING,
            lastEvent = if (finished) SlidingEvent.COMPLETED else SlidingEvent.MOVED,
        )
    }

    fun reshuffle(round: SlidingRound): SlidingRound {
        if (round.status != RoundStatus.PLAYING) return round
        val seed = Xorshift32.step(round.rngState).state
        val shuffled = SlidingBoard.shuffle(round.difficulty.size, seed, round.difficulty.shuffleMoves)
        return round.copy(
            id = "${round.roomId}-${modeKey(round.mode)}-${round.difficulty.key}-${round.catId}-$seed",
            tiles = shuffled.tiles,
            moves = 0,
            score = 0,
            rngState = shuffled.rngState,
            lastEvent = SlidingEvent.SHUFFLED,
        )
    }
}
