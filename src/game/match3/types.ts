import type { GameMode } from '../economy'

export const MATCH3_SIZE = 7
export const MATCH3_MOVES = 20

export interface Match3Tile {
  id: number
  catId: string
  kind: 'normal'
}

export interface Match3Run {
  cells: number[]
  length: number
}

export interface Match3Round {
  id: string
  roomId: number
  mode: GameMode
  rulesId: 'classic-7x7'
  board: Match3Tile[]
  movesLeft: number
  score: number
  maxCombo: number
  rngState: number
  nextTileId: number
  status: 'playing' | 'finished'
  lastGain: number
  lastCombo: number
  shuffled: boolean
}

export interface Match3TurnResult {
  round: Match3Round
  accepted: boolean
}
