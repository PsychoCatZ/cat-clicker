package dev.psychocat.catclicker.game.model

import dev.psychocat.catclicker.game.data.SceneLayout

/** Port of `GameAction` in src/game/economy.ts (mini-game actions arrive with the mini-games). */
sealed interface GameAction {
    data object Click : GameAction
    data class Tick(val seconds: Double) : GameAction
    data object ToggleLights : GameAction
    data object DismissOfflineReport : GameAction
    data class BuyResource(val id: String) : GameAction
    data class BuyUpgrade(val id: String) : GameAction
    data class PlaceFurniture(val id: String, val layout: SceneLayout, val x: Double, val y: Double) : GameAction
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
