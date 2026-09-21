package dev.psychocat.catclicker.game.engine

import dev.psychocat.catclicker.game.Balance
import dev.psychocat.catclicker.game.data.Cats
import dev.psychocat.catclicker.game.data.Furniture
import dev.psychocat.catclicker.game.data.FurniturePoint
import dev.psychocat.catclicker.game.data.FurniturePosition
import dev.psychocat.catclicker.game.data.Items
import dev.psychocat.catclicker.game.data.Rooms
import dev.psychocat.catclicker.game.data.UpgradeTier
import dev.psychocat.catclicker.game.minigames.MiniGames
import dev.psychocat.catclicker.game.minigames.pairs.PairsGame
import dev.psychocat.catclicker.game.minigames.pairs.PairsRound
import dev.psychocat.catclicker.game.minigames.sliding.SlidingGame
import dev.psychocat.catclicker.game.minigames.sliding.SlidingRound
import dev.psychocat.catclicker.game.data.Upgrades
import dev.psychocat.catclicker.game.model.GameAction
import dev.psychocat.catclicker.game.model.GameMode
import dev.psychocat.catclicker.game.model.GameState
import dev.psychocat.catclicker.game.model.OfflineReport
import dev.psychocat.catclicker.game.model.RoomProgress
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * Port of `gameReducer` and `applyOfflineProgress` from src/game/economy.ts.
 * `reduce` returns the very same instance when an action changes nothing, so callers can use `===` to skip work.
 */
object GameEngine {
    const val MAX_FISH = 1e15
    const val OFFLINE_LIMIT_SECONDS = 8.0 * 60 * 60
    const val MAX_TICK_SECONDS = 2.0

    private fun GameState.withProgress(next: RoomProgress): GameState =
        copy(rooms = rooms.mapIndexed { index, room -> if (index == currentRoom - 1) next else room })

    /** Reaching the last room's completion re-opens the final screen. */
    private fun completeIfNeeded(state: GameState): GameState =
        if (state.currentRoom == Rooms.count && Economy.roomComplete(state)) state.copy(finalDismissed = false) else state

