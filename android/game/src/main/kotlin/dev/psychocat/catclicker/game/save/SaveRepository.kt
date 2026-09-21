package dev.psychocat.catclicker.game.save

import dev.psychocat.catclicker.game.model.GameState
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Crash-safe storage of one save. A write goes through three files:
 *
 *  1. `save.json.tmp` is written completely and fsynced,
 *  2. the previous `save.json` is moved to `save.bak`,
 *  3. the temporary file is renamed to `save.json`.
 *
 * Whatever moment the process dies at, at least one complete file exists. [load] decodes all three and returns
 * the newest readable one, so a half-finished write can never destroy progress.
 */
class SaveRepository(private val directory: File) {
    private val primary = File(directory, "save.json")
    private val temporary = File(directory, "save.json.tmp")
    private val backup = File(directory, "save.bak")

    @Synchronized
    fun save(state: GameState, savedAtMillis: Long) {
        if (!directory.isDirectory && !directory.mkdirs()) throw IOException("Cannot create $directory")
        val bytes = SaveCodec.encode(state, savedAtMillis).toByteArray(Charsets.UTF_8)
        FileOutputStream(temporary).use { out ->
            out.write(bytes)
            out.flush()
            out.fd.sync()
        }
        if (primary.exists()) {
            backup.delete()
            if (!primary.renameTo(backup)) throw IOException("Cannot rotate $primary")
        }
        if (!temporary.renameTo(primary)) throw IOException("Cannot publish $temporary")
    }

    @Synchronized
    fun load(): LoadedSave? =
        listOf(primary, temporary, backup)
            .mapNotNull { file -> read(file) }
            .maxByOrNull { it.savedAtMillis }

    @Synchronized
    fun clear() {
        listOf(primary, temporary, backup).forEach { it.delete() }
    }

    private fun read(file: File): LoadedSave? =
        try {
            if (file.isFile) SaveCodec.decode(file.readText(Charsets.UTF_8)) else null
        } catch (e: IOException) {
            null
        }
}
