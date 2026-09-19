import { cats, initialCatId } from './cats'
import { upgrades } from './upgrades'

export interface GameState {
  fish: number
  clickPower: number
  upgradeLevels: Record<string, number>
  unlockedCats: string[]
  selectedCat: string
}

export type GameAction =
  | { type: 'click' }
  | { type: 'tick' }
  | { type: 'buyUpgrade'; id: string }
  | { type: 'buyCat'; id: string }
  | { type: 'selectCat'; id: string }
  | { type: 'reset' }

export const initialState = (): GameState => ({
  fish: 0,
  clickPower: 1,
  upgradeLevels: Object.fromEntries(upgrades.map((upgrade) => [upgrade.id, 0])),
  unlockedCats: [initialCatId],
  selectedCat: initialCatId,
})

export const upgradeCost = (baseCost: number, growth: number, level: number): number =>
  Math.ceil(baseCost * growth ** level)

export const fishPerSecond = (state: GameState): number =>
  upgrades.reduce((total, upgrade) => total + (upgrade.kind === 'second' ? upgrade.bonus * (state.upgradeLevels[upgrade.id] ?? 0) : 0), 0)

export const clickPowerFromLevels = (levels: Record<string, number>): number =>
  1 + upgrades.reduce((total, upgrade) => total + (upgrade.kind === 'click' ? upgrade.bonus * (levels[upgrade.id] ?? 0) : 0), 0)

export function gameReducer(state: GameState, action: GameAction): GameState {
  switch (action.type) {
    case 'click':
      return { ...state, fish: state.fish + state.clickPower }
    case 'tick':
      return { ...state, fish: state.fish + fishPerSecond(state) }
    case 'buyUpgrade': {
      const upgrade = upgrades.find((item) => item.id === action.id)
      if (!upgrade) return state
      const level = state.upgradeLevels[upgrade.id] ?? 0
      const cost = upgradeCost(upgrade.baseCost, upgrade.growth, level)
      if (state.fish < cost) return state
      const upgradeLevels = { ...state.upgradeLevels, [upgrade.id]: level + 1 }
      return { ...state, fish: state.fish - cost, upgradeLevels, clickPower: clickPowerFromLevels(upgradeLevels) }
    }
    case 'buyCat': {
      const cat = cats.find((item) => item.id === action.id)
      if (!cat || cat.cost === null || state.unlockedCats.includes(cat.id) || state.fish < cat.cost) return state
      return { ...state, fish: state.fish - cat.cost, unlockedCats: [...state.unlockedCats, cat.id], selectedCat: cat.id }
    }
    case 'selectCat':
      return state.unlockedCats.includes(action.id) && cats.some((cat) => cat.id === action.id)
        ? { ...state, selectedCat: action.id }
        : state
    case 'reset':
      return initialState()
  }
}
