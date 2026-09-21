import type { GameMode } from '../economy'

export type SlidingDifficulty = 'easy' | 'normal' | 'hard'
export type SlidingEvent = 'moved' | 'shuffled' | 'completed' | null

export interface SlidingConfig {
  id: SlidingDifficulty
  name: string
  size: number
  shuffleMoves: number
}

export interface SlidingRound {
  id: string
  roomId: number
  mode: GameMode
  rulesId: 'sliding-v1'
  difficulty: SlidingDifficulty
  size: number
  catId: string
  tiles: Array<number | null>
  moves: number
  score: number
  rngState: number
  status: 'playing' | 'finished'
  lastEvent: SlidingEvent
}
