package dev.psychocat.catclicker.game.engine

import dev.psychocat.catclicker.game.Balance
import dev.psychocat.catclicker.game.data.Cats
import dev.psychocat.catclicker.game.data.Items
import dev.psychocat.catclicker.game.data.Rooms
import dev.psychocat.catclicker.game.data.Upgrade
import dev.psychocat.catclicker.game.data.UpgradeTier
import dev.psychocat.catclicker.game.data.Upgrades
import dev.psychocat.catclicker.game.model.GameMode
import dev.psychocat.catclicker.game.model.GameState
import kotlin.math.ceil

/**
 * Pure price and income formulas, ported from src/game/economy.ts.
 * The order of floating-point operations is kept identical to the TypeScript source on purpose.
 */
object Economy {
    fun hungerDuration(mode: GameMode): Double = if (mode == GameMode.EXPERT) 12.0 * 60 else 20.0 * 60

    private fun costMultiplier(state: GameState): Double =
        Balance.roomEconomyScale(state.currentRoom) * (if (state.mode == GameMode.EXPERT) Balance.EXPERT_COST_SCALE else 1.0)

    fun catCost(state: GameState, baseCost: Int): Double = ceil(baseCost * costMultiplier(state))
    fun upgradeCost(state: GameState, upgrade: Upgrade): Double = ceil(upgrade.baseCost * costMultiplier(state))
    fun foodCost(state: GameState, baseCost: Int): Double = ceil(baseCost * costMultiplier(state))

    fun resourceCost(state: GameState, id: String): Double {
        val resource = Items.resource(id) ?: return Double.POSITIVE_INFINITY
        val level = state.progress.resourceLevels[id] ?: 0
        return ceil(resource.baseCost * StrictMath.pow(resource.growth, level.toDouble()) * costMultiplier(state))
    }

    fun clickPower(state: GameState): Double =
        1.0 + Items.resources.sumOf { it.bonus * (state.progress.resourceLevels[it.id] ?: 0) }

    fun currentClickReward(state: GameState): Double =
        clickPower(state) * (if (state.progress.caviarSeconds > 0) 2 else 1)

    fun fishPerSecond(state: GameState): Double = fishPerSecondForRoom(state, state.currentRoom)

    fun fishPerSecondForRoom(state: GameState, roomId: Int): Double {
        val progress = state.rooms.getOrNull(roomId - 1) ?: return 0.0
        return Upgrades.forRoom(roomId).sumOf { if (it.id in progress.boughtUpgrades) it.income else 0 }.toDouble()
    }

    fun basicUpgradesBought(state: GameState): Boolean =
        Upgrades.forRoom(state.currentRoom).filter { it.tier == UpgradeTier.BASIC }
            .all { it.id in state.progress.boughtUpgrades }

    fun roomComplete(state: GameState, roomId: Int = state.currentRoom): Boolean {
        val progress = state.rooms[roomId - 1]
        return Cats.forRoom(roomId).all { it.id in progress.unlockedCats } &&
            Upgrades.forRoom(roomId).all { it.id in progress.boughtUpgrades }
    }

    /** Keeps a sleeping cat with nothing bought from being stuck forever: a mouse every ~30 minutes. */
    fun safetyIncome(state: GameState): Double {
        val progress = state.progress
        val mousePrice = foodCost(state, Items.foods[0].baseCost)
        return if (progress.hunger == 0.0 && progress.boughtUpgrades.isEmpty() && progress.fish < mousePrice) {
            mousePrice / (30 * 60)
        } else {
            0.0
        }
    }

    val finalRoom: Int get() = Rooms.count
}
