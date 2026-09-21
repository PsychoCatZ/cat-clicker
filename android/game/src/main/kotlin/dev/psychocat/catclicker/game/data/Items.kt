package dev.psychocat.catclicker.game.data

/** Port of src/game/items.ts: click-income resources and food. */
data class ClickResource(
    val id: String,
    val name: String,
    val image: String,
    val bonus: Int,
    val baseCost: Int,
    val growth: Double,
)

data class Food(
    val id: String,
    val name: String,
    val image: String,
    val restore: Int,
    val baseCost: Int,
    val boostSeconds: Int? = null,
)

object Items {
    val resources: List<ClickResource> = listOf(
        ClickResource("fish", "Рыбка", "resource_01", bonus = 1, baseCost = 15, growth = 1.85),
        ClickResource("school", "Стайка рыб", "resource_02", bonus = 4, baseCost = 500, growth = 2.0),
        ClickResource("golden", "Золотая рыбка", "resource_03", bonus = 12, baseCost = 6000, growth = 2.1),
        ClickResource("coin", "Лапомонета", "resource_04", bonus = 35, baseCost = 60000, growth = 2.2),
        ClickResource("gift", "Подарок", "resource_05", bonus = 100, baseCost = 250000, growth = 2.3),
    )

    val foods: List<Food> = listOf(
        Food("mouse", "Мышка", "food_01", restore = 25, baseCost = 60),
        Food("dry", "Сухой корм", "food_02", restore = 50, baseCost = 160),
        Food("wet", "Влажный корм", "food_03", restore = 100, baseCost = 360),
        Food("caviar", "Икра", "food_04", restore = 100, baseCost = 750, boostSeconds = 60),
    )

    fun resource(id: String): ClickResource? = resources.firstOrNull { it.id == id }
    fun food(id: String): Food? = foods.firstOrNull { it.id == id }
}
