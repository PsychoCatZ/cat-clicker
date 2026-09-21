package dev.psychocat.catclicker.game.data

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/** Port of src/game/furniture.ts. Portrait phones use [SceneLayout.MOBILE], landscape and tablets [SceneLayout.DESKTOP]. */
enum class SceneLayout(val key: String) { DESKTOP("desktop"), MOBILE("mobile") }

data class FurniturePoint(val x: Double, val y: Double)

/** Positions of one item in each layout; a missing layout means "not placed yet" for that layout. */
data class FurniturePosition(val desktop: FurniturePoint? = null, val mobile: FurniturePoint? = null) {
    operator fun get(layout: SceneLayout): FurniturePoint? = if (layout == SceneLayout.DESKTOP) desktop else mobile

    fun with(layout: SceneLayout, point: FurniturePoint): FurniturePosition =
        if (layout == SceneLayout.DESKTOP) copy(desktop = point) else copy(mobile = point)
}

object Furniture {
    /** One item per slot: the advanced version replaces the basic one once bought. */
    fun visible(roomId: Int, boughtUpgrades: Collection<String>): List<Upgrade> {
        val owned = boughtUpgrades.toSet()
        val items = Upgrades.forRoom(roomId)
        return (0 until 5).mapNotNull { slot ->
            items.firstOrNull { it.slot == slot && it.tier == UpgradeTier.ADVANCED && it.id in owned }
                ?: items.firstOrNull { it.slot == slot && it.tier == UpgradeTier.BASIC && it.id in owned }
        }
    }

    fun defaultPosition(upgrade: Upgrade): FurniturePosition {
        val p = upgrade.placement
        return FurniturePosition(
            desktop = FurniturePoint(p.x, p.y),
            mobile = FurniturePoint(max(19.0, min(81.0, p.mobileX ?: p.x)), p.mobileY ?: p.y),
        )
    }

    fun positionForLayout(position: FurniturePosition?, layout: SceneLayout): FurniturePoint? {
        val other = if (layout == SceneLayout.DESKTOP) SceneLayout.MOBILE else SceneLayout.DESKTOP
        return position?.get(layout) ?: position?.get(other)
    }

    fun constrainPoint(upgrade: Upgrade, layout: SceneLayout, point: FurniturePoint): FurniturePoint? {
        if (!point.x.isFinite() || !point.y.isFinite()) return null
        val p = upgrade.placement
        val mobile = layout == SceneLayout.MOBILE
        val width = if (mobile) min(35.0, p.width * 1.4) else p.width
        val height = if (mobile) p.mobileHeight ?: p.height else p.height
        val edge = width / 2 + 2
        val wall = upgrade.surface == FurnitureSurface.WALL
        val minimumY = if (wall) max(17.0, height / 2 + 4) else 57.0
        val maximumY = if (wall) min(49.0, 55 - height / 2) else 97 - height / 2
        // JS Math.round: ties go up, same as floor(v + 0.5).
        fun clamp(value: Double, low: Double, high: Double): Double =
            floor(max(low, min(high, value)) * 10 + 0.5) / 10
        return FurniturePoint(clamp(point.x, edge, 100 - edge), clamp(point.y, minimumY, maximumY))
    }
}
