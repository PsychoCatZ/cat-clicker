import type { GameMode } from '../economy'

export const PAIRS_CARD_COUNTS = [10, 16, 20] as const
export type PairsCardCount = typeof PAIRS_CARD_COUNTS[number]
export type PairsRulesId = `pairs-${PairsCardCount}`

export interface PairCard {
  id: number
  catId: string
  matched: boolean
}

export interface PairsRound {
  id: string
  roomId: number
  mode: GameMode
  rulesId: PairsRulesId
  cardCount: PairsCardCount
  cards: PairCard[]
  revealed: number[]
  attempts: number
  matches: number
  status: 'playing' | 'finished'
  lastMatch: boolean | null
  rngState: number
}
