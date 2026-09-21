package dev.psychocat.catclicker.game

import kotlin.test.Test
import kotlin.test.assertEquals

class BalanceTest {
    @Test
    fun roomScaleGrowsByOneAndAHalfPerRoom() {
        assertEquals(1.0, Balance.roomEconomyScale(1))
        assertEquals(1.5, Balance.roomEconomyScale(2))
        assertEquals(5.0625, Balance.roomEconomyScale(5))
    }

    @Test
    fun expertScaleMatchesWebVersion() {
        assertEquals(2.5, Balance.EXPERT_COST_SCALE)
    }
}
