import { scoreWave } from './scoring'
import { MATCH3_SIZE, type Match3Run, type Match3Tile } from './types'

type NullableBoard = Array<Match3Tile | null>

interface RandomValue {
  state: number
  value: number
}

interface GeneratedBoard {
  board: Match3Tile[]
  rngState: number
  nextTileId: number
}

export interface ResolvedBoard extends GeneratedBoard {
  gainedScore: number
  cascades: number
  shuffled: boolean
}

const nextRandom = (current: number): RandomValue => {
  let state = current >>> 0 || 0x6d2b79f5
  state ^= state << 13
  state ^= state >>> 17
  state ^= state << 5
  state >>>= 0
  return { state, value: state / 0x100000000 }
}

const choose = (values: string[], rngState: number): { value: string; state: number } => {
  const random = nextRandom(rngState)
  return { value: values[Math.floor(random.value * values.length)], state: random.state }
}

const sameCat = (a: Match3Tile | null | undefined, b: Match3Tile | null | undefined): boolean =>
  Boolean(a && b && a.catId === b.catId)

export const areAdjacent = (first: number, second: number): boolean => {
  if (first < 0 || second < 0 || first >= MATCH3_SIZE ** 2 || second >= MATCH3_SIZE ** 2) return false
  const rowA = Math.floor(first / MATCH3_SIZE)
  const rowB = Math.floor(second / MATCH3_SIZE)
  const colA = first % MATCH3_SIZE
  const colB = second % MATCH3_SIZE
  return Math.abs(rowA - rowB) + Math.abs(colA - colB) === 1
}

export const swapBoardTiles = (board: Match3Tile[], first: number, second: number): Match3Tile[] => {
  const swapped = [...board]
  ;[swapped[first], swapped[second]] = [swapped[second], swapped[first]]
  return swapped
}

export const findMatchRuns = (board: Array<Match3Tile | null>): Match3Run[] => {
  const runs: Match3Run[] = []
  for (let row = 0; row < MATCH3_SIZE; row += 1) {
    let start = 0
    while (start < MATCH3_SIZE) {
      const first = row * MATCH3_SIZE + start
      if (!board[first]) { start += 1; continue }
      let end = start + 1
      while (end < MATCH3_SIZE && sameCat(board[first], board[row * MATCH3_SIZE + end])) end += 1
      if (end - start >= 3) runs.push({ length: end - start, cells: Array.from({ length: end - start }, (_, offset) => first + offset) })
      start = end
    }
  }
  for (let col = 0; col < MATCH3_SIZE; col += 1) {
    let start = 0
    while (start < MATCH3_SIZE) {
      const first = start * MATCH3_SIZE + col
      if (!board[first]) { start += 1; continue }
      let end = start + 1
      while (end < MATCH3_SIZE && sameCat(board[first], board[end * MATCH3_SIZE + col])) end += 1
      if (end - start >= 3) runs.push({
        length: end - start,
        cells: Array.from({ length: end - start }, (_, offset) => (start + offset) * MATCH3_SIZE + col),
      })
      start = end
    }
  }
  return runs
}

export const swapCreatesMatch = (board: Match3Tile[], first: number, second: number): boolean =>
  areAdjacent(first, second) && findMatchRuns(swapBoardTiles(board, first, second)).length > 0

export const findPossibleSwap = (board: Match3Tile[]): [number, number] | null => {
  for (let index = 0; index < board.length; index += 1) {
    const col = index % MATCH3_SIZE
    if (col < MATCH3_SIZE - 1 && swapCreatesMatch(board, index, index + 1)) return [index, index + 1]
    if (index + MATCH3_SIZE < board.length && swapCreatesMatch(board, index, index + MATCH3_SIZE)) return [index, index + MATCH3_SIZE]
  }
  return null
}

const generateCandidate = (catIds: string[], initialState: number, firstTileId: number): GeneratedBoard => {
  const board: Match3Tile[] = []
  let rngState = initialState
  let nextTileId = firstTileId
  for (let index = 0; index < MATCH3_SIZE ** 2; index += 1) {
    const row = Math.floor(index / MATCH3_SIZE)
    const col = index % MATCH3_SIZE
    const allowed = catIds.filter((catId) => {
      const horizontal = col >= 2 && board[index - 1]?.catId === catId && board[index - 2]?.catId === catId
      const vertical = row >= 2 && board[index - MATCH3_SIZE]?.catId === catId && board[index - MATCH3_SIZE * 2]?.catId === catId
      return !horizontal && !vertical
    })
    const selected = choose(allowed, rngState)
    rngState = selected.state
    board.push({ id: nextTileId, catId: selected.value, kind: 'normal' })
    nextTileId += 1
  }
  return { board, rngState, nextTileId }
}

