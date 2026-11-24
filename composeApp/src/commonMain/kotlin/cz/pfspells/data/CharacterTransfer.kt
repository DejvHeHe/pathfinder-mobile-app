package cz.pfspells.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

// ---------------- formát „pf-spellbook-characters“ v1 ----------------

@Serializable
data class CharacterFile(
    val format: String = CharacterTransfer.FORMAT,
    val version: Int = 1,
    val characters: List<CharacterDto>,
)

/** Postava v exportu. Spelly a featy nesou id i jméno (spárování s jinou verzí databáze); herní režim se neexportuje. */
@Serializable
data class CharacterDto(
    val name: String,
    val classes: List<ClassDto>,
    val abilities: Abilities = Abilities(),
    val feats: List<FeatDto> = emptyList(),
    val extraFeats: Int = 0,
    /** propojený inventář (jen když ho uživatel přibalil) */
    val inventory: InventoryDto? = null,
    val trackers: List<Tracker> = emptyList(),
    val hiddenPresets: Set<String> = emptySet(),
    /** rasa ([RaceCatalog] id); neznámá rasa se při importu zahodí */
    val race: String? = null,
    val raceBonus: Ability? = null,
    val skills: Map<String, Int> = emptyMap(),
    val extraSkillPoints: Int = 0,
    val alignment: Alignment? = null,
    val deity: String = "",
    val confirmedPrereqs: Set<String> = emptySet(),
    /** mazlíček: pořadí vlastníka v tomto exportu (0 = první postava) */
    val owner: Int? = null,
    val companion: CompanionKind? = null,
    val size: CreatureSize? = null,
    val quadruped: Boolean = false,
    /** druh zvířete ([SpeciesCatalog] id) a efektivní úroveň druida zvířecího společníka */
    val species: String? = null,
    val companionLevel: Int? = null,
    val rarity: CompanionRarity? = null,
    val respawnRules: String? = null,
    /** obrana a životy (zranění se jako seslaná kouzla neexportuje) */
    val hpBonus: Int = 0,
    val hpOverride: Int? = null,
    val acMisc: Int = 0,
    /** alternativní rasové traity */
    val racialTraits: List<RacialTraitPick> = emptyList(),
)

@Serializable
data class ClassDto(
    @SerialName("class") val classId: String,
    val level: Int,
    val subclass: Map<String, String> = emptyMap(),
    val spells: List<DeckSpellDto> = emptyList(),
    /** doménový slot */
    val bonusSpells: List<DeckSpellDto> = emptyList(),
    /** hexy / revelations */
    val abilities: List<AbilityPick> = emptyList(),
)

@Serializable
data class FeatDto(
    val id: String? = null,
    val name: String,
    val note: String = "",
    val text: String? = null,
)

// ---------------- formát „pf-spellbook-feats“ v1 ----------------

@Serializable
data class FeatFile(
    val format: String = CharacterTransfer.FEATS_FORMAT,
    val version: Int = 1,
    val feats: List<FeatDto>,
)

/** Postava připravená k importu (bez id) + její inventář a spelly, které se nepodařilo najít. */
data class ImportedCharacter(
    val character: Character,
    val inventory: Inventory?,
    val missingSpells: List<String>,
    /** mazlíček: pořadí vlastníka v importu (id se při importu přidělují znovu) */
    val ownerIndex: Int? = null,
)

object CharacterTransfer {
    const val FORMAT = "pf-spellbook-characters"
    const val FEATS_FORMAT = "pf-spellbook-feats"

