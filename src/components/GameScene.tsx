import type { CSSProperties } from 'react'
import type { Cat } from '../game/cats'
import type { RoomProgress } from '../game/economy'
import type { Room } from '../game/rooms'
import type { Upgrade } from '../game/upgrades'

interface Props {
  room: Room
  cat: Cat
  progress: RoomProgress
  upgrades: Upgrade[]
  clickReward: number
  showDoor: boolean
  onClick: () => void
  onDoor: () => void
}

export function GameScene({ room, cat, progress, upgrades, clickReward, showDoor, onClick, onDoor }: Props) {
  const sleeping = progress.hunger <= 0
  const furniture = Array.from({ length: 5 }, (_, slot) => {
    const advanced = upgrades.find((item) => item.slot === slot && item.tier === 'advanced' && progress.boughtUpgrades.includes(item.id))
    return advanced ?? upgrades.find((item) => item.slot === slot && item.tier === 'basic' && progress.boughtUpgrades.includes(item.id))
  })
  return (
    <section className="scene-wrap" aria-label={`Игровая комната: ${room.name}`}>
      <div className={sleeping ? 'scene asleep' : 'scene'} style={{ backgroundImage: `url(${sleeping ? room.night : room.day})` }}>
        <div className="scene-shade" />
        {furniture.map((item) => {
          if (!item) return null
          const { x, y, width, height, mobileX, mobileY, mobileHeight } = item.placement
          const style = {
            '--furniture-x': `${x}%`,
            '--furniture-y': `${y}%`,
            '--furniture-width': `${width}%`,
            '--furniture-height': `${height}%`,
            '--furniture-mobile-x': `${Math.max(19, Math.min(81, mobileX ?? x))}%`,
            '--furniture-mobile-y': `${mobileY ?? y}%`,
            '--furniture-mobile-width': `${Math.min(35, width * 1.4)}%`,
            '--furniture-mobile-height': `${mobileHeight ?? height}%`,
          } as CSSProperties
          return <img key={item.id} className="scene-furniture" style={style} src={item.image} alt={item.name} draggable="false" />
        })}
        <div className="scene-caption"><span className="scene-caption-label">Сейчас с вами</span><strong>{cat.name}</strong></div>
        <button className="cat-button" type="button" disabled={sleeping} onClick={onClick}
          aria-label={sleeping ? `${cat.name} спит. Купите корм` : `Нажать на кота ${cat.name} и получить ${clickReward} рыбок`}>
          <img src={sleeping ? cat.sleepingImage : cat.image} alt={sleeping ? `${cat.name} спит` : cat.name} draggable="false" />
        </button>
        {showDoor && <button className="door-button" type="button" onClick={onDoor} aria-label="Перейти в следующую комнату">
          <img src="/assets/ui/door/01.png" alt="" /><span>Следующая комната</span>
        </button>}
        <div className="scene-hint">{sleeping ? 'Кот уснул. Купите корм — пассивный доход остаётся' : 'Нажимайте на кота, чтобы собирать рыбок'}</div>
      </div>
    </section>
  )
}
