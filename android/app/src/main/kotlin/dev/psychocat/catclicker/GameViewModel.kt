package dev.psychocat.catclicker

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.psychocat.catclicker.game.model.GameAction
import dev.psychocat.catclicker.game.model.GameState
import dev.psychocat.catclicker.game.session.Clock
import dev.psychocat.catclicker.game.session.GameSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object AndroidClock : Clock {
    override fun wallMillis(): Long = System.currentTimeMillis()
    override fun monotonicMillis(): Long = SystemClock.elapsedRealtime()
}

/**
 * Thin Android wrapper around [GameSession]. The ViewModel outlives screen rotations, so the game state and the
 * running clock survive them; process death is covered by the session's saves.
 */
class GameViewModel(private val session: GameSession) : ViewModel() {
    private val mutableState = MutableStateFlow(session.state)
    val state: StateFlow<GameState> = mutableState

    private var tickJob: Job? = null

    init {
        // Writes happen off the main thread. Actions are saved within half a second, pure time passing every 30 s.
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(SAVE_POLL_MILLIS)
                session.saveIfNeeded()
            }
        }
    }

    /** Applies [action]; returns true if it changed the game (false for rejected purchases and no-ops). */
    fun dispatch(action: GameAction): Boolean {
        val before = session.state
        session.dispatch(action)
        val after = session.state
        mutableState.value = after
        return after !== before
    }

    /** The screen became visible (Activity.onStart). */
    fun onForeground() {
        session.onForeground()
        mutableState.value = session.state
        tickJob?.cancel()
        tickJob = viewModelScope.launch {
            while (isActive) {
                delay(TICK_MILLIS)
                session.advance()
                mutableState.value = session.state
            }
        }
    }

    /** The screen went away (Activity.onStop): stop the clock and write everything to disk. */
    fun onBackground() {
        tickJob?.cancel()
        tickJob = null
        session.onBackground()
        mutableState.value = session.state
    }

    private companion object {
        const val TICK_MILLIS = 1000L
        const val SAVE_POLL_MILLIS = 500L
    }
}
