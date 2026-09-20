import assert from 'node:assert/strict'
import { existsSync } from 'node:fs'
import { join } from 'node:path'
import { build } from 'esbuild'

const bundled = await build({
  stdin: {
    contents: "export * from './src/game/economy.ts'; export * from './src/game/cats.ts'; export * from './src/game/upgrades.ts'; export * from './src/game/items.ts'; export * from './src/game/rooms.ts'; export * from './src/game/save.ts';",
    resolveDir: process.cwd(),
    sourcefile: 'verification.ts',
  },
  bundle: true,
  platform: 'node',
  format: 'esm',
  write: false,
})
const game = await import(`data:text/javascript;base64,${Buffer.from(bundled.outputFiles[0].contents).toString('base64')}`)
const reduce = (state, action) => game.gameReducer(state, action)
const fund = (state, amount = 1e9) => ({
  ...state,
  rooms: state.rooms.map((room, index) => index === state.currentRoom - 1 ? { ...room, fish: amount } : room),
})

for (const image of [
  ...game.cats.flatMap((cat) => [cat.image, cat.sleepingImage]),
  ...game.upgrades.map((item) => item.image),
  ...game.resources.map((item) => item.image),
  ...game.foods.map((item) => item.image),
  ...game.rooms.flatMap((room) => [room.day, room.night]),
  ...Array.from({ length: 5 }, (_, index) => `/assets/ui/${String(index + 1).padStart(2, '0')}.png`),
  '/assets/ui/door/01.png', '/assets/ui/final.png',
]) {
  assert.ok(existsSync(join('public', image.slice(1))), `Missing asset: ${image}`)
}

let state = game.initialState()
assert.equal(game.clickPower(state), 1)
assert.equal(game.activeProgress(state).hunger, 100)
state = reduce(state, { type: 'click' })
assert.equal(game.activeProgress(state).fish, 1)
state = fund(state, 15)
state = reduce(state, { type: 'buyResource', id: 'fish' })
assert.equal(game.clickPower(state), 2)
assert.ok(game.resourceCost(state, 'fish') > 15)
state = fund(state, game.resourceCost(state, 'fish'))
state = reduce(state, { type: 'buyResource', id: 'fish' })
assert.equal(game.clickPower(state), 3, 'each resource level adds a fixed bonus')
state = reduce(state, { type: 'buyUpgrade', id: 'room-1-advanced-1' })
assert.equal(game.activeProgress(state).boughtUpgrades.length, 0)

let sleeping = game.initialState()
sleeping.rooms[0].hunger = 0
assert.equal(reduce(sleeping, { type: 'click' }), sleeping)
for (let i = 0; i < 1800; i++) sleeping = reduce(sleeping, { type: 'tick', seconds: 1 })
assert.ok(Math.abs(game.activeProgress(sleeping).fish - 60) < 0.1, 'mouse fallback takes about 30 minutes')
sleeping = reduce(sleeping, { type: 'buyFood', id: 'mouse' })
assert.ok(game.activeProgress(sleeping).hunger >= 25)
assert.ok(game.activeProgress(sleeping).fish < 1)
sleeping = fund(sleeping, 1000)
sleeping = reduce(sleeping, { type: 'buyFood', id: 'caviar' })
assert.equal(game.activeProgress(sleeping).hunger, 100)
assert.equal(game.currentClickReward(sleeping), 2)
for (let i = 0; i < 60; i++) sleeping = reduce(sleeping, { type: 'tick', seconds: 1 })
assert.equal(game.currentClickReward(sleeping), 1)

state = game.initialState()
for (let roomId = 1; roomId <= 5; roomId++) {
  assert.equal(state.currentRoom, roomId)
  assert.equal(game.clickPower(state), 1, 'new room has local click power')
  assert.equal(game.activeProgress(state).fish, 0, 'new room has local fish')
  assert.equal(game.activeProgress(state).hunger, 100)
  for (const upgrade of game.upgradesForRoom(roomId)) {
    state = fund(state)
    state = reduce(state, { type: 'buyUpgrade', id: upgrade.id })
  }
  for (const cat of game.catsForRoom(roomId)) {
    state = fund(state)
    state = reduce(state, { type: 'buyCat', id: cat.id })
  }
  assert.equal(game.roomComplete(state), true)
  if (roomId < 5) state = reduce(state, { type: 'enterNextRoom' })
}
assert.equal(state.finalDismissed, false)
state = reduce(state, { type: 'visitRoom', roomId: 1 })
assert.equal(game.roomComplete(state), true, 'earlier room state is retained')
state = reduce(state, { type: 'visitRoom', roomId: 5 })

const saved = new Map()
globalThis.localStorage = {
  getItem: (key) => saved.get(key) ?? null,
  setItem: (key, value) => saved.set(key, value),
}
game.saveGame(state)
assert.deepEqual(game.loadGame(), state)
const staleBoost = game.initialState()
staleBoost.rooms[0].caviarSeconds = 60
saved.set('cat-clicker-save-v2', JSON.stringify({ ...staleBoost, savedAt: Date.now() - 61000 }))
assert.equal(game.loadGame().rooms[0].caviarSeconds, 0, 'caviar expires while page is closed')
saved.delete('cat-clicker-save-v2')
saved.set('cat-clicker-save-v1', JSON.stringify({
  fish: 123, upgradeLevels: { bowl: 2, bed: 1 },
  unlockedCats: ['ryzhik', 'ugolyok'], selectedCat: 'ugolyok',
}))
const migrated = game.loadGame()
assert.equal(migrated.rooms[0].fish, 123)
assert.equal(migrated.rooms[0].selectedCat, 'basic-02')
assert.equal(migrated.rooms[0].resourceLevels.fish, 2)
state = reduce(state, { type: 'startExpert' })
assert.equal(state.mode, 'expert')
assert.equal(state.currentRoom, 1)
assert.equal(state.unlockedRoom, 1)
assert.equal(game.activeProgress(state).fish, 0)
assert.equal(game.activeProgress(state).unlockedCats.length, 1)
state = reduce(state, { type: 'reset' })
assert.equal(state.mode, 'normal')
assert.equal(state.currentRoom, 1)
console.log('Game checks passed: purchases, hunger, fallback, five rooms, save, Expert and reset.')
