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

/** Android má systémové gesto Zpět, desktop a web Esc / tlačítko prohlížeče. */
actual val edgeSwipeBack: Boolean = false

/** Analytika jen na webu. */
actual fun trackEvent(name: String, data: Map<String, String>) {}

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

// ---------- aktualizace ----------

/**
 * Desktop: instalátor podle systému (Windows .msi, macOS .dmg, Linux .deb) se stáhne v prohlížeči – běžící appka
 * se sama přepsat nemůže, uživatel instalátor spustí (Windows MSI díky upgradeUuid nahradí starou verzi).
 */
actual val appUpdater: AppUpdater = object : AppUpdater {
    override val assetExtensions: List<String> = run {
        val os = System.getProperty("os.name").orEmpty().lowercase()
        when {
            "win" in os -> listOf(".msi")
            "mac" in os || "darwin" in os -> listOf(".dmg")
            "linux" in os -> listOf(".deb")
            else -> emptyList()
        }
    }

    /** Desktop nic předem nestahuje – instalátor stáhne prohlížeč. */
    override suspend fun prepare(asset: GithubAsset, onProgress: (Float?) -> Unit) = PreparedUpdate(asset)

    override fun install(update: PreparedUpdate): Boolean = browse(update.asset.downloadUrl)

    override fun openPage(url: String) {
        browse(url)
    }

    private fun browse(url: String): Boolean = try {
        if (!java.awt.Desktop.isDesktopSupported()) false
        else java.awt.Desktop.getDesktop().takeIf { it.isSupported(java.awt.Desktop.Action.BROWSE) }
            ?.let { it.browse(java.net.URI(url)); true } ?: false
    } catch (e: Exception) {
        false
    }
}
