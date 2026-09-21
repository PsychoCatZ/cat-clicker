package dev.psychocat.catclicker.game.save

import dev.psychocat.catclicker.game.data.Cats
import dev.psychocat.catclicker.game.data.FurniturePoint
import dev.psychocat.catclicker.game.data.FurniturePosition
import dev.psychocat.catclicker.game.data.Items
import dev.psychocat.catclicker.game.data.Rooms
import dev.psychocat.catclicker.game.data.Upgrades
import dev.psychocat.catclicker.game.engine.GameEngine
import dev.psychocat.catclicker.game.model.GameMode
import dev.psychocat.catclicker.game.model.GameState
import dev.psychocat.catclicker.game.model.RoomProgress
import kotlinx.serialization.json.Json
import kotlin.math.max
import kotlin.math.min

data class LoadedSave(val state: GameState, val savedAtMillis: Long)

/**
 * Converts [GameState] to and from JSON. Decoding never trusts the file: like `readCurrentSave` in the web
 * version's save.ts it drops unknown ids, clamps numbers and rebuilds anything that is inconsistent.
 * The transient offline report is never stored.
 */
object SaveCodec {
    const val SCHEMA_VERSION = 1

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        allowSpecialFloatingPointValues = true
    }

    fun encode(state: GameState, savedAtMillis: Long): String = json.encodeToString(SaveDto.serializer(), state.toDto(savedAtMillis))

    /** Returns null when [text] is not a usable save at all. */
    fun decode(text: String): LoadedSave? {
        val dto = try {
            json.decodeFromString(SaveDto.serializer(), text)
        } catch (e: IllegalArgumentException) {
            // SerializationException is an IllegalArgumentException; so are number-format problems.
            return null
        }
        if (dto.schemaVersion > SCHEMA_VERSION || dto.schemaVersion < 1) return null
        return LoadedSave(dto.toState(), max(0L, dto.savedAt))
    }

    private fun GameState.toDto(savedAt: Long) = SaveDto(
        savedAt = savedAt,
        mode = if (mode == GameMode.EXPERT) "expert" else "normal",
        currentRoom = currentRoom,
        unlockedRoom = unlockedRoom,
        finalDismissed = finalDismissed,
        rooms = rooms.map { room ->
            RoomDto(
                fish = room.fish,
                hunger = room.hunger,
                lightsOff = room.lightsOff,
                resourceLevels = room.resourceLevels,
                boughtUpgrades = room.boughtUpgrades,
                furniturePositions = room.furniturePositions.mapValues { (_, p) ->
                    PositionDto(p.desktop?.let { PointDto(it.x, it.y) }, p.mobile?.let { PointDto(it.x, it.y) })
                },
                unlockedCats = room.unlockedCats,
                selectedCat = room.selectedCat,
                caviarSeconds = room.caviarSeconds,
            )
        },
    )

    private fun SaveDto.toState(): GameState {
        val unlocked = unlockedRoom.coerceIn(1, Rooms.count)
        return GameState(
            mode = if (mode == "expert") GameMode.EXPERT else GameMode.NORMAL,
            currentRoom = currentRoom.coerceIn(1, unlocked),
            unlockedRoom = unlocked,
            rooms = Rooms.all.map { cleanRoom(rooms.getOrNull(it.id - 1), it.id) },
            offlineReport = null,
            finalDismissed = finalDismissed,
        )
    }

    private fun number(value: Double, fallback: Double = 0.0): Double = if (value.isFinite() && value >= 0) value else fallback

    private fun cleanPoint(point: PointDto?): FurniturePoint? =
        if (point != null && point.x.isFinite() && point.y.isFinite() && point.x in 0.0..100.0 && point.y in 0.0..100.0) {
            FurniturePoint(point.x, point.y)
        } else {
            null
        }

    private fun cleanRoom(dto: RoomDto?, roomId: Int): RoomProgress {
        val empty = RoomProgress.new(roomId)
        if (dto == null) return empty
        val validCats = Cats.forRoom(roomId).map { it.id }.toSet()
        val roomUpgrades = Upgrades.forRoom(roomId).map { it.id }.toSet()
        val bought = dto.boughtUpgrades.filter { it in roomUpgrades }.distinct()
        val positions = LinkedHashMap<String, FurniturePosition>()
        for (id in bought) {
            val raw = dto.furniturePositions[id] ?: continue
            val desktop = cleanPoint(raw.desktop)
            val mobile = cleanPoint(raw.mobile)
            if (desktop != null || mobile != null) positions[id] = FurniturePosition(desktop, mobile)
        }
        val cats = (listOf(Cats.firstForRoom(roomId).id) + dto.unlockedCats.filter { it in validCats }).distinct()
        return RoomProgress(
            fish = min(number(dto.fish), GameEngine.MAX_FISH),
            hunger = min(number(dto.hunger, 100.0), 100.0),
            lightsOff = dto.lightsOff,
            resourceLevels = Items.resources.associate { item -> item.id to (dto.resourceLevels[item.id] ?: 0).coerceIn(0, 1000) },
            boughtUpgrades = bought,
            furniturePositions = positions,
            unlockedCats = cats,
            selectedCat = if (dto.selectedCat in cats) dto.selectedCat else Cats.firstForRoom(roomId).id,
            caviarSeconds = min(number(dto.caviarSeconds), 60.0),
        )
    }
}
