package dev.psychocat.catclicker.game.session

import dev.psychocat.catclicker.game.data.Cats
import dev.psychocat.catclicker.game.data.Upgrades
import dev.psychocat.catclicker.game.engine.GameEngine
import dev.psychocat.catclicker.game.model.GameAction
import dev.psychocat.catclicker.game.model.GameState
import dev.psychocat.catclicker.game.save.SaveRepository
import java.io.IOException
import kotlin.math.max
import kotlin.math.min

/** Time source, injectable so tests can move the clock. */
interface Clock {
    /** Wall-clock time, may be changed by the user; used only to measure time spent away. */
    fun wallMillis(): Long

    /** Monotonic time, used to measure time while the game is on screen. */
    fun monotonicMillis(): Long
}

/**
 * The live game: owns the current [GameState], advances it with the clock, and keeps it on disk.
 *
 * Threading: [dispatch], [advance], [onForeground] and [onBackground] belong to one thread (the UI thread).
 * [saveIfNeeded] / [save] may run on another thread; they only read immutable snapshots.
 *
 * Survival rules (see docs/ANDROID_PLAN.md):
 *  - the file stores the state together with the moment it was written, so after the process is killed the time
 *    since that moment is credited as offline time on the next start, and only once (the start saves again);
 *  - going to the background saves immediately and remembers when the player left;
 *  - coming back credits the time away exactly like a cold start would.
 */
class GameSession(private val repository: SaveRepository, private val clock: Clock) {
    @Volatile
    var state: GameState
        private set

    @Volatile private var dirty = false
    @Volatile private var ticksUnsaved = false
    @Volatile private var lastSaveMonotonic = clock.monotonicMillis()
    private var lastAdvanceMonotonic = clock.monotonicMillis()
    @Volatile private var leftAtWall: Long? = null

    init {
        val loaded = repository.load()
        if (loaded == null) {
            state = GameState.initial()
        } else {
            state = GameEngine.applyOfflineProgress(loaded.state, secondsSince(loaded.savedAtMillis))
            // Persist right away: otherwise a second cold start would credit the same absence again.
            trySave()
        }
    }

    private fun secondsSince(wallMillis: Long): Double = max(0L, clock.wallMillis() - wallMillis) / 1000.0

    fun dispatch(action: GameAction): GameState {
        val before = state
        val after = GameEngine.reduce(before, action)
        if (after !== before) {
            state = after
            if (action !is GameAction.Tick) dirty = true
        }
        return after
    }

    /** Call about once a second while the game is on screen. */
    fun advance() {
        val now = clock.monotonicMillis()
        var remaining = (now - lastAdvanceMonotonic) / 1000.0
        lastAdvanceMonotonic = now
        if (remaining <= 0) return
        if (remaining > LONG_GAP_SECONDS) {
            // The app was frozen while nominally in the foreground: treat it like time away.
            state = GameEngine.applyOfflineProgress(state, remaining)
        } else {
            while (remaining > 0) {
                val step = min(remaining, GameEngine.MAX_TICK_SECONDS)
                state = GameEngine.reduce(state, GameAction.Tick(step))
                remaining -= step
            }
        }
        ticksUnsaved = true
    }

    /** The screen became visible: credit the time away. */
    fun onForeground() {
        val left = leftAtWall
        leftAtWall = null
        if (left != null) {
            state = GameEngine.applyOfflineProgress(state, secondsSince(left))
            dirty = true
        }
        lastAdvanceMonotonic = clock.monotonicMillis()
    }

    /** The screen went away: bring the state up to date, save it and remember when the player left. Idempotent. */
    fun onBackground() {
        if (leftAtWall != null) return
        advance()
        leftAtWall = clock.wallMillis()
        trySave()
    }

    fun saveIfNeeded() {
        val periodic = ticksUnsaved && clock.monotonicMillis() - lastSaveMonotonic >= PERIODIC_SAVE_MILLIS
        if (dirty || periodic) trySave()
    }

    /** Saves the current state. Throws [IOException] if the disk refuses; the state stays marked as unsaved. */
    fun save() {
        dirty = false
        ticksUnsaved = false
        val snapshot = state
        // While in the background the state is only current up to the moment the player left.
        val stamp = leftAtWall ?: clock.wallMillis()
        try {
            repository.save(snapshot, stamp)
            lastSaveMonotonic = clock.monotonicMillis()
        } catch (e: IOException) {
            dirty = true
            throw e
        }
    }

    private fun trySave() {
        try {
            save()
        } catch (e: IOException) {
            // Keep playing; the next save attempt retries.
        }
    }

    val isDirty: Boolean get() = dirty

    /** Development aid (offered only in debuggable builds): adds [amount] fish to the current room. */
    fun debugGrantFish(amount: Double) {
        state = withFish(state, min(GameEngine.MAX_FISH, state.progress.fish + amount))
        dirty = true
    }

    /** Development aid: buys every cat and upgrade of the current room through the normal purchase rules. */
    fun debugCompleteRoom() {
        var next = state
        for (cat in Cats.forRoom(next.currentRoom)) {
            next = GameEngine.reduce(withFish(next, GameEngine.MAX_FISH), GameAction.BuyCat(cat.id))
        }
        for (upgrade in Upgrades.forRoom(next.currentRoom)) {
            next = GameEngine.reduce(withFish(next, GameEngine.MAX_FISH), GameAction.BuyUpgrade(upgrade.id))
        }
        state = withFish(next, state.progress.fish)
        dirty = true
    }

    private fun withFish(source: GameState, fish: Double): GameState = source.copy(
        rooms = source.rooms.mapIndexed { index, room -> if (index == source.currentRoom - 1) room.copy(fish = fish) else room },
    )

    companion object {
        const val PERIODIC_SAVE_MILLIS = 30_000L
        const val LONG_GAP_SECONDS = 5.0
    }
}
