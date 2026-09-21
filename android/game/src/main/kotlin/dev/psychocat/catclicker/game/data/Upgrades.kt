package dev.psychocat.catclicker.game.data

/** Port of src/game/upgrades.ts. Placement values are percentages of the room scene. */
enum class UpgradeTier(val key: String) { BASIC("basic"), ADVANCED("advanced") }
enum class FurnitureSurface { FLOOR, WALL }

data class ScenePlacement(
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double,
    val mobileX: Double? = null,
    val mobileY: Double? = null,
    val mobileHeight: Double? = null,
)

data class Upgrade(
    val id: String,
    val roomId: Int,
    val tier: UpgradeTier,
    val slot: Int,
    val name: String,
    val image: String,
    val income: Int,
    val baseCost: Int,
    val placement: ScenePlacement,
    val surface: FurnitureSurface,
)

object Upgrades {
    private class TierNames(val basic: List<String>, val advanced: List<String>)

    private val names = listOf(
        TierNames(
            listOf("Миска с рыбой", "Мягкая лежанка", "Когтеточка", "Игрушечная мышь", "Клубок"),
            listOf("Домик", "Окно с птицами", "Автокормушка", "Фонтанчик", "Игровой комплекс"),
        ),
        TierNames(
            listOf("Туннель", "Настенные полки", "Кошачья трава", "Гамак", "Щётка"),
            listOf("Лазерная игрушка", "Робомышь", "Аквариум", "Массажная арка", "Подвесной мост"),
        ),
        TierNames(
            listOf("Тёплая лежанка", "Замок", "Головоломка", "Когтеточка-дерево", "Кошачий телевизор"),
            listOf("Голограмма рыбок", "Игровая стена", "Капсула сна", "Умная кормушка", "Умный домик"),
        ),
        TierNames(
            listOf("Проектор", "Шкаф костюмов", "Тренажёр", "Сундук героя", "Зал славы"),
            listOf("Карта города", "Энергостанция", "Медкапсула", "Портал", "Витрина наград"),
        ),
        TierNames(
            listOf("Капсула уюта", "Голографическая когтеточка", "Автокормушка", "Пульт управления", "Сервер"),
            listOf("Станция спасения", "Аналитический стол", "Умное кресло", "Капсула будущего", "Центр Котификации"),
        ),
    )

    private val basicCosts = listOf(45, 180, 700, 2600, 9000)
    private val advancedCosts = listOf(18000, 42000, 95000, 220000, 500000)
    private val basicIncome = listOf(1, 2, 5, 12, 30)
    private val advancedIncome = listOf(50, 90, 160, 280, 480)

    /** Slot indices of fixtures attached to the wall rather than placed on the floor. */
    private class WallSlots(val basic: List<Int>, val advanced: List<Int>)

    private val wallSlots = listOf(
        WallSlots(emptyList(), listOf(1)),
        WallSlots(listOf(1, 3), listOf(2, 4)),
        WallSlots(listOf(4), listOf(1)),
        WallSlots(listOf(4), listOf(4)),
        WallSlots(emptyList(), emptyList()),
    )

    private fun place(
        x: Int, y: Int, width: Int, height: Int,
        mobileX: Int? = null, mobileY: Int? = null, mobileHeight: Int? = null,
    ) = ScenePlacement(
        x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble(),
        mobileX?.toDouble(), mobileY?.toDouble(), mobileHeight?.toDouble(),
    )

    private class TierPlacements(val basic: List<ScenePlacement>, val advanced: List<ScenePlacement>)

