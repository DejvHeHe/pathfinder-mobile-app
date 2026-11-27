package cz.pfspells.data

/**
 * Odvozené prerekvizity featů: class features z class a úrovní, přesvědčení, božstvo, velikost a typ tvora,
 * Hit Dice, základní záchrany, proficience, druh sesílaných kouzel a konkrétní kouzla.
 * Každá funkce vrací null, když text nerozpozná (pak zkouší další pravidla), jinak MET / UNMET / UNKNOWN.
 */
internal object PrereqRules {

    // ---------------- class features ----------------

    /** Classa (id) dostává schopnost od úrovně [level]; [sure] = false → jen volitelně (druid si může vybrat doménu…). */
    private data class Grant(val classId: String, val level: Int, val sure: Boolean = true)

    private fun g(vararg pairs: Pair<String, Int>) = pairs.map { Grant(it.first, it.second) }
    private fun maybe(classId: String, level: Int) = Grant(classId, level, sure = false)

    private val FEATURES: Map<String, List<Grant>> = mapOf(
        "channel energy" to g("cleric" to 1, "paladin" to 4, "antipaladin" to 4, "warpriest" to 4),
        "channel positive energy" to g("paladin" to 4) + maybe("cleric", 1) + maybe("warpriest", 4),
        "channel negative energy" to g("antipaladin" to 4) + maybe("cleric", 1) + maybe("warpriest", 4),
        "rage" to g("barbarian" to 1, "barbarian_unchained" to 1, "bloodrager" to 1),
        "bloodrage" to g("bloodrager" to 1),
        "raging song" to g("skald" to 1),
        "bardic performance" to g("bard" to 1),
        "inspire courage" to g("bard" to 1),
        "bardic knowledge" to g("bard" to 1),
        "versatile performance" to g("bard" to 2),
        "dirge of doom" to g("bard" to 8),
        "perform dirge of doom" to g("bard" to 8),
        "wild shape" to g("druid" to 4, "shifter" to 4),
        "wild empathy" to g("druid" to 1, "ranger" to 1),
        "nature bond" to g("druid" to 1),
        "animal companion" to g("hunter" to 1) + maybe("druid", 1) + maybe("ranger", 4),
        "acquire animal companion" to g("hunter" to 1) + maybe("druid", 1) + maybe("ranger", 4),
        "acquire new familiar" to g("witch" to 1) + maybe("wizard", 1) + maybe("sorcerer", 1),
        "special mount" to listOf(maybe("paladin", 5)),
        "sorcerer bloodline" to g("sorcerer" to 1),
        "specialized arcane school" to listOf(maybe("wizard", 1)),
        "second judgment" to g("inquisitor" to 8),
        "monster lore" to g("inquisitor" to 1),
        "stern gaze" to g("inquisitor" to 1),
        "still mind" to g("monk" to 3, "monk_unchained" to 3),
        "blood casting" to g("bloodrager" to 2),
        "gather power" to g("kineticist" to 1),
        "inspire competence" to g("bard" to 3),
        "inspire competence bardic performance" to g("bard" to 3),
        "brawler's cunning" to g("brawler" to 1),
        "prepare spells" to g(
            "wizard" to 1, "cleric" to 1, "druid" to 1, "witch" to 1, "magus" to 1, "alchemist" to 1, "paladin" to 4,
            "ranger" to 4, "antipaladin" to 4, "shaman" to 1, "investigator" to 1, "warpriest" to 1, "arcanist" to 1,
            "hunter" to 1, "occultist" to 1, "medium" to 4, "adept" to 1,
        ),
        "animal focus" to g("hunter" to 1),
        "ki pool" to g("monk" to 4, "monk_unchained" to 3, "ninja" to 2),
        "ki" to g("monk" to 4, "monk_unchained" to 3, "ninja" to 2),
        "flurry of blows" to g("monk" to 1, "monk_unchained" to 1),
        "brawler's flurry" to g("brawler" to 1),
        "martial flexibility" to g("brawler" to 1),
        "abundant step" to g("monk" to 12, "monk_unchained" to 12),
        "armor training" to g("fighter" to 3),
        "weapon training" to g("fighter" to 5),
        "bravery" to g("fighter" to 2),
        "bane" to g("inquisitor" to 5),
        "judgment" to g("inquisitor" to 1),
        "hex" to g("witch" to 1, "shaman" to 2),
        "patron" to g("witch" to 1),
        "lay on hands" to g("paladin" to 2),
        "mercy" to g("paladin" to 3),
        "smite evil" to g("paladin" to 1),
        "aura of courage" to g("paladin" to 3),
        "divine bond" to g("paladin" to 5),
        "smite good" to g("antipaladin" to 1),
        "touch of corruption" to g("antipaladin" to 2),
        "sneak attack" to g("rogue" to 1, "rogue_unchained" to 1, "ninja" to 1, "slayer" to 3),
        "trapfinding" to g("rogue" to 1, "rogue_unchained" to 1, "investigator" to 1),
        "evasion" to g("rogue" to 2, "rogue_unchained" to 2, "monk" to 2, "monk_unchained" to 2, "ranger" to 9),
        "uncanny dodge" to g("barbarian" to 2, "barbarian_unchained" to 2, "rogue" to 4, "rogue_unchained" to 4),
        "rogue talent" to g("rogue" to 2, "rogue_unchained" to 2),
        "ninja trick" to g("ninja" to 2),
        "slayer talent" to g("slayer" to 2),
        "studied target" to g("slayer" to 1),
        "favored enemy" to g("ranger" to 1),
        "favored terrain" to g("ranger" to 3),
        "hunter's bond" to g("ranger" to 4),
        "track" to g("ranger" to 1, "slayer" to 1),
        "grit" to g("gunslinger" to 1),
        "deeds" to g("gunslinger" to 1, "swashbuckler" to 1),
        "panache" to g("swashbuckler" to 1),
        "arcane pool" to g("magus" to 1),
        "spell combat" to g("magus" to 1),
        "spellstrike" to g("magus" to 2),
        "mystery" to g("oracle" to 1),
        "revelation" to g("oracle" to 1),
        "oracle's curse" to g("oracle" to 1),
        "bloodline" to g("sorcerer" to 1, "bloodrager" to 1),
        "domain" to g("cleric" to 1, "inquisitor" to 1) + maybe("druid", 1),
        "blessings" to g("warpriest" to 1),
        "fervor" to g("warpriest" to 2),
        "sacred weapon" to g("warpriest" to 1),
        "eidolon" to g("summoner" to 1, "summoner_unchained" to 1),
        "phantom" to g("spiritualist" to 1),
        "spirit" to g("shaman" to 1, "medium" to 1),
        "painful stare" to g("mesmerist" to 1),
        "hypnotic stare" to g("mesmerist" to 1),
        "mesmerist trick" to g("mesmerist" to 1),
        "kinetic blast" to g("kineticist" to 1),
        "burn" to g("kineticist" to 1),
        "poison use" to g("alchemist" to 2, "ninja" to 1),
        "inspiration" to g("investigator" to 1),
        "studied combat" to g("investigator" to 4),
        "challenge" to g("cavalier" to 1, "samurai" to 1),
        "order" to g("cavalier" to 1, "samurai" to 1),
        "tactician" to g("cavalier" to 1),
        "mount" to g("cavalier" to 1, "samurai" to 1) + maybe("paladin", 5),
        "mental focus" to g("occultist" to 1),
        "implement" to g("occultist" to 1),
        "focus powers" to g("occultist" to 1),
        "bomb" to g("alchemist" to 1),
        "bombs" to g("alchemist" to 1),
        "mutagen" to g("alchemist" to 1),
        "discovery" to g("alchemist" to 2),
        "arcane bond" to g("wizard" to 1),
        "familiar" to g("witch" to 1) + maybe("wizard", 1) + maybe("sorcerer", 1),
        "arcane school" to g("wizard" to 1),
        "arcane reservoir" to g("arcanist" to 1),
        "arcanist exploit" to g("arcanist" to 1),
        "spontaneous casting" to g("cleric" to 1, "druid" to 1),
    )

