import { activeProgress, foodCost, safetyIncome, type GameState } from '../game/economy'
import { foods } from '../game/items'

interface Props {
  state: GameState
  onBuy: (id: string) => void
}

export function FoodShop({ state, onBuy }: Props) {
  const progress = activeProgress(state)
  return <section className="content-section" aria-labelledby="food-title">
    <div className="section-heading"><div><span className="eyebrow">Разбудите кота</span><h2 id="food-title">Корм</h2></div><p>Когда шкала пуста, клики не работают. Рыбки в секунду продолжают поступать.</p></div>
    {safetyIncome(state) > 0 && <p className="help-note">Даже без улучшений рыбки медленно копятся. На мышку хватит примерно через 30 минут.</p>}
    <div className="card-grid food-grid">{foods.map((item) => {
      const cost = foodCost(state, item.baseCost)
      return <article className="item-card" key={item.id}>
        <div className="item-art"><img src={item.image} alt="" /></div>
        <div className="item-details"><span className="level">Сытость +{item.restore}%</span><h3>{item.name}</h3><p>{item.boostSeconds ? 'Доход за клик ×2 на 1 минуту' : 'Восстанавливает шкалу голода'}</p></div>
        <button className="buy-button" type="button" disabled={progress.fish < cost || (progress.hunger >= 100 && !item.boostSeconds)} onClick={() => onBuy(item.id)} aria-label={`Купить ${item.name} за ${cost} рыбок`}>
          <span>Купить</span><span className="price"><img src="/assets/resources/01.png" alt="" />{cost.toLocaleString('ru-RU')}</span>
        </button>
      </article>
    })}</div>
  </section>
}
