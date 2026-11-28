package cz.pfspells

import cz.pfspells.data.ResourceUnit
import cz.pfspells.data.AbilityGroup
import cz.pfspells.data.AbilityKind
import cz.pfspells.data.SubclassType
import cz.pfspells.data.PrereqStatus
import cz.pfspells.data.ClassGroup
import cz.pfspells.data.CastingKind
import cz.pfspells.data.MagicTradition
import cz.pfspells.data.CompanionKind
import cz.pfspells.data.CompanionRarity
import cz.pfspells.data.ItemSlot
import cz.pfspells.data.BonusTarget
import cz.pfspells.data.BonusType
import cz.pfspells.data.Defense
import cz.pfspells.data.CompanionSpecial
import cz.pfspells.data.AnimalSpecies
import cz.pfspells.data.StrictBlock
import cz.pfspells.data.CharacterStats
import cz.pfspells.data.Ability
import cz.pfspells.data.SpellItemKind
import cz.pfspells.data.CreatureSize
import cz.pfspells.data.RaceGroup
import cz.pfspells.data.Alignment
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import cz.pfspells.data.ImportError
import cz.pfspells.data.ItemCategory
import cz.pfspells.data.Load
import cz.pfspells.data.SaveAction

/**
 * Jazyk rozhraní. Data (názvy a popisy spellů/itemů, classy, školy magie) zůstávají v originále anglicky.
 * Nový jazyk = nová implementace [Strings] + položka zde.
 */
enum class Lang(val code: String, val label: String) {
    EN("en", "EN"),
    CS("cs", "CZ");

    val strings: Strings get() = when (this) { EN -> EnStrings; CS -> CsStrings }

    companion object {
        fun of(code: String?): Lang? = entries.firstOrNull { it.code == code }

        /** Výchozí jazyk podle systému: čeština (i pro slovenštinu), jinak angličtina. */
        fun system(): Lang = try {
            if (androidx.compose.ui.text.intl.Locale.current.language in setOf("cs", "sk")) CS else EN
        } catch (e: Exception) {
            EN
        }
    }
}

val LocalStrings = staticCompositionLocalOf<Strings> { EnStrings }

/** Texty v aktuálním jazyce (zkratka pro `LocalStrings.current`). */
val str: Strings
    @Composable @ReadOnlyComposable get() = LocalStrings.current

/** Všechny texty rozhraní. Interface (ne open class), aby kompilátor hlídal, že žádný překlad nechybí. */
interface Strings {
    // obecné
    val back: String
    val cancel: String
    val save: String
    val delete: String
    val rename: String
    val create: String
    val add: String
    val close: String
    val all: String
    val anyOption: String
    val open: String
    val menu: String
    val decrease: String
    val increase: String
    val clearSearch: String
    val closeDetails: String
    val loading: String
    val name: String
    val language: String
    fun results(n: Int): String
    fun sourceLine(src: String): String

    // start
    val loadingSpells: String
    fun couldNotLoadSpells(msg: String): String
    val useBundledData: String
    val pricesRevealed: String
    val pricesHidden: String

    // sekce, hlavička
    val tabSpellbook: String
    val tabInventory: String
    val spellCompendium: String
    val compendium: String
    val searchSpells: String
    val searchItem: String
    val backToInventory: String
    val placeholderNoInventories: String
    val placeholderPickInventory: String
    fun placeholderPickCharacter(spellCount: Int): String

    // menu
    val importCharacter: String
    val exportAllCharacters: String
    val importInventory: String
    val exportAllInventories: String
    val menuDatabase: String
    val menuAbout: String

    // decky
    val myCharacters: String
    val newCharacter: String
    val noCharactersYet: String
    fun emptyCharacters(spellCount: Int): String
    val characterNameLabel: String
    val classLabel: String
    val characterLevel: String
    val levelLabel: String
    val addSpells: String
    val exportCharacter: String
    val deleteCharacter: String
    val deleteCharacterTitle: String
    fun deleteCharacterText(name: String): String
    val renameCharacter: String
    fun missingSpells(n: Int): String
    val characterNoSpells: String
    val notOnClassList: String
    val notAvailableAtLevel: String
    val importedSuffix: String

    // spelly
    fun spells(n: Int): String
    fun copies(n: Int): String
    fun spellLevelLabel(level: Int): String
    /** „3 / 3+1 prepared“ */
    fun slotsPrepared(chosen: Int, limit: String): String
    /** „2 / 4 known“ */
    fun slotsKnown(chosen: Int, limit: String): String
    /** „5+1 per day“ */
    fun slotsPerDay(slots: String): String
    val atWill: String

    // alternativní rasové traity
    fun racialTraitsButton(n: Int): String
    fun racialTraitsTitle(race: String): String
    val defaultTraitsLabel: String
    val chosenTraitsLabel: String
    fun replacesTraits(list: String): String
    fun traitConflict(list: String): String
    val noAlternateTraits: String
    val racialTraitsHint: String
    val sourceRacialTraits: String
    fun archetypesButton(n: Int): String
    fun archetypesTitle(cls: String): String
    val chosenArchetypesLabel: String
    val archetypesHint: String
    fun archetypeConflict(list: String): String
    val noArchetypes: String
    val sourceArchetypes: String
    fun prestigeTag(maxLevel: Int): String
    fun prestigeKind(kind: String): String
    fun prestigeAdvancesTag(kinds: String): String
    fun advanceLabel(kindName: String): String
    val advanceNone: String
    fun advanceSummary(cls: String, n: Int): String
    fun casterLevelInfo(n: Int): String
    val missingAdvance: String
    val prestigeDetails: String
    val prestigeRequirementsHeader: String
    val prestigeTableHeader: String
    val prestigeFeaturesHeader: String
    val prestigeHint: String
    val prestigeColumns: List<String>
    val prestigeUnmet: String
    val sourcePrestige: String
    fun fxRemoves(list: String): String
    fun fxGains(list: String): String
    fun fxChanges(list: String): String
    val fxNoSpells: String
    val fxNoCantrips: String
    val fxNoSubclass: String
    val fxNoSubclassSpells: String
    val fxNoSubclassPowers: String
    fun fxLostAbilities(n: Int): String
    fun fxLostFeats(n: Int): String
    fun fxGainedFeats(n: Int): String
    fun fxPerDay(delta: Int): String
    fun fxKnown(delta: Int): String
    val fxOneDomain: String
    val secondBloodline: String
    fun fxSpellList(list: String): String
    fun fxAddedSpells(n: Int): String
    fun fxAsClass(cls: String): String
    fun fxSubclassLevel(delta: Int): String
    val crossbloodedHint: String
    /** „+2 k:“ / „−2 k:“ – volba atributu u alternativního traitu */
    fun abilityChoiceLabel(amount: Int): String
    fun missingAbilityChoice(trait: String): String

    // hexy a revelations
    fun abilityGroupName(g: AbilityGroup): String
    fun addClassAbility(g: AbilityGroup): String
    fun noClassAbilities(g: AbilityGroup): String
    fun abilityKind(k: AbilityKind): String
    /** „od 7. úrovně“ */
    fun fromClassLevel(n: Int): String
    val allMysteries: String
    val allSpirits: String
    val removeAbility: String
    val searchAbilities: String
    val loadingAbilities: String
    fun abilitiesHint(g: AbilityGroup): String
    val sourceClassAbilities: String
    val sourceSubclassAbilities: String

    // featy
    val featsHeader: String
    /** „3 / 6“ – vybrané / dostupné */
    fun featCount(have: Int, total: Int): String
    /** rozpis počtu: „Levels 3 · Fighter +2“ */
    fun featSlotsGeneral(n: Int): String
    val extraFeats: String
    val addFeat: String
    val customFeat: String
    val featName: String
    val featText: String
    val featNote: String
    val noFeats: String
    val removeFeat: String
    val prerequisites: String
    fun prereqStatus(st: PrereqStatus): String
    val prereqUnmet: String
    val onlyMet: String
    val featType: String
    val generalFeat: String
    val searchFeats: String
    fun featsInDatabase(n: Int): String
    val addFeatToCharacter: String
    fun featTaken(n: Int): String
    val customBadge: String
    val loadingFeats: String
    fun featsFailed(msg: String): String
    val editFeat: String

    // propojení s inventářem
    val inventoryHeader: String

    // schopnosti s počtem použití
    val resourcesHeader: String
    val addTracker: String
    val editTracker: String
    val removeTracker: String
    val trackerName: String
    val trackerMax: String
    val trackerUnit: String
    val trackerNote: String
    fun unitName(u: ResourceUnit): String
    /** „3× za den“, „12 kol za den“, „4 body“; null = bez omezení */
    fun resourceAmount(n: Int?, u: ResourceUnit): String
    fun remaining(left: Int, max: Int): String
    fun showHidden(n: Int): String
    val hideFeature: String
    val noResources: String

    // podtřídy
    fun subclassTitle(className: String): String
    fun subclassLabel(t: SubclassType): String
    fun domainN(n: Int): String
    val subdomain: String
    fun oppositionN(n: Int): String
    val universalist: String
    fun elementalSchool(element: String): String
    fun element(name: String): String
    val noneChosen: String
    val chooseSubclass: String
    val subclassLaterHint: String
    val subdomainHint: String
    val druidDomainHint: String
    val schoolHint: String
    fun bonusSpellsHeader(t: SubclassType): String
    fun domainSlot(chosen: Int): String
    fun knownFromLevel(n: Int): String
    val knownAutomatically: String
    val schoolSlot: String
    val oppositionWarn: String
    val oppositionShort: String
    val subclassNotAvailable: String
    fun blessingN(n: Int): String
    val blessingHint: String
    fun deityBlessingsHint(deity: String): String
    val inquisitorDomainHint: String
    val inquisitionLabel: String
    fun inquisitionName(name: String): String
    val patronPrepareHint: String
    val wanderingSpirit: String
    val wanderingSpiritHint: String
    fun bloodlineBonusFeats(feats: String): String
    val patronHint: String
    val patronSpellsNote: String
    val spiritHint: String
    val spiritSlot: String
    val bloodragerBloodlineHint: String
    val disciplineHint: String
    val subclassPowersHeader: String
    val includeInventory: String
    fun withInventory(name: String): String
    val exportFeats: String
    val importFeats: String
    fun exportFeatsTitle(n: Int): String
    val exportFeatsHint: String
    fun importFeatsTitle(character: String): String
    fun featsImportSummary(n: Int, skipped: Int): String
    val linkInventory: String
    val chooseOption: String
    val newInventoryForCharacter: String
    val openInventory: String
    val unlinkInventory: String
    fun strengthFromCharacter(name: String): String
    /** „Inventory: Elara · 45 lb · light load“ */
    fun inventorySummary(name: String, weight: String, load: String): String
    val featAlreadyTaken: String
    val gameMode: String
    val rest: String
    /** záložky rozdělovníku v detailu postavy (Deník dobrodruha) */
    val tabCharacter: String
    val tabCombat: String
    val tabCompanions: String
    val tabSpells: String
    val sectionsLabel: String
    /** přepínač režimu kouzel / schopností: příprava (úpravy) | hra (odklikávání) */
    val modePrepare: String
    val modePlay: String
    val modeHintPrepare: String
    val modeHintPlay: String
    /** souhrn postavy jako tabulka (ÚR., ZÚB, záchrany) */
    val levelShort: String
    val babShort: String
    val fortShort: String
    val refShort: String
    val willShort: String
    /** ruční poznámka u hlavního atributu sesílání */
    val mainAbilityNote: String
    /** razítka stavů, které mění pravidla */
    val stampOverLimit: String
    val stampUnconscious: String
    val stampOverloaded: String
    /** kladné razítko po odpočinku */
    val restDone: String
    /** ruční nápověda u životů */
    val hpHandHint: String
    /** čárkovaný box volných míst ve spell levelu (příprava) */
    fun freeSlots(n: Int): String
    /** nadpisy prázdných stavů (ručním písmem) */
    val emptySpellsTitle: String
    val emptyInventoryTitle: String
    val emptyCompanionsTitle: String
    val emptyNoCasterTitle: String
    /** hlavička širokého okna: data jsou jen v zařízení */
    val savedOffline: String
    val restedToast: String
    val castSpell: String
    /** „2 / 5 cast“ */
    fun castOf(used: Int, total: Int): String
    val slotLabel: String
    val removeSpell: String
    /** „needs INT 13“ */
    fun abilityNeeded(ability: String, score: Int): String
    /** „Casting: INT 16 (+3)“ */
    fun castingAbility(ability: String, score: Int, mod: String): String
    val noSpellsYet: String
    fun spellLevelSingle(level: Int): String
    fun spellLevelRange(min: Int, max: Int): String
    fun unknownClassLevel(level: Int): String
    fun addTo(deckName: String): String
    fun spellsInDatabase(n: Int): String
    val school: String
    val searchDescriptions: String
    val hideAdded: String
    fun cannotCastYet(className: String?, level: Int): String
    val addToCharacter: String
    fun addCopy(inDeck: Int): String
    fun addToCharacterCount(inDeck: Int): String
    fun cannotCastThis(className: String, level: Int): String
    val castingTime: String
    val components: String
    val range: String
    val effect: String
    val target: String
    val duration: String
    val savingThrow: String
    val spellResistance: String
    val description: String
    val mythic: String
    val openOnAon: String

    // export / import
    fun exportCharactersTitle(n: Int): String
    val exportCharactersHint: String
    fun notFoundSkipped(names: String): String
    fun exportInventoriesTitle(n: Int): String
    val exportInventoriesHint: String
    val copy: String
    val copiedToClipboard: String
    val done: String
    fun failed(msg: String?): String
    val pasteFromClipboard: String
    val openFile: String
    fun cannotReadFile(msg: String?): String
    val exportText: String
    fun importButton(n: Int): String
    fun saveAction(a: SaveAction): String
    fun importError(e: ImportError): String

    // inventář
    val myInventories: String
    val newInventory: String
    val defaultInventoryName: String
    val inventoryNameLabel: String
    val noInventoriesYet: String
    fun itemTypes(n: Int): String
    fun worth(value: String): String
    val purse: String
    val load: String
    val categories: String
    fun category(c: ItemCategory): String
    val totalWeight: String
    fun loadName(l: Load): String
    fun loadLimits(light: String, medium: String, heavy: String): String
    fun coinsWeight(w: String): String
    val strength: String
    fun editCoin(coin: String): String
    fun coinsTitle(coin: String): String
    val addItem: String
    val exportInventory: String
    val deleteInventory: String
    val deleteInventoryTitle: String
    fun deleteInventoryText(name: String): String
    val renameInventory: String
    val inventoryEmpty: String
    val notes: String
    val notesHint: String
    val colItem: String
    val colQty: String
    val colWeight: String
    val colPrice: String
    val colNotes: String
    val colName: String
    val colType: String
    val colDmg: String
    val colCrit: String

    // itemy
    fun couldNotLoadItems(msg: String): String
    val loadingItems: String
    fun itemPickerSubtitle(items: Int, invName: String): String
    val customItem: String
    fun searchItems(n: Int): String
    fun proficiency(p: String): String
    fun upTo(price: String): String
    val maxPrice: String
    val anyPrice: String
    fun owned(n: Int): String
    fun addNamed(name: String): String
    val itemFallback: String
    val cost: String
    val weight: String
    val damageS: String
    val damageM: String
    val critical: String
    val type: String
    val special: String
    val armorBonus: String
    val maxDex: String
    val checkPenalty: String
    val spellFailure: String
    val aura: String
    val casterLevel: String
    val slot: String
    val requirements: String
    val info: String
    fun youOwn(n: Int): String
    val inInventory: String
    val quantity: String
    val noteOptional: String
    val notePlaceholder: String
    val editItem: String
    val removeFromInventory: String
    val addToInventory: String
    val itemNamePlaceholder: String
    val categoryLabel: String
    val priceGp: String
    val weightLb: String
    val shortInfo: String
    val shortInfoPlaceholder: String

    // databáze
    val databases: String
    val spellsHeader: String
    val itemsHeader: String
    val bundledVersion: String
    val downloadedVersion: String
    fun downloadedAt(date: String): String
    val spellsDbInfo: String
    val itemsDbInfo: String
    val downloading: String
    val downloadLatestSpells: String
    val downloadLatestItems: String
    val restoreBundled: String
    val charactersKept: String
    val inventoriesKept: String
    // aktualizace appky (GitHub Releases)
    val appUpdatesHeader: String
    fun installedVersion(v: String): String
    val devBuild: String
    val checkForUpdates: String
    val checkingUpdates: String
    val upToDate: String
    fun updateAvailable(v: String): String
    val downloadAndInstall: String
    val releaseNotes: String
    fun updateDownloading(percent: Int?): String
    fun updateFailed(msg: String): String
    val updateHintApk: String
    val updateHintInstaller: String
    val updateLater: String
    val updateSkip: String
    fun updateConfirmTitle(v: String): String
    val updateBackupHint: String
    val updateSignatureMismatch: String
    val installNow: String
    val installAndClose: String
    val downloadApkInBrowser: String
    val itemDbFailed: String
    fun itemsCount(n: Int): String
    fun downloadedSpells(n: Int): String
    fun downloadedItems(n: Int): String
    fun downloadFailed(msg: String?): String
    val bundledSpellsRestored: String
    val bundledItemsRestored: String

    // about
    val aboutTitle: String
    val couldNotLoadLicense: String
    val gplText: String
    val gameContentNotGpl: String
    val oglText: String
    val showOgl: String
    val hideOgl: String
    val dataSources: String
    val sourceSpells: String
    val sourceFeats: String
    val sourceClasses: String
    val sourceRaces: String
    val sourceDeities: String
    val sourceCompanions: String
    val privacyHeader: String
    val privacyText: String
    val licenseHeader: String
    val sourceWeapons: String
    val sourceGear: String
    val fonts: String
    val fontsText: String
    val libraries: String
    val librariesText: String

    // classy, školy magie, spell listy (data jsou anglicky; EN vrací originál)
    /** [original] = anglický název z [cz.pfspells.data.CasterClass.name] */
    fun className(id: String, original: String): String
    /** položka v nabídce class: v češtině i s originálem, aby šlo hledat v příručkách */
    fun classOption(id: String, original: String): String
    /** název spell listu ze zdroje („sorcerer/wizard“) */
    fun spellListName(key: String): String
    fun school(name: String): String
    fun subschool(name: String): String

    // řazení a PFS
    val sortLabel: String
    fun spellSort(s: SpellSort): String
    val pfsOnly: String
    val notPfsLegal: String
    val favoritesOnly: String
    val favorite: String
    val addFavorite: String
    val removeFavorite: String