    /** Normalizace: „the channel energy class feature“ → „channel energy“. */
    private fun normalizeFeature(t: String): String = t
        .replace('’', '\'').replace('�', '\'')
        .replace(Regex("""\s*\([^)]*\)"""), "")
        // zkratka knihy přilepená ke slovu („brawler's flurryacg“, „painful stareoa“)
        .replace(Regex("""(?<=[a-z]{3})(acg|apg|arg|uc|ui|oa|isg|iswg|ue|uw)\b"""), "")
        .replace(Regex("""\s+with\s+.*$"""), "")
        .replace(Regex("""\b(the|a|an|class features?|class ability|special ability|ability to use|ability to|access to|feature)\b"""), " ")
        .replace(Regex("""\s+"""), " ").trim()

    private val DICE = Regex("""^(channel energy|channel positive energy|channel negative energy|sneak attack)\s*\+?(\d+)d6$""")

    fun classFeature(c: Character, t: String): PrereqStatus? {
        val n = normalizeFeature(t)
        DICE.find(n)?.let { m ->
            val dice = m.groupValues[2].toInt()
            val have = c.classes.maxOfOrNull { e ->
                when (m.groupValues[1]) {
                    "channel energy", "channel positive energy", "channel negative energy" -> when (e.classId) {
                        "cleric" -> (e.level + 1) / 2
                        "paladin", "antipaladin" -> if (e.level >= 4) e.level / 2 else 0
                        else -> 0
                    }
                    else -> when (e.classId) {
                        "rogue", "rogue_unchained", "ninja" -> (e.level + 1) / 2
                        "slayer" -> e.level / 3
                        else -> 0
                    }
                }
            } ?: 0
            return if (have >= dice) PrereqStatus.MET else PrereqStatus.UNMET
        }
        val grants = FEATURES[n] ?: FEATURES[n.removeSuffix("s")] ?: return null
        val levels = c.classes.associate { it.classId to it.level }
        val hit = grants.filter { (levels[it.classId] ?: 0) >= it.level }
        return when {
            hit.any { it.sure } -> PrereqStatus.MET
            hit.isNotEmpty() -> PrereqStatus.UNKNOWN
            else -> PrereqStatus.UNMET
        }
    }

