import type { GameMode } from '../economy'
import { findGeometryRemovalSequence, findMahjongPairs, isMahjongTileFree } from './board'
import { getMahjongLayout } from './layouts'
import { MAHJONG_PAIR_POINTS, mahjongCompletionBonus } from './scoring'
import type { MahjongDifficulty, MahjongRound, MahjongTile } from './types'

const safeSeed = (seed: number): number => (Number.isFinite(seed) ? Math.floor(seed) : Date.now()) >>> 0 || 1

const advanceSeed = (state: number): number => {
  let next = state >>> 0 || 1
  next ^= next << 13
  next ^= next >>> 17
  next ^= next << 5
  return next >>> 0 || 1
}

function assignSolvableCats(tiles: MahjongTile[], catIds: string[], seed: number): MahjongTile[] | null {
  const active = tiles.filter((tile) => !tile.removed)
  const sequence = findGeometryRemovalSequence(active)
  if (!sequence) return null
  const assignments = new Map<number, string>()
  const offset = seed % catIds.length
  sequence.forEach(([first, second], index) => {
    const catId = catIds[(index + offset) % catIds.length]
    assignments.set(first, catId)
    assignments.set(second, catId)
  })
  return tiles.map((tile) => tile.removed ? tile : { ...tile, catId: assignments.get(tile.id) ?? catIds[0] })
}

export function createMahjongRound(roomId: number, mode: GameMode, catIds: string[], difficulty: MahjongDifficulty, seed: number): MahjongRound {
  if (catIds.length !== 5) throw new Error('Mahjong requires exactly five room cats')
  const layout = getMahjongLayout(difficulty)
  const rngState = safeSeed(seed)
  const emptyTiles = layout.slots.map((slot) => ({ ...slot, catId: catIds[0], removed: false }))
  const tiles = assignSolvableCats(emptyTiles, catIds, rngState)
  if (!tiles) throw new Error(`Mahjong layout ${layout.id} is not removable in pairs`)
  return {
    id: `${roomId}-${mode}-${difficulty}-${rngState}`,
    roomId,
    mode,
    rulesId: 'mahjong-v1',
    difficulty,
    layoutId: layout.id,
    tiles,
    selectedId: null,
    hintedIds: [],
    score: 0,
    pairsFound: 0,
    hintsUsed: 0,
    shuffles: 0,
    rngState,
    status: 'playing',
    lastEvent: null,
  }
}

export function selectMahjongTile(round: MahjongRound, tileId: number): MahjongRound {
  if (round.status !== 'playing' || !isMahjongTileFree(round.tiles, tileId)) return round
  const tile = round.tiles.find((item) => item.id === tileId)
  if (!tile || tile.removed) return round
  if (round.selectedId === null) return { ...round, selectedId: tileId, hintedIds: [], lastEvent: 'selected' }
  if (round.selectedId === tileId) return { ...round, selectedId: null, hintedIds: [], lastEvent: null }

  const selected = round.tiles.find((item) => item.id === round.selectedId)
  if (!selected || selected.removed || !isMahjongTileFree(round.tiles, selected.id)) {
    return { ...round, selectedId: tileId, hintedIds: [], lastEvent: 'selected' }
  }
  if (selected.catId !== tile.catId) {
    return { ...round, selectedId: tileId, hintedIds: [], lastEvent: 'mismatch' }
  }

  const tiles = round.tiles.map((item) => item.id === selected.id || item.id === tile.id ? { ...item, removed: true } : item)
  const finished = tiles.every((item) => item.removed)
  const score = round.score + MAHJONG_PAIR_POINTS
    + (finished ? mahjongCompletionBonus(round.hintsUsed, round.shuffles) : 0)
  return {
    ...round,
    tiles,
    selectedId: null,
    hintedIds: [],
    score,
    pairsFound: round.pairsFound + 1,
    status: finished ? 'finished' : 'playing',
    lastEvent: finished ? 'completed' : 'match',
  }
}

export function hintMahjongPair(round: MahjongRound): MahjongRound {
  if (round.status !== 'playing') return round
  const pair = findMahjongPairs(round.tiles)[0]
  if (!pair) return round
  return { ...round, selectedId: null, hintedIds: pair, hintsUsed: round.hintsUsed + 1, lastEvent: 'hint' }
}

export function shuffleMahjong(round: MahjongRound, catIds: string[]): MahjongRound {
  if (round.status !== 'playing' || findMahjongPairs(round.tiles).length > 0) return round
  const rngState = advanceSeed(round.rngState)
  const tiles = assignSolvableCats(round.tiles, catIds, rngState)
  if (!tiles) return round
  return {
    ...round,
    tiles,
    selectedId: null,
    hintedIds: [],
    shuffles: round.shuffles + 1,
    rngState,
    lastEvent: 'shuffled',
  }
}
