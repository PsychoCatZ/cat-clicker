import { rooms } from './rooms'

export interface Cat {
  id: string
  name: string
  roomId: number
  image: string
  sleepingImage: string
  baseCost: number
}

const names = [
  ['Рыжик', 'Уголёк', 'Полоска', 'Снежка', 'Ирис'],
  ['Калико', 'Тучка', 'Пингвин', 'Персик', 'Луна'],
  ['Гроза', 'Пончик', 'Искра', 'Облачко', 'Ночь'],
  ['Тень', 'Паутинка', 'Броня', 'Щит', 'Плащ'],
  ['Дип', 'Кью', 'Джем', 'Клод', 'Чатти'],
]

const costs = [0, 150, 1800, 18000, 180000]

export const cats: Cat[] = rooms.flatMap((room) => names[room.id - 1].map((name, index) => {
  const number = String(index + 1).padStart(2, '0')
  const folder = `/assets/cats/${room.catGroup}`
  return {
    id: `${room.catGroup}-${number}`,
    name,
    roomId: room.id,
    image: `${folder}/${number}.png`,
    sleepingImage: `${folder}/sleep/${number}.png`,
    baseCost: costs[index],
  }
}))

export const catsForRoom = (roomId: number): Cat[] => cats.filter((cat) => cat.roomId === roomId)
export const firstCatForRoom = (roomId: number): Cat => catsForRoom(roomId)[0]
