package dev.psychocat.catclicker.game.data

/** Port of src/game/cats.ts. */
data class Cat(
    val id: String,
    val name: String,
    val roomId: Int,
    val image: String,
    val sleepingImage: String,
    val baseCost: Int,
)

object Cats {
    private val names = listOf(
        listOf("Рыжик", "Уголёк", "Полоска", "Снежка", "Ирис"),
        listOf("Калико", "Тучка", "Пингвин", "Персик", "Луна"),
        listOf("Гроза", "Пончик", "Искра", "Облачко", "Ночь"),
        listOf("Тень", "Паутинка", "Броня", "Щит", "Плащ"),
        listOf("Дип", "Кью", "Джем", "Клод", "Чатти"),
    )

    private val costs = listOf(0, 150, 1800, 18000, 180000)

    val all: List<Cat> = Rooms.all.flatMap { room ->
        names[room.id - 1].mapIndexed { index, name ->
            val number = (index + 1).toString().padStart(2, '0')
            Cat(
                id = "${room.catGroup}-$number",
                name = name,
                roomId = room.id,
                image = "cat_${room.catGroup}_$number",
                sleepingImage = "cat_${room.catGroup}_${number}_sleep",
                baseCost = costs[index],
            )
        }
    }

    private val byRoom: Map<Int, List<Cat>> = all.groupBy { it.roomId }
    private val byId: Map<String, Cat> = all.associateBy { it.id }

    fun forRoom(roomId: Int): List<Cat> = byRoom.getValue(roomId)
    fun firstForRoom(roomId: Int): Cat = forRoom(roomId).first()
    fun find(id: String): Cat? = byId[id]
}