    // zatížení
    /** postihy podle zatížení, null = žádné */
    fun loadPenalties(l: Load): String?
    fun loadIncreased(l: Load): String
    val sizeLabel: String
    fun creatureSize(c: CreatureSize): String
    val quadruped: String

    // postava: classy, atributy (multiclass)
    val classesHeader: String
    val addClass: String
    val removeClass: String
    fun removeClassText(className: String): String
    val abilitiesHeader: String
    fun abilityShort(a: Ability): String
    fun abilityLong(a: Ability): String
    fun statsLine(st: CharacterStats): String
    val chooseClass: String
    val searchClasses: String
    fun classGroup(g: ClassGroup): String
    /** štítek druhu magie na kartě classy */
    fun magicTradition(t: MagicTradition): String
    /** štítek způsobu sesílání na kartě classy */
    fun castingKind(k: CastingKind): String

    // přísný režim
    val strictMode: String
    val strictModeHint: String
    fun strictBlocked(b: StrictBlock): String

    // mazlíčci a společníci
    val companionsHeader: String
    val companionsHint: String
    val addCompanion: String
    val newCompanion: String
    val companionSettings: String
    val companionKindLabel: String
    fun companionKind(k: CompanionKind): String
    val creatureLabel: String
    val hitDiceLabel: String
    /** velikost mazlíčka nezadaná – platí rasa */
    val sizeByRace: String
    /** „Mazlíček postavy Valeros“ (odkaz na vlastníka) */
    fun companionOf(kind: String, owner: String): String
    val speciesLabel: String
    val speciesCustom: String
    fun speciesName(sp: AnimalSpecies): String
    val companionProgression: String
    val companionLevelLabel: String
    val familiarLabel: String
    val familiarLevelLabel: String
    val masterBonusLabel: String
    val masterBonusConditional: String
    val familiarNoteLabel: String
    fun familiarSpecial(sp: cz.pfspells.data.FamiliarSpecial): String
    fun familiarStats(naturalArmor: Int, int: Int, sr: Int?): String
    val familiarHint: String
    val eidolonFormLabel: String
    val specialsLabel: String
    val eidolonSubtypeLabel: String
    val summonerLevelLabel: String
    fun eidolonStats(hd: Int, naturalArmor: Int?, strDex: Int, maxAttacks: Int): String
    fun evolutionPool(used: Int, total: Int?): String
    val evolutionsOver: String
    val evolutionsHeader: String
    val addEvolution: String
    fun evolutionCost(n: Int): String
    val freeEvolutionsLabel: String
    val eidolonHint: String
    fun companionSpecial(sp: CompanionSpecial): String
    /** „Přirozená zbroj +4 · Str/Dex +1 · triky navíc 2“ */
    fun companionStats(naturalArmor: Int?, strDex: Int?, tricks: Int?): String
    fun abilityIncreases(n: Int): String
    val speedLabel: String
    val attacksLabel: String
    val qualitiesLabel: String
    /** „vylepšení na 4. úrovni“ */
    fun advancementAt(level: Int): String
    fun companionIncludedHint(mods: String): String
    val rarityLabel: String
    fun rarity(r: CompanionRarity): String
    val respawnLabel: String
    /** výchozí pravidla pro návrat / náhradu po smrti podle druhu (Core Rulebook, APG) */
    fun defaultRespawn(k: CompanionKind): String
    val resetToDefault: String

    // nasazené vybavení
    val equipmentHeader: String
    val equippedLabel: String
    val equipHint: String
    fun itemSlot(sl: ItemSlot): String
    val noSlot: String
    val bonusesLabel: String
    val addBonus: String
    val bonusTargetLabel: String
    fun bonusTarget(t: BonusTarget): String
    val bonusTypeLabel: String
    fun bonusType(t: BonusType): String
    val bonusValueLabel: String
    val bonusSkillLabel: String
    fun slotConflict(slots: String): String
    fun gearIncludedHint(mods: String): String
    /** „Vybavení: Odol +2, Reflex +2, Vůle +2 · AC +2 deflection“ */
    fun gearSummary(parts: String): String
    fun noBonuses(): String

    // obrana, životy, metamagie
    val defenseHeader: String
    val acLabel: String
    val touchLabel: String
    val flatFootedLabel: String
    val hpLabel: String
    /** „10 + zbroj 5 + štít 2 + Obr 2 (max 3) + …“ */
    fun acBreakdown(d: Defense): String
    fun hpHint(auto: Boolean): String
    fun armorCheckPenalty(n: Int): String
    val adjust: String
    val hpBonusLabel: String
    val hpOverrideLabel: String
    val acMiscLabel: String
    val damageHint: String
    val metamagicHeader: String
    /** řádek metamagie u kouzla: [levels] zvýšení (null = proměnné), [slot] výsledná úroveň slotu, [tooHigh] nad max. úrovní classy */
    fun metamagicLine(feat: String, levels: Int?, slot: Int?, tooHigh: Boolean): String
    /** u metamagického featu: o kolik zvyšuje slot */
    fun metamagicSlot(levels: Int?): String

    // inventář 2.0 – figurka a sloty
    fun itemsTab(n: Int): String
    fun slotsFilled(filled: Int, total: Int): String
    val emptySlot: String
    fun ringSlot(i: Int): String
    val carriedLabel: String
    val carriedGroup: String
    fun conflictBanner(slot: String, n: Int): String
    val resolve: String
    val slotSheetLabel: String
    val conflictExplain: String
    val ignoredNotWorn: String
    val moveOthersToCarried: String
    val equipAnother: String
    val equip: String
    val noSlotCandidates: String
    fun sameSlotAs(name: String): String
    val keepThisWorn: String
    fun unequipOther(name: String): String
    val swap: String
    fun selectedSlot(slot: String): String
    val equippedOnly: String
    fun classSpellsHeader(className: String): String
    val noCasterClasses: String
    val maxLevelReached: String

    // rasa
    val raceLabel: String
    val chooseRaceTitle: String
    val searchRaces: String
    val noRace: String
    fun raceGroup(g: RaceGroup): String
    fun raceName(id: String, original: String): String
    val anyAbilityBonus: String
    val raceSizeHint: String
    val raceBonusLabel: String

    // skills
    val skillsHeader: String

    // přesvědčení, božstvo, ruční potvrzení prerekvizit
    val alignmentLabel: String
    fun alignmentName(a: Alignment): String
    val deityLabel: String
    val chooseDeityTitle: String
    fun deityDomainsHint(deity: String): String
    fun deityConflict(deity: String, items: String): String
    val searchDeities: String
    val customDeity: String
    val noDeity: String
    val confirmPrereq: String
    val unconfirmPrereq: String
    val confirmedByHand: String
    val confirmPrereqHint: String
    fun skillName(id: String, original: String): String
    val classSkillShort: String
    fun ranks(n: Int): String
    val showAllSkills: String
    val showTrainedSkills: String
    val noSkillRanks: String
    fun addSubSkill(skill: String): String
    val subSkillLabel: String
    val skillsHint: String
    /** [raceMod] = úprava rasy + společníka */
    fun abilityBaseTitle(name: String, raceMod: Int): String
    fun raceIncludedHint(mods: String): String

    // vlastní katalog
    val myItems: String
    val saveToMyItems: String
    val removeFromMyItems: String
    val savedToMyItems: String
    val myItemsEmpty: String

    // lektvary, svitky, hůlky ze spellu
    val fromSpell: String
    val spellItemTitle: String
    fun spellItemKind(k: SpellItemKind): String
    val spellListLabel: String
    fun spellLevelTooHigh(k: SpellItemKind): String
    val potionNote: String
    val materialCostNote: String
    val pickSpell: String
}

object EnStrings : Strings {
    override val back = "Back"
    override val cancel = "Cancel"
    override val save = "Save"
    override val delete = "Delete"
    override val rename = "Rename"
    override val create = "Create"
    override val add = "Add"
    override val close = "Close"
    override val all = "All"
    override val anyOption = "all"
    override val open = "Open"
    override val menu = "Menu"
    override val decrease = "Decrease"
    override val increase = "Increase"
    override val clearSearch = "Clear search"
    override val closeDetails = "Close details"
    override val loading = "Loading…"
    override val name = "Name"
    override val language = "Language"
    override fun results(n: Int) = if (n == 1) "1 result" else "$n results"
    override fun sourceLine(src: String) = "Source: $src"

    override val loadingSpells = "Loading spells…"
    override fun couldNotLoadSpells(msg: String) = "Could not load spells:\n$msg"
    override val useBundledData = "Use bundled data"
    override val pricesRevealed = "All prices revealed"
    override val pricesHidden = "High prices hidden"

    override val tabSpellbook = "Spellbook"
    override val tabInventory = "Inventory"
    override val spellCompendium = "Spell compendium"
    override val compendium = "Compendium"
    override val searchSpells = "Search spells…"
    override val searchItem = "Search item…"
    override val backToInventory = "Back to inventory"
    override val placeholderNoInventories = "Create an inventory on the left – one per character, or a shared party stash."
    override val placeholderPickInventory = "Pick an inventory on the left, or create a new one."
    override fun placeholderPickCharacter(spellCount: Int) = "Pick a character on the left or create a new one.\nThe compendium has all $spellCount spells."

    override val importCharacter = "Import character"
    override val exportAllCharacters = "Export all characters"
    override val importInventory = "Import inventory"
    override val exportAllInventories = "Export all inventories"
    override val menuDatabase = "Databases & updates"
    override val menuAbout = "About & licenses"

    override val myCharacters = "My characters"
    override val newCharacter = "New character"
    override val noCharactersYet = "No characters yet."
    override fun emptyCharacters(spellCount: Int) =
        "You have no characters yet.\nCreate one with its class and level – only spells it can cast will be offered.\n" +
            "Load an exported character via the menu (top right) → Import character.\n\n$spellCount spells in the database."
    override val characterNameLabel = "Character name"
    override val classLabel = "Class"
    override val characterLevel = "Character level"
    override val levelLabel = "Level"
    override val addSpells = "Add spells"
    override val exportCharacter = "Export character"
    override val deleteCharacter = "Delete character"
    override val deleteCharacterTitle = "Delete character?"
    override fun deleteCharacterText(name: String) = "Character \"$name\" will be permanently deleted."
    override val renameCharacter = "Rename character"
    override fun missingSpells(n: Int) = "$n spells of this character are no longer in the database (e.g. after an update)."
    override val characterNoSpells = "No spells yet. Add them with \"Add spells\"."
    override val notOnClassList = "Not on the class spell list"
    override val notAvailableAtLevel = "Not available at this level"
    override val importedSuffix = "(imported)"

