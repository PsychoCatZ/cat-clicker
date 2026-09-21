package dev.psychocat.catclicker.game.format

import kotlin.test.Test
import kotlin.test.assertEquals

class NumbersTest {
    private val nb = ' '

    @Test
    fun smallValues() {
        assertEquals("0", Numbers.format(0.0))
        assertEquals("0", Numbers.format(-5.0))
        assertEquals("0", Numbers.format(Double.NaN))
        assertEquals("0,25", Numbers.format(0.25))
        assertEquals("0,05", Numbers.format(0.05))
        assertEquals("1", Numbers.format(1.99))
        assertEquals("999", Numbers.format(999.9))
    }

    @Test
    fun groupsThousands() {
        assertEquals("1${nb}000", Numbers.format(1000.0))
        assertEquals("12${nb}345", Numbers.format(12345.6))
        assertEquals("999${nb}999${nb}999", Numbers.format(999_999_999.9))
    }

    @Test
    fun hugeValuesAreCompactAndNeverRoundedUp() {
        assertEquals("1${nb}млрд", Numbers.format(1e9))
        assertEquals("1,5${nb}млрд", Numbers.format(1.5e9))
        assertEquals("1,9${nb}млрд", Numbers.format(1.99e9))
        assertEquals("2${nb}трлн", Numbers.format(2e12))
        assertEquals("1${nb}000${nb}трлн", Numbers.format(1e15))
    }

    @Test
    fun awayTime() {
        assertEquals("1 мин", Numbers.formatAway(30.0))
        assertEquals("5 мин", Numbers.formatAway(300.0))
        assertEquals("1 ч", Numbers.formatAway(3600.0))
        assertEquals("2 ч 5 мин", Numbers.formatAway(7500.0))
    }
}
