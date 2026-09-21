package dev.psychocat.catclicker.game.rng

/**
 * The xorshift32 generator of the web version. States are unsigned 32-bit numbers kept in a [Long];
 * zero is not a valid state (the web code maps it to 1), so every state is in 1..2^32-1.
 * Keeping the exact algorithm means a round started from the same seed is identical in both versions.
 */
object Xorshift32 {
    private const val MASK = 0xFFFFFFFFL

    class Step(val value: Double, val state: Long)

    /** JS `seed >>> 0 || 1`: any integer becomes a valid state. */
    fun normalize(seed: Long): Long {
        val unsigned = seed and MASK
        return if (unsigned == 0L) 1L else unsigned
    }

    /** One step: a uniform value in [0, 1) and the next state. */
    fun step(state: Long): Step {
        var n = normalize(state).toInt()
        n = n xor (n shl 13)
        n = n xor (n ushr 17)
        n = n xor (n shl 5)
        val unsigned = n.toLong() and MASK
        return Step(unsigned / 4294967296.0, if (unsigned == 0L) 1L else unsigned)
    }
}
