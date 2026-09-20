export interface ClickResource {
  id: string
  name: string
  image: string
  bonus: number
  baseCost: number
  growth: number
}

export interface Food {
  id: string
  name: string
  image: string
  restore: number
  baseCost: number
  boostSeconds?: number
}

export const resources: ClickResource[] = [
  { id: 'fish', name: 'Рыбка', image: '/assets/resources/01.png', bonus: 1, baseCost: 15, growth: 1.85 },
  { id: 'school', name: 'Стайка рыб', image: '/assets/resources/02.png', bonus: 4, baseCost: 500, growth: 2 },
  { id: 'golden', name: 'Золотая рыбка', image: '/assets/resources/03.png', bonus: 12, baseCost: 6000, growth: 2.1 },
  { id: 'coin', name: 'Лапомонета', image: '/assets/resources/04.png', bonus: 35, baseCost: 60000, growth: 2.2 },
  { id: 'gift', name: 'Подарок', image: '/assets/resources/05.png', bonus: 100, baseCost: 250000, growth: 2.3 },
]

export const foods: Food[] = [
  { id: 'mouse', name: 'Мышка', image: '/assets/food/01.png', restore: 25, baseCost: 60 },
  { id: 'dry', name: 'Сухой корм', image: '/assets/food/02.png', restore: 50, baseCost: 160 },
  { id: 'wet', name: 'Влажный корм', image: '/assets/food/03.png', restore: 100, baseCost: 360 },
  { id: 'caviar', name: 'Икра', image: '/assets/food/04.png', restore: 100, baseCost: 750, boostSeconds: 60 },
]
