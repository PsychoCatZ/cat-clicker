import assert from 'node:assert/strict'
import { build } from 'esbuild'

const bundled = await build({
  stdin: {
    contents: "export * from './src/game/match3/board.ts'; export * from './src/game/match3/reducer.ts'; export * from './src/game/match3/scoring.ts'; export * from './src/game/cats.ts';",
    resolveDir: process.cwd(),
    sourcefile: 'match3-verification.ts',
  },
  bundle: true,
  platform: 'node',
  format: 'esm',
  write: false,
})
const game = await import(`data:text/javascript;base64,${Buffer.from(bundled.outputFiles[0].contents).toString('base64')}`)

for (let roomId = 1; roomId <= 5; roomId += 1) {
  const catIds = game.catsForRoom(roomId).map((cat) => cat.id)
  assert.equal(catIds.length, 5)
  for (let seed = 1; seed <= 50; seed += 1) {
    const round = game.createMatch3Round(roomId, 'normal', catIds, seed)
    assert.equal(round.board.length, 49)
    assert.equal(game.findMatchRuns(round.board).length, 0, 'start board has no matches')
    assert.ok(game.findPossibleSwap(round.board), 'start board has a possible move')
  }
}

const catIds = game.catsForRoom(1).map((cat) => cat.id)
let round = game.createMatch3Round(1, 'normal', catIds, 20260921)
let invalid = null
for (let index = 0; index < round.board.length && !invalid; index += 1) {
  for (const target of [index + 1, index + 7]) {
    if (game.areAdjacent(index, target) && !game.swapCreatesMatch(round.board, index, target)) {
      invalid = [index, target]
      break
    }
  }
}
assert.ok(invalid)
const rejected = game.playMatch3Turn(round, invalid[0], invalid[1], catIds)
assert.equal(rejected.accepted, false)
assert.equal(rejected.round, round)

while (round.movesLeft > 0) {
  const move = game.findPossibleSwap(round.board)
  assert.ok(move, 'stable board remains playable')
  const result = game.playMatch3Turn(round, move[0], move[1], catIds)
  assert.equal(result.accepted, true)
  assert.equal(result.round.movesLeft, round.movesLeft - 1)
  assert.ok(result.round.score > round.score)
  assert.equal(game.findMatchRuns(result.round.board).length, 0)
  assert.ok(game.findPossibleSwap(result.round.board))
  round = result.round
}
assert.equal(round.status, 'finished')
assert.ok(round.score > 0)

assert.equal(game.runPoints(3), 30)
assert.equal(game.runPoints(4), 60)
assert.equal(game.runPoints(5), 100)
assert.equal(game.runPoints(7), 180)
assert.equal(game.cascadeMultiplier(1), 1)
assert.equal(game.cascadeMultiplier(3), 2)
assert.equal(game.cascadeMultiplier(9), 3)
assert.equal(game.match3FishReward(270, 2, 'normal'), 20)
assert.equal(game.match3FishReward(600, 1, 'normal'), 30)
assert.equal(game.match3FishReward(1200, 2, 'normal'), 82)
assert.equal(game.match3FishReward(1200, 3, 'normal'), 123)
assert.equal(game.match3FishReward(2500, 5, 'normal'), 405)
assert.equal(game.match3FishReward(2500, 5, 'expert'), 607)

console.log('Match-3 checks passed: generation, valid and invalid moves, cascades, 20-move round and rewards.')
