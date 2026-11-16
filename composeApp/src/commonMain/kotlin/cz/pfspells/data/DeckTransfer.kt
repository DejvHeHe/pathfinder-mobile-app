package cz.pfspells.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Formát souboru pro export/import decků. Spelly nesou id i jméno, aby šly spárovat i s jinou verzí databáze. */
@Serializable
data class DeckFile(
    val format: String = DeckTransfer.FORMAT,
    val version: Int = 1,
    val decks: List<DeckDto>,
)

@Serializable
data class DeckDto(
    val name: String,
    @SerialName("class") val classId: String,
    val level: Int,
    val spells: List<DeckSpellDto> = emptyList(),
)

@Serializable
data class DeckSpellDto(val id: String, val name: String, val count: Int = 1)

/** Deck připravený k importu (bez id) + spelly, které se nepodařilo najít. */
data class ImportedDeck(val deck: Deck, val missingSpells: List<String>)

class DeckImportException(message: String) : Exception(message)

object DeckTransfer {
    const val FORMAT = "pf-spellbook-decks"

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun export(decks: List<Deck>, db: SpellDatabase): String {
        val file = DeckFile(decks = decks.map { d ->
            DeckDto(
                name = d.name,
                classId = d.classId,
                level = d.classLevel,
                spells = d.spells.entries
                    .sortedBy { db.get(it.key)?.searchName ?: it.key }
                    .map { (id, count) -> DeckSpellDto(id, db.get(id)?.name ?: id, count) },
            )
        })
        return json.encodeToString(DeckFile.serializer(), file)
    }

    /** Vyhodí [DeckImportException] s českou hláškou, když text není platný export. */
    fun parse(text: String, db: SpellDatabase): List<ImportedDeck> {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) throw DeckImportException("Paste an export or choose a file.")
        val file = try {
            // přijmi i samotný deck (bez obálky)
            if (trimmed.contains("\"decks\"")) json.decodeFromString(DeckFile.serializer(), trimmed)
            else DeckFile(decks = listOf(json.decodeFromString(DeckDto.serializer(), trimmed)))
        } catch (e: Exception) {
            throw DeckImportException("This is not a valid deck export (${e.message?.lineSequence()?.firstOrNull() ?: "invalid JSON"}).")
        }
        if (file.format != FORMAT) throw DeckImportException("Unknown format \"${file.format}\".")
        if (file.version > 1) throw DeckImportException("The export comes from a newer app version (version ${file.version}).")
        if (file.decks.isEmpty()) throw DeckImportException("The export contains no decks.")

        return file.decks.map { dto ->
            CasterClasses.get(dto.classId)
                ?: throw DeckImportException("Deck \"${dto.name}\" has an unknown class \"${dto.classId}\".")
            val spells = LinkedHashMap<String, Int>()
            val missing = ArrayList<String>()
            for (s in dto.spells) {
                val spell = db.get(s.id) ?: db.findByName(s.name)
                if (spell == null) {
                    missing += s.name
                } else if (s.count > 0) {
                    spells[spell.id] = ((spells[spell.id] ?: 0) + s.count).coerceAtMost(99)
                }
            }
            ImportedDeck(
                Deck(
                    id = "",
                    name = dto.name.trim().ifEmpty { "Imported deck" },
                    classId = dto.classId,
                    classLevel = dto.level.coerceIn(1, 20),
                    spells = spells,
                ),
                missing,
            )
        }
    }

    /** Název souboru z názvu decku. */
    fun fileName(base: String): String = (SpellParser.slug(base).ifEmpty { "decks" }) + ".pfdeck.json"
}
