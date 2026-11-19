package cz.pfspells.data

/**
 * Skill PF1 (Core Rulebook). [id] = klíč jako ve Foundry VTT PF1 („disableDevice“, „knowledge.arcana“),
 * [subtyped] = Craft / Perform / Profession – ranky se dávají do konkrétního druhu („craft:alchemy“).
 */
data class SkillInfo(
    val id: String,
    val name: String,
    val ability: Ability,
    val trainedOnly: Boolean = false,
    val subtyped: Boolean = false,
)

object Skills {
    val KNOWLEDGE = listOf("arcana", "dungeoneering", "engineering", "geography", "history", "local", "nature", "nobility", "planes", "religion")

    val all: List<SkillInfo> = listOf(
        SkillInfo("acrobatics", "Acrobatics", Ability.DEX),
        SkillInfo("appraise", "Appraise", Ability.INT),
        SkillInfo("bluff", "Bluff", Ability.CHA),
        SkillInfo("climb", "Climb", Ability.STR),
        SkillInfo("craft", "Craft", Ability.INT, subtyped = true),
        SkillInfo("diplomacy", "Diplomacy", Ability.CHA),
        SkillInfo("disableDevice", "Disable Device", Ability.DEX, trainedOnly = true),
        SkillInfo("disguise", "Disguise", Ability.CHA),
        SkillInfo("escapeArtist", "Escape Artist", Ability.DEX),
        SkillInfo("fly", "Fly", Ability.DEX),
        SkillInfo("handleAnimal", "Handle Animal", Ability.CHA, trainedOnly = true),
        SkillInfo("heal", "Heal", Ability.WIS),
        SkillInfo("intimidate", "Intimidate", Ability.CHA),
    ) + KNOWLEDGE.map { k ->
        SkillInfo("knowledge.$k", "Knowledge (${k})", Ability.INT, trainedOnly = true)
    } + listOf(
        SkillInfo("linguistics", "Linguistics", Ability.INT, trainedOnly = true),
        SkillInfo("perception", "Perception", Ability.WIS),
        SkillInfo("perform", "Perform", Ability.CHA, subtyped = true),
        SkillInfo("profession", "Profession", Ability.WIS, trainedOnly = true, subtyped = true),
        SkillInfo("ride", "Ride", Ability.DEX),
        SkillInfo("senseMotive", "Sense Motive", Ability.WIS),
        SkillInfo("sleightOfHand", "Sleight of Hand", Ability.DEX, trainedOnly = true),
        SkillInfo("spellcraft", "Spellcraft", Ability.INT, trainedOnly = true),
        SkillInfo("stealth", "Stealth", Ability.DEX),
        SkillInfo("survival", "Survival", Ability.WIS),
        SkillInfo("swim", "Swim", Ability.STR),
        SkillInfo("useMagicDevice", "Use Magic Device", Ability.CHA, trainedOnly = true),
    )

    private val byId = all.associateBy { it.id }

    fun get(id: String): SkillInfo? = byId[id]

    /** Skill podle anglického jména z textu prerekvizit („sense motive“, „knowledge“…). */
    private val byName: Map<String, SkillInfo> = all.associateBy { it.name.lowercase() }

    fun byName(name: String): SkillInfo? = byName[name.lowercase().trim()]

    /** Názvy skillů pro rozpoznání v textu (bez specializací Knowledge). */
    val baseNames: List<String> = all.filterNot { it.id.startsWith("knowledge.") }.map { it.name.lowercase() } + "knowledge"

    /** Klíč ranků: „perception“, „knowledge.arcana“, „craft:alchemy“ (druh malými písmeny). */
    fun key(skillId: String, subtype: String? = null): String =
        if (subtype.isNullOrBlank()) skillId else "$skillId:${subtype.trim().lowercase()}"

    /** Skill id z klíče ranků. */
    fun idOf(key: String): String = key.substringBefore(':')

    /** Druh z klíče („craft:alchemy“ → „alchemy“), null u skillu bez druhu. */
    fun subtypeOf(key: String): String? = key.substringAfter(':', "").ifEmpty { null }
}

/** Je skill (id, i „craft:…“) class skill některé classy postavy nebo rasy? */
fun Character.isClassSkill(key: String): Boolean {
    val id = Skills.idOf(key)
    fun inSet(set: Set<String>) = id in set || (id.startsWith("knowledge.") && "knowledge" in set)
    return classes.any { e -> ClassCatalog.get(e.classId)?.classSkills?.let(::inSet) == true } ||
        raceInfo?.classSkills?.let(::inSet) == true
}

/** Rasový bonus ke skillu („knowledge“ / „craft“ ve zdroji = všechny druhy). */
fun Character.raceSkillMod(key: String): Int {
    val id = Skills.idOf(key)
    val mods = raceInfo?.skillMods ?: return 0
    return (mods[id] ?: 0) + if (id.startsWith("knowledge.")) mods["knowledge"] ?: 0 else 0
}

/** Ranky ve skillu (klíč z [Skills.key]). */
fun Character.ranks(key: String): Int = skills[key] ?: 0

/**
 * Celkový bonus: ranky + modifikátor atributu + 3 za class skill (s alespoň 1 rankem) + rasa.
 * Null = skill „trained only“ bez ranků (nejde použít).
 */
fun Character.skillTotal(key: String): Int? {
    val info = Skills.get(Skills.idOf(key)) ?: return null
    val r = ranks(key)
    if (info.trainedOnly && r == 0) return null
    return r + abilityMod(scores[info.ability]) + (if (r > 0 && isClassSkill(key)) 3 else 0) + raceSkillMod(key)
}

/**
 * Skill body: za každou úroveň classy (skill body classy + mod INT, min. 1), člověk +1 za úroveň (Skilled).
 * Bonus za oblíbenou classu se nepočítá (lze přidat ručně přes [Character.extraSkillPoints]).
 */
fun Character.skillPoints(): Int {
    val intMod = abilityMod(scores.int)
    val perClass = classes.sumOf { e ->
        val base = ClassCatalog.get(e.classId)?.skills ?: 2
        e.level * maxOf(1, base + intMod)
    }
    val skilled = if (raceInfo?.id == "human") level else 0
    return perClass + skilled + extraSkillPoints
}

/** Utracené skill body (součet ranků). */
fun Character.skillPointsUsed(): Int = skills.values.sum()
