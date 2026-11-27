package cz.pfspells.data

/*
 * Zvířecí společník (Core Rulebook, Druid – Animal Companions, OGL; ověřeno podle d20pfsrd):
 * tabulka základních statistik podle efektivní úrovně druida a startovní statistiky + vylepšení zvířat.
 * HD, BAB, záchrany, skill body a featy z tabulky dávají stejná čísla jako rasové HD „animal“ ([ClassCatalog.creatures]) –
 * proto se z úrovně společníka nastaví jen úroveň „classy“ animal a zbytek (Str/Dex bonus, přirozená zbroj, triky,
 * zvláštní schopnosti, vylepšení zvířete na 4. / 7. úrovni) se dopočítá tady.
 */

enum class CompanionSpecial { LINK, SHARE_SPELLS, EVASION, ABILITY_INCREASE, DEVOTION, MULTIATTACK, IMPROVED_EVASION }

/** Řádek tabulky „Animal Companion Base Statistics“ (efektivní úroveň druida 1–20). */
data class CompanionLevelRow(
    val level: Int,
    val hd: Int,
    val naturalArmor: Int,
    val strDex: Int,
    val bonusTricks: Int,
    val special: List<CompanionSpecial> = emptyList(),
)

object CompanionProgression {
    val rows: List<CompanionLevelRow> = listOf(
        CompanionLevelRow(1, 2, 0, 0, 1, listOf(CompanionSpecial.LINK, CompanionSpecial.SHARE_SPELLS)),
        CompanionLevelRow(2, 3, 0, 0, 1),
        CompanionLevelRow(3, 3, 2, 1, 2, listOf(CompanionSpecial.EVASION)),
        CompanionLevelRow(4, 4, 2, 1, 2, listOf(CompanionSpecial.ABILITY_INCREASE)),
        CompanionLevelRow(5, 5, 2, 1, 2),
        CompanionLevelRow(6, 6, 4, 2, 3, listOf(CompanionSpecial.DEVOTION)),
        CompanionLevelRow(7, 6, 4, 2, 3),
        CompanionLevelRow(8, 7, 4, 2, 3),
        CompanionLevelRow(9, 8, 6, 3, 4, listOf(CompanionSpecial.ABILITY_INCREASE, CompanionSpecial.MULTIATTACK)),
        CompanionLevelRow(10, 9, 6, 3, 4),
        CompanionLevelRow(11, 9, 6, 3, 4),
        CompanionLevelRow(12, 10, 8, 4, 5),
        CompanionLevelRow(13, 11, 8, 4, 5),
        CompanionLevelRow(14, 12, 8, 4, 5, listOf(CompanionSpecial.ABILITY_INCREASE)),
        CompanionLevelRow(15, 12, 10, 5, 6, listOf(CompanionSpecial.IMPROVED_EVASION)),
        CompanionLevelRow(16, 13, 10, 5, 6),
        CompanionLevelRow(17, 14, 10, 5, 6),
        CompanionLevelRow(18, 15, 12, 6, 7),
        CompanionLevelRow(19, 15, 12, 6, 7),
        CompanionLevelRow(20, 16, 12, 6, 7, listOf(CompanionSpecial.ABILITY_INCREASE)),
    )

    fun at(level: Int): CompanionLevelRow = rows[level.coerceIn(1, 20) - 1]

    /** Zvláštní schopnosti získané do dané úrovně (Evasion → Improved evasion; zvýšení atributu jen jednou – počet viz [abilityIncreases]). */
    fun specialsUpTo(level: Int): List<CompanionSpecial> =
        rows.take(level.coerceIn(1, 20)).flatMap { it.special }.distinct()
            .filterNot { it == CompanionSpecial.EVASION && level >= 15 }

    /** Kolikrát může společník přidat +1 k atributu (4., 9., 14., 20. úroveň) – rozděluje se ručně. */
    fun abilityIncreases(level: Int): Int = rows.take(level.coerceIn(1, 20)).count { CompanionSpecial.ABILITY_INCREASE in it.special }
}

/**
 * Druh zvířete: startovní statistiky a vylepšení na úrovni [advLevel] (4. nebo 7.).
 * [naturalArmor] a [advNaturalArmor] = přirozená zbroj druhu (vylepšení přičítá); útoky a schopnosti jsou anglicky jako v příručce.
 */
data class AnimalSpecies(
    val id: String,
    val name: String,
    val size: CreatureSize,
    val quadruped: Boolean,
    val speed: String,
    val naturalArmor: Int,
    val attacks: String,
    val abilities: Abilities,
    val qualities: String,
    val advLevel: Int,
    val advSize: CreatureSize? = null,
    val advNaturalArmor: Int = 0,
    val advAttacks: String? = null,
    val advStr: Int = 0,
    val advDex: Int = 0,
    val advCon: Int = 0,
    val advQualities: String? = null,
)

