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
) {
    val caster: CasterClass? get() = CasterClasses.get(id)

    companion object {
        /** Základní záchrana: dobrá 2 + L/2, špatná L/3 (Core Rulebook). */
        fun save(good: Boolean, level: Int): Int = if (good) 2 + level / 2 else level / 3
    }
}

/** Skupina class ve výběru (podle počtu úrovní kouzel). */
enum class ClassGroup { FULL, SIX, FOUR, NONE, NPC }

val ClassInfo.group: ClassGroup
    get() = when (caster?.progression) {
        Progression.PREPARED_9, Progression.SPONTANEOUS_9 -> ClassGroup.FULL
        Progression.SIX -> ClassGroup.SIX
        Progression.FOUR, Progression.FOUR_KNACKS -> ClassGroup.FOUR
        null -> if (npc) ClassGroup.NPC else ClassGroup.NONE
    }

object ClassCatalog {
    val all: List<ClassInfo> = CLASS_TABLE
    private val byId = all.associateBy { it.id }

    fun get(id: String): ClassInfo? = byId[id]
}

/** Souhrnné hodnoty postavy (multiclass = součet za každou classu). */
data class CharacterStats(val level: Int, val bab: Int, val fort: Int, val ref: Int, val will: Int)

fun Character.stats(): CharacterStats {
    var bab = 0; var fort = 0; var ref = 0; var will = 0
    for (e in classes) {
        val info = ClassCatalog.get(e.classId) ?: continue
        bab += info.bab.at(e.level)
        fort += ClassInfo.save(info.fort, e.level)
        ref += ClassInfo.save(info.ref, e.level)
        will += ClassInfo.save(info.will, e.level)
    }
    return CharacterStats(level, bab, fort, ref, will)
}

/** Modifikátor atributu: (hodnota − 10) / 2 zaokrouhleno dolů. */
fun abilityMod(score: Int): Int = (score - 10).floorDiv(2)
