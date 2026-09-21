import { useCallback, useEffect, useReducer, useState } from 'react'
import { CatCollection } from './components/CatCollection'
import { FoodShop } from './components/FoodShop'
import { GameScene } from './components/GameScene'
import { Match3Game } from './components/Match3Game'
import { MiniGamesHub } from './components/MiniGamesHub'
import { Navigation, type Page } from './components/Navigation'
import { PairsGame } from './components/PairsGame'
import { ResourceBar } from './components/ResourceBar'
import { ResourceShop } from './components/ResourceShop'
import { Shop } from './components/Shop'
import { cats, catsForRoom } from './game/cats'
import { activeProgress, basicUpgradesBought, catCost, currentClickReward, foodCost, gameReducer, resourceCost, roomComplete, upgradeCost } from './game/economy'
import { foods, resources } from './game/items'
import { rooms } from './game/rooms'
import { loadGame, saveGame } from './game/save'
import { upgradesForRoom } from './game/upgrades'
import { match3FishReward } from './game/match3/scoring'
import { pairsFishReward } from './game/pairs/scoring'

const formatAwayTime = (seconds: number): string => {
  const minutes = Math.max(1, Math.floor(seconds / 60))
  if (minutes < 60) return `${minutes} мин`
  const hours = Math.floor(minutes / 60)
  const rest = minutes % 60
  return rest ? `${hours} ч ${rest} мин` : `${hours} ч`
}

