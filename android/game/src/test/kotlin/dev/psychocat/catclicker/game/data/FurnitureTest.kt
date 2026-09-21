package dev.psychocat.catclicker.game.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Kotlin counterparts of the furniture assertions in tools/check_game.mjs. */
class FurnitureTest {
    private val basicBowl = Upgrades.find(1, "room-1-basic-1")!!
    private val wallWindow = Upgrades.find(1, "room-1-advanced-2")!!

    @Test
    fun advancedItemReplacesBasicInTheSameSlot() {
        val visible = Furniture.visible(1, listOf("room-1-basic-1", "room-1-basic-2", "room-1-advanced-2"))
        assertEquals(listOf("room-1-basic-1", "room-1-advanced-2"), visible.map { it.id })
    }

    @Test
    fun floorItemStaysOnTheFloor() {
        val point = Furniture.constrainPoint(basicBowl, SceneLayout.DESKTOP, FurniturePoint(50.0, 10.0))!!
        assertTrue(point.y >= 57.0)
    }

    @Test
    fun mobileItemStaysInsideTheScene() {
        val point = Furniture.constrainPoint(basicBowl, SceneLayout.MOBILE, FurniturePoint(90.0, 90.0))!!
        assertTrue(point.x < 90.0)
    }

    @Test
    fun wallItemStaysOnTheWall() {
        val point = Furniture.constrainPoint(wallWindow, SceneLayout.DESKTOP, FurniturePoint(30.0, 95.0))!!
        assertTrue(point.y < 55.0)
    }

    @Test
    fun nonFinitePointIsRejected() {
        assertNull(Furniture.constrainPoint(basicBowl, SceneLayout.DESKTOP, FurniturePoint(Double.NaN, 60.0)))
    }

    @Test
    fun missingLayoutFallsBackToTheOtherOne() {
        val onlyDesktop = FurniturePosition(desktop = FurniturePoint(20.0, 70.0))
        assertEquals(FurniturePoint(20.0, 70.0), Furniture.positionForLayout(onlyDesktop, SceneLayout.MOBILE))
        assertNull(Furniture.positionForLayout(null, SceneLayout.MOBILE))
    }

    @Test
    fun defaultPositionClampsMobileX() {
        val position = Furniture.defaultPosition(Upgrades.find(1, "room-1-advanced-3")!!)
        assertNotNull(position.mobile)
        assertTrue(position.mobile!!.x in 19.0..81.0)
    }
}
