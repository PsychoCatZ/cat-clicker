import { catsForRoom, firstCatForRoom } from './cats'
import { applyOfflineProgress, initialState, newRoomProgress, type GameMode, type GameState, type RoomProgress } from './economy'
import { defaultFurniturePosition, type FurniturePoint, type FurniturePosition } from './furniture'
import { resources } from './items'
import { findMatchRuns, findPossibleSwap } from './match3/board'
import { MATCH3_MOVES, MATCH3_SIZE, type Match3Round, type Match3Tile } from './match3/types'
import { findMahjongPairs, isMahjongTileFree } from './mahjong/board'
import { getMahjongLayout, mahjongDifficulties } from './mahjong/layouts'
import type { MahjongDifficulty, MahjongEvent, MahjongRound, MahjongTile } from './mahjong/types'
import { PAIRS_CARD_COUNTS, type PairCard, type PairsCardCount, type PairsRound, type PairsRulesId } from './pairs/types'
import { rooms } from './rooms'
import { isSlidingBoardSolvable, isSlidingBoardSolved } from './sliding/board'
import { getSlidingConfig, slidingDifficulties } from './sliding/layouts'
import { slidingCompletionScore } from './sliding/scoring'
import type { SlidingDifficulty, SlidingEvent, SlidingRound } from './sliding/types'
import { upgradesForRoom } from './upgrades'

const SAVE_KEY = 'cat-clicker-save-v6'
const CURRENT_SAVE_KEYS = [SAVE_KEY, 'cat-clicker-save-v5', 'cat-clicker-save-v4', 'cat-clicker-save-v3', 'cat-clicker-save-v2']
const OLD_SAVE_KEY = 'cat-clicker-save-v1'

const safeNumber = (value: unknown, fallback = 0): number =>
  typeof value === 'number' && Number.isFinite(value) && value >= 0 ? value : fallback

function readPoint(value: unknown): FurniturePoint | null {
  if (!value || typeof value !== 'object') return null
  const point = value as Partial<FurniturePoint>
  return typeof point.x === 'number' && Number.isFinite(point.x) && point.x >= 0 && point.x <= 100
    && typeof point.y === 'number' && Number.isFinite(point.y) && point.y >= 0 && point.y <= 100
    ? { x: point.x, y: point.y } : null
}

function readRoom(value: unknown, roomId: number): RoomProgress {
  const empty = newRoomProgress(roomId)
  if (!value || typeof value !== 'object') return empty
  const data = value as Partial<RoomProgress>
  const validCats = new Set(catsForRoom(roomId).map((cat) => cat.id))
  const roomUpgrades = upgradesForRoom(roomId)
  const validUpgrades = new Set(roomUpgrades.map((item) => item.id))
  const boughtUpgrades = Array.isArray(data.boughtUpgrades)
    ? [...new Set(data.boughtUpgrades.filter((id): id is string => typeof id === 'string' && validUpgrades.has(id)))]
    : []
  const hasPositions = Object.prototype.hasOwnProperty.call(data, 'furniturePositions')
  const furniturePositions = Object.fromEntries(boughtUpgrades.flatMap((id) => {
    const upgrade = roomUpgrades.find((item) => item.id === id)
    if (!upgrade) return []
    if (!hasPositions) return [[id, defaultFurniturePosition(upgrade)]]
    const raw = data.furniturePositions?.[id] as FurniturePosition | undefined
    const desktop = readPoint(raw?.desktop)
    const mobile = readPoint(raw?.mobile)
    return desktop || mobile ? [[id, { ...(desktop && { desktop }), ...(mobile && { mobile }) }]] : []
  }))
  const unlockedCats = Array.isArray(data.unlockedCats)
    ? [...new Set([firstCatForRoom(roomId).id, ...data.unlockedCats.filter((id): id is string => typeof id === 'string' && validCats.has(id))])]
    : empty.unlockedCats
  return {
    fish: Math.min(safeNumber(data.fish), 1e15),
    hunger: Math.min(safeNumber(data.hunger, 100), 100),
    lightsOff: data.lightsOff === true,
    resourceLevels: Object.fromEntries(resources.map((item) => {
      const level = data.resourceLevels?.[item.id]
      return [item.id, typeof level === 'number' && Number.isInteger(level) && level >= 0 ? Math.min(level, 1000) : 0]
    })),
    boughtUpgrades,
    furniturePositions,
    unlockedCats,
    selectedCat: typeof data.selectedCat === 'string' && unlockedCats.includes(data.selectedCat)
      ? data.selectedCat : firstCatForRoom(roomId).id,
    caviarSeconds: Math.min(safeNumber(data.caviarSeconds), 60),
  }
}

