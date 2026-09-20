import { activeProgress, resourceCost, type GameState } from '../game/economy'
import { resources } from '../game/items'

interface Props {
  state: GameState
  onBuy: (id: string) => void
}

export function ResourceShop({ state, onBuy }: Props) {
  const progress = activeProgress(state)
  return <section className="content-section" aria-labelledby="resources-title">
    <div className="section-heading"><div><span className="eyebrow">Доход за нажатие</span><h2 id="resources-title">Ресурсы</h2></div><p>Каждая покупка добавляет столько же рыбок за клик. Цена следующего уровня растёт.</p></div>
    <div className="card-grid">{resources.map((item) => {
      const level = progress.resourceLevels[item.id] ?? 0
      const cost = resourceCost(state, item.id)
      return <article className="item-card" key={item.id}>
        <div className="item-art"><img src={item.image} alt="" /></div>
        <div className="item-details"><span className="level">Уровень {level}</span><h3>{item.name}</h3><p>+{item.bonus} за клик при каждой покупке · сейчас +{item.bonus * level}</p></div>
        <button className="buy-button" type="button" disabled={progress.fish < cost} onClick={() => onBuy(item.id)} aria-label={`Купить ${item.name} за ${cost} рыбок`}>
          <span>Улучшить</span><span className="price"><img src="/assets/resources/01.png" alt="" />{cost.toLocaleString('ru-RU')}</span>
        </button>
      </article>
    })}</div>
  </section>
}
