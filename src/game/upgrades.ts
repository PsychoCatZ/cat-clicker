export type UpgradeKind = 'click' | 'second'

export interface Upgrade {
  id: string
  name: string
  description: string
  image: string
  kind: UpgradeKind
  bonus: number
  baseCost: number
  growth: number
}

export const upgrades: Upgrade[] = [
  { id: 'bowl', name: 'Миска с рыбой', description: '+1 рыбка за клик', image: '/assets/upgrades/basic/01.png', kind: 'click', bonus: 1, baseCost: 12, growth: 1.6 },
  { id: 'bed', name: 'Мягкая лежанка', description: '+1 рыбка в секунду', image: '/assets/upgrades/basic/02.png', kind: 'second', bonus: 1, baseCost: 30, growth: 1.7 },
  { id: 'scratcher', name: 'Когтеточка', description: '+3 рыбки в секунду', image: '/assets/upgrades/basic/03.png', kind: 'second', bonus: 3, baseCost: 65, growth: 1.75 },
  { id: 'mouse', name: 'Игрушечная мышь', description: '+2 рыбки за клик', image: '/assets/upgrades/basic/04.png', kind: 'click', bonus: 2, baseCost: 20, growth: 1.65 },
  { id: 'yarn', name: 'Пушистый клубок', description: '+2 рыбки в секунду', image: '/assets/upgrades/basic/05.png', kind: 'second', bonus: 2, baseCost: 42, growth: 1.7 },
]
