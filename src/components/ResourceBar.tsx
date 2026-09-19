import type { GameState } from '../game/economy'
import { fishPerSecond } from '../game/economy'

const fishIcon = '/assets/resources/01.png'

interface Props {
  state: GameState
  onReset: () => void
}

export function ResourceBar({ state, onReset }: Props) {
  return (
    <header className="topbar">
      <div className="brand">
        <img src="/assets/ui/01.png" alt="" />
        <div>
          <span className="brand-kicker">Уютная игра</span>
          <h1>Котокликер</h1>
        </div>
      </div>
      <div className="stats" aria-label="Ресурсы и доход">
        <div className="stat stat-main">
          <img src={fishIcon} alt="" />
          <div><span>Рыбки</span><strong data-testid="fish-count">{state.fish.toLocaleString('ru-RU')}</strong></div>
        </div>
        <div className="stat">
          <span className="stat-symbol">+</span>
          <div><span>За клик</span><strong data-testid="click-power">{state.clickPower.toLocaleString('ru-RU')}</strong></div>
        </div>
        <div className="stat">
          <span className="stat-symbol">/с</span>
          <div><span>В секунду</span><strong data-testid="fish-per-second">{fishPerSecond(state).toLocaleString('ru-RU')}</strong></div>
        </div>
      </div>
      <button className="reset-button" onClick={onReset} type="button" title="Сбросить прогресс">
        <img src="/assets/ui/05.png" alt="" />
        <span>Сбросить прогресс</span>
      </button>
    </header>
  )
}
