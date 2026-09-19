export type CatRarity = 'basic' | 'rare' | 'special'

export interface Cat {
  id: string
  name: string
  rarity: CatRarity
  image: string
  cost: number | null
}

const cat = (id: string, name: string, rarity: CatRarity, number: number, cost: number | null): Cat => ({
  id,
  name,
  rarity,
  image: `/assets/cats/${rarity}/${String(number).padStart(2, '0')}.png`,
  cost,
})

export const cats: Cat[] = [
  cat('ryzhik', 'Рыжик', 'basic', 1, 0),
  cat('ugolyok', 'Уголёк', 'basic', 2, 35),
  cat('poloska', 'Полоска', 'basic', 3, 130),
  cat('snezhka', 'Снежка', 'basic', 4, 400),
  cat('iris', 'Ирис', 'basic', 5, 950),
  cat('kaliko', 'Калико', 'rare', 1, null),
  cat('tuchka', 'Тучка', 'rare', 2, null),
  cat('pingvin', 'Пингвин', 'rare', 3, null),
  cat('persik', 'Персик', 'rare', 4, null),
  cat('luna', 'Луна', 'rare', 5, null),
  cat('groza', 'Гроза', 'special', 1, null),
  cat('ponchik', 'Пончик', 'special', 2, null),
  cat('iskra', 'Искра', 'special', 3, null),
  cat('oblachko', 'Облачко', 'special', 4, null),
  cat('noch', 'Ночь', 'special', 5, null),
]

export const basicCats = cats.filter((item) => item.rarity === 'basic')
export const initialCatId = basicCats[0].id
