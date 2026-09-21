import { useState, type CSSProperties } from 'react'
import { catsForRoom } from '../game/cats'
import type { GameMode } from '../game/economy'
import { movableSlidingTileIds } from '../game/sliding/board'
import { getSlidingConfig } from '../game/sliding/layouts'
import { slidingFishReward } from '../game/sliding/scoring'
import type { SlidingRound } from '../game/sliding/types'
import type { Room } from '../game/rooms'
import './SlidingPuzzleGame.css'

interface Props {
  room: Room
  mode: GameMode
  round: SlidingRound
  onMove: (tileId: number) => void
  onReshuffle: () => void
  onPlayAgain: () => void
  onExit: () => void
}

const fishIcon = '/assets/resources/01.png'

export function SlidingPuzzleGame({ room, mode, round, onMove, onReshuffle, onPlayAgain, onExit }: Props) {
  const [showOriginal, setShowOriginal] = useState(false)
  const config = getSlidingConfig(round.difficulty)
  const cat = catsForRoom(round.roomId).find((item) => item.id === round.catId)
  const movable = new Set(movableSlidingTileIds(round.tiles, round.size))
  const reward = slidingFishReward(round.score, round.roomId, round.mode)
  const emptyIndex = round.tiles.indexOf(null)
  const message = round.lastEvent === 'shuffled' ? 'Картинка перемешана допустимыми ходами и точно решаема.'
    : round.lastEvent === 'moved' ? 'Передвигайте соседние плитки в пустую клетку.'
      : 'Соберите цельное изображение котика.'

  return <section className="match3-screen sliding-screen" style={{ backgroundImage: `url(${room.match3Background})` }} aria-labelledby="sliding-title">
    <div className="match3-card sliding-card">
      <div className="match3-title-row">
        <div><span className="eyebrow">Сосредоточенный режим · {room.name}</span><h2 id="sliding-title">Кошачьи пятнашки</h2></div>
        <button type="button" className="match3-exit" onClick={onExit}>Вернуться</button>
      </div>
      <div className="sliding-summary" aria-label="Состояние пазла">
        <div><span>Сложность</span><strong>{config.name} · {round.size}×{round.size}</strong></div>
        <div><span>Котик</span><strong>{cat?.name ?? 'Котик'}</strong></div>
        <div><span>Ходы</span><strong>{round.moves.toLocaleString('ru-RU')}</strong></div>
        <div><span>Рыбки сейчас</span><strong><img src={fishIcon} alt="" />{reward.toLocaleString('ru-RU')}</strong></div>
      </div>
      <p className="match3-message sliding-message" role="status" aria-live="polite">{message}</p>
      <div className="sliding-controls">
        <button type="button" onClick={() => setShowOriginal(true)}>Показать оригинал</button>
        <button type="button" onClick={onReshuffle} disabled={round.status !== 'playing'}>Перемешать заново</button>
      </div>
      <div className={`sliding-board sliding-${round.difficulty}`} aria-label={`Пятнашки ${round.size} на ${round.size}, кот ${cat?.name ?? ''}`}>
        <div className="sliding-empty" aria-hidden="true" style={{
          left: `${emptyIndex % round.size / round.size * 100}%`,
          top: `${Math.floor(emptyIndex / round.size) / round.size * 100}%`,
          width: `${100 / round.size}%`,
          height: `${100 / round.size}%`,
        }} />
        {Array.from({ length: round.size ** 2 - 1 }, (_, tileId) => {
          const position = round.tiles.indexOf(tileId)
          const targetColumn = tileId % round.size
          const targetRow = Math.floor(tileId / round.size)
          const pieceStyle = {
            left: `${position % round.size / round.size * 100}%`,
            top: `${Math.floor(position / round.size) / round.size * 100}%`,
            width: `${100 / round.size}%`,
            height: `${100 / round.size}%`,
          } as CSSProperties
          const imageStyle = {
            width: `${round.size * 100}%`,
            height: `${round.size * 100}%`,
            left: `${-targetColumn * 100}%`,
            top: `${-targetRow * 100}%`,
          } as CSSProperties
          return <button key={tileId} type="button" className={`sliding-piece${movable.has(tileId) ? ' movable' : ''}`}
            style={pieceStyle} disabled={!movable.has(tileId) || round.status !== 'playing'} onClick={() => onMove(tileId)}
            aria-label={`Фрагмент ${tileId + 1}${movable.has(tileId) ? ', можно передвинуть' : ''}`}>
            {cat && <img src={cat.image} alt="" draggable="false" style={imageStyle} />}
          </button>
        })}
      </div>
      <p className="match3-help">Нажимайте на плитку рядом с пустой клеткой. Число ходов ничем не ограничено.</p>
      <p className="match3-mode-note">{mode === 'expert' ? 'Режим Эксперт: котификация увеличена.' : 'Таймера и штрафов нет — собирайте картинку в своём темпе.'}</p>

      {showOriginal && <div className="sliding-original" role="dialog" aria-modal="true" aria-labelledby="sliding-original-title">
        <div>
          <span className="eyebrow">Образец</span>
          <h3 id="sliding-original-title">{cat?.name ?? 'Котик'}</h3>
          <div className="sliding-original-image">{cat && <img src={cat.image} alt={`Оригинальное изображение: ${cat.name}`} />}</div>
          <button type="button" className="match3-primary" onClick={() => setShowOriginal(false)}>Продолжить</button>
        </div>
      </div>}

      {round.status === 'finished' && <div className="match3-result" role="dialog" aria-modal="true" aria-labelledby="sliding-result-title">
        <div>
          <span className="eyebrow">Пазл завершён</span>
          <h3 id="sliding-result-title">Картинка собрана!</h3>
          <p>Сложность: <strong>{config.name} · {round.size}×{round.size}</strong></p>
          <p>Количество ходов: <strong>{round.moves.toLocaleString('ru-RU')}</strong></p>
          <p>Набрано очков: <strong>{round.score.toLocaleString('ru-RU')}</strong></p>
          <p className="match3-reward"><img src={fishIcon} alt="" /> Котифицировано: <strong>{reward.toLocaleString('ru-RU')} рыбок</strong></p>
          <div className="sliding-result-actions">
            <button type="button" className="match3-primary" onClick={onPlayAgain}>Сыграть ещё</button>
            <button type="button" className="secondary-button" onClick={onExit}>Вернуться</button>
          </div>
        </div>
      </div>}
    </div>
  </section>
}
