package cz.pfspells.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

actual object AppFiles {
    private val dir: File by lazy {
        File(System.getProperty("user.home"), ".pfspells").apply { mkdirs() }
    }

    actual suspend fun readText(name: String): String? =
        File(dir, name).takeIf { it.exists() }?.readText()

    actual suspend fun writeText(name: String, text: String) {
        val target = File(dir, name)
        val tmp = File(dir, "$name.tmp")
        tmp.writeText(text)
        if (!tmp.renameTo(target)) {
            target.delete(); tmp.renameTo(target)
        }
    }

    actual suspend fun delete(name: String) {
        File(dir, name).delete()
    }
}

actual fun createHttpClient(): HttpClient = HttpClient(OkHttp)

actual fun nowDisplayText(): String = SimpleDateFormat("d. M. yyyy HH:mm", Locale.getDefault()).format(Date())
