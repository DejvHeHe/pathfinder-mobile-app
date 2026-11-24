package cz.pfspells.data

/*
 * Metamagické featy: o kolik úrovní zvyšují slot kouzla. Čte se z textu featu (Feats.json z AoN):
 * „uses up a spell slot two levels higher“ → 2, „does not use up a higher-level spell slot“ / „same spell level“ → 0,
 * jinak (Heighten Spell – libovolná vyšší úroveň, nebo text bez údaje) null.
 */

val Feat.isMetamagic: Boolean get() = types.any { it.equals("Metamagic", ignoreCase = true) }

object Metamagic {
    private val NUMBERS = mapOf("one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5)
    private val HIGHER = Regex("""\b(one|two|three|four|five|\d+)\s+(?:spell\s+)?levels?\s+higher""", RegexOption.IGNORE_CASE)
    private val SAME = listOf("does not use up a higher-level", "same spell level", "equal to the spell's normal spell level", "equal to the spell’s normal spell level")

    /** Zvýšení úrovně slotu; 0 = bez zvýšení; null = proměnné nebo neuvedené. */
    fun levels(feat: Feat): Int? {
        if (!feat.isMetamagic) return null
        val text = feat.sections.joinToString(" ") { it.getOrNull(1).orEmpty() }
        HIGHER.find(text)?.let { m ->
            val w = m.groupValues[1].lowercase()
            return NUMBERS[w] ?: w.toIntOrNull()
        }
        return if (SAME.any { text.contains(it, ignoreCase = true) }) 0 else null
    }

    /** Metamagické featy postavy (bez opakování) se zvýšením slotu; vlastní featy se nepočítají. */
    fun ofCharacter(c: Character, db: FeatDatabase?): List<Pair<Feat, Int?>> =
        c.feats.mapNotNull { p -> p.featId?.let { db?.get(it) } }.filter { it.isMetamagic }.distinctBy { it.id }
            .map { it to levels(it) }
}
