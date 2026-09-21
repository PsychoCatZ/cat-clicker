import { roomEconomyScale } from '../balance'
import type { GameMode } from '../economy'
import type { Match3Run } from './types'

export const runPoints = (length: number): number => {
  if (length < 3) return 0
  if (length === 3) return 30
  if (length === 4) return 60
  return 100 + (length - 5) * 40
}

export const cascadeMultiplier = (depth: number): number => Math.min(3, 1 + Math.max(0, depth - 1) * 0.5)

export const scoreWave = (runs: Match3Run[], depth: number): number =>
  Math.round(runs.reduce((total, run) => total + runPoints(run.length), 0) * cascadeMultiplier(depth))

export const baseFishReward = (score: number): number => {
  const safeScore = Math.max(0, score)
  return Math.min(safeScore, 1000) / 20
    + Math.min(Math.max(safeScore - 1000, 0), 1000) / 40
    + Math.max(safeScore - 2000, 0) / 100
}

export const match3FishReward = (score: number, roomId: number, mode: GameMode): number =>
  Math.floor(baseFishReward(score) * roomEconomyScale(roomId) * (mode === 'expert' ? 1.5 : 1))
