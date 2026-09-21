package dev.psychocat.catclicker.game.model

import dev.psychocat.catclicker.game.data.Cats
import dev.psychocat.catclicker.game.data.FurniturePosition
import dev.psychocat.catclicker.game.data.Items
import dev.psychocat.catclicker.game.data.Rooms

enum class GameMode { NORMAL, EXPERT }

/** Everything the player has earned in one room. Port of `RoomProgress` in src/game/economy.ts. */
data class RoomProgress(
    val fish: Double,
    val hunger: Double,
    val lightsOff: Boolean,
    val resourceLevels: Map<String, Int>,
    val boughtUpgrades: List<String>,
    val furniturePositions: Map<String, FurniturePosition>,
    val unlockedCats: List<String>,
    val selectedCat: String,
    val caviarSeconds: Double,
) {
    companion object {
        fun new(roomId: Int) = RoomProgress(
            fish = 0.0,
            hunger = 100.0,
            lightsOff = false,
            resourceLevels = Items.resources.associate { it.id to 0 },
            boughtUpgrades = emptyList(),
            furniturePositions = emptyMap(),
            unlockedCats = listOf(Cats.firstForRoom(roomId).id),
            selectedCat = Cats.firstForRoom(roomId).id,
            caviarSeconds = 0.0,
        )
    }
}

/** Summary shown after the player was away. */
data class OfflineReport(
    val elapsedSeconds: Double,
    val creditedSeconds: Double,
    val fishEarned: Double,
    val hungerSpent: Double,
)

data class GameState(
    val mode: GameMode,
    val currentRoom: Int,
    val unlockedRoom: Int,
    val rooms: List<RoomProgress>,
    val offlineReport: OfflineReport? = null,
    val finalDismissed: Boolean = false,
) {
    val progress: RoomProgress get() = rooms[currentRoom - 1]

    companion object {
        fun initial(mode: GameMode = GameMode.NORMAL) = GameState(
            mode = mode,
            currentRoom = 1,
            unlockedRoom = 1,
            rooms = Rooms.all.map { RoomProgress.new(it.id) },
        )
    }
}
