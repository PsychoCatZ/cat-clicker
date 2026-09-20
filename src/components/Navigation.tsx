export type Page = 'upgrades' | 'resources' | 'food' | 'cats'

interface Props {
  page: Page
  onChange: (page: Page) => void
}

const pages: { id: Page; name: string; icon: string }[] = [
  { id: 'upgrades', name: 'Улучшения', icon: '/assets/ui/02.png' },
  { id: 'resources', name: 'Ресурсы', icon: '/assets/resources/02.png' },
  { id: 'food', name: 'Корм', icon: '/assets/food/02.png' },
  { id: 'cats', name: 'Коты', icon: '/assets/ui/03.png' },
]

export function Navigation({ page, onChange }: Props) {
  return (
    <nav className="navigation" aria-label="Разделы игры">
      {pages.map((item) => <button key={item.id} type="button" className={page === item.id ? 'nav-button active' : 'nav-button'}
        onClick={() => onChange(item.id)} aria-current={page === item.id ? 'page' : undefined}>
        <img src={item.icon} alt="" /> {item.name}
      </button>)}
    </nav>
  )
}
