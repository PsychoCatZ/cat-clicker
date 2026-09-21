import { catsForRoom, firstCatForRoom } from './cats'
import { expertCostScale, roomEconomyScale } from './balance'
import { constrainFurniturePoint, visibleFurniture, type FurniturePosition, type SceneLayout } from './furniture'
import { foods, resources } from './items'
import { createMatch3Round, playMatch3Turn } from './match3/reducer'
import { match3FishReward } from './match3/scoring'
import type { Match3Round } from './match3/types'
import { rooms } from './rooms'
import { upgradesForRoom, type Upgrade } from './upgrades'

export type GameMode = 'normal' | 'expert'

export interface RoomProgress {
  fish: number
  hunger: number
  lightsOff: boolean
  resourceLevels: Record<string, number>
  boughtUpgrades: string[]
  furniturePositions: Record<string, FurniturePosition>
  unlockedCats: string[]
  selectedCat: string
  caviarSeconds: number
}

export interface GameState {
  mode: GameMode
  currentRoom: number
  unlockedRoom: number
  rooms: RoomProgress[]
  match3: { activeRound: Match3Round | null }
  finalDismissed: boolean
}

export type GameAction =
  | { type: 'click' }
  | { type: 'tick'; seconds: number }
  | { type: 'toggleLights' }
  | { type: 'startMatch3'; seed: number }
  | { type: 'match3Swap'; first: number; second: number }
  | { type: 'settleMatch3' }
  | { type: 'buyResource'; id: string }
  | { type: 'buyUpgrade'; id: string }
  | { type: 'placeFurniture'; id: string; layout: SceneLayout; x: number; y: number }
  | { type: 'buyFood'; id: string }
  | { type: 'buyCat'; id: string }
  | { type: 'selectCat'; id: string }
  | { type: 'visitRoom'; roomId: number }
  | { type: 'enterNextRoom' }
  | { type: 'dismissFinal' }
  | { type: 'showFinal' }
  | { type: 'startExpert' }
  | { type: 'reset' }

export const newRoomProgress = (roomId: number): RoomProgress => ({
  fish: 0,
  hunger: 100,
  lightsOff: false,
  resourceLevels: Object.fromEntries(resources.map((item) => [item.id, 0])),
  boughtUpgrades: [],
  furniturePositions: {},
  unlockedCats: [firstCatForRoom(roomId).id],
  selectedCat: firstCatForRoom(roomId).id,
  caviarSeconds: 0,
})

export const initialState = (mode: GameMode = 'normal'): GameState => ({
  mode,
  currentRoom: 1,
  unlockedRoom: 1,
  rooms: rooms.map((room) => newRoomProgress(room.id)),
  match3: { activeRound: null },
  finalDismissed: false,
})

export const activeProgress = (state: GameState): RoomProgress => state.rooms[state.currentRoom - 1]
export const hungerDuration = (mode: GameMode): number => mode === 'expert' ? 12 * 60 : 20 * 60

const costMultiplier = (state: GameState): number =>
  roomEconomyScale(state.currentRoom) * (state.mode === 'expert' ? expertCostScale : 1)

export const catCost = (state: GameState, baseCost: number): number => Math.ceil(baseCost * costMultiplier(state))
export const upgradeCost = (state: GameState, upgrade: Upgrade): number => Math.ceil(upgrade.baseCost * costMultiplier(state))
export const foodCost = (state: GameState, baseCost: number): number => Math.ceil(baseCost * costMultiplier(state))
export const resourceCost = (state: GameState, id: string): number => {
  const resource = resources.find((item) => item.id === id)
  if (!resource) return Infinity
  const level = activeProgress(state).resourceLevels[id] ?? 0
  return Math.ceil(resource.baseCost * resource.growth ** level * costMultiplier(state))
}

export const clickPower = (state: GameState): number =>
  1 + resources.reduce((total, item) => total + item.bonus * (activeProgress(state).resourceLevels[item.id] ?? 0), 0)

