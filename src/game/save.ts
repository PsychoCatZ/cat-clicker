import { catsForRoom, firstCatForRoom } from './cats'
import { initialState, newRoomProgress, type GameMode, type GameState, type RoomProgress } from './economy'
import { defaultFurniturePosition, type FurniturePoint, type FurniturePosition } from './furniture'
import { resources } from './items'
import { findMatchRuns, findPossibleSwap } from './match3/board'
import { MATCH3_MOVES, MATCH3_SIZE, type Match3Round, type Match3Tile } from './match3/types'
import { rooms } from './rooms'
import { upgradesForRoom } from './upgrades'

const SAVE_KEY = 'cat-clicker-save-v3'
const PREVIOUS_SAVE_KEY = 'cat-clicker-save-v2'
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
  return {
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
    finalDismissed: data.finalDismissed === true,
  }
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
    const current = localStorage.getItem(SAVE_KEY)
    if (current) return readCurrentSave(current) ?? initialState()
    const previous = localStorage.getItem(PREVIOUS_SAVE_KEY)
    if (previous) return readCurrentSave(previous) ?? initialState()
    const old = localStorage.getItem(OLD_SAVE_KEY)
    if (old) return migrateOldSave(old) ?? initialState()
  } catch {
    // Storage may be unavailable or contain an invalid save.
  }
  return initialState()
}

export function saveGame(state: GameState): void {
  try {
    localStorage.setItem(SAVE_KEY, JSON.stringify({ ...state, savedAt: Date.now() }))
  } catch {
    // Gameplay remains available when storage is blocked.
  }
}
