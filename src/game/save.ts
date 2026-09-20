import { catsForRoom, firstCatForRoom } from './cats'
import { initialState, newRoomProgress, type GameMode, type GameState, type RoomProgress } from './economy'
import { resources } from './items'
import { rooms } from './rooms'
import { upgradesForRoom } from './upgrades'

const SAVE_KEY = 'cat-clicker-save-v2'
const OLD_SAVE_KEY = 'cat-clicker-save-v1'

const safeNumber = (value: unknown, fallback = 0): number =>
  typeof value === 'number' && Number.isFinite(value) && value >= 0 ? value : fallback

function readRoom(value: unknown, roomId: number): RoomProgress {
  const empty = newRoomProgress(roomId)
  if (!value || typeof value !== 'object') return empty
  const data = value as Partial<RoomProgress>
  const validCats = new Set(catsForRoom(roomId).map((cat) => cat.id))
  const validUpgrades = new Set(upgradesForRoom(roomId).map((item) => item.id))
  const unlockedCats = Array.isArray(data.unlockedCats)
    ? [...new Set([firstCatForRoom(roomId).id, ...data.unlockedCats.filter((id): id is string => typeof id === 'string' && validCats.has(id))])]
    : empty.unlockedCats
  return {
    fish: Math.min(safeNumber(data.fish), 1e15),
    hunger: Math.min(safeNumber(data.hunger, 100), 100),
    resourceLevels: Object.fromEntries(resources.map((item) => {
      const level = data.resourceLevels?.[item.id]
      return [item.id, typeof level === 'number' && Number.isInteger(level) && level >= 0 ? Math.min(level, 1000) : 0]
    })),
    boughtUpgrades: Array.isArray(data.boughtUpgrades)
      ? [...new Set(data.boughtUpgrades.filter((id): id is string => typeof id === 'string' && validUpgrades.has(id)))]
      : [],
    unlockedCats,
    selectedCat: typeof data.selectedCat === 'string' && unlockedCats.includes(data.selectedCat)
      ? data.selectedCat : firstCatForRoom(roomId).id,
    caviarSeconds: Math.min(safeNumber(data.caviarSeconds), 60),
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
  state.rooms[0] = {
    ...room,
    fish: Math.min(safeNumber(old.fish), 1e15),
    resourceLevels: {
      ...room.resourceLevels,
      fish: Math.min(Math.floor(safeNumber(levels.bowl) + safeNumber(levels.mouse)), 1000),
    },
    boughtUpgrades: [
      safeNumber(levels.bed) > 0 ? 'room-1-basic-2' : '',
      safeNumber(levels.scratcher) > 0 ? 'room-1-basic-3' : '',
      safeNumber(levels.yarn) > 0 ? 'room-1-basic-5' : '',
    ].filter(Boolean),
    unlockedCats: [...new Set([room.unlockedCats[0], ...unlockedCats])],
    selectedCat: newCats.find((_, index) => old.selectedCat === oldIds[index] && unlockedCats.includes(newCats[index].id))?.id ?? room.selectedCat,
  }
  return state
}

export function loadGame(): GameState {
  try {
    const current = localStorage.getItem(SAVE_KEY)
    if (current) return readCurrentSave(current) ?? initialState()
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