function readMatch3Round(value: unknown, mode: GameMode, currentRoom: number, unlockedRoom: number): Match3Round | null {
  if (!value || typeof value !== 'object') return null
  const data = value as Partial<Match3Round>
  const roomId = typeof data.roomId === 'number' && Number.isInteger(data.roomId)
    ? data.roomId : 0
  if (roomId !== currentRoom || roomId < 1 || roomId > unlockedRoom || data.mode !== mode || data.rulesId !== 'classic-7x7') return null
  const validCats = new Set(catsForRoom(roomId).map((cat) => cat.id))
  if (!Array.isArray(data.board) || data.board.length !== MATCH3_SIZE ** 2) return null
  const ids = new Set<number>()
  const board: Match3Tile[] = []
  for (const value of data.board as unknown[]) {
    if (!value || typeof value !== 'object') return null
    const tile = value as Partial<Match3Tile>
    if (typeof tile.id !== 'number' || !Number.isInteger(tile.id) || tile.id < 0 || ids.has(tile.id)
      || typeof tile.catId !== 'string' || !validCats.has(tile.catId) || tile.kind !== 'normal') return null
    ids.add(tile.id)
    board.push({ id: tile.id, catId: tile.catId, kind: 'normal' })
  }
  if (findMatchRuns(board).length > 0 || !findPossibleSwap(board)) return null
  const movesLeft = typeof data.movesLeft === 'number' && Number.isInteger(data.movesLeft)
    ? Math.max(0, Math.min(MATCH3_MOVES, data.movesLeft)) : MATCH3_MOVES
  const maxTileId = Math.max(...board.map((tile) => tile.id))
  return {
    id: typeof data.id === 'string' ? data.id.slice(0, 100) : `${roomId}-${mode}-saved`,
    roomId,
    mode,
    rulesId: 'classic-7x7',
    board,
    movesLeft,
    score: Math.min(safeNumber(data.score), 1e9),
    maxCombo: Math.min(Math.floor(safeNumber(data.maxCombo)), 50),
    rngState: typeof data.rngState === 'number' && Number.isInteger(data.rngState) ? data.rngState >>> 0 || 1 : 1,
    nextTileId: typeof data.nextTileId === 'number' && Number.isInteger(data.nextTileId)
      ? Math.max(maxTileId + 1, data.nextTileId) : maxTileId + 1,
    status: movesLeft === 0 ? 'finished' : 'playing',
    lastGain: Math.min(safeNumber(data.lastGain), 1e9),
    lastCombo: Math.min(Math.floor(safeNumber(data.lastCombo)), 50),
    shuffled: data.shuffled === true,
  }
}

