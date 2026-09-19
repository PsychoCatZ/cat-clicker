import type { GameState } from '../game/economy'
import { upgradeCost } from '../game/economy'
import { upgrades } from '../game/upgrades'

interface Props {
  state: GameState
  onBuy: (id: string) => void
}

export function Shop({ state, onBuy }: Props) {
  return (
    <section className="content-section" aria-labelledby="shop-title">
      <div className="section-heading">
        <div><span className="eyebrow">Для счастливого кота</span><h2 id="shop-title">Магазин улучшений</h2></div>
        <p>Уютные вещи помогают собирать больше рыбок</p>
      </div>
      <div className="card-grid shop-grid">
        {upgrades.map((upgrade) => {
          const level = state.upgradeLevels[upgrade.id] ?? 0
          const cost = upgradeCost(upgrade.baseCost, upgrade.growth, level)
          return (
            <article className="item-card" key={upgrade.id}>
              <div className="item-art"><img src={upgrade.image} alt="" /></div>
              <div className="item-details">
                <span className="level">Уровень {level}</span>
                <h3>{upgrade.name}</h3>
                <p>{upgrade.description}</p>
              </div>
              <button className="buy-button" type="button" disabled={state.fish < cost} onClick={() => onBuy(upgrade.id)} aria-label={`Купить ${upgrade.name} за ${cost} рыбок`}>
                <span>Улучшить</span><span className="price"><img src="/assets/resources/01.png" alt="" />{cost.toLocaleString('ru-RU')}</span>
              </button>
            </article>
          )
        })}
      </div>
    </section>
  )
}
