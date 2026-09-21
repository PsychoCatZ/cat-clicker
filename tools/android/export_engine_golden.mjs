// Records scripted and randomised play sessions of the web reducer so the Kotlin engine can replay them
// and compare the resulting state. Usage (repo root, after `npm install`):
//   node tools/android/export_engine_golden.mjs
// Output: android/game/src/test/resources/golden/engine.json
//
// Step formats:
//   { a: <GameAction> }      -> reducer(state, action)
//   { fund: n }              -> set the current room's fish to n (test helper, like `fund` in check_game.mjs)
//   { offline: seconds }     -> applyOfflineProgress(state, seconds)
// `snap` (optional) is the expected state after the step.
import { mkdirSync, writeFileSync } from 'node:fs'
import { build } from 'esbuild'

const bundled = await build({
  stdin: {
    contents: "export * from './src/game/economy.ts'; export * from './src/game/cats.ts'; export * from './src/game/upgrades.ts'; export * from './src/game/items.ts'; export * from './src/game/rooms.ts';",
    resolveDir: process.cwd(),
    sourcefile: 'engine-golden.ts',
  },
  bundle: true, platform: 'node', format: 'esm', write: false,
})
const game = await import(`data:text/javascript;base64,${Buffer.from(bundled.outputFiles[0].contents).toString('base64')}`)

const snapshot = (state) => ({
  mode: state.mode,
  currentRoom: state.currentRoom,
  unlockedRoom: state.unlockedRoom,
  finalDismissed: state.finalDismissed,
  offlineReport: state.offlineReport,
  rooms: state.rooms.map((room) => ({
    fish: room.fish,
    hunger: room.hunger,
    lightsOff: room.lightsOff,
    resourceLevels: room.resourceLevels,
    boughtUpgrades: room.boughtUpgrades,
    furniturePositions: room.furniturePositions,
    unlockedCats: room.unlockedCats,
    selectedCat: room.selectedCat,
    caviarSeconds: room.caviarSeconds,
  })),
})

const setFish = (state, amount) => ({
  ...state,
  rooms: state.rooms.map((room, index) => index === state.currentRoom - 1 ? { ...room, fish: amount } : room),
})

function apply(state, step) {
  if ('a' in step) return game.gameReducer(state, step.a)
  if ('fund' in step) return setFish(state, step.fund)
  if ('offline' in step) return game.applyOfflineProgress(state, step.offline)
  throw new Error('Unknown step')
}

function record(name, steps, every) {
  let state = game.initialState()
  const out = steps.map((step, index) => {
    state = apply(state, step)
    return every === 1 || index % every === 0 || index === steps.length - 1 ? { ...step, snap: snapshot(state) } : step
  })
  return { name, steps: out }
}

// --- Scripted scenario mirroring tools/check_game.mjs ---
const scripted = [
  { a: { type: 'click' } },
  { fund: 15 }, { a: { type: 'buyResource', id: 'fish' } }, { a: { type: 'buyResource', id: 'fish' } },
  { fund: 1e9 }, { a: { type: 'buyResource', id: 'fish' } }, { a: { type: 'buyResource', id: 'nonsense' } },
  { a: { type: 'buyUpgrade', id: 'room-1-advanced-1' } },
  { a: { type: 'buyUpgrade', id: 'room-1-basic-1' } }, { a: { type: 'buyUpgrade', id: 'room-1-basic-1' } },
  { a: { type: 'placeFurniture', id: 'room-1-basic-1', layout: 'desktop', x: 50, y: 10 } },
  { a: { type: 'placeFurniture', id: 'room-1-basic-1', layout: 'mobile', x: 90, y: 90 } },
  { a: { type: 'placeFurniture', id: 'room-1-advanced-2', layout: 'desktop', x: 30, y: 30 } },
  { a: { type: 'tick', seconds: 1 } }, { a: { type: 'tick', seconds: 5 } }, { a: { type: 'tick', seconds: 0 } },
  { a: { type: 'toggleLights' } }, { a: { type: 'click' } }, { a: { type: 'tick', seconds: 1 } },
  { a: { type: 'toggleLights' } }, { a: { type: 'tick', seconds: 1 } }, { a: { type: 'click' } },
  { a: { type: 'buyCat', id: 'basic-02' } }, { a: { type: 'selectCat', id: 'basic-01' } }, { a: { type: 'selectCat', id: 'basic-05' } },
  { a: { type: 'buyFood', id: 'mouse' } }, { a: { type: 'buyFood', id: 'caviar' } }, { a: { type: 'tick', seconds: 1.5 } },
  { a: { type: 'click' } },
  { offline: 30 }, { offline: 3600 }, { offline: 99999 }, { a: { type: 'dismissOfflineReport' } },
  { a: { type: 'visitRoom', roomId: 2 } }, { a: { type: 'enterNextRoom' } },
  { a: { type: 'showFinal' } }, { a: { type: 'dismissFinal' } }, { a: { type: 'startExpert' } },
  { a: { type: 'reset' } },
]
// Empty-hunger fallback income: half an hour of one-second ticks with nothing bought.
const sleeping = [{ a: { type: 'reset' } }]
for (let i = 0; i < 90; i += 1) sleeping.push({ offline: 60 })
for (let i = 0; i < 1900; i += 1) sleeping.push({ a: { type: 'tick', seconds: 1 } })
sleeping.push({ a: { type: 'buyFood', id: 'mouse' } })

