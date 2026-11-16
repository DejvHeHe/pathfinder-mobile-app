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
    /** Popisek tlačítka pro export: „Uložit do souboru“ / „Sdílet“. */
    val saveLabel: String
    /** Jestli platforma umí otevřít soubor (jinak jen vložení ze schránky). */
    val canOpen: Boolean
    /** @return true, když se uložení/sdílení spustilo. */
    suspend fun saveText(fileName: String, text: String): Boolean
    /** @return obsah vybraného souboru, nebo null při zrušení. */
    suspend fun openText(): String?
}

expect val deckFileIo: DeckFileIo

/** Aktuální datum a čas jako text pro zobrazení (např. „16. 11. 2026 19:30“). */
expect fun nowDisplayText(): String