    fun reduce(state: GameState, action: GameAction): GameState {
        val progress = state.progress
        return when (action) {
            GameAction.Click ->
                if (progress.hunger > 0 && !progress.lightsOff) {
                    state.withProgress(progress.copy(fish = progress.fish + Economy.currentClickReward(state)))
                } else {
                    state
                }

            GameAction.ToggleLights ->
                if (progress.hunger <= 0) state else state.withProgress(progress.copy(lightsOff = !progress.lightsOff))

            is GameAction.Tick -> tick(state, action.seconds)

            GameAction.DismissOfflineReport -> if (state.offlineReport != null) state.copy(offlineReport = null) else state

            is GameAction.StartSliding ->
                if (state.activeGame != null) {
                    state
                } else {
                    state.copy(
                        activeGame = SlidingGame.create(
                            state.currentRoom, state.mode, Cats.forRoom(state.currentRoom).map { it.id },
                            action.difficulty, action.seed, action.catId,
                        ),
                    )
                }

            is GameAction.SlidingMove -> {
                val round = state.activeGame as? SlidingRound ?: return state
                if (round.roomId != state.currentRoom) return state
                val next = SlidingGame.move(round, action.tileId)
                if (next === round) state else state.copy(activeGame = next)
            }

            GameAction.SlidingReshuffle -> {
                val round = state.activeGame as? SlidingRound ?: return state
                val next = SlidingGame.reshuffle(round)
                if (next === round) state else state.copy(activeGame = next)
            }

            is GameAction.StartPairs ->
                if (state.activeGame != null || action.cardCount !in PairsGame.CARD_COUNTS) {
                    state
                } else {
                    state.copy(
                        activeGame = PairsGame.create(
                            state.currentRoom, state.mode, Cats.forRoom(state.currentRoom).map { it.id },
                            action.seed, action.cardCount,
                        ),
                    )
                }

            is GameAction.PairsReveal -> {
                val round = state.activeGame as? PairsRound ?: return state
                if (round.roomId != state.currentRoom) return state
                val next = PairsGame.reveal(round, action.cardId)
                if (next === round) state else state.copy(activeGame = next)
            }

            GameAction.PairsHideMismatch -> {
                val round = state.activeGame as? PairsRound ?: return state
                val next = PairsGame.hideMismatch(round)
                if (next === round) state else state.copy(activeGame = next)
            }

            GameAction.SettleMiniGame -> settle(state)

            is GameAction.BuyResource -> {
                val resource = Items.resource(action.id) ?: return state
                val cost = Economy.resourceCost(state, resource.id)
                if (progress.fish < cost) return state
                state.withProgress(
                    progress.copy(
                        fish = progress.fish - cost,
                        resourceLevels = progress.resourceLevels + (resource.id to (progress.resourceLevels[resource.id] ?: 0) + 1),
                    ),
                )
            }

            is GameAction.BuyUpgrade -> {
                val upgrade = Upgrades.find(state.currentRoom, action.id) ?: return state
                if (upgrade.id in progress.boughtUpgrades) return state
                if (upgrade.tier == UpgradeTier.ADVANCED && !Economy.basicUpgradesBought(state)) return state
                val cost = Economy.upgradeCost(state, upgrade)
                if (progress.fish < cost) return state
                completeIfNeeded(
                    state.withProgress(
                        progress.copy(fish = progress.fish - cost, boughtUpgrades = progress.boughtUpgrades + upgrade.id),
                    ),
                )
            }

            is GameAction.PlaceFurniture -> {
                val upgrade = Furniture.visible(state.currentRoom, progress.boughtUpgrades).firstOrNull { it.id == action.id }
                    ?: return state
                val point = Furniture.constrainPoint(upgrade, action.layout, FurniturePoint(action.x, action.y)) ?: return state
                val position = (progress.furniturePositions[upgrade.id] ?: FurniturePosition())
                    .with(action.layout, point)
                state.withProgress(progress.copy(furniturePositions = progress.furniturePositions + (upgrade.id to position)))
            }

            is GameAction.RemoveFurniture -> {
                if (progress.furniturePositions[action.id] == null) return state
                state.withProgress(progress.copy(furniturePositions = progress.furniturePositions - action.id))
            }

            is GameAction.BuyFood -> {
                val food = Items.food(action.id) ?: return state
                if (progress.hunger >= 100 && food.boostSeconds == null) return state
                val cost = Economy.foodCost(state, food.baseCost)
                if (progress.fish < cost) return state
                state.withProgress(
                    progress.copy(
                        fish = progress.fish - cost,
                        hunger = min(100.0, progress.hunger + food.restore),
                        caviarSeconds = food.boostSeconds?.toDouble() ?: progress.caviarSeconds,
                    ),
                )
            }

            is GameAction.BuyCat -> {
                val cat = Cats.forRoom(state.currentRoom).firstOrNull { it.id == action.id } ?: return state
                if (cat.id in progress.unlockedCats) return state
                val cost = Economy.catCost(state, cat.baseCost)
                if (progress.fish < cost) return state
                completeIfNeeded(
                    state.withProgress(
                        progress.copy(
                            fish = progress.fish - cost,
                            unlockedCats = progress.unlockedCats + cat.id,
                            selectedCat = cat.id,
                        ),
                    ),
                )
            }

            is GameAction.SelectCat ->
                if (action.id in progress.unlockedCats && Cats.forRoom(state.currentRoom).any { it.id == action.id }) {
                    state.withProgress(progress.copy(selectedCat = action.id))
                } else {
                    state
                }

            is GameAction.VisitRoom ->
                if (action.roomId in 1..state.unlockedRoom) settle(state).copy(currentRoom = action.roomId) else state

            GameAction.EnterNextRoom ->
                if (Economy.roomComplete(state) && state.currentRoom < Rooms.count) {
                    state.copy(currentRoom = state.currentRoom + 1, unlockedRoom = max(state.unlockedRoom, state.currentRoom + 1))
                } else {
                    state
                }

            GameAction.DismissFinal -> state.copy(finalDismissed = true)

            GameAction.ShowFinal ->
                if (state.currentRoom == Rooms.count && Economy.roomComplete(state)) state.copy(finalDismissed = false) else state

            GameAction.StartExpert ->
                if (state.mode == GameMode.NORMAL && Economy.roomComplete(state, Rooms.count)) GameState.initial(GameMode.EXPERT) else state

            GameAction.Reset -> GameState.initial()
        }
    }

