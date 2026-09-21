package dev.psychocat.catclicker.game.minigames

import dev.psychocat.catclicker.game.minigames.mahjong.MahjongGame
import dev.psychocat.catclicker.game.minigames.mahjong.MahjongRound
import dev.psychocat.catclicker.game.minigames.match3.Match3Game
import dev.psychocat.catclicker.game.minigames.match3.Match3Round
import dev.psychocat.catclicker.game.minigames.pairs.PairsGame
import dev.psychocat.catclicker.game.minigames.pairs.PairsRound
import dev.psychocat.catclicker.game.minigames.sliding.SlidingGame
import dev.psychocat.catclicker.game.minigames.sliding.SlidingRound
import dev.psychocat.catclicker.game.model.GameMode

enum class RoundStatus { PLAYING, FINISHED }

/**
 * (Not `sealed`: Kotlin requires the subclasses of a sealed type to share its package, and each game has its own.)
 *
 * The one mini-game round that can be active at a time (the web version enforces the same). It lives in the game
 * state, so it is saved and comes back after the app is closed. Its reward always goes to [roomId], the room
 * where the round was started, even if the round is settled later.
 */
interface MiniGameRound {
    val roomId: Int
    val mode: GameMode
    val status: RoundStatus
}

object MiniGames {
    /** Fish that settling [round] right now would pay out (score is only earned by completed steps). */
    fun fishReward(round: MiniGameRound): Double = when (round) {
        is SlidingRound -> SlidingGame.fishReward(round.score, round.roomId, round.mode)
        is PairsRound -> PairsGame.fishReward(round)
        is Match3Round -> Match3Game.fishReward(round.score, round.roomId, round.mode)
        is MahjongRound -> MahjongGame.fishReward(round.score, round.roomId, round.mode)
        else -> 0.0
    }
}
