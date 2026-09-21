import type { SlidingConfig, SlidingDifficulty } from './types'

export const slidingConfigs: Record<SlidingDifficulty, SlidingConfig> = {
  easy: { id: 'easy', name: 'Лёгкий', size: 3, shuffleMoves: 72 },
  normal: { id: 'normal', name: 'Обычный', size: 4, shuffleMoves: 144 },
  hard: { id: 'hard', name: 'Сложный', size: 5, shuffleMoves: 240 },
}

export const slidingDifficulties: SlidingDifficulty[] = ['easy', 'normal', 'hard']

export const getSlidingConfig = (difficulty: SlidingDifficulty): SlidingConfig => slidingConfigs[difficulty]