    override fun spells(n: Int) = if (n == 1) "1 spell" else "$n spells"
    override fun copies(n: Int) = if (n == 1) "1 copy" else "$n copies"
    override fun spellLevelLabel(level: Int) = if (level == 0) "Cantrips (0)" else "Level $level"
    override fun slotsPrepared(chosen: Int, limit: String) = "$chosen / $limit prepared"
    override fun slotsKnown(chosen: Int, limit: String) = "$chosen / $limit known"
    override fun slotsPerDay(slots: String) = "$slots per day"
    override val atWill = "at will"
    override fun racialTraitsButton(n: Int) = if (n == 0) "Alternate racial traits" else "Alternate racial traits ($n)"
    override fun racialTraitsTitle(race: String) = "$race – racial traits"
    override val defaultTraitsLabel = "Default traits"
    override val chosenTraitsLabel = "Alternate traits"
    override fun replacesTraits(list: String) = "Replaces: $list"
    override fun traitConflict(list: String) = "Two alternate traits replace the same trait: $list"
    override val noAlternateTraits = "This race has no alternate racial traits."
    override val racialTraitsHint = "Replaced default traits stop counting for feat prerequisites, the human bonus feat and skill points."
    override val sourceRacialTraits = "Default and alternate racial traits"
    override fun archetypesButton(n: Int) = if (n == 0) "Archetypes" else "Archetypes ($n)"
    override fun archetypesTitle(cls: String) = "$cls – archetypes"
    override val chosenArchetypesLabel = "Chosen archetypes"
    override val archetypesHint = "Replaced class features disappear (limited-use abilities, subclass, hexes, bonus feats); known archetypes also change spellcasting and add abilities. Everything else is only a record – check the text."
    override fun archetypeConflict(list: String) = "Two archetypes replace the same class feature: $list"
    override val noArchetypes = "This class has no archetypes."
    override val sourceArchetypes = "Class archetypes"
    override fun prestigeTag(maxLevel: Int) = "prestige · max $maxLevel"
    override fun prestigeKind(kind: String) = when (kind) {
        "arcane" -> "arcane"; "divine" -> "divine"; "psychic" -> "psychic"; else -> "any"
    }
    override fun prestigeAdvancesTag(kinds: String) = "+ spellcasting ($kinds)"
    override fun advanceLabel(kindName: String) = "Spellcasting ($kindName)"
    override val advanceNone = "no class"
    override fun advanceSummary(cls: String, n: Int) = "+$n caster level${if (n == 1) "" else "s"}: $cls"
    override fun casterLevelInfo(n: Int) = "caster level $n"
    override val missingAdvance = "Choose which spellcasting class the prestige class advances."
    override val prestigeDetails = "Requirements and features"
    override val prestigeRequirementsHeader = "Requirements"
    override val prestigeTableHeader = "Class table"
    override val prestigeFeaturesHeader = "Class features"
    override val prestigeHint = "BAB, saves, hit dice, skills, bonus feats and spellcasting are counted; other class features are text only."
    override val prestigeColumns = listOf("Lvl", "BAB", "Fort", "Ref", "Will", "Spells")
    override val prestigeUnmet = "Requirements not met"
    override val sourcePrestige = "Prestige classes"
    override fun fxRemoves(list: String) = "Removes: $list"
    override fun fxGains(list: String) = "Gains: $list"
    override fun fxChanges(list: String) = "Changes: $list"
    override val fxNoSpells = "no spellcasting"
    override val fxNoCantrips = "no cantrips"
    override val fxNoSubclass = "no subclass"
    override val fxNoSubclassSpells = "no subclass bonus spells"
    override val fxNoSubclassPowers = "no subclass powers"
    override fun fxLostAbilities(n: Int) = "−$n hexes / revelations"
    override fun fxLostFeats(n: Int) = if (n == 1) "−1 bonus feat" else "−$n bonus feats"
    override fun fxGainedFeats(n: Int) = if (n == 1) "+1 bonus feat" else "+$n bonus feats"
    override fun fxPerDay(delta: Int) = "${if (delta < 0) "−${-delta}" else "+$delta"} spell per day of each level"
    override fun fxKnown(delta: Int) = "${if (delta < 0) "−${-delta}" else "+$delta"} spell known of each level"
    override val fxOneDomain = "one domain only"
    override val secondBloodline = "Second bloodline"
    override fun fxSpellList(list: String) = "Spellcasting: $list"
    override fun fxAddedSpells(n: Int) = if (n == 1) "+1 spell on the list" else "+$n spells on the list"
    override fun fxAsClass(cls: String) = "slots as $cls"
    override fun fxSubclassLevel(delta: Int) = "subclass powers at class level ${if (delta < 0) "−${-delta}" else "+$delta"}"
    override val crossbloodedHint = "Crossblooded: bonus spells, powers and bonus feats of both bloodlines are offered – at each level you take one of them."
    override fun abilityChoiceLabel(amount: Int) = (if (amount < 0) "−${-amount}" else "+$amount") + " to"
    override fun missingAbilityChoice(trait: String) = "Choose an ability score for $trait"
    override fun abilityGroupName(g: AbilityGroup) = when (g) {
        AbilityGroup.HEXES -> "Hexes"
        AbilityGroup.REVELATIONS -> "Revelations"
    }
    override fun addClassAbility(g: AbilityGroup) = when (g) {
        AbilityGroup.HEXES -> "Add hex"
        AbilityGroup.REVELATIONS -> "Add revelation"
    }
    override fun noClassAbilities(g: AbilityGroup) = when (g) {
        AbilityGroup.HEXES -> "No hexes chosen yet."
        AbilityGroup.REVELATIONS -> "No revelations chosen yet."
    }
    override fun abilityKind(k: AbilityKind) = when (k) {
        AbilityKind.HEX -> "Hex"
        AbilityKind.MAJOR_HEX -> "Major hex"
        AbilityKind.GRAND_HEX -> "Grand hex"
        AbilityKind.REVELATION -> "Revelation"
    }
    override fun fromClassLevel(n: Int) = "from level $n"
    override val allMysteries = "All mysteries"
    override val allSpirits = "Hexes of all spirits"
    override val removeAbility = "Remove"
    override val searchAbilities = "Search by name or text"
    override val loadingAbilities = "Loading…"
    override fun abilitiesHint(g: AbilityGroup) = when (g) {
        AbilityGroup.HEXES -> "The count follows the class level plus each Extra Hex feat. A shaman can also pick the hexes of her spirit."
        AbilityGroup.REVELATIONS -> "The count follows the class level plus each Extra Revelation feat; the final revelation comes at level 20."
    }
    override val sourceClassAbilities = "Domain, bloodline and school powers, hexes, revelations"
    override val sourceSubclassAbilities = "Spirit, bloodrager bloodline, discipline and inquisition powers, spirit hexes"
    override val featsHeader = "Feats"
    override fun featCount(have: Int, total: Int) = "$have / $total"
    override fun featSlotsGeneral(n: Int) = "Levels $n"
    override val extraFeats = "Extra"
    override val addFeat = "Add feat"
    override val customFeat = "Custom feat"
    override val featName = "Name"
    override val featText = "Description"
    override val featNote = "Note (e.g. chosen weapon)"
    override val noFeats = "No feats yet."
    override val removeFeat = "Remove feat"
    override val prerequisites = "Prerequisites"
    override fun prereqStatus(st: PrereqStatus) = when (st) {
        PrereqStatus.MET -> "met"
        PrereqStatus.UNMET -> "not met"
        PrereqStatus.UNKNOWN -> "check manually"
    }
    override val prereqUnmet = "Prerequisites not met"
    override val onlyMet = "Only feats I qualify for"
    override val featType = "Type"
    override val generalFeat = "General"
    override val searchFeats = "Search feats…"
    override fun featsInDatabase(n: Int) = "$n feats"
    override val addFeatToCharacter = "Add to character"
    override fun featTaken(n: Int) = "Taken ×$n"
    override val customBadge = "custom"
    override val loadingFeats = "Loading feats…"
    override fun featsFailed(msg: String) = "Feats failed to load: $msg"
    override val editFeat = "Edit feat"
    override val inventoryHeader = "Inventory"
    override val resourcesHeader = "Limited-use abilities"
    override val addTracker = "Add counter"
    override val editTracker = "Edit counter"
    override val removeTracker = "Remove counter"
    override val trackerName = "Name"
    override val trackerMax = "Maximum"
    override val trackerUnit = "Unit"
    override val trackerNote = "Note"
    override fun unitName(u: ResourceUnit) = when (u) {
        ResourceUnit.PER_DAY -> "uses per day"
        ResourceUnit.ROUNDS -> "rounds per day"
        ResourceUnit.POINTS -> "points"
    }
    override fun resourceAmount(n: Int?, u: ResourceUnit) = when {
        n == null -> "unlimited"
        u == ResourceUnit.PER_DAY -> "$n× per day"
        u == ResourceUnit.ROUNDS -> (if (n == 1) "1 round" else "$n rounds") + " per day"
        else -> if (n == 1) "1 point" else "$n points"
    }
    override fun remaining(left: Int, max: Int) = "$left / $max left"
    override fun showHidden(n: Int) = "Show hidden ($n)"
    override val hideFeature = "Hide"
    override val noResources = "No limited-use abilities – add a counter for e.g. a racial ability or a magic item."
    override fun subclassTitle(className: String) = "$className – subclass"
    override fun subclassLabel(t: SubclassType) = when (t) {
        SubclassType.TWO_DOMAINS -> "Domains"
        SubclassType.ONE_DOMAIN -> "Domain"
        SubclassType.MYSTERY -> "Mystery"
        SubclassType.BLOODLINE -> "Bloodline"
        SubclassType.SCHOOL -> "Arcane school"
        SubclassType.PATRON -> "Patron"
        SubclassType.SPIRIT -> "Spirit"
        SubclassType.BLOODRAGER_BLOODLINE -> "Bloodline"
        SubclassType.DISCIPLINE -> "Discipline"
        SubclassType.BLESSINGS -> "Blessings"
        SubclassType.INQUISITOR_DOMAIN -> "Domain"
    }
    override fun domainN(n: Int) = "Domain $n"
    override val subdomain = "Subdomain"
    override fun oppositionN(n: Int) = "Opposition school $n"
    override val universalist = "Universalist"
    override fun elementalSchool(element: String) = "Elemental – $element"
    override fun element(name: String) = name.replaceFirstChar { it.uppercase() }
    override val noneChosen = "none"
    override val chooseSubclass = "choose…"
    override val subclassLaterHint = "Optional – you can also set or change it later in the character detail."
    override val subdomainHint = "A subdomain replaces some spells and a power of its domain – only subdomains of the chosen domain are offered."
    override val druidDomainHint = "Nature bond: a druid picks one of the nature domains (Air, Animal, Earth, Fire, Plant, Water, Weather), optionally one of its subdomains (not Metal)."
    override val schoolHint = "Specialists get one extra slot per spell level for spells of their school; preparing an opposition-school spell takes two slots."
    override fun bonusSpellsHeader(t: SubclassType) = when (t) {
        SubclassType.TWO_DOMAINS, SubclassType.ONE_DOMAIN -> "Domain spells"
        SubclassType.MYSTERY -> "Mystery spells"
        SubclassType.BLOODLINE -> "Bloodline spells"
        SubclassType.SCHOOL -> "School spells"
        SubclassType.PATRON -> "Patron spells"
        SubclassType.SPIRIT -> "Spirit magic"
        SubclassType.BLOODRAGER_BLOODLINE -> "Bloodline spells"
        SubclassType.DISCIPLINE -> "Discipline spells"
        SubclassType.BLESSINGS, SubclassType.INQUISITOR_DOMAIN -> "Subclass powers"
    }
    override fun domainSlot(chosen: Int) = "$chosen / 1 domain slot"
    override fun knownFromLevel(n: Int) = "from class level $n"
    override val knownAutomatically = "known automatically"
    override val schoolSlot = "+1 school"
    override val oppositionWarn = "Opposition school – takes 2 slots"
    override val oppositionShort = "opposition"
    override val subclassNotAvailable = "Not yet available at this level."
    override fun blessingN(n: Int) = "Blessing $n"
    override val blessingHint = "Two blessings from the domains of the warpriest's deity: the minor power at 1st level, the major at 10th. All blessings share one daily pool (Blessings, 3 + ½ level)."
    override fun deityBlessingsHint(deity: String) = "Only blessings of the domains granted by $deity are offered (without a deity, or with a custom one, all blessings)."
    override val inquisitorDomainHint = "An inquisitor picks one domain (or subdomain) of the deity, or an inquisition instead – she gains its powers, but no domain spells or domain slot."
    override val inquisitionLabel = "Inquisition"
    override fun inquisitionName(name: String) = "$name Inquisition"
    override val patronPrepareHint = "Prepare them in normal slots with the stepper – they then appear among the witch's spells."
    override val wanderingSpirit = "Wandering spirit"
    override val wanderingSpiritHint = "From 4th level the shaman bonds daily with a second spirit: she gains its spirit ability (greater at 12th, true at 20th) and its spirit magic spells; from 6th level one of its hexes (two from 14th) as a wandering hex."
    override fun bloodlineBonusFeats(feats: String) = "Bloodline bonus feats: $feats"
    override val patronHint = "Patron spells are added to the familiar at every even level (2nd, 4th…); the witch prepares them in her normal slots."
    override val patronSpellsNote = "added to the familiar, prepared in normal slots"
    override val spiritHint = "Spirit magic: the shaman has one extra slot per spell level she can cast and spontaneously casts any spirit magic spell of that level from it."
    override val spiritSlot = "1 spirit magic slot / day"
    override val bloodragerBloodlineHint = "Bonus spells known at 7th, 10th, 13th and 16th level."
    override val disciplineHint = "Bonus spells known at 1st level and then at every even level."
    override val subclassPowersHeader = "Subclass powers"
    override val includeInventory = "Include linked inventory"
    override fun withInventory(name: String) = "+ inventory $name"
    override val exportFeats = "Export"
    override val importFeats = "Import"
    override fun exportFeatsTitle(n: Int) = if (n == 1) "Export 1 feat" else "Export $n feats"
    override val exportFeatsHint = "The text below contains the character's feats (incl. notes and custom feats). Import them into another character via \"Import\" in its Feats section."
    override fun importFeatsTitle(character: String) = "Import feats into $character"
    override fun featsImportSummary(n: Int, skipped: Int) =
        (if (n == 1) "1 feat" else "$n feats") + if (skipped > 0) " ($skipped already taken – skipped)" else ""
    override val linkInventory = "Link inventory"
    override val chooseOption = "choose…"
    override val newInventoryForCharacter = "New inventory"
    override val openInventory = "Open"
    override val unlinkInventory = "Unlink inventory"
    override fun strengthFromCharacter(name: String) = "linked to character $name"
    override fun inventorySummary(name: String, weight: String, load: String) = "$name · $weight · $load"
    override val featAlreadyTaken = "Already taken"
    override val gameMode = "Game mode"
    override val rest = "Rest"
    override val tabCharacter = "Character"
    override val tabCombat = "Combat"
    override val tabCompanions = "Companions"
    override val tabSpells = "Spells"
    override val sectionsLabel = "Sections"
    override val modePrepare = "Prepare"
    override val modePlay = "Play"
    override val modeHintPrepare = "prepare for tomorrow"
    override val modeHintPlay = "tap a circle = cast"
    override val levelShort = "LVL"
    override val babShort = "BAB"
    override val fortShort = "FORT"
    override val refShort = "REF"
    override val willShort = "WILL"
    override val mainAbilityNote = "main!"
    override val stampOverLimit = "OVER LIMIT"
    override val stampUnconscious = "UNCONSCIOUS"
    override val stampOverloaded = "OVERLOADED"
    override val restDone = "slots restored"
    override val emptySpellsTitle = "An empty spellbook"
    override val emptyInventoryTitle = "Your pack is empty"
    override val emptyCompanionsTitle = "No companions yet"
    override val emptyNoCasterTitle = "No spellcasting here"
    override val savedOffline = "saved offline"
    override val hpHandHint = "tap − to record damage, + to heal"
    override fun freeSlots(n: Int) = "+ $n free ${if (n == 1) "slot" else "slots"} – prepare a spell"
    override val restedToast = "Rested – spells, slots and abilities are back, 1 HP per level healed."
    override val castSpell = "Cast"
    override fun castOf(used: Int, total: Int) = "$used / $total cast"
    override val slotLabel = "Spell slot"
    override val removeSpell = "Remove spell"
    override fun abilityNeeded(ability: String, score: Int) = "needs $ability $score"
    override fun castingAbility(ability: String, score: Int, mod: String) = "Casting: $ability $score ($mod)"
    override val noSpellsYet = "no spells yet"
    override fun spellLevelSingle(level: Int) = "spell level $level"
    override fun spellLevelRange(min: Int, max: Int) = "spell levels $min–$max"
    override fun unknownClassLevel(level: Int) = "Unknown class · level $level"
    override fun addTo(deckName: String) = "Add to: $deckName"
    override fun spellsInDatabase(n: Int) = "$n spells in the database"
    override val school = "School"
    override val searchDescriptions = "search descriptions too"
    override val hideAdded = "hide added"
    override fun cannotCastYet(className: String?, level: Int) =
        "${className ?: "This class"} cannot cast spells at level $level yet (from level 4)."
    override val addToCharacter = "Add to spellbook"
    override fun addCopy(inDeck: Int) = "Add a copy ($inDeck in spellbook)"
    override fun addToCharacterCount(inDeck: Int) = "Add to spellbook (×$inDeck)"
    override fun cannotCastThis(className: String, level: Int) = "$className $level cannot cast this spell."
    override val castingTime = "Casting Time"
    override val components = "Components"
    override val range = "Range"
    override val effect = "Effect"
    override val target = "Target"
    override val duration = "Duration"
    override val savingThrow = "Saving Throw"
    override val spellResistance = "Spell Resistance"
    override val description = "Description"
    override val mythic = "Mythic"
    override val openOnAon = "Open on Archives of Nethys"

    override fun exportCharactersTitle(n: Int) = if (n == 1) "Export character" else "Export $n characters"
    override val exportCharactersHint = "The text below contains the character(s) with classes, abilities, spells and feats. Save or send it – load it on another device via \"Import character\"."
    override fun notFoundSkipped(names: String) = "Not found in the database (will be skipped): $names"
    override fun exportInventoriesTitle(n: Int) = if (n == 1) "Export inventory" else "Export $n inventories"
    override val exportInventoriesHint = "The text below contains the inventory with coins, items and notes. Load it on another device via \"Import inventory\"."
    override val copy = "Copy"
    override val copiedToClipboard = "Copied to clipboard."
    override val done = "Done."
    override fun failed(msg: String?) = "Failed: $msg"
    override val pasteFromClipboard = "Paste from clipboard"
    override val openFile = "Open file"
    override fun cannotReadFile(msg: String?) = "Cannot read the file: $msg"
    override val exportText = "Export text"
    override fun importButton(n: Int) = if (n > 1) "Import ($n)" else "Import"
    override fun saveAction(a: SaveAction) = when (a) {
        SaveAction.SAVE_FILE -> "Save to file"
        SaveAction.SHARE -> "Share"
        SaveAction.DOWNLOAD -> "Download file"
    }
    override fun importError(e: ImportError) = when (e) {
        ImportError.Empty -> "Paste an export or choose a file."
        is ImportError.Invalid -> "This is not a valid ${if (e.inventory) "inventory" else "character"} export (${e.detail ?: "invalid JSON"})."
        is ImportError.UnknownFormat -> "Unknown format \"${e.format}\"."
        is ImportError.NewerVersion -> "The export comes from a newer app version (version ${e.version})."
        ImportError.NoDecks -> "The export contains no characters."
        ImportError.NoInventories -> "The export contains no inventories."
        ImportError.NoFeats -> "The export contains no feats."
        is ImportError.UnknownClass -> "Character \"${e.deck}\" has an unknown class \"${e.classId}\"."
    }

    override val myInventories = "My inventories"
    override val newInventory = "New inventory"
    override val defaultInventoryName = "Inventory"
    override val inventoryNameLabel = "Name (character / stash)"
    override val noInventoriesYet = "No inventories yet.\nCreate one for each character (or a shared party stash) and add items from the PF1 item database."
    override fun itemTypes(n: Int) = if (n == 1) "1 item type" else "$n item types"
    override fun worth(value: String) = "worth ≈ $value"
    override val purse = "Purse"
    override val load = "Load"
    override val categories = "Categories"
    override fun category(c: ItemCategory) = c.label
    override val totalWeight = "Total weight"
    override fun loadName(l: Load) = when (l) {
        Load.LIGHT -> "Light load"
        Load.MEDIUM -> "Medium load"
        Load.HEAVY -> "Heavy load"
        Load.OVERLOADED -> "Overloaded"
    }
    override fun loadLimits(light: String, medium: String, heavy: String) = "light ≤ $light, medium ≤ $medium, heavy ≤ $heavy"
    override fun coinsWeight(w: String) = "coins $w"
    override val strength = "Strength"
    override fun editCoin(coin: String) = "Edit $coin"
    override fun coinsTitle(coin: String) = "$coin – coins"
    override val addItem = "Add item"
    override val exportInventory = "Export inventory"
    override val deleteInventory = "Delete inventory"
    override val deleteInventoryTitle = "Delete inventory?"
    override fun deleteInventoryText(name: String) = "Inventory \"$name\" will be permanently deleted."
    override val renameInventory = "Rename inventory"
    override val inventoryEmpty = "This inventory is empty. Add items from the database with \"Add item\"."
    override val notes = "Notes"
    override val notesHint = "Free notes (keys, debts, stash locations…)"
    override val colItem = "Item"
    override val colQty = "Qty"
    override val colWeight = "Weight"
    override val colPrice = "Price"
    override val colNotes = "Notes"
    override val colName = "Name"
    override val colType = "Type"
    override val colDmg = "Dmg"
    override val colCrit = "Crit"

    override fun couldNotLoadItems(msg: String) = "Could not load items:\n$msg"
    override val loadingItems = "Loading item database…"
    override fun itemPickerSubtitle(items: Int, invName: String) = "PF1 item database · $items items · to $invName"
    override val customItem = "Custom item"
    override fun searchItems(n: Int) = "Search $n items…"
    override fun proficiency(p: String) = p
    override fun upTo(price: String) = "up to $price"
    override val maxPrice = "max"
    override val anyPrice = "any"
    override fun owned(n: Int) = "$n owned"
    override fun addNamed(name: String) = "Add $name"
    override val itemFallback = "Item"
    override val cost = "Cost"
    override val weight = "Weight"
    override val damageS = "Damage (S)"
    override val damageM = "Damage (M)"
    override val critical = "Critical"
    override val type = "Type"
    override val special = "Special"
    override val armorBonus = "Armor bonus"
    override val maxDex = "Max Dex"
    override val checkPenalty = "Check penalty"
    override val spellFailure = "Spell failure"
    override val aura = "Aura"
    override val casterLevel = "Caster level"
    override val slot = "Slot"
    override val requirements = "Requirements"
    override val info = "Info"
    override fun youOwn(n: Int) = "You own $n"
    override val inInventory = "In inventory"
    override val quantity = "Quantity"
    override val noteOptional = "Note (optional)"
    override val notePlaceholder = "e.g. masterwork, found in crypt"
    override val editItem = "Edit item"
    override val removeFromInventory = "Remove from inventory"
    override val addToInventory = "Add to inventory"
    override val itemNamePlaceholder = "e.g. Scroll of Mage Armor"
    override val categoryLabel = "Category"
    override val priceGp = "Price (gp)"
    override val weightLb = "Weight (lb)"
    override val shortInfo = "Short info (optional)"
    override val shortInfoPlaceholder = "e.g. 1d8+1, CL 1"

    override val databases = "Databases & updates"
    override val spellsHeader = "Spells"
    override val itemsHeader = "Items"
    override val bundledVersion = "Bundled version"
    override val downloadedVersion = "Downloaded version"
    override fun downloadedAt(date: String) = "Downloaded $date"
    override val spellsDbInfo = "Spells come from Archives of Nethys (Paizo, Pathfinder 1e) via the open SpellTable-pf1e project. " +
        "Pathfinder 1e is finished, so new spells practically never appear – an update mostly brings typo fixes."
    override val itemsDbInfo = "Weapons, ammunition and magic items come from the d20pfsrd tables and can be updated here. " +
        "Mundane gear, armor and alchemical items (from the Foundry VTT PF1 system) are bundled with the app – core PF1 equipment does not change."
    override val downloading = "Downloading…"
    override val downloadLatestSpells = "Download latest spell database"
    override val downloadLatestItems = "Download latest item database"
    override val restoreBundled = "Restore bundled version"
    override val charactersKept = "Characters are kept – spells are matched by id and name."
    override val inventoriesKept = "Inventories are kept – entries store their own name, price and weight."
    override val appUpdatesHeader = "App updates"
    override fun installedVersion(v: String) = "Installed version $v"
    override val devBuild = "development build – the automatic check is off"
    override val checkForUpdates = "Check for updates"
    override val checkingUpdates = "Checking…"
    override val upToDate = "You have the latest version."
    override fun updateAvailable(v: String) = "Version $v is available"
    override val downloadAndInstall = "Download and install"
    override val releaseNotes = "What's new"
    override fun updateDownloading(percent: Int?) = "Downloading…" + (percent?.let { " $it %" } ?: "")
    override fun updateFailed(msg: String) = "Update failed: $msg"
    override val updateHintApk = "The APK is downloaded and the system installer opens – allow installing from this app the first time. Characters and inventories are kept."
    override val updateHintInstaller = "The app downloads the installer to your Downloads folder, starts it and closes itself so it can be replaced. Characters and inventories are kept."
    override val updateLater = "Later"
    override val updateSkip = "Skip this version"
    override fun updateConfirmTitle(v: String) = "Install version $v?"
    override val updateBackupHint = "The update keeps your characters and inventories. To be safe, export them first and save the file outside the app."
    override val updateSignatureMismatch = "This version is signed with a different key than the installed app, so Android won't install it over it. " +
        "The old version has to be uninstalled – and that deletes all characters and inventories!\n\n" +
        "1. Export characters and inventories and save the files outside the app (e.g. Downloads, Drive).\n" +
        "2. Download the APK in the browser (it stays in Downloads).\n" +
        "3. Uninstall PF Spellbook and install the downloaded APK.\n" +
        "4. Import the characters and inventories again (in the main menu choose Import)."
    override val installNow = "Install"
    override val installAndClose = "Install and close the app"
    override val downloadApkInBrowser = "Download APK in browser"
    override val itemDbFailed = "Item database failed to load."
    override fun itemsCount(n: Int) = "$n items"
    override fun downloadedSpells(n: Int) = "Downloaded $n spells."
    override fun downloadedItems(n: Int) = "Downloaded $n items."
    override fun downloadFailed(msg: String?) = "Download failed: $msg"
    override val bundledSpellsRestored = "Bundled database restored."
    override val bundledItemsRestored = "Bundled item database restored."

