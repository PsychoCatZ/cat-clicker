export type Page = 'shop' | 'cats'

interface Props {
  page: Page
  onChange: (page: Page) => void
}

export function Navigation({ page, onChange }: Props) {
  return (
    <nav className="navigation" aria-label="Разделы игры">
      <button type="button" className={page === 'shop' ? 'nav-button active' : 'nav-button'} onClick={() => onChange('shop')} aria-current={page === 'shop' ? 'page' : undefined}>
        <img src="/assets/ui/02.png" alt="" /> Магазин
      </button>
      <button type="button" className={page === 'cats' ? 'nav-button active' : 'nav-button'} onClick={() => onChange('cats')} aria-current={page === 'cats' ? 'page' : undefined}>
        <img src="/assets/ui/03.png" alt="" /> Коты
      </button>
    </nav>
  )
}
