import { roomEconomyScale } from '../balance'
import type { GameMode } from '../economy'
import type { SlidingDifficulty } from './types'

const scoreRules: Record<SlidingDifficulty, { base: number; bonusMoves: number; bonusPerMove: number }> = {
  easy: { base: 900, bonusMoves: 80, bonusPerMove: 4 },
  normal: { base: 2000, bonusMoves: 180, bonusPerMove: 5 },
  hard: { base: 3600, bonusMoves: 360, bonusPerMove: 6 },
}

export const slidingCompletionScore = (difficulty: SlidingDifficulty, moves: number): number => {
  const rule = scoreRules[difficulty]
  const safeMoves = Math.max(0, Math.floor(moves))
  return rule.base + Math.max(0, rule.bonusMoves - safeMoves) * rule.bonusPerMove
}

export const slidingFishReward = (score: number, roomId: number, mode: GameMode): number =>
  Math.floor(Math.max(0, score) / 30 * roomEconomyScale(roomId) * (mode === 'expert' ? 1.5 : 1))