function readPairsRound(value: unknown, mode: GameMode, currentRoom: number, unlockedRoom: number): PairsRound | null {
  if (!value || typeof value !== 'object') return null
  const data = value as Partial<PairsRound>
  const roomId = typeof data.roomId === 'number' && Number.isInteger(data.roomId) ? data.roomId : 0
  const rawRulesId = (value as { rulesId?: unknown }).rulesId
  const legacy = rawRulesId === 'pairs-5'
  const parsedCardCount = typeof rawRulesId === 'string' ? Number(rawRulesId.replace('pairs-', '')) : 0
  const cardCount = legacy ? 10 : PAIRS_CARD_COUNTS.find((count) => count === parsedCardCount)
  if (roomId !== currentRoom || roomId < 1 || roomId > unlockedRoom || data.mode !== mode || !cardCount) return null
  const validCats = new Set(catsForRoom(roomId).map((cat) => cat.id))
  if (!Array.isArray(data.cards) || data.cards.length !== cardCount) return null
  const ids = new Set<number>()
  const counts = new Map<string, number>()
  const cards: PairCard[] = []
  for (const value of data.cards as unknown[]) {
    if (!value || typeof value !== 'object') return null
    const card = value as Partial<PairCard>
    if (typeof card.id !== 'number' || !Number.isInteger(card.id) || card.id < 0 || ids.has(card.id)
      || typeof card.catId !== 'string' || !validCats.has(card.catId) || typeof card.matched !== 'boolean') return null
    ids.add(card.id)
    counts.set(card.catId, (counts.get(card.catId) ?? 0) + 1)
    cards.push({ id: card.id, catId: card.catId, matched: card.matched })
  }
  const actualCounts = [...validCats].map((catId) => counts.get(catId) ?? 0).sort((a, b) => a - b)
  const expectedCounts = cardCount === 10 ? [2, 2, 2, 2, 2]
    : cardCount === 16 ? [2, 2, 4, 4, 4] : [4, 4, 4, 4, 4]
  if (actualCounts.some((count, index) => count !== expectedCounts[index])) return null
  const matchedCards = cards.filter((card) => card.matched)
  if (matchedCards.length % 2 !== 0 || [...validCats].some((catId) => {
    const count = matchedCards.filter((card) => card.catId === catId).length
    return count % 2 !== 0
  })) return null
  const matches = matchedCards.length / 2
  const revealed = Array.isArray(data.revealed)
    ? data.revealed.filter((id): id is number => typeof id === 'number' && Number.isInteger(id) && ids.has(id)).slice(0, 2)
    : []
  if (new Set(revealed).size !== revealed.length || revealed.some((id) => cards.find((card) => card.id === id)?.matched)) return null
  if (revealed.length === 2 && cards.find((card) => card.id === revealed[0])?.catId === cards.find((card) => card.id === revealed[1])?.catId) return null
  const attempts = Math.min(100000, Math.floor(safeNumber(data.attempts)))
  return {
    id: typeof data.id === 'string' ? data.id.slice(0, 100) : `${roomId}-${mode}-pairs-saved`,
    roomId,
    mode,
    rulesId: `pairs-${cardCount}` as PairsRulesId,
    cardCount: cardCount as PairsCardCount,
    cards,
    revealed,
    attempts,
    matches,
    status: matches === cardCount / 2 ? 'finished' : 'playing',
    lastMatch: revealed.length === 2 ? false : data.lastMatch === true ? true : null,
    rngState: typeof data.rngState === 'number' && Number.isInteger(data.rngState) ? data.rngState >>> 0 || 1 : 1,
  }
}

function readMahjongRound(value: unknown, mode: GameMode, currentRoom: number, unlockedRoom: number): MahjongRound | null {
  if (!value || typeof value !== 'object') return null
  const data = value as Partial<MahjongRound>
  const roomId = typeof data.roomId === 'number' && Number.isInteger(data.roomId) ? data.roomId : 0
  const difficulty = mahjongDifficulties.includes(data.difficulty as MahjongDifficulty)
    ? data.difficulty as MahjongDifficulty : null
  if (roomId !== currentRoom || roomId < 1 || roomId > unlockedRoom || data.mode !== mode
    || data.rulesId !== 'mahjong-v1' || !difficulty || data.layoutId !== difficulty) return null
  const layout = getMahjongLayout(difficulty)
  if (!Array.isArray(data.tiles) || data.tiles.length !== layout.tileCount) return null
  const validCats = new Set(catsForRoom(roomId).map((cat) => cat.id))
  const rawById = new Map<number, Partial<MahjongTile>>()
  for (const value of data.tiles as unknown[]) {
    if (!value || typeof value !== 'object') return null
    const tile = value as Partial<MahjongTile>
    if (typeof tile.id !== 'number' || !Number.isInteger(tile.id) || rawById.has(tile.id)) return null
    rawById.set(tile.id, tile)
  }
  const tiles: MahjongTile[] = []
  for (const slot of layout.slots) {
    const raw = rawById.get(slot.id)
    if (!raw || typeof raw.catId !== 'string' || !validCats.has(raw.catId) || typeof raw.removed !== 'boolean') return null
    tiles.push({ ...slot, catId: raw.catId, removed: raw.removed })
  }
  const removedCount = tiles.filter((tile) => tile.removed).length
  if (removedCount % 2 !== 0 || [...validCats].some((catId) => tiles.filter((tile) => !tile.removed && tile.catId === catId).length % 2 !== 0)) return null
  const selectedId = typeof data.selectedId === 'number' && Number.isInteger(data.selectedId)
    && isMahjongTileFree(tiles, data.selectedId) ? data.selectedId : null
  const availablePairs = findMahjongPairs(tiles)
  const hintedIds = Array.isArray(data.hintedIds) && data.hintedIds.length === 2
    && data.hintedIds.every((id) => typeof id === 'number' && Number.isInteger(id))
    && availablePairs.some(([first, second]) => data.hintedIds?.includes(first) && data.hintedIds?.includes(second))
    ? data.hintedIds as number[] : []
  const validEvents: MahjongEvent[] = [null, 'selected', 'mismatch', 'match', 'hint', 'shuffled', 'completed']
  const lastEvent = validEvents.includes(data.lastEvent as MahjongEvent) ? data.lastEvent as MahjongEvent : null
  const finished = removedCount === layout.tileCount
  return {
    id: typeof data.id === 'string' ? data.id.slice(0, 120) : `${roomId}-${mode}-${difficulty}-saved`,
    roomId,
    mode,
    rulesId: 'mahjong-v1',
    difficulty,
    layoutId: difficulty,
    tiles,
    selectedId: finished ? null : selectedId,
    hintedIds: finished ? [] : hintedIds,
    score: Math.min(safeNumber(data.score), 1e9),
    pairsFound: removedCount / 2,
    hintsUsed: Math.min(100000, Math.floor(safeNumber(data.hintsUsed))),
    shuffles: Math.min(100000, Math.floor(safeNumber(data.shuffles))),
    rngState: typeof data.rngState === 'number' && Number.isInteger(data.rngState) ? data.rngState >>> 0 || 1 : 1,
    status: finished ? 'finished' : 'playing',
    lastEvent: finished ? 'completed' : lastEvent,
  }
}

