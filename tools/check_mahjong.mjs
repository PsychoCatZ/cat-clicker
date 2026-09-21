import assert from 'node:assert/strict'
import { build } from 'esbuild'

const bundled = await build({
  stdin: {
    contents: "export * from './src/game/mahjong/board.ts'; export * from './src/game/mahjong/layouts.ts'; export * from './src/game/mahjong/reducer.ts'; export * from './src/game/mahjong/scoring.ts'; export * from './src/game/cats.ts';",
    resolveDir: process.cwd(),
    sourcefile: 'mahjong-verification.ts',
  },
  bundle: true,
  platform: 'node',
  format: 'esm',
  write: false,
})
const game = await import(`data:text/javascript;base64,${Buffer.from(bundled.outputFiles[0].contents).toString('base64')}`)

const expectedCounts = { easy: 24, normal: 40, hard: 56 }
for (const difficulty of game.mahjongDifficulties) {
  const layout = game.getMahjongLayout(difficulty)
  assert.equal(layout.tileCount, expectedCounts[difficulty])
  assert.equal(layout.slots.length, layout.tileCount)
  assert.equal(new Set(layout.slots.map((slot) => slot.id)).size, layout.tileCount)
  const geometrySolution = game.findGeometryRemovalSequence(layout.slots)
  assert.ok(geometrySolution, `${difficulty} layout has a geometric solution`)
  assert.equal(geometrySolution.length, layout.tileCount / 2)

  for (let roomId = 1; roomId <= 5; roomId += 1) {
    const catIds = game.catsForRoom(roomId).map((cat) => cat.id)
    for (let seed = 1; seed <= 20; seed += 1) {
      let round = game.createMahjongRound(roomId, 'normal', catIds, difficulty, seed)
      assert.equal(round.tiles.length, layout.tileCount)
      assert.ok(round.tiles.every((tile) => catIds.includes(tile.catId)), 'only current-room cats are used')
      for (const [first, second] of geometrySolution) {
        assert.equal(game.isMahjongTileFree(round.tiles, first), true)
        assert.equal(game.isMahjongTileFree(round.tiles, second), true)
        assert.equal(round.tiles.find((tile) => tile.id === first).catId, round.tiles.find((tile) => tile.id === second).catId)
        round = game.selectMahjongTile(round, first)
        round = game.selectMahjongTile(round, second)
      }
      assert.equal(round.status, 'finished')
      assert.equal(round.pairsFound, layout.tileCount / 2)
      assert.equal(round.tiles.every((tile) => tile.removed), true)
      assert.equal(round.score, layout.tileCount / 2 * game.MAHJONG_PAIR_POINTS
        + game.MAHJONG_CLEAR_BONUS + game.MAHJONG_NO_HINT_BONUS + game.MAHJONG_NO_SHUFFLE_BONUS)
    }
  }
}

const catIds = game.catsForRoom(1).map((cat) => cat.id)
let round = game.createMahjongRound(1, 'normal', catIds, 'easy', 20260921)
const blocked = round.tiles.find((tile) => !game.isMahjongTileFree(round.tiles, tile.id))
assert.ok(blocked)
assert.equal(game.selectMahjongTile(round, blocked.id), round, 'blocked tile cannot be selected')

round = game.hintMahjongPair(round)
assert.equal(round.hintsUsed, 1)
assert.equal(round.hintedIds.length, 2)
assert.ok(round.hintedIds.every((id) => game.isMahjongTileFree(round.tiles, id)))
assert.equal(round.tiles.find((tile) => tile.id === round.hintedIds[0]).catId, round.tiles.find((tile) => tile.id === round.hintedIds[1]).catId)
assert.equal(round.score, 0, 'hint has no score penalty')

let mismatchRound = game.createMahjongRound(1, 'normal', catIds, 'easy', 30303)
const free = game.freeMahjongTiles(mismatchRound.tiles)
let mismatch = null
for (const first of free) {
  const second = free.find((tile) => tile.id !== first.id && tile.catId !== first.catId)
  if (second) { mismatch = [first, second]; break }
}
assert.ok(mismatch)
mismatchRound = game.selectMahjongTile(mismatchRound, mismatch[0].id)
mismatchRound = game.selectMahjongTile(mismatchRound, mismatch[1].id)
assert.equal(mismatchRound.selectedId, mismatch[1].id, 'mismatch simply changes the selection')
assert.equal(mismatchRound.score, 0)
assert.equal(mismatchRound.tiles.some((tile) => tile.removed), false)

let deadlock = game.createMahjongRound(1, 'normal', catIds, 'easy', 40404)
const sequence = game.findGeometryRemovalSequence(game.getMahjongLayout('easy').slots)
for (const [first, second] of sequence.slice(0, -2)) {
  deadlock.tiles.find((tile) => tile.id === first).removed = true
  deadlock.tiles.find((tile) => tile.id === second).removed = true
}
deadlock.pairsFound = deadlock.tiles.filter((tile) => tile.removed).length / 2
const lastFree = game.freeMahjongTiles(deadlock.tiles)
assert.ok(lastFree.length <= catIds.length)
lastFree.forEach((tile, index) => { tile.catId = catIds[index] })
assert.equal(game.findMahjongPairs(deadlock.tiles).length, 0)
const shuffled = game.shuffleMahjong(deadlock, catIds)
assert.equal(shuffled.shuffles, 1)
assert.ok(game.findMahjongPairs(shuffled.tiles).length > 0, 'shuffle restores an available pair')
const remainingSolution = game.findGeometryRemovalSequence(shuffled.tiles.filter((tile) => !tile.removed))
for (const [first, second] of remainingSolution) {
  assert.equal(shuffled.tiles.find((tile) => tile.id === first).catId, shuffled.tiles.find((tile) => tile.id === second).catId)
}

assert.equal(game.mahjongCompletionBonus(0, 0), 900)
assert.equal(game.mahjongCompletionBonus(1, 0), 700)
assert.equal(game.mahjongCompletionBonus(0, 1), 700)
assert.equal(game.mahjongFishReward(2100, 1, 'normal'), 70)
assert.equal(game.mahjongFishReward(2100, 2, 'expert'), 157)

console.log('Mahjong checks passed: layouts, freedom rules, guaranteed solutions, hints, deadlock shuffle, completion and rewards.')
