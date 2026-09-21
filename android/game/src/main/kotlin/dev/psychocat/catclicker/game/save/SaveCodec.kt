package dev.psychocat.catclicker.game.save

import dev.psychocat.catclicker.game.data.Cats
import dev.psychocat.catclicker.game.data.FurniturePoint
import dev.psychocat.catclicker.game.data.FurniturePosition
import dev.psychocat.catclicker.game.data.Items
import dev.psychocat.catclicker.game.data.Rooms
import dev.psychocat.catclicker.game.data.Upgrades
import dev.psychocat.catclicker.game.engine.GameEngine
import dev.psychocat.catclicker.game.minigames.MiniGameRound
import dev.psychocat.catclicker.game.minigames.RoundStatus
import dev.psychocat.catclicker.game.minigames.mahjong.MahjongBoard
import dev.psychocat.catclicker.game.minigames.mahjong.MahjongDifficulty
import dev.psychocat.catclicker.game.minigames.mahjong.MahjongEvent
import dev.psychocat.catclicker.game.minigames.mahjong.MahjongLayouts
import dev.psychocat.catclicker.game.minigames.mahjong.MahjongRound
import dev.psychocat.catclicker.game.minigames.mahjong.MahjongTile
import dev.psychocat.catclicker.game.minigames.match3.MATCH3_MOVES
import dev.psychocat.catclicker.game.minigames.match3.MATCH3_SIZE
import dev.psychocat.catclicker.game.minigames.match3.Match3Board
import dev.psychocat.catclicker.game.minigames.match3.Match3Round
import dev.psychocat.catclicker.game.minigames.match3.Match3Tile
import dev.psychocat.catclicker.game.minigames.pairs.PairCard
import dev.psychocat.catclicker.game.minigames.pairs.PairsGame
import dev.psychocat.catclicker.game.minigames.pairs.PairsRound
import dev.psychocat.catclicker.game.minigames.sliding.SlidingBoard
import dev.psychocat.catclicker.game.minigames.sliding.SlidingDifficulty
import dev.psychocat.catclicker.game.minigames.sliding.SlidingEvent
import dev.psychocat.catclicker.game.minigames.sliding.SlidingGame
import dev.psychocat.catclicker.game.minigames.sliding.SlidingRound
import dev.psychocat.catclicker.game.rng.Xorshift32
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
        activeGame = activeGame?.toDto(),
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
        val restoredMode = if (mode == "expert") GameMode.EXPERT else GameMode.NORMAL
        val restoredRoom = currentRoom.coerceIn(1, unlocked)
        return GameState(
            mode = restoredMode,
            currentRoom = restoredRoom,
            unlockedRoom = unlocked,
            rooms = Rooms.all.map { cleanRoom(rooms.getOrNull(it.id - 1), it.id) },
            offlineReport = null,
            finalDismissed = finalDismissed,
            activeGame = cleanActiveGame(activeGame, restoredMode, restoredRoom, unlocked),
        )
    }

    private fun MiniGameRound.toDto(): ActiveGameDto = when (this) {
        is SlidingRound -> ActiveGameDto(
            sliding = SlidingRoundDto(
                id = id, roomId = roomId, mode = if (mode == GameMode.EXPERT) "expert" else "normal",
                difficulty = difficulty.key, catId = catId, tiles = tiles, moves = moves, rngState = rngState,
                lastEvent = lastEvent?.key,
            ),
        )
        is PairsRound -> ActiveGameDto(
            pairs = PairsRoundDto(
                id = id, roomId = roomId, mode = if (mode == GameMode.EXPERT) "expert" else "normal",
                cardCount = cardCount, cards = cards.map { PairCardDto(it.id, it.catId, it.matched) },
                revealed = revealed, attempts = attempts, rngState = rngState, lastMatch = lastMatch,
            ),
        )
        is Match3Round -> ActiveGameDto(
            match3 = Match3RoundDto(
                id = id, roomId = roomId, mode = if (mode == GameMode.EXPERT) "expert" else "normal",
                board = board.map { Match3TileDto(it.id, it.catId) }, movesLeft = movesLeft, score = score,
                maxCombo = maxCombo, rngState = rngState, nextTileId = nextTileId, lastGain = lastGain,
                lastCombo = lastCombo, shuffled = shuffled,
            ),
        )
        is MahjongRound -> ActiveGameDto(
            mahjong = MahjongRoundDto(
                id = id, roomId = roomId, mode = if (mode == GameMode.EXPERT) "expert" else "normal",
                difficulty = difficulty.key, tiles = tiles.map { MahjongTileDto(it.id, it.catId, it.removed) },
                selectedId = selectedId, hintedIds = hintedIds, score = score, hintsUsed = hintsUsed, shuffles = shuffles,
                rngState = rngState, lastEvent = lastEvent?.key,
            ),
        )
        else -> ActiveGameDto() // a game the save format does not know yet: not stored
    }

    /** Like the web version, a round is only restored in the room and mode where it was started. */
    private fun cleanActiveGame(dto: ActiveGameDto?, mode: GameMode, currentRoom: Int, unlockedRoom: Int): MiniGameRound? {
        dto?.sliding?.let { return cleanSliding(it, mode, currentRoom, unlockedRoom) }
        dto?.pairs?.let { return cleanPairs(it, mode, currentRoom, unlockedRoom) }
        dto?.match3?.let { return cleanMatch3(it, mode, currentRoom, unlockedRoom) }
        dto?.mahjong?.let { return cleanMahjong(it, mode, currentRoom, unlockedRoom) }
        return null
    }

    private fun cleanMahjong(dto: MahjongRoundDto, mode: GameMode, currentRoom: Int, unlockedRoom: Int): MahjongRound? {
        val modeKey = if (mode == GameMode.EXPERT) "expert" else "normal"
        val difficulty = MahjongDifficulty.fromKey(dto.difficulty) ?: return null
        if (dto.roomId != currentRoom || dto.roomId < 1 || dto.roomId > unlockedRoom || dto.mode != modeKey) return null
        val layout = MahjongLayouts.of(difficulty)
        if (dto.tiles.size != layout.tileCount) return null
        val roomCats = Cats.forRoom(dto.roomId).map { it.id }.toSet()
        val rawById = HashMap<Int, MahjongTileDto>()
        for (raw in dto.tiles) {
            if (rawById.put(raw.id, raw) != null) return null
        }
        val tiles = ArrayList<MahjongTile>()
        for (slot in layout.slots) {
            val raw = rawById[slot.id] ?: return null
            if (raw.catId !in roomCats) return null
            tiles.add(MahjongTile(slot, raw.catId, raw.removed))
        }
        val removedCount = tiles.count { it.removed }
        if (removedCount % 2 != 0 || roomCats.any { catId -> tiles.count { !it.removed && it.catId == catId } % 2 != 0 }) return null
        val finished = removedCount == layout.tileCount
        val selectedId = dto.selectedId?.takeIf { MahjongBoard.isFree(tiles, it) }
        val pairs = MahjongBoard.findPairs(tiles)
        val hinted = dto.hintedIds.size == 2 && pairs.any { (a, b) -> a in dto.hintedIds && b in dto.hintedIds }
        return MahjongRound(
            id = dto.id.take(120).ifEmpty { "${dto.roomId}-$modeKey-${difficulty.key}-saved" },
            roomId = dto.roomId,
            mode = mode,
            difficulty = difficulty,
            tiles = tiles,
            selectedId = if (finished) null else selectedId,
            hintedIds = if (finished || !hinted) emptyList() else dto.hintedIds,
            score = dto.score.coerceIn(0, 1_000_000_000),
            pairsFound = removedCount / 2,
            hintsUsed = dto.hintsUsed.coerceIn(0, 100_000),
            shuffles = dto.shuffles.coerceIn(0, 100_000),
            rngState = Xorshift32.normalize(dto.rngState),
            status = if (finished) RoundStatus.FINISHED else RoundStatus.PLAYING,
            lastEvent = if (finished) MahjongEvent.COMPLETED else MahjongEvent.fromKey(dto.lastEvent),
        )
    }

    private fun cleanMatch3(dto: Match3RoundDto, mode: GameMode, currentRoom: Int, unlockedRoom: Int): Match3Round? {
        val modeKey = if (mode == GameMode.EXPERT) "expert" else "normal"
        if (dto.roomId != currentRoom || dto.roomId < 1 || dto.roomId > unlockedRoom || dto.mode != modeKey) return null
        if (dto.board.size != MATCH3_SIZE * MATCH3_SIZE) return null
        val roomCats = Cats.forRoom(dto.roomId).map { it.id }.toSet()
        val ids = HashSet<Int>()
        val board = ArrayList<Match3Tile>()
        for (tile in dto.board) {
            if (tile.id < 0 || !ids.add(tile.id) || tile.catId !in roomCats) return null
            board.add(Match3Tile(tile.id, tile.catId))
        }
        // A saved board is always a settled one: no ready lines and at least one possible move.
        if (Match3Board.findRuns(board).isNotEmpty() || Match3Board.findPossibleSwap(board) == null) return null
        val movesLeft = dto.movesLeft.coerceIn(0, MATCH3_MOVES)
        val maxId = board.maxOf { it.id }
        return Match3Round(
            id = dto.id.take(100).ifEmpty { "${dto.roomId}-$modeKey-saved" },
            roomId = dto.roomId,
            mode = mode,
            board = board,
            movesLeft = movesLeft,
            score = dto.score.coerceIn(0, 1_000_000_000),
            maxCombo = dto.maxCombo.coerceIn(0, 50),
            rngState = Xorshift32.normalize(dto.rngState),
            nextTileId = maxOf(maxId + 1, dto.nextTileId),
            status = if (movesLeft == 0) RoundStatus.FINISHED else RoundStatus.PLAYING,
            lastGain = dto.lastGain.coerceIn(0, 1_000_000_000),
            lastCombo = dto.lastCombo.coerceIn(0, 50),
            shuffled = dto.shuffled,
        )
    }

    private fun cleanPairs(dto: PairsRoundDto, mode: GameMode, currentRoom: Int, unlockedRoom: Int): PairsRound? {
        val modeKey = if (mode == GameMode.EXPERT) "expert" else "normal"
        if (dto.roomId != currentRoom || dto.roomId < 1 || dto.roomId > unlockedRoom || dto.mode != modeKey) return null
        if (dto.cardCount !in PairsGame.CARD_COUNTS || dto.cards.size != dto.cardCount) return null
        val roomCats = Cats.forRoom(dto.roomId).map { it.id }
        val ids = HashSet<Int>()
        for (card in dto.cards) {
            if (card.id < 0 || !ids.add(card.id) || card.catId !in roomCats) return null
        }
        val perCat = roomCats.map { catId -> dto.cards.count { it.catId == catId } }.sorted()
        if (perCat != PairsGame.expectedCatCounts(dto.cardCount)) return null
        val matchedCards = dto.cards.filter { it.matched }
        if (matchedCards.size % 2 != 0 || roomCats.any { catId -> matchedCards.count { it.catId == catId } % 2 != 0 }) return null
        val matches = matchedCards.size / 2
        val byId = dto.cards.associateBy { it.id }
        val revealed = dto.revealed.filter { it in ids }.take(2)
        if (revealed.toSet().size != revealed.size || revealed.any { byId.getValue(it).matched }) return null
        if (revealed.size == 2 && byId.getValue(revealed[0]).catId == byId.getValue(revealed[1]).catId) return null
        return PairsRound(
            id = dto.id.take(100).ifEmpty { "${dto.roomId}-$modeKey-pairs-saved" },
            roomId = dto.roomId,
            mode = mode,
            cardCount = dto.cardCount,
            cards = dto.cards.map { PairCard(it.id, it.catId, it.matched) },
            revealed = revealed,
            attempts = dto.attempts.coerceIn(0, 100_000),
            matches = matches,
            status = if (matches == dto.cardCount / 2) RoundStatus.FINISHED else RoundStatus.PLAYING,
            lastMatch = if (revealed.size == 2) false else if (dto.lastMatch == true) true else null,
            rngState = Xorshift32.normalize(dto.rngState),
        )
    }

    private fun cleanSliding(dto: SlidingRoundDto, mode: GameMode, currentRoom: Int, unlockedRoom: Int): SlidingRound? {
        val modeKey = if (mode == GameMode.EXPERT) "expert" else "normal"
        val difficulty = SlidingDifficulty.fromKey(dto.difficulty) ?: return null
        if (dto.roomId != currentRoom || dto.roomId < 1 || dto.roomId > unlockedRoom || dto.mode != modeKey) return null
        if (dto.tiles.size != difficulty.size * difficulty.size || !SlidingBoard.isSolvable(dto.tiles, difficulty.size)) return null
        if (Cats.forRoom(dto.roomId).none { it.id == dto.catId }) return null
        val moves = dto.moves.coerceIn(0, 1_000_000)
        val finished = SlidingBoard.isSolved(dto.tiles)
        val savedEvent = SlidingEvent.fromKey(dto.lastEvent)
        return SlidingRound(
            id = dto.id.take(160).ifEmpty { "${dto.roomId}-$modeKey-${difficulty.key}-${dto.catId}-saved" },
            roomId = dto.roomId,
            mode = mode,
            difficulty = difficulty,
            size = difficulty.size,
            catId = dto.catId,
            tiles = dto.tiles,
            moves = moves,
            score = if (finished) SlidingGame.completionScore(difficulty, moves) else 0,
            rngState = Xorshift32.normalize(dto.rngState),
            status = if (finished) RoundStatus.FINISHED else RoundStatus.PLAYING,
            lastEvent = when {
                finished -> SlidingEvent.COMPLETED
                savedEvent == SlidingEvent.COMPLETED -> null
                else -> savedEvent
            },
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
