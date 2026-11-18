package cz.pfspells.data

import kotlinx.serialization.Serializable

/**
 * Jeden spell. Texty jsou v originále (anglicky).
 * [levels]: klíč = název spell listu ze zdroje (např. "sorcerer/wizard", "cleric/oracle"), hodnota = spell level.
 */
data class Spell(
    val id: String,
    val name: String,
    val school: String,
    val subschool: String?,
    val descriptor: String?,
    val levels: Map<String, Int>,
    val castingTime: String?,
    val components: String?,
    val range: String?,
    val effect: String?,
    val targets: String?,
    val duration: String?,
    val savingThrow: String?,
    val spellResistance: String?,
    val shortDescription: String?,
    val description: String,
    val mythic: String?,
    val source: String?,
    val url: String?,
    /** povolené v Pathfinder Society (pole „PFS legal“ ve zdroji; chybí-li, bere se jako povolené) */
    val pfsLegal: Boolean = true,
    /** Podtřídy, které spell dávají: „domain:fire“ → spell level (viz [SubclassKind]). */
    val subLevels: Map<String, Int> = emptyMap(),
) {
    val levelText: String = levels.entries.joinToString(", ") { "${it.key} ${it.value}" }

    /** Odstavce popisu (zdroj je odděluje `\n`). */
    val paragraphs: List<String> by lazy {
        description.split('\n').map { it.trim() }.filter { it.isNotEmpty() }
    }

    /** Malými písmeny pro rychlé hledání. */
    val searchName: String = name.lowercase()
}

@Serializable
data class Deck(
    val id: String,
    val name: String,
    val classId: String,
    val classLevel: Int,
    /** spellId -> počet kopií (připravených) */
    val spells: Map<String, Int> = emptyMap(),
)
