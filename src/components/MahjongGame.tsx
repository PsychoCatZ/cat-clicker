import type { CSSProperties } from 'react'
import { catsForRoom } from '../game/cats'
import type { GameMode } from '../game/economy'
import { findMahjongPairs, isMahjongTileFree } from '../game/mahjong/board'
import { getMahjongLayout } from '../game/mahjong/layouts'
import { mahjongFishReward } from '../game/mahjong/scoring'
import type { MahjongRound } from '../game/mahjong/types'
import type { Room } from '../game/rooms'
import './MahjongGame.css'

interface Props {
  room: Room
  mode: GameMode
  round: MahjongRound
  onSelect: (tileId: number) => void
  onHint: () => void
  onShuffle: () => void
  onPlayAgain: () => void
  onExit: () => void
}

const fishIcon = '/assets/resources/01.png'

export function MahjongGame({ room, mode, round, onSelect, onHint, onShuffle, onPlayAgain, onExit }: Props) {
  const layout = getMahjongLayout(round.difficulty)
  const catsById = new Map(catsForRoom(room.id).map((cat) => [cat.id, cat]))
  const availablePairs = findMahjongPairs(round.tiles)
  const activeCount = round.tiles.filter((tile) => !tile.removed).length
  const reward = mahjongFishReward(round.score, round.roomId, round.mode)
  const message = round.lastEvent === 'mismatch' ? 'Эти котики разные. Выбран новый свободный котик.'
    : round.lastEvent === 'match' ? 'Пара найдена! Открылись новые фишки.'
      : round.lastEvent === 'hint' ? 'Подсказка мягко подсветила доступную пару.'
        : round.lastEvent === 'shuffled' ? 'Фишки перемешаны — поле снова проходимо.'
          : round.selectedId !== null ? 'Теперь выберите такую же свободную фишку.'
            : availablePairs.length === 0 && activeCount > 0 ? 'Доступных пар нет. Можно спокойно перемешать фишки.'
              : 'Выберите две одинаковые свободные фишки.'

  return <section className="match3-screen mahjong-screen" style={{ backgroundImage: `url(${room.match3Background})` }} aria-labelledby="mahjong-title">
    <div className="match3-card mahjong-card">
      <div className="match3-title-row">
        <div><span className="eyebrow">Сосредоточенный режим · {room.name}</span><h2 id="mahjong-title">Кошачий маджонг</h2></div>
        <button type="button" className="match3-exit" onClick={onExit}>Вернуться</button>
      </div>
      <div className="mahjong-summary" aria-label="Состояние раунда">
        <div><span>Сложность</span><strong>{layout.name}</strong></div>
        <div><span>Найдено пар</span><strong>{round.pairsFound} / {layout.tileCount / 2}</strong></div>
        <div><span>Очки</span><strong>{round.score.toLocaleString('ru-RU')}</strong></div>
        <div><span>Рыбки сейчас</span><strong><img src={fishIcon} alt="" />{reward.toLocaleString('ru-RU')}</strong></div>
      </div>
      <p className="match3-message mahjong-message" role="status" aria-live="polite">{message}</p>
      <div className="mahjong-controls">
        <button type="button" onClick={onHint} disabled={round.status !== 'playing' || availablePairs.length === 0}>Подсказка</button>
        <button type="button" onClick={onShuffle} disabled={round.status !== 'playing' || availablePairs.length > 0 || activeCount === 0}>Перемешать</button>
        <span>Подсказки: {round.hintsUsed} · Перемешивания: {round.shuffles}</span>
      </div>
      <div className={`mahjong-board mahjong-${round.difficulty}`} style={{ aspectRatio: `${layout.width} / ${layout.height * 1.08}` }}
        aria-label={`Поле маджонга: ${layout.name.toLowerCase()} уровень, ${layout.tileCount} фишек`}>
        {round.tiles.map((tile) => {
          const cat = catsById.get(tile.catId)
          const free = isMahjongTileFree(round.tiles, tile.id)
          const selected = round.selectedId === tile.id
          const hinted = round.hintedIds.includes(tile.id)
          const tileStyle = {
            '--mahjong-left': `${tile.x / layout.width * 100}%`,
            '--mahjong-top': `${tile.y / layout.height * 100}%`,
            '--mahjong-width': `${2 / layout.width * 100}%`,
            '--mahjong-height': `${2 / layout.height * 100}%`,
            '--mahjong-layer-x': `${tile.z * 5}px`,
            '--mahjong-layer-y': `${tile.z * -6}px`,
            zIndex: tile.z * 100 + tile.y * 2 + tile.x,
          } as CSSProperties
          return <button key={tile.id} type="button" style={tileStyle}
            className={`mahjong-tile${free ? ' free' : ' locked'}${selected ? ' selected' : ''}${hinted ? ' hinted' : ''}${tile.removed ? ' removed' : ''}`}
            disabled={tile.removed || !free || round.status !== 'playing'} onClick={() => onSelect(tile.id)}
            aria-pressed={selected} aria-label={`${cat?.name ?? 'Котик'}${free ? ', свободная фишка' : ', заблокированная фишка'}, слой ${tile.z + 1}`}>
            {cat && <img src={cat.image} alt="" draggable="false" />}
          </button>
        })}
      </div>
      <p className="match3-help">Свободная фишка не перекрыта сверху, а слева или справа от неё есть выход.</p>
      <p className="match3-mode-note">{mode === 'expert' ? 'Режим Эксперт: котификация увеличена.' : 'Ошибки не штрафуются, таймера нет, подсказки бесплатны.'}</p>
      {round.status === 'finished' && <div className="match3-result" role="dialog" aria-modal="true" aria-labelledby="mahjong-result-title">
        <div>
          <span className="eyebrow">Поле очищено</span>
          <h3 id="mahjong-result-title">Все коты найдены!</h3>
          <p>Найдено пар: <strong>{round.pairsFound}</strong></p>
          <p>Набрано очков: <strong>{round.score.toLocaleString('ru-RU')}</strong></p>
          <p className="match3-reward"><img src={fishIcon} alt="" /> Котифицировано: <strong>{reward.toLocaleString('ru-RU')} рыбок</strong></p>
          <div className="mahjong-result-actions">
            <button type="button" className="match3-primary" onClick={onPlayAgain}>Сыграть ещё</button>
            <button type="button" className="secondary-button" onClick={onExit}>Вернуться</button>
          </div>
        </div>
      </div>}
    </div>
  </section>
}
