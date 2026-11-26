package cz.pfspells

import cz.pfspells.data.AppUpdates
import cz.pfspells.data.GithubAsset
import cz.pfspells.data.GithubRelease
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Kontrola aktualizací: porovnání verzí, výběr souboru podle platformy, čtení odpovědi GitHub API. */
class AppUpdateTest {
    private val assets = listOf(
        GithubAsset("pf-spellbook-1.5.0.apk", "https://example/apk", 30_000_000),
        GithubAsset("PF Spellbook-1.5.0.msi", "https://example/msi"),
        GithubAsset("PF Spellbook-1.5.0.dmg", "https://example/dmg"),
        GithubAsset("pf-spellbook_1.5.0_amd64.deb", "https://example/deb"),
        GithubAsset("pf-spellbook-web-1.5.0.zip", "https://example/zip"),
    )

    @Test
    fun parsesVersions() {
        assertEquals(listOf(1, 5, 0), AppUpdates.parseVersion("v1.5.0"))
        assertEquals(listOf(1, 10, 2), AppUpdates.parseVersion("1.10.2"))
        assertNull(AppUpdates.parseVersion("1.5"))
        assertNull(AppUpdates.parseVersion("v1.5.0-beta"))
        assertNull(AppUpdates.parseVersion("latest"))
    }

    @Test
    fun comparesNumerically() {
        assertTrue(AppUpdates.isNewer("v1.5.0", "1.4.0"))
        assertTrue(AppUpdates.isNewer("v1.10.0", "1.9.9")) // textově by „1.10“ < „1.9“
        assertTrue(AppUpdates.isNewer("v2.0.0", "1.99.99"))
        assertFalse(AppUpdates.isNewer("v1.5.0", "1.5.0"))
        assertFalse(AppUpdates.isNewer("v1.4.9", "1.5.0"))
        assertFalse(AppUpdates.isNewer("nightly", "1.0.0"))
    }

    @Test
    fun picksAssetByPlatform() {
        assertEquals("https://example/apk", AppUpdates.pickAsset(assets, listOf(".apk"))?.downloadUrl)
        assertEquals("https://example/msi", AppUpdates.pickAsset(assets, listOf(".msi"))?.downloadUrl)
        assertEquals("https://example/deb", AppUpdates.pickAsset(assets, listOf(".deb"))?.downloadUrl)
        assertNull(AppUpdates.pickAsset(assets, listOf(".exe")))
        assertNull(AppUpdates.pickAsset(assets, emptyList()))
    }

    @Test
    fun evaluatesRelease() {
        val release = GithubRelease("v1.5.0", "https://github.com/x/releases/tag/v1.5.0", assets = assets)
        val info = AppUpdates.evaluate(release, "1.4.0", listOf(".apk"))!!
        assertEquals("1.5.0", info.version)
        assertEquals("https://example/apk", info.asset?.downloadUrl)
        // stejná verze, pre-release a draft se nehlásí
        assertNull(AppUpdates.evaluate(release, "1.5.0", listOf(".apk")))
        assertNull(AppUpdates.evaluate(release.copy(prerelease = true), "1.4.0", listOf(".apk")))
        assertNull(AppUpdates.evaluate(release.copy(draft = true), "1.4.0", listOf(".apk")))
        // platforma bez souboru v release → jen odkaz na stránku
        assertNull(AppUpdates.evaluate(release, "1.4.0", listOf(".exe"))!!.asset)
    }

    @Test
    fun parsesGithubResponse() {
        // zkrácená odpověď GitHub API – neznámá pole (author, body…) se ignorují
        val json = """
            {"url":"https://api.github.com/…","tag_name":"v1.5.0","name":"v1.5.0","draft":false,"prerelease":false,
             "html_url":"https://github.com/DejvHeHe/pathfinder-mobile-app/releases/tag/v1.5.0",
             "author":{"login":"x"},"body":"notes",
             "assets":[{"name":"pf-spellbook-1.5.0.apk","size":31457280,"content_type":"application/vnd.android.package-archive",
                        "browser_download_url":"https://github.com/DejvHeHe/pathfinder-mobile-app/releases/download/v1.5.0/pf-spellbook-1.5.0.apk"}]}
        """.trimIndent()
        val r = AppUpdates.parseRelease(json)
        assertEquals("v1.5.0", r.tagName)
        assertEquals(1, r.assets.size)
        assertEquals(31457280L, r.assets[0].size)
        assertTrue(r.assets[0].downloadUrl.endsWith(".apk"))
    }
}