// --- Completionist: buys everything, walks through all five rooms, then starts Expert ---
function completionist() {
  const steps = []
  for (let room = 1; room <= 5; room += 1) {
    steps.push({ fund: 1e13 })
    for (const resource of game.resources) for (let i = 0; i < 3; i += 1) steps.push({ a: { type: 'buyResource', id: resource.id } })
    for (const food of game.foods) steps.push({ a: { type: 'buyFood', id: food.id } })
    for (const cat of game.catsForRoom(room)) steps.push({ fund: 1e13 }, { a: { type: 'buyCat', id: cat.id } })
    for (const tier of ['basic', 'advanced']) {
      for (const upgrade of game.upgradesForRoom(room).filter((item) => item.tier === tier)) {
        steps.push({ fund: 1e13 }, { a: { type: 'buyUpgrade', id: upgrade.id } })
        steps.push({ a: { type: 'placeFurniture', id: upgrade.id, layout: room % 2 ? 'desktop' : 'mobile', x: 40 + room, y: 70 } })
      }
    }
    steps.push({ a: { type: 'tick', seconds: 2 } }, { a: { type: 'enterNextRoom' } })
    if (room < 5) steps.push({ a: { type: 'visitRoom', roomId: room } }, { a: { type: 'visitRoom', roomId: room + 1 } })
  }
  steps.push({ a: { type: 'dismissFinal' } }, { a: { type: 'showFinal' } }, { a: { type: 'startExpert' } })
  steps.push({ fund: 1e13 }, { a: { type: 'buyUpgrade', id: 'room-1-basic-1' } }, { a: { type: 'tick', seconds: 2 } })
  return steps
}

// --- Randomised bots (xorshift32, so the recording is reproducible) ---
function bot(seed, count) {
  let rng = seed >>> 0 || 1
  const next = () => {
    rng ^= rng << 13; rng >>>= 0
    rng ^= rng >>> 17
    rng ^= rng << 5; rng >>>= 0
    return rng / 0x100000000
  }
  const pick = (list) => list[Math.floor(next() * list.length)]
  const resourceIds = game.resources.map((item) => item.id).concat('bogus')
  const foodIds = game.foods.map((item) => item.id).concat('bogus')
  const upgradeIds = game.upgrades.map((item) => item.id)
  const catIds = game.cats.map((item) => item.id)
  const seconds = [0, 0.25, 0.5, 1, 1, 1, 1.5, 2, 2, 3, 10, -1]
  const funds = [0, 10, 100, 1e3, 1e4, 1e5, 1e6, 1e8, 1e10, 1e12, 5e14]

  let state = game.initialState()
  const steps = []
  for (let i = 0; i < count; i += 1) {
    const roll = next()
    let step
    if (roll < 0.28) step = { a: { type: 'click' } }
    else if (roll < 0.5) step = { a: { type: 'tick', seconds: pick(seconds) } }
    else if (roll < 0.56) step = { fund: pick(funds) }
    else if (roll < 0.62) step = { a: { type: 'buyResource', id: pick(resourceIds) } }
    else if (roll < 0.68) step = { a: { type: 'buyFood', id: pick(foodIds) } }
    else if (roll < 0.76) {
      // Smart buy: first affordable cat or upgrade of the current room, so the bot actually progresses.
      const room = state.currentRoom
      const wanted = [
        ...game.catsForRoom(room).map((item) => ({ type: 'buyCat', id: item.id })),
        ...game.upgradesForRoom(room).map((item) => ({ type: 'buyUpgrade', id: item.id })),
      ]
      step = { a: pick(wanted) }
    } else if (roll < 0.80) step = { a: { type: 'buyUpgrade', id: pick(upgradeIds) } }
    else if (roll < 0.83) step = { a: { type: 'buyCat', id: pick(catIds) } }
    else if (roll < 0.85) step = { a: { type: 'selectCat', id: pick(catIds) } }
    else if (roll < 0.88) step = { a: { type: 'toggleLights' } }
    else if (roll < 0.92) {
      step = { a: { type: 'placeFurniture', id: pick(upgradeIds), layout: pick(['desktop', 'mobile', 'sideways']), x: next() * 110 - 5, y: next() * 110 - 5 } }
    } else if (roll < 0.94) step = { a: { type: 'visitRoom', roomId: 1 + Math.floor(next() * 6) } }
    else if (roll < 0.96) step = { a: { type: 'enterNextRoom' } }
    else if (roll < 0.98) step = { offline: pick([0.5, 45, 600, 7200, 40000, 100000]) }
    else if (roll < 0.985) step = { a: { type: pick(['dismissFinal', 'showFinal', 'dismissOfflineReport']) } }
    else if (roll < 0.9875) step = { a: { type: 'startExpert' } }
    else if (roll < 0.988) step = { a: { type: 'reset' } }
    else step = { a: { type: 'click' } }
    state = apply(state, step)
    steps.push(step)
  }
  return steps
}

const scenarios = [
  record('scripted', scripted, 1),
  record('fallback-income', sleeping, 25),
  record('completionist', completionist(), 3),
  ...[11, 2027, 90210, 424242].map((seed) => record(`bot-${seed}`, bot(seed, 3000), 20)),
]

const outDir = 'android/game/src/test/resources/golden'
mkdirSync(outDir, { recursive: true })
writeFileSync(`${outDir}/engine.json`, JSON.stringify({ scenarios }))
for (const scenario of scenarios) {
  const last = scenario.steps.at(-1).snap
  console.log(`${scenario.name}: ${scenario.steps.length} steps, final room ${last.currentRoom}/${last.unlockedRoom}, mode ${last.mode}`)
}