    // ---------------- přesvědčení a božstvo ----------------

    private val ALIGNMENT = Regex("""^(?:any\s+)?((?:(?:non-?)?(?:lawful|chaotic|good|evil|neutral)\s*)+)\s*alignment$""")

    /** Přesvědčení bez slova „alignment“: „lawful neutral“, „chaotic evil“ (jen celé dvojice). */
    private val ALIGNMENT_PAIR = Regex("""^((?:lawful|neutral|chaotic)\s+(?:good|neutral|evil))$""")

    fun alignment(c: Character, t: String): PrereqStatus? {
        val m = ALIGNMENT.find(t) ?: ALIGNMENT_PAIR.find(t) ?: return null
        val a = c.alignment ?: return PrereqStatus.UNKNOWN
        val words = m.groupValues[1].trim().split(Regex("""\s+""")).map { it.replace("-", "") }
        val ok = words.all { w ->
            when (w) {
                "lawful" -> a.law == 1
                "chaotic" -> a.law == -1
                "good" -> a.good == 1
                "evil" -> a.good == -1
                "nonlawful" -> a.law != 1
                "nonchaotic" -> a.law != -1
                "nongood" -> a.good != 1
                "nonevil" -> a.good != -1
                // „neutral“ samo = neutrální aspoň na jedné ose; „chaotic neutral“ = neutrální na ose dobro–zlo
                "neutral" -> if (words.size == 1) a.law == 0 || a.good == 0 else
                    (if (words.any { it == "lawful" || it == "chaotic" }) a.good == 0 else a.law == 0)
                else -> true
            }
        }
        return if (ok) PrereqStatus.MET else PrereqStatus.UNMET
    }

    private val WORSHIPER = Regex("""^(?:must\s+be\s+a\s+)?worshipp?er\s+of\s+(.+)$""")
    private val DEITY_KIND = Regex("""^(?:an?\s+)?((?:non)?(?:lawful|chaotic|good|evil|neutral)(?:\s+(?:good|evil|neutral))?)\s+(?:deity|god|goddess)$""")

