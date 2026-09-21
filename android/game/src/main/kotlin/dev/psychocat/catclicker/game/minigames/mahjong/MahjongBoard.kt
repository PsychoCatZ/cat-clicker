package dev.psychocat.catclicker.game.minigames.mahjong

/**
 * A place for a tile. Coordinates are in half-tile units: a tile is 2 wide and 2 tall, [z] is the layer
 * (a higher layer lies on top of the lower one).
 */
data class MahjongSlot(val id: Int, val x: Int, val y: Int, val z: Int)

data class MahjongTile(val slot: MahjongSlot, val catId: String, val removed: Boolean) {
    val id: Int get() = slot.id
}

enum class MahjongDifficulty(val key: String, val title: String) {
    EASY("easy", "Лёгкий"),
    NORMAL("normal", "Обычный"),
    HARD("hard", "Сложный"),
    ;

    companion object {
        fun fromKey(key: String?): MahjongDifficulty? = entries.firstOrNull { it.key == key }
    }
}

class MahjongLayout(val difficulty: MahjongDifficulty, val width: Int, val height: Int, val slots: List<MahjongSlot>) {
    val tileCount: Int get() = slots.size
}

/** Port of src/game/mahjong/layouts.ts. The three layouts are fixed, so a round can always be cleared. */
object MahjongLayouts {
    private fun grid(cols: Int, rows: Int, startX: Int, startY: Int, z: Int, firstId: Int): List<MahjongSlot> =
        List(cols * rows) { index -> MahjongSlot(firstId + index, startX + (index % cols) * 2, startY + (index / cols) * 2, z) }

    private val easy = MahjongLayout(
        MahjongDifficulty.EASY, 8, 8,
        grid(4, 4, 0, 0, 0, 0) + grid(4, 2, 0, 2, 1, 16),
    )
    private val normal = MahjongLayout(
        MahjongDifficulty.NORMAL, 10, 8,
        grid(5, 4, 0, 0, 0, 0) + grid(4, 4, 1, 0, 1, 20) + grid(2, 2, 3, 2, 2, 36),
    )
    private val hard = MahjongLayout(
        MahjongDifficulty.HARD, 12, 10,
        grid(6, 5, 0, 0, 0, 0) + grid(5, 4, 1, 1, 1, 30) + grid(3, 2, 3, 3, 2, 50),
    )

    fun of(difficulty: MahjongDifficulty): MahjongLayout = when (difficulty) {
        MahjongDifficulty.EASY -> easy
        MahjongDifficulty.NORMAL -> normal
        MahjongDifficulty.HARD -> hard
    }
}

/** Port of src/game/mahjong/board.ts: which tiles are free, which pairs exist, and a guaranteed way to clear a layout. */
object MahjongBoard {
    private fun overlaps(a: MahjongSlot, b: MahjongSlot): Boolean =
        a.x < b.x + 2 && a.x + 2 > b.x && a.y < b.y + 2 && a.y + 2 > b.y

    private fun verticallyOverlaps(a: MahjongSlot, b: MahjongSlot): Boolean = a.y < b.y + 2 && a.y + 2 > b.y

    /**
     * A tile is free when nothing lies on top of it and at least one of its sides (left or right, on the same layer)
     * is open, exactly like in classic mahjong solitaire.
     */
    fun isFree(tiles: List<MahjongTile>, tileId: Int): Boolean {
        val tile = tiles.firstOrNull { it.id == tileId } ?: return false
        if (tile.removed) return false
        val active = tiles.filter { !it.removed }
        val covered = active.any { it.id != tile.id && it.slot.z > tile.slot.z && overlaps(tile.slot, it.slot) }
        if (covered) return false
        val leftBlocked = active.any {
            it.id != tile.id && it.slot.z == tile.slot.z && it.slot.x + 2 == tile.slot.x && verticallyOverlaps(tile.slot, it.slot)
        }
        val rightBlocked = active.any {
            it.id != tile.id && it.slot.z == tile.slot.z && tile.slot.x + 2 == it.slot.x && verticallyOverlaps(tile.slot, it.slot)
        }
        return !leftBlocked || !rightBlocked
    }

    fun freeTiles(tiles: List<MahjongTile>): List<MahjongTile> = tiles.filter { !it.removed && isFree(tiles, it.id) }

    /** All pairs of equal free tiles, in board order. The first one is what the hint shows. */
    fun findPairs(tiles: List<MahjongTile>): List<Pair<Int, Int>> {
        val free = freeTiles(tiles)
        val pairs = ArrayList<Pair<Int, Int>>()
        for (first in free.indices) {
            for (second in first + 1 until free.size) {
                if (free[first].catId == free[second].catId) pairs.add(free[first].id to free[second].id)
            }
        }
        return pairs
    }

    /**
     * Removes pairs of free tiles (top layer first) until the layout is empty; null if that is impossible.
     * The cats are then dealt along this sequence, which is why every round can be cleared.
     */
    fun findRemovalSequence(slots: List<MahjongSlot>): List<Pair<Int, Int>>? {
        val working = slots.map { MahjongTile(it, "", false) }.toMutableList()
        val sequence = ArrayList<Pair<Int, Int>>()
        while (working.any { !it.removed }) {
            val free = freeTiles(working).sortedWith(
                compareByDescending<MahjongTile> { it.slot.z }.thenBy { it.slot.y }.thenBy { it.slot.x },
            )
            if (free.size < 2) return null
            val first = free.first()
            val second = free.last()
            for (id in listOf(first.id, second.id)) {
                val index = working.indexOfFirst { it.id == id }
                working[index] = working[index].copy(removed = true)
            }
            sequence.add(first.id to second.id)
        }
        return sequence
    }
}
