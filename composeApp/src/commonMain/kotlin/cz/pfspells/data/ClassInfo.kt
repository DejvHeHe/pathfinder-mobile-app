package cz.pfspells.data

/** Progrese základního útočného bonusu. */
enum class Bab {
    HIGH, MED, LOW;

    fun at(level: Int): Int = when (this) {
        HIGH -> level
        MED -> level * 3 / 4
        LOW -> level / 2
    }
}

/**
 * Základní údaje classy (data z Foundry VTT PF1 – `ClassTable.kt`, generuje `tools/build_classes.py`).
 * Sesílání řeší [CasterClasses] (stejná id).
 */
data class ClassInfo(
    val id: String,
    val name: String,
    val bab: Bab,
    val fort: Boolean,
    val ref: Boolean,
    val will: Boolean,
    val hd: Int,
    val skills: Int,
    val npc: Boolean = false,
    /** class skills (id z [Skills]; „knowledge“ = všechny Knowledge, „knowledge.local“ = jedna) */
    val classSkills: Set<String> = emptySet(),
    /** proficience se zbrojí: lgt, med, hvy, shl (štíty vč. bucklerů), twr (pavéza) */
    val armorProf: Set<String> = emptySet(),
    /** proficience se zbraněmi: simple, martial nebo jednotlivé zbraně („club“, „favored weapon“…) */
    val weaponProf: Set<String> = emptySet(),
    /** rasové HD tvora (zvíře, magická bestie) – pro mazlíčky a společníky */
    val creature: Boolean = false,
    /** prestižní classa (AoN): tabulka po úrovních, sesílání, požadavky */
    val prestige: PrestigeInfo? = null,
) {
    /** nejvyšší úroveň classy (prestižní obvykle 10, některé 5 nebo 3) */
    val maxLevel: Int get() = prestige?.maxLevel ?: 20

    val caster: CasterClass? get() = CasterClasses.get(id)

    companion object {
        /** Základní záchrana: dobrá 2 + L/2, špatná L/3 (Core Rulebook). */
        fun save(good: Boolean, level: Int): Int = if (good) 2 + level / 2 else level / 3
    }
}

/** Skupina class ve výběru (podle počtu úrovní kouzel). */
enum class ClassGroup { FULL, SIX, FOUR, NONE, PRESTIGE, NPC, CREATURE }

val ClassInfo.group: ClassGroup
    get() = if (prestige != null) ClassGroup.PRESTIGE else when (caster?.progression) {
        Progression.PREPARED_9, Progression.SPONTANEOUS_9 -> ClassGroup.FULL
        Progression.SIX -> ClassGroup.SIX
        Progression.FOUR, Progression.FOUR_KNACKS -> ClassGroup.FOUR
        null -> when {
            creature -> ClassGroup.CREATURE
            npc -> ClassGroup.NPC
            else -> ClassGroup.NONE
        }
    }

object ClassCatalog {
    /**
     * Rasové HD tvorů (Bestiary – Creature Types) pro mazlíčky: animal companion, kůň, familiár…
     * Feat za každé liché HD a skill body 2 + Int za HD platí stejně jako u class.
     */
    private val CREATURE_SKILLS = setOf("acrobatics", "climb", "fly", "perception", "stealth", "swim")
    val creatures: List<ClassInfo> = listOf(
        ClassInfo("animal", "Animal", Bab.MED, fort = true, ref = true, will = false, hd = 8, skills = 2, classSkills = CREATURE_SKILLS, creature = true),
        ClassInfo("magical_beast", "Magical Beast", Bab.HIGH, fort = true, ref = true, will = false, hd = 10, skills = 2, classSkills = CREATURE_SKILLS, creature = true),
    )

    val prestige: List<ClassInfo> = PRESTIGE_TABLE.map { it.info }

    val all: List<ClassInfo> = CLASS_TABLE + prestige + creatures
    private val byId = all.associateBy { it.id }

    fun get(id: String): ClassInfo? = byId[id]
}

/** Souhrnné hodnoty postavy (multiclass = součet za každou classu). */
data class CharacterStats(val level: Int, val bab: Int, val fort: Int, val ref: Int, val will: Int)

fun Character.stats(): CharacterStats {
    var bab = 0; var fort = 0; var ref = 0; var will = 0
    for (e in classes) {
        val info = ClassCatalog.get(e.classId) ?: continue
        val p = info.prestige
        if (p != null) {
            // prestižní classa: hodnoty přímo z tabulky (jiné vzorce záchran než u základních class)
            bab += p.at(p.babs, e.level)
            fort += p.at(p.forts, e.level)
            ref += p.at(p.refs, e.level)
            will += p.at(p.wills, e.level)
            continue
        }
        bab += info.bab.at(e.level)
        fort += ClassInfo.save(info.fort, e.level)
        ref += ClassInfo.save(info.ref, e.level)
        will += ClassInfo.save(info.will, e.level)
    }
    return CharacterStats(level, bab, fort, ref, will)
}

/** Modifikátor atributu: (hodnota − 10) / 2 zaokrouhleno dolů. */
fun abilityMod(score: Int): Int = (score - 10).floorDiv(2)
