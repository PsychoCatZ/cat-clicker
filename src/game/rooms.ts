export interface Room {
  id: number
  name: string
  catGroup: 'basic' | 'rare' | 'special' | 'superhero' | 'ai'
  day: string
  night: string
}

export const rooms: Room[] = [
  { id: 1, name: 'Комната', catGroup: 'basic', day: '/assets/backgrounds/room-1-day.png', night: '/assets/backgrounds/room-1-night.png' },
  { id: 2, name: 'Уютный приют', catGroup: 'rare', day: '/assets/backgrounds/room-2-day.png', night: '/assets/backgrounds/room-2-night.png' },
  { id: 3, name: 'Котодом', catGroup: 'special', day: '/assets/backgrounds/room-3-day.png', night: '/assets/backgrounds/room-3-night.png' },
  { id: 4, name: 'Котоцентр', catGroup: 'superhero', day: '/assets/backgrounds/room-4-day.png', night: '/assets/backgrounds/room-4-night.png' },
  { id: 5, name: 'Штаб Котификации', catGroup: 'ai', day: '/assets/backgrounds/room-5-day.png', night: '/assets/backgrounds/room-5-night.png' },
]
