import { useState } from 'react'
import { catsForRoom } from '../game/cats'
import { getMahjongLayout, mahjongDifficulties } from '../game/mahjong/layouts'
import type { MahjongDifficulty } from '../game/mahjong/types'
import { PAIRS_CARD_COUNTS, type PairsCardCount } from '../game/pairs/types'
import type { Room } from '../game/rooms'
import { getSlidingConfig, slidingDifficulties } from '../game/sliding/layouts'
import type { SlidingDifficulty } from '../game/sliding/types'

interface Props {
  room: Room
  selectedCatId: string
  onStartMatch3: () => void
  onStartPairs: (cardCount: PairsCardCount) => void
  onStartMahjong: (difficulty: MahjongDifficulty) => void
  onStartSliding: (difficulty: SlidingDifficulty, catId: string) => void
}

export function MiniGamesHub({ room, selectedCatId, onStartMatch3, onStartPairs, onStartMahjong, onStartSliding }: Props) {
  const roomCats = catsForRoom(room.id)
  const [pairsCards, setPairsCards] = useState<PairsCardCount>(10)
  const [mahjongDifficulty, setMahjongDifficulty] = useState<MahjongDifficulty>('normal')
  const [slidingDifficulty, setSlidingDifficulty] = useState<SlidingDifficulty>('easy')
  const [slidingCatId, setSlidingCatId] = useState(selectedCatId)
  const mahjongLayout = getMahjongLayout(mahjongDifficulty)
  const slidingConfig = getSlidingConfig(slidingDifficulty)
  const slidingCat = roomCats.find((cat) => cat.id === slidingCatId)
    ?? roomCats.find((cat) => cat.id === selectedCatId) ?? roomCats[0]
  return <section className="minigames-hub content-section" aria-labelledby="minigames-title">
    <div className="section-heading">
      <div><span className="eyebrow">Спокойные игры · {room.name}</span><h2 id="minigames-title">Мини-игры</h2></div>
      <p>Без таймера и без влияния на открытие комнат</p>
    </div>
    <div className="minigames-grid">
      <article className="minigame-card">
        <div className="minigame-preview match3-preview" aria-hidden="true">
          {roomCats.slice(0, 4).map((cat) => <img key={cat.id} src={cat.image} alt="" />)}
        </div>
        <span className="eyebrow">20 ходов</span>
        <h3>Котики в ряд</h3>
        <p>Меняйте соседних котиков местами и собирайте линии из трёх и больше.</p>
        <button type="button" className="match3-primary" onClick={onStartMatch3}>Играть в «три в ряд»</button>
      </article>
      <article className="minigame-card">
        <div className="minigame-preview pairs-preview" aria-hidden="true">
          {roomCats.slice(0, 3).map((cat) => <div key={cat.id}><img src={cat.image} alt="" /></div>)}
          <div className="pair-card-back">?</div>
        </div>
        <span className="eyebrow">{pairsCards / 2} пар</span>
        <h3>Найди пару</h3>
        <p>Открывайте по две карточки и запоминайте, где спрятались одинаковые котики.</p>
        <div className="pairs-mode-picker" aria-label="Размер поля">
          {PAIRS_CARD_COUNTS.map((count) => <button key={count} type="button"
            className={pairsCards === count ? 'active' : ''} aria-pressed={pairsCards === count}
            onClick={() => setPairsCards(count)}>{count} карточек</button>)}
        </div>
        <button type="button" className="match3-primary" onClick={() => onStartPairs(pairsCards)}>Играть · {pairsCards} карточек</button>
      </article>
      <article className="minigame-card">
        <div className="minigame-preview mahjong-preview" aria-hidden="true">
          {roomCats.slice(0, 5).map((cat, index) => <div key={cat.id} style={{ transform: `translate(${index * 13 - 26}px, ${Math.abs(index - 2) * 5}px)`, zIndex: index }}><img src={cat.image} alt="" /></div>)}
        </div>
        <span className="eyebrow">{mahjongLayout.tileCount} фишек · {mahjongLayout.tileCount / 2} пар</span>
        <h3>Кошачий маджонг</h3>
        <p>Снимайте одинаковых свободных котиков со слоёв. Без таймера, штрафов и спешки.</p>
        <div className="mahjong-mode-picker" aria-label="Сложность маджонга">
          {mahjongDifficulties.map((difficulty) => {
            const layout = getMahjongLayout(difficulty)
            return <button key={difficulty} type="button" className={mahjongDifficulty === difficulty ? 'active' : ''}
              aria-pressed={mahjongDifficulty === difficulty} onClick={() => setMahjongDifficulty(difficulty)}>
              {layout.name}<small>{layout.tileCount}</small>
            </button>
          })}
        </div>
        <button type="button" className="match3-primary" onClick={() => onStartMahjong(mahjongDifficulty)}>Играть · {mahjongLayout.name.toLowerCase()}</button>
      </article>
      <article className="minigame-card sliding-hub-card">
        <div className="minigame-preview sliding-preview" aria-hidden="true">
          <img src={slidingCat.image} alt="" />
          <span className="sliding-preview-grid" />
          <span className="sliding-preview-empty" />
        </div>
        <span className="eyebrow">{slidingConfig.size}×{slidingConfig.size} · один котик</span>
        <h3>Кошачьи пятнашки</h3>
        <p>Передвигайте соседние плитки в пустую клетку и восстановите изображение котика.</p>
        <div className="sliding-cat-picker" aria-label="Кот для пятнашек">
          {roomCats.map((cat) => <button key={cat.id} type="button" className={slidingCat.id === cat.id ? 'active' : ''}
            aria-pressed={slidingCat.id === cat.id} onClick={() => setSlidingCatId(cat.id)} title={cat.name}>
            <img src={cat.image} alt={cat.name} />
          </button>)}
        </div>
        <div className="mahjong-mode-picker" aria-label="Сложность пятнашек">
          {slidingDifficulties.map((difficulty) => {
            const config = getSlidingConfig(difficulty)
            return <button key={difficulty} type="button" className={slidingDifficulty === difficulty ? 'active' : ''}
              aria-pressed={slidingDifficulty === difficulty} onClick={() => setSlidingDifficulty(difficulty)}>
              {config.name}<small>{config.size}×{config.size}</small>
            </button>
          })}
        </div>
        <button type="button" className="match3-primary" onClick={() => onStartSliding(slidingDifficulty, slidingCat.id)}>
          Играть · {slidingConfig.size}×{slidingConfig.size}
        </button>
      </article>
    </div>
  </section>
}
