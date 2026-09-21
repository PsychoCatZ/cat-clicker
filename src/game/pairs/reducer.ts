import type { GameMode } from '../economy'
import type { PairCard, PairsCardCount, PairsRound } from './types'

const safeSeed = (seed: number): number => (Number.isFinite(seed) ? Math.floor(seed) : Date.now()) >>> 0 || 1

const nextRandom = (state: number): [number, number] => {
  let next = state >>> 0 || 1
  next ^= next << 13
  next ^= next >>> 17
  next ^= next << 5
  return [(next >>> 0) / 0x100000000, next >>> 0 || 1]
}

function shuffleCards(cards: PairCard[], seed: number): { cards: PairCard[]; rngState: number } {
  const shuffled = [...cards]
  let rngState = seed
  for (let index = shuffled.length - 1; index > 0; index -= 1) {
    const [random, nextState] = nextRandom(rngState)
    rngState = nextState
    const target = Math.floor(random * (index + 1))
    ;[shuffled[index], shuffled[target]] = [shuffled[target], shuffled[index]]
  }
  return { cards: shuffled, rngState }
}

export const createPairsRound = (roomId: number, mode: GameMode, catIds: string[], seed: number, cardCount: PairsCardCount = 10): PairsRound => {
  if (catIds.length !== 5) throw new Error('Pairs requires exactly five room cats')
  const rngState = safeSeed(seed)
  const catOffset = rngState % catIds.length
  const deck = Array.from({ length: cardCount / 2 }, (_, pairIndex) => catIds[(pairIndex + catOffset) % catIds.length])
    .flatMap((catId, pairIndex) => [
    { id: pairIndex * 2, catId, matched: false },
    { id: pairIndex * 2 + 1, catId, matched: false },
  ])
  const shuffled = shuffleCards(deck, rngState)
  return {
    id: `${roomId}-${mode}-${rngState}`,
    roomId,
    mode,
    rulesId: `pairs-${cardCount}`,
    cardCount,
    cards: shuffled.cards,
    revealed: [],
    attempts: 0,
    matches: 0,
    status: 'playing',
    lastMatch: null,
    rngState: shuffled.rngState,
  }
}

export const revealPairCard = (round: PairsRound, cardId: number): PairsRound => {
  if (round.status !== 'playing' || round.revealed.length >= 2 || round.revealed.includes(cardId)) return round
  const card = round.cards.find((item) => item.id === cardId)
  if (!card || card.matched) return round
  if (round.revealed.length === 0) return { ...round, revealed: [cardId], lastMatch: null }

  const first = round.cards.find((item) => item.id === round.revealed[0])
  if (!first) return { ...round, revealed: [cardId], lastMatch: null }
  const attempts = round.attempts + 1
  if (first.catId !== card.catId) return { ...round, revealed: [first.id, card.id], attempts, lastMatch: false }

  const matches = round.matches + 1
  return {
    ...round,
    cards: round.cards.map((item) => item.id === first.id || item.id === card.id ? { ...item, matched: true } : item),
    revealed: [],
    attempts,
    matches,
    status: matches === round.cardCount / 2 ? 'finished' : 'playing',
    lastMatch: true,
  }
}

export const hidePairMismatch = (round: PairsRound): PairsRound =>
  round.revealed.length === 2 && round.lastMatch === false
    ? { ...round, revealed: [], lastMatch: null }
    : round
