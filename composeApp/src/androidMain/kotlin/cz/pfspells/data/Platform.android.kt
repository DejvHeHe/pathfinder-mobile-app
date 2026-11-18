package cz.pfspells.data

import android.content.Context
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

actual object AppFiles {
    private var dir: File? = null

    /** Volá MainActivity před prvním použitím. */
    fun init(context: Context) {
        dir = context.applicationContext.filesDir
    }

    private fun file(name: String) = File(requireNotNull(dir) { "AppFiles.init() was not called" }, name)

    actual suspend fun readText(name: String): String? = file(name).takeIf { it.exists() }?.readText()

    actual suspend fun writeText(name: String, text: String) {
        val target = file(name)
        val tmp = File(target.parentFile, "$name.tmp")
        tmp.writeText(text)
        if (!tmp.renameTo(target)) {
            target.delete(); tmp.renameTo(target)
        }
    }

    actual suspend fun delete(name: String) {
        file(name).delete()
    }
}

actual fun createHttpClient(): HttpClient = HttpClient(OkHttp)

actual fun nowDisplayText(): String = SimpleDateFormat("d. M. yyyy HH:mm", Locale.getDefault()).format(Date())

/** Android má systémové gesto Zpět, desktop a web Esc / tlačítko prohlížeče. */
actual val edgeSwipeBack: Boolean = false
