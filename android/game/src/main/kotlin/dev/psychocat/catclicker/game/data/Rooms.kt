package dev.psychocat.catclicker.game.data

/**
 * Port of src/game/rooms.ts. Image fields are drawable resource names (see tools/android/convert_assets.py),
 * so this module stays free of Android dependencies.
 */
data class Room(
    val id: Int,
    val name: String,
    val catGroup: String,
    val dayImage: String,
    val nightImage: String,
    val match3Image: String,
)

object Rooms {
    private fun room(id: Int, name: String, catGroup: String) = Room(
        id = id,
        name = name,
        catGroup = catGroup,
        dayImage = "room_${id}_day",
        nightImage = "room_${id}_night",
        match3Image = "match3_room_$id",
    )

    val all: List<Room> = listOf(
        room(1, "Комната", "basic"),
        room(2, "Уютный приют", "rare"),
        room(3, "Котодом", "special"),
        room(4, "Котоцентр", "superhero"),
        room(5, "Штаб Котификации", "ai"),
    )

    val count: Int get() = all.size

    fun byId(id: Int): Room = all[id - 1]
}
