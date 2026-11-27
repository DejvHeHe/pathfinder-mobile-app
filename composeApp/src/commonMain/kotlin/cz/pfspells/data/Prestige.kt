package cz.pfspells.data

/*
 * Prestižní classy z Archives of Nethys (tools/build_prestige_classes.py → PrestigeTable.kt + PrestigeClasses.json).
 * BAB a záchrany se berou z tabulky classy po úrovních (prestižní classy mají jiné vzorce než základní).
 * Sesílání: úroveň prestižní classy s „+1 level of existing arcane spellcasting class“ zvýší sesílací úroveň classy,
 * kterou hráč u prestižní classy vybere (ClassEntry.advances: druh → classId) – Character.casterLevel.
 * Schopnosti prestižní classy jsou jen text (PrestigeClasses.json), požadavky se ověřují jako prerekvizity featů.
 */

/** Prestižní classa: hodnoty po úrovních jako řetězce „1 2 3 …“, sesílání „|arcane|arcane+divine|…“. */
class PrestigeInfo(
    val id: String,
    val name: String,
    val hd: Int,
    val skills: Int,
    val classSkills: Set<String>,
    bab: String,
    fort: String,
    ref: String,
    will: String,
    casting: String,
    /** úrovně s bonusovým featem (sloupec Special) */
    val bonusFeats: List<Int> = emptyList(),
    /** požadavky „Feats: Combat Casting“, „Spells: Able to cast 3rd-level arcane spells.“ */
    val requirements: List<String> = emptyList(),
    val source: String = "",
    val pfs: Boolean = false,
) {
    private fun ints(s: String) = s.split(' ').mapNotNull { it.toIntOrNull() }

    val babs: List<Int> = ints(bab)
    val forts: List<Int> = ints(fort)
    val refs: List<Int> = ints(ref)
    val wills: List<Int> = ints(will)

    /** sesílání po úrovních: [úroveň − 1] → druhy („arcane“, „divine“, „psychic“, „any“, „class:witch“) */
    val castingKinds: List<List<String>> = casting.split('|').map { l -> l.split('+').filter { it.isNotEmpty() } }

    val maxLevel: Int get() = babs.size

    /** druhy sesílání, které classa zvyšuje (Mystic Theurge: arcane i divine) */
    val advanceKinds: List<String> get() = castingKinds.flatten().distinct()

    /** Hodnota z tabulky pro úroveň (1 … [maxLevel]). */
    fun at(values: List<Int>, level: Int): Int = if (level < 1) 0 else values.getOrElse(level.coerceAtMost(maxLevel) - 1) { 0 }

    /** Kolik úrovní sesílání druhu [kind] dává [level] úrovní této classy. */
    fun advancesAt(level: Int, kind: String): Int = castingKinds.take(level).sumOf { ks -> ks.count { it == kind } }

    /** Záznam pro [ClassCatalog]: BAB / záchrany přibližně (přesné hodnoty z tabulky počítá `Character.stats`). */
    val info: ClassInfo by lazy {
        val top = maxLevel.coerceAtLeast(1)
        val b = babs.lastOrNull() ?: 0
        ClassInfo(
            id, name,
            bab = when { b * 4 >= top * 4 -> Bab.HIGH; b * 4 >= top * 3 -> Bab.MED; else -> Bab.LOW },
            fort = (forts.lastOrNull() ?: 0) * 2 >= top, ref = (refs.lastOrNull() ?: 0) * 2 >= top,
            will = (wills.lastOrNull() ?: 0) * 2 >= top,
            hd = hd, skills = skills, classSkills = classSkills, prestige = this,
        )
    }
}

object Prestige {
    /** Classa (sesílající, ne prestižní) odpovídá druhu sesílání prestižní classy. */
    fun matches(e: ClassEntry, kind: String): Boolean {
        val cls = e.caster ?: return false
        if (ClassCatalog.get(e.classId)?.prestige != null) return false
        return when (kind) {
            "arcane" -> cls.tradition == MagicTradition.ARCANE
            "divine" -> cls.tradition == MagicTradition.DIVINE
            "psychic" -> cls.tradition == MagicTradition.PSYCHIC
            // „+1 level of witch class“ – jen konkrétní classa
            else -> if (kind.startsWith("class:")) e.classId == kind.removePrefix("class:") else true
        }
    }

