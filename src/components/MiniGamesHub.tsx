import { useState } from 'react'
import { catsForRoom } from '../game/cats'
import { PAIRS_CARD_COUNTS, type PairsCardCount } from '../game/pairs/types'
import type { Room } from '../game/rooms'

interface Props {
  room: Room
  onStartMatch3: () => void
  onStartPairs: (cardCount: PairsCardCount) => void
}

export function MiniGamesHub({ room, onStartMatch3, onStartPairs }: Props) {
  const roomCats = catsForRoom(room.id)
  const [pairsCards, setPairsCards] = useState<PairsCardCount>(10)
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
    </div>
  </section>
}