    override fun className(id: String, original: String) = original
    override fun classOption(id: String, original: String) = original
    override fun spellListName(key: String) = key.replaceFirstChar { it.uppercase() }
    override fun school(name: String) = name.replaceFirstChar { it.uppercase() }
    override fun subschool(name: String) = name

    override val sortLabel = "Sort"
    override fun spellSort(s: SpellSort) = when (s) {
        SpellSort.NAME -> "name"
        SpellSort.LEVEL -> "level"
        SpellSort.SCHOOL -> "school"
    }
    override val pfsOnly = "PFS legal only"
    override val notPfsLegal = "Not PFS legal"
    override val favoritesOnly = "favorites only"
    override val favorite = "Favorite"
    override val addFavorite = "Add to favorites"
    override val removeFavorite = "Remove from favorites"

    override fun loadPenalties(l: Load) = when (l) {
        Load.LIGHT -> null
        Load.MEDIUM, Load.HEAVY ->
            "Max Dex +${l.maxDex} · check penalty ${l.checkPenalty.signed()} · speed 30 → 20 ft, 20 → 15 ft · run ×${l.runMultiplier}"
        Load.OVERLOADED -> "Can only stagger 5 ft per round (full-round action) and loses Dex bonus to AC"
    }
    override fun loadIncreased(l: Load) = "Load increased: ${loadName(l)}"
    override val sizeLabel = "Size"
    override fun creatureSize(c: CreatureSize) = c.name.lowercase().replaceFirstChar { it.uppercase() }
    override val quadruped = "Four legs"

    override val classesHeader = "Classes"
    override val addClass = "Add class"
    override val removeClass = "Remove class"
    override fun removeClassText(className: String) = "Remove $className and its spells from the character?"
    override val abilitiesHeader = "Abilities"
    override fun abilityShort(a: Ability) = a.name
    override fun abilityLong(a: Ability) = when (a) {
        Ability.STR -> "Strength"
        Ability.DEX -> "Dexterity"
        Ability.CON -> "Constitution"
        Ability.INT -> "Intelligence"
        Ability.WIS -> "Wisdom"
        Ability.CHA -> "Charisma"
    }
    override fun statsLine(st: CharacterStats) =
        "Level ${st.level} · BAB ${st.bab.signedPlus()} · Fort ${st.fort.signedPlus()}, Ref ${st.ref.signedPlus()}, Will ${st.will.signedPlus()}"
    override val chooseClass = "Choose a class"
    override val searchClasses = "Search classes…"
    override fun classGroup(g: ClassGroup) = when (g) {
        ClassGroup.FULL -> "9 spell levels"
        ClassGroup.SIX -> "6 spell levels"
        ClassGroup.FOUR -> "4 spell levels"
        ClassGroup.NONE -> "No spells"
        ClassGroup.PRESTIGE -> "Prestige classes"
        ClassGroup.NPC -> "NPC classes"
        ClassGroup.CREATURE -> "Creatures (racial hit dice)"
    }
    override fun magicTradition(t: MagicTradition) = when (t) {
        MagicTradition.ARCANE -> "arcane"
        MagicTradition.DIVINE -> "divine"
        MagicTradition.PSYCHIC -> "psychic"
        MagicTradition.ALCHEMICAL -> "extracts"
    }
    override fun castingKind(k: CastingKind) = when (k) {
        CastingKind.PREPARED -> "prepared"
        CastingKind.SPONTANEOUS -> "spontaneous"
        CastingKind.HYBRID -> "prepared + spontaneous"
    }
    override val strictMode = "Strict mode"
    override val strictModeHint = "Spells only up to the per-level limit, feats only into free slots and with met prerequisites."
    override fun strictBlocked(b: StrictBlock) = "Strict mode: " + when (b) {
        is StrictBlock.SpellLevelFull -> "spell level ${b.spellLevel} is full (${b.limit})"
        is StrictBlock.AbilityTooLow -> "casting ability is too low for spell level ${b.spellLevel}"
        StrictBlock.SpellNotAvailable -> "the class can't cast this spell yet"
        is StrictBlock.FeatSlotsFull -> "all ${b.total} feats are taken"
        StrictBlock.FeatPrereqsUnmet -> "prerequisites are not met"
        is StrictBlock.ClassAbilitySlotsFull -> "all ${b.total} choices at this level are taken"
        is StrictBlock.ClassAbilityLevel -> "requires class level ${b.minLevel}"
        is StrictBlock.RacialTraitReplaced -> "\"${b.trait}\" is already replaced by another alternate trait"
        is StrictBlock.ArchetypeConflict -> "\"${b.feature}\" is already replaced by another archetype"
        is StrictBlock.PrestigeRequirementUnmet -> "prestige class requirement not met – ${b.requirement}"
    }
    override val companionsHeader = "Companions"
    override val companionsHint = "Animal companion, mount, familiar, eidolon or a pet – each with its own sheet, feats, skills and inventory."
    override val addCompanion = "Add companion"
    override val newCompanion = "New companion"
    override val companionSettings = "Companion settings"
    override val companionKindLabel = "Kind"
    override fun companionKind(k: CompanionKind) = when (k) {
        CompanionKind.ANIMAL_COMPANION -> "Animal companion"
        CompanionKind.MOUNT -> "Mount"
        CompanionKind.FAMILIAR -> "Familiar"
        CompanionKind.EIDOLON -> "Eidolon"
        CompanionKind.PET -> "Pet"
    }
    override val creatureLabel = "Creature type"
    override val hitDiceLabel = "Hit dice"
    override val sizeByRace = "by race"
    override fun companionOf(kind: String, owner: String) = "$kind of $owner"
    override val speciesLabel = "Animal"
    override val speciesCustom = "Custom"
    override fun speciesName(sp: AnimalSpecies) = sp.name
    override val companionProgression = "Animal companion progression"
    override val companionLevelLabel = "Effective druid level"
    override val familiarLabel = "Familiar"
    override val familiarLevelLabel = "Master's class level"
    override val masterBonusLabel = "Master's bonus"
    override val masterBonusConditional = "conditional – not counted automatically"
    override val familiarNoteLabel = "Statistics"
    override fun familiarSpecial(sp: cz.pfspells.data.FamiliarSpecial) = when (sp) {
        cz.pfspells.data.FamiliarSpecial.ALERTNESS -> "alertness (master)"
        cz.pfspells.data.FamiliarSpecial.IMPROVED_EVASION -> "improved evasion"
        cz.pfspells.data.FamiliarSpecial.SHARE_SPELLS -> "share spells"
        cz.pfspells.data.FamiliarSpecial.EMPATHIC_LINK -> "empathic link"
        cz.pfspells.data.FamiliarSpecial.DELIVER_TOUCH_SPELLS -> "deliver touch spells"
        cz.pfspells.data.FamiliarSpecial.SPEAK_WITH_MASTER -> "speak with master"
        cz.pfspells.data.FamiliarSpecial.SPEAK_WITH_ANIMALS -> "speak with animals of its kind"
        cz.pfspells.data.FamiliarSpecial.SPELL_RESISTANCE -> "spell resistance"
        cz.pfspells.data.FamiliarSpecial.SCRY_ON_FAMILIAR -> "scry on familiar"
    }
    override fun familiarStats(naturalArmor: Int, int: Int, sr: Int?) =
        listOfNotNull("natural armor +$naturalArmor", "Int $int", sr?.let { "SR $it" }).joinToString(" · ")
    override val eidolonFormLabel = "Base form"
    override val specialsLabel = "Special"
    override val eidolonSubtypeLabel = "Subtype"
    override val summonerLevelLabel = "Summoner level"
    override fun eidolonStats(hd: Int, naturalArmor: Int?, strDex: Int, maxAttacks: Int) =
        listOfNotNull("HD $hd", naturalArmor?.let { "natural armor +$it" }, "Str/Dex +$strDex", "max attacks $maxAttacks").joinToString(" · ")
    override fun evolutionPool(used: Int, total: Int?) = "Evolution points: $used" + (total?.let { " / $it" } ?: "")
    override val evolutionsOver = "More points spent than the evolution pool allows."
    override val evolutionsHeader = "Evolutions"
    override val addEvolution = "Add evolution"
    override fun evolutionCost(n: Int) = "$n pt"
    override val freeEvolutionsLabel = "Free evolutions"
    override val eidolonHint = "Ability Increase (+2) and Improved Natural Armor (+2) are counted, Large changes the size; other evolutions are text only."
    override val familiarHint = "The master's bonus counts on the master's sheet (skills, saves, AC, HP). HD = master's level; the familiar uses the master's BAB and base saves if better. Ability scores, size, speed and attacks come from the Bestiary when you pick the kind."
    override fun companionSpecial(sp: CompanionSpecial) = when (sp) {
        CompanionSpecial.LINK -> "Link"
        CompanionSpecial.SHARE_SPELLS -> "Share spells"
        CompanionSpecial.EVASION -> "Evasion"
        CompanionSpecial.ABILITY_INCREASE -> "Ability score increase"
        CompanionSpecial.DEVOTION -> "Devotion"
        CompanionSpecial.MULTIATTACK -> "Multiattack"
        CompanionSpecial.IMPROVED_EVASION -> "Improved evasion"
    }
    override fun companionStats(naturalArmor: Int?, strDex: Int?, tricks: Int?) = listOfNotNull(
        naturalArmor?.let { "Natural armor +$it" },
        strDex?.let { "Str/Dex +$it" },
        tricks?.let { "Bonus tricks $it" },
    ).joinToString(" · ")
    override fun abilityIncreases(n: Int) = "+1 to an ability score ×$n (add it to the base score yourself)"
    override val speedLabel = "Speed"
    override val attacksLabel = "Attacks"
    override val qualitiesLabel = "Special"
    override fun advancementAt(level: Int) = "advancement at level $level"
    override fun companionIncludedHint(mods: String) = "Includes companion bonuses: $mods."
    override val rarityLabel = "Rarity"
    override fun rarity(r: CompanionRarity) = when (r) {
        CompanionRarity.COMMON -> "Common"
        CompanionRarity.UNCOMMON -> "Uncommon"
        CompanionRarity.RARE -> "Rare"
        CompanionRarity.UNIQUE -> "Unique"
    }
    override val respawnLabel = "If it dies (return / replacement)"
    override fun defaultRespawn(k: CompanionKind) = when (k) {
        CompanionKind.ANIMAL_COMPANION ->
            "If the animal companion dies, a new one can be gained through a ceremony requiring 24 uninterrupted hours " +
                "of prayer in the environment where the new companion typically lives."
        CompanionKind.MOUNT ->
            "Paladin (divine bond): another mount can't be summoned for 30 days or until the paladin gains a level, " +
                "whichever comes first; meanwhile the paladin takes a −1 penalty on attack and weapon damage rolls. " +
                "Cavalier: a new mount can be found after 1 week of mourning."
        CompanionKind.FAMILIAR ->
            "If the familiar dies or is dismissed, it can't be replaced for 1 week; then a ritual costing 200 gp per " +
                "wizard level and taking 8 hours brings a new one."
        CompanionKind.EIDOLON ->
            "If the eidolon is sent back to its home plane by death, it can't be summoned again until the following day."
        CompanionKind.PET -> "No special rules – write your own."
    }
    override val resetToDefault = "Default text"
    override val equipmentHeader = "Equipment"
    override val equippedLabel = "Equipped"
    override val equipHint = "Bonuses of equipped items count for the character linked to this inventory (same-type bonuses don't stack – the highest applies)."
    override fun itemSlot(sl: ItemSlot) = when (sl) {
        ItemSlot.MAIN_HAND -> "Main hand"
        ItemSlot.SHIELD -> "Off hand"
        else -> sl.name.lowercase().replaceFirstChar { it.uppercase() }
    }
    override val noSlot = "No slot"
    override val bonusesLabel = "Bonuses"
    override val addBonus = "Add bonus"
    override val bonusTargetLabel = "Applies to"
    override fun bonusTarget(t: BonusTarget) = when (t) {
        BonusTarget.STR -> "Strength"
        BonusTarget.DEX -> "Dexterity"
        BonusTarget.CON -> "Constitution"
        BonusTarget.INT -> "Intelligence"
        BonusTarget.WIS -> "Wisdom"
        BonusTarget.CHA -> "Charisma"
        BonusTarget.FORT -> "Fortitude"
        BonusTarget.REF -> "Reflex"
        BonusTarget.WILL -> "Will"
        BonusTarget.AC -> "AC"
        BonusTarget.SKILL -> "Skill"
        BonusTarget.MAX_DEX -> "Max Dex"
        BonusTarget.ARMOR_CHECK -> "Armor check penalty"
        BonusTarget.HP -> "Hit points"
    }
    override val bonusTypeLabel = "Bonus type"
    override fun bonusType(t: BonusType) = t.name.lowercase().replace('_', ' ')
    override val bonusValueLabel = "Value"
    override val bonusSkillLabel = "Skill"
    override fun slotConflict(slots: String) = "Too many items equipped in: $slots"
    override fun gearIncludedHint(mods: String) = "Includes equipment: $mods."
    override fun gearSummary(parts: String) = "Equipment: $parts"
    override fun noBonuses() = "No bonuses – add them below (e.g. +2 enhancement to Intelligence)."
    override val defenseHeader = "Defense & hit points"
    override val acLabel = "AC"
    override val touchLabel = "Touch"
    override val flatFootedLabel = "Flat-footed"
    override val hpLabel = "HP"
    override fun acBreakdown(d: Defense) = acParts(d, "armor", "shield", "Dex", "max", "size", "natural", "deflection", "dodge", "other")
    override fun hpHint(auto: Boolean) =
        if (auto) "1st HD maximum, then average (half + 1), creature HD average; + Con per HD, Toughness and extra HP."
        else "Max HP entered by hand."
    override fun armorCheckPenalty(n: Int) = "Armor check penalty $n (Str and Dex skills)"
    override val adjust = "Adjust"
    override val hpBonusLabel = "Extra HP (favored class, traits…)"
    override val hpOverrideLabel = "Rolled max HP (empty = automatic)"
    override val acMiscLabel = "AC – other (monk, feats, spells…)"
    override val damageHint = "− takes damage, + heals. Rest heals 1 HP per level."
    override val metamagicHeader = "Metamagic"
    override fun metamagicLine(feat: String, levels: Int?, slot: Int?, tooHigh: Boolean) = when {
        levels == null -> "$feat – any higher level (up to 9th)"
        levels == 0 -> "$feat – no higher slot"
        else -> "$feat +$levels → level $slot slot" + if (tooHigh) " (you can't cast that level yet)" else ""
    }
    override fun metamagicSlot(levels: Int?) = when (levels) {
        null -> "Spell slot: varies (see text)"
        0 -> "Spell slot: same level"
        else -> "Spell slot: +$levels levels"
    }
    override fun itemsTab(n: Int) = "Items ($n)"
    override fun slotsFilled(filled: Int, total: Int) = "$filled of $total slots"
    override val emptySlot = "Empty"
    override fun ringSlot(i: Int) = "Ring $i"
    override val carriedLabel = "carried"
    override val carriedGroup = "Carried"
    override fun conflictBanner(slot: String, n: Int) = "$slot: $n items, only the first works."
    override val resolve = "Resolve"
    override val slotSheetLabel = "Slot"
    override val conflictExplain = "Only as many items as the slot holds work at a time. Pick the one that is worn."
    override val ignoredNotWorn = "ignored while not worn"
    override val moveOthersToCarried = "Move the others to Carried"
    override val equipAnother = "Equip another item"
    override val equip = "Equip"
    override val noSlotCandidates = "No other item in this inventory fits this slot."
    override fun sameSlotAs(name: String) = "Same slot as $name"
    override val keepThisWorn = "Keep this item worn and move the other one to Carried?"
    override fun unequipOther(name: String) = "Unequip $name"
    override val swap = "Swap"
    override fun selectedSlot(slot: String) = "Selected slot · $slot"
    override val equippedOnly = "Equipped"
    override fun classSpellsHeader(className: String) = "$className spells"
    override val noCasterClasses = "This character casts no spells – add a spellcasting class to build a spellbook."
    override val maxLevelReached = "Character level 20 reached."

    override val raceLabel = "Race"
    override val chooseRaceTitle = "Choose race"
    override val searchRaces = "Search races…"
    override val noRace = "No race"
    override fun raceGroup(g: RaceGroup) = when (g) {
        RaceGroup.CORE -> "Core races"
        RaceGroup.FEATURED -> "Featured races"
        RaceGroup.UNCOMMON -> "Uncommon races"
        RaceGroup.OTHER -> "Other races"
    }
    override fun raceName(id: String, original: String) = original
    override val anyAbilityBonus = "+2 to one ability"
    override val raceSizeHint = "Racial ability modifiers are added to the character's base scores. The race size is copied to the linked inventory (carrying capacity)."
    override val raceBonusLabel = "+2 to"

    override val skillsHeader = "Skills"

    override val alignmentLabel = "Alignment"
    override fun alignmentName(a: Alignment) = when (a) {
        Alignment.LG -> "Lawful good"; Alignment.NG -> "Neutral good"; Alignment.CG -> "Chaotic good"
        Alignment.LN -> "Lawful neutral"; Alignment.N -> "Neutral"; Alignment.CN -> "Chaotic neutral"
        Alignment.LE -> "Lawful evil"; Alignment.NE -> "Neutral evil"; Alignment.CE -> "Chaotic evil"
    }
    override val deityLabel = "Deity"
    override val chooseDeityTitle = "Choose deity"
    override fun deityConflict(deity: String, items: String) = "$deity does not grant: $items – choose again."
    override fun deityDomainsHint(deity: String) = "Only domains granted by $deity are offered (without a deity, or with a custom one, all domains)."
    override val searchDeities = "Search name, pantheon, area…"
    override val customDeity = "Custom…"
    override val noDeity = "No deity"
    override val confirmPrereq = "I have it"
    override val unconfirmPrereq = "Undo"
    override val confirmedByHand = "confirmed"
    override val confirmPrereqHint = "What the app cannot check (racial traits, archetypes…) you can confirm by hand – it then counts for every feat with the same prerequisite."
    override fun skillName(id: String, original: String) = original
    override val classSkillShort = "class skill"
    override fun ranks(n: Int) = if (n == 1) "1 rank" else "$n ranks"
    override val showAllSkills = "Edit skills"
    override val showTrainedSkills = "Done"
    override val noSkillRanks = "No skill ranks yet. Until you assign some, skill prerequisites of feats stay „?“."
    override fun addSubSkill(skill: String) = "Add $skill"
    override val subSkillLabel = "Kind (e.g. alchemy, sing, sailor)"
    override val skillsHint = "Class skill = +3 with at least 1 rank. Max ranks per skill = character level. Points per level: class + Int modifier (min 1), human +1; favored class bonus goes to Extra."
    override fun abilityBaseTitle(name: String, raceMod: Int) =
        if (raceMod == 0) name else "$name – base score (bonuses ${if (raceMod < 0) "−${-raceMod}" else "+$raceMod"})"
    override fun raceIncludedHint(mods: String) = "Includes race: $mods. Tap a score to edit its base value."

