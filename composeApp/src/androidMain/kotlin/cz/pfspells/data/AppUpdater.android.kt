package cz.pfspells.data

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.core.content.FileProvider
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.lang.ref.WeakReference

actual val appUpdater: AppUpdater = AndroidAppUpdater

/**
 * Android: APK z release se stáhne do cache appky (`cache/updates/`), ověří se jeho podpis a po potvrzení uživatelem
 * se otevře systémový instalátor.
 * Soubor se instalátoru předává přes [FileProvider] (authority `<balíček>.updates`, `res/xml/update_paths.xml`) –
 * od Androidu 7 nejde předat jiné appce cestu `file://`. Instalaci z appky musí uživatel jednou povolit
 * (oprávnění `REQUEST_INSTALL_PACKAGES`, systém se zeptá sám).
 *
 * Aktualizace přes starou verzi projde jen se STEJNÝM podpisovým klíčem APK – s dočasným debug klíčem CI
 * instalátor nahlásí konflikt a je nutné starou verzi odinstalovat (viz docs/deploy.md – podpis APK).
 */
object AndroidAppUpdater : AppUpdater {
    private var activityRef: WeakReference<ComponentActivity>? = null

    /** Volá MainActivity v onCreate. */
    fun register(activity: ComponentActivity) {
        activityRef = WeakReference(activity)
    }

    override val assetExtensions: List<String> = listOf(".apk")

    /** Stáhne APK a porovná jeho podpis s nainstalovanou verzí – instalace až po potvrzení uživatelem. */
    override suspend fun prepare(asset: GithubAsset, onProgress: (Float?) -> Unit): PreparedUpdate {
        val activity = activityRef?.get() ?: error("App is not running")
        val file = download(asset, File(activity.cacheDir, "updates"), onProgress)
        val matches = withContext(Dispatchers.IO) { signatureMatches(activity, file) }
        return PreparedUpdate(asset, file.absolutePath, matches)
    }

    override fun install(update: PreparedUpdate): Boolean {
        val activity = activityRef?.get() ?: return false
        val file = update.localPath?.let(::File)?.takeIf { it.exists() } ?: return false
        val uri = FileProvider.getUriForFile(activity, "${activity.packageName}.updates", file)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        return try {
            activity.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }

    /**
     * Podepsal stažené APK stejný klíč jako nainstalovanou appku? Android jinak aktualizaci odmítne („konflikt balíčků“)
     * a jediná cesta je odinstalovat – tím se smažou postavy a inventáře. null = podpis nejde přečíst.
     * Od API 28 se bere i historie klíčů (rotace podpisu), dřív jen aktuální podpis.
     */
    @Suppress("DEPRECATION")
    private fun signatureMatches(context: Context, apk: File): Boolean? = try {
        val pm = context.packageManager
        val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        val installed = signers(pm.getPackageInfo(context.packageName, flags))
        val downloaded = signers(pm.getPackageArchiveInfo(apk.absolutePath, flags))
        if (installed == null || downloaded == null) null else installed.any { it in downloaded }
    } catch (e: Exception) {
        null
    }

    @Suppress("DEPRECATION")
    private fun signers(info: PackageInfo?): Set<String>? {
        info ?: return null
        val sigs: Array<Signature>? = if (Build.VERSION.SDK_INT >= 28) {
            info.signingInfo?.let { if (it.hasMultipleSigners()) it.apkContentsSigners else it.signingCertificateHistory }
        } else {
            info.signatures
        }
        return sigs?.map { it.toCharsString() }?.toSet()?.takeIf { it.isNotEmpty() }
    }

    /** Stažení po blocích s průběhem; staré APK v adresáři se nejdřív smažou (každá verze má ~30 MB). */
    private suspend fun download(asset: GithubAsset, dir: File, onProgress: (Float?) -> Unit): File = withContext(Dispatchers.IO) {
        dir.mkdirs()
        dir.listFiles()?.forEach { it.delete() }
        val target = File(dir, asset.name.replace(Regex("[^A-Za-z0-9._-]"), "_"))
        val client = createHttpClient()
        try {
            client.prepareGet(asset.downloadUrl).execute { resp ->
                if (!resp.status.isSuccess()) error("Download failed: HTTP ${resp.status.value}")
                // GitHub přesměruje na úložiště souborů – velikost bereme z odpovědi, jinak z API
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
                        val progress = total?.let { (done.toFloat() / it).coerceIn(0f, 1f) }
                        withContext(Dispatchers.Main) { onProgress(progress) }
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

    override fun openPage(url: String) {
        val activity = activityRef?.get() ?: return
        try {
            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: ActivityNotFoundException) {
        }
    }
}
