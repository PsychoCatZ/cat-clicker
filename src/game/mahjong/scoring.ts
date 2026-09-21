import { roomEconomyScale } from '../balance'
import type { GameMode } from '../economy'

export const MAHJONG_PAIR_POINTS = 100
export const MAHJONG_CLEAR_BONUS = 500
export const MAHJONG_NO_HINT_BONUS = 200
export const MAHJONG_NO_SHUFFLE_BONUS = 200

export const mahjongCompletionBonus = (hintsUsed: number, shuffles: number): number =>
  MAHJONG_CLEAR_BONUS
  + (hintsUsed === 0 ? MAHJONG_NO_HINT_BONUS : 0)
  + (shuffles === 0 ? MAHJONG_NO_SHUFFLE_BONUS : 0)

export const mahjongFishReward = (score: number, roomId: number, mode: GameMode): number =>
  Math.floor(Math.max(0, score) / 30 * roomEconomyScale(roomId) * (mode === 'expert' ? 1.5 : 1))
