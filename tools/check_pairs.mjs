import assert from 'node:assert/strict'
import { build } from 'esbuild'

const bundled = await build({
  stdin: {
    contents: "export * from './src/game/pairs/reducer.ts'; export * from './src/game/pairs/scoring.ts'; export * from './src/game/pairs/types.ts'; export * from './src/game/cats.ts';",
    resolveDir: process.cwd(),
    sourcefile: 'pairs-verification.ts',
  },
  bundle: true,
  platform: 'node',
  format: 'esm',
  write: false,
})
const game = await import(`data:text/javascript;base64,${Buffer.from(bundled.outputFiles[0].contents).toString('base64')}`)

for (let roomId = 1; roomId <= 5; roomId += 1) {
  const catIds = game.catsForRoom(roomId).map((cat) => cat.id)
  for (const cardCount of game.PAIRS_CARD_COUNTS) {
    for (let seed = 1; seed <= 50; seed += 1) {
      const round = game.createPairsRound(roomId, 'normal', catIds, seed, cardCount)
      assert.equal(round.cards.length, cardCount)
      assert.equal(round.cardCount, cardCount)
      assert.equal(round.rulesId, `pairs-${cardCount}`)
      assert.equal(new Set(round.cards.map((card) => card.id)).size, cardCount)
      const counts = catIds.map((catId) => round.cards.filter((card) => card.catId === catId).length).sort((a, b) => a - b)
      assert.deepEqual(counts, cardCount === 10 ? [2, 2, 2, 2, 2]
        : cardCount === 16 ? [2, 2, 4, 4, 4] : [4, 4, 4, 4, 4])
    }
  }
}

const catIds = game.catsForRoom(1).map((cat) => cat.id)
let round = game.createPairsRound(1, 'normal', catIds, 20260921)
const first = round.cards[0]
const wrong = round.cards.find((card) => card.catId !== first.catId)
round = game.revealPairCard(round, first.id)
round = game.revealPairCard(round, wrong.id)
assert.equal(round.attempts, 1)
assert.equal(round.lastMatch, false)
assert.equal(game.revealPairCard(round, round.cards[2].id), round, 'board locks while a mismatch is visible')
round = game.hidePairMismatch(round)
assert.deepEqual(round.revealed, [])

for (const catId of catIds) {
  const pair = round.cards.filter((card) => card.catId === catId)
  round = game.revealPairCard(round, pair[0].id)
  round = game.revealPairCard(round, pair[1].id)
}
assert.equal(round.matches, 5)
assert.equal(round.status, 'finished')
assert.equal(round.cards.every((card) => card.matched), true)
assert.equal(game.pairsBaseReward(5, 5, 5, true), 70)
assert.equal(game.pairsFishReward(5, 5, 5, 2, 'normal', true), 105)
assert.equal(game.pairsFishReward(5, 5, 5, 2, 'expert', true), 157)

for (const cardCount of [16, 20]) {
  let largeRound = game.createPairsRound(1, 'normal', catIds, cardCount * 101, cardCount)
  while (largeRound.status === 'playing') {
    const first = largeRound.cards.find((card) => !card.matched)
    const second = largeRound.cards.find((card) => !card.matched && card.id !== first.id && card.catId === first.catId)
    assert.ok(second, `${cardCount}-card board always has a matching card`)
    largeRound = game.revealPairCard(largeRound, first.id)
    largeRound = game.revealPairCard(largeRound, second.id)
  }
  assert.equal(largeRound.matches, cardCount / 2)
  assert.equal(largeRound.cards.every((card) => card.matched), true)
}

console.log('Pairs checks passed: 10/16/20-card decks, repeated cats, mismatch lock, completion and rewards.')
