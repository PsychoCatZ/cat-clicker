package dev.psychocat.catclicker.game.minigames.mahjong

import dev.psychocat.catclicker.game.Balance
import dev.psychocat.catclicker.game.minigames.MiniGameRound
import dev.psychocat.catclicker.game.minigames.RoundStatus
import dev.psychocat.catclicker.game.model.GameMode
import dev.psychocat.catclicker.game.rng.Xorshift32
import kotlin.math.floor
import kotlin.math.max

enum class MahjongEvent(val key: String) {
    SELECTED("selected"),
    MISMATCH("mismatch"),
    MATCH("match"),
    HINT("hint"),
    SHUFFLED("shuffled"),
    COMPLETED("completed"),
    ;

    companion object {
        fun fromKey(key: String?): MahjongEvent? = entries.firstOrNull { it.key == key }
    }
}

/** State of one "Cat mahjong" round. Port of `MahjongRound` in src/game/mahjong/types.ts. */
data class MahjongRound(
    val id: String,
    override val roomId: Int,
    override val mode: GameMode,
    val difficulty: MahjongDifficulty,
    val tiles: List<MahjongTile>,
    val selectedId: Int?,
    val hintedIds: List<Int>,
    val score: Int,
    val pairsFound: Int,
    val hintsUsed: Int,
    val shuffles: Int,
    val rngState: Long,
    override val status: RoundStatus,
    val lastEvent: MahjongEvent?,
) : MiniGameRound {
    val layout: MahjongLayout get() = MahjongLayouts.of(difficulty)
}

/** Port of src/game/mahjong/{reducer,scoring}.ts. */
object MahjongGame {
    const val PAIR_POINTS = 100
    const val CLEAR_BONUS = 500
    const val NO_HINT_BONUS = 200
    const val NO_SHUFFLE_BONUS = 200

    /** A clean round (no hints, no shuffles) earns the most; hints and shuffles are free but lower the bonus. */
    fun completionBonus(hintsUsed: Int, shuffles: Int): Int =
        CLEAR_BONUS + (if (hintsUsed == 0) NO_HINT_BONUS else 0) + (if (shuffles == 0) NO_SHUFFLE_BONUS else 0)

    fun fishReward(score: Int, roomId: Int, mode: GameMode): Double =
        floor(max(0, score) / 30.0 * Balance.roomEconomyScale(roomId) * (if (mode == GameMode.EXPERT) 1.5 else 1.0))

    /** Deals cats to the remaining tiles pair by pair along a guaranteed removal sequence, so the rest is always clearable. */
    private fun assignSolvableCats(tiles: List<MahjongTile>, catIds: List<String>, seed: Long): List<MahjongTile>? {
        val active = tiles.filter { !it.removed }
        val sequence = MahjongBoard.findRemovalSequence(active.map { it.slot }) ?: return null
        val assignments = HashMap<Int, String>()
        val offset = (seed % catIds.size).toInt()
        sequence.forEachIndexed { index, (first, second) ->
            val catId = catIds[(index + offset) % catIds.size]
            assignments[first] = catId
            assignments[second] = catId
        }
        return tiles.map { if (it.removed) it else it.copy(catId = assignments[it.id] ?: catIds[0]) }
    }

    fun create(roomId: Int, mode: GameMode, catIds: List<String>, difficulty: MahjongDifficulty, seed: Long): MahjongRound {
        require(catIds.size == 5) { "Mahjong requires exactly five room cats" }
        val layout = MahjongLayouts.of(difficulty)
        val rngState = Xorshift32.normalize(seed)
        val empty = layout.slots.map { MahjongTile(it, catIds[0], false) }
        val tiles = assignSolvableCats(empty, catIds, rngState) ?: error("Mahjong layout ${difficulty.key} is not removable in pairs")
        return MahjongRound(
            id = "$roomId-${if (mode == GameMode.EXPERT) "expert" else "normal"}-${difficulty.key}-$rngState",
            roomId = roomId,
            mode = mode,
            difficulty = difficulty,
            tiles = tiles,
            selectedId = null,
            hintedIds = emptyList(),
            score = 0,
            pairsFound = 0,
            hintsUsed = 0,
            shuffles = 0,
            rngState = rngState,
            status = RoundStatus.PLAYING,
            lastEvent = null,
        )
    }

    /** Taps a tile: select it, deselect it, remove a matching pair, or move the selection if the cats differ. */
    fun select(round: MahjongRound, tileId: Int): MahjongRound {
        if (round.status != RoundStatus.PLAYING || !MahjongBoard.isFree(round.tiles, tileId)) return round
        val tile = round.tiles.firstOrNull { it.id == tileId } ?: return round
        if (tile.removed) return round
        val selectedId = round.selectedId
            ?: return round.copy(selectedId = tileId, hintedIds = emptyList(), lastEvent = MahjongEvent.SELECTED)
        if (selectedId == tileId) return round.copy(selectedId = null, hintedIds = emptyList(), lastEvent = null)

        val selected = round.tiles.firstOrNull { it.id == selectedId }
        if (selected == null || selected.removed || !MahjongBoard.isFree(round.tiles, selected.id)) {
            return round.copy(selectedId = tileId, hintedIds = emptyList(), lastEvent = MahjongEvent.SELECTED)
        }
        if (selected.catId != tile.catId) {
            return round.copy(selectedId = tileId, hintedIds = emptyList(), lastEvent = MahjongEvent.MISMATCH)
        }

        val tiles = round.tiles.map { if (it.id == selected.id || it.id == tile.id) it.copy(removed = true) else it }
        val finished = tiles.all { it.removed }
        val score = round.score + PAIR_POINTS + (if (finished) completionBonus(round.hintsUsed, round.shuffles) else 0)
        return round.copy(
            tiles = tiles,
            selectedId = null,
            hintedIds = emptyList(),
            score = score,
            pairsFound = round.pairsFound + 1,
            status = if (finished) RoundStatus.FINISHED else RoundStatus.PLAYING,
            lastEvent = if (finished) MahjongEvent.COMPLETED else MahjongEvent.MATCH,
        )
    }

    /** Shows an available pair; free of charge, only the "no hints" bonus is lost. */
    fun hint(round: MahjongRound): MahjongRound {
        if (round.status != RoundStatus.PLAYING) return round
        val pair = MahjongBoard.findPairs(round.tiles).firstOrNull() ?: return round
        return round.copy(
            selectedId = null,
            hintedIds = listOf(pair.first, pair.second),
            hintsUsed = round.hintsUsed + 1,
            lastEvent = MahjongEvent.HINT,
        )
    }

    /** Only offered in a dead end (no pair available); re-deals the remaining cats so the rest is clearable again. */
    fun shuffle(round: MahjongRound, catIds: List<String>): MahjongRound {
        if (round.status != RoundStatus.PLAYING || MahjongBoard.findPairs(round.tiles).isNotEmpty()) return round
        val rngState = Xorshift32.step(round.rngState).state
        val tiles = assignSolvableCats(round.tiles, catIds, rngState) ?: return round
        return round.copy(
            tiles = tiles,
            selectedId = null,
            hintedIds = emptyList(),
            shuffles = round.shuffles + 1,
            rngState = rngState,
            lastEvent = MahjongEvent.SHUFFLED,
        )
    }
}
