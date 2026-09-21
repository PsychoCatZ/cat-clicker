import { roomEconomyScale } from '../balance'
import type { GameMode } from '../economy'
export const pairsBaseReward = (matches: number, attempts: number, totalPairs: number, finished: boolean): number => {
  const safeTotal = Math.max(1, Math.floor(totalPairs))
  const safeMatches = Math.max(0, Math.min(safeTotal, Math.floor(matches)))
  const pairReward = safeMatches * 10
  const efficiencyBonus = finished ? Math.max(0, safeTotal * 3 - Math.max(safeTotal, Math.floor(attempts))) * 2 : 0
  return pairReward + efficiencyBonus
}

export const pairsFishReward = (matches: number, attempts: number, totalPairs: number, roomId: number, mode: GameMode, finished: boolean): number =>
  Math.floor(pairsBaseReward(matches, attempts, totalPairs, finished) * roomEconomyScale(roomId) * (mode === 'expert' ? 1.5 : 1))