object SpeciesCatalog {
    /** Core Rulebook (ručně, ověřeno) + další z d20pfsrd ([EXTRA_SPECIES], tools/build_companions.py) */
    private fun ab(str: Int, dex: Int, con: Int, int: Int, wis: Int, cha: Int) = Abilities(str, dex, con, int, wis, cha)
    private const val LL_SCENT = "low-light vision, scent"

    val all: List<AnimalSpecies> = listOf(
        AnimalSpecies("ape", "Ape", CreatureSize.MEDIUM, false, "30 ft., climb 30 ft.", 1, "bite (1d4), 2 claws (1d4)",
            ab(13, 17, 10, 2, 12, 7), LL_SCENT, 4, CreatureSize.LARGE, 2, "bite (1d6), 2 claws (1d6)", 8, -2, 4),
        AnimalSpecies("badger", "Badger (wolverine)", CreatureSize.SMALL, true, "30 ft., burrow 10 ft., climb 10 ft.", 2, "bite (1d4), 2 claws (1d3)",
            ab(10, 17, 15, 2, 12, 10), "rage (6 rounds/day, as barbarian); $LL_SCENT", 4, CreatureSize.MEDIUM, 0, "bite (1d6), 2 claws (1d4)", 4, -2, 2),
        AnimalSpecies("bear", "Bear", CreatureSize.SMALL, true, "40 ft.", 2, "bite (1d4), 2 claws (1d3)",
            ab(15, 15, 13, 2, 12, 6), LL_SCENT, 4, CreatureSize.MEDIUM, 0, "bite (1d6), 2 claws (1d4)", 4, -2, 2),
        AnimalSpecies("bird", "Bird (eagle, hawk, owl)", CreatureSize.SMALL, false, "10 ft., fly 80 ft. (average)", 1, "bite (1d4), 2 talons (1d4)",
            ab(10, 15, 12, 2, 14, 6), "low-light vision", 4, advStr = 2, advCon = 2),
        AnimalSpecies("boar", "Boar", CreatureSize.SMALL, true, "40 ft.", 6, "gore (1d6)",
            ab(13, 12, 15, 2, 13, 4), LL_SCENT, 4, CreatureSize.MEDIUM, 0, "gore (1d8)", 4, -2, 2, "ferocity"),
        AnimalSpecies("camel", "Camel", CreatureSize.LARGE, true, "50 ft.", 1,
            "bite (1d4) or spit (ranged touch, sickened 1d4 rounds, range 10 ft.)",
            ab(18, 16, 14, 2, 11, 4), LL_SCENT, 4, advStr = 2, advCon = 2),
        AnimalSpecies("cat_big", "Cat, big (lion, tiger)", CreatureSize.MEDIUM, true, "40 ft.", 1, "bite (1d6), 2 claws (1d4); rake (1d4)",
            ab(13, 17, 13, 2, 15, 10), LL_SCENT, 7, CreatureSize.LARGE, 2, "bite (1d8), 2 claws (1d6); grab, pounce, rake (1d6)", 8, -2, 4),
        AnimalSpecies("cat_small", "Cat, small (cheetah, leopard)", CreatureSize.SMALL, true, "50 ft.", 1, "bite (1d4 plus trip), 2 claws (1d2)",
            ab(12, 21, 13, 2, 12, 6), LL_SCENT, 4, CreatureSize.MEDIUM, 0, "bite (1d6 plus trip), 2 claws (1d3)", 4, -2, 2,
            "sprint (once per hour, 10× speed on a charge)"),
        AnimalSpecies("crocodile", "Crocodile (alligator)", CreatureSize.SMALL, true, "20 ft., swim 30 ft.", 4, "bite (1d6 plus grab)",
            ab(15, 14, 15, 1, 12, 2), "hold breath, low-light vision", 4, CreatureSize.MEDIUM, 0, "bite (1d8) or tail slap (1d12)", 4, -2, 2,
            "death roll, sprint"),
        AnimalSpecies("deinonychus", "Dinosaur (deinonychus, velociraptor)", CreatureSize.SMALL, false, "60 ft.", 1, "2 talons (1d6), bite (1d4)",
            ab(11, 17, 17, 2, 12, 14), LL_SCENT, 7, CreatureSize.MEDIUM, 1, "2 talons (1d8), bite (1d6), 2 claws (1d4)", 4, -2, 2, "pounce"),
        AnimalSpecies("dog", "Dog", CreatureSize.SMALL, true, "40 ft.", 2, "bite (1d4)",
            ab(13, 17, 15, 2, 12, 6), LL_SCENT, 4, CreatureSize.MEDIUM, 0, "bite (1d6)", 4, -2, 2),
        AnimalSpecies("horse", "Horse", CreatureSize.LARGE, true, "50 ft.", 4, "bite (1d4), 2 hooves (1d6, secondary)",
            ab(16, 13, 15, 2, 12, 6), LL_SCENT, 4, advStr = 2, advCon = 2, advQualities = "combat trained"),
        AnimalSpecies("pony", "Pony", CreatureSize.MEDIUM, true, "40 ft.", 2, "2 hooves (1d3)",
            ab(13, 13, 12, 2, 11, 4), LL_SCENT, 4, advStr = 2, advCon = 2, advQualities = "combat trained"),
        AnimalSpecies("shark", "Shark", CreatureSize.SMALL, false, "swim 60 ft.", 4, "bite (1d4)",
            ab(13, 15, 15, 1, 12, 2), LL_SCENT, 4, CreatureSize.MEDIUM, 0, "bite (1d6)", 4, -2, 2, "blindsense 30 ft."),
        AnimalSpecies("snake_constrictor", "Snake, constrictor", CreatureSize.MEDIUM, false, "20 ft., climb 20 ft., swim 20 ft.", 2, "bite (1d3 plus grab)",
            ab(15, 17, 13, 1, 12, 2), LL_SCENT, 4, CreatureSize.LARGE, 1, "bite (1d4)", 8, -2, 4, "constrict (1d4)"),
        AnimalSpecies("snake_viper", "Snake, viper", CreatureSize.SMALL, false, "20 ft., climb 20 ft., swim 20 ft.", 2, "bite (1d3 plus poison)",
            ab(8, 17, 11, 1, 12, 2), LL_SCENT, 4, CreatureSize.MEDIUM, 0, "bite (1d4 plus poison)", 4, -2, 2),
        AnimalSpecies("wolf", "Wolf", CreatureSize.MEDIUM, true, "50 ft.", 2, "bite (1d6 plus trip)",
            ab(13, 15, 15, 2, 12, 6), LL_SCENT, 7, CreatureSize.LARGE, 2, "bite (1d8 plus trip)", 8, -2, 4),
    ) + EXTRA_SPECIES
    private val byId = all.associateBy { it.id }

