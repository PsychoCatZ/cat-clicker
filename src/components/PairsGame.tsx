import { useEffect } from 'react'
import { catsForRoom } from '../game/cats'
import type { GameMode } from '../game/economy'
import { pairsFishReward } from '../game/pairs/scoring'
import type { PairsRound } from '../game/pairs/types'
import type { Room } from '../game/rooms'

interface Props {
  room: Room
  mode: GameMode
  round: PairsRound
  onReveal: (cardId: number) => void
  onHideMismatch: () => void
  onExit: () => void
}

const fishIcon = '/assets/resources/01.png'

export function PairsGame({ room, mode, round, onReveal, onHideMismatch, onExit }: Props) {
  const catsById = new Map(catsForRoom(room.id).map((cat) => [cat.id, cat]))
  const totalPairs = round.cardCount / 2
  const reward = pairsFishReward(round.matches, round.attempts, totalPairs, round.roomId, round.mode, round.status === 'finished')

  useEffect(() => {
    if (round.revealed.length !== 2 || round.lastMatch !== false) return
    const timer = window.setTimeout(onHideMismatch, 850)
    return () => window.clearTimeout(timer)
  }, [round.revealed, round.lastMatch, onHideMismatch])

  const message = round.lastMatch === true
    ? 'Пара найдена! Продолжайте.'
    : round.lastMatch === false
      ? 'Не совпали — запомните карточки.'
      : round.revealed.length === 1 ? 'Теперь откройте вторую карточку.' : 'Откройте две карточки и найдите одинаковых котиков.'

  return <section className="match3-screen pairs-screen" style={{ backgroundImage: `url(${room.match3Background})` }} aria-labelledby="pairs-title">
    <div className="match3-card pairs-card">
      <div className="match3-title-row">
        <div><span className="eyebrow">Сосредоточенный режим · {room.name}</span><h2 id="pairs-title">Найди пару</h2></div>
        <button type="button" className="match3-exit" onClick={onExit}>Закончить игру</button>
      </div>
      <div className="match3-stats" aria-label="Состояние раунда">
        <div><span>Найдено</span><strong>{round.matches} / {totalPairs}</strong></div>
        <div><span>Попытки</span><strong>{round.attempts}</strong></div>
        <div><span>Награда сейчас</span><strong><img src={fishIcon} alt="Рыбки" />{reward.toLocaleString('ru-RU')}</strong></div>
      </div>
      <p className="match3-message" role="status" aria-live="polite">{message}</p>
      <div className={`pairs-board cards-${round.cardCount}`} aria-label={`Поле из ${round.cardCount} карточек`}>
        {round.cards.map((card, index) => {
          const cat = catsById.get(card.catId)
          const visible = card.matched || round.revealed.includes(card.id)
          return <button key={card.id} type="button"
            className={`pair-card${visible ? ' revealed' : ''}${card.matched ? ' matched' : ''}`}
            onClick={() => onReveal(card.id)} disabled={card.matched || round.revealed.length >= 2}
            aria-label={visible ? `${cat?.name ?? 'Котик'}, карточка ${index + 1}` : `Закрытая карточка ${index + 1}`}
            aria-pressed={visible}>
            <span className="pair-card-inner">
              <span className="pair-card-front" aria-hidden="true">?</span>
              <span className="pair-card-face">{cat && <img src={cat.image} alt="" draggable="false" />}</span>
            </span>
          </button>
        })}
      </div>
      {round.status === 'finished' && <div className="match3-result" role="dialog" aria-modal="true" aria-labelledby="pairs-result-title">
        <div>
          <span className="eyebrow">Все пары найдены</span>
          <h3 id="pairs-result-title">Прекрасная память!</h3>
          <p>Понадобилось попыток: <strong>{round.attempts}</strong>.</p>
          <p className="match3-reward"><img src={fishIcon} alt="" /> Награда: <strong>{reward.toLocaleString('ru-RU')} рыбок</strong></p>
          <button type="button" className="match3-primary" onClick={onExit}>Получить рыбки</button>
        </div>
      </div>}
      {round.status === 'playing' && <p className="match3-help">{totalPairs} пар, без таймера. Незавершённый раунд сохранится автоматически.</p>}
      <p className="match3-mode-note">{mode === 'expert' ? 'Режим Эксперт: награда увеличена.' : 'Мини-игра необязательна и не влияет на открытие комнат.'}</p>
    </div>
  </section>
}
