import { catsForRoom } from '../game/cats'
import { activeProgress, catCost, type GameState } from '../game/economy'

interface Props {
  state: GameState
  onBuy: (id: string) => void
  onSelect: (id: string) => void
}

export function CatCollection({ state, onBuy, onSelect }: Props) {
  const progress = activeProgress(state)
  return <section className="content-section" aria-labelledby="cats-title">
    <div className="section-heading"><div><span className="eyebrow">Спасите всех пятерых</span><h2 id="cats-title">Коты комнаты</h2></div><p>Купленные коты навсегда остаются в своей комнате. Любого можно выбрать.</p></div>
    <div className="card-grid">{catsForRoom(state.currentRoom).map((cat) => {
      const unlocked = progress.unlockedCats.includes(cat.id)
      const selected = progress.selectedCat === cat.id
      const cost = catCost(state, cat.baseCost)
      return <article className={selected ? 'item-card cat-card selected' : 'item-card cat-card'} key={cat.id}>
        <div className="cat-art"><img src={cat.image} alt={cat.name} /></div>
        <div className="cat-details"><h3>{cat.name}</h3><p>{selected ? 'Сейчас в комнате' : unlocked ? 'В коллекции' : 'Ждёт спасения'}</p></div>
        {selected ? <div className="selected-label">Выбран</div>
          : unlocked ? <button className="buy-button" type="button" onClick={() => onSelect(cat.id)}>Выбрать</button>
            : <button className="buy-button" type="button" disabled={progress.fish < cost} onClick={() => onBuy(cat.id)} aria-label={`Открыть кота ${cat.name} за ${cost} рыбок`}>
              <span>Открыть</span><span className="price"><img src="/assets/resources/01.png" alt="" />{cost.toLocaleString('ru-RU')}</span>
            </button>}
      </article>
    })}</div>
  </section>
}
