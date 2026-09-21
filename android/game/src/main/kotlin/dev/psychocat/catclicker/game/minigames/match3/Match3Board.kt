package dev.psychocat.catclicker.game.minigames.match3

import dev.psychocat.catclicker.game.rng.Xorshift32
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.min

const val MATCH3_SIZE = 7
const val MATCH3_MOVES = 20

/** One cat tile. The id stays with the tile while it falls, which lets the UI animate it. */
data class Match3Tile(val id: Int, val catId: String)

/** A straight line of at least three equal tiles: their cell indices. */
class Match3Run(val cells: List<Int>) {
    val length: Int get() = cells.size
}

/** Board contents together with the generator state after producing them. */
class GeneratedBoard(val board: List<Match3Tile>, val rngState: Long, val nextTileId: Int)

class ResolvedBoard(
    val board: List<Match3Tile>,
    val rngState: Long,
    val nextTileId: Int,
    val gainedScore: Int,
    val cascades: Int,
    val shuffled: Boolean,
)

/** Port of src/game/match3/board.ts; the order of random draws is kept exactly so rounds match the web version. */
object Match3Board {
    private class Chosen(val value: String, val state: Long)

    private fun choose(values: List<String>, rngState: Long): Chosen {
        val step = Xorshift32.step(rngState)
        return Chosen(values[floor(step.value * values.size).toInt()], step.state)
    }

    private fun sameCat(a: Match3Tile?, b: Match3Tile?): Boolean = a != null && b != null && a.catId == b.catId

    fun areAdjacent(first: Int, second: Int): Boolean {
        val cells = MATCH3_SIZE * MATCH3_SIZE
        if (first < 0 || second < 0 || first >= cells || second >= cells) return false
        val rowDistance = abs(first / MATCH3_SIZE - second / MATCH3_SIZE)
        val columnDistance = abs(first % MATCH3_SIZE - second % MATCH3_SIZE)
        return rowDistance + columnDistance == 1
    }

    fun swap(board: List<Match3Tile>, first: Int, second: Int): List<Match3Tile> {
        val swapped = board.toMutableList()
        swapped[first] = board[second]
        swapped[second] = board[first]
        return swapped
    }

    fun findRuns(board: List<Match3Tile?>): List<Match3Run> {
        val runs = ArrayList<Match3Run>()
        for (row in 0 until MATCH3_SIZE) {
            var start = 0
            while (start < MATCH3_SIZE) {
                val first = row * MATCH3_SIZE + start
                if (board[first] == null) {
                    start += 1
                    continue
                }
                var end = start + 1
                while (end < MATCH3_SIZE && sameCat(board[first], board[row * MATCH3_SIZE + end])) end += 1
                if (end - start >= 3) runs.add(Match3Run((0 until end - start).map { first + it }))
                start = end
            }
        }
        for (column in 0 until MATCH3_SIZE) {
            var start = 0
            while (start < MATCH3_SIZE) {
                val first = start * MATCH3_SIZE + column
                if (board[first] == null) {
                    start += 1
                    continue
                }
                var end = start + 1
                while (end < MATCH3_SIZE && sameCat(board[first], board[end * MATCH3_SIZE + column])) end += 1
                if (end - start >= 3) runs.add(Match3Run((start until end).map { it * MATCH3_SIZE + column }))
                start = end
            }
        }
        return runs
    }

    fun swapCreatesMatch(board: List<Match3Tile>, first: Int, second: Int): Boolean =
        areAdjacent(first, second) && findRuns(swap(board, first, second)).isNotEmpty()

    /** The first swap (scanning right, then down, cell by cell) that makes a line; also used for the hint-free "is it playable" check. */
    fun findPossibleSwap(board: List<Match3Tile>): Pair<Int, Int>? {
        for (index in board.indices) {
            val column = index % MATCH3_SIZE
            if (column < MATCH3_SIZE - 1 && swapCreatesMatch(board, index, index + 1)) return index to index + 1
            if (index + MATCH3_SIZE < board.size && swapCreatesMatch(board, index, index + MATCH3_SIZE)) return index to index + MATCH3_SIZE
        }
        return null
    }

    private fun generateCandidate(catIds: List<String>, initialState: Long, firstTileId: Int): GeneratedBoard {
        val board = ArrayList<Match3Tile>()
        var rng = initialState
        var nextId = firstTileId
        for (index in 0 until MATCH3_SIZE * MATCH3_SIZE) {
            val row = index / MATCH3_SIZE
            val column = index % MATCH3_SIZE
            // Never start with a ready-made line: forbid a third equal cat next to two equal ones.
            val allowed = catIds.filter { catId ->
                val horizontal = column >= 2 && board[index - 1].catId == catId && board[index - 2].catId == catId
                val vertical = row >= 2 && board[index - MATCH3_SIZE].catId == catId && board[index - MATCH3_SIZE * 2].catId == catId
                !horizontal && !vertical
            }
            val selected = choose(allowed, rng)
            rng = selected.state
            board.add(Match3Tile(nextId, selected.value))
            nextId += 1
        }
        return GeneratedBoard(board, rng, nextId)
    }

