const nextRandom = (state: number): [number, number] => {
  let next = state >>> 0 || 1
  next ^= next << 13
  next ^= next >>> 17
  next ^= next << 5
  return [(next >>> 0) / 0x100000000, next >>> 0 || 1]
}

export const createSolvedSlidingBoard = (size: number): Array<number | null> => [
  ...Array.from({ length: size * size - 1 }, (_, index) => index),
  null,
]

export const isSlidingBoardSolved = (tiles: Array<number | null>): boolean =>
  tiles.every((tile, index) => index === tiles.length - 1 ? tile === null : tile === index)

export function slidingNeighborIndices(index: number, size: number): number[] {
  const row = Math.floor(index / size)
  const column = index % size
  return [
    row > 0 ? index - size : -1,
    row < size - 1 ? index + size : -1,
    column > 0 ? index - 1 : -1,
    column < size - 1 ? index + 1 : -1,
  ].filter((value) => value >= 0)
}

export const movableSlidingTileIds = (tiles: Array<number | null>, size: number): number[] => {
  const emptyIndex = tiles.indexOf(null)
  if (emptyIndex < 0) return []
  return slidingNeighborIndices(emptyIndex, size)
    .map((index) => tiles[index])
    .filter((tile): tile is number => tile !== null)
}

export function moveSlidingTileOnBoard(tiles: Array<number | null>, size: number, tileId: number): Array<number | null> | null {
  const emptyIndex = tiles.indexOf(null)
  const tileIndex = tiles.indexOf(tileId)
  if (emptyIndex < 0 || tileIndex < 0 || !slidingNeighborIndices(emptyIndex, size).includes(tileIndex)) return null
  const next = [...tiles]
  next[emptyIndex] = tileId
  next[tileIndex] = null
  return next
}

export function isSlidingBoardSolvable(tiles: Array<number | null>, size: number): boolean {
  if (tiles.length !== size * size || tiles.filter((tile) => tile === null).length !== 1) return false
  const numbered = tiles.filter((tile): tile is number => tile !== null)
  if (numbered.length !== size * size - 1
    || new Set(numbered).size !== numbered.length
    || numbered.some((tile) => !Number.isInteger(tile) || tile < 0 || tile >= size * size - 1)) return false
  let inversions = 0
  for (let first = 0; first < numbered.length; first += 1) {
    for (let second = first + 1; second < numbered.length; second += 1) {
      if (numbered[first] > numbered[second]) inversions += 1
    }
  }
  if (size % 2 === 1) return inversions % 2 === 0
  const emptyRowFromBottom = size - Math.floor(tiles.indexOf(null) / size)
  return (inversions + emptyRowFromBottom) % 2 === 1
}

export function shuffleSlidingBoard(size: number, seed: number, moveCount: number): {
  tiles: Array<number | null>
  rngState: number
  history: number[]
} {
  let tiles = createSolvedSlidingBoard(size)
  let rngState = seed >>> 0 || 1
  let previousEmpty = -1
  const history: number[] = []
  for (let move = 0; move < moveCount; move += 1) {
    const emptyIndex = tiles.indexOf(null)
    const neighbors = slidingNeighborIndices(emptyIndex, size)
    const choices = neighbors.filter((index) => index !== previousEmpty)
    const candidates = choices.length > 0 ? choices : neighbors
    const [random, nextState] = nextRandom(rngState)
    rngState = nextState
    const tileIndex = candidates[Math.floor(random * candidates.length)]
    const tileId = tiles[tileIndex]
    if (tileId === null) continue
    const next = moveSlidingTileOnBoard(tiles, size, tileId)
    if (!next) continue
    previousEmpty = emptyIndex
    tiles = next
    history.push(tileId)
  }
  if (isSlidingBoardSolved(tiles)) {
    const tileId = movableSlidingTileIds(tiles, size)[0]
    tiles = moveSlidingTileOnBoard(tiles, size, tileId) ?? tiles
    history.push(tileId)
  }
  return { tiles, rngState, history }
}
