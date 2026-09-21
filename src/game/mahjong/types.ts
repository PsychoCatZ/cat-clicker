import type { GameMode } from '../economy'

export type MahjongDifficulty = 'easy' | 'normal' | 'hard'
export type MahjongEvent = 'selected' | 'mismatch' | 'match' | 'hint' | 'shuffled' | 'completed' | null

export interface MahjongSlot {
  id: number
  x: number
  y: number
  z: number
}

export interface MahjongLayout {
  id: MahjongDifficulty
  name: string
  tileCount: number
  width: number
  height: number
  slots: MahjongSlot[]
}

export interface MahjongTile extends MahjongSlot {
  catId: string
  removed: boolean
}

export interface MahjongRound {
  id: string
  roomId: number
  mode: GameMode
  rulesId: 'mahjong-v1'
  difficulty: MahjongDifficulty
  layoutId: MahjongDifficulty
  tiles: MahjongTile[]
  selectedId: number | null
  hintedIds: number[]
  score: number
  pairsFound: number
  hintsUsed: number
  shuffles: number
  rngState: number
  status: 'playing' | 'finished'
  lastEvent: MahjongEvent
}
