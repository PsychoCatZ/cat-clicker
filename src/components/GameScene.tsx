import type { Cat } from '../game/cats'

interface Props {
  cat: Cat
  clickPower: number
  onClick: () => void
}

export function GameScene({ cat, clickPower, onClick }: Props) {
  return (
    <section className="scene-wrap" aria-label="Игровая комната">
      <div className="scene" style={{ backgroundImage: 'url(/assets/backgrounds/room-1.png)' }}>
        <div className="scene-shade" />
        <div className="scene-caption">
          <span className="scene-caption-label">Сейчас с вами</span>
          <strong>{cat.name}</strong>
        </div>
        <button className="cat-button" type="button" onClick={onClick} aria-label={`Погладить кота ${cat.name} и получить ${clickPower} рыбок`}>
          <img src={cat.image} alt={cat.name} draggable="false" />
        </button>
        <div className="scene-hint">Нажимайте на кота, чтобы собирать рыбок</div>
      </div>
    </section>
  )
}
