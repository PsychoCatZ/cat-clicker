package dev.psychocat.catclicker.game.data

import dev.psychocat.catclicker.game.Balance
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Compares the Kotlin data tables with data.json exported from the web game
 * (`node tools/android/export_golden.mjs`), so hand-typed values cannot drift.
 */
class DataParityTest {
    private val golden: JsonObject = Json.parseToJsonElement(
        javaClass.getResourceAsStream("/golden/data.json")!!.bufferedReader(Charsets.UTF_8).readText(),
    ).jsonObject

    private fun array(name: String): JsonArray = golden.getValue(name).jsonArray

    /** Mirrors the naming rules of tools/android/convert_assets.py. */
    private fun resourceName(webPath: String): String {
        val parts = webPath.removePrefix("/assets/").removeSuffix(".png").split('/')
        return when (parts[0]) {
            "cats" -> if (parts[2] == "sleep") "cat_${parts[1]}_${parts[3]}_sleep" else "cat_${parts[1]}_${parts[2]}"
            "backgrounds" -> parts[1].replace('-', '_')
            "upgrades" -> "upgrade_r${parts[1].split('-')[1]}_${parts[2]}_${parts[3]}"
            "resources" -> "resource_${parts[1]}"
            "food" -> "food_${parts[1]}"
            else -> error("Unmapped $webPath")
        }
    }

    @Test
    fun roomsMatch() {
        val rooms = array("rooms")
        assertEquals(rooms.size, Rooms.all.size)
        rooms.forEachIndexed { index, item ->
            val web = item.jsonObject
            val room = Rooms.all[index]
            assertEquals(web.getValue("id").jsonPrimitive.int, room.id)
            assertEquals(web.getValue("name").jsonPrimitive.content, room.name)
            assertEquals(web.getValue("catGroup").jsonPrimitive.content, room.catGroup)
            assertEquals(resourceName(web.getValue("day").jsonPrimitive.content), room.dayImage)
            assertEquals(resourceName(web.getValue("night").jsonPrimitive.content), room.nightImage)
            assertEquals(resourceName(web.getValue("match3Background").jsonPrimitive.content), room.match3Image)
        }
    }

    @Test
    fun catsMatch() {
        val cats = array("cats")
        assertEquals(25, Cats.all.size)
        assertEquals(cats.size, Cats.all.size)
        cats.forEachIndexed { index, item ->
            val web = item.jsonObject
            val cat = Cats.all[index]
            assertEquals(web.getValue("id").jsonPrimitive.content, cat.id)
            assertEquals(web.getValue("name").jsonPrimitive.content, cat.name)
            assertEquals(web.getValue("roomId").jsonPrimitive.int, cat.roomId)
            assertEquals(web.getValue("baseCost").jsonPrimitive.int, cat.baseCost)
            assertEquals(resourceName(web.getValue("image").jsonPrimitive.content), cat.image)
            assertEquals(resourceName(web.getValue("sleepingImage").jsonPrimitive.content), cat.sleepingImage)
        }
    }

