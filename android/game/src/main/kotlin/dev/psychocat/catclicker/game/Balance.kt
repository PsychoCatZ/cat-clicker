package dev.psychocat.catclicker.game

/** Port of src/game/balance.ts. */
object Balance {
    const val EXPERT_COST_SCALE = 2.5

    // StrictMath.pow is fdlibm, the same algorithm V8 uses for `**`, so prices match the web version bit for bit.
    fun roomEconomyScale(roomId: Int): Double = StrictMath.pow(1.5, (roomId - 1).toDouble())
}
