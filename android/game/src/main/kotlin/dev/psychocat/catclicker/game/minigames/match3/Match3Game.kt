package dev.psychocat.catclicker.game.minigames.match3

import dev.psychocat.catclicker.game.Balance
import dev.psychocat.catclicker.game.minigames.MiniGameRound
import dev.psychocat.catclicker.game.minigames.RoundStatus
import dev.psychocat.catclicker.game.model.GameMode
import dev.psychocat.catclicker.game.rng.Xorshift32
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/** State of one "Cats in a row" (match-3) round on a 7x7 board with 20 moves. Port of `Match3Round`. */
data class Match3Round(
    val id: String,
    override val roomId: Int,
    override val mode: GameMode,
    val board: List<Match3Tile>,
    val movesLeft: Int,
    val score: Int,
    val maxCombo: Int,
    val rngState: Long,
    val nextTileId: Int,
    override val status: RoundStatus,
    val lastGain: Int,
    val lastCombo: Int,
    val shuffled: Boolean,
) : MiniGameRound

/** Result of trying a swap. When [accepted] is false the round is returned unchanged and no move is spent. */
class Match3Turn(val round: Match3Round, val accepted: Boolean)

/** Port of src/game/match3/{reducer,scoring}.ts. */
object Match3Game {
    /** Score to fish: a generous rate for the first 1000 points, then less, then less again. */
    fun baseFishReward(score: Int): Double {
        val safe = max(0, score).toDouble()
        return min(safe, 1000.0) / 20 + min(max(safe - 1000, 0.0), 1000.0) / 40 + max(safe - 2000, 0.0) / 100
    }

    fun fishReward(score: Int, roomId: Int, mode: GameMode): Double =
        floor(baseFishReward(score) * Balance.roomEconomyScale(roomId) * (if (mode == GameMode.EXPERT) 1.5 else 1.0))

    fun create(roomId: Int, mode: GameMode, catIds: List<String>, seed: Long): Match3Round {
        val rngState = Xorshift32.normalize(seed)
        val generated = Match3Board.generateStable(catIds, rngState)
        return Match3Round(
            id = "$roomId-${if (mode == GameMode.EXPERT) "expert" else "normal"}-$rngState",
            roomId = roomId,
            mode = mode,
            board = generated.board,
            movesLeft = MATCH3_MOVES,
            score = 0,
            maxCombo = 0,
            rngState = generated.rngState,
            nextTileId = generated.nextTileId,
            status = RoundStatus.PLAYING,
            lastGain = 0,
            lastCombo = 0,
            shuffled = false,
        )
    }

    fun play(round: Match3Round, first: Int, second: Int, catIds: List<String>): Match3Turn {
        if (round.status != RoundStatus.PLAYING || !Match3Board.areAdjacent(first, second) ||
            !Match3Board.swapCreatesMatch(round.board, first, second)
        ) {
            return Match3Turn(round, false)
        }
        val resolved = Match3Board.resolve(Match3Board.swap(round.board, first, second), catIds, round.rngState, round.nextTileId)
        val movesLeft = max(0, round.movesLeft - 1)
        return Match3Turn(
            round.copy(
                board = resolved.board,
                movesLeft = movesLeft,
                score = round.score + resolved.gainedScore,
                maxCombo = max(round.maxCombo, resolved.cascades),
                rngState = resolved.rngState,
                nextTileId = resolved.nextTileId,
                status = if (movesLeft == 0) RoundStatus.FINISHED else RoundStatus.PLAYING,
                lastGain = resolved.gainedScore,
                lastCombo = resolved.cascades,
                shuffled = resolved.shuffled,
            ),
            true,
        )
    }
}
