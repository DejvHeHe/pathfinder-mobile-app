package cz.pfspells.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

actual val deckFileIo: DeckFileIo = object : DeckFileIo {
    override val saveLabel = "Save to file"
    override val canOpen = true

    override suspend fun saveText(fileName: String, text: String): Boolean {
        val dialog = FileDialog(null as Frame?, "Save deck", FileDialog.SAVE)
        dialog.file = fileName
        dialog.isVisible = true // modální, vrátí se po zavření
        val name = dialog.file ?: return false
        val target = File(dialog.directory, if (name.endsWith(".json")) name else "$name.json")
        withContext(Dispatchers.IO) { target.writeText(text) }
        return true
    }

    override suspend fun openText(): String? {
        val dialog = FileDialog(null as Frame?, "Open deck export", FileDialog.LOAD)
        dialog.file = "*.json"
        dialog.isVisible = true
        val name = dialog.file ?: return null
        return withContext(Dispatchers.IO) { File(dialog.directory, name).readText() }
    }
}