export const currentClickReward = (state: GameState): number =>
  clickPower(state) * (activeProgress(state).caviarSeconds > 0 ? 2 : 1)

export const fishPerSecond = (state: GameState): number =>
  upgradesForRoom(state.currentRoom).reduce((total, item) =>
    total + (activeProgress(state).boughtUpgrades.includes(item.id) ? item.income : 0), 0)

export const basicUpgradesBought = (state: GameState): boolean =>
  upgradesForRoom(state.currentRoom).filter((item) => item.tier === 'basic')
    .every((item) => activeProgress(state).boughtUpgrades.includes(item.id))

export const roomComplete = (state: GameState, roomId = state.currentRoom): boolean => {
  const progress = state.rooms[roomId - 1]
  return catsForRoom(roomId).every((cat) => progress.unlockedCats.includes(cat.id))
    && upgradesForRoom(roomId).every((item) => progress.boughtUpgrades.includes(item.id))
}

export const safetyIncome = (state: GameState): number => {
  const progress = activeProgress(state)
  const mousePrice = foodCost(state, foods[0].baseCost)
  return progress.hunger === 0 && progress.boughtUpgrades.length === 0 && progress.fish < mousePrice
    ? mousePrice / (30 * 60)
    : 0
}

function updateProgress(state: GameState, next: RoomProgress): GameState {
  const updated = [...state.rooms]
  updated[state.currentRoom - 1] = next
  return { ...state, rooms: updated }
}

function settleMatch3(state: GameState): GameState {
  const round = state.match3.activeRound
  if (!round) return state
  const reward = match3FishReward(round.score, round.roomId, round.mode)
  return {
    ...state,
    rooms: state.rooms.map((room, index) => index === round.roomId - 1
      ? { ...room, fish: Math.min(1e15, room.fish + reward) }
      : room),
    match3: { activeRound: null },
  }
}

function completeIfNeeded(state: GameState): GameState {
  return state.currentRoom === rooms.length && roomComplete(state)
    ? { ...state, finalDismissed: false }
    : state
}

