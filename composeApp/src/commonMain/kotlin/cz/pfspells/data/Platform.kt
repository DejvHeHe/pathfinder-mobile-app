package cz.pfspells.data

import io.ktor.client.HttpClient

/** Jednoduché úložiště textových souborů v soukromém adresáři appky. */
expect object AppFiles {
    suspend fun readText(name: String): String?
    suspend fun writeText(name: String, text: String)
    suspend fun delete(name: String)
}

/** HTTP klient s enginem dané platformy. */
expect fun createHttpClient(): HttpClient

/** Uložení / sdílení a otevření textového souboru (export a import decků). */
interface DeckFileIo {
    /** Druh tlačítka pro export (popisek v `Strings.saveAction`). */
    val saveAction: SaveAction
    /** Jestli platforma umí otevřít soubor (jinak jen vložení ze schránky). */
    val canOpen: Boolean
    /** @return true, když se uložení/sdílení spustilo. */
    suspend fun saveText(fileName: String, text: String): Boolean
    /** @return obsah vybraného souboru, nebo null při zrušení. */
    suspend fun openText(): String?
}

enum class SaveAction { SAVE_FILE, SHARE, DOWNLOAD }

expect val deckFileIo: DeckFileIo

/** Aktuální datum a čas jako text pro zobrazení (např. „16. 11. 2026 19:30“). */
expect fun nowDisplayText(): String

/** Zpět tažením od levého okraje obrazovky (iOS – nemá systémové tlačítko Zpět). */
expect val edgeSwipeBack: Boolean

/**
 * Anonymní událost pro analytiku webu (Umami, jen když ji provozovatel webu nastavil – jinak i na ostatních
 * platformách nic). Posílat jen název akce a obecné údaje (druh, classa, počet) – nikdy jména, texty ani id.
 */
expect fun trackEvent(name: String, data: Map<String, String> = emptyMap())