    /** Classy postavy, které může prestižní classa u druhu [kind] zvyšovat. */
    fun candidates(c: Character, kind: String): List<ClassEntry> = c.classes.filter { matches(it, kind) }

    /** Výchozí volba: jediná vhodná classa pro každý druh (jinak nevybráno). */
    fun defaultAdvances(c: Character, info: PrestigeInfo): Map<String, String> =
        info.advanceKinds.mapNotNull { k -> candidates(c, k).singleOrNull()?.let { k to it.classId } }.toMap()

    /** Druhy sesílání prestižní classy, pro které hráč ještě nevybral classu. */
    fun missingAdvances(e: ClassEntry): List<String> {
        val info = ClassCatalog.get(e.classId)?.prestige ?: return emptyList()
        return info.advanceKinds.filter { e.advances[it] == null }
    }
}

/**
 * Sesílací úroveň classy: úroveň classy + úrovně prestižních class, které ji zvyšují („+1 level of existing class“).
 * Platí pro kouzla za den / známá a nejvyšší spell level – schopnosti classy (domény, hexy…) se řídí úrovní classy.
 */
fun Character.casterLevel(e: ClassEntry): Int = e.level + classes.sumOf { p ->
    val info = ClassCatalog.get(p.classId)?.prestige ?: return@sumOf 0
    p.advances.entries.sumOf { (kind, target) -> if (target == e.classId) info.advancesAt(p.level, kind) else 0 }
}

/** Jeden požadavek prestižní classy („Spells: Able to cast 3rd-level arcane spells.“) a stav jeho částí. */
data class PrestigeRequirement(val label: String, val text: String, val parts: List<Prereq>) {
    val status: PrereqStatus get() = FeatPrereqs.overall(parts)
}

object PrestigeRequirements {
    /** „Base Attack Bonus: +5“ → „base attack bonus +5“, „Must be proficient with…“ → „proficient with…“. */
    fun prereqText(label: String, text: String): String {
        val t = text.trim().trimEnd('.').replace(Regex("^(?:must|should) (?:be|have) ", RegexOption.IGNORE_CASE), "")
            // „Able to cast 2nd-level divine spells and 2nd-level arcane spells“ → dvě prerekvizity
            .replace(Regex("""(?i)^(able to cast|ability to cast) (.+?) and (\d+(?:st|nd|rd|th)-level [a-z]+ spells)$"""), "$1 $2, $1 $3")
        return when (label.trim().lowercase()) {
            "base attack bonus", "bab" -> "base attack bonus $t"
            "caster level" -> "caster level $t"
            else -> t
        }
    }

    fun evaluate(c: Character, info: PrestigeInfo, feats: FeatDatabase?, spells: SpellDatabase?): List<PrestigeRequirement> =
        info.requirements.map { r ->
            val label = r.substringBefore(":", "").trim()
            val text = r.substringAfter(":").trim()
            PrestigeRequirement(label, text, FeatPrereqs.evaluate(c, feats, prereqText(label, text), spells))
        }

    fun overall(list: List<PrestigeRequirement>): PrereqStatus = FeatPrereqs.overall(list.flatMap { it.parts })
}

/** Popis a text schopností prestižní classy (`PrestigeClasses.json`, líně). */
@kotlinx.serialization.Serializable
data class PrestigeText(val id: String, val description: String = "", val features: String = "")

object PrestigeTextRepository {
    private val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }

    fun parse(text: String): Map<String, PrestigeText> =
        json.decodeFromString(kotlinx.serialization.builtins.ListSerializer(PrestigeText.serializer()), text).associateBy { it.id }

    @OptIn(org.jetbrains.compose.resources.ExperimentalResourceApi::class)
    suspend fun load(): Map<String, PrestigeText> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
        parse(cz.pfspells.resources.Res.readBytes("files/PrestigeClasses.json").decodeToString())
    }
}
