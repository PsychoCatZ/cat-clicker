import { activeProgress, basicUpgradesBought, upgradeCost, type GameState } from '../game/economy'
import { upgradesForRoom, type Upgrade } from '../game/upgrades'

interface Props {
  state: GameState
  onBuy: (id: string) => void
}

export function Shop({ state, onBuy }: Props) {
  const progress = activeProgress(state)
  const advancedOpen = basicUpgradesBought(state)
  const upgrades = upgradesForRoom(state.currentRoom)
  const renderCard = (upgrade: Upgrade) => {
    const owned = progress.boughtUpgrades.includes(upgrade.id)
    const cost = upgradeCost(state, upgrade)
    const locked = upgrade.tier === 'advanced' && !advancedOpen
    return <article className={`item-card${owned ? ' owned' : ''}${locked ? ' locked' : ''}`} key={upgrade.id}>
      <div className="item-art"><img src={upgrade.image} alt="" /></div>
      <div className="item-details"><span className="level">{upgrade.tier === 'advanced' ? 'Продвинутое' : 'Обычное'} · место {upgrade.slot + 1}</span><h3>{upgrade.name}</h3><p>+{upgrade.income.toLocaleString('ru-RU')} рыбок в секунду</p></div>
      {owned ? <div className="selected-label">Куплено</div>
        : <button className="buy-button" type="button" disabled={locked || progress.fish < cost} onClick={() => onBuy(upgrade.id)}
          aria-label={`Купить ${upgrade.name} за ${cost} рыбок`}>
          <span>{locked ? 'Сначала обычные' : 'Купить'}</span><span className="price"><img src="/assets/resources/01.png" alt="" />{cost.toLocaleString('ru-RU')}</span>
        </button>}
    </article>
  }
  return <section className="content-section" aria-labelledby="shop-title">
    <div className="section-heading"><div><span className="eyebrow">Обставьте комнату</span><h2 id="shop-title">Улучшения</h2></div><p>Купите 5 обычных, чтобы открыть продвинутые. Все 10 нужны для перехода.</p></div>
    <div className="tier-heading">Обычные · {upgrades.filter((item) => item.tier === 'basic' && progress.boughtUpgrades.includes(item.id)).length}/5</div>
    <div className="card-grid">{upgrades.filter((item) => item.tier === 'basic').map(renderCard)}</div>
    <div className="tier-heading">Продвинутые · {upgrades.filter((item) => item.tier === 'advanced' && progress.boughtUpgrades.includes(item.id)).length}/5</div>
    <p className="tier-note">Продвинутый предмет заменяет обычный в комнате. Доход обоих сохраняется.</p>
    <div className="card-grid">{upgrades.filter((item) => item.tier === 'advanced').map(renderCard)}</div>
  </section>
}