    override val myItems = "My items"
    override val saveToMyItems = "Save to my items"
    override val removeFromMyItems = "Remove from my items"
    override val savedToMyItems = "Saved to my items"
    override val myItemsEmpty = "No items of your own yet. Create one with \"Custom item\" – it can be reused in every inventory."

    override val fromSpell = "From spell"
    override val spellItemTitle = "Potion, scroll or wand"
    override fun spellItemKind(k: SpellItemKind) = when (k) {
        SpellItemKind.POTION -> "Potion"
        SpellItemKind.SCROLL -> "Scroll"
        SpellItemKind.WAND -> "Wand"
    }
    override val spellListLabel = "Spell list"
    override fun spellLevelTooHigh(k: SpellItemKind) = "${spellItemKind(k)}: spells up to level ${k.maxSpellLevel} only."
    override val potionNote = "Potions and oils: only spells that target creatures or objects (not personal or area spells)."
    override val materialCostNote = "The spell has a costly material component – add its cost to the price."
    override val pickSpell = "Pick a spell from the list."

    override val aboutTitle = "About & licenses"
    override val couldNotLoadLicense = "Could not load the license text."
    override val gplText = "This program is free software: you can redistribute it and/or modify it under the terms of the " +
        "GNU General Public License version 3. It is distributed WITHOUT ANY WARRANTY. " +
        "The full license text is in the LICENSE file of the source code repository."
    override val gameContentNotGpl = "The game content (spells, items) is not covered by the GPL – it remains under its own licenses below."
    override val oglText = "Game rules text of Pathfinder Roleplaying Game (1st edition) spells and items is Open Game Content under the " +
        "Open Game License v1.0a. Proper names of the Pathfinder campaign setting, the \"Pathfinder\" name and trademarks " +
        "are Product Identity of Paizo Inc., used under the Community Use Policy above."
    override val showOgl = "Show Open Game License v1.0a"
    override val hideOgl = "Hide license text"
    override val dataSources = "Data sources"
    override val sourceSpells = "Spells"
    override val sourceFeats = "Feats"
    override val sourceClasses = "Classes (spells per day, BAB, saves)"
    override val sourceRaces = "Races (size, speed, ability modifiers)"
    override val sourceDeities = "Deities (alignment, domains, favored weapon)"
    override val sourceCompanions = "Animal companions, familiars and eidolons"
    override val privacyHeader = "Privacy"
    override val privacyText = "The app collects no personal data. There is no account, no tracking and no ads. Characters, inventories " +
        "and settings are stored only on your device (in the web version in your browser) and never leave it unless you export them yourself. " +
        "The app works fully offline – it connects to the internet only when you tap to download an updated spell or item database " +
        "(from GitHub), and in the Android and desktop versions at most once a day to check GitHub Releases for a new app version " +
        "(a plain request to the public GitHub API – GitHub sees your IP address like for any web page; nothing about you is sent). A website hosting the web version may count anonymous visits and app actions (e.g. opening the inventory, " +
        "creating a character, export) with cookie-less analytics (Umami). Only the kind of action is sent – never names, notes " +
        "or the content of your characters; no personal data is collected there either."
    override val licenseHeader = "License"
    override val sourceWeapons = "Weapons, ammunition, magic items"
    override val sourceGear = "Gear, armor, alchemical items, mounts"
    override val fonts = "Fonts"
    override val fontsText = "Alegreya, Alegreya Sans, IBM Plex Mono and Caveat – SIL Open Font License 1.1."
    override val libraries = "Libraries"
    override val librariesText = "Kotlin, Compose Multiplatform, kotlinx.coroutines, kotlinx.serialization, Ktor, Multiplatform Settings, " +
        "AndroidX – Apache License 2.0."
}

/** Česká množná čísla: 1 kouzlo, 2–4 kouzla, 5+ kouzel (0 jako 5+). */
private fun cz(n: Int, one: String, few: String, many: String): String = "$n " + when {
    n == 1 -> one
    n in 2..4 -> few
    else -> many
}

/** +3 / −3 (bonus) */
private fun Int.signedPlus(): String = if (this < 0) "−${-this}" else "+$this"

/** −3 s typografickým minus */
private fun Int.signed(): String = if (this < 0) "−${-this}" else "$this"

/** České názvy class podle [cz.pfspells.data.CasterClass.id] (a částí spell listů se stejným názvem). */
private val csSkills = mapOf(
    "acrobatics" to "Akrobacie", "appraise" to "Oceňování", "bluff" to "Blafování", "climb" to "Šplhání",
    "craft" to "Řemeslo", "diplomacy" to "Diplomacie", "disableDevice" to "Zneškodnění mechanismu", "disguise" to "Převleky",
    "escapeArtist" to "Vyklouznutí", "fly" to "Létání", "handleAnimal" to "Zacházení se zvířaty", "heal" to "Léčení",
    "intimidate" to "Zastrašování", "knowledge" to "Znalost", "linguistics" to "Jazykověda", "perception" to "Vnímání",
    "perform" to "Vystupování", "profession" to "Profese", "ride" to "Jízda", "senseMotive" to "Odhalení úmyslu",
    "sleightOfHand" to "Zručnost", "spellcraft" to "Kouzelnictví", "stealth" to "Nenápadnost", "survival" to "Přežití",
    "swim" to "Plavání", "useMagicDevice" to "Použití magického předmětu",
)

private val csRaces = mapOf(
    "dwarf" to "Trpaslík", "elf" to "Elf", "gnome" to "Gnóm", "half-elf" to "Půlelf", "half-orc" to "Půlork",
    "halfling" to "Půlčík", "human" to "Člověk", "orc" to "Ork", "drow" to "Drow", "aquatic-elf" to "Vodní elf",
)

/** Rozpis AC: nenulové části „10 + zbroj 5 + Obr 2 (max 3) + …“ (popisky podle jazyka). */
private fun acParts(
    d: Defense, armor: String, shield: String, dex: String, max: String, size: String,
    natural: String, deflection: String, dodge: String, other: String,
): String {
    fun sg(v: Int) = if (v < 0) "− ${-v}" else "+ $v"
    val parts = listOf(
        armor to d.armor, shield to d.shield, dex to d.dex, size to d.size, natural to d.natural,
        deflection to d.deflection, dodge to d.dodge, other to d.other,
    ).filter { (label, v) -> v != 0 || (label == dex && d.maxDex != null) }
    return "10 " + parts.joinToString(" ") { (label, v) ->
        "${sg(v)} $label" + if (label == dex && d.maxDex != null) " ($max ${d.maxDex})" else ""
    }
}

private val csSpecies = mapOf(
    "ape" to "Lidoop", "badger" to "Jezevec (rosomák)", "bear" to "Medvěd", "bird" to "Pták (orel, jestřáb, sova)",
    "boar" to "Kanec", "camel" to "Velbloud", "cat_big" to "Velká kočka (lev, tygr)", "cat_small" to "Malá kočka (gepard, levhart)",
    "crocodile" to "Krokodýl (aligátor)", "deinonychus" to "Dinosaurus (deinonychus, velociraptor)", "dog" to "Pes",
    "horse" to "Kůň", "pony" to "Poník", "shark" to "Žralok", "snake_constrictor" to "Had, škrtič", "snake_viper" to "Had, zmije",
    "wolf" to "Vlk",
)

private val csClasses = mapOf(
    "animal" to "Zvíře", "magical_beast" to "Magická bestie",
    "wizard" to "Kouzelník", "sorcerer" to "Čaroděj", "arcanist" to "Arkanista", "cleric" to "Klerik",
    "oracle" to "Orákulum", "druid" to "Druid", "witch" to "Čarodějnice", "shaman" to "Šaman", "psychic" to "Psychik",
    "bard" to "Bard", "skald" to "Skald", "inquisitor" to "Inkvizitor", "magus" to "Mágus", "alchemist" to "Alchymista",
    "investigator" to "Vyšetřovatel", "summoner" to "Vyvolávač", "summoner_unchained" to "Vyvolávač (Unchained)",
    "warpriest" to "Válečný kněz", "hunter" to "Lovec", "mesmerist" to "Hypnotizér", "occultist" to "Okultista",
    "spiritualist" to "Spiritualista", "paladin" to "Paladin", "antipaladin" to "Antipaladin", "ranger" to "Hraničář",
    "bloodrager" to "Krvezuřivec", "medium" to "Médium",
    // nesesílací classy
    "barbarian" to "Barbar", "barbarian_unchained" to "Barbar (Unchained)", "brawler" to "Rváč", "cavalier" to "Kavalír",
    "fighter" to "Bojovník", "gunslinger" to "Pistolník", "kineticist" to "Kinetik", "monk" to "Mnich",
    "monk_unchained" to "Mnich (Unchained)", "ninja" to "Ninja", "rogue" to "Tulák", "rogue_unchained" to "Tulák (Unchained)",
    "samurai" to "Samuraj", "shifter" to "Měňavec", "slayer" to "Zabiják", "swashbuckler" to "Šermíř", "vigilante" to "Mstitel",
    "adept" to "Adept", "aristocrat" to "Aristokrat", "commoner" to "Prosťák", "expert" to "Odborník", "warrior" to "Válečník",
)

/** Spell listy ze zdroje, které nejsou id classy. */
private val csListParts = mapOf(
    "unchained summoner" to "Vyvolávač (Unchained)", "adept" to "Adept",
    "red mantiss assasin" to "Vrah Rudé kudlanky",
)

private val csSchools = mapOf(
    "abjuration" to "Odvrhávání", "conjuration" to "Vyvolávání", "divination" to "Věštění", "enchantment" to "Očarování",
    "evocation" to "Zaklínání", "illusion" to "Iluze", "necromancy" to "Nekromancie", "transmutation" to "Přeměňování",
    "universal" to "Univerzální", "multiple" to "Více škol",
)

private val csSubschools = mapOf(
    "calling" to "přivolávání", "creation" to "tvoření", "healing" to "léčení", "summoning" to "povolávání",
    "teleportation" to "teleportace", "scrying" to "sledování", "charm" to "okouzlení", "compulsion" to "nutkání",
    "figment" to "přelud", "glamer" to "klam", "pattern" to "vzorec", "phantasm" to "fantazma", "shadow" to "stín",
    "polymorph" to "proměna", "haunted" to "strašidelné", "curse" to "kletba", "or" to "nebo",
)

object CsStrings : Strings {
    override val back = "Zpět"
    override val cancel = "Zrušit"
    override val save = "Uložit"
    override val delete = "Smazat"
    override val rename = "Přejmenovat"
    override val create = "Vytvořit"
    override val add = "Přidat"
    override val close = "Zavřít"
    override val all = "Vše"
    override val anyOption = "vše"
    override val open = "Otevřeno"
    override val menu = "Menu"
    override val decrease = "Ubrat"
    override val increase = "Přidat"
    override val clearSearch = "Vymazat hledání"
    override val closeDetails = "Zavřít detail"
    override val loading = "Načítám…"
    override val name = "Název"
    override val language = "Jazyk"
    override fun results(n: Int) = cz(n, "výsledek", "výsledky", "výsledků")
    override fun sourceLine(src: String) = "Zdroj: $src"

    override val loadingSpells = "Načítám kouzla…"
    override fun couldNotLoadSpells(msg: String) = "Kouzla se nepodařilo načíst:\n$msg"
    override val useBundledData = "Použít přibalená data"
    override val pricesRevealed = "Všechny ceny odhaleny"
    override val pricesHidden = "Vysoké ceny skryty"

    override val tabSpellbook = "Kouzla"
    override val tabInventory = "Inventář"
    override val spellCompendium = "Kompendium kouzel"
    override val compendium = "Kompendium"
    override val searchSpells = "Hledat kouzla…"
    override val searchItem = "Hledat předmět…"
    override val backToInventory = "Zpět do inventáře"
    override val placeholderNoInventories = "Vlevo založte inventář – pro každou postavu, nebo společnou truhlu družiny."
    override val placeholderPickInventory = "Vyberte inventář vlevo, nebo založte nový."
    override fun placeholderPickCharacter(spellCount: Int) = "Vyberte postavu vlevo, nebo založte novou.\nV kompendiu je všech $spellCount kouzel."

    override val importCharacter = "Importovat postavu"
    override val exportAllCharacters = "Exportovat všechny postavy"
    override val importInventory = "Importovat inventář"
    override val exportAllInventories = "Exportovat všechny inventáře"
    override val menuDatabase = "Databáze a aktualizace"
    override val menuAbout = "O aplikaci a licence"

    override val myCharacters = "Moje postavy"
    override val newCharacter = "Nová postava"
    override val noCharactersYet = "Zatím žádné postavy."
    override fun emptyCharacters(spellCount: Int) =
        "Zatím nemáte žádnou postavu.\nZaložte ji s classou a úrovní – nabídnou se jen kouzla, která umí sesílat.\n" +
            "Exportovanou postavu nahrajete přes menu (vpravo nahoře) → Importovat postavu.\n\nV databázi je $spellCount kouzel."
    override val characterNameLabel = "Jméno postavy"
    override val classLabel = "Classa"
    override val characterLevel = "Úroveň postavy"
    override val levelLabel = "Úroveň"
    override val addSpells = "Přidat kouzla"
    override val exportCharacter = "Exportovat postavu"
    override val deleteCharacter = "Smazat postavu"
    override val deleteCharacterTitle = "Smazat postavu?"
    override fun deleteCharacterText(name: String) = "Postava „$name“ bude trvale smazána."
    override val renameCharacter = "Přejmenovat postavu"
    override fun missingSpells(n: Int) = "Kouzla této postavy, která už nejsou v databázi (např. po aktualizaci): $n."
    override val characterNoSpells = "Zatím žádná kouzla. Přidáte je tlačítkem „Přidat kouzla“."
    override val notOnClassList = "Mimo seznam kouzel classy"
    override val notAvailableAtLevel = "Na této úrovni nedostupné"
    override val importedSuffix = "(import)"