    /** Pays the active round's reward to the room where it was started and clears it. */
    private fun settle(state: GameState): GameState {
        val round = state.activeGame ?: return state
        val reward = MiniGames.fishReward(round)
        return state.copy(
            rooms = state.rooms.mapIndexed { index, room ->
                if (index == round.roomId - 1) room.copy(fish = min(MAX_FISH, room.fish + reward)) else room
            },
            activeGame = null,
        )
    }

    private fun tick(state: GameState, requestedSeconds: Double): GameState {
        val seconds = max(0.0, min(requestedSeconds, MAX_TICK_SECONDS))
        if (seconds == 0.0) return state
        val progress = state.progress
        val fallback = Economy.safetyIncome(state)
        val income = Economy.fishPerSecond(state) + fallback
        val earnedFish = progress.fish + income * seconds
        val mousePrice = Economy.foodCost(state, Items.foods[0].baseCost)
        val next = progress.copy(
            fish = if (fallback > 0 && earnedFish >= mousePrice - 1e-7) mousePrice else earnedFish,
            hunger = if (progress.lightsOff) {
                progress.hunger
            } else {
                max(0.0, progress.hunger - seconds * 100 / Economy.hungerDuration(state.mode))
            },
            caviarSeconds = max(0.0, progress.caviarSeconds - seconds),
        )
        return state.copy(
            rooms = state.rooms.mapIndexed { index, room ->
                when {
                    index == state.currentRoom - 1 -> next
                    room.caviarSeconds > 0 -> room.copy(caviarSeconds = max(0.0, room.caviarSeconds - seconds))
                    else -> room
                }
            },
        )
    }

    /**
     * Credits passive income and hunger for time spent away (at most [OFFLINE_LIMIT_SECONDS]) to every unlocked room.
     * The caviar timer runs in real time, so it uses the full elapsed time.
     */
    fun applyOfflineProgress(state: GameState, elapsedSeconds: Double): GameState {
        val elapsed = max(0.0, if (elapsedSeconds.isFinite()) elapsedSeconds else 0.0)
        if (elapsed < 1) return state
        val credited = min(elapsed, OFFLINE_LIMIT_SECONDS)
        var fishEarned = 0.0
        var hungerSpent = 0.0
        val expertScale = if (state.mode == GameMode.EXPERT) Balance.EXPERT_COST_SCALE else 1.0

        val nextRooms = state.rooms.mapIndexed { index, progress ->
            val roomId = index + 1
            val boost = max(0.0, progress.caviarSeconds - elapsed)
            if (roomId > state.unlockedRoom) {
                return@mapIndexed if (boost == progress.caviarSeconds) progress else progress.copy(caviarSeconds = boost)
            }

            val passiveEarned = Economy.fishPerSecondForRoom(state, roomId) * credited
            val spent = if (progress.lightsOff) 0.0 else min(progress.hunger, credited * 100 / Economy.hungerDuration(state.mode))
            val hunger = max(0.0, progress.hunger - spent)
            var fish = min(MAX_FISH, progress.fish + passiveEarned)

            val mouseBase = Items.foods[0].baseCost
            if (!progress.lightsOff && progress.boughtUpgrades.isEmpty() &&
                fish < ceil(mouseBase * Balance.roomEconomyScale(roomId) * expertScale)
            ) {
                val timeUntilHungry = progress.hunger / 100 * Economy.hungerDuration(state.mode)
                val sleepingSeconds = max(0.0, credited - timeUntilHungry)
                val mousePrice = ceil(mouseBase * Balance.roomEconomyScale(roomId) * expertScale)
                fish = min(mousePrice, fish + sleepingSeconds * mousePrice / (30 * 60))
            }

            fishEarned += max(0.0, fish - progress.fish)
            hungerSpent += spent
            progress.copy(fish = fish, hunger = hunger, caviarSeconds = boost)
        }

        return state.copy(
            rooms = nextRooms,
            offlineReport = if (elapsed >= 60 && (fishEarned > 0 || hungerSpent > 0)) {
                OfflineReport(elapsed, credited, fishEarned, hungerSpent)
            } else {
                null
            },
        )
    }
}