    /** A fresh board with no lines and at least one possible move. */
    fun generateStable(catIds: List<String>, initialState: Long, firstTileId: Int = 1): GeneratedBoard {
        require(catIds.size >= 3) { "Match-3 needs at least three tile types" }
        var rng = initialState
        var nextId = firstTileId
        repeat(300) {
            val candidate = generateCandidate(catIds, rng, nextId)
            rng = candidate.rngState
            nextId = candidate.nextTileId
            if (findPossibleSwap(candidate.board) != null) return candidate
        }
        error("Could not create a playable match-3 board")
    }

    private fun collapseAndFill(board: List<Match3Tile?>, catIds: List<String>, initialState: Long, firstTileId: Int): GeneratedBoard {
        val collapsed = arrayOfNulls<Match3Tile>(MATCH3_SIZE * MATCH3_SIZE)
        var rng = initialState
        var nextId = firstTileId
        for (column in 0 until MATCH3_SIZE) {
            val remaining = ArrayList<Match3Tile>()
            for (row in MATCH3_SIZE - 1 downTo 0) {
                board[row * MATCH3_SIZE + column]?.let { remaining.add(it) }
            }
            var row = MATCH3_SIZE - 1
            for (tile in remaining) {
                collapsed[row * MATCH3_SIZE + column] = tile
                row -= 1
            }
            while (row >= 0) {
                val selected = choose(catIds, rng)
                rng = selected.state
                collapsed[row * MATCH3_SIZE + column] = Match3Tile(nextId, selected.value)
                nextId += 1
                row -= 1
            }
        }
        return GeneratedBoard(collapsed.map { it!! }, rng, nextId)
    }

    private fun shuffleStable(board: List<Match3Tile>, catIds: List<String>, initialState: Long, nextTileId: Int): GeneratedBoard {
        var rng = initialState
        repeat(300) {
            val shuffled = board.toMutableList()
            for (index in shuffled.size - 1 downTo 1) {
                val step = Xorshift32.step(rng)
                rng = step.state
                val target = floor(step.value * (index + 1)).toInt()
                val swap = shuffled[index]
                shuffled[index] = shuffled[target]
                shuffled[target] = swap
            }
            if (findRuns(shuffled).isEmpty() && findPossibleSwap(shuffled) != null) return GeneratedBoard(shuffled, rng, nextTileId)
        }
        return generateStable(catIds, rng, nextTileId)
    }

    // ---- scoring of one turn ----

    /** 3 in a row = 30, 4 = 60, 5 = 100, then +40 per extra tile. */
    fun runPoints(length: Int): Int = when {
        length < 3 -> 0
        length == 3 -> 30
        length == 4 -> 60
        else -> 100 + (length - 5) * 40
    }

    /** Each further step of a chain reaction is worth 1.5, 2, 2.5... times, at most 3 times. */
    fun cascadeMultiplier(depth: Int): Double = min(3.0, 1 + maxOf(0, depth - 1) * 0.5)

    fun scoreWave(runs: List<Match3Run>, depth: Int): Int =
        floor(runs.sumOf { runPoints(it.length) } * cascadeMultiplier(depth) + 0.5).toInt() // JS Math.round

    /**
     * Clears lines, lets the tiles above fall, fills the gaps with new tiles and repeats until nothing is left to
     * clear (at most 50 steps). If the result has no possible move the board is shuffled without ending the round.
     */
    fun resolve(swappedBoard: List<Match3Tile>, catIds: List<String>, initialState: Long, firstTileId: Int): ResolvedBoard {
        var board = swappedBoard
        var rng = initialState
        var nextId = firstTileId
        var gained = 0
        var cascades = 0
        for (depth in 1..50) {
            val runs = findRuns(board)
            if (runs.isEmpty()) break
            cascades = depth
            gained += scoreWave(runs, depth)
            val cleared = board.toMutableList<Match3Tile?>()
            for (cell in runs.flatMap { it.cells }.toSet()) cleared[cell] = null
            val filled = collapseAndFill(cleared, catIds, rng, nextId)
            board = filled.board
            rng = filled.rngState
            nextId = filled.nextTileId
        }
        if (findRuns(board).isNotEmpty()) {
            val regenerated = generateStable(catIds, rng, nextId)
            board = regenerated.board
            rng = regenerated.rngState
            nextId = regenerated.nextTileId
        }
        val needsShuffle = findPossibleSwap(board) == null
        if (needsShuffle) {
            val shuffled = shuffleStable(board, catIds, rng, nextId)
            board = shuffled.board
            rng = shuffled.rngState
            nextId = shuffled.nextTileId
        }
        return ResolvedBoard(board, rng, nextId, gained, cascades, needsShuffle)
    }
}
