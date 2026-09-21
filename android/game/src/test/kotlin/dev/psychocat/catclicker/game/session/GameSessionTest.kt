package dev.psychocat.catclicker.game.session

import dev.psychocat.catclicker.game.engine.Economy
import dev.psychocat.catclicker.game.model.GameAction
import dev.psychocat.catclicker.game.save.SaveRepository
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakeClock(var wall: Long = 1_000_000_000L, var mono: Long = 0L) : Clock {
    override fun wallMillis() = wall
    override fun monotonicMillis() = mono

    fun pass(seconds: Double) {
        wall += (seconds * 1000).toLong()
        mono += (seconds * 1000).toLong()
    }

    /** Time that passes while the process does not run (monotonic time restarts for a new process). */
    fun processGap(seconds: Double) {
        wall += (seconds * 1000).toLong()
    }
}

class GameSessionTest {
    private lateinit var dir: File
    private lateinit var repository: SaveRepository
    private val clock = FakeClock()

    @BeforeTest
    fun setUp() {
        dir = Files.createTempDirectory("catclicker-session").toFile()
        repository = SaveRepository(dir)
    }

    @AfterTest
    fun tearDown() {
        dir.deleteRecursively()
    }

    private fun newSession() = GameSession(repository, clock)

    /** Writes a save with [fish] fish, optionally with the first upgrade (1 fish/s) already bought. */
    private fun seedSave(fish: Double, boughtIncome: Boolean) {
        val first = newSession()
        var state = first.state
        state = state.copy(rooms = state.rooms.mapIndexed { i, r -> if (i == 0) r.copy(fish = fish) else r })
        repository.save(state, clock.wall)
        val second = newSession()
        if (boughtIncome) second.dispatch(GameAction.BuyUpgrade("room-1-basic-1"))
        second.save()
    }

    @Test
    fun coldStartWithoutSaveIsAFreshGame() {
        val session = newSession()
        assertEquals(0.0, session.state.progress.fish)
        assertFalse(session.isDirty)
    }

    @Test
    fun onScreenTimeAdvancesHunger() {
        val session = newSession()
        session.onForeground()
        repeat(60) {
            clock.pass(1.0)
            session.advance()
        }
        assertTrue(session.state.progress.hunger < 100.0)
        assertEquals(100.0 - 60 * 100 / (20 * 60.0), session.state.progress.hunger, 1e-9)
    }

    @Test
    fun timeAwayInTheBackgroundIsCreditedOnReturn() {
        seedSave(fish = 1000.0, boughtIncome = true)
        val session = newSession()
        session.onForeground()
        val before = session.state.progress.fish
        session.onBackground()
        clock.pass(3600.0)
        session.onForeground()
        assertEquals(before + 3600 * Economy.fishPerSecond(session.state), session.state.progress.fish, 1e-6)
        assertNotNull(session.state.offlineReport)
    }

    @Test
    fun rotationLikeQuickBackgroundGapIsHarmless() {
        seedSave(fish = 1000.0, boughtIncome = true)
        val session = newSession()
        session.onForeground()
        val before = session.state.progress.fish
        session.onBackground()
        clock.pass(0.3)
        session.onForeground()
        assertEquals(before, session.state.progress.fish, 1.0)
        assertNull(session.state.offlineReport)
    }

    @Test
    fun onBackgroundIsIdempotent() {
        val session = newSession()
        session.onForeground()
        session.onBackground()
        clock.pass(100.0)
        session.onBackground()
        session.onForeground()
        assertNotNull(session.state.offlineReport, "the whole absence counts from the first onBackground")
    }

    @Test
    fun killedProcessCreditsTheAbsenceExactlyOnce() {
        seedSave(fish = 500.0, boughtIncome = true)
        val income = Economy.fishPerSecond(newSession().state)
        assertEquals(1.0, income)

        clock.processGap(600.0)
        val afterFirstStart = newSession()
        val fish = afterFirstStart.state.progress.fish
        assertEquals(500.0 - 45 + 600, fish, 1e-6)

        // Killed again immediately, no time passes: the same absence must not be credited a second time.
        val afterSecondStart = newSession()
        assertEquals(fish, afterSecondStart.state.progress.fish, 1e-6)
    }

    @Test
    fun actionsAreSavedByTheNextSaveIfNeeded() {
        val session = newSession()
        session.onForeground()
        session.dispatch(GameAction.Click)
        assertTrue(session.isDirty)
        session.saveIfNeeded()
        assertFalse(session.isDirty)
        assertEquals(1.0, newSession().state.progress.fish, 1e-9)
    }

    @Test
    fun ticksAloneAreSavedOnlyEveryThirtySeconds() {
        val session = newSession()
        session.onForeground()
        session.dispatch(GameAction.Click)
        session.save()
        val firstStamp = repository.load()!!.savedAtMillis
        clock.pass(10.0)
        session.advance()
        session.saveIfNeeded()
        assertEquals(firstStamp, repository.load()!!.savedAtMillis, "10 s of ticks are not written yet")
        clock.pass(25.0)
        session.advance()
        session.saveIfNeeded()
        assertTrue(repository.load()!!.savedAtMillis > firstStamp)
    }

    @Test
    fun clockGoingBackwardsNeverCostsProgress() {
        seedSave(fish = 500.0, boughtIncome = true)
        val session = newSession()
        session.onForeground()
        val before = session.state.progress.fish
        session.onBackground()
        clock.wall -= 3_600_000
        session.onForeground()
        assertEquals(before, session.state.progress.fish, 1e-9)
    }

    @Test
    fun longFreezeInTheForegroundCountsAsAbsence() {
        seedSave(fish = 500.0, boughtIncome = true)
        val session = newSession()
        session.onForeground()
        val before = session.state.progress.fish
        clock.pass(120.0)
        session.advance()
        assertEquals(before + 120, session.state.progress.fish, 1e-6)
    }

    @Test
    fun savingInTheBackgroundDoesNotInventTime() {
        seedSave(fish = 500.0, boughtIncome = true)
        val session = newSession()
        session.onForeground()
        session.onBackground()
        val fishAtLeaving = session.state.progress.fish
        clock.processGap(1000.0)
        // A late periodic save must still be stamped with the moment the player left.
        session.dispatch(GameAction.ToggleLights)
        session.saveIfNeeded()
        val restarted = newSession()
        assertEquals(fishAtLeaving + 1000 * 1.0, restarted.state.progress.fish, 1e-6)
    }
}
