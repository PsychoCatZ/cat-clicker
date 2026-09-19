import { useEffect, useReducer, useState } from 'react'
import { CatCollection } from './components/CatCollection'
import { GameScene } from './components/GameScene'
import { Navigation, type Page } from './components/Navigation'
import { ResourceBar } from './components/ResourceBar'
import { Shop } from './components/Shop'
import { cats } from './game/cats'
import { gameReducer } from './game/economy'
import { loadGame, saveGame } from './game/save'

function App() {
  const [state, dispatch] = useReducer(gameReducer, undefined, loadGame)
  const [page, setPage] = useState<Page>('shop')

  useEffect(() => {
    saveGame(state)
  }, [state])

  useEffect(() => {
    const timer = window.setInterval(() => dispatch({ type: 'tick' }), 1000)
    return () => window.clearInterval(timer)
  }, [])

  const currentCat = cats.find((cat) => cat.id === state.selectedCat) ?? cats[0]

  function resetProgress() {
    if (window.confirm('Полностью сбросить рыбок, улучшения и коллекцию котов?')) {
      dispatch({ type: 'reset' })
      setPage('shop')
    }
  }

  return (
    <main className="app-shell">
      <ResourceBar state={state} onReset={resetProgress} />
      <GameScene cat={currentCat} clickPower={state.clickPower} onClick={() => dispatch({ type: 'click' })} />
      <div className="lower-panel">
        <Navigation page={page} onChange={setPage} />
        {page === 'shop'
          ? <Shop state={state} onBuy={(id) => dispatch({ type: 'buyUpgrade', id })} />
          : <CatCollection state={state} onBuy={(id) => dispatch({ type: 'buyCat', id })} onSelect={(id) => dispatch({ type: 'selectCat', id })} />}
      </div>
      <footer>Маленькая комната, большие кошачьи планы</footer>
    </main>
  )
}

export default App
