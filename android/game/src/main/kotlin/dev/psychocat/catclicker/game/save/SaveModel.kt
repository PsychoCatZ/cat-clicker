package dev.psychocat.catclicker.game.save

import kotlinx.serialization.Serializable

/**
 * On-disk format. Every field has a default so that older or partially damaged files still decode;
 * [SaveCodec] then repairs the values. New fields must always get a default (that is the migration story).
 */
@Serializable
internal data class SaveDto(
    val schemaVersion: Int = SaveCodec.SCHEMA_VERSION,
    val savedAt: Long = 0,
    val mode: String = "normal",
    val currentRoom: Int = 1,
    val unlockedRoom: Int = 1,
    val finalDismissed: Boolean = false,
    val rooms: List<RoomDto> = emptyList(),
    val activeGame: ActiveGameDto? = null,
)

@Serializable
internal data class RoomDto(
    val fish: Double = 0.0,
    val hunger: Double = 100.0,
    val lightsOff: Boolean = false,
    val resourceLevels: Map<String, Int> = emptyMap(),
    val boughtUpgrades: List<String> = emptyList(),
    val furniturePositions: Map<String, PositionDto> = emptyMap(),
    val unlockedCats: List<String> = emptyList(),
    val selectedCat: String = "",
    val caviarSeconds: Double = 0.0,
)

@Serializable
internal data class PointDto(val x: Double, val y: Double)

@Serializable
internal data class PositionDto(val desktop: PointDto? = null, val mobile: PointDto? = null)

/** The active mini-game round. One optional field per game; a new game only adds a field (with a default). */
@Serializable
internal data class ActiveGameDto(val sliding: SlidingRoundDto? = null, val pairs: PairsRoundDto? = null, val match3: Match3RoundDto? = null, val mahjong: MahjongRoundDto? = null)

@Serializable
internal data class SlidingRoundDto(
    val id: String = "",
    val roomId: Int = 0,
    val mode: String = "",
    val difficulty: String = "",
    val catId: String = "",
    val tiles: List<Int?> = emptyList(),
    val moves: Int = 0,
    val rngState: Long = 1,
    val lastEvent: String? = null,
)

@Serializable
internal data class PairCardDto(val id: Int = -1, val catId: String = "", val matched: Boolean = false)

@Serializable
internal data class PairsRoundDto(
    val id: String = "",
    val roomId: Int = 0,
    val mode: String = "",
    val cardCount: Int = 0,
    val cards: List<PairCardDto> = emptyList(),
    val revealed: List<Int> = emptyList(),
    val attempts: Int = 0,
    val rngState: Long = 1,
    val lastMatch: Boolean? = null,
)

@Serializable
internal data class Match3TileDto(val id: Int = -1, val catId: String = "")

@Serializable
internal data class Match3RoundDto(
    val id: String = "",
    val roomId: Int = 0,
    val mode: String = "",
    val board: List<Match3TileDto> = emptyList(),
    val movesLeft: Int = MATCH3_MOVES_DEFAULT,
    val score: Int = 0,
    val maxCombo: Int = 0,
    val rngState: Long = 1,
    val nextTileId: Int = 0,
    val lastGain: Int = 0,
    val lastCombo: Int = 0,
    val shuffled: Boolean = false,
)

internal const val MATCH3_MOVES_DEFAULT = 20

@Serializable
internal data class MahjongTileDto(val id: Int = -1, val catId: String = "", val removed: Boolean = false)

@Serializable
internal data class MahjongRoundDto(
    val id: String = "",
    val roomId: Int = 0,
    val mode: String = "",
    val difficulty: String = "",
    val tiles: List<MahjongTileDto> = emptyList(),
    val selectedId: Int? = null,
    val hintedIds: List<Int> = emptyList(),
    val score: Int = 0,
    val hintsUsed: Int = 0,
    val shuffles: Int = 0,
    val rngState: Long = 1,
    val lastEvent: String? = null,
)
