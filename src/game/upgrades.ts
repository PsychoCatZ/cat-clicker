export type UpgradeTier = 'basic' | 'advanced'

export interface ScenePlacement {
  x: number
  y: number
  width: number
  height: number
  mobileX?: number
  mobileY?: number
  mobileHeight?: number
}

export interface Upgrade {
  id: string
  roomId: number
  tier: UpgradeTier
  slot: number
  name: string
  image: string
  income: number
  baseCost: number
  placement: ScenePlacement
}

const names: { basic: string[]; advanced: string[] }[] = [
  { basic: ['Миска с рыбой', 'Мягкая лежанка', 'Когтеточка', 'Игрушечная мышь', 'Клубок'], advanced: ['Домик', 'Окно с птицами', 'Автокормушка', 'Фонтанчик', 'Игровой комплекс'] },
  { basic: ['Туннель', 'Настенные полки', 'Кошачья трава', 'Гамак', 'Щётка'], advanced: ['Лазерная игрушка', 'Робомышь', 'Аквариум', 'Массажная арка', 'Подвесной мост'] },
  { basic: ['Тёплая лежанка', 'Замок', 'Головоломка', 'Когтеточка-дерево', 'Кошачий телевизор'], advanced: ['Голограмма рыбок', 'Игровая стена', 'Капсула сна', 'Умная кормушка', 'Умный домик'] },
  { basic: ['Проектор', 'Шкаф костюмов', 'Тренажёр', 'Сундук героя', 'Зал славы'], advanced: ['Карта города', 'Энергостанция', 'Медкапсула', 'Портал', 'Витрина наград'] },
  { basic: ['Капсула уюта', 'Голографическая когтеточка', 'Автокормушка', 'Пульт управления', 'Сервер'], advanced: ['Станция спасения', 'Аналитический стол', 'Умное кресло', 'Капсула будущего', 'Центр Котификации'] },
]

const basicCosts = [45, 180, 700, 2600, 9000]
const advancedCosts = [18000, 42000, 95000, 220000, 500000]
const basicIncome = [1, 2, 5, 12, 30]
const advancedIncome = [50, 90, 160, 280, 480]

// Percentages of the scene. Each tier has its own positions because a wall
// fixture and the floor item it replaces need different places in the room.
const place = (x: number, y: number, width: number, height: number, mobileX?: number, mobileY?: number, mobileHeight?: number): ScenePlacement =>
  ({ x, y, width, height, mobileX, mobileY, mobileHeight })

const placements: { basic: ScenePlacement[]; advanced: ScenePlacement[] }[] = [
  {
    basic: [
      place(17, 83, 17, 22), place(78, 82, 23, 25), place(72, 63, 18, 41),
      place(28, 70, 15, 16), place(84, 69, 15, 19),
    ],
    advanced: [
      place(17, 76, 25, 39, 19, 79), place(27, 38, 25, 37, 24, 32), place(85, 83, 19, 30, 81, 89, 21),
      place(68, 82, 19, 25, 81, 69, 22), place(76, 54, 27, 39, 80, 44),
    ],
  },
  {
    basic: [
      place(18, 82, 26, 27), place(73, 39, 27, 37, 76, 31), place(82, 80, 19, 25),
      place(27, 47, 26, 27, 24, 36), place(77, 66, 19, 29),
    ],
    advanced: [
      place(20, 71, 19, 30), place(30, 83, 17, 18), place(72, 39, 27, 35, 77, 31),
      place(79, 73, 22, 35), place(29, 33, 29, 29, 27, 26),
    ],
  },
  {
    basic: [
      place(17, 84, 22, 25), place(77, 68, 28, 47), place(82, 84, 20, 21),
      place(29, 62, 20, 40), place(27, 38, 25, 35, 26, 31),
    ],
    advanced: [
      place(24, 48, 20, 34, 23, 39), place(73, 38, 28, 42, 76, 31), place(17, 82, 24, 35),
      place(83, 84, 20, 27), place(72, 66, 26, 42),
    ],
  },
  {
    basic: [
      place(31, 56, 18, 38, 24, 49), place(18, 67, 23, 46), place(79, 67, 23, 43),
      place(22, 86, 21, 23), place(73, 36, 25, 35, 76, 30),
    ],
    advanced: [
      place(26, 59, 27, 34), place(80, 54, 26, 42, 80, 45), place(18, 81, 25, 39),
      place(78, 82, 25, 39), place(70, 32, 26, 38, 75, 27),
    ],
  },
  {
    basic: [
      place(18, 82, 24, 35), place(29, 52, 20, 39, 25, 44), place(83, 83, 20, 33),
      place(72, 43, 26, 34, 76, 34), place(79, 69, 23, 42),
    ],
    advanced: [
      place(18, 76, 26, 44), place(27, 45, 28, 37, 25, 36), place(79, 82, 22, 34),
      place(79, 57, 24, 42, 79, 49), place(69, 34, 28, 40, 75, 27),
    ],
  },
]

const assetNumber = (roomIndex: number, tier: UpgradeTier, index: number): number =>
  roomIndex === 1 && tier === 'basic' ? [1, 2, 4, 3, 5][index] : index + 1

export const upgrades: Upgrade[] = names.flatMap((room, roomIndex) =>
  (['basic', 'advanced'] as const).flatMap((tier) => room[tier].map((name, index) => ({
    id: `room-${roomIndex + 1}-${tier}-${index + 1}`,
    roomId: roomIndex + 1,
    tier,
    slot: index,
    name,
    image: `/assets/upgrades/room-${roomIndex + 1}/${tier}/${String(assetNumber(roomIndex, tier, index)).padStart(2, '0')}.png`,
    income: (tier === 'basic' ? basicIncome : advancedIncome)[index],
    baseCost: (tier === 'basic' ? basicCosts : advancedCosts)[index],
    placement: placements[roomIndex][tier][index],
  }))),
)

export const upgradesForRoom = (roomId: number): Upgrade[] => upgrades.filter((item) => item.roomId === roomId)