    /**
     * Božstvo: „worshiper of Iomedae“, „must worship a deity“, „worshiper of a good deity“ (přesvědčení božstva ze seznamu),
     * „alignment must match that of your worshiped deity“, „alignment must be within one step of your deity's“.
     */
    fun deity(c: Character, t: String): PrereqStatus? {
        val deity = c.deity.trim().lowercase()
        val info = c.deityInfo
        if ("alignment" in t && "deity" in t && ("match" in t || "one step" in t)) {
            val a = c.alignment ?: return PrereqStatus.UNKNOWN
            val d = info?.alignment ?: return PrereqStatus.UNKNOWN
            val dist = kotlin.math.abs(a.law - d.law) + kotlin.math.abs(a.good - d.good)
            return met(if ("match" in t) dist == 0 else dist <= 1)
        }
        if (t.startsWith("must worship") && "not" !in t && "receive" !in t) {
            return if (deity.isEmpty()) PrereqStatus.UNKNOWN else PrereqStatus.MET
        }
        val m = WORSHIPER.find(t) ?: return null
        val who = m.groupValues[1].trim()
        if (deity.isEmpty()) return PrereqStatus.UNKNOWN
        DEITY_KIND.find(who)?.let { k ->
            val d = info?.alignment ?: return PrereqStatus.UNKNOWN
            val words = k.groupValues[1].split(' ')
            return met(words.all { w ->
                when (w) {
                    "lawful" -> d.law == 1
                    "chaotic" -> d.law == -1
                    "good" -> d.good == 1
                    "evil" -> d.good == -1
                    "nongood" -> d.good != 1
                    "nonevil" -> d.good != -1
                    "nonlawful" -> d.law != 1
                    "nonchaotic" -> d.law != -1
                    "neutral" -> if (words.size == 1) d.law == 0 || d.good == 0 else d.good == 0
                    else -> true
                }
            })
        }
        if (who.startsWith("a ") || who.startsWith("an ") || "deity" in who) return PrereqStatus.UNKNOWN
        // jméno ze seznamu se porovná přesně, vlastní text volněji
        val name = info?.name?.lowercase() ?: deity
        return met(if (info != null) who == name || who.substringBefore(" (") == name else who in deity || deity in who)
    }

    // ---------------- velikost, typ a podtyp tvora ----------------

    private val SIZES = CreatureSize.entries.map { it.name.lowercase() }
    private val SIZE_RE = Regex("""^(?:size\s+)?(fine|diminutive|tiny|small|medium|large|huge|gargantuan|colossal)(?:\s+size)?(?:\s+or\s+(smaller|larger))?(?:\s+size)?$""")

    fun size(c: Character, t: String): PrereqStatus? {
        val m = SIZE_RE.find(t.replace(Regex("""\s+"""), " ")) ?: return null
        val size = c.creatureSize ?: return PrereqStatus.UNKNOWN
        val want = SIZES.indexOf(m.groupValues[1])
        val have = size.ordinal
        val ok = when (m.groupValues[2]) {
            "smaller" -> have <= want
            "larger" -> have >= want
            else -> have == want
        }
        return if (ok) PrereqStatus.MET else PrereqStatus.UNMET
    }

    private val TYPES = setOf(
        "aberration", "animal", "construct", "dragon", "fey", "humanoid", "magical beast", "monstrous humanoid",
        "ooze", "outsider", "plant", "undead", "vermin",
    )

    /** „monstrousHumanoid“ (Foundry) → „monstrous humanoid“. */
    private fun typeName(raw: String) = raw.replace(Regex("([a-z])([A-Z])"), "$1 $2").lowercase()

    fun creatureType(c: Character, t: String): PrereqStatus? {
        val sub = t.removeSuffix(" subtype").trim()
        val isSubtype = t.endsWith(" subtype")
        if (!isSubtype && t !in TYPES) return null
        val race = c.raceInfo ?: return PrereqStatus.UNKNOWN
        val ok = if (isSubtype) race.countsAs(sub) || typeName(race.type) == sub else typeName(race.type) == t
        return if (ok) PrereqStatus.MET else PrereqStatus.UNMET
    }

    // ---------------- Hit Dice, záchrany ----------------

    private val HIT_DICE = Regex("""^(\d+)\s+(?:or more\s+)?hit dice$""")
    private val BASE_SAVE = Regex("""^base\s+(fortitude|reflex|will)\s+(?:save|saving throw)(?:\s+bonus)?\s+(?:of\s+)?\+(\d+)$""")