    override fun spells(n: Int) = cz(n, "kouzlo", "kouzla", "kouzel")
    override fun copies(n: Int) = cz(n, "kopie", "kopie", "kopií")
    override fun spellLevelLabel(level: Int) = if (level == 0) "Triky (0)" else "Úroveň $level"
    override fun slotsPrepared(chosen: Int, limit: String) = "$chosen / $limit připraveno"
    override fun slotsKnown(chosen: Int, limit: String) = "$chosen / $limit známých"
    override fun slotsPerDay(slots: String) = "$slots za den"
    override val atWill = "libovolně"
    override fun racialTraitsButton(n: Int) = if (n == 0) "Alternativní rasové traity" else "Alternativní rasové traity ($n)"
    override fun racialTraitsTitle(race: String) = "$race – rasové traity"
    override val defaultTraitsLabel = "Výchozí traity"
    override val chosenTraitsLabel = "Alternativní traity"
    override fun replacesTraits(list: String) = "Nahrazuje: $list"
    override fun traitConflict(list: String) = "Dva alternativní traity nahrazují stejný trait: $list"
    override val noAlternateTraits = "Tahle rasa nemá alternativní rasové traity."
    override val racialTraitsHint = "Nahrazené výchozí traity se přestanou počítat do prerekvizit featů, bonusového featu člověka a skill bodů."
    override val sourceRacialTraits = "Výchozí a alternativní rasové traity"
    override fun archetypesButton(n: Int) = if (n == 0) "Archetypy" else "Archetypy ($n)"
    override fun archetypesTitle(cls: String) = "$cls – archetypy"
    override val chosenArchetypesLabel = "Vybrané archetypy"
    override val archetypesHint = "Nahrazené schopnosti classy zmizí (schopnosti s počtem použití, podtřída, hexy, bonusové featy); známé archetypy navíc mění sesílání a přidávají schopnosti. Zbytek je jen evidence – řiďte se textem."
    override fun archetypeConflict(list: String) = "Dva archetypy nahrazují stejnou schopnost classy: $list"
    override val noArchetypes = "Tahle classa nemá archetypy."
    override val sourceArchetypes = "Archetypy class"
    override fun prestigeTag(maxLevel: Int) = "prestižní · max $maxLevel"
    override fun prestigeKind(kind: String) = when (kind) {
        "arcane" -> "tajemné"; "divine" -> "božské"; "psychic" -> "psychické"; else -> "libovolné"
    }
    override fun prestigeAdvancesTag(kinds: String) = "+ sesílání ($kinds)"
    override fun advanceLabel(kindName: String) = "Sesílání ($kindName)"
    override val advanceNone = "žádná classa"
    override fun advanceSummary(cls: String, n: Int) = "+$n k sesílací úrovni: $cls"
    override fun casterLevelInfo(n: Int) = "sesílací úroveň $n"
    override val missingAdvance = "Vyberte, kterou sesílající classu prestižní classa zvyšuje."
    override val prestigeDetails = "Požadavky a schopnosti"
    override val prestigeRequirementsHeader = "Požadavky"
    override val prestigeTableHeader = "Tabulka classy"
    override val prestigeFeaturesHeader = "Schopnosti classy"
    override val prestigeHint = "Počítá se BAB, záchrany, kostky životů, skilly, bonusové featy a sesílání; ostatní schopnosti jsou jen text."
    override val prestigeColumns = listOf("Úr.", "ZÚB", "Odol", "Refl", "Vůle", "Kouzla")
    override val prestigeUnmet = "Nesplněné požadavky"
    override val sourcePrestige = "Prestižní classy"
    override fun fxRemoves(list: String) = "Ruší: $list"
    override fun fxGains(list: String) = "Získává: $list"
    override fun fxChanges(list: String) = "Mění: $list"
    override val fxNoSpells = "bez sesílání"
    override val fxNoCantrips = "bez triků"
    override val fxNoSubclass = "bez podtřídy"
    override val fxNoSubclassSpells = "bez bonusových kouzel podtřídy"
    override val fxNoSubclassPowers = "bez schopností podtřídy"
    override fun fxLostAbilities(n: Int) = "−$n hexů / zjevení"
    override fun fxLostFeats(n: Int) = if (n == 1) "−1 bonusový feat" else if (n in 2..4) "−$n bonusové featy" else "−$n bonusových featů"
    override fun fxGainedFeats(n: Int) = if (n == 1) "+1 bonusový feat" else if (n in 2..4) "+$n bonusové featy" else "+$n bonusových featů"
    override fun fxPerDay(delta: Int) = "${if (delta < 0) "−${-delta}" else "+$delta"} kouzlo za den na každém levelu"
    override fun fxKnown(delta: Int) = "${if (delta < 0) "−${-delta}" else "+$delta"} známé kouzlo na každém levelu"
    override val fxOneDomain = "jen jedna doména"
    override val secondBloodline = "Druhá bloodline"
    override fun fxSpellList(list: String) = "Sesílání: $list"
    override fun fxAddedSpells(n: Int) = if (n == 1) "+1 kouzlo na listu" else if (n in 2..4) "+$n kouzla na listu" else "+$n kouzel na listu"
    override fun fxAsClass(cls: String) = "sloty jako $cls"
    override fun fxSubclassLevel(delta: Int) = "schopnosti podtřídy na úrovni classy ${if (delta < 0) "−${-delta}" else "+$delta"}"
    override val crossbloodedHint = "Crossblooded: nabízejí se bonusová kouzla, schopnosti i bonusové featy obou bloodline – na každé úrovni si vezmete jednu z nich."
    override fun abilityChoiceLabel(amount: Int) = (if (amount < 0) "−${-amount}" else "+$amount") + " k"
    override fun missingAbilityChoice(trait: String) = "Vyberte atribut u traitu $trait"
    override fun abilityGroupName(g: AbilityGroup) = when (g) {
        AbilityGroup.HEXES -> "Hexy"
        AbilityGroup.REVELATIONS -> "Zjevení (revelations)"
    }
    override fun addClassAbility(g: AbilityGroup) = when (g) {
        AbilityGroup.HEXES -> "Přidat hex"
        AbilityGroup.REVELATIONS -> "Přidat zjevení"
    }
    override fun noClassAbilities(g: AbilityGroup) = when (g) {
        AbilityGroup.HEXES -> "Zatím žádné hexy."
        AbilityGroup.REVELATIONS -> "Zatím žádná zjevení."
    }
    override fun abilityKind(k: AbilityKind) = when (k) {
        AbilityKind.HEX -> "Hex"
        AbilityKind.MAJOR_HEX -> "Velký hex"
        AbilityKind.GRAND_HEX -> "Nejvyšší hex"
        AbilityKind.REVELATION -> "Zjevení"
    }
    override fun fromClassLevel(n: Int) = "od $n. úrovně"
    override val allMysteries = "Všechna mystéria"
    override val allSpirits = "Hexy všech duchů"
    override val removeAbility = "Odebrat"
    override val searchAbilities = "Hledat v názvu nebo textu"
    override val loadingAbilities = "Načítám…"
    override fun abilitiesHint(g: AbilityGroup) = when (g) {
        AbilityGroup.HEXES -> "Počet podle úrovně classy plus každý feat Extra Hex. Šaman může vybírat i hexy svého ducha."
        AbilityGroup.REVELATIONS -> "Počet podle úrovně classy plus každý feat Extra Revelation; finální zjevení přijde na 20. úrovni."
    }
    override val sourceClassAbilities = "Schopnosti domén, bloodline a škol, hexy, zjevení"
    override val sourceSubclassAbilities = "Schopnosti duchů, bloodline bloodragera, disciplín a inquisitions, hexy duchů"
    override val featsHeader = "Featy"
    override fun featCount(have: Int, total: Int) = "$have / $total"
    override fun featSlotsGeneral(n: Int) = "Za úrovně $n"
    override val extraFeats = "Navíc"
    override val addFeat = "Přidat feat"
    override val customFeat = "Vlastní feat"
    override val featName = "Název"
    override val featText = "Popis"
    override val featNote = "Poznámka (např. zvolená zbraň)"
    override val noFeats = "Zatím žádné featy."
    override val removeFeat = "Odebrat feat"
    override val prerequisites = "Prerekvizity"
    override fun prereqStatus(st: PrereqStatus) = when (st) {
        PrereqStatus.MET -> "splněno"
        PrereqStatus.UNMET -> "nesplněno"
        PrereqStatus.UNKNOWN -> "ověř ručně"
    }
    override val prereqUnmet = "Nesplněné prerekvizity"
    override val onlyMet = "Jen featy, na které mám"
    override val featType = "Typ"
    override val generalFeat = "Obecný"
    override val searchFeats = "Hledat featy…"
    override fun featsInDatabase(n: Int) = cz(n, "feat", "featy", "featů")
    override val addFeatToCharacter = "Přidat k postavě"
    override fun featTaken(n: Int) = "Vybráno ×$n"
    override val customBadge = "vlastní"
    override val loadingFeats = "Načítám featy…"
    override fun featsFailed(msg: String) = "Featy se nepodařilo načíst: $msg"
    override val editFeat = "Upravit feat"
    override val inventoryHeader = "Inventář"
    override val resourcesHeader = "Schopnosti s počtem použití"
    override val addTracker = "Přidat počítadlo"
    override val editTracker = "Upravit počítadlo"
    override val removeTracker = "Odebrat počítadlo"
    override val trackerName = "Název"
    override val trackerMax = "Maximum"
    override val trackerUnit = "Jednotka"
    override val trackerNote = "Poznámka"
    override fun unitName(u: ResourceUnit) = when (u) {
        ResourceUnit.PER_DAY -> "použití za den"
        ResourceUnit.ROUNDS -> "kol za den"
        ResourceUnit.POINTS -> "body"
    }
    override fun resourceAmount(n: Int?, u: ResourceUnit) = when {
        n == null -> "bez omezení"
        u == ResourceUnit.PER_DAY -> "$n× za den"
        u == ResourceUnit.ROUNDS -> cz(n, "kolo", "kola", "kol") + " za den"
        else -> cz(n, "bod", "body", "bodů")
    }
    override fun remaining(left: Int, max: Int) = "zbývá $left / $max"
    override fun showHidden(n: Int) = "Zobrazit skryté ($n)"
    override val hideFeature = "Skrýt"
    override val noResources = "Žádné schopnosti s počtem použití – přidejte počítadlo např. pro rasovou schopnost nebo magický předmět."
    override fun subclassTitle(className: String) = "$className – podtřída"
    override fun subclassLabel(t: SubclassType) = when (t) {
        SubclassType.TWO_DOMAINS -> "Domény"
        SubclassType.ONE_DOMAIN -> "Doména"
        SubclassType.MYSTERY -> "Mystérium"
        SubclassType.BLOODLINE -> "Bloodline"
        SubclassType.SCHOOL -> "Škola magie"
        SubclassType.PATRON -> "Patron"
        SubclassType.SPIRIT -> "Duch (spirit)"
        SubclassType.BLOODRAGER_BLOODLINE -> "Bloodline"
        SubclassType.DISCIPLINE -> "Disciplína"
        SubclassType.BLESSINGS -> "Požehnání (blessings)"
        SubclassType.INQUISITOR_DOMAIN -> "Doména"
    }
    override fun domainN(n: Int) = "Doména $n"
    override val subdomain = "Subdoména"
    override fun oppositionN(n: Int) = "Opoziční škola $n"
    override val universalist = "Univerzalista"
    override fun elementalSchool(element: String) = "Elementární – $element"
    override fun element(name: String) = when (name) {
        "air" -> "vzduch"
        "earth" -> "země"
        "fire" -> "oheň"
        "water" -> "voda"
        "metal" -> "kov"
        "wood" -> "dřevo"
        "void" -> "prázdnota"
        else -> name
    }
    override val noneChosen = "žádná"
    override val chooseSubclass = "vybrat…"
    override val subclassLaterHint = "Nepovinné – jde nastavit nebo změnit i později v detailu postavy."
    override val subdomainHint = "Subdoména nahrazuje některá kouzla a schopnost své domény – nabízí se jen subdomény zvolené domény."
    override val druidDomainHint = "Pouto s přírodou: druid volí jednu z přírodních domén (Air, Animal, Earth, Fire, Plant, Water, Weather), volitelně její subdoménu (kromě Metal)."
    override val schoolHint = "Specialista má na každé úrovni kouzel slot navíc pro kouzlo své školy; kouzlo opoziční školy zabere při přípravě dva sloty."
    override fun bonusSpellsHeader(t: SubclassType) = when (t) {
        SubclassType.TWO_DOMAINS, SubclassType.ONE_DOMAIN -> "Doménová kouzla"
        SubclassType.MYSTERY -> "Kouzla mystéria"
        SubclassType.BLOODLINE -> "Kouzla bloodline"
        SubclassType.SCHOOL -> "Kouzla školy"
        SubclassType.PATRON -> "Kouzla patrona"
        SubclassType.SPIRIT -> "Magie ducha"
        SubclassType.BLOODRAGER_BLOODLINE -> "Kouzla bloodline"
        SubclassType.DISCIPLINE -> "Kouzla disciplíny"
        SubclassType.BLESSINGS, SubclassType.INQUISITOR_DOMAIN -> "Schopnosti podtřídy"
    }
    override fun domainSlot(chosen: Int) = "$chosen / 1 doménový slot"
    override fun knownFromLevel(n: Int) = "od úrovně classy $n"
    override val knownAutomatically = "známá automaticky"
    override val schoolSlot = "+1 škola"
    override val oppositionWarn = "Opoziční škola – zabere 2 sloty"
    override val oppositionShort = "opoziční"
    override val subclassNotAvailable = "Na této úrovni zatím nedostupné."
    override fun blessingN(n: Int) = "Požehnání $n"
    override val blessingHint = "Dvě požehnání z domén božstva warpriesta: menší schopnost na 1. úrovni, větší na 10. Všechna požehnání čerpají ze společného denního počtu (Blessings, 3 + ½ úrovně)."
    override fun deityBlessingsHint(deity: String) = "Nabízí se jen požehnání domén božstva $deity (bez božstva nebo s vlastním všechna)."
    override val inquisitorDomainHint = "Inquisitor volí jednu doménu (nebo subdoménu) božstva, případně místo ní inquisition – získá její schopnosti, ale ne doménová kouzla ani doménový slot."
    override val inquisitionLabel = "Inquisition"
    override fun inquisitionName(name: String) = "Inquisition $name"
    override val patronPrepareHint = "Připravte je do běžných slotů tlačítky +/− – pak se objeví mezi kouzly čarodějnice."
    override val wanderingSpirit = "Toulavý duch (wandering spirit)"
    override val wanderingSpiritHint = "Od 4. úrovně se šaman každý den spojí s dalším duchem: získá jeho schopnost (větší od 12., pravou od 20. úrovně) a jeho kouzla magie ducha; od 6. úrovně jeden jeho hex (od 14. dva) jako wandering hex."
    override fun bloodlineBonusFeats(feats: String) = "Bonusové featy bloodline: $feats"
    override val patronHint = "Kouzla patrona přibývají familiárovi na každé sudé úrovni (2., 4.…); čarodějnice je připravuje do běžných slotů."
    override val patronSpellsNote = "přibývají familiárovi, připravují se do běžných slotů"
    override val spiritHint = "Magie ducha: šaman má na každé úrovni kouzel, kterou umí sesílat, slot navíc a spontánně z něj sesílá libovolné kouzlo ducha té úrovně."
    override val spiritSlot = "1 slot magie ducha / den"
    override val bloodragerBloodlineHint = "Bonusová známá kouzla na 7., 10., 13. a 16. úrovni."
    override val disciplineHint = "Bonusová známá kouzla na 1. úrovni a pak na každé sudé."
    override val subclassPowersHeader = "Schopnosti podtřídy"
    override val includeInventory = "Včetně propojeného inventáře"
    override fun withInventory(name: String) = "+ inventář $name"
    override val exportFeats = "Export"
    override val importFeats = "Import"
    override fun exportFeatsTitle(n: Int) = "Export featů ($n)"
    override val exportFeatsHint = "Text níže obsahuje featy postavy (i s poznámkami a vlastními featy). Do jiné postavy je načtete přes „Import“ v její sekci Featy."
    override fun importFeatsTitle(character: String) = "Import featů do postavy $character"
    override fun featsImportSummary(n: Int, skipped: Int) =
        cz(n, "feat", "featy", "featů") + if (skipped > 0) " ($skipped už postava má – přeskočí se)" else ""
    override val linkInventory = "Propojit inventář"
    override val chooseOption = "vybrat…"
    override val newInventoryForCharacter = "Nový inventář"
    override val openInventory = "Otevřít"
    override val unlinkInventory = "Odpojit inventář"
    override fun strengthFromCharacter(name: String) = "propojeno s postavou $name"
    override fun inventorySummary(name: String, weight: String, load: String) = "$name · $weight · $load"
    override val featAlreadyTaken = "Postava ho už má"
    override val gameMode = "Herní režim"
    override val rest = "Odpočinek"
    override val tabCharacter = "Postava"
    override val tabCombat = "Boj"
    override val tabCompanions = "Mazlíčci"
    override val tabSpells = "Kouzla"
    override val sectionsLabel = "Sekce"
    override val modePrepare = "Příprava"
    override val modePlay = "Hra"
    override val modeHintPrepare = "připrav na zítřek"
    override val modeHintPlay = "ťukni na kroužek = sesláno"
    override val levelShort = "ÚR."
    override val babShort = "ZÚB"
    override val fortShort = "ODOL"
    override val refShort = "REFL"
    override val willShort = "VŮLE"
    override val mainAbilityNote = "hlavní!"
    override val stampOverLimit = "NAD LIMIT"
    override val stampUnconscious = "BEZ VĚDOMÍ"
    override val stampOverloaded = "PŘETÍŽENO"
    override val restDone = "sloty obnoveny"
    override val emptySpellsTitle = "Prázdná kniha kouzel"
    override val emptyInventoryTitle = "Batoh zeje prázdnotou"
    override val emptyCompanionsTitle = "Zatím bez parťáka"
    override val emptyNoCasterTitle = "Tahle postava nečaruje"
    override val savedOffline = "uloženo offline"
    override val hpHandHint = "ťukni na − a zapiš zranění, + léčí"
    override fun freeSlots(n: Int) = "+ $n " + when {
        n == 1 -> "volné místo"
        n in 2..4 -> "volná místa"
        else -> "volných míst"
    } + " – připrav kouzlo"
    override val restedToast = "Odpočinuto – kouzla, sloty a schopnosti zpět, vyléčeno 1 HP za úroveň."
    override val castSpell = "Seslat"
    override fun castOf(used: Int, total: Int) = "$used / $total sesláno"
    override val slotLabel = "Slot kouzla"
    override val removeSpell = "Odebrat kouzlo"
    override fun abilityNeeded(ability: String, score: Int) = "vyžaduje $ability $score"
    override fun castingAbility(ability: String, score: Int, mod: String) = "Sesílání: $ability $score ($mod)"
    override val noSpellsYet = "zatím bez kouzel"
    override fun spellLevelSingle(level: Int) = "kouzla $level. úrovně"
    override fun spellLevelRange(min: Int, max: Int) = "kouzla úrovně $min–$max"
    override fun unknownClassLevel(level: Int) = "Neznámá classa · úroveň $level"
    override fun addTo(deckName: String) = "Přidat do: $deckName"
    override fun spellsInDatabase(n: Int) = "V databázi: ${spells(n)}"
    override val school = "Škola"
    override val searchDescriptions = "hledat i v popisu"
    override val hideAdded = "skrýt přidaná"
    override fun cannotCastYet(className: String?, level: Int) =
        "${className ?: "Tato classa"} na úrovni $level ještě nesesílá kouzla (až od 4. úrovně)."
    override val addToCharacter = "Přidat do knihy kouzel"
    override fun addCopy(inDeck: Int) = "Přidat kopii (v knize: $inDeck)"
    override fun addToCharacterCount(inDeck: Int) = "Přidat do knihy (×$inDeck)"
    override fun cannotCastThis(className: String, level: Int) = "$className $level toto kouzlo sesílat nemůže."
    override val castingTime = "Doba sesílání"
    override val components = "Složky"
    override val range = "Dosah"
    override val effect = "Účinek"
    override val target = "Cíl"
    override val duration = "Trvání"
    override val savingThrow = "Záchranný hod"
    override val spellResistance = "Odolnost (SR)"
    override val description = "Popis"
    override val mythic = "Mýtická verze"
    override val openOnAon = "Otevřít na Archives of Nethys"

    override fun exportCharactersTitle(n: Int) = if (n == 1) "Export postavy" else "Export postav ($n)"
    override val exportCharactersHint = "Text níže obsahuje postavu (postavy) s classami, atributy, kouzly a featy. Uložte ho nebo pošlete – na jiném zařízení ho načtete přes „Importovat postavu“."
    override fun notFoundSkipped(names: String) = "Nenalezeno v databázi (přeskočí se): $names"
    override fun exportInventoriesTitle(n: Int) = if (n == 1) "Export inventáře" else "Export inventářů ($n)"
    override val exportInventoriesHint = "Text níže obsahuje inventář s mincemi, předměty a poznámkami. Na jiném zařízení ho načtete přes „Importovat inventář“."
    override val copy = "Kopírovat"
    override val copiedToClipboard = "Zkopírováno do schránky."
    override val done = "Hotovo."
    override fun failed(msg: String?) = "Chyba: $msg"
    override val pasteFromClipboard = "Vložit ze schránky"
    override val openFile = "Otevřít soubor"
    override fun cannotReadFile(msg: String?) = "Soubor nejde přečíst: $msg"
    override val exportText = "Text exportu"
    override fun importButton(n: Int) = if (n > 1) "Importovat ($n)" else "Importovat"
    override fun saveAction(a: SaveAction) = when (a) {
        SaveAction.SAVE_FILE -> "Uložit do souboru"
        SaveAction.SHARE -> "Sdílet"
        SaveAction.DOWNLOAD -> "Stáhnout soubor"
    }
    override fun importError(e: ImportError) = when (e) {
        ImportError.Empty -> "Vložte export nebo vyberte soubor."
        is ImportError.Invalid -> "Tohle není platný export ${if (e.inventory) "inventáře" else "postavy"} (${e.detail ?: "neplatný JSON"})."
        is ImportError.UnknownFormat -> "Neznámý formát „${e.format}“."
        is ImportError.NewerVersion -> "Export pochází z novější verze appky (verze ${e.version})."
        ImportError.NoDecks -> "Export neobsahuje žádné postavy."
        ImportError.NoInventories -> "Export neobsahuje žádné inventáře."
        ImportError.NoFeats -> "Export neobsahuje žádné featy."
        is ImportError.UnknownClass -> "Postava „${e.deck}“ má neznámou classu „${e.classId}“."
    }