    private val placements = listOf(
        TierPlacements(
            basic = listOf(
                place(17, 83, 17, 22), place(78, 82, 23, 25), place(72, 63, 18, 41),
                place(28, 70, 15, 16), place(84, 69, 15, 19),
            ),
            advanced = listOf(
                place(17, 76, 25, 39, 19, 79), place(27, 38, 25, 37, 24, 32), place(85, 83, 19, 30, 81, 89, 21),
                place(68, 82, 19, 25, 81, 69, 22), place(76, 54, 27, 39, 80, 44),
            ),
        ),
        TierPlacements(
            basic = listOf(
                place(18, 82, 26, 27), place(73, 39, 27, 37, 76, 31), place(82, 80, 19, 25),
                place(27, 47, 26, 27, 24, 36), place(77, 66, 19, 29),
            ),
            advanced = listOf(
                place(20, 71, 19, 30), place(30, 83, 17, 18), place(72, 39, 27, 35, 77, 31),
                place(79, 73, 22, 35), place(29, 33, 29, 29, 27, 26),
            ),
        ),
        TierPlacements(
            basic = listOf(
                place(17, 84, 22, 25), place(77, 68, 28, 47), place(82, 84, 20, 21),
                place(29, 62, 20, 40), place(27, 38, 25, 35, 26, 31),
            ),
            advanced = listOf(
                place(24, 48, 20, 34, 23, 39), place(73, 38, 28, 42, 76, 31), place(17, 82, 24, 35),
                place(83, 84, 20, 27), place(72, 66, 26, 42),
            ),
        ),
        TierPlacements(
            basic = listOf(
                place(31, 56, 18, 38, 24, 49), place(18, 67, 23, 46), place(79, 67, 23, 43),
                place(22, 86, 21, 23), place(73, 36, 25, 35, 76, 30),
            ),
            advanced = listOf(
                place(26, 59, 27, 34), place(80, 54, 26, 42, 80, 45), place(18, 81, 25, 39),
                place(78, 82, 25, 39), place(70, 32, 26, 38, 75, 27),
            ),
        ),
        TierPlacements(
            basic = listOf(
                place(18, 82, 24, 35), place(29, 52, 20, 39, 25, 44), place(83, 83, 20, 33),
                place(72, 43, 26, 34, 76, 34), place(79, 69, 23, 42),
            ),
            advanced = listOf(
                place(18, 76, 26, 44), place(27, 45, 28, 37, 25, 36), place(79, 82, 22, 34),
                place(79, 57, 24, 42, 79, 49), place(69, 34, 28, 40, 75, 27),
            ),
        ),
    )

    /** The web version's second room lists its basic sprites in a different order. */
    private fun assetNumber(roomIndex: Int, tier: UpgradeTier, index: Int): Int =
        if (roomIndex == 1 && tier == UpgradeTier.BASIC) listOf(1, 2, 4, 3, 5)[index] else index + 1

    val all: List<Upgrade> = names.indices.flatMap { roomIndex ->
        UpgradeTier.entries.flatMap { tier ->
            val basic = tier == UpgradeTier.BASIC
            val tierNames = if (basic) names[roomIndex].basic else names[roomIndex].advanced
            val walls = if (basic) wallSlots[roomIndex].basic else wallSlots[roomIndex].advanced
            tierNames.mapIndexed { index, name ->
                Upgrade(
                    id = "room-${roomIndex + 1}-${tier.key}-${index + 1}",
                    roomId = roomIndex + 1,
                    tier = tier,
                    slot = index,
                    name = name,
                    image = "upgrade_r${roomIndex + 1}_${tier.key}_" +
                        assetNumber(roomIndex, tier, index).toString().padStart(2, '0'),
                    income = (if (basic) basicIncome else advancedIncome)[index],
                    baseCost = (if (basic) basicCosts else advancedCosts)[index],
                    placement = (if (basic) placements[roomIndex].basic else placements[roomIndex].advanced)[index],
                    surface = if (index in walls) FurnitureSurface.WALL else FurnitureSurface.FLOOR,
                )
            }
        }
    }

    private val byRoom: Map<Int, List<Upgrade>> = all.groupBy { it.roomId }

    fun forRoom(roomId: Int): List<Upgrade> = byRoom.getValue(roomId)
    fun find(roomId: Int, id: String): Upgrade? = forRoom(roomId).firstOrNull { it.id == id }
}
