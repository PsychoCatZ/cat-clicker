import { useEffect, useRef, useState, type CSSProperties, type PointerEvent } from 'react'
import type { Cat } from '../game/cats'
import type { RoomProgress } from '../game/economy'
import { constrainFurniturePoint, positionForLayout, visibleFurniture, type FurniturePoint, type SceneLayout } from '../game/furniture'
import type { Room } from '../game/rooms'
import type { Upgrade } from '../game/upgrades'

interface Props {
  room: Room
  cat: Cat
  progress: RoomProgress
  clickReward: number
  showDoor: boolean
  onClick: () => void
  onDoor: () => void
  onPlace: (id: string, layout: SceneLayout, point: FurniturePoint) => void
}

interface DragState { id: string; pointerId: number }
interface DraftPosition { id: string; point: FurniturePoint }

export function GameScene({ room, cat, progress, clickReward, showDoor, onClick, onDoor, onPlace }: Props) {
  const sleeping = progress.hunger <= 0
  const furniture = visibleFurniture(room.id, progress.boughtUpgrades)
  const [editing, setEditing] = useState(false)
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [layout, setLayout] = useState<SceneLayout>(() => window.matchMedia('(max-width: 650px)').matches ? 'mobile' : 'desktop')
  const [draft, setDraft] = useState<DraftPosition | null>(null)
  const sceneRef = useRef<HTMLDivElement>(null)
  const drag = useRef<DragState | null>(null)
  const selected = furniture.find((item) => item.id === selectedId) ?? null
  const unplaced = furniture.filter((item) => !positionForLayout(progress.furniturePositions[item.id], layout))
  const waitingText = unplaced.length === 1 ? '1 предмет ждёт места'
    : `${unplaced.length} ${unplaced.length < 5 ? 'предмета' : 'предметов'} ждут места`

  useEffect(() => {
    const media = window.matchMedia('(max-width: 650px)')
    const update = () => setLayout(media.matches ? 'mobile' : 'desktop')
    media.addEventListener('change', update)
    return () => media.removeEventListener('change', update)
  }, [])

  function pointAt(clientX: number, clientY: number, item: Upgrade): FurniturePoint | null {
    const rect = sceneRef.current?.getBoundingClientRect()
    if (!rect || clientX < rect.left || clientX > rect.right || clientY < rect.top || clientY > rect.bottom) return null
    return constrainFurniturePoint(item, layout, {
      x: (clientX - rect.left) / rect.width * 100,
      y: (clientY - rect.top) / rect.height * 100,
    })
  }

  function beginDrag(event: PointerEvent<HTMLElement>, item: Upgrade) {
    event.stopPropagation()
    setEditing(true)
    setSelectedId(item.id)
    drag.current = { id: item.id, pointerId: event.pointerId }
    event.currentTarget.setPointerCapture(event.pointerId)
    const point = pointAt(event.clientX, event.clientY, item)
    setDraft(point ? { id: item.id, point } : null)
  }

  function moveDrag(event: PointerEvent<HTMLElement>) {
    if (drag.current?.pointerId !== event.pointerId) return
    const item = furniture.find((entry) => entry.id === drag.current?.id)
    if (!item) return
    const point = pointAt(event.clientX, event.clientY, item)
    setDraft(point ? { id: item.id, point } : null)
  }

  function endDrag(event: PointerEvent<HTMLElement>) {
    if (drag.current?.pointerId !== event.pointerId) return
    const item = furniture.find((entry) => entry.id === drag.current?.id)
    const point = item && pointAt(event.clientX, event.clientY, item)
    if (item && point) onPlace(item.id, layout, point)
    drag.current = null
    setDraft(null)
  }

  function cancelDrag() {
    drag.current = null
    setDraft(null)
  }

  function toggleEditing() {
    setEditing((current) => !current)
    setSelectedId(editing ? null : unplaced[0]?.id ?? furniture[0]?.id ?? null)
    cancelDrag()
  }

  function furnitureStyle(item: Upgrade, point: FurniturePoint): CSSProperties {
    const { width, height, mobileHeight } = item.placement
    return {
      '--furniture-x': `${point.x}%`,
      '--furniture-y': `${point.y}%`,
      '--furniture-width': `${width}%`,
      '--furniture-height': `${height}%`,
      '--furniture-mobile-x': `${point.x}%`,
      '--furniture-mobile-y': `${point.y}%`,
      '--furniture-mobile-width': `${Math.min(35, width * 1.4)}%`,
      '--furniture-mobile-height': `${mobileHeight ?? height}%`,
    } as CSSProperties
  }

  return <section className="scene-wrap" aria-label={`Игровая комната: ${room.name}`}>
    <div ref={sceneRef} className={`scene${sleeping ? ' asleep' : ''}${editing ? ' editing' : ''}`}
      style={{ backgroundImage: `url(${sleeping ? room.night : room.day})` }}
      onPointerDown={(event) => { if (editing && selected) beginDrag(event, selected) }}
      onPointerMove={moveDrag} onPointerUp={endDrag} onPointerCancel={cancelDrag}>
      <div className="scene-shade" />
      {editing && selected && <div className={`placement-guide ${selected.surface}`} aria-hidden="true" />}
      {furniture.map((item) => {
        const point = draft?.id === item.id ? draft.point : positionForLayout(progress.furniturePositions[item.id], layout)
        if (!point) return null
        return <button key={item.id} type="button" tabIndex={editing ? 0 : -1} aria-hidden={!editing}
          className={`scene-furniture${editing ? ' movable' : ''}${selectedId === item.id ? ' selected' : ''}`}
          style={furnitureStyle(item, point)} aria-label={`Переместить: ${item.name}`}
          onPointerDown={(event) => { if (editing) beginDrag(event, item) }}
          onPointerMove={moveDrag} onPointerUp={endDrag} onPointerCancel={cancelDrag}
          onClick={() => { if (editing) setSelectedId(item.id) }}>
          <img src={item.image} alt="" draggable="false" />
        </button>
      })}
      <div className="scene-caption"><span className="scene-caption-label">Сейчас с вами</span><strong>{cat.name}</strong></div>
      <button className="cat-button" type="button" disabled={sleeping || editing} onClick={onClick}
        aria-label={sleeping ? `${cat.name} спит. Купите корм` : `Нажать на кота ${cat.name} и получить ${clickReward} рыбок`}>
        <img src={sleeping ? cat.sleepingImage : cat.image} alt={sleeping ? `${cat.name} спит` : cat.name} draggable="false" />
      </button>
      {showDoor && <button className="door-button" type="button" disabled={editing} onClick={onDoor} aria-label="Перейти в следующую комнату">
        <img src="/assets/ui/door/01.png" alt="" /><span>Следующая комната</span>
      </button>}
      <div className="scene-hint">{editing
        ? selected ? `${selected.surface === 'wall' ? 'Стена' : 'Пол'} · перетащите или коснитесь места` : 'Выберите предмет в панели ниже'
        : sleeping ? 'Кот уснул. Купите корм — пассивный доход остаётся' : 'Нажимайте на кота, чтобы собирать рыбок'}</div>
    </div>
    {furniture.length > 0 && <div className="furniture-panel">
      <div className="furniture-heading">
        <div><strong>Обставить комнату</strong><span>{unplaced.length ? waitingText : 'Все купленные предметы размещены'}</span></div>
        <button type="button" className="furniture-mode" onClick={toggleEditing}>{editing ? 'Готово' : 'Расставить'}</button>
      </div>
      <div className="furniture-tray" aria-label="Купленные предметы комнаты">
        {furniture.map((item) => {
          const placed = Boolean(positionForLayout(progress.furniturePositions[item.id], layout))
          return <button key={item.id} type="button" className={`furniture-chip${selectedId === item.id && editing ? ' selected' : ''}`}
            aria-pressed={editing && selectedId === item.id}
            onPointerDown={(event) => beginDrag(event, item)} onPointerMove={moveDrag} onPointerUp={endDrag} onPointerCancel={cancelDrag}
            onClick={() => { setEditing(true); setSelectedId(item.id) }}>
            <img src={item.image} alt="" draggable="false" />
            <span>{item.name}<small>{placed ? 'В комнате · можно двигать' : 'Ждёт места'}</small></span>
          </button>
        })}
      </div>
      {editing && <p className="furniture-help">Перетащите предмет в комнату или выберите его и коснитесь места. Кот не собирает рыбок во время расстановки.</p>}
    </div>}
  </section>
}
