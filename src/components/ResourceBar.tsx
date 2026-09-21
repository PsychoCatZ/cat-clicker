import { activeProgress, clickPower, currentClickReward, fishPerSecond, hungerDuration, safetyIncome, type GameState } from '../game/economy'
import { rooms } from '../game/rooms'

const fishIcon = '/assets/resources/01.png'
const format = (value: number): string => value < 1 && value > 0 ? value.toFixed(2) : Math.floor(value).toLocaleString('ru-RU')

interface Props {
  state: GameState
  onReset: () => void
  onToggleLights: () => void
}

export function ResourceBar({ state, onReset, onToggleLights }: Props) {
  const progress = activeProgress(state)
  const sleeping = progress.hunger <= 0 || progress.lightsOff
  const minutes = Math.ceil(progress.hunger / 100 * hungerDuration(state.mode) / 60)
  return (
    <header className="topbar">
      <div className="topbar-main">
        <div className="brand">
          <img src="/assets/ui/01.png" alt="" />
          <div><span className="brand-kicker">{state.mode === 'expert' ? 'Режим Эксперт' : 'Обычный режим'} · {rooms[state.currentRoom - 1].name}</span><h1>Котокликер</h1></div>
        </div>
        <div className="stats" aria-label="Ресурсы и доход">
          <div className="stat stat-main"><img src={fishIcon} alt="" /><div><span>Рыбки комнаты</span><strong data-testid="fish-count">{format(progress.fish)}</strong></div></div>
          <div className="stat"><span className="stat-symbol">+</span><div><span>За клик</span><strong data-testid="click-power">{currentClickReward(state).toLocaleString('ru-RU')}</strong></div></div>
          <div className="stat"><span className="stat-symbol">/с</span><div><span>В секунду</span><strong data-testid="fish-per-second">{format(fishPerSecond(state) + safetyIncome(state))}</strong></div></div>
        </div>
        <button className="reset-button" onClick={onReset} type="button" title="Сбросить прогресс" aria-label="Сбросить прогресс">
          <img src="/assets/ui/05.png" alt="" /><span>Сбросить</span>
        </button>
      </div>
      <div className="hunger-row">
        <span className="hunger-label">Голод</span>
        <div className="hunger-track" role="progressbar" aria-label="Сытость кота" aria-valuemin={0} aria-valuemax={100} aria-valuenow={Math.ceil(progress.hunger)}>
          <div className="hunger-fill" style={{ width: `${progress.hunger}%` }} />
        </div>
        <strong>{progress.hunger <= 0 ? 'Кот голоден' : progress.lightsOff ? `${Math.ceil(progress.hunger)}% · пауза` : `${Math.ceil(progress.hunger)}% · ~${minutes} мин`}</strong>
        <button className="light-toggle" type="button" onClick={onToggleLights} disabled={progress.hunger <= 0}
          aria-pressed={progress.lightsOff} title={progress.hunger <= 0 ? 'Сначала покормите кота' : undefined}>
          {progress.lightsOff ? 'Включить свет' : 'Выключить свет'}
        </button>
        {progress.caviarSeconds > 0 && <span className="boost-label">Икра ×2 · {Math.ceil(progress.caviarSeconds)} с</span>}
        {!sleeping && <span className="base-power" title="Без покупок">База: {clickPower(state)}</span>}
      </div>
    </header>
  )
}
