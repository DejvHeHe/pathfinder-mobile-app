package cz.pfspells.data

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/*
 * Kontrola aktualizací appky z GitHub Releases (https://github.com/DejvHeHe/pathfinder-mobile-app/releases).
 * Jen veřejné API bez přihlášení (limit 60 požadavků / h z jedné IP) – appka kontroluje nejvýš jednou denně.
 * Co se stáhne / otevře, rozhoduje platforma ([appUpdater]): Android APK, desktop instalátor, web a iOS nic.
 */

/** Release z GitHub API – jen pole, která appka potřebuje (zbytek odpovědi se ignoruje). */
@Serializable
data class GithubRelease(
    @SerialName("tag_name") val tagName: String,
    @SerialName("html_url") val htmlUrl: String,
    val name: String? = null,
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    val assets: List<GithubAsset> = emptyList(),
)

@Serializable
data class GithubAsset(
    val name: String,
    @SerialName("browser_download_url") val downloadUrl: String,
    val size: Long = 0,
)

/** Nalezená novější verze: [asset] = soubor pro tuto platformu (null = jen odkaz na stránku release). */
data class UpdateInfo(
    val version: String,
    val pageUrl: String,
    val asset: GithubAsset?,
)

object AppUpdates {
    const val LATEST_RELEASE_API = "https://api.github.com/repos/DejvHeHe/pathfinder-mobile-app/releases/latest"

    private val json = Json { ignoreUnknownKeys = true }

    /** „v1.5.0“ / „1.5.0“ → [1, 5, 0]; jiný tvar (např. „1.0.0-beta“) → null. */
    fun parseVersion(tag: String): List<Int>? {
        val parts = tag.trim().removePrefix("v").removePrefix("V").split('.')
        if (parts.size != 3) return null
        return parts.map { it.toIntOrNull()?.takeIf { n -> n >= 0 } ?: return null }
    }

    /** Je [remote] novější než [current]? Porovnává se číselně po částech (1.10.0 > 1.9.0). */
    fun isNewer(remote: String, current: String): Boolean {
        val r = parseVersion(remote) ?: return false
        val c = parseVersion(current) ?: return false
        for (i in 0 until 3) if (r[i] != c[i]) return r[i] > c[i]
        return false
    }

    /** První asset s příponou podle pořadí [extensions] (např. „.apk“, „.msi“). */
    fun pickAsset(assets: List<GithubAsset>, extensions: List<String>): GithubAsset? =
        extensions.firstNotNullOfOrNull { ext -> assets.firstOrNull { it.name.endsWith(ext, ignoreCase = true) } }

    /** Novější release pro tuto platformu, nebo null (stejná / starší verze, draft, pre-release, jiný tvar tagu). */
    fun evaluate(release: GithubRelease, currentVersion: String, extensions: List<String>): UpdateInfo? {
        if (release.draft || release.prerelease || !isNewer(release.tagName, currentVersion)) return null
        val version = release.tagName.removePrefix("v")
        return UpdateInfo(version, release.htmlUrl, pickAsset(release.assets, extensions))
    }

    fun parseRelease(text: String): GithubRelease = json.decodeFromString(GithubRelease.serializer(), text)

    /** Stáhne poslední release z GitHub API (vyhodí výjimku při chybě sítě / HTTP). */
    suspend fun fetchLatest(): GithubRelease {
        val client = createHttpClient()
        val text = try {
            val resp = client.get(LATEST_RELEASE_API) {
                header("Accept", "application/vnd.github+json")
            }
            if (!resp.status.isSuccess()) error("GitHub returned ${resp.status.value}")
            resp.bodyAsText()
        } finally {
            client.close()
        }
        return parseRelease(text)
    }
}

/**
 * Aktualizace připravená k instalaci – mezi stažením a instalací se appka zeptá uživatele (a nabídne zálohu postav).
 * [localPath] = stažený soubor (Android), null = instalátor se stáhne až v prohlížeči (desktop).
 * [signatureMatches] = podpis staženého APK sedí s nainstalovanou verzí (false = Android ho přes ni nenainstaluje,
 * je nutné odinstalovat → smažou se data; null = neznámé / desktop).
 */
data class PreparedUpdate(
    val asset: GithubAsset,
    val localPath: String? = null,
    val signatureMatches: Boolean? = null,
)

/**
 * Aktualizace na dané platformě. [assetExtensions] prázdné = platforma aktualizace nenabízí
 * (web je vždy aktuální ze serveru, iOS se staví v Xcode).
 */
interface AppUpdater {
    /** Přípony souborů z release v pořadí preference (Android „.apk“, Windows „.msi“…). */
    val assetExtensions: List<String>

    /** Připraví instalaci: Android stáhne APK ([onProgress] 0–1, null = neznámá velikost) a ověří podpis, desktop nic. */
    suspend fun prepare(asset: GithubAsset, onProgress: (Float?) -> Unit): PreparedUpdate

    /** Spustí instalaci (Android instalátor, desktop stažení v prohlížeči). Vrací true, když se spustila. */
    fun install(update: PreparedUpdate): Boolean

    /** Otevře adresu v prohlížeči (stránka release nebo přímé stažení souboru). */
    fun openPage(url: String)
}

val AppUpdater.isSupported: Boolean get() = assetExtensions.isNotEmpty()

expect val appUpdater: AppUpdater
