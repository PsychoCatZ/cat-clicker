package dev.psychocat.catclicker.game.format

import kotlin.math.floor
import kotlin.math.max

/**
 * Russian number formatting for the UI, implemented by hand so it behaves the same on the JVM and on Android
 * regardless of the device locale. Groups are separated by a non-breaking space, the decimal mark is a comma.
 */
object Numbers {
    private const val NBSP = ' '

    private fun group(value: Long): String {
        val digits = value.toString()
        val out = StringBuilder()
        digits.forEachIndexed { index, char ->
            if (index > 0 && (digits.length - index) % 3 == 0) out.append(NBSP)
            out.append(char)
        }
        return out.toString()
    }

    /** Divides by [unit], keeps one decimal without rounding up (never shows more than the player owns). */
    private fun compact(value: Double, unit: Double, suffix: String): String {
        val tenths = floor(value / unit * 10).toLong()
        val whole = tenths / 10
        val fraction = tenths % 10
        return if (fraction == 0L) "${group(whole)}$NBSP$suffix" else "${group(whole)},$fraction$NBSP$suffix"
    }

    /**
     * Fish and prices. Whole numbers up to 999 999 999 are written in full; bigger amounts become "1,5 млрд" or
     * "2,3 трлн" so they fit on large buttons. A fraction of a fish (0 < v < 1) shows two decimals.
     */
    fun format(value: Double): String {
        if (!value.isFinite() || value <= 0) return "0"
        if (value < 1) return "0," + (floor(value * 100).toLong()).toString().padStart(2, '0')
        return when {
            value >= 1e12 -> compact(value, 1e12, "трлн")
            value >= 1e9 -> compact(value, 1e9, "млрд")
            else -> group(floor(value).toLong())
        }
    }

    /** "5 мин", "1 ч 20 мин" — the time the player was away. */
    fun formatAway(seconds: Double): String {
        val minutes = max(1, floor(seconds / 60).toInt())
        if (minutes < 60) return "$minutes мин"
        val hours = minutes / 60
        val rest = minutes % 60
        return if (rest > 0) "$hours ч $rest мин" else "$hours ч"
    }

    /** "1 предмет ждёт места", "3 предмета ждут места", "5 предметов ждут места". */
    fun itemsWaiting(count: Int): String = when {
        count == 1 -> "1 предмет ждёт места"
        count in 2..4 -> "$count предмета ждут места"
        else -> "$count предметов ждут места"
    }
}