function readSlidingRound(value: unknown, mode: GameMode, currentRoom: number, unlockedRoom: number): SlidingRound | null {
  if (!value || typeof value !== 'object') return null
  const data = value as Partial<SlidingRound>
  const roomId = typeof data.roomId === 'number' && Number.isInteger(data.roomId) ? data.roomId : 0
  const difficulty = slidingDifficulties.includes(data.difficulty as SlidingDifficulty)
    ? data.difficulty as SlidingDifficulty : null
  if (roomId !== currentRoom || roomId < 1 || roomId > unlockedRoom || data.mode !== mode
    || data.rulesId !== 'sliding-v1' || !difficulty) return null
  const config = getSlidingConfig(difficulty)
  if (data.size !== config.size || !Array.isArray(data.tiles) || data.tiles.length !== config.size ** 2) return null
  const tiles: Array<number | null> = []
  for (const tile of data.tiles as unknown[]) {
    if (tile !== null && (typeof tile !== 'number' || !Number.isInteger(tile))) return null
    tiles.push(tile as number | null)
  }
  if (!isSlidingBoardSolvable(tiles, config.size)) return null
  const validCats = new Set(catsForRoom(roomId).map((cat) => cat.id))
  if (typeof data.catId !== 'string' || !validCats.has(data.catId)) return null
  const moves = Math.min(1000000, Math.floor(safeNumber(data.moves)))
  const finished = isSlidingBoardSolved(tiles)
  const validEvents: SlidingEvent[] = [null, 'moved', 'shuffled', 'completed']
  const savedEvent = validEvents.includes(data.lastEvent as SlidingEvent) ? data.lastEvent as SlidingEvent : null
  return {
    id: typeof data.id === 'string' ? data.id.slice(0, 160) : `${roomId}-${mode}-${difficulty}-${data.catId}-saved`,
    roomId,
    mode,
    rulesId: 'sliding-v1',
    difficulty,
    size: config.size,
    catId: data.catId,
    tiles,
    moves,
    score: finished ? slidingCompletionScore(difficulty, moves) : 0,
    rngState: typeof data.rngState === 'number' && Number.isInteger(data.rngState) ? data.rngState >>> 0 || 1 : 1,
    status: finished ? 'finished' : 'playing',
    lastEvent: finished ? 'completed' : savedEvent === 'completed' ? null : savedEvent,
  }
}

