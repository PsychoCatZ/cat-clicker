package dev.psychocat.catclicker.game.minigames.sliding

import dev.psychocat.catclicker.game.rng.Xorshift32
import kotlin.math.floor

/** Port of src/game/sliding/board.ts. A board is a list of size*size cells: tile ids 0..n-2 and one null (the gap). */
object SlidingBoard {
    fun solved(size: Int): List<Int?> = List(size * size - 1) { it } + listOf(null)

    fun isSolved(tiles: List<Int?>): Boolean =
        tiles.withIndex().all { (index, tile) -> if (index == tiles.size - 1) tile == null else tile == index }

    /** Neighbour cells in the web version's order (up, down, left, right); the order matters for shuffling. */
    fun neighborIndices(index: Int, size: Int): List<Int> {
        val row = index / size
        val column = index % size
        return listOf(
            if (row > 0) index - size else -1,
            if (row < size - 1) index + size else -1,
            if (column > 0) index - 1 else -1,
            if (column < size - 1) index + 1 else -1,
        ).filter { it >= 0 }
    }

    fun movableTileIds(tiles: List<Int?>, size: Int): List<Int> {
        val empty = tiles.indexOf(null)
        if (empty < 0) return emptyList()
        return neighborIndices(empty, size).mapNotNull { tiles[it] }
    }

    /** The board after sliding [tileId] into the gap, or null if that tile is not next to the gap. */
    fun move(tiles: List<Int?>, size: Int, tileId: Int): List<Int?>? {
        val empty = tiles.indexOf(null)
        val tileIndex = tiles.indexOf(tileId)
        if (empty < 0 || tileIndex < 0 || tileIndex !in neighborIndices(empty, size)) return null
        val next = tiles.toMutableList()
        next[empty] = tileId
        next[tileIndex] = null
        return next
    }

    /** A board is solvable when the inversion parity fits the usual 15-puzzle rule. */
    fun isSolvable(tiles: List<Int?>, size: Int): Boolean {
        if (tiles.size != size * size || tiles.count { it == null } != 1) return false
        val numbered = tiles.filterNotNull()
        if (numbered.size != size * size - 1 || numbered.toSet().size != numbered.size ||
            numbered.any { it < 0 || it >= size * size - 1 }
        ) {
            return false
        }
        var inversions = 0
        for (first in numbered.indices) {
            for (second in first + 1 until numbered.size) {
                if (numbered[first] > numbered[second]) inversions += 1
            }
        }
        if (size % 2 == 1) return inversions % 2 == 0
        val emptyRowFromBottom = size - tiles.indexOf(null) / size
        return (inversions + emptyRowFromBottom) % 2 == 1
    }

    class Shuffled(val tiles: List<Int?>, val rngState: Long, val history: List<Int>)

    /** Random legal moves from the solved board, so the result is always solvable. Undoing [Shuffled.history] solves it. */
    fun shuffle(size: Int, seed: Long, moveCount: Int): Shuffled {
        var tiles = solved(size)
        var rng = Xorshift32.normalize(seed)
        var previousEmpty = -1
        val history = ArrayList<Int>()
        repeat(moveCount) {
            val empty = tiles.indexOf(null)
            val neighbors = neighborIndices(empty, size)
            val choices = neighbors.filter { it != previousEmpty }
            val candidates = if (choices.isNotEmpty()) choices else neighbors
            val step = Xorshift32.step(rng)
            rng = step.state
            val tileIndex = candidates[floor(step.value * candidates.size).toInt()]
            val tileId = tiles[tileIndex] ?: return@repeat
            val next = move(tiles, size, tileId) ?: return@repeat
            previousEmpty = empty
            tiles = next
            history.add(tileId)
        }
        if (isSolved(tiles)) {
            val tileId = movableTileIds(tiles, size).first()
            tiles = move(tiles, size, tileId) ?: tiles
            history.add(tileId)
        }
        return Shuffled(tiles, rng, history)
    }
}