    override val myInventories = "Moje inventáře"
    override val newInventory = "Nový inventář"
    override val defaultInventoryName = "Inventář"
    override val inventoryNameLabel = "Název (postava / truhla)"
    override val noInventoriesYet = "Zatím žádné inventáře.\nZaložte jeden pro každou postavu (nebo společnou truhlu družiny) a přidejte předměty z databáze PF1."
    override fun itemTypes(n: Int) = cz(n, "druh předmětu", "druhy předmětů", "druhů předmětů")
    override fun worth(value: String) = "hodnota ≈ $value"
    override val purse = "Měšec"
    override val load = "Zatížení"
    override val categories = "Kategorie"
    override fun category(c: ItemCategory) = when (c) {
        ItemCategory.WEAPON -> "Zbraně"
        ItemCategory.AMMO -> "Střelivo"
        ItemCategory.ARMOR -> "Zbroje a štíty"
        ItemCategory.GEAR -> "Výbava"
        ItemCategory.ALCHEMICAL -> "Alchymie"
        ItemCategory.MOUNT -> "Zvířata a oři"
        ItemCategory.POTION -> "Lektvary a oleje"
        ItemCategory.SCROLL -> "Svitky"
        ItemCategory.WAND -> "Hůlky"
        ItemCategory.RING -> "Prsteny"
        ItemCategory.ROD -> "Žezla"
        ItemCategory.STAFF -> "Hole"
        ItemCategory.WONDROUS -> "Zázračné předměty"
        ItemCategory.MAGIC_WEAPON -> "Magické zbraně"
        ItemCategory.MAGIC_ARMOR -> "Magické zbroje"
        ItemCategory.ARTIFACT -> "Artefakty"
        ItemCategory.OTHER -> "Ostatní"
    }
    override val totalWeight = "Celková váha"
    override fun loadName(l: Load) = when (l) {
        Load.LIGHT -> "Lehké zatížení"
        Load.MEDIUM -> "Střední zatížení"
        Load.HEAVY -> "Těžké zatížení"
        Load.OVERLOADED -> "Přetížení"
    }
    override fun loadLimits(light: String, medium: String, heavy: String) = "lehké ≤ $light, střední ≤ $medium, těžké ≤ $heavy"
    override fun coinsWeight(w: String) = "mince $w"
    override val strength = "Síla"
    override fun editCoin(coin: String) = "Upravit $coin"
    override fun coinsTitle(coin: String) = "$coin – mince"
    override val addItem = "Přidat předmět"
    override val exportInventory = "Exportovat inventář"
    override val deleteInventory = "Smazat inventář"
    override val deleteInventoryTitle = "Smazat inventář?"
    override fun deleteInventoryText(name: String) = "Inventář „$name“ bude trvale smazán."
    override val renameInventory = "Přejmenovat inventář"
    override val inventoryEmpty = "Inventář je prázdný. Předměty z databáze přidáte tlačítkem „Přidat předmět“."
    override val notes = "Poznámky"
    override val notesHint = "Volné poznámky (klíče, dluhy, skrýše…)"
    override val colItem = "Předmět"
    override val colQty = "Počet"
    override val colWeight = "Váha"
    override val colPrice = "Cena"
    override val colNotes = "Poznámky"
    override val colName = "Název"
    override val colType = "Typ"
    override val colDmg = "Zran."
    override val colCrit = "Krit."

    override fun couldNotLoadItems(msg: String) = "Předměty se nepodařilo načíst:\n$msg"
    override val loadingItems = "Načítám databázi předmětů…"
    override fun itemPickerSubtitle(items: Int, invName: String) = "Databáze PF1 · ${cz(items, "předmět", "předměty", "předmětů")} · do: $invName"
    override val customItem = "Vlastní předmět"
    override fun searchItems(n: Int) = "Hledat mezi $n předměty…"
    override fun proficiency(p: String) = when (p) {
        "Simple" -> "Jednoduché"
        "Martial" -> "Válečné"
        "Exotic" -> "Exotické"
        else -> p
    }
    override fun upTo(price: String) = "do $price"
    override val maxPrice = "max"
    override val anyPrice = "libov."
    override fun owned(n: Int) = "máte $n"
    override fun addNamed(name: String) = "Přidat $name"
    override val itemFallback = "Předmět"
    override val cost = "Cena"
    override val weight = "Váha"
    override val damageS = "Zranění (S)"
    override val damageM = "Zranění (M)"
    override val critical = "Kritický zásah"
    override val type = "Typ"
    override val special = "Zvláštní"
    override val armorBonus = "Bonus zbroje"
    override val maxDex = "Max. Obr"
    override val checkPenalty = "Postih zbroje"
    override val spellFailure = "Selhání kouzel"
    override val aura = "Aura"
    override val casterLevel = "Úroveň sesilatele"
    override val slot = "Místo na těle"
    override val requirements = "Požadavky"
    override val info = "Info"
    override fun youOwn(n: Int) = "Máte $n ks"
    override val inInventory = "V inventáři"
    override val quantity = "Počet"
    override val noteOptional = "Poznámka (nepovinná)"
    override val notePlaceholder = "např. mistrovské, nalezeno v kryptě"
    override val editItem = "Upravit předmět"
    override val removeFromInventory = "Odebrat z inventáře"
    override val addToInventory = "Přidat do inventáře"
    override val itemNamePlaceholder = "např. Svitek Mage Armor"
    override val categoryLabel = "Kategorie"
    override val priceGp = "Cena (gp)"
    override val weightLb = "Váha (lb)"
    override val shortInfo = "Krátké info (nepovinné)"
    override val shortInfoPlaceholder = "např. 1d8+1, CL 1"

    override val databases = "Databáze a aktualizace"
    override val spellsHeader = "Kouzla"
    override val itemsHeader = "Předměty"
    override val bundledVersion = "Přibalená verze"
    override val downloadedVersion = "Stažená verze"
    override fun downloadedAt(date: String) = "Staženo $date"
    override val spellsDbInfo = "Kouzla pocházejí z Archives of Nethys (Paizo, Pathfinder 1e) přes otevřený projekt SpellTable-pf1e. " +
        "Pathfinder 1e je uzavřený systém, nová kouzla prakticky nepřibývají – aktualizace přinese hlavně opravy překlepů."
    override val itemsDbInfo = "Zbraně, střelivo a magické předměty pocházejí z tabulek d20pfsrd a jdou tu aktualizovat. " +
        "Běžná výbava, zbroje a alchymie (ze systému Foundry VTT PF1) jsou přibalené v appce – základní vybavení PF1 se nemění."
    override val downloading = "Stahuji…"
    override val downloadLatestSpells = "Stáhnout aktuální databázi kouzel"
    override val downloadLatestItems = "Stáhnout aktuální databázi předmětů"
    override val restoreBundled = "Obnovit přibalenou verzi"
    override val charactersKept = "Postavy zůstanou – kouzla se párují podle id a názvu."
    override val inventoriesKept = "Inventáře zůstanou – položky si nesou vlastní název, cenu a váhu."
    override val appUpdatesHeader = "Aktualizace appky"
    override fun installedVersion(v: String) = "Nainstalovaná verze $v"
    override val devBuild = "vývojový build – automatická kontrola je vypnutá"
    override val checkForUpdates = "Zkontrolovat aktualizace"
    override val checkingUpdates = "Kontroluji…"
    override val upToDate = "Máte nejnovější verzi."
    override fun updateAvailable(v: String) = "Je dostupná verze $v"
    override val downloadAndInstall = "Stáhnout a nainstalovat"
    override val releaseNotes = "Co je nového"
    override fun updateDownloading(percent: Int?) = "Stahuji…" + (percent?.let { " $it %" } ?: "")
    override fun updateFailed(msg: String) = "Aktualizace se nezdařila: $msg"
    override val updateHintApk = "Stáhne se APK a otevře se systémový instalátor – poprvé povolte instalaci z této appky. Postavy i inventáře zůstanou."
    override val updateHintInstaller = "Appka stáhne instalátor do složky Stažené soubory, spustí ho a sama se zavře, aby ji mohl nahradit. Postavy i inventáře zůstanou."
    override val updateLater = "Později"
    override val updateSkip = "Tuto verzi přeskočit"
    override fun updateConfirmTitle(v: String) = "Nainstalovat verzi $v?"
    override val updateBackupHint = "Aktualizace postavy ani inventáře nemaže. Pro jistotu je ale nejdřív exportujte a soubor si uložte mimo appku."
    override val updateSignatureMismatch = "Tahle verze je podepsaná jiným klíčem než nainstalovaná appka, takže ji Android přes ni nenainstaluje. " +
        "Starou verzi je nutné odinstalovat – a tím se smažou všechny postavy a inventáře!\n\n" +
        "1. Exportujte postavy a inventáře a soubory si uložte mimo appku (Stažené soubory, Disk…).\n" +
        "2. Stáhněte APK v prohlížeči (zůstane ve Stažených souborech).\n" +
        "3. Odinstalujte PF Spellbook a nainstalujte stažené APK.\n" +
        "4. Postavy a inventáře znovu importujte (v hlavním menu zvolte Import)."
    override val installNow = "Instalovat"
    override val installAndClose = "Nainstalovat a zavřít appku"
    override val downloadApkInBrowser = "Stáhnout APK v prohlížeči"
    override val itemDbFailed = "Databázi předmětů se nepodařilo načíst."
    override fun itemsCount(n: Int) = cz(n, "předmět", "předměty", "předmětů")
    override fun downloadedSpells(n: Int) = "Staženo: ${spells(n)}."
    override fun downloadedItems(n: Int) = "Staženo: ${itemsCount(n)}."
    override fun downloadFailed(msg: String?) = "Stažení selhalo: $msg"
    override val bundledSpellsRestored = "Přibalená databáze obnovena."
    override val bundledItemsRestored = "Přibalená databáze předmětů obnovena."

    override fun className(id: String, original: String) = csClasses[id] ?: original
    override fun classOption(id: String, original: String) =
        className(id, original).let { if (it == original) it else "$it ($original)" }
    override fun spellListName(key: String) = key.split('/').joinToString("/") { part ->
        csListParts[part] ?: csClasses[part] ?: part.replaceFirstChar { it.uppercase() }
    }
    override fun school(name: String) = csSchools[name] ?: name.replaceFirstChar { it.uppercase() }
    override fun subschool(name: String) = Regex("[a-z]+").replace(name) { csSubschools[it.value] ?: it.value }

    override val sortLabel = "Řadit"
    override fun spellSort(s: SpellSort) = when (s) {
        SpellSort.NAME -> "název"
        SpellSort.LEVEL -> "úroveň"
        SpellSort.SCHOOL -> "škola"
    }
    override val pfsOnly = "jen PFS legal"
    override val notPfsLegal = "Není PFS legal"
    override val favoritesOnly = "jen oblíbená"
    override val favorite = "Oblíbené"
    override val addFavorite = "Přidat do oblíbených"
    override val removeFavorite = "Odebrat z oblíbených"

    override fun loadPenalties(l: Load) = when (l) {
        Load.LIGHT -> null
        Load.MEDIUM, Load.HEAVY ->
            "Max. Obr +${l.maxDex} · postih k ověřením ${l.checkPenalty.signed()} · rychlost 30 → 20 ft, 20 → 15 ft · běh ×${l.runMultiplier}"
        Load.OVERLOADED -> "Pohyb jen 5 ft za kolo (akce na celé kolo), bez bonusu Obr k OČ"
    }
    override fun loadIncreased(l: Load) = "Zatížení vzrostlo: ${loadName(l)}"
    override val sizeLabel = "Velikost"
    override fun creatureSize(c: CreatureSize) = when (c) {
        CreatureSize.FINE -> "Nepatrná"
        CreatureSize.DIMINUTIVE -> "Drobná"
        CreatureSize.TINY -> "Malinká"
        CreatureSize.SMALL -> "Malá"
        CreatureSize.MEDIUM -> "Střední"
        CreatureSize.LARGE -> "Velká"
        CreatureSize.HUGE -> "Obrovská"
        CreatureSize.GARGANTUAN -> "Gigantická"
        CreatureSize.COLOSSAL -> "Kolosální"
    }
    override val quadruped = "Čtyřnohý"

