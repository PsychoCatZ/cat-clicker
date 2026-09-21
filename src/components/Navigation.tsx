export type Page = 'upgrades' | 'resources' | 'food' | 'cats' | 'minigames' | 'match3' | 'pairs' | 'mahjong' | 'sliding'

interface Props {
  page: Page
  onChange: (page: Page) => void
}

const pages: { id: Exclude<Page, 'match3' | 'pairs' | 'mahjong' | 'sliding'>; name: string; icon: string }[] = [
  { id: 'upgrades', name: 'Улучшения', icon: '/assets/ui/02.png' },
  { id: 'resources', name: 'Ресурсы', icon: '/assets/resources/02.png' },
  { id: 'food', name: 'Корм', icon: '/assets/food/02.png' },
  { id: 'cats', name: 'Коты', icon: '/assets/ui/03.png' },
  { id: 'minigames', name: 'Мини-игры', icon: '/assets/ui/04.png' },
]

export function Navigation({ page, onChange }: Props) {
  return (
    <nav className="navigation" aria-label="Разделы игры">
      {pages.map((item) => {
        const active = page === item.id || (item.id === 'minigames' && (page === 'match3' || page === 'pairs' || page === 'mahjong' || page === 'sliding'))
        return <button key={item.id} type="button" className={active ? 'nav-button active' : 'nav-button'}
        onClick={() => onChange(item.id)} aria-current={active ? 'page' : undefined}>
        <img src={item.icon} alt="" /> {item.name}
      </button>})}
    </nav>
  )
}
