import type { MahjongSlot, MahjongTile } from './types'

type BoardTile = MahjongSlot & { removed?: boolean; catId?: string }

const activeTiles = <T extends BoardTile>(tiles: T[]): T[] => tiles.filter((tile) => !tile.removed)
const overlaps = (a: MahjongSlot, b: MahjongSlot): boolean =>
  a.x < b.x + 2 && a.x + 2 > b.x && a.y < b.y + 2 && a.y + 2 > b.y
const verticallyOverlaps = (a: MahjongSlot, b: MahjongSlot): boolean => a.y < b.y + 2 && a.y + 2 > b.y

export function isMahjongTileFree<T extends BoardTile>(tiles: T[], tileId: number): boolean {
  const tile = tiles.find((item) => item.id === tileId)
  if (!tile || tile.removed) return false
  const active = activeTiles(tiles)
  const covered = active.some((other) => other.id !== tile.id && other.z > tile.z && overlaps(tile, other))
  if (covered) return false
  const leftBlocked = active.some((other) => other.id !== tile.id && other.z === tile.z
    && other.x + 2 === tile.x && verticallyOverlaps(tile, other))
  const rightBlocked = active.some((other) => other.id !== tile.id && other.z === tile.z
    && tile.x + 2 === other.x && verticallyOverlaps(tile, other))
  return !leftBlocked || !rightBlocked
}

export const freeMahjongTiles = <T extends BoardTile>(tiles: T[]): T[] =>
  activeTiles(tiles).filter((tile) => isMahjongTileFree(tiles, tile.id))

export function findMahjongPairs(tiles: MahjongTile[]): Array<[number, number]> {
  const free = freeMahjongTiles(tiles)
  const pairs: Array<[number, number]> = []
  for (let first = 0; first < free.length; first += 1) {
    for (let second = first + 1; second < free.length; second += 1) {
      if (free[first].catId === free[second].catId) pairs.push([free[first].id, free[second].id])
    }
  }
  return pairs
}

export function findGeometryRemovalSequence(slots: MahjongSlot[]): Array<[number, number]> | null {
  const working = slots.map((slot) => ({ ...slot, removed: false }))
  const sequence: Array<[number, number]> = []
  while (working.some((tile) => !tile.removed)) {
    const free = freeMahjongTiles(working).sort((a, b) => b.z - a.z || a.y - b.y || a.x - b.x)
    if (free.length < 2) return null
    const first = free[0]
    const second = free[free.length - 1]
    working.find((tile) => tile.id === first.id)!.removed = true
    working.find((tile) => tile.id === second.id)!.removed = true
    sequence.push([first.id, second.id])
  }
  return sequence
}