    @Test
    fun upgradesMatch() {
        val upgrades = array("upgrades")
        assertEquals(50, Upgrades.all.size)
        assertEquals(upgrades.size, Upgrades.all.size)
        upgrades.forEachIndexed { index, item ->
            val web = item.jsonObject
            val upgrade = Upgrades.all[index]
            val label = upgrade.id
            assertEquals(web.getValue("id").jsonPrimitive.content, upgrade.id)
            assertEquals(web.getValue("roomId").jsonPrimitive.int, upgrade.roomId, label)
            assertEquals(web.getValue("tier").jsonPrimitive.content, upgrade.tier.key, label)
            assertEquals(web.getValue("slot").jsonPrimitive.int, upgrade.slot, label)
            assertEquals(web.getValue("name").jsonPrimitive.content, upgrade.name, label)
            assertEquals(resourceName(web.getValue("image").jsonPrimitive.content), upgrade.image, label)
            assertEquals(web.getValue("income").jsonPrimitive.int, upgrade.income, label)
            assertEquals(web.getValue("baseCost").jsonPrimitive.int, upgrade.baseCost, label)
            assertEquals(web.getValue("surface").jsonPrimitive.content, upgrade.surface.name.lowercase(), label)
            val p = web.getValue("placement").jsonObject
            val placement = upgrade.placement
            assertEquals(p.getValue("x").jsonPrimitive.double, placement.x, label)
            assertEquals(p.getValue("y").jsonPrimitive.double, placement.y, label)
            assertEquals(p.getValue("width").jsonPrimitive.double, placement.width, label)
            assertEquals(p.getValue("height").jsonPrimitive.double, placement.height, label)
            assertEquals(p["mobileX"]?.jsonPrimitive?.doubleOrNull, placement.mobileX, label)
            assertEquals(p["mobileY"]?.jsonPrimitive?.doubleOrNull, placement.mobileY, label)
            assertEquals(p["mobileHeight"]?.jsonPrimitive?.doubleOrNull, placement.mobileHeight, label)
        }
    }

    @Test
    fun resourcesAndFoodMatch() {
        val resources = array("resources")
        assertEquals(resources.size, Items.resources.size)
        resources.forEachIndexed { index, item ->
            val web = item.jsonObject
            val resource = Items.resources[index]
            assertEquals(web.getValue("id").jsonPrimitive.content, resource.id)
            assertEquals(web.getValue("name").jsonPrimitive.content, resource.name)
            assertEquals(resourceName(web.getValue("image").jsonPrimitive.content), resource.image)
            assertEquals(web.getValue("bonus").jsonPrimitive.int, resource.bonus)
            assertEquals(web.getValue("baseCost").jsonPrimitive.int, resource.baseCost)
            assertEquals(web.getValue("growth").jsonPrimitive.double, resource.growth)
        }
        val foods = array("foods")
        assertEquals(foods.size, Items.foods.size)
        foods.forEachIndexed { index, item ->
            val web = item.jsonObject
            val food = Items.foods[index]
            assertEquals(web.getValue("id").jsonPrimitive.content, food.id)
            assertEquals(web.getValue("name").jsonPrimitive.content, food.name)
            assertEquals(resourceName(web.getValue("image").jsonPrimitive.content), food.image)
            assertEquals(web.getValue("restore").jsonPrimitive.int, food.restore)
            assertEquals(web.getValue("baseCost").jsonPrimitive.int, food.baseCost)
            assertEquals(web["boostSeconds"]?.jsonPrimitive?.contentOrNull?.toInt(), food.boostSeconds)
        }
    }

    @Test
    fun balanceMatches() {
        val balance = golden.getValue("balance").jsonObject
        assertEquals(balance.getValue("expertCostScale").jsonPrimitive.double, Balance.EXPERT_COST_SCALE)
        balance.getValue("roomScales").jsonArray.forEachIndexed { index, value ->
            assertEquals(value.jsonPrimitive.double, Balance.roomEconomyScale(index + 1))
        }
    }

    @Test
    fun everyImageKeyHasAConvertedDrawable() {
        val drawables = File("../app/src/main/res/drawable-nodpi")
        assertTrue(drawables.isDirectory, "drawable-nodpi not found from ${File(".").absolutePath}")
        val keys = Rooms.all.flatMap { listOf(it.dayImage, it.nightImage, it.match3Image) } +
            Cats.all.flatMap { listOf(it.image, it.sleepingImage) } +
            Upgrades.all.map { it.image } +
            Items.resources.map { it.image } +
            Items.foods.map { it.image } +
            (1..5).map { "ui_0$it" } + listOf("ui_door", "ui_final")
        val missing = keys.filterNot { File(drawables, "$it.webp").isFile }
        assertTrue(missing.isEmpty(), "Missing drawables: $missing")
    }
}