export const generateStableBoard = (catIds: string[], initialState: number, firstTileId = 1): GeneratedBoard => {
  if (catIds.length < 3) throw new Error('Match-3 needs at least three tile types')
  let rngState = initialState
  let nextTileId = firstTileId
  for (let attempt = 0; attempt < 300; attempt += 1) {
    const candidate = generateCandidate(catIds, rngState, nextTileId)
    rngState = candidate.rngState
    nextTileId = candidate.nextTileId
    if (findPossibleSwap(candidate.board)) return candidate
  }
  throw new Error('Could not create a playable match-3 board')
}

const collapseAndFill = (board: NullableBoard, catIds: string[], initialState: number, firstTileId: number): GeneratedBoard => {
  const collapsed: NullableBoard = Array(MATCH3_SIZE ** 2).fill(null)
  let rngState = initialState
  let nextTileId = firstTileId
  for (let col = 0; col < MATCH3_SIZE; col += 1) {
    const remaining: Match3Tile[] = []
    for (let row = MATCH3_SIZE - 1; row >= 0; row -= 1) {
      const tile = board[row * MATCH3_SIZE + col]
      if (tile) remaining.push(tile)
    }
    let row = MATCH3_SIZE - 1
    for (const tile of remaining) {
      collapsed[row * MATCH3_SIZE + col] = tile
      row -= 1
    }
    while (row >= 0) {
      const selected = choose(catIds, rngState)
      rngState = selected.state
      collapsed[row * MATCH3_SIZE + col] = { id: nextTileId, catId: selected.value, kind: 'normal' }
      nextTileId += 1
      row -= 1
    }
  }
  return { board: collapsed as Match3Tile[], rngState, nextTileId }
}

const shuffleStable = (board: Match3Tile[], catIds: string[], initialState: number, nextTileId: number): GeneratedBoard => {
  let rngState = initialState
  for (let attempt = 0; attempt < 300; attempt += 1) {
    const shuffled = [...board]
    for (let index = shuffled.length - 1; index > 0; index -= 1) {
      const random = nextRandom(rngState)
      rngState = random.state
      const target = Math.floor(random.value * (index + 1))
      ;[shuffled[index], shuffled[target]] = [shuffled[target], shuffled[index]]
    }
    if (findMatchRuns(shuffled).length === 0 && findPossibleSwap(shuffled)) return { board: shuffled, rngState, nextTileId }
  }
  return generateStableBoard(catIds, rngState, nextTileId)
}

export const resolveBoard = (
  swappedBoard: Match3Tile[], catIds: string[], initialState: number, firstTileId: number,
): ResolvedBoard => {
  let board = swappedBoard
  let rngState = initialState
  let nextTileId = firstTileId
  let gainedScore = 0
  let cascades = 0
  for (let depth = 1; depth <= 50; depth += 1) {
    const runs = findMatchRuns(board)
    if (runs.length === 0) break
    cascades = depth
    gainedScore += scoreWave(runs, depth)
    const cleared: NullableBoard = [...board]
    for (const cell of new Set(runs.flatMap((run) => run.cells))) cleared[cell] = null
    const filled = collapseAndFill(cleared, catIds, rngState, nextTileId)
    board = filled.board
    rngState = filled.rngState
    nextTileId = filled.nextTileId
  }
  if (findMatchRuns(board).length > 0) {
    const regenerated = generateStableBoard(catIds, rngState, nextTileId)
    board = regenerated.board
    rngState = regenerated.rngState
    nextTileId = regenerated.nextTileId
  }
  const needsShuffle = !findPossibleSwap(board)
  if (needsShuffle) {
    const shuffled = shuffleStable(board, catIds, rngState, nextTileId)
    board = shuffled.board
    rngState = shuffled.rngState
    nextTileId = shuffled.nextTileId
  }
  return { board, rngState, nextTileId, gainedScore, cascades, shuffled: needsShuffle }
}
