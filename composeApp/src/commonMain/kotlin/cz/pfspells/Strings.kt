package cz.pfspells

import cz.pfspells.data.ResourceUnit
import cz.pfspells.data.SubclassType
import cz.pfspells.data.PrereqStatus
import cz.pfspells.data.ClassGroup
import cz.pfspells.data.CharacterStats
import cz.pfspells.data.Ability
import cz.pfspells.data.SpellItemKind
import cz.pfspells.data.CreatureSize
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
    val subdomainHint: String
    val schoolHint: String
    fun bonusSpellsHeader(t: SubclassType): String
    fun domainSlot(chosen: Int): String
    fun knownFromLevel(n: Int): String
    val knownAutomatically: String
    val schoolSlot: String
    val oppositionWarn: String
    val oppositionShort: String
    val subclassNotAvailable: String
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
    fun classSpellsHeader(className: String): String
    val noCasterClasses: String
    val maxLevelReached: String

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
    override val menuDatabase = "Spell & item database / update"
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
    }
    override fun domainN(n: Int) = "Domain $n"
    override val subdomain = "Subdomain"
    override fun oppositionN(n: Int) = "Opposition school $n"
    override val universalist = "Universalist"
    override fun elementalSchool(element: String) = "Elemental – $element"
    override fun element(name: String) = name.replaceFirstChar { it.uppercase() }
    override val noneChosen = "none"
    override val chooseSubclass = "choose…"
    override val subdomainHint = "A subdomain replaces some spells of its domain – pick one that belongs to the chosen domain."
    override val schoolHint = "Specialists get one extra slot per spell level for spells of their school; preparing an opposition-school spell takes two slots."
    override fun bonusSpellsHeader(t: SubclassType) = when (t) {
        SubclassType.TWO_DOMAINS, SubclassType.ONE_DOMAIN -> "Domain spells"
        SubclassType.MYSTERY -> "Mystery spells"
        SubclassType.BLOODLINE -> "Bloodline spells"
        SubclassType.SCHOOL -> "School spells"
    }
    override fun domainSlot(chosen: Int) = "$chosen / 1 domain slot"
    override fun knownFromLevel(n: Int) = "from class level $n"
    override val knownAutomatically = "known automatically"
    override val schoolSlot = "+1 school"
    override val oppositionWarn = "Opposition school – takes 2 slots"
    override val oppositionShort = "opposition"
    override val subclassNotAvailable = "Not yet available at this level."
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
    override val restedToast = "Rested – all spells and slots are back."
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

    override val databases = "Databases"
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
        ClassGroup.NPC -> "NPC classes"
    }
    override fun classSpellsHeader(className: String) = "$className spells"
    override val noCasterClasses = "This character casts no spells – add a spellcasting class to build a spellbook."
    override val maxLevelReached = "Character level 20 reached."

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
    override val sourceWeapons = "Weapons, ammunition, magic items"
    override val sourceGear = "Gear, armor, alchemical items, mounts"
    override val fonts = "Fonts"
    override val fontsText = "Alegreya, Alegreya Sans and IBM Plex Mono – SIL Open Font License 1.1."
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
private val csClasses = mapOf(
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
    override val menuDatabase = "Databáze kouzel a předmětů"
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
    override val subdomainHint = "Subdoména nahrazuje některá kouzla své domény – vyberte takovou, která k doméně patří."
    override val schoolHint = "Specialista má na každé úrovni kouzel slot navíc pro kouzlo své školy; kouzlo opoziční školy zabere při přípravě dva sloty."
    override fun bonusSpellsHeader(t: SubclassType) = when (t) {
        SubclassType.TWO_DOMAINS, SubclassType.ONE_DOMAIN -> "Doménová kouzla"
        SubclassType.MYSTERY -> "Kouzla mystéria"
        SubclassType.BLOODLINE -> "Kouzla bloodline"
        SubclassType.SCHOOL -> "Kouzla školy"
    }
    override fun domainSlot(chosen: Int) = "$chosen / 1 doménový slot"
    override fun knownFromLevel(n: Int) = "od úrovně classy $n"
    override val knownAutomatically = "známá automaticky"
    override val schoolSlot = "+1 škola"
    override val oppositionWarn = "Opoziční škola – zabere 2 sloty"
    override val oppositionShort = "opoziční"
    override val subclassNotAvailable = "Na této úrovni zatím nedostupné."
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
    override val restedToast = "Odpočinuto – všechna kouzla a sloty jsou zpět."
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

    override val databases = "Databáze"
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
        ClassGroup.NPC -> "NPC classy"
    }
    override fun classSpellsHeader(className: String) = "Kouzla – $className"
    override val noCasterClasses = "Tato postava nesesílá kouzla – pro knihu kouzel přidejte sesílací classu."
    override val maxLevelReached = "Postava má 20. úroveň."

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
    override val sourceWeapons = "Zbraně, střelivo, magické předměty"
    override val sourceGear = "Výbava, zbroje, alchymie, zvířata"
    override val fonts = "Písma"
    override val fontsText = "Alegreya, Alegreya Sans a IBM Plex Mono – SIL Open Font License 1.1."
    override val libraries = "Knihovny"
    override val librariesText = "Kotlin, Compose Multiplatform, kotlinx.coroutines, kotlinx.serialization, Ktor, Multiplatform Settings, " +
        "AndroidX – Apache License 2.0."
}
