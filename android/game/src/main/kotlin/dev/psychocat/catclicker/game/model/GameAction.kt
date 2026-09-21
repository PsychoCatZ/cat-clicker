package dev.psychocat.catclicker.game.model

import dev.psychocat.catclicker.game.data.SceneLayout
import dev.psychocat.catclicker.game.minigames.sliding.SlidingDifficulty

/** Port of `GameAction` in src/game/economy.ts (mini-game actions arrive with the mini-games). */
sealed interface GameAction {
    data object Click : GameAction
    data class Tick(val seconds: Double) : GameAction
    data object ToggleLights : GameAction
    data object DismissOfflineReport : GameAction

    /** Starts a round of the sliding puzzle in the current room; ignored while another round is active. */
    data class StartSliding(val seed: Long, val difficulty: SlidingDifficulty, val catId: String? = null) : GameAction
    data class SlidingMove(val tileId: Int) : GameAction
    data object SlidingReshuffle : GameAction

    /** Starts a "Find the pair" round (10, 16 or 20 cards); ignored while another round is active. */
    data class StartPairs(val seed: Long, val cardCount: Int) : GameAction
    data class PairsReveal(val cardId: Int) : GameAction
    data object PairsHideMismatch : GameAction

    /** Starts a "Cats in a row" (match-3) round; ignored while another round is active. */
    data class StartMatch3(val seed: Long) : GameAction
    data class Match3Swap(val first: Int, val second: Int) : GameAction

    /** Ends the active mini-game and pays its reward to the room where it was started. */
    data object SettleMiniGame : GameAction
    data class BuyResource(val id: String) : GameAction
    data class BuyUpgrade(val id: String) : GameAction
    data class PlaceFurniture(val id: String, val layout: SceneLayout, val x: Double, val y: Double) : GameAction
    /** Takes an item out of the room; it goes back to the tray and can be placed again later. */
    data class RemoveFurniture(val id: String) : GameAction
    data class BuyFood(val id: String) : GameAction
    data class BuyCat(val id: String) : GameAction
    data class SelectCat(val id: String) : GameAction
    data class VisitRoom(val roomId: Int) : GameAction
    data object EnterNextRoom : GameAction
    data object DismissFinal : GameAction
    data object ShowFinal : GameAction
    data object StartExpert : GameAction
    data object Reset : GameAction
}
