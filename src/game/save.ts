import { cats, initialCatId } from './cats'
import { clickPowerFromLevels, initialState, type GameState } from './economy'
import { upgrades } from './upgrades'

const SAVE_KEY = 'cat-clicker-save-v1'

export function loadGame(): GameState {
  try {
    const raw = localStorage.getItem(SAVE_KEY)
    if (!raw) return initialState()
    const parsed: unknown = JSON.parse(raw)
    if (!parsed || typeof parsed !== 'object') return initialState()
    const value = parsed as Partial<GameState>
    const fish = typeof value.fish === 'number' && Number.isFinite(value.fish) && value.fish >= 0 ? Math.floor(value.fish) : 0
    const upgradeLevels = Object.fromEntries(upgrades.map((upgrade) => {
      const level = value.upgradeLevels?.[upgrade.id]
      return [upgrade.id, typeof level === 'number' && Number.isInteger(level) && level >= 0 && level <= 1000 ? level : 0]
    }))
    const unlockedCats = Array.isArray(value.unlockedCats)
      ? [...new Set([initialCatId, ...value.unlockedCats.filter((id): id is string => typeof id === 'string' && cats.some((cat) => cat.id === id))])]
      : [initialCatId]
    const selectedCat = typeof value.selectedCat === 'string' && unlockedCats.includes(value.selectedCat) ? value.selectedCat : initialCatId
    return { fish, clickPower: clickPowerFromLevels(upgradeLevels), upgradeLevels, unlockedCats, selectedCat }
  } catch {
    return initialState()
  }
}

export function saveGame(state: GameState): void {
  try {
    localStorage.setItem(SAVE_KEY, JSON.stringify(state))
  } catch {
    // Private browsing or full storage can block saving; gameplay still works.
  }
}