    fun get(id: String?): AnimalSpecies? = id?.let { byId[it] }
}

/** Druh zvířete mazlíčka (null = vlastní / bez šablony). */
val Character.speciesInfo: AnimalSpecies? get() = SpeciesCatalog.get(species)

/** Řádek progrese zvířecího společníka (null = bez progrese – HD a vše ostatní ručně). */
val Character.companionRow: CompanionLevelRow? get() = if (isFamiliar || isEidolon) null else companionLevel?.let { CompanionProgression.at(it) }

/** Zvíře už prošlo vylepšením druhu (4. / 7. úroveň společníka). */
val Character.speciesAdvanced: Boolean
    get() {
        val sp = speciesInfo ?: return false
        return (companionLevel ?: 0) >= sp.advLevel
    }

/** Úprava atributu ze společníka: Str/Dex bonus z tabulky + vylepšení druhu (Str, Dex, Con). */
fun Character.companionMod(a: Ability): Int {
    // familiár: Int aspoň podle tabulky (6 na 1.–2. úrovni pána … 15)
    familiarLevel?.let { lvl -> return if (a == Ability.INT) maxOf(0, FamiliarProgression.intelligence(lvl) - abilities.int) else 0 }
    if (isEidolon) return eidolonMod(a)
    val row = companionRow ?: return 0
    val sp = speciesInfo
    val adv = speciesAdvanced && sp != null
    return when (a) {
        Ability.STR -> row.strDex + if (adv) sp!!.advStr else 0
        Ability.DEX -> row.strDex + if (adv) sp!!.advDex else 0
        Ability.CON -> if (adv) sp!!.advCon else 0
        else -> 0
    }
}

/** Velikost zvířete podle druhu a úrovně společníka (null = bez šablony). */
fun companionSize(species: AnimalSpecies?, companionLevel: Int?): CreatureSize? {
    species ?: return null
    return if ((companionLevel ?: 0) >= species.advLevel) species.advSize ?: species.size else species.size
}

/** Přirozená zbroj celkem: druh (+ vylepšení) + bonus z tabulky společníka; null = bez šablony i progrese. */
val Character.naturalArmor: Int?
    get() {
        familiarLevel?.let { return FamiliarProgression.naturalArmor(it) }
        if (isEidolon) return eidolonNaturalArmor
        val sp = speciesInfo
        val row = companionRow
        if (sp == null && row == null) return null
        return (sp?.naturalArmor ?: 0) + (if (speciesAdvanced) sp?.advNaturalArmor ?: 0 else 0) + (row?.naturalArmor ?: 0)
    }