function App() {
  const [state, dispatch] = useReducer(gameReducer, undefined, loadGame)
  const [page, setPage] = useState<Page>(() => state.match3.activeRound ? 'match3' : state.pairs.activeRound ? 'pairs' : 'upgrades')
  const [notice, setNotice] = useState('')
  const hidePairMismatch = useCallback(() => dispatch({ type: 'pairsHideMismatch' }), [])
  const progress = activeProgress(state)
  const room = rooms[state.currentRoom - 1]
  const cat = cats.find((item) => item.id === progress.selectedCat) ?? catsForRoom(state.currentRoom)[0]
  const finished = state.currentRoom === rooms.length && roomComplete(state)

  useEffect(() => { saveGame(state) }, [state])
  useEffect(() => {
    const timer = window.setInterval(() => dispatch({ type: 'tick', seconds: 1 }), 1000)
    return () => window.clearInterval(timer)
  }, [])
  useEffect(() => {
    if (!notice) return
    const timer = window.setTimeout(() => setNotice(''), 2500)
    return () => window.clearTimeout(timer)
  }, [notice])

  function resetProgress() {
    if (window.confirm('Полностью сбросить все комнаты, рыбок, котов, ресурсы, улучшения и мини-игры?')) {
      dispatch({ type: 'reset' })
      setPage('upgrades')
      setNotice('Прогресс сброшен')
    }
  }

  function finishMatch3(nextPage: Page = 'minigames') {
    const round = state.match3.activeRound
    const reward = round ? match3FishReward(round.score, round.roomId, round.mode) : 0
    if (round) dispatch({ type: 'settleMatch3' })
    setPage(nextPage)
    if (round) setNotice(reward > 0 ? `Котификация: +${reward.toLocaleString('ru-RU')} рыбок` : 'Раунд завершён')
  }

  function finishPairs(nextPage: Page = 'minigames') {
    const round = state.pairs.activeRound
    const reward = round ? pairsFishReward(round.matches, round.attempts, round.cardCount / 2, round.roomId, round.mode, round.status === 'finished') : 0
    if (round) dispatch({ type: 'settlePairs' })
    setPage(nextPage)
    if (round) setNotice(reward > 0 ? `Котификация: +${reward.toLocaleString('ru-RU')} рыбок` : 'Раунд завершён')
  }

  function changePage(nextPage: Page) {
    if (page === 'match3' && nextPage !== 'match3') finishMatch3(nextPage)
    else if (page === 'pairs' && nextPage !== 'pairs') finishPairs(nextPage)
    else setPage(nextPage)
  }

  function visitRoom(roomId: number) {
    const round = state.match3.activeRound
    const pairsRound = state.pairs.activeRound
    const reward = round ? match3FishReward(round.score, round.roomId, round.mode)
      : pairsRound ? pairsFishReward(pairsRound.matches, pairsRound.attempts, pairsRound.cardCount / 2, pairsRound.roomId, pairsRound.mode, pairsRound.status === 'finished') : 0
    if (round) dispatch({ type: 'settleMatch3' })
    if (pairsRound) dispatch({ type: 'settlePairs' })
    dispatch({ type: 'visitRoom', roomId })
    setPage('upgrades')
    if (round || pairsRound) setNotice(reward > 0 ? `Котификация: +${reward.toLocaleString('ru-RU')} рыбок` : 'Раунд завершён')
  }

  function buyResource(id: string) {
    const item = resources.find((value) => value.id === id)
    if (!item || progress.fish < resourceCost(state, id)) return
    dispatch({ type: 'buyResource', id })
    setNotice(`${item.name}: доход за клик увеличен`)
  }

  function buyUpgrade(id: string) {
    const item = upgradesForRoom(state.currentRoom).find((value) => value.id === id)
    if (!item || progress.boughtUpgrades.includes(id) || progress.fish < upgradeCost(state, item)
      || (item.tier === 'advanced' && !basicUpgradesBought(state))) return
    dispatch({ type: 'buyUpgrade', id })
    setNotice(`Улучшение куплено: ${item.name}. Предмет ждёт места под комнатой`)
  }

  function buyFood(id: string) {
    const item = foods.find((value) => value.id === id)
    if (!item || progress.fish < foodCost(state, item.baseCost) || (progress.hunger >= 100 && !item.boostSeconds)) return
    dispatch({ type: 'buyFood', id })
    setNotice(`${item.name}: кот сыт`)
  }

  function buyCat(id: string) {
    const item = catsForRoom(state.currentRoom).find((value) => value.id === id)
    if (!item || progress.unlockedCats.includes(id) || progress.fish < catCost(state, item.baseCost)) return
    dispatch({ type: 'buyCat', id })
    setNotice(`Кот открыт: ${item.name}`)
  }

  function startExpert() {
    if (window.confirm('Начать режим «Эксперт»? Все комнаты, коты, рыбки, ресурсы и улучшения будут сброшены.')) {
      dispatch({ type: 'startExpert' })
      setPage('upgrades')
      setNotice('Режим «Эксперт» начался')
    }
  }

  const offlineNotice = state.offlineReport && <aside className="offline-report" role="status">
    <div>
      <strong>Котики ждали вас {formatAwayTime(state.offlineReport.elapsedSeconds)}</strong>
      <span>Пассивный доход: +{Math.floor(state.offlineReport.fishEarned).toLocaleString('ru-RU')} рыбок · Сытость: −{state.offlineReport.hungerSpent.toFixed(1)}%</span>
      {state.offlineReport.elapsedSeconds > state.offlineReport.creditedSeconds && <small>Доход и расход сытости учтены максимум за 8 часов.</small>}
    </div>
    <button type="button" onClick={() => dispatch({ type: 'dismissOfflineReport' })} aria-label="Закрыть сводку">Понятно</button>
  </aside>

  if (page === 'match3' && state.match3.activeRound) return <main className="app-shell mini-game-focus-shell">
    <Match3Game room={room} mode={state.mode} round={state.match3.activeRound}
      onStart={() => undefined}
      onSwap={(first, second) => dispatch({ type: 'match3Swap', first, second })}
      onExit={() => finishMatch3()} />
    {offlineNotice}
    {notice && <div className="notice" role="status">{notice}</div>}
  </main>

  if (page === 'pairs' && state.pairs.activeRound) return <main className="app-shell mini-game-focus-shell">
    <PairsGame room={room} mode={state.mode} round={state.pairs.activeRound}
      onReveal={(cardId) => dispatch({ type: 'pairsReveal', cardId })}
      onHideMismatch={hidePairMismatch}
      onExit={() => finishPairs()} />
    {offlineNotice}
    {notice && <div className="notice" role="status">{notice}</div>}
  </main>

  return <main className="app-shell">
    <ResourceBar state={state} onReset={resetProgress} onToggleLights={() => dispatch({ type: 'toggleLights' })} />
    <nav className="room-tabs" aria-label="Комнаты">
      {rooms.map((item) => <button key={item.id} type="button" disabled={item.id > state.unlockedRoom}
        className={item.id === state.currentRoom ? 'room-tab active' : 'room-tab'}
        onClick={() => visitRoom(item.id)}>
        <span>Комната {item.id}</span><strong>{item.name}</strong>
      </button>)}
    </nav>
    <GameScene key={`${state.mode}-${room.id}`} room={room} cat={cat} progress={progress}
      clickReward={currentClickReward(state)} showDoor={roomComplete(state) && state.currentRoom < rooms.length}
      onClick={() => dispatch({ type: 'click' })} onDoor={() => { dispatch({ type: 'enterNextRoom' }); setPage('upgrades'); setNotice('Новая комната открыта') }}
      onPlace={(id, layout, point) => dispatch({ type: 'placeFurniture', id, layout, ...point })} />
    <div className="lower-panel">
      <Navigation page={page} onChange={changePage} />
      {page === 'upgrades' && <Shop state={state} onBuy={buyUpgrade} />}
      {page === 'resources' && <ResourceShop state={state} onBuy={buyResource} />}
      {page === 'food' && <FoodShop state={state} onBuy={buyFood} />}
      {page === 'cats' && <CatCollection state={state} onBuy={buyCat} onSelect={(id) => dispatch({ type: 'selectCat', id })} />}
      {page === 'minigames' && <MiniGamesHub room={room}
        onStartMatch3={() => { dispatch({ type: 'startMatch3', seed: Date.now() }); setPage('match3') }}
        onStartPairs={(cardCount) => { dispatch({ type: 'startPairs', seed: Date.now(), cardCount }); setPage('pairs') }} />}
    </div>
    {finished && state.finalDismissed && <button type="button" className="final-reopen" onClick={() => dispatch({ type: 'showFinal' })}>Все коты спасены · Финал</button>}
    <footer>Пять комнат · двадцать пять котов · одна большая Котификация</footer>
    {notice && <div className="notice" role="status">{notice}</div>}
    {offlineNotice}
    {finished && !state.finalDismissed && <div className="final-backdrop" role="dialog" aria-modal="true" aria-label="Все коты спасены">
      <div className="final-card">
        <img src="/assets/ui/final.png" alt="Котификация началась! Все коты собраны" />
        <div className="final-actions">
          <p>Все пять комнат обустроены, все 25 котов спасены.</p>
          {state.mode === 'normal' && <button type="button" className="buy-button" onClick={startExpert}>Начать режим «Эксперт»</button>}
          <button type="button" className="secondary-button" onClick={() => dispatch({ type: 'dismissFinal' })}>Вернуться в комнаты</button>
        </div>
      </div>
    </div>}
  </main>
}

export default App
