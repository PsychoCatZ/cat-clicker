import assert from 'node:assert/strict'
import { build } from 'esbuild'

const bundled = await build({
  stdin: {
    contents: "export * from './src/game/sliding/board.ts'; export * from './src/game/sliding/layouts.ts'; export * from './src/game/sliding/reducer.ts'; export * from './src/game/sliding/scoring.ts'; export * from './src/game/cats.ts';",
    resolveDir: process.cwd(),
    sourcefile: 'sliding-verification.ts',
  },
  bundle: true,
  platform: 'node',
  format: 'esm',
  write: false,
})
const game = await import(`data:text/javascript;base64,${Buffer.from(bundled.outputFiles[0].contents).toString('base64')}`)

const expectedSizes = { easy: 3, normal: 4, hard: 5 }
for (const difficulty of game.slidingDifficulties) {
  const config = game.getSlidingConfig(difficulty)
  assert.equal(config.size, expectedSizes[difficulty])
  for (let seed = 1; seed <= 50; seed += 1) {
    const shuffled = game.shuffleSlidingBoard(config.size, seed, config.shuffleMoves)
    assert.equal(shuffled.tiles.length, config.size ** 2)
    assert.equal(game.isSlidingBoardSolvable(shuffled.tiles, config.size), true)
    assert.equal(game.isSlidingBoardSolved(shuffled.tiles), false)
    assert.equal(new Set(shuffled.tiles).size, config.size ** 2)

    let restored = shuffled.tiles
    for (const tileId of [...shuffled.history].reverse()) {
      const next = game.moveSlidingTileOnBoard(restored, config.size, tileId)
      assert.ok(next, 'reverse shuffle move remains legal')
      restored = next
    }
    assert.equal(game.isSlidingBoardSolved(restored), true, `${difficulty} shuffle reverses to the goal`)
  }
}

for (let roomId = 1; roomId <= 5; roomId += 1) {
  const catIds = game.catsForRoom(roomId).map((cat) => cat.id)
  for (const difficulty of game.slidingDifficulties) {
    const chosen = catIds[3]
    const round = game.createSlidingRound(roomId, 'normal', catIds, difficulty, 20260921 + roomId, chosen)
    assert.equal(round.catId, chosen)
    assert.ok(catIds.includes(round.catId))
    assert.equal(round.size, game.getSlidingConfig(difficulty).size)
    assert.equal(game.isSlidingBoardSolvable(round.tiles, round.size), true)
  }
  const randomCatRound = game.createSlidingRound(roomId, 'normal', catIds, 'easy', 12345)
  assert.ok(catIds.includes(randomCatRound.catId), 'automatic cat selection stays within the room')
}

const catIds = game.catsForRoom(1).map((cat) => cat.id)
let round = game.createSlidingRound(1, 'normal', catIds, 'easy', 98765, catIds[1])
const movable = game.movableSlidingTileIds(round.tiles, round.size)
const blocked = round.tiles.find((tile) => tile !== null && !movable.includes(tile))
assert.notEqual(blocked, undefined)
assert.equal(game.moveSlidingTile(round, blocked), round, 'non-adjacent tile is ignored')
const moved = game.moveSlidingTile(round, movable[0])
assert.equal(moved.moves, 1)
assert.notDeepEqual(moved.tiles, round.tiles)

const easyConfig = game.getSlidingConfig('easy')
const goal = game.createSolvedSlidingBoard(easyConfig.size)
const finalTile = easyConfig.size ** 2 - 2
const oneMoveAway = game.moveSlidingTileOnBoard(goal, easyConfig.size, finalTile)
assert.ok(oneMoveAway)
round = {
  ...round,
  tiles: oneMoveAway,
  moves: 0,
  score: 0,
  status: 'playing',
  lastEvent: null,
}
round = game.moveSlidingTile(round, finalTile)
assert.equal(round.status, 'finished')
assert.equal(round.moves, 1)
assert.equal(round.score, game.slidingCompletionScore('easy', 1))
assert.equal(game.isSlidingBoardSolved(round.tiles), true)

const beforeReshuffle = game.createSlidingRound(1, 'normal', catIds, 'normal', 33333, catIds[2])
const afterMove = game.moveSlidingTile(beforeReshuffle, game.movableSlidingTileIds(beforeReshuffle.tiles, beforeReshuffle.size)[0])
const reshuffled = game.reshuffleSlidingRound(afterMove)
assert.equal(reshuffled.moves, 0)
assert.equal(reshuffled.catId, beforeReshuffle.catId)
assert.equal(game.isSlidingBoardSolvable(reshuffled.tiles, reshuffled.size), true)
assert.equal(game.isSlidingBoardSolved(reshuffled.tiles), false)

assert.equal(game.slidingCompletionScore('easy', 1000), 900, 'many moves never reduce the base reward')
assert.ok(game.slidingCompletionScore('normal', 1) > game.slidingCompletionScore('easy', 1))
assert.ok(game.slidingCompletionScore('hard', 1) > game.slidingCompletionScore('normal', 1))
assert.equal(game.slidingFishReward(900, 1, 'normal'), 30)
assert.equal(game.slidingFishReward(900, 2, 'expert'), 67)

console.log('Sliding checks passed: 3x3/4x4/5x5 solvable shuffles, legal moves, cat selection, completion, reshuffle and rewards.')