    @OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true; explicitNulls = false }

    /** Mazlíček se exportuje s odkazem na pořadí vlastníka – jen když je vlastník v [characters] taky. */
    fun export(characters: List<Character>, db: SpellDatabase, inventoryOf: (Character) -> Inventory?): String =
        json.encodeToString(CharacterFile.serializer(), CharacterFile(characters = characters.map { c ->
            CharacterDto(
                owner = c.ownerId?.let { o -> characters.indexOfFirst { it.id == o }.takeIf { it >= 0 } },
                companion = c.companion,
                size = c.size,
                quadruped = c.quadruped,
                species = c.species,
                companionLevel = c.companionLevel,
                rarity = c.rarity,
                respawnRules = c.respawnRules,
                hpBonus = c.hpBonus,
                hpOverride = c.hpOverride,
                acMisc = c.acMisc,
                racialTraits = c.racialTraits,
                name = c.name,
                classes = c.classes.map { e ->
                    ClassDto(
                        classId = e.classId, level = e.level, subclass = e.subclass,
                        spells = spellDtos(e.spells, db),
                        bonusSpells = spellDtos(e.bonusSpells, db),
                        abilities = e.abilities,
                    )
                },
                abilities = c.abilities,
                feats = c.feats.map(::featDto),
                extraFeats = c.extraFeats,
                inventory = inventoryOf(c)?.let(InventoryTransfer::toDto),
                trackers = c.trackers,
                hiddenPresets = c.hiddenPresets,
                race = c.race,
                raceBonus = c.raceBonus,
                skills = c.skills,
                extraSkillPoints = c.extraSkillPoints,
                alignment = c.alignment,
                deity = c.deity,
                confirmedPrereqs = c.confirmedPrereqs,
            )
        }))

    private fun spellDtos(m: Map<String, Int>, db: SpellDatabase) = m.entries
        .sortedBy { db.get(it.key)?.searchName ?: it.key }
        .map { (id, n) -> DeckSpellDto(id, db.get(id)?.name ?: id, n) }

    fun exportFeats(feats: List<FeatPick>): String =
        json.encodeToString(FeatFile.serializer(), FeatFile(feats = feats.map(::featDto)))

    private fun featDto(p: FeatPick) = FeatDto(p.featId, p.name, p.note, p.text)

    /** Formát exportu (pole `format`), nebo null, když text není JSON objekt. */
    private fun formatOf(text: String): String? = try {
        (json.parseToJsonElement(text) as? JsonObject)?.get("format")?.jsonPrimitive?.content
    } catch (e: Exception) {
        null
    }

    /**
     * Postavy z exportu – nový formát i starý `pf-spellbook-decks` v1 (deck = postava s jednou classou).
     * @throws DeckImportException
     */
    fun parse(text: String, db: SpellDatabase, featDb: FeatDatabase?, itemDb: ItemDatabase?): List<ImportedCharacter> {
        val t = text.trim()
        if (t.isEmpty()) throw DeckImportException(ImportError.Empty)
        val format = formatOf(t)
        if (format == DeckTransfer.FORMAT || (format == null && t.contains("\"decks\""))) {
            return DeckTransfer.parse(t, db).map { ImportedCharacter(it.deck.toCharacter(), null, it.missingSpells) }
        }
        val file = try {
            json.decodeFromString(CharacterFile.serializer(), t)
        } catch (e: Exception) {
            // samotný deck bez obálky (starý formát)
            if (format == null) return DeckTransfer.parse(t, db).map { ImportedCharacter(it.deck.toCharacter(), null, it.missingSpells) }
            throw DeckImportException(ImportError.Invalid(inventory = false, e.message?.lineSequence()?.firstOrNull()))
        }
        if (file.format != FORMAT) throw DeckImportException(ImportError.UnknownFormat(file.format))
        if (file.version > 1) throw DeckImportException(ImportError.NewerVersion(file.version))
        if (file.characters.isEmpty()) throw DeckImportException(ImportError.NoDecks)

        return file.characters.mapIndexed { index, dto ->
            val missing = ArrayList<String>()
            val classes = dto.classes.map { cd ->
                ClassCatalog.get(cd.classId) ?: throw DeckImportException(ImportError.UnknownClass(dto.name, cd.classId))
                fun match(list: List<DeckSpellDto>): Map<String, Int> {
                    val out = LinkedHashMap<String, Int>()
                    for (s in list) {
                        val spell = db.get(s.id) ?: db.findByName(s.name)
                        if (spell == null) missing += s.name
                        else if (s.count > 0) out[spell.id] = ((out[spell.id] ?: 0) + s.count).coerceAtMost(99)
                    }
                    return out
                }
                ClassEntry(
                    cd.classId, cd.level.coerceIn(1, 20), cd.subclass, match(cd.spells), match(cd.bonusSpells),
                    abilities = cd.abilities.filter { it.id.isNotBlank() && it.name.isNotBlank() }.distinctBy { it.id },
                )
            }.distinctBy { it.classId }
            if (classes.isEmpty()) throw DeckImportException(ImportError.Invalid(inventory = false, "no classes"))
            ImportedCharacter(
                Character(
                    id = "",
                    name = dto.name.trim().ifEmpty { "Imported character" },
                    classes = classes,
                    abilities = dto.abilities,
                    feats = dto.feats.mapNotNull { pickOf(it, featDb) },
                    extraFeats = dto.extraFeats.coerceIn(-20, 40),
                    trackers = dto.trackers.filter { it.name.isNotBlank() }.map { it.copy(id = newTrackerId(), max = it.max.coerceIn(1, 999)) },
                    hiddenPresets = dto.hiddenPresets,
                    race = dto.race?.takeIf { RaceCatalog.get(it) != null },
                    raceBonus = dto.raceBonus,
                    skills = dto.skills.filter { (k, v) -> Skills.get(Skills.idOf(k)) != null && v > 0 },
                    extraSkillPoints = dto.extraSkillPoints,
                    alignment = dto.alignment,
                    deity = dto.deity,
                    confirmedPrereqs = dto.confirmedPrereqs,
                    companion = dto.companion,
                    size = dto.size,
                    quadruped = dto.quadruped && dto.size != null,
                    species = dto.species?.takeIf { SpeciesCatalog.get(it) != null },
                    companionLevel = dto.companionLevel?.coerceIn(1, 20),
                    rarity = dto.rarity,
                    respawnRules = dto.respawnRules?.takeIf { it.isNotBlank() },
                    hpBonus = dto.hpBonus.coerceIn(-999, 999),
                    hpOverride = dto.hpOverride?.coerceIn(1, 9999),
                    acMisc = dto.acMisc.coerceIn(-50, 50),
                    // traity jen k rase, která se importovala
                    racialTraits = if (dto.race?.let { RaceCatalog.get(it) } != null)
                        dto.racialTraits.filter { it.id.isNotBlank() && it.name.isNotBlank() }.distinctBy { it.id } else emptyList(),
                ),
                dto.inventory?.let { InventoryTransfer.fromDto(it, itemDb) },
                missing,
                ownerIndex = dto.owner?.takeIf { it in file.characters.indices && it != index },
            )
        }
    }

    /** Featy z exportu featů (případně i z exportu postav – vezmou se featy všech postav). @throws DeckImportException */
    fun parseFeats(text: String, featDb: FeatDatabase?): List<FeatPick> {
        val t = text.trim()
        if (t.isEmpty()) throw DeckImportException(ImportError.Empty)
        val format = formatOf(t)
        val dtos = try {
            when (format) {
                FEATS_FORMAT -> json.decodeFromString(FeatFile.serializer(), t).also {
                    if (it.version > 1) throw DeckImportException(ImportError.NewerVersion(it.version))
                }.feats
                FORMAT -> json.decodeFromString(CharacterFile.serializer(), t).characters.flatMap { it.feats }
                null -> throw DeckImportException(ImportError.Invalid(inventory = false, null))
                else -> throw DeckImportException(ImportError.UnknownFormat(format))
            }
        } catch (e: DeckImportException) {
            throw e
        } catch (e: Exception) {
            throw DeckImportException(ImportError.Invalid(inventory = false, e.message?.lineSequence()?.firstOrNull()))
        }
        val picks = dtos.mapNotNull { pickOf(it, featDb) }
        if (picks.isEmpty()) throw DeckImportException(ImportError.NoFeats)
        return picks
    }

    /** Feat z databáze spárovaný podle id, pak jména; nenalezený s id zůstane (jiná verze dat), bez id = vlastní. */
    private fun pickOf(d: FeatDto, featDb: FeatDatabase?): FeatPick? {
        if (d.name.isBlank()) return null
        val feat = d.id?.let { featDb?.get(it) } ?: featDb?.byName(d.name)?.takeIf { d.id != null }
        return FeatPick(feat?.id ?: d.id, feat?.name ?: d.name.trim(), d.note.trim(), d.text?.trim()?.ifEmpty { null })
    }

    private fun newTrackerId(): String = kotlin.random.Random.nextLong().toULong().toString(36)

    fun fileName(base: String): String = (SpellParser.slug(base).ifEmpty { "characters" }) + ".pfchar.json"
    fun featsFileName(base: String): String = (SpellParser.slug(base).ifEmpty { "feats" }) + ".pffeats.json"
}
