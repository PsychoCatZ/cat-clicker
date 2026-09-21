import type { GameMode } from '../economy'
import { isSlidingBoardSolved, moveSlidingTileOnBoard, shuffleSlidingBoard } from './board'
import { getSlidingConfig } from './layouts'
import { slidingCompletionScore } from './scoring'
import type { SlidingDifficulty, SlidingRound } from './types'

const safeSeed = (seed: number): number => (Number.isFinite(seed) ? Math.floor(seed) : Date.now()) >>> 0 || 1

const advanceSeed = (state: number): number => {
  let next = state >>> 0 || 1
  next ^= next << 13
  next ^= next >>> 17
  next ^= next << 5
  return next >>> 0 || 1
}

export function createSlidingRound(
  roomId: number,
  mode: GameMode,
  catIds: string[],
  difficulty: SlidingDifficulty,
  seed: number,
  requestedCatId?: string,
): SlidingRound {
  if (catIds.length !== 5) throw new Error('Sliding puzzle requires exactly five room cats')
  const config = getSlidingConfig(difficulty)
  const initialSeed = safeSeed(seed)
  const catId = requestedCatId && catIds.includes(requestedCatId)
    ? requestedCatId
    : catIds[initialSeed % catIds.length]
  const shuffled = shuffleSlidingBoard(config.size, initialSeed, config.shuffleMoves)
  return {
    id: `${roomId}-${mode}-${difficulty}-${catId}-${initialSeed}`,
    roomId,
    mode,
    rulesId: 'sliding-v1',
    difficulty,
    size: config.size,
    catId,
    tiles: shuffled.tiles,
    moves: 0,
    score: 0,
    rngState: shuffled.rngState,
    status: 'playing',
    lastEvent: 'shuffled',
  }
}

export function moveSlidingTile(round: SlidingRound, tileId: number): SlidingRound {
  if (round.status !== 'playing') return round
  const tiles = moveSlidingTileOnBoard(round.tiles, round.size, tileId)
  if (!tiles) return round
  const moves = round.moves + 1
  const finished = isSlidingBoardSolved(tiles)
  return {
    ...round,
    tiles,
    moves,
    score: finished ? slidingCompletionScore(round.difficulty, moves) : 0,
    status: finished ? 'finished' : 'playing',
    lastEvent: finished ? 'completed' : 'moved',
  }
}

export function reshuffleSlidingRound(round: SlidingRound): SlidingRound {
  if (round.status !== 'playing') return round
  const config = getSlidingConfig(round.difficulty)
  const seed = advanceSeed(round.rngState)
  const shuffled = shuffleSlidingBoard(config.size, seed, config.shuffleMoves)
  return {
    ...round,
    id: `${round.roomId}-${round.mode}-${round.difficulty}-${round.catId}-${seed}`,
    tiles: shuffled.tiles,
    moves: 0,
    score: 0,
    rngState: shuffled.rngState,
    lastEvent: 'shuffled',
  }
}
