import { basicCats } from '../game/cats'
import type { GameState } from '../game/economy'

interface Props {
  state: GameState
  onBuy: (id: string) => void
  onSelect: (id: string) => void
}

export function CatCollection({ state, onBuy, onSelect }: Props) {
  return (
    <section className="content-section" aria-labelledby="cats-title">
      <div className="section-heading">
        <div><span className="eyebrow">Ваша компания</span><h2 id="cats-title">Коллекция котов</h2></div>
        <p>Откройте кота один раз и выбирайте его в любое время</p>
      </div>
      <div className="card-grid cat-grid">
        {basicCats.map((cat) => {
          const unlocked = state.unlockedCats.includes(cat.id)
          const selected = state.selectedCat === cat.id
          return (
            <article className={selected ? 'item-card cat-card selected' : 'item-card cat-card'} key={cat.id}>
              <div className="cat-art"><img src={cat.image} alt={cat.name} /></div>
              <div className="cat-details"><h3>{cat.name}</h3><p>{selected ? 'Сейчас с вами' : unlocked ? 'В коллекции' : 'Ждёт знакомства'}</p></div>
              {selected ? (
                <div className="selected-label">Выбран</div>
              ) : unlocked ? (
                <button className="buy-button" type="button" onClick={() => onSelect(cat.id)}>Выбрать</button>
              ) : (
                <button className="buy-button" type="button" disabled={state.fish < (cat.cost ?? 0)} onClick={() => onBuy(cat.id)} aria-label={`Открыть кота ${cat.name} за ${cat.cost} рыбок`}>
                  <span>Открыть</span><span className="price"><img src="/assets/resources/01.png" alt="" />{cat.cost?.toLocaleString('ru-RU')}</span>
                </button>
              )}
            </article>
          )
        })}
      </div>
    </section>
  )
}
