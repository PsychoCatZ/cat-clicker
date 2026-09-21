package dev.psychocat.catclicker.game.save

import dev.psychocat.catclicker.game.model.GameState
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SaveRepositoryTest {
    private lateinit var dir: File
    private lateinit var repository: SaveRepository

    @BeforeTest
    fun setUp() {
        dir = Files.createTempDirectory("catclicker-save").toFile()
        repository = SaveRepository(File(dir, "save"))
    }

    @AfterTest
    fun tearDown() {
        dir.deleteRecursively()
    }

    private fun stateWithFish(fish: Double) = GameState.initial().let {
        it.copy(rooms = it.rooms.mapIndexed { i, room -> if (i == 0) room.copy(fish = fish) else room })
    }

    private fun fishOf(save: LoadedSave?) = assertNotNull(save).state.rooms[0].fish

    @Test
    fun emptyDirectoryHasNoSave() {
        assertNull(repository.load())
    }

    @Test
    fun savedStateComesBack() {
        repository.save(stateWithFish(42.0), 1000L)
        assertEquals(42.0, fishOf(repository.load()))
        assertEquals(1000L, repository.load()!!.savedAtMillis)
    }

    @Test
    fun secondSaveKeepsThePreviousOneAsBackup() {
        repository.save(stateWithFish(1.0), 1000L)
        repository.save(stateWithFish(2.0), 2000L)
        assertEquals(2.0, fishOf(repository.load()))
        assertTrue(File(dir, "save/save.bak").isFile)
    }

    @Test
    fun corruptPrimaryFallsBackToTheBackup() {
        repository.save(stateWithFish(1.0), 1000L)
        repository.save(stateWithFish(2.0), 2000L)
        File(dir, "save/save.json").writeText("{ this is broken")
        assertEquals(1.0, fishOf(repository.load()))
    }

    @Test
    fun completeTemporaryFileFromAnInterruptedWriteWins() {
        repository.save(stateWithFish(1.0), 1000L)
        // Simulate a crash after the new file was written but before it replaced save.json.
        File(dir, "save/save.json.tmp").writeText(SaveCodec.encode(stateWithFish(9.0), 5000L))
        assertEquals(9.0, fishOf(repository.load()))
    }

    @Test
    fun halfWrittenTemporaryFileIsIgnored() {
        repository.save(stateWithFish(1.0), 1000L)
        File(dir, "save/save.json.tmp").writeText("""{"schemaVersion":1,"savedAt":9000,"rooms":[{"fish":""")
        assertEquals(1.0, fishOf(repository.load()))
    }

    @Test
    fun everythingCorruptMeansNoSave() {
        repository.save(stateWithFish(1.0), 1000L)
        File(dir, "save").listFiles()!!.forEach { it.writeText("garbage") }
        assertNull(repository.load())
    }

    @Test
    fun clearRemovesAllFiles() {
        repository.save(stateWithFish(1.0), 1000L)
        repository.save(stateWithFish(2.0), 2000L)
        repository.clear()
        assertNull(repository.load())
    }
}