export function gameReducer(state: GameState, action: GameAction): GameState {
  const progress = activeProgress(state)
  switch (action.type) {
    case 'click':
      return progress.hunger > 0 && !progress.lightsOff ? updateProgress(state, { ...progress, fish: progress.fish + currentClickReward(state) }) : state
    case 'toggleLights':
      return progress.hunger <= 0 ? state : updateProgress(state, { ...progress, lightsOff: !progress.lightsOff })
    case 'startMatch3':
      return state.match3.activeRound ? state : {
        ...state,
        match3: {
          activeRound: createMatch3Round(
            state.currentRoom,
            state.mode,
            catsForRoom(state.currentRoom).map((cat) => cat.id),
            action.seed,
          ),
        },
      }
    case 'match3Swap': {
      const round = state.match3.activeRound
      if (!round || round.roomId !== state.currentRoom) return state
      const result = playMatch3Turn(round, action.first, action.second, catsForRoom(round.roomId).map((cat) => cat.id))
      return result.accepted ? { ...state, match3: { activeRound: result.round } } : state
    }
    case 'settleMatch3':
      return settleMatch3(state)
    case 'tick': {
      const seconds = Math.max(0, Math.min(action.seconds, 2))
      if (seconds === 0) return state
      const fallback = safetyIncome(state)
      const income = fishPerSecond(state) + fallback
      const earnedFish = progress.fish + income * seconds
      const mousePrice = foodCost(state, foods[0].baseCost)
      const nextProgress = {
        ...progress,
        fish: fallback > 0 && earnedFish >= mousePrice - 1e-7 ? mousePrice : earnedFish,
        hunger: progress.lightsOff ? progress.hunger : Math.max(0, progress.hunger - seconds * 100 / hungerDuration(state.mode)),
        caviarSeconds: Math.max(0, progress.caviarSeconds - seconds),
      }
      return {
        ...state,
        rooms: state.rooms.map((room, index) => index === state.currentRoom - 1
          ? nextProgress
          : room.caviarSeconds > 0 ? { ...room, caviarSeconds: Math.max(0, room.caviarSeconds - seconds) } : room),
      }
    }
    case 'buyResource': {
      const resource = resources.find((item) => item.id === action.id)
      if (!resource) return state
      const cost = resourceCost(state, resource.id)
      if (progress.fish < cost) return state
      return updateProgress(state, {
        ...progress,
        fish: progress.fish - cost,
        resourceLevels: { ...progress.resourceLevels, [resource.id]: (progress.resourceLevels[resource.id] ?? 0) + 1 },
      })
    }
    case 'buyUpgrade': {
      const upgrade = upgradesForRoom(state.currentRoom).find((item) => item.id === action.id)
      if (!upgrade || progress.boughtUpgrades.includes(upgrade.id)) return state
      if (upgrade.tier === 'advanced' && !basicUpgradesBought(state)) return state
      const cost = upgradeCost(state, upgrade)
      if (progress.fish < cost) return state
      return completeIfNeeded(updateProgress(state, {
        ...progress,
        fish: progress.fish - cost,
        boughtUpgrades: [...progress.boughtUpgrades, upgrade.id],
      }))
    }
    case 'placeFurniture': {
      const upgrade = visibleFurniture(state.currentRoom, progress.boughtUpgrades).find((item) => item.id === action.id)
      if (!upgrade || (action.layout !== 'desktop' && action.layout !== 'mobile')) return state
      const point = constrainFurniturePoint(upgrade, action.layout, { x: action.x, y: action.y })
      if (!point) return state
      return updateProgress(state, {
        ...progress,
        furniturePositions: {
          ...progress.furniturePositions,
          [upgrade.id]: { ...progress.furniturePositions[upgrade.id], [action.layout]: point },
        },
      })
    }
    case 'buyFood': {
      const food = foods.find((item) => item.id === action.id)
      if (!food || (progress.hunger >= 100 && !food.boostSeconds)) return state
      const cost = foodCost(state, food.baseCost)
      if (progress.fish < cost) return state
      return updateProgress(state, {
        ...progress,
        fish: progress.fish - cost,
        hunger: Math.min(100, progress.hunger + food.restore),
        caviarSeconds: food.boostSeconds ?? progress.caviarSeconds,
      })
    }
    case 'buyCat': {
      const cat = catsForRoom(state.currentRoom).find((item) => item.id === action.id)
      if (!cat || progress.unlockedCats.includes(cat.id)) return state
      const cost = catCost(state, cat.baseCost)
      if (progress.fish < cost) return state
      return completeIfNeeded(updateProgress(state, {
        ...progress,
        fish: progress.fish - cost,
        unlockedCats: [...progress.unlockedCats, cat.id],
        selectedCat: cat.id,
      }))
    }
    case 'selectCat':
      return progress.unlockedCats.includes(action.id) && catsForRoom(state.currentRoom).some((cat) => cat.id === action.id)
        ? updateProgress(state, { ...progress, selectedCat: action.id }) : state
    case 'visitRoom':
      return Number.isInteger(action.roomId) && action.roomId >= 1 && action.roomId <= state.unlockedRoom
        ? { ...settleMatch3(state), currentRoom: action.roomId } : state
    case 'enterNextRoom':
      return roomComplete(state) && state.currentRoom < rooms.length
        ? { ...state, currentRoom: state.currentRoom + 1, unlockedRoom: Math.max(state.unlockedRoom, state.currentRoom + 1) } : state
    case 'dismissFinal':
      return { ...state, finalDismissed: true }
    case 'showFinal':
      return state.currentRoom === rooms.length && roomComplete(state) ? { ...state, finalDismissed: false } : state
    case 'startExpert':
      return state.mode === 'normal' && roomComplete(state, rooms.length) ? initialState('expert') : state
    case 'reset':
      return initialState()
  }
}