    fun hitDiceOrSave(c: Character, t: String): PrereqStatus? {
        HIT_DICE.find(t)?.let { return met(c.level >= it.groupValues[1].toInt()) }
        BASE_SAVE.find(t)?.let { m ->
            val st = c.stats()
            val v = when (m.groupValues[1]) { "fortitude" -> st.fort; "reflex" -> st.ref; else -> st.will }
            return met(v >= m.groupValues[2].toInt())
        }
        return null
    }

    // ---------------- proficience ----------------

    private val ARMOR_WORDS = mapOf(
        "light armor" to "lgt", "medium armor" to "med", "heavy armor" to "hvy",
        "bucklers" to "shl", "buckler" to "shl", "light shields" to "shl", "heavy shields" to "shl", "light shield" to "shl",
        "heavy shield" to "shl", "shields" to "shl", "shield" to "shl", "tower shields" to "twr", "tower shield" to "twr",
    )

    /** Proficience postavy: z class + featy (Armor Proficiency, Martial Weapon Proficiency…). */
    private fun armorProfs(c: Character): Set<String> {
        val set = c.classes.flatMap { ClassCatalog.get(it.classId)?.armorProf.orEmpty() }.toMutableSet()
        val feats = c.feats.map { it.name.lowercase() }
        if (feats.any { it.startsWith("armor proficiency, light") || it == "light armor proficiency" }) set += "lgt"
        if (feats.any { it.startsWith("armor proficiency, medium") || it == "medium armor proficiency" }) set += "med"
        if (feats.any { it.startsWith("armor proficiency, heavy") || it == "heavy armor proficiency" }) set += "hvy"
        if (feats.any { it == "shield proficiency" }) set += "shl"
        if (feats.any { it == "tower shield proficiency" }) set += "twr"
        return set
    }

    private fun weaponProfs(c: Character): Set<String> {
        val set = c.classes.flatMap { ClassCatalog.get(it.classId)?.weaponProf.orEmpty() }.toMutableSet()
        val feats = c.feats.map { it.name.lowercase() }
        if ("martial weapon proficiency" in feats) set += "martial"
        if ("simple weapon proficiency" in feats) set += "simple"
        return set
    }

    fun proficiency(c: Character, t: String): PrereqStatus? {
        val armorPhrase = Regex("""^(?:proficiency with|proficient with|proficient in)\s+(.+)$""").find(t)?.groupValues?.get(1)
            ?: Regex("""^(light armor|medium armor|heavy armor|shield|tower shield) proficiency$""").find(t)?.groupValues?.get(1)
            ?: return null
        val phrase = armorPhrase.removePrefix("all ").removePrefix("a ").trim()
        if ("martial weapon" in phrase) return met("martial" in weaponProfs(c))
        if ("simple weapon" in phrase) return met("simple" in weaponProfs(c) || "martial" in weaponProfs(c))
        // „medium or heavy armor“, „bucklers and light shields“, „light or heavy shields“
        val anyOf = " or " in phrase
        val parts = phrase.split(" or ", " and ").map { it.trim() }
        val noun = parts.last().substringAfterLast(' ') // armor / shields
        val keys = parts.map { p -> ARMOR_WORDS[p] ?: ARMOR_WORDS["$p $noun"] ?: return PrereqStatus.UNKNOWN }
        val have = armorProfs(c)
        return met(if (anyOf) keys.any { it in have } else keys.all { it in have })
    }

    // ---------------- druh kouzel, konkrétní kouzla ----------------

    /** Classy postavy, které sesílají kouzla daného druhu / jména classy (a mají na úrovni aspoň 1 kouzlo). */
    fun castersOf(c: Character, kind: String?): List<ClassEntry> = c.classes.filter { e ->
        val cls = e.caster ?: return@filter false
        val any = cls.maxSpellLevel(e.level) >= (if (cls.hasCantrips) 0 else 1) && (cls.maxSpellLevel(e.level) > 0 || cls.hasCantrips)
        any && when (kind) {
            null -> true
            // druh magie z classy (archetyp ho může změnit: Filidh → divine, Mindblade → psychic)
            "arcane" -> cls.tradition == MagicTradition.ARCANE
            "divine" -> cls.tradition == MagicTradition.DIVINE
            "psychic" -> cls.tradition == MagicTradition.PSYCHIC
            else -> e.classId == kind || e.classId == kind.replace(' ', '_') || cls.name.lowercase() == kind
        }
    }

