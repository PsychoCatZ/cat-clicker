import { useEffect, useState } from 'react'
import { catsForRoom } from '../game/cats'
import type { GameMode } from '../game/economy'
import { areAdjacent, swapCreatesMatch } from '../game/match3/board'
import { match3FishReward } from '../game/match3/scoring'
import type { Match3Round } from '../game/match3/types'
import type { Room } from '../game/rooms'

interface Props {
  room: Room
  mode: GameMode
  round: Match3Round | null
  onStart: () => void
  onSwap: (first: number, second: number) => void
  onExit: () => void
}

const fishIcon = '/assets/resources/01.png'

export function Match3Game({ room, mode, round, onStart, onSwap, onExit }: Props) {
  const roomCats = catsForRoom(room.id)
  const catsById = new Map(roomCats.map((cat) => [cat.id, cat]))
  const [selected, setSelected] = useState<number | null>(null)
  const [message, setMessage] = useState('Выберите котика, затем коснитесь соседнего.')
  const reward = round ? match3FishReward(round.score, round.roomId, round.mode) : 0

  useEffect(() => {
    setSelected(null)
    if (!round) {
      setMessage('Здесь нет таймера. Играйте спокойно в удобном темпе.')
    } else if (round.lastGain > 0) {
      const combo = round.lastCombo > 1 ? ` Комбо ×${round.lastCombo}!` : ''
      const shuffle = round.shuffled ? ' Поле мягко перемешано: все ходы снова доступны.' : ''
      setMessage(`Получено ${round.lastGain.toLocaleString('ru-RU')} очков.${combo}${shuffle}`)
    }
  }, [round?.id, round?.score, round?.lastGain, round?.lastCombo, round?.shuffled])

  function chooseTile(index: number) {
    if (!round || round.status !== 'playing') return
    if (selected === null) {
      setSelected(index)
      setMessage('Теперь выберите соседнего котика.')
      return
    }
    if (selected === index) {
      setSelected(null)
      setMessage('Выбор отменён. Можно выбрать другого котика.')
      return
    }
    if (!areAdjacent(selected, index)) {
      setSelected(index)
      setMessage('Выбран новый котик. Теперь коснитесь соседнего.')
      return
    }
    if (!swapCreatesMatch(round.board, selected, index)) {
      setSelected(null)
      setMessage('Ряд не получился, но ход не потрачен. Попробуйте другую пару.')
      return
    }
    onSwap(selected, index)
    setSelected(null)
  }

  return <section className="match3-screen" style={{ backgroundImage: `url(${room.match3Background})` }} aria-labelledby="match3-title">
    <div className="match3-card">
      <div className="match3-title-row">
        <div><span className="eyebrow">Спокойная мини-игра · {room.name}</span><h2 id="match3-title">Котики в ряд</h2></div>
        {round && <button type="button" className="match3-exit" onClick={onExit}>Закончить игру</button>}
      </div>

      {!round ? <div className="match3-intro">
        <div className="match3-cat-preview" aria-hidden="true">
          {roomCats.map((cat) => <img key={cat.id} src={cat.image} alt="" />)}
        </div>
        <h3>Собирайте по три одинаковых котика</h3>
        <p>Выберите одного котика, а затем соседнего. Если получится ряд из трёх или больше, ход засчитается.</p>
        <ul>
          <li>20 ходов, без ограничения времени.</li>
          <li>Ошибочная перестановка не расходует ход.</li>
          <li>Все очки превратятся в рыбки этой комнаты.</li>
        </ul>
        <button type="button" className="match3-primary" onClick={onStart}>Начать спокойный раунд</button>
      </div> : <>
        <div className="match3-stats" aria-label="Состояние раунда">
          <div><span>Ходы</span><strong>{round.movesLeft}</strong></div>
          <div><span>Очки</span><strong>{round.score.toLocaleString('ru-RU')}</strong></div>
          <div><span>Награда сейчас</span><strong><img src={fishIcon} alt="Рыбки" />{reward.toLocaleString('ru-RU')}</strong></div>
        </div>
        <p className="match3-message" role="status" aria-live="polite">{message}</p>
        <div className="match3-board" aria-label="Поле семь на семь">
          {round.board.map((tile, index) => {
            const cat = catsById.get(tile.catId)
            const row = Math.floor(index / 7) + 1
            const column = index % 7 + 1
            return <button key={tile.id} type="button" className={`match3-tile${selected === index ? ' selected' : ''}`}
              onClick={() => chooseTile(index)} aria-pressed={selected === index}
              aria-label={`${cat?.name ?? 'Котик'}, ряд ${row}, столбец ${column}`}>
              {cat && <img src={cat.image} alt="" draggable="false" />}
            </button>
          })}
        </div>
        {round.status === 'finished' && <div className="match3-result" role="dialog" aria-modal="true" aria-labelledby="match3-result-title">
          <div>
            <span className="eyebrow">Раунд завершён</span>
            <h3 id="match3-result-title">Отличная работа!</h3>
            <p>Набрано <strong>{round.score.toLocaleString('ru-RU')}</strong> очков.</p>
            <p className="match3-reward"><img src={fishIcon} alt="" /> Награда: <strong>{reward.toLocaleString('ru-RU')} рыбок</strong></p>
            <button type="button" className="match3-primary" onClick={onExit}>Получить рыбки</button>
          </div>
        </div>}
        {round.status === 'playing' && <p className="match3-help">Крупная рамка показывает выбранного котика. Можно играть без спешки — таймера нет.</p>}
      </>}
      <p className="match3-mode-note">{mode === 'expert' ? 'Режим Эксперт: награда увеличена, но цены основной игры остаются выше.' : 'Мини-игра необязательна и не влияет на открытие комнат.'}</p>
    </div>
  </section>
}