    override val classesHeader = "Classy"
    override val addClass = "Přidat classu"
    override val removeClass = "Odebrat classu"
    override fun removeClassText(className: String) = "Odebrat z postavy classu $className i s jejími kouzly?"
    override val abilitiesHeader = "Atributy"
    override fun abilityShort(a: Ability) = when (a) {
        Ability.STR -> "SÍL"
        Ability.DEX -> "OBR"
        Ability.CON -> "ODL"
        Ability.INT -> "INT"
        Ability.WIS -> "MDR"
        Ability.CHA -> "CHA"
    }
    override fun abilityLong(a: Ability) = when (a) {
        Ability.STR -> "Síla"
        Ability.DEX -> "Obratnost"
        Ability.CON -> "Odolnost"
        Ability.INT -> "Inteligence"
        Ability.WIS -> "Moudrost"
        Ability.CHA -> "Charisma"
    }
    override fun statsLine(st: CharacterStats) =
        "Úroveň ${st.level} · ZÚB ${st.bab.signedPlus()} · Odol ${st.fort.signedPlus()}, Reflex ${st.ref.signedPlus()}, Vůle ${st.will.signedPlus()}"
    override val chooseClass = "Vyber classu"
    override val searchClasses = "Hledat classy…"
    override fun classGroup(g: ClassGroup) = when (g) {
        ClassGroup.FULL -> "9 úrovní kouzel"
        ClassGroup.SIX -> "6 úrovní kouzel"
        ClassGroup.FOUR -> "4 úrovně kouzel"
        ClassGroup.NONE -> "Bez kouzel"
        ClassGroup.PRESTIGE -> "Prestižní classy"
        ClassGroup.NPC -> "NPC classy"
        ClassGroup.CREATURE -> "Tvorové (rasové kostky životů)"
    }
    override fun magicTradition(t: MagicTradition) = when (t) {
        MagicTradition.ARCANE -> "tajemná"
        MagicTradition.DIVINE -> "božská"
        MagicTradition.PSYCHIC -> "psychická"
        MagicTradition.ALCHEMICAL -> "extrakty"
    }
    override fun castingKind(k: CastingKind) = when (k) {
        CastingKind.PREPARED -> "připravuje"
        CastingKind.SPONTANEOUS -> "spontánní"
        CastingKind.HYBRID -> "připraví + spontánní"
    }
    override val strictMode = "Přísný režim"
    override val strictModeHint = "Kouzla jen do limitu spell levelu, featy jen do volných slotů a se splněnými prerekvizitami."
    override fun strictBlocked(b: StrictBlock) = "Přísný režim: " + when (b) {
        is StrictBlock.SpellLevelFull -> "úroveň kouzel ${b.spellLevel} je plná (${b.limit})"
        is StrictBlock.AbilityTooLow -> "na úroveň kouzel ${b.spellLevel} nestačí atribut sesílání"
        StrictBlock.SpellNotAvailable -> "tohle kouzlo classa zatím nesesílá"
        is StrictBlock.FeatSlotsFull -> "všech ${b.total} featů je vybráno"
        StrictBlock.FeatPrereqsUnmet -> "prerekvizity nejsou splněné"
        is StrictBlock.ClassAbilitySlotsFull -> "na této úrovni je vybráno všech ${b.total}"
        is StrictBlock.ClassAbilityLevel -> "vyžaduje ${b.minLevel}. úroveň classy"
        is StrictBlock.RacialTraitReplaced -> "„${b.trait}“ už nahrazuje jiný alternativní trait"
        is StrictBlock.ArchetypeConflict -> "„${b.feature}“ už nahrazuje jiný archetyp"
        is StrictBlock.PrestigeRequirementUnmet -> "nesplněný požadavek prestižní classy – ${b.requirement}"
    }
    override val companionsHeader = "Mazlíčci a společníci"
    override val companionsHint = "Zvířecí společník, jezdecké zvíře, familiár, eidolon nebo mazlíček – každý má vlastní list, featy, skills i inventář."
    override val addCompanion = "Přidat mazlíčka"
    override val newCompanion = "Nový mazlíček"
    override val companionSettings = "Nastavení mazlíčka"
    override val companionKindLabel = "Druh"
    override fun companionKind(k: CompanionKind) = when (k) {
        CompanionKind.ANIMAL_COMPANION -> "Zvířecí společník"
        CompanionKind.MOUNT -> "Jezdecké zvíře"
        CompanionKind.FAMILIAR -> "Familiár"
        CompanionKind.EIDOLON -> "Eidolon"
        CompanionKind.PET -> "Mazlíček"
    }
    override val creatureLabel = "Druh tvora"
    override val hitDiceLabel = "Kostky životů (HD)"
    override val sizeByRace = "podle rasy"
    override fun companionOf(kind: String, owner: String) = "$kind · patří k: $owner"
    override val speciesLabel = "Zvíře"
    override val speciesCustom = "Vlastní"
    override fun speciesName(sp: AnimalSpecies) = csSpecies[sp.id]?.let { "$it (${sp.name})" } ?: sp.name
    override val companionProgression = "Progrese zvířecího společníka"
    override val companionLevelLabel = "Efektivní úroveň druida"
    override val familiarLabel = "Familiár"
    override val familiarLevelLabel = "Úroveň pána"
    override val masterBonusLabel = "Bonus pánovi"
    override val masterBonusConditional = "podmíněný – nepočítá se automaticky"
    override val familiarNoteLabel = "Statistiky"
    override fun familiarSpecial(sp: cz.pfspells.data.FamiliarSpecial) = when (sp) {
        cz.pfspells.data.FamiliarSpecial.ALERTNESS -> "alertness (pán)"
        cz.pfspells.data.FamiliarSpecial.IMPROVED_EVASION -> "improved evasion"
        cz.pfspells.data.FamiliarSpecial.SHARE_SPELLS -> "sdílení kouzel"
        cz.pfspells.data.FamiliarSpecial.EMPATHIC_LINK -> "empatické pouto"
        cz.pfspells.data.FamiliarSpecial.DELIVER_TOUCH_SPELLS -> "doručení dotykových kouzel"
        cz.pfspells.data.FamiliarSpecial.SPEAK_WITH_MASTER -> "řeč s pánem"
        cz.pfspells.data.FamiliarSpecial.SPEAK_WITH_ANIMALS -> "řeč se zvířaty svého druhu"
        cz.pfspells.data.FamiliarSpecial.SPELL_RESISTANCE -> "odolnost vůči kouzlům"
        cz.pfspells.data.FamiliarSpecial.SCRY_ON_FAMILIAR -> "věštění přes familiára"
    }
    override fun familiarStats(naturalArmor: Int, int: Int, sr: Int?) =
        listOfNotNull("přirozená zbroj +$naturalArmor", "Int $int", sr?.let { "SR $it" }).joinToString(" · ")
    override val eidolonFormLabel = "Základní forma"
    override val specialsLabel = "Schopnosti"
    override val eidolonSubtypeLabel = "Podtyp"
    override val summonerLevelLabel = "Úroveň summonera"
    override fun eidolonStats(hd: Int, naturalArmor: Int?, strDex: Int, maxAttacks: Int) =
        listOfNotNull("HD $hd", naturalArmor?.let { "přirozená zbroj +$it" }, "Str/Obr +$strDex", "max. útoků $maxAttacks").joinToString(" · ")
    override fun evolutionPool(used: Int, total: Int?) = "Body evolucí: $used" + (total?.let { " / $it" } ?: "")
    override val evolutionsOver = "Utraceno víc bodů, než dovoluje tabulka."
    override val evolutionsHeader = "Evoluce"
    override val addEvolution = "Přidat evoluci"
    override fun evolutionCost(n: Int) = "$n b."
    override val freeEvolutionsLabel = "Volné evoluce"
    override val eidolonHint = "Počítá se Ability Increase (+2) a Improved Natural Armor (+2), Large mění velikost; ostatní evoluce jsou jen text."
    override val familiarHint = "Bonus pánovi se počítá na listu pána (skilly, záchrany, AC, HP). HD = úroveň pána; familiár má BAB a základní záchrany pána, jsou-li lepší. Atributy, velikost, rychlost a útoky se vyplní z bestiáře při výběru druhu."
    override fun companionSpecial(sp: CompanionSpecial) = when (sp) {
        CompanionSpecial.LINK -> "Pouto (Link)"
        CompanionSpecial.SHARE_SPELLS -> "Sdílení kouzel"
        CompanionSpecial.EVASION -> "Vyhýbání (Evasion)"
        CompanionSpecial.ABILITY_INCREASE -> "Zvýšení atributu"
        CompanionSpecial.DEVOTION -> "Oddanost (Devotion)"
        CompanionSpecial.MULTIATTACK -> "Multiattack"
        CompanionSpecial.IMPROVED_EVASION -> "Zlepšené vyhýbání"
    }
    override fun companionStats(naturalArmor: Int?, strDex: Int?, tricks: Int?) = listOfNotNull(
        naturalArmor?.let { "Přirozená zbroj +$it" },
        strDex?.let { "Síl/Obr +$it" },
        tricks?.let { "Triky navíc $it" },
    ).joinToString(" · ")
    override fun abilityIncreases(n: Int) = "+1 k atributu ×$n (přičtěte ručně k základní hodnotě)"
    override val speedLabel = "Rychlost"
    override val attacksLabel = "Útoky"
    override val qualitiesLabel = "Zvláštní"
    override fun advancementAt(level: Int) = "vylepšení na $level. úrovni"
    override fun companionIncludedHint(mods: String) = "Včetně bonusů společníka: $mods."
    override val rarityLabel = "Rarita"
    override fun rarity(r: CompanionRarity) = when (r) {
        CompanionRarity.COMMON -> "Běžný"
        CompanionRarity.UNCOMMON -> "Neobvyklý"
        CompanionRarity.RARE -> "Vzácný"
        CompanionRarity.UNIQUE -> "Jedinečný"
    }
    override val respawnLabel = "Když zemře (návrat / náhrada)"
    override fun defaultRespawn(k: CompanionKind) = when (k) {
        CompanionKind.ANIMAL_COMPANION ->
            "Když zvířecí společník zemře, nového lze získat obřadem – 24 hodin nepřerušené modlitby v prostředí, " +
                "kde nový společník obvykle žije."
        CompanionKind.MOUNT ->
            "Paladin (divine bond): dalšího oře nelze přivolat 30 dní nebo do získání další úrovně paladina (co nastane dřív); " +
                "do té doby má paladin postih −1 k útoku a zranění zbraní. Cavalier: nové zvíře si najde po týdnu truchlení."
        CompanionKind.FAMILIAR ->
            "Když familiár zemře nebo je propuštěn, nelze ho týden nahradit; pak rituál za 200 gp za úroveň kouzelníka, " +
                "který trvá 8 hodin, přivolá nového."
        CompanionKind.EIDOLON ->
            "Když je eidolon smrtí poslán zpět na svou rovinu, nelze ho znovu přivolat dřív než následující den."
        CompanionKind.PET -> "Bez zvláštních pravidel – zapište vlastní."
    }
    override val resetToDefault = "Výchozí text"
    override val equipmentHeader = "Vybavení"
    override val equippedLabel = "Nasazeno"
    override val equipHint = "Bonusy nasazených předmětů platí pro postavu propojenou s tímto inventářem (bonusy stejného typu se nesčítají – platí nejvyšší)."
    override fun itemSlot(sl: ItemSlot) = when (sl) {
        ItemSlot.HEAD -> "Hlava"
        ItemSlot.HEADBAND -> "Čelenka"
        ItemSlot.EYES -> "Oči"
        ItemSlot.SHOULDERS -> "Ramena"
        ItemSlot.NECK -> "Krk"
        ItemSlot.CHEST -> "Hruď"
        ItemSlot.BODY -> "Tělo"
        ItemSlot.ARMOR -> "Zbroj"
        ItemSlot.BELT -> "Opasek"
        ItemSlot.WRISTS -> "Zápěstí"
        ItemSlot.HANDS -> "Ruce"
        ItemSlot.RING -> "Prsten"
        ItemSlot.FEET -> "Nohy"
        ItemSlot.MAIN_HAND -> "Hlavní ruka"
        ItemSlot.SHIELD -> "Druhá ruka"
    }
    override val noSlot = "Bez slotu"
    override val bonusesLabel = "Bonusy"
    override val addBonus = "Přidat bonus"
    override val bonusTargetLabel = "Na co"
    override fun bonusTarget(t: BonusTarget) = when (t) {
        BonusTarget.STR -> "Síla"
        BonusTarget.DEX -> "Obratnost"
        BonusTarget.CON -> "Odolnost"
        BonusTarget.INT -> "Inteligence"
        BonusTarget.WIS -> "Moudrost"
        BonusTarget.CHA -> "Charisma"
        BonusTarget.FORT -> "Odolnost (záchrana)"
        BonusTarget.REF -> "Reflexy"
        BonusTarget.WILL -> "Vůle"
        BonusTarget.AC -> "AC"
        BonusTarget.SKILL -> "Skill"
        BonusTarget.MAX_DEX -> "Max. Obratnost"
        BonusTarget.ARMOR_CHECK -> "Postih zbroje"
        BonusTarget.HP -> "Životy"
    }
    override val bonusTypeLabel = "Typ bonusu"
    override fun bonusType(t: BonusType) = t.name.lowercase().replace('_', ' ')
    override val bonusValueLabel = "Hodnota"
    override val bonusSkillLabel = "Skill"
    override fun slotConflict(slots: String) = "Příliš mnoho nasazených předmětů ve slotu: $slots"
    override fun gearIncludedHint(mods: String) = "Včetně vybavení: $mods."
    override fun gearSummary(parts: String) = "Vybavení: $parts"
    override fun noBonuses() = "Bez bonusů – přidejte je níže (např. +2 enhancement k Inteligenci)."
    override val defenseHeader = "Obrana a životy"
    override val acLabel = "AC"
    override val touchLabel = "Dotyk"
    override val flatFootedLabel = "Zaskočen"
    override val hpLabel = "Životy"
    override fun acBreakdown(d: Defense) = acParts(d, "zbroj", "štít", "Obr", "max", "velikost", "přirozená", "deflection", "dodge", "ostatní")
    override fun hpHint(auto: Boolean) =
        if (auto) "1. HD naplno, dál průměr (polovina + 1), HD tvorů průměr; + Odolnost za HD, Toughness a HP navíc."
        else "Max. HP zadané ručně."
    override fun armorCheckPenalty(n: Int) = "Postih zbroje $n (skills Síly a Obratnosti)"
    override val adjust = "Upravit"
    override val hpBonusLabel = "HP navíc (oblíbená classa, traity…)"
    override val hpOverrideLabel = "Házené max. HP (prázdné = automaticky)"
    override val acMiscLabel = "AC – ostatní (monk, featy, kouzla…)"
    override val damageHint = "− zranění, + léčení. Odpočinek vyléčí 1 HP za úroveň."
    override val metamagicHeader = "Metamagie"
    override fun metamagicLine(feat: String, levels: Int?, slot: Int?, tooHigh: Boolean) = when {
        levels == null -> "$feat – libovolná vyšší úroveň (max. 9.)"
        levels == 0 -> "$feat – slot stejné úrovně"
        else -> "$feat +$levels → slot $slot. úrovně" + if (tooHigh) " (tuto úroveň zatím nesesíláte)" else ""
    }
    override fun metamagicSlot(levels: Int?) = when (levels) {
        null -> "Slot kouzla: proměnný (viz text)"
        0 -> "Slot kouzla: stejná úroveň"
        else -> "Slot kouzla: +$levels úrovně"
    }
    override fun itemsTab(n: Int) = "Předměty ($n)"
    override fun slotsFilled(filled: Int, total: Int) = "$filled z $total slotů"
    override val emptySlot = "Prázdné"
    override fun ringSlot(i: Int) = "Prsten $i"
    override val carriedLabel = "neseno"
    override val carriedGroup = "Neseno"
    override fun conflictBanner(slot: String, n: Int) = "$slot: $n předměty, platí jen první."
    override val resolve = "Vyřešit"
    override val slotSheetLabel = "Slot"
    override val conflictExplain = "Platí jen tolik předmětů, kolik se do slotu vejde. Vyberte ten, který má postava na sobě."
    override val ignoredNotWorn = "neplatí, dokud není nasazený"
    override val moveOthersToCarried = "Ostatní odložit do batohu"
    override val equipAnother = "Nasadit jiný předmět"
    override val equip = "Nasadit"
    override val noSlotCandidates = "Do tohoto slotu se v inventáři nic dalšího nehodí."
    override fun sameSlotAs(name: String) = "Stejný slot jako $name"
    override val keepThisWorn = "Nechat nasazený tento předmět a ten druhý odložit do batohu?"
    override fun unequipOther(name: String) = "Sundat $name"
    override val swap = "Prohodit"
    override fun selectedSlot(slot: String) = "Vybraný slot · $slot"
    override val equippedOnly = "Nasazeno"
    override fun classSpellsHeader(className: String) = "Kouzla – $className"
    override val noCasterClasses = "Tato postava nesesílá kouzla – pro knihu kouzel přidejte sesílací classu."
    override val maxLevelReached = "Postava má 20. úroveň."

    override val raceLabel = "Rasa"
    override val chooseRaceTitle = "Výběr rasy"
    override val searchRaces = "Hledat rasy…"
    override val noRace = "Bez rasy"
    override fun raceGroup(g: RaceGroup) = when (g) {
        RaceGroup.CORE -> "Základní rasy"
        RaceGroup.FEATURED -> "Významné rasy"
        RaceGroup.UNCOMMON -> "Neobvyklé rasy"
        RaceGroup.OTHER -> "Ostatní rasy"
    }
    override fun raceName(id: String, original: String) = csRaces[id] ?: original
    override val anyAbilityBonus = "+2 k jednomu atributu"
    override val raceSizeHint = "Rasové úpravy se přičtou k základním atributům postavy. Velikost rasy se propíše do propojeného inventáře (nosnost)."
    override val raceBonusLabel = "+2 k"

    override val skillsHeader = "Dovednosti"

    override val alignmentLabel = "Přesvědčení"
    override fun alignmentName(a: Alignment) = when (a) {
        Alignment.LG -> "Zákonně dobrý"; Alignment.NG -> "Neutrálně dobrý"; Alignment.CG -> "Chaoticky dobrý"
        Alignment.LN -> "Zákonně neutrální"; Alignment.N -> "Neutrální"; Alignment.CN -> "Chaoticky neutrální"
        Alignment.LE -> "Zákonně zlý"; Alignment.NE -> "Neutrálně zlý"; Alignment.CE -> "Chaoticky zlý"
    }
    override val deityLabel = "Božstvo"
    override val chooseDeityTitle = "Výběr božstva"
    override fun deityConflict(deity: String, items: String) = "$deity nedává: $items – vyberte znovu."
    override fun deityDomainsHint(deity: String) = "Nabízí se jen domény božstva $deity (bez božstva nebo s vlastním všechny)."
    override val searchDeities = "Hledat jméno, panteon, oblast…"
    override val customDeity = "Vlastní…"
    override val noDeity = "Bez božstva"
    override val confirmPrereq = "Mám"
    override val unconfirmPrereq = "Zrušit"
    override val confirmedByHand = "potvrzeno"
    override val confirmPrereqHint = "Co appka neověří (rasové traity, archetypy…), můžete potvrdit ručně – platí pak pro všechny featy se stejnou prerekvizitou."
    override fun skillName(id: String, original: String) = csSkills[id.substringBefore('.')]
        ?.let { if (id.startsWith("knowledge.")) "$it (${id.substringAfter('.')})" else it } ?: original
    override val classSkillShort = "třídní"
    override fun ranks(n: Int) = cz(n, "rank", "ranky", "ranků")
    override val showAllSkills = "Upravit dovednosti"
    override val showTrainedSkills = "Hotovo"
    override val noSkillRanks = "Zatím žádné ranky. Dokud je nezadáte, prerekvizity featů na dovednosti zůstanou „?“."
    override fun addSubSkill(skill: String) = "Přidat: $skill"
    override val subSkillLabel = "Druh (např. alchemy, sing, sailor)"
    override val skillsHint = "Třídní dovednost = +3, má-li aspoň 1 rank. Max. ranků v dovednosti = úroveň postavy. Body za úroveň: classa + mod. INT (min. 1), člověk +1; bonus za oblíbenou classu přidejte do „Navíc“."
    override fun abilityBaseTitle(name: String, raceMod: Int) =
        if (raceMod == 0) name else "$name – základ (bonusy ${if (raceMod < 0) "−${-raceMod}" else "+$raceMod"})"
    override fun raceIncludedHint(mods: String) = "Včetně rasy: $mods. Klepnutím na atribut upravíte základní hodnotu."

    override val myItems = "Moje předměty"
    override val saveToMyItems = "Uložit do mých předmětů"
    override val removeFromMyItems = "Odebrat z mých předmětů"
    override val savedToMyItems = "Uloženo do mých předmětů"
    override val myItemsEmpty = "Zatím žádné vlastní předměty. Vytvořte je přes „Vlastní předmět“ – půjdou použít v každém inventáři."

    override val fromSpell = "Z kouzla"
    override val spellItemTitle = "Lektvar, svitek nebo hůlka"
    override fun spellItemKind(k: SpellItemKind) = when (k) {
        SpellItemKind.POTION -> "Lektvar"
        SpellItemKind.SCROLL -> "Svitek"
        SpellItemKind.WAND -> "Hůlka"
    }
    override val spellListLabel = "Seznam kouzel"
    override fun spellLevelTooHigh(k: SpellItemKind) = "${spellItemKind(k)}: jen kouzla do ${k.maxSpellLevel}. úrovně."
    override val potionNote = "Lektvary a oleje: jen kouzla cílící na tvory nebo předměty (ne osobní ani plošná)."
    override val materialCostNote = "Kouzlo má drahou materiální složku – její cenu připočtěte."
    override val pickSpell = "Vyberte kouzlo ze seznamu."

    override val aboutTitle = "O aplikaci a licence"
    override val couldNotLoadLicense = "Text licence se nepodařilo načíst."
    override val gplText = "Tento program je svobodný software: můžete ho šířit a upravovat podle podmínek " +
        "GNU General Public License verze 3. Je šířen BEZ JAKÉKOLI ZÁRUKY. " +
        "Plný text licence je v souboru LICENSE v repozitáři se zdrojovým kódem."
    override val gameContentNotGpl = "Herní obsah (kouzla, předměty) pod GPL nespadá – zůstává pod vlastními licencemi níže."
    override val oglText = "Pravidlové texty kouzel a předmětů Pathfinder Roleplaying Game (1. edice) jsou Open Game Content podle " +
        "Open Game License v1.0a. Vlastní jména světa Pathfinderu, název „Pathfinder“ a ochranné známky jsou " +
        "Product Identity společnosti Paizo Inc., použité podle Community Use Policy výše."
    override val showOgl = "Zobrazit Open Game License v1.0a"
    override val hideOgl = "Skrýt text licence"
    override val dataSources = "Zdroje dat"
    override val sourceSpells = "Kouzla"
    override val sourceFeats = "Featy"
    override val sourceClasses = "Classy (kouzla za den, ZÚB, záchrany)"
    override val sourceRaces = "Rasy (velikost, rychlost, úpravy atributů)"
    override val sourceDeities = "Božstva (přesvědčení, domény, oblíbená zbraň)"
    override val sourceCompanions = "Zvířecí společníci, familiáři a eidoloni"
    override val privacyHeader = "Soukromí"
    override val privacyText = "Appka nesbírá žádné osobní údaje. Nemá účet, sledování ani reklamy. Postavy, inventáře a nastavení " +
        "jsou uložené jen ve vašem zařízení (u webové verze ve vašem prohlížeči) a nikam se neposílají, dokud je sami neexportujete. " +
        "Appka funguje úplně offline – na internet se připojí jen tehdy, když klepnete na stažení aktualizované databáze kouzel " +
        "nebo předmětů (z GitHubu), a ve verzi pro Android a desktop nejvýš jednou denně kvůli kontrole nové verze appky " +
        "v GitHub Releases (obyčejný dotaz na veřejné GitHub API – GitHub vidí vaši IP adresu jako u každé webové stránky; nic o vás se " +
        "neposílá). Web, na kterém běží webová verze, může počítat anonymní návštěvy analytikou bez cookies " +
        "(Umami), včetně anonymních akcí v appce (např. otevření inventáře, vytvoření postavy, export). Posílá se jen druh akce – " +
        "nikdy jména, poznámky ani obsah postav; ani tam se osobní údaje nesbírají."
    override val licenseHeader = "Licence"
    override val sourceWeapons = "Zbraně, střelivo, magické předměty"
    override val sourceGear = "Výbava, zbroje, alchymie, zvířata"
    override val fonts = "Písma"
    override val fontsText = "Alegreya, Alegreya Sans, IBM Plex Mono a Caveat – SIL Open Font License 1.1."
    override val libraries = "Knihovny"
    override val librariesText = "Kotlin, Compose Multiplatform, kotlinx.coroutines, kotlinx.serialization, Ktor, Multiplatform Settings, " +
        "AndroidX – Apache License 2.0."
}