    private val SPELL_KIND = Regex("""^(?:ability to cast|able to cast|can cast)\s+(arcane|divine|psychic)\s+spells$|^(arcane|divine|psychic)\s+spellcaster$""")
    private val CAST_N_LEVEL = Regex("""^(?:ability to cast|able to cast)\s+(\d+)(?:st|nd|rd|th)[- ]level\s+(?:([a-z ]+?)\s+)?spells$""")

    fun spellKind(c: Character, t: String): PrereqStatus? {
        if (t == "ability to cast spells" || t == "spellcaster" || t == "able to cast spells") return met(castersOf(c, null).isNotEmpty())
        SPELL_KIND.find(t)?.let { m ->
            val kind = m.groupValues[1].ifEmpty { m.groupValues[2] }
            return met(castersOf(c, kind).isNotEmpty())
        }
        CAST_N_LEVEL.find(t)?.let { m ->
            val n = m.groupValues[1].toInt()
            val kind = m.groupValues[2].trim().ifEmpty { null }
            return met(castersOf(c, kind).any { e -> e.caster!!.maxSpellLevel(e.level) >= n })
        }
        // „ability to cast druid or ranger spells“ – druh = jméno classy
        Regex("""^(?:ability to cast|able to cast)\s+([a-z]+)\s+spells$""").find(t)?.let { m ->
            val k = m.groupValues[1]
            if (CasterClasses.get(k) != null) return met(castersOf(c, k).isNotEmpty())
        }
        return null
    }

    /** „ability to cast dispel magic“, „… summon monster“, „… detect magic as a spell or spell-like ability“. */
    fun namedSpell(c: Character, t: String, spells: SpellDatabase?): PrereqStatus? {
        if (spells == null) return null
        val m = Regex("""^(?:ability to cast|able to cast|can cast|cast)\s+(.+?)(?:\s+as a (?:known )?spell.*)?$""").find(t) ?: return null
        val name = m.groupValues[1].trim().removePrefix("the ").removePrefix("a ").removeSuffix(" spell").removeSuffix(" spells")
        val matching = spells.findByName(name)?.let { listOf(it) }
            ?: spells.spells.filter { it.searchName.startsWith("$name ") }.takeIf { it.isNotEmpty() }
            ?: return null
        val can = c.classes.any { e -> e.caster?.let { cls -> matching.any { cls.canCast(it, e.level) } } == true }
        // rasovou SLA nebo předmět jde potvrdit ručně
        return if (can) PrereqStatus.MET else PrereqStatus.UNMET
    }

    // ---------------- domény ----------------

    /** „Fire domain“, „Animal or Plant domain“: domény klerika / druida (i přes subdoménu); inkvizitor domény nevede. */
    fun domain(c: Character, t: String): PrereqStatus? {
        val name = Regex("""^([a-z]+)\s+domain$""").find(t)?.groupValues?.get(1) ?: return null
        val chosen = c.classes.flatMap { e ->
            listOfNotNull(e.subclass[SubclassSlot.DOMAIN1], e.subclass[SubclassSlot.DOMAIN2]) +
                listOfNotNull(e.subclass[SubclassSlot.SUBDOMAIN1], e.subclass[SubclassSlot.SUBDOMAIN2]).flatMap { subdomainParents(it) }
        }
        if (name in chosen) return PrereqStatus.MET
        val couldHave = c.classes.any { it.classId == "inquisitor" } ||
            c.classes.any { (it.classId == "cleric" || it.classId == "druid") && it.subclass[SubclassSlot.DOMAIN1] == null }
        return if (couldHave) PrereqStatus.UNKNOWN else PrereqStatus.UNMET
    }

    // ---------------- rasové traity ----------------

