import { upgradesForRoom, type Upgrade } from './upgrades'

export type SceneLayout = 'desktop' | 'mobile'
export interface FurniturePoint { x: number; y: number }
export type FurniturePosition = Partial<Record<SceneLayout, FurniturePoint>>

export function visibleFurniture(roomId: number, boughtUpgrades: string[]): Upgrade[] {
  const owned = new Set(boughtUpgrades)
  const items = upgradesForRoom(roomId)
  return Array.from({ length: 5 }, (_, slot) =>
    items.find((item) => item.slot === slot && item.tier === 'advanced' && owned.has(item.id))
      ?? items.find((item) => item.slot === slot && item.tier === 'basic' && owned.has(item.id)),
  ).filter((item): item is Upgrade => Boolean(item))
}

export function defaultFurniturePosition(upgrade: Upgrade): FurniturePosition {
  const { x, y, mobileX, mobileY } = upgrade.placement
  return {
    desktop: { x, y },
    mobile: { x: Math.max(19, Math.min(81, mobileX ?? x)), y: mobileY ?? y },
  }
}

export function positionForLayout(position: FurniturePosition | undefined, layout: SceneLayout): FurniturePoint | null {
  return position?.[layout] ?? position?.[layout === 'desktop' ? 'mobile' : 'desktop'] ?? null
}

export function constrainFurniturePoint(upgrade: Upgrade, layout: SceneLayout, point: FurniturePoint): FurniturePoint | null {
  if (!Number.isFinite(point.x) || !Number.isFinite(point.y)) return null
  const width = layout === 'mobile' ? Math.min(35, upgrade.placement.width * 1.4) : upgrade.placement.width
  const height = layout === 'mobile' ? upgrade.placement.mobileHeight ?? upgrade.placement.height : upgrade.placement.height
  const edge = width / 2 + 2
  const minimumY = upgrade.surface === 'wall' ? Math.max(17, height / 2 + 4) : 57
  const maximumY = upgrade.surface === 'wall' ? Math.min(49, 55 - height / 2) : 97 - height / 2
  const clamp = (value: number, min: number, max: number) => Math.round(Math.max(min, Math.min(max, value)) * 10) / 10
  return { x: clamp(point.x, edge, 100 - edge), y: clamp(point.y, minimumY, maximumY) }
}