function readCurrentSave(raw: string): GameState | null {
  const value: unknown = JSON.parse(raw)
  if (!value || typeof value !== 'object') return null
  const data = value as Partial<GameState> & { savedAt?: unknown }
  const mode: GameMode = data.mode === 'expert' ? 'expert' : 'normal'
  const unlockedRoom = Number.isInteger(data.unlockedRoom)
    ? Math.max(1, Math.min(rooms.length, data.unlockedRoom as number)) : 1
  const currentRoom = Number.isInteger(data.currentRoom)
    ? Math.max(1, Math.min(unlockedRoom, data.currentRoom as number)) : 1
  const elapsedSeconds = typeof data.savedAt === 'number' && Number.isFinite(data.savedAt)
    ? Math.max(0, (Date.now() - data.savedAt) / 1000) : 0
  const state: GameState = {
    mode,
    currentRoom,
    unlockedRoom,
    rooms: rooms.map((room) => {
      const progress = readRoom(data.rooms?.[room.id - 1], room.id)
      return { ...progress, caviarSeconds: Math.max(0, progress.caviarSeconds - elapsedSeconds) }
    }),
    match3: {
      activeRound: readMatch3Round(data.match3?.activeRound, mode, currentRoom, unlockedRoom),
    },
    pairs: {
      activeRound: readPairsRound(data.pairs?.activeRound, mode, currentRoom, unlockedRoom),
    },
    mahjong: {
      activeRound: readMahjongRound(data.mahjong?.activeRound, mode, currentRoom, unlockedRoom),
    },
    sliding: {
      activeRound: readSlidingRound(data.sliding?.activeRound, mode, currentRoom, unlockedRoom),
    },
    offlineReport: null,
    finalDismissed: data.finalDismissed === true,
  }
  if (state.match3.activeRound) {
    state.pairs.activeRound = null
    state.mahjong.activeRound = null
    state.sliding.activeRound = null
  } else if (state.pairs.activeRound) {
    state.mahjong.activeRound = null
    state.sliding.activeRound = null
  } else if (state.mahjong.activeRound) state.sliding.activeRound = null
  return applyOfflineProgress(state, elapsedSeconds)
}

function migrateOldSave(raw: string): GameState | null {
  const value: unknown = JSON.parse(raw)
  if (!value || typeof value !== 'object') return null
  const old = value as { fish?: unknown; upgradeLevels?: Record<string, unknown>; unlockedCats?: unknown; selectedCat?: unknown }
  const state = initialState()
  const room = state.rooms[0]
  const oldIds = ['ryzhik', 'ugolyok', 'poloska', 'snezhka', 'iris']
  const newCats = catsForRoom(1)
  const oldUnlockedCats: unknown[] = Array.isArray(old.unlockedCats) ? old.unlockedCats : []
  const unlockedCats = oldUnlockedCats.length
    ? newCats.filter((_, index) => oldUnlockedCats.includes(oldIds[index])).map((cat) => cat.id)
    : []
  const levels = old.upgradeLevels ?? {}
  const boughtUpgrades = [
    safeNumber(levels.bed) > 0 ? 'room-1-basic-2' : '',
    safeNumber(levels.scratcher) > 0 ? 'room-1-basic-3' : '',
    safeNumber(levels.yarn) > 0 ? 'room-1-basic-5' : '',
  ].filter(Boolean)
  state.rooms[0] = {
    ...room,
    fish: Math.min(safeNumber(old.fish), 1e15),
    resourceLevels: {
      ...room.resourceLevels,
      fish: Math.min(Math.floor(safeNumber(levels.bowl) + safeNumber(levels.mouse)), 1000),
    },
    boughtUpgrades,
    furniturePositions: Object.fromEntries(boughtUpgrades.flatMap((id) => {
      const upgrade = upgradesForRoom(1).find((item) => item.id === id)
      return upgrade ? [[id, defaultFurniturePosition(upgrade)]] : []
    })),
    unlockedCats: [...new Set([room.unlockedCats[0], ...unlockedCats])],
    selectedCat: newCats.find((_, index) => old.selectedCat === oldIds[index] && unlockedCats.includes(newCats[index].id))?.id ?? room.selectedCat,
  }
  return state
}

export function loadGame(): GameState {
  try {
    for (const key of CURRENT_SAVE_KEYS) {
      const current = localStorage.getItem(key)
      if (current) return readCurrentSave(current) ?? initialState()
    }
    const old = localStorage.getItem(OLD_SAVE_KEY)
    if (old) return migrateOldSave(old) ?? initialState()
  } catch {
    // Storage may be unavailable or contain an invalid save.
  }
  return initialState()
}

export function saveGame(state: GameState): void {
  try {
    localStorage.setItem(SAVE_KEY, JSON.stringify({ ...state, offlineReport: null, savedAt: Date.now() }))
  } catch {
    // Gameplay remains available when storage is blocked.
  }
}
