import assert from 'node:assert/strict'
import { existsSync } from 'node:fs'
import { join } from 'node:path'
import { build } from 'esbuild'

const bundled = await build({
  stdin: {
    contents: "export * from './src/game/economy.ts'; export * from './src/game/cats.ts'; export * from './src/game/upgrades.ts'; export * from './src/game/items.ts'; export * from './src/game/rooms.ts'; export * from './src/game/save.ts'; export * from './src/game/furniture.ts'; export * from './src/game/match3/board.ts'; export * from './src/game/match3/scoring.ts'; export * from './src/game/pairs/scoring.ts'; export * from './src/game/mahjong/board.ts'; export * from './src/game/mahjong/layouts.ts'; export * from './src/game/mahjong/scoring.ts'; export * from './src/game/sliding/board.ts'; export * from './src/game/sliding/layouts.ts'; export * from './src/game/sliding/scoring.ts';",
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
  ...game.rooms.flatMap((room) => [room.day, room.night, room.match3Background]),
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

let decorating = fund(game.initialState())
decorating = reduce(decorating, { type: 'buyUpgrade', id: 'room-1-basic-1' })
assert.equal(game.fishPerSecond(decorating), 1, 'income starts before placing the item')
assert.equal(game.visibleFurniture(1, game.activeProgress(decorating).boughtUpgrades).length, 1)
assert.deepEqual(game.activeProgress(decorating).furniturePositions, {}, 'new purchases wait in the tray')
decorating = reduce(decorating, { type: 'placeFurniture', id: 'room-1-basic-1', layout: 'desktop', x: 50, y: 10 })
assert.ok(game.activeProgress(decorating).furniturePositions['room-1-basic-1'].desktop.y >= 57, 'floor item stays on the floor')
decorating = reduce(decorating, { type: 'placeFurniture', id: 'room-1-basic-1', layout: 'mobile', x: 90, y: 90 })
assert.ok(game.activeProgress(decorating).furniturePositions['room-1-basic-1'].mobile.x < 90, 'mobile item stays within the scene')
assert.equal(game.fishPerSecond(decorating), 1, 'placing does not change income')
let miniGame = game.initialState()
miniGame = reduce(miniGame, { type: 'startMatch3', seed: 12345 })
assert.ok(miniGame.match3.activeRound)
assert.equal(game.findMatchRuns(miniGame.match3.activeRound.board).length, 0)
const miniMove = game.findPossibleSwap(miniGame.match3.activeRound.board)
assert.ok(miniMove)
miniGame = reduce(miniGame, { type: 'match3Swap', first: miniMove[0], second: miniMove[1] })
assert.equal(miniGame.match3.activeRound.movesLeft, 19)
const miniReward = game.match3FishReward(miniGame.match3.activeRound.score, 1, 'normal')
miniGame = reduce(miniGame, { type: 'settleMatch3' })
assert.equal(miniGame.match3.activeRound, null)
assert.equal(game.activeProgress(miniGame).fish, miniReward, 'match-3 reward reaches the starting room')
let mahjong = reduce(game.initialState(), { type: 'startMahjong', seed: 24680, difficulty: 'easy' })
assert.ok(mahjong.mahjong.activeRound)
const mahjongMove = game.findMahjongPairs(mahjong.mahjong.activeRound.tiles)[0]
mahjong = reduce(mahjong, { type: 'mahjongSelect', tileId: mahjongMove[0] })
mahjong = reduce(mahjong, { type: 'mahjongSelect', tileId: mahjongMove[1] })
assert.equal(mahjong.mahjong.activeRound.score, 100)
const mahjongReward = game.mahjongFishReward(100, 1, 'normal')
mahjong = reduce(mahjong, { type: 'settleMahjong' })
assert.equal(mahjong.mahjong.activeRound, null)
assert.equal(game.activeProgress(mahjong).fish, mahjongReward, 'mahjong reward reaches the starting room on early exit')
let sliding = reduce(game.initialState(), { type: 'startSliding', seed: 11223, difficulty: 'easy', catId: 'basic-03' })
assert.ok(sliding.sliding.activeRound)
const slidingSize = sliding.sliding.activeRound.size
const finalTile = slidingSize ** 2 - 2
sliding.sliding.activeRound.tiles = game.moveSlidingTileOnBoard(game.createSolvedSlidingBoard(slidingSize), slidingSize, finalTile)
sliding = reduce(sliding, { type: 'slidingMove', tileId: finalTile })
assert.equal(sliding.sliding.activeRound.status, 'finished')
const slidingReward = game.slidingFishReward(sliding.sliding.activeRound.score, 1, 'normal')
sliding = reduce(sliding, { type: 'settleSliding' })
assert.equal(sliding.sliding.activeRound, null)
assert.equal(game.activeProgress(sliding).fish, slidingReward, 'sliding reward reaches the starting room')
let resting = reduce(decorating, { type: 'toggleLights' })
assert.equal(game.activeProgress(resting).lightsOff, true)
const beforeRest = game.activeProgress(resting)
assert.equal(reduce(resting, { type: 'click' }), resting, 'sleeping cat does not earn clicks')
resting = reduce(resting, { type: 'tick', seconds: 1 })
assert.equal(game.activeProgress(resting).hunger, beforeRest.hunger, 'lights off pauses hunger')
assert.equal(game.activeProgress(resting).fish, beforeRest.fish + 1, 'passive income continues in the dark')
resting = reduce(resting, { type: 'toggleLights' })
assert.equal(game.activeProgress(resting).lightsOff, false)
resting = reduce(resting, { type: 'tick', seconds: 1 })
assert.ok(game.activeProgress(resting).hunger < beforeRest.hunger, 'hunger resumes when lights are on')
const beforeClick = game.activeProgress(resting).fish
resting = reduce(resting, { type: 'click' })
assert.equal(game.activeProgress(resting).fish, beforeClick + 1, 'clicking resumes when lights are on')
const beforeInvalidPlacement = decorating
decorating = reduce(decorating, { type: 'placeFurniture', id: 'room-1-advanced-2', layout: 'desktop', x: 30, y: 30 })
assert.equal(decorating, beforeInvalidPlacement, 'unowned items cannot be placed')
for (const upgrade of game.upgradesForRoom(1).filter((item) => item.tier === 'basic' && item.slot > 0)) {
  decorating = fund(decorating)
  decorating = reduce(decorating, { type: 'buyUpgrade', id: upgrade.id })
}
decorating = fund(decorating)
decorating = reduce(decorating, { type: 'buyUpgrade', id: 'room-1-advanced-2' })
assert.equal(game.visibleFurniture(1, game.activeProgress(decorating).boughtUpgrades)[1].id, 'room-1-advanced-2', 'advanced item replaces the basic image')
decorating = reduce(decorating, { type: 'placeFurniture', id: 'room-1-advanced-2', layout: 'desktop', x: 30, y: 95 })
assert.ok(game.activeProgress(decorating).furniturePositions['room-1-advanced-2'].desktop.y < 55, 'wall item stays on the wall')

let sleeping = game.initialState()
sleeping.rooms[0].hunger = 0
assert.equal(reduce(sleeping, { type: 'click' }), sleeping)
assert.equal(reduce(sleeping, { type: 'toggleLights' }), sleeping, 'empty hunger cannot wake the cat')
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
game.saveGame(decorating)
assert.deepEqual(game.loadGame().rooms[0].furniturePositions, decorating.rooms[0].furniturePositions, 'furniture positions survive reload')
game.saveGame(reduce(decorating, { type: 'toggleLights' }))
assert.equal(game.loadGame().rooms[0].lightsOff, true, 'manual sleep survives reload')
let savedRound = reduce(game.initialState(), { type: 'startMatch3', seed: 54321 })
game.saveGame(savedRound)
assert.deepEqual(game.loadGame().match3.activeRound, savedRound.match3.activeRound, 'active match-3 round survives reload')
let savedPairs = reduce(game.initialState(), { type: 'startPairs', seed: 98765, cardCount: 16 })
savedPairs = reduce(savedPairs, { type: 'pairsReveal', cardId: savedPairs.pairs.activeRound.cards[0].id })
game.saveGame(savedPairs)
assert.deepEqual(game.loadGame().pairs.activeRound, savedPairs.pairs.activeRound, 'active pairs round survives reload')
let savedMahjong = reduce(game.initialState(), { type: 'startMahjong', seed: 13579, difficulty: 'normal' })
savedMahjong = reduce(savedMahjong, { type: 'mahjongHint' })
game.saveGame(savedMahjong)
assert.deepEqual(game.loadGame().mahjong.activeRound, savedMahjong.mahjong.activeRound, 'active mahjong round survives reload')
let savedSliding = reduce(game.initialState(), { type: 'startSliding', seed: 86420, difficulty: 'hard', catId: 'basic-05' })
savedSliding = reduce(savedSliding, { type: 'slidingMove', tileId: game.movableSlidingTileIds(savedSliding.sliding.activeRound.tiles, 5)[0] })
game.saveGame(savedSliding)
assert.deepEqual(game.loadGame().sliding.activeRound, savedSliding.sliding.activeRound, 'active sliding puzzle survives reload')
const legacyPairs = reduce(game.initialState(), { type: 'startPairs', seed: 45678, cardCount: 10 })
legacyPairs.pairs.activeRound.rulesId = 'pairs-5'
delete legacyPairs.pairs.activeRound.cardCount
saved.delete('cat-clicker-save-v6')
saved.delete('cat-clicker-save-v5')
saved.set('cat-clicker-save-v4', JSON.stringify({ ...legacyPairs, savedAt: Date.now() }))
assert.equal(game.loadGame().pairs.activeRound.rulesId, 'pairs-10', 'original ten-card round migrates to the sized rules id')
assert.equal(game.loadGame().pairs.activeRound.cardCount, 10)
let offline = game.initialState()
offline.rooms[0].boughtUpgrades = ['room-1-basic-1']
offline = game.applyOfflineProgress(offline, 60 * 60)
assert.equal(offline.rooms[0].fish, 3600, 'passive income is credited while away')
assert.equal(offline.rooms[0].hunger, 0, 'hunger is spent while away with lights on')
assert.equal(offline.offlineReport.fishEarned, 3600)
let offlineRest = game.initialState()
offlineRest.rooms[0].boughtUpgrades = ['room-1-basic-1']
offlineRest.rooms[0].lightsOff = true
offlineRest = game.applyOfflineProgress(offlineRest, 60 * 60)
assert.equal(offlineRest.rooms[0].hunger, 100, 'lights off protects hunger while away')
assert.equal(offlineRest.rooms[0].fish, 3600, 'passive income continues while resting')
const cappedOffline = game.applyOfflineProgress(offlineRest, 10 * 60 * 60)
assert.equal(cappedOffline.rooms[0].fish - offlineRest.rooms[0].fish, 8 * 60 * 60, 'offline income is capped at eight hours')
let richSleeper = game.initialState()
richSleeper.rooms[0].fish = 500
richSleeper.rooms[0].hunger = 0
richSleeper = game.applyOfflineProgress(richSleeper, 60 * 60)
assert.equal(richSleeper.rooms[0].fish, 500, 'safety income never reduces an existing balance')
const legacyV2 = game.initialState()
legacyV2.rooms[0].boughtUpgrades = ['room-1-basic-1']
delete legacyV2.rooms[0].furniturePositions
delete legacyV2.rooms[0].lightsOff
delete legacyV2.match3
delete legacyV2.pairs
delete legacyV2.mahjong
delete legacyV2.sliding
delete legacyV2.offlineReport
saved.delete('cat-clicker-save-v6')
saved.delete('cat-clicker-save-v5')
saved.delete('cat-clicker-save-v4')
saved.delete('cat-clicker-save-v3')
saved.set('cat-clicker-save-v2', JSON.stringify(legacyV2))
assert.ok(game.loadGame().rooms[0].furniturePositions['room-1-basic-1'].desktop, 'older saves keep their room layout')
assert.equal(game.loadGame().rooms[0].lightsOff, false, 'older saves start with lights on')
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
assert.deepEqual(game.activeProgress(state).furniturePositions, {}, 'reset clears room decoration')
console.log('Game checks passed: purchases, furniture placement, lights, hunger, offline progress, five rooms, save, Expert and reset.')