    /** Výchozí rasové traity (Core Rulebook / ARG) → rasy, které je mají. Alternativní traity je můžou nahradit. */
    private val RACIAL_TRAITS: Map<String, Set<String>> = mapOf(
        "weapon familiarity" to setOf(
            "dwarf", "elf", "gnome", "halfling", "half-orc", "orc", "drow", "duergar", "hobgoblin", "goblin", "kobold",
            "ratfolk", "tengu", "catfolk", "kitsune", "svirfneblin", "aquatic-elf", "vanara",
        ),
        "defensive training" to setOf("dwarf", "gnome", "duergar", "svirfneblin"),
        "hatred" to setOf("dwarf", "gnome", "duergar"),
        "stonecunning" to setOf("dwarf", "duergar", "svirfneblin"),
        "stability" to setOf("dwarf", "duergar"),
        "hardy" to setOf("dwarf", "duergar"),
        "greed" to setOf("dwarf"),
        "keen senses" to setOf("elf", "gnome", "half-elf", "halfling", "drow", "aquatic-elf"),
        "gnome magic" to setOf("gnome"),
        "illusion resistance" to setOf("gnome"),
        "obsessive" to setOf("gnome"),
        "elven magic" to setOf("elf", "aquatic-elf"),
        "elven immunities" to setOf("elf", "half-elf", "drow", "aquatic-elf"),
        "adaptability" to setOf("half-elf"),
        "elf blood" to setOf("half-elf"),
        "orc blood" to setOf("half-orc"),
        "multitalented" to setOf("half-elf"),
        "intimidating" to setOf("half-orc"),
        "ferocity" to setOf("half-orc", "orc"),
        "orc ferocity" to setOf("half-orc"),
        "halfling luck" to setOf("halfling"),
        "fearless" to setOf("halfling"),
        "sure-footed" to setOf("halfling"),
        "skilled" to setOf("human"),
        "bonus feat" to setOf("human"),
        "drow spell-like abilities" to setOf("drow", "drow-noble"),
        "drow immunities" to setOf("drow", "drow-noble"),
        "light blindness" to setOf("drow", "drow-noble", "orc", "duergar"),
        "light sensitivity" to setOf("orc", "kobold", "svirfneblin"),
        "swarming" to setOf("ratfolk"),
        "change shape" to setOf("kitsune", "skinwalker"),
        "toxic" to setOf("grippli"),
        "spell resistance" to setOf("drow", "drow-noble", "svirfneblin", "duergar"),
    )

    /** „Stonecunning racial trait“, „defensive training and hatred racial traits“, „darkvision or low-light vision racial trait“. */
    fun racialTrait(c: Character, t: String): PrereqStatus? {
        val picked = c.racialTraits.map { it.name.lowercase() }.toSet()
        val defaults = RacialTraits.defaults(c.race)?.map { it.lowercase() }?.toSet().orEmpty()
        val m = Regex("""^(.+?)\s+(?:alternate\s+)?racial\s+(?:traits?|abilit(?:y|ies))$""").find(t)
            ?: Regex("""^(.+?)\s+traits?$""").find(t)?.takeIf { val n = it.groupValues[1].trim(); RACIAL_TRAITS.containsKey(n) || n in picked }
            ?: return null
        val body = m.groupValues[1].replace(Regex("""(?<=[a-z]{3})(arg|apg|acg|uc)\b"""), "").trim()
        val race = c.raceInfo ?: return PrereqStatus.UNKNOWN
        val replaced = RacialTraits.replaced(c)
        fun has(trait: String): Boolean? = when (val tr = trait.trim()) {
            in picked -> true
            // výchozí trait nahrazený vybraným alternativním traitem
            in replaced -> false
            "darkvision" -> race.darkvision > 0
            "low-light vision" -> race.lowLightVision
            in defaults -> true
            else -> RACIAL_TRAITS[tr]?.contains(race.id)
        }
        val anyOf = " or " in body
        val parts = body.split(" or ", " and ").map { it.trim() }
        val known = parts.map { has(it) }
        return when {
            anyOf && known.any { it == true } -> PrereqStatus.MET
            !anyOf && known.all { it == true } -> PrereqStatus.MET
            // nahrazený trait postava jistě nemá
            anyOf && parts.all { it in replaced } -> PrereqStatus.UNMET
            !anyOf && parts.any { it in replaced } -> PrereqStatus.UNMET
            // výchozí trait rasa nemá – mohla ho získat alternativním traitem, appka to neví
            else -> PrereqStatus.UNKNOWN
        }
    }

    private fun met(b: Boolean) = if (b) PrereqStatus.MET else PrereqStatus.UNMET
}
