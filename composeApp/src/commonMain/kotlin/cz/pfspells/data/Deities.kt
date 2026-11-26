package cz.pfspells.data

/** Božstvo PF1 (Archives of Nethys, `tools/build_deities.py` → [DEITY_TABLE]); [domains] a [subdomains] malými písmeny. */
data class DeityInfo(
    val name: String,
    val pantheon: String,
    val alignment: Alignment?,
    val areas: String,
    val domains: List<String>,
    val weapon: String,
    /** subdomény, které božstvo dává (jména bez závorky, „archon“, „caves“…); null = neznámé (nabídnou se všechny) */
    val subdomains: List<String>? = null,
)

object DeityCatalog {
    val all: List<DeityInfo> get() = DEITY_TABLE

    private val byName by lazy { DEITY_TABLE.associateBy { it.name.lowercase() } }

    /** Božstvo podle jména (velikost písmen nevadí); null = vlastní / neznámé božstvo. */
    fun get(name: String?): DeityInfo? = name?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }?.let { byName[it] }
}

/** Božstvo postavy ze seznamu (null = nezadané nebo vlastní text). */
val Character.deityInfo: DeityInfo? get() = DeityCatalog.get(deity)

/** Classy, které volí domény / blessingy jen ze svého božstva. */
val DEITY_DOMAIN_CLASSES = setOf("cleric", "inquisitor", "warpriest")

/**
 * Domény a subdomény klerika / inquisitora a blessingy warpriesta, které jeho božstvo (ze seznamu) nedává –
 * např. po změně božstva. Prázdné = vše v pořádku, jiná classa, nebo božstvo vlastní / nezadané.
 */
fun ClassEntry.deityConflicts(deity: DeityInfo?): List<String> {
    if (classId !in DEITY_DOMAIN_CLASSES || deity == null || deity.domains.isEmpty()) return emptyList()
    if (classId == "warpriest") {
        return listOfNotNull(subclass[SubclassSlot.BLESSING1], subclass[SubclassSlot.BLESSING2]).filter { !blessingAllowed(it, deity) }
    }
    if (classId == "inquisitor" && subclass[SubclassSlot.INQUISITION] != null) {
        return listOfNotNull(subclass[SubclassSlot.INQUISITION]).filter { !inquisitionAllowed(it, deity) }
    }
    val domains = listOfNotNull(subclass[SubclassSlot.DOMAIN1], subclass[SubclassSlot.DOMAIN2]).filter { it !in deity.domains }
    val subs = deity.subdomains?.let { allowed ->
        listOfNotNull(subclass[SubclassSlot.SUBDOMAIN1], subclass[SubclassSlot.SUBDOMAIN2]).filter { it.substringBefore(" (") !in allowed }
    }.orEmpty()
    return domains + subs
}
