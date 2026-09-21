import { areAdjacent, generateStableBoard, resolveBoard, swapBoardTiles, swapCreatesMatch } from './board'
import { MATCH3_MOVES, type Match3Round, type Match3TurnResult } from './types'
import type { GameMode } from '../economy'

const safeSeed = (seed: number): number => (Number.isFinite(seed) ? Math.floor(seed) : Date.now()) >>> 0 || 1

export const createMatch3Round = (roomId: number, mode: GameMode, catIds: string[], seed: number): Match3Round => {
  const rngState = safeSeed(seed)
  const generated = generateStableBoard(catIds, rngState)
  return {
    id: `${roomId}-${mode}-${rngState}`,
    roomId,
    mode,
    rulesId: 'classic-7x7',
    board: generated.board,
    movesLeft: MATCH3_MOVES,
    score: 0,
    maxCombo: 0,
    rngState: generated.rngState,
    nextTileId: generated.nextTileId,
    status: 'playing',
    lastGain: 0,
    lastCombo: 0,
    shuffled: false,
  }
}

export const playMatch3Turn = (round: Match3Round, first: number, second: number, catIds: string[]): Match3TurnResult => {
  if (round.status !== 'playing' || !areAdjacent(first, second) || !swapCreatesMatch(round.board, first, second)) {
    return { round, accepted: false }
  }
  const resolved = resolveBoard(swapBoardTiles(round.board, first, second), catIds, round.rngState, round.nextTileId)
  const movesLeft = Math.max(0, round.movesLeft - 1)
  return {
    accepted: true,
    round: {
      ...round,
      board: resolved.board,
      movesLeft,
      score: round.score + resolved.gainedScore,
      maxCombo: Math.max(round.maxCombo, resolved.cascades),
      rngState: resolved.rngState,
      nextTileId: resolved.nextTileId,
      status: movesLeft === 0 ? 'finished' : 'playing',
      lastGain: resolved.gainedScore,
      lastCombo: resolved.cascades,
      shuffled: resolved.shuffled,
    },
  }
}
