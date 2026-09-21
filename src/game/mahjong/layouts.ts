import type { MahjongDifficulty, MahjongLayout, MahjongSlot } from './types'

function grid(cols: number, rows: number, startX: number, startY: number, z: number, firstId: number): MahjongSlot[] {
  return Array.from({ length: cols * rows }, (_, index) => ({
    id: firstId + index,
    x: startX + (index % cols) * 2,
    y: startY + Math.floor(index / cols) * 2,
    z,
  }))
}

const easySlots = [
  ...grid(4, 4, 0, 0, 0, 0),
  ...grid(4, 2, 0, 2, 1, 16),
]

const normalSlots = [
  ...grid(5, 4, 0, 0, 0, 0),
  ...grid(4, 4, 1, 0, 1, 20),
  ...grid(2, 2, 3, 2, 2, 36),
]

const hardSlots = [
  ...grid(6, 5, 0, 0, 0, 0),
  ...grid(5, 4, 1, 1, 1, 30),
  ...grid(3, 2, 3, 3, 2, 50),
]

export const mahjongLayouts: Record<MahjongDifficulty, MahjongLayout> = {
  easy: { id: 'easy', name: 'Лёгкий', tileCount: 24, width: 8, height: 8, slots: easySlots },
  normal: { id: 'normal', name: 'Обычный', tileCount: 40, width: 10, height: 8, slots: normalSlots },
  hard: { id: 'hard', name: 'Сложный', tileCount: 56, width: 12, height: 10, slots: hardSlots },
}

export const mahjongDifficulties: MahjongDifficulty[] = ['easy', 'normal', 'hard']

export const getMahjongLayout = (difficulty: MahjongDifficulty): MahjongLayout => mahjongLayouts[difficulty]
