package cz.pfspells.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import io.ktor.utils.io.readAvailable
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

/** Desktop (JVM) nastavení omezení pohybu jednotně nezná. */
actual fun systemReducedMotion(): Boolean = false

/** Analytika jen na webu. */
actual fun trackEvent(name: String, data: Map<String, String>) {}

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

// ---------- aktualizace ----------

/**
 * Desktop: instalátor podle systému (Windows .msi, macOS .dmg, Linux .deb) stáhne appka sama do složky Stažené soubory
 * (s průběhem), po potvrzení ho spustí a sama se zavře – běžící appku instalátor přepsat nemůže (Windows MSI díky
 * upgradeUuid nahradí starou verzi). Když stažení nebo spuštění selže, otevře se stažení v prohlížeči.
 */
actual val appUpdater: AppUpdater = object : AppUpdater {
    private val os = System.getProperty("os.name").orEmpty().lowercase()

    override val assetExtensions: List<String> = when {
        "win" in os -> listOf(".msi")
        "mac" in os || "darwin" in os -> listOf(".dmg")
        "linux" in os -> listOf(".deb")
        else -> emptyList()
    }

    override suspend fun prepare(asset: GithubAsset, onProgress: (Float?) -> Unit): PreparedUpdate {
        val downloads = File(System.getProperty("user.home"), "Downloads").takeIf { it.isDirectory }
            ?: File(System.getProperty("user.home"), ".pfspells/updates").apply { mkdirs() }
        return PreparedUpdate(asset, download(asset, downloads, onProgress).absolutePath)
    }

    /** Spustí stažený instalátor a appku za chvíli ukončí (data jsou uložená průběžně). */
    override fun install(update: PreparedUpdate): Boolean {
        val file = update.localPath?.let(::File)?.takeIf { it.exists() } ?: return false
        val started = try {
            val cmd = when {
                "win" in os -> listOf("msiexec", "/i", file.absolutePath)
                "mac" in os || "darwin" in os -> listOf("open", file.absolutePath)
                else -> listOf("xdg-open", file.absolutePath)
            }
            ProcessBuilder(cmd).start()
            true
        } catch (e: Exception) {
            try {
                java.awt.Desktop.getDesktop().open(file)
                true
            } catch (e2: Exception) {
                false
            }
        }
        if (started) Thread {
            Thread.sleep(1500)
            kotlin.system.exitProcess(0)
        }.apply { isDaemon = true }.start()
        return started
    }

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

    /** Stažení po blocích s průběhem (stejné jako na Androidu); nepovedené stažení se smaže. */
    private suspend fun download(asset: GithubAsset, dir: File, onProgress: (Float?) -> Unit): File =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val target = File(dir, asset.name.replace(Regex("[^A-Za-z0-9._-]"), "_"))
            val client = createHttpClient()
            try {
                client.prepareGet(asset.downloadUrl).execute { resp ->
                    if (!resp.status.isSuccess()) error("Download failed: HTTP ${resp.status.value}")
                    val total = resp.contentLength()?.takeIf { it > 0 } ?: asset.size.takeIf { it > 0 }
                    val channel = resp.bodyAsChannel()
                    val buffer = ByteArray(64 * 1024)
                    var done = 0L
                    target.outputStream().use { out ->
                        while (!channel.isClosedForRead) {
                            val n = channel.readAvailable(buffer)
                            if (n <= 0) continue
                            out.write(buffer, 0, n)
                            done += n
                            onProgress(total?.let { (done.toFloat() / it).coerceIn(0f, 1f) })
                        }
                    }
                }
            } catch (e: Exception) {
                target.delete()
                throw e
            } finally {
                client.close()
            }
            target
        }
}