/**
 * Navržená efektivní úroveň druida pro společníka podle class vlastníka: druid a hunter plně, ranger −3,
 * u jezdeckého zvířete paladin a cavalier / samurai. Vždy aspoň 1.
 */
fun Character.suggestedCompanionLevel(kind: CompanionKind): Int {
    if (kind == CompanionKind.FAMILIAR) return suggestedFamiliarLevel()
    if (kind == CompanionKind.EIDOLON) return suggestedEidolonLevel()
    fun lvl(id: String) = classes.firstOrNull { it.classId == id }?.level ?: 0
    val nature = lvl("druid") + lvl("hunter") + maxOf(0, lvl("ranger") - 3)
    val mount = lvl("paladin") + lvl("cavalier") + lvl("samurai")
    val total = when (kind) {
        CompanionKind.MOUNT -> mount + nature
        else -> nature
    }
    return total.coerceIn(1, 20)
}

/**
 * Nastaví mazlíčkovi druh, progresi a velikost. Se šablonou druhu se velikost a „čtyřnohý“ berou z druhu
 * (vylepšení na 4. / 7. úrovni zvětší zvíře), při změně druhu se základní atributy přepíšou startovními.
 * S progresí se úroveň rasových HD „animal“ nastaví podle tabulky (chybí-li, přidá se jako první classa).
 */
fun Character.withCompanion(
    kind: CompanionKind?,
    species: String?,
    companionLevel: Int?,
    size: CreatureSize?,
    quadruped: Boolean,
    familiar: String? = null,
    eidolonForm: String? = null,
    eidolonSubtype: String? = null,
): Character {
    if (kind == CompanionKind.EIDOLON) return withEidolon(companionLevel, eidolonForm, eidolonSubtype, size, quadruped)
    val sp = SpeciesCatalog.get(species)
    val lvl = companionLevel?.coerceIn(1, 20)
    var classes = classes
    if (lvl != null) {
        // familiár má HD podle úrovně pána, zvířecí společník podle tabulky
        val hd = if (kind == CompanionKind.FAMILIAR) lvl else CompanionProgression.at(lvl).hd
        classes = if (classes.any { it.classId == "animal" }) classes.map { if (it.classId == "animal") it.copy(level = hd) else it }
        else listOf(ClassEntry("animal", hd)) + classes
    }
    val newSize = companionSize(sp, lvl) ?: size
    return copy(
        eidolonForm = null,
        eidolonSubtype = null,
        companion = kind,
        species = sp?.id,
        familiar = FamiliarCatalog.get(familiar)?.id?.takeIf { kind == CompanionKind.FAMILIAR },
        companionLevel = lvl,
        size = newSize,
        quadruped = (sp?.quadruped ?: quadruped) && newSize != null,
        abilities = if (sp != null && sp.id != this.species) sp.abilities else abilities,
        classes = classes,
    )
}

/**
 * Eidolon: základní forma (startovní atributy při změně formy, velikost, čtyřnohý = quadruped / tauric), progrese podle
 * úrovně summonera – HD z tabulky jako úroveň „classy“ eidolon (chybí-li, přidá se jako první).
 */
private fun Character.withEidolon(companionLevel: Int?, formId: String?, subtype: String?, size: CreatureSize?, quadruped: Boolean): Character {
    val form = Eidolons.form(formId)
    val lvl = companionLevel?.coerceIn(1, 20)
    var classes = classes
    if (lvl != null) {
        val hd = Eidolons.row(lvl, form?.unchained == true).hd
        classes = if (classes.any { it.classId == "eidolon" }) classes.map { if (it.classId == "eidolon") it.copy(level = hd) else it }
        else listOf(ClassEntry("eidolon", hd)) + classes.filterNot { it.classId == "animal" && it.level <= 1 }
    }
    val large = evolutions.any { it.id == "large" }
    val newSize = form?.let { if (large) CreatureSize.LARGE else it.size } ?: size
    return copy(
        companion = CompanionKind.EIDOLON,
        species = null,
        familiar = null,
        companionLevel = lvl,
        eidolonForm = form?.id,
        eidolonSubtype = subtype?.takeIf { form?.unchained != false && it in EIDOLON_SUBTYPES },
        size = newSize,
        quadruped = if (form != null) form.id.removePrefix("u_") in setOf("quadruped", "tauric") else quadruped && newSize != null,
        abilities = if (form != null && form.id != this.eidolonForm) form.abilities else abilities,
        classes = classes,
    )
}

