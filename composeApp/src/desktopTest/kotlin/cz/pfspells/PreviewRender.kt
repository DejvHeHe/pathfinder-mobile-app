package cz.pfspells

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import androidx.compose.ui.use
import com.russhwolf.settings.PreferencesSettings
import cz.pfspells.data.InvEntry
import cz.pfspells.data.InventoryStore
import cz.pfspells.data.ItemCategory
import cz.pfspells.data.ItemRepository
import cz.pfspells.ui.ClassPickerDialog
import cz.pfspells.ui.PfTheme
import cz.pfspells.ui.RacePickerDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import java.util.prefs.Preferences
import kotlin.test.Test

/**
 * Vykreslí ukázkové obrazovky do composeApp/build/previews (ruční kontrola designu).
 * Spouští se jen s -Dpreviews=true, aby nezdržoval běžné testy.
 */
class PreviewRender {
    @Test
    fun render() = runBlocking {
        if (System.getProperty("previews") != "true") return@runBlocking
        val node = Preferences.userRoot().node("pfspells-preview-test")
        node.clear()
        val settings = PreferencesSettings(node)
        val state = withContext(Dispatchers.Main) {
            AppState(inventoryStore = InventoryStore(settings), itemRepo = ItemRepository(settings), settings = settings)
                .also { it.changeLang(Lang.EN) }
        }
        while (state.db !is DbState.Ready) delay(50)
        val elara = withContext(Dispatchers.Main) {
            val thomas = state.createCharacter("Brother Thomas", "cleric", 7)
            state.addClass(thomas.id, "fighter", 2)
            state.setAbility(thomas.id, cz.pfspells.data.Ability.WIS, 17)
            state.setAbility(thomas.id, cz.pfspells.data.Ability.STR, 14)
            state.setRace(thomas.id, "dwarf")
            state.setAlignment(thomas.id, cz.pfspells.data.Alignment.LG)
            state.setDeity(thomas.id, "Torag")
            mapOf("knowledge.religion" to 5, "heal" to 4, "diplomacy" to 3, "sense motive" to 0, "intimidate" to 2, "craft:armor" to 2)
                .filterValues { it > 0 }.forEach { (k, v) -> state.setSkillRanks(thomas.id, k, v) }
            state.updateCharacter(thomas.id) {
                it.copy(
                    feats = listOf(
                        cz.pfspells.data.FeatPick("power-attack", "Power Attack"),
                        cz.pfspells.data.FeatPick("dodge", "Dodge"),
                        cz.pfspells.data.FeatPick("weapon-focus", "Weapon Focus", note = "morningstar"),
                        cz.pfspells.data.FeatPick(null, "Blessed by Iomedae", text = "Once per day re-roll a Will save."),
                    ),
                )
            }
            state.ensureFeats()
            state.setSubclass(thomas.id, "cleric", cz.pfspells.data.SubclassSlot.DOMAIN1, "fire")
            state.setSubclass(thomas.id, "cleric", cz.pfspells.data.SubclassSlot.DOMAIN2, "magic")
            state.setSubclass(thomas.id, "cleric", cz.pfspells.data.SubclassSlot.SUBDOMAIN2, "arcane")
            state.setBonusSpellCount(thomas.id, "cleric", "burning-hands", 1)
            state.setAbility(thomas.id, cz.pfspells.data.Ability.CHA, 14)
            state.addTracker(thomas.id, "Wand of Cure Light Wounds", 50, cz.pfspells.data.ResourceUnit.POINTS, "charges")
            state.addTracker(thomas.id, "Pearl of Power (1st)", 1, cz.pfspells.data.ResourceUnit.PER_DAY, "")
            state.setBonusSpellCount(thomas.id, "cleric", "fireball", 1)
            val nyx = state.createCharacter("Nyx", "sorcerer", 4)
            state.setAbility(nyx.id, cz.pfspells.data.Ability.CHA, 18)
            state.setSubclass(nyx.id, "sorcerer", cz.pfspells.data.SubclassSlot.BLOODLINE, "arcane")
            listOf("detect-magic", "light", "magic-missile", "shield", "mage-armor", "invisibility")
                .forEach { state.addSpell(nyx.id, null, it) }
            val d = state.createCharacter("Elara", "wizard", 5)
            state.setAbility(d.id, cz.pfspells.data.Ability.INT, 16)
            state.setAbility(d.id, cz.pfspells.data.Ability.DEX, 14)
            state.setAbility(d.id, cz.pfspells.data.Ability.WIS, 12)
            state.setRace(d.id, "elf")
            state.setAlignment(d.id, cz.pfspells.data.Alignment.NG)
            state.setDeity(d.id, "Nethys")
            mapOf("spellcraft" to 5, "knowledge.arcana" to 5, "knowledge.planes" to 3, "perception" to 2, "linguistics" to 1)
                .forEach { (k, v) -> state.setSkillRanks(d.id, k, v) }
            state.updateCharacter(d.id) {
                it.copy(feats = listOf(
                    cz.pfspells.data.FeatPick("spell-focus", "Spell Focus", note = "evocation"),
                    cz.pfspells.data.FeatPick("scribe-scroll", "Scribe Scroll"),
                    cz.pfspells.data.FeatPick("improved-initiative", "Improved Initiative"),
                ))
            }
            state.setSubclass(d.id, "wizard", cz.pfspells.data.SubclassSlot.SCHOOL, "evocation")
            state.setSubclass(d.id, "wizard", cz.pfspells.data.SubclassSlot.OPPOSITION1, "enchantment")
            state.setSubclass(d.id, "wizard", cz.pfspells.data.SubclassSlot.OPPOSITION2, "illusion")
            mapOf("detect-magic" to 1, "light" to 1, "magic-missile" to 2, "shield" to 1, "mage-armor" to 1,
                "web" to 1, "invisibility" to 1, "fireball" to 1).forEach { (k, v) -> state.setSpellCount(d.id, null, k, v) }
            state.toggleFavorite("fireball"); state.toggleFavorite("magic-missile"); state.toggleFavorite("ablative-barrier")
            d
        }
        withContext(Dispatchers.Main) { state.ensureItems() }
        while (state.items !is ItemDbState.Ready) delay(50)
        val inv = withContext(Dispatchers.Main) {
            state.createInventory("Party stash")
            val i = state.createInventory("Elara")
            state.linkInventory(elara.id, i.id)
            val db = state.itemDb!!
            listOf("Longsword" to 1, "Shortbow" to 1, "Arrows (20)" to 1, "Leather armor" to 1, "Backpack, common" to 1,
                "Bedroll" to 1, "Rope, hemp (50 ft.)" to 1, "Torch" to 3, "Trail rations (per day)" to 5, "Bag of Holding (Type I)" to 1)
                .forEach { (n, q) -> (db.findByName(n) ?: db.items.firstOrNull { it.name.startsWith(n.substringBefore(" (").substringBefore(",")) })?.let { state.addItem(i.id, it, q) } }
            state.addCustomEntry(i.id, InvEntry("p", null, "Potion of Cure Light Wounds", ItemCategory.POTION.name, 2, 50.0, null, info = "1d8+1"))
            state.updateInventory(i.id) { it.copy(gp = 124, sp = 6, cp = 12, notes = "Chest at the Golden Stag inn – 2 scrolls, spare rope.") }
            state.saveCustomItem(cz.pfspells.data.CustomItem("p1", "Elven rope", ItemCategory.GEAR.name, 25.0, 3.0, "50 ft., DC 15 to break"))
            state.inventory(i.id)!!
        }
        // čarodějnice s hexy a orákulum se zjeveními (Foundry)
        withContext(Dispatchers.Main) { state.ensureClassAbilities() }
        while (state.abilityDb == null) delay(50)
        val morgana = withContext(Dispatchers.Main) {
            val m = state.createCharacter("Morgana", "witch", 10)
            state.setAbility(m.id, cz.pfspells.data.Ability.INT, 18)
            listOf("evil-eye", "slumber", "cackle", "misfortune", "agony").forEach { id -> state.abilityDb!!.get(id)?.let { state.addClassAbility(m.id, "witch", it) } }
            state.addClass(m.id, "oracle", 3)
            state.setSubclass(m.id, "oracle", cz.pfspells.data.SubclassSlot.MYSTERY, "flame")
            state.abilityDb!!.forClass("oracle").filter { "flame" in it.mysteries && it.uses != null }.take(1)
                .forEach { state.addClassAbility(m.id, "oracle", it) }
            state.character(m.id)!!
        }
        // půlčík s alternativními rasovými traity (AoN)
        withContext(Dispatchers.Main) { state.ensureRacialTraits() }
        while (state.traitDb == null) delay(50)
        val pip = withContext(Dispatchers.Main) {
            val p = state.createCharacter("Pip", "rogue", 3)
            state.setRace(p.id, "halfling")
            state.traitDb!!.forRace("halfling").filter { it.name in setOf("Halfling Jinx", "Underfoot Dodger") }.forEach { state.addRacialTrait(p.id, it) }
            state.character(p.id)!!
        }
        val aria = withContext(Dispatchers.Main) {
            val h = state.createCharacter("Aria", "sorcerer", 3)
            state.setRace(h.id, "human")
            state.traitDb!!.get("human:dual-talent")?.let { state.addRacialTrait(h.id, it) }
            state.setRacialTraitAbility(h.id, "human:dual-talent", 0, cz.pfspells.data.Ability.CHA)
            state.character(h.id)!!
        }
        // archetypy (AoN): paladinka se dvěma archetypy – Sacred Servant a Hospitaler nahrazují oba Smite Evil (konflikt)
        withContext(Dispatchers.Main) { state.ensureArchetypes() }
        while (state.archetypeDb == null) delay(50)
        val seelah = withContext(Dispatchers.Main) {
            val p = state.createCharacter("Seelah", "paladin", 6)
            state.addClass(p.id, "rogue_unchained", 2)
            state.archetypeDb!!.forClass("paladin").filter { it.name in setOf("Sacred Servant", "Hospitaler") }.forEach { state.addArchetype(p.id, "paladin", it) }
            state.character(p.id)!!
        }
        // Crossblooded čaroděj (dvě bloodline) + Eldritch Scion magus (spontánně, Cha, sloty jako bard)
        val kael = withContext(Dispatchers.Main) {
            val k = state.createCharacter("Kael", "sorcerer", 5)
            state.setSubclass(k.id, "sorcerer", cz.pfspells.data.SubclassSlot.BLOODLINE, "arcane")
            state.setSubclass(k.id, "sorcerer", cz.pfspells.data.SubclassSlot.BLOODLINE2, "draconic")
            state.archetypeDb!!.get("sorcerer:crossblooded")?.let { state.addArchetype(k.id, "sorcerer", it) }
            state.addClass(k.id, "magus", 3)
            state.archetypeDb!!.get("magus:eldritch-scion")?.let { state.addArchetype(k.id, "magus", it) }
            state.character(k.id)!!
        }
        // prestižní classa: wizard 5 / fighter 1 / eldritch knight 3 (zvyšuje sesílání kouzelníka)
        val ezren = withContext(Dispatchers.Main) {
            val z = state.createCharacter("Ezren", "wizard", 5)
            state.setAbility(z.id, cz.pfspells.data.Ability.INT, 18)
            state.addClass(z.id, "fighter", 1)
            state.addClass(z.id, "eldritch_knight", 3)
            state.character(z.id)!!
        }
        // familiár (kočka: +3 Stealth pánovi) a eidolon summonera s evolucemi
        val mittens = withContext(Dispatchers.Main) {
            state.createCompanion(ezren.id, "Mittens", cz.pfspells.data.CompanionKind.FAMILIAR, "animal", 1,
                cz.pfspells.data.CreatureSize.TINY, true, companionLevel = 5, familiar = "cat")
        }
        withContext(Dispatchers.Main) { state.ensureEidolonData() }
        while (state.eidolonData == null) delay(50)
        val doloran = withContext(Dispatchers.Main) {
            val b = state.createCharacter("Balazar", "summoner", 6)
            val e = state.createCompanion(b.id, "Doloran", cz.pfspells.data.CompanionKind.EIDOLON, "eidolon", 1, null, false,
                companionLevel = 6, eidolonForm = "serpentine")
            val evo = state.eidolonData!!.evolutions
            listOf("ability_increase", "improved_natural_armor", "reach", "constrict").forEach { id -> evo.firstOrNull { it.id == id }?.let { state.addEvolution(e.id, it) } }
            state.setEvolutionAbility(e.id, 0, cz.pfspells.data.Ability.STR)
            state.character(e.id)!!
        }
        // podtřídy z Foundry: patron (Morgana), spirit, blessingy warpriesta, doména inquisitora
        val kaya = withContext(Dispatchers.Main) {
            state.setSubclass(morgana.id, "witch", cz.pfspells.data.SubclassSlot.PATRON, "agility")
            val k = state.createCharacter("Kaya", "shaman", 5)
            state.setAbility(k.id, cz.pfspells.data.Ability.WIS, 16)
            state.setSubclass(k.id, "shaman", cz.pfspells.data.SubclassSlot.SPIRIT, "flame")
            state.setSubclass(k.id, "shaman", cz.pfspells.data.SubclassSlot.WANDERING_SPIRIT, "life")
            state.character(k.id)!!
        }
        val brannoc = withContext(Dispatchers.Main) {
            val b = state.createCharacter("Brannoc", "warpriest", 10)
            state.setDeity(b.id, "Iomedae")
            state.setSubclass(b.id, "warpriest", cz.pfspells.data.SubclassSlot.BLESSING1, "glory")
            state.setSubclass(b.id, "warpriest", cz.pfspells.data.SubclassSlot.BLESSING2, "war")
            state.addClass(b.id, "inquisitor", 3)
            state.setSubclass(b.id, "inquisitor", cz.pfspells.data.SubclassSlot.DOMAIN1, "sun")
            state.character(b.id)!!
        }
        val out = File("build/previews").apply { mkdirs() }

        suspend fun shot(name: String, w: Int, h: Int, overlay: (@Composable () -> Unit)? = null, nav: AppState.() -> Unit) {
            // detail postavy začíná na záložce Postava, pokud náhled neřekne jinak
            withContext(Dispatchers.Main) { state.characterTab = CharacterTab.CHARACTER; state.nav() }
            ImageComposeScene(w, h, Density(1f)) {
                App(state)
                overlay?.let { PfTheme { CompositionLocalProvider(LocalStrings provides state.strings) { it() } } }
            }.use { scene ->
                var t = 0L
                // 1,5 s – animace deníku (dopsání, zakroužkování se zpožděním) doběhnou do konečného stavu
                repeat(30) { scene.render(t); t += 50_000_000; delay(40) }
                val img = scene.render(t)
                File(out, "$name.png").writeBytes(img.encodeToData(EncodedImageFormat.PNG)!!.bytes)
            }
        }

        shot("phone-decks", 390, 844) { openRoot(Screen.Decks) }
        shot("phone-deck", 390, 844) { openRoot(Screen.CharacterDetail(elara.id)) }
        shot("phone-spell", 390, 844) { openRoot(Screen.CharacterDetail(elara.id)); showSpell("fireball", elara.id) }
        shot("phone-picker", 390, 844) { openRoot(Screen.CharacterDetail(elara.id)); push(Screen.Picker(elara.id)) }
        shot("desktop", 1440, 1000) { openRoot(Screen.CharacterDetail(elara.id)); showSpell("fireball", elara.id) }
        shot("desktop-compendium", 1440, 1000) { openRoot(Screen.Compendium) }
        shot("phone-inventory", 390, 844) { switchSection(Section.INVENTORY); push(Screen.InventoryDetail(inv.id)) }
        shot("phone-itempicker", 390, 844) { itemFilter = ItemFilter(category = ItemCategory.WEAPON); push(Screen.ItemPicker(inv.id)) }
        shot("desktop-inventory", 1440, 1000) { openRoot(Screen.InventoryDetail(inv.id)) }
        shot("desktop-itempicker", 1440, 1000) {
            openRoot(Screen.ItemPicker(inv.id)); itemFilter = ItemFilter(category = ItemCategory.WEAPON, proficiency = setOf("Simple", "Martial"), maxPrice = 50.0)
            showItem(itemDb!!.findByName("Longsword")!!.id, inv.id)
        }
        shot("desktop-wondrous", 1440, 1000) {
            openRoot(Screen.ItemPicker(inv.id)); itemFilter = ItemFilter(category = ItemCategory.WONDROUS)
            showItem(itemDb!!.items.first { it.name.startsWith("Bag of Holding") }.id, inv.id)
        }
        shot("phone-decks-inventory-tab", 390, 844) { switchSection(Section.INVENTORY) }
        shot("phone-about", 390, 844) { openRoot(Screen.Decks); push(Screen.About) }
        shot("desktop-about", 1440, 1000) { openRoot(Screen.About) }

        // čeština
        withContext(Dispatchers.Main) { state.changeLang(Lang.CS) }
        shot("cs-phone-decks", 390, 844) { switchSection(Section.SPELLBOOK) }
        shot("cs-phone-spell", 390, 844) { openRoot(Screen.CharacterDetail(elara.id)); showSpell("fireball", elara.id) }
        shot("cs-phone-inventory", 390, 844) { switchSection(Section.INVENTORY); push(Screen.InventoryDetail(inv.id)) }
        shot("cs-desktop", 1440, 1000) { openRoot(Screen.CharacterDetail(elara.id)); showSpell("fireball", elara.id) }
        shot("cs-desktop-inventory", 1440, 1000) {
            openRoot(Screen.InventoryDetail(inv.id)); showItem(itemDb!!.findByName("Longsword")!!.id, inv.id)
        }
        shot("cs-desktop-itempicker", 1440, 1000) {
            openRoot(Screen.ItemPicker(inv.id)); itemFilter = ItemFilter(category = ItemCategory.WEAPON, proficiency = setOf("Simple", "Martial"), maxPrice = 50.0)
        }
        shot("cs-phone-multiclass", 390, 844) { openRoot(Screen.CharacterDetail(characters.first { it.name == "Brother Thomas" }.id)) }
        shot("cs-desktop-multiclass", 1440, 1000) { openRoot(Screen.CharacterDetail(characters.first { it.name == "Brother Thomas" }.id)) }
        shot("phone-spontaneous", 390, 844) { characterTab = CharacterTab.SPELLS; openRoot(Screen.CharacterDetail(characters.first { it.name == "Nyx" }.id)) }
        shot("cs-phone-game-spontaneous", 390, 844) {
            val n = characters.first { it.name == "Nyx" }
            gameMode = true
            setCast(n.id, cz.pfspells.data.Character.slotKey("sorcerer", 1), 4)
            characterTab = CharacterTab.SPELLS; openRoot(Screen.CharacterDetail(n.id))
        }
        shot("cs-phone-game-prepared", 390, 844) {
            gameMode = true
            setCast(elara.id, cz.pfspells.data.Character.castKey("wizard", "magic-missile"), 1)
            setCast(elara.id, cz.pfspells.data.Character.castKey("wizard", "shield"), 1)
            characterTab = CharacterTab.SPELLS; openRoot(Screen.CharacterDetail(elara.id))
        }
        shot("cs-desktop-game", 1440, 1000) { gameMode = true; characterTab = CharacterTab.SPELLS; openRoot(Screen.CharacterDetail(elara.id)) }
        val thomasId = state.characters.first { it.name == "Brother Thomas" }.id
        shot("cs-phone-feats", 390, 1400) { gameMode = false; openRoot(Screen.CharacterDetail(thomasId)) }
        shot("cs-phone-featpicker", 390, 844) { openRoot(Screen.CharacterDetail(thomasId)); push(Screen.FeatPicker(thomasId)) }
        shot("cs-desktop-feat", 1440, 1000) { featFilter = FeatFilter(query = "power att"); openRoot(Screen.FeatPicker(thomasId)); showFeat("power-attack", thomasId) }
        shot("cs-phone-domains", 390, 2000) { gameMode = false; characterTab = CharacterTab.SPELLS; openRoot(Screen.CharacterDetail(thomasId)) }
        shot("cs-phone-bloodline", 390, 1300) { gameMode = false; characterTab = CharacterTab.SPELLS; openRoot(Screen.CharacterDetail(characters.first { it.name == "Nyx" }.id)) }
        shot("cs-phone-wizard", 390, 1700) { gameMode = false; openRoot(Screen.CharacterDetail(elara.id)) }
        // Deník dobrodruha: záložky rozdělovníku
        shot("cs-phone-tab-combat", 390, 844) { gameMode = false; characterTab = CharacterTab.COMBAT; openRoot(Screen.CharacterDetail(elara.id)) }
        shot("cs-phone-tab-spells", 390, 844) { gameMode = false; characterTab = CharacterTab.SPELLS; openRoot(Screen.CharacterDetail(elara.id)) }
        shot("cs-phone-tab-spells-play", 390, 844) {
            gameMode = true; characterTab = CharacterTab.SPELLS
            setCast(elara.id, cz.pfspells.data.Character.castKey("wizard", "magic-missile"), 1)
            openRoot(Screen.CharacterDetail(elara.id))
        }
        shot("cs-phone-combat-down", 390, 844) { gameMode = true; characterTab = CharacterTab.COMBAT; setDamage(elara.id, 18); openRoot(Screen.CharacterDetail(elara.id)) }
        shot("cs-phone-combat-down-after", 390, 844) { setDamage(elara.id, 0); gameMode = false; characterTab = CharacterTab.COMBAT; openRoot(Screen.CharacterDetail(elara.id)) }
        shot("cs-phone-tab-inventory", 390, 844) { gameMode = false; characterTab = CharacterTab.INVENTORY; openRoot(Screen.CharacterDetail(elara.id)) }
        shot("cs-desktop-tab-spells", 1440, 1000) { gameMode = false; characterTab = CharacterTab.SPELLS; openRoot(Screen.CharacterDetail(elara.id)); showSpell("fireball", elara.id) }
        shot("cs-phone-resources-game", 390, 1500) {
            gameMode = true
            setCast(thomasId, "res:cleric-channel", 2)
            characterTab = CharacterTab.COMBAT; openRoot(Screen.CharacterDetail(thomasId))
        }
        shot("cs-phone-myitems", 390, 844) { itemFilter = ItemFilter(mine = true); openRoot(Screen.Decks); push(Screen.InventoryDetail(inv.id)); push(Screen.ItemPicker(inv.id)) }
        shot("cs-desktop-myitem", 1440, 1000) {
            openRoot(Screen.ItemPicker(inv.id)); itemFilter = ItemFilter(mine = true); showItem("my-p1", inv.id)
        }
        shot("cs-phone-deitypicker", 390, 844, overlay = { cz.pfspells.ui.DeityPickerDialog("Torag", onDismiss = {}, onCustom = {}) { } }) { openRoot(Screen.Decks) }
        shot("cs-phone-racial-traits", 390, 844, overlay = {
            cz.pfspells.ui.RacialTraitsDialog(state, state.character(pip.id)!!) { }
        }) { openRoot(Screen.Decks) }
        shot("desktop-racial-traits", 1440, 1000, overlay = {
            cz.pfspells.ui.RacialTraitsDialog(state, state.character(pip.id)!!) { }
        }) { openRoot(Screen.CharacterDetail(pip.id)) }
        shot("cs-phone-race-panel", 390, 1000) { openRoot(Screen.CharacterDetail(pip.id)) }
        shot("cs-phone-archetypes-panel", 390, 1000) { gameMode = false; openRoot(Screen.CharacterDetail(seelah.id)) }
        shot("cs-phone-familiar", 390, 1100) { gameMode = false; openRoot(Screen.CharacterDetail(mittens.id)) }
        shot("cs-phone-eidolon", 390, 1400) { gameMode = false; openRoot(Screen.CharacterDetail(doloran.id)) }
        shot("cs-phone-tab-companions", 390, 844) { gameMode = false; characterTab = CharacterTab.COMPANIONS; openRoot(Screen.CharacterDetail(ezren.id)) }
        shot("cs-desktop-tab-companions", 1440, 1000) { gameMode = false; characterTab = CharacterTab.COMPANIONS; openRoot(Screen.CharacterDetail(ezren.id)) }
        // mazlíček záložku Mazlíčci nemá – zůstane na Postavě
        shot("cs-phone-companion-tabs", 390, 844) { characterTab = CharacterTab.COMPANIONS; openRoot(Screen.CharacterDetail(mittens.id)) }
        shot("cs-phone-prestige-panel", 390, 1000) { gameMode = false; openRoot(Screen.CharacterDetail(ezren.id)) }
        shot("cs-phone-prestige-spells", 390, 2600) { gameMode = false; characterTab = CharacterTab.SPELLS; openRoot(Screen.CharacterDetail(ezren.id)) }
        shot("cs-phone-prestige-dialog", 390, 844, overlay = {
            cz.pfspells.ui.PrestigeDialog(state, state.character(ezren.id)!!, cz.pfspells.data.ClassCatalog.get("eldritch_knight")!!.prestige!!) { }
        }) { openRoot(Screen.Decks) }
        shot("desktop-prestige-dialog", 1440, 1000, overlay = {
            cz.pfspells.ui.PrestigeDialog(state, state.character(ezren.id)!!, cz.pfspells.data.ClassCatalog.get("mystic_theurge")!!.prestige!!) { }
        }) { openRoot(Screen.CharacterDetail(ezren.id)) }
        shot("cs-phone-archetypes-casting", 390, 1400) { gameMode = false; characterTab = CharacterTab.SPELLS; openRoot(Screen.CharacterDetail(kael.id)) }
        shot("cs-phone-archetypes", 390, 844, overlay = {
            cz.pfspells.ui.ArchetypesDialog(state, state.character(seelah.id)!!, "paladin") { }
        }) { openRoot(Screen.Decks) }
        shot("desktop-archetypes", 1440, 1000, overlay = {
            cz.pfspells.ui.ArchetypesDialog(state, state.character(seelah.id)!!, "rogue_unchained") { }
        }) { openRoot(Screen.CharacterDetail(seelah.id)) }
        shot("cs-phone-dual-talent", 390, 844, overlay = {
            cz.pfspells.ui.RacialTraitsDialog(state, state.character(aria.id)!!) { }
        }) { openRoot(Screen.Decks) }
        shot("cs-phone-race-dual-talent", 390, 900) { openRoot(Screen.CharacterDetail(aria.id)) }
        shot("cs-phone-hexes", 390, 2600) { gameMode = false; openRoot(Screen.CharacterDetail(morgana.id)) }
        shot("cs-phone-hexpicker", 390, 844, overlay = {
            cz.pfspells.ui.ClassAbilityPickerDialog(state, state.character(morgana.id)!!, state.character(morgana.id)!!.classes[0],
                cz.pfspells.data.AbilityGroup.HEXES, onDismiss = {}) { }
        }) { openRoot(Screen.Decks) }
        shot("desktop-revelations", 1440, 1000, overlay = {
            cz.pfspells.ui.ClassAbilityPickerDialog(state, state.character(morgana.id)!!, state.character(morgana.id)!!.classes[1],
                cz.pfspells.data.AbilityGroup.REVELATIONS, onDismiss = {}) { }
        }) { openRoot(Screen.CharacterDetail(morgana.id)) }
        shot("cs-phone-feat-prereqs", 390, 1000) { openRoot(Screen.CharacterDetail(thomasId)); showFeat("cloak-of-stone", thomasId) }
        shot("cs-phone-database", 390, 844) { openRoot(Screen.Decks); push(Screen.Database) }
        shot("cs-phone-database-updates", 390, 1900) { openRoot(Screen.Decks); push(Screen.Database) }
        // dotaz před instalací: běžný (podpis sedí) a varování při jiném podpisu APK
        val apk = cz.pfspells.data.GithubAsset("PF-Spellbook-1.6.0.apk", "https://example/apk")
        val info = cz.pfspells.data.UpdateInfo("1.6.0", "https://example/release", apk)
        shot("cs-phone-update-confirm", 390, 844) {
            openRoot(Screen.Decks)
            update = UpdateState.ReadyToInstall(info, cz.pfspells.data.PreparedUpdate(apk, "x.apk", signatureMatches = true))
        }
        shot("cs-phone-update-mismatch", 390, 844) {
            update = UpdateState.ReadyToInstall(info, cz.pfspells.data.PreparedUpdate(apk, "x.apk", signatureMatches = false))
        }
        withContext(Dispatchers.Main) { state.update = UpdateState.Idle }
        // výběr classy s podtřídou (nová postava – klerik)
        val subIdx = (state.db as DbState.Ready).db.subclasses
        shot("cs-phone-classpicker-subclass", 390, 844, overlay = {
            ClassPickerDialog("Nová postava", "Vytvořit", withName = true, subclasses = subIdx, initialClassId = "cleric", initialRace = "half-elf", onDismiss = {}) { }
        }) { openRoot(Screen.Decks) }
        shot("cs-phone-classpicker-druid", 390, 844, overlay = {
            ClassPickerDialog(
                "Nová postava", "Vytvořit", withName = true, subclasses = subIdx, initialClassId = "druid",
                initialSubclass = mapOf(cz.pfspells.data.SubclassSlot.DOMAIN1 to "fire"), onDismiss = {},
            ) { }
        }) { openRoot(Screen.Decks) }
        shot("cs-desktop-classpicker-cleric", 1440, 1000, overlay = {
            ClassPickerDialog(
                "Nová postava", "Vytvořit", withName = true, subclasses = subIdx, initialClassId = "cleric",
                initialSubclass = mapOf(cz.pfspells.data.SubclassSlot.DOMAIN1 to "sun", cz.pfspells.data.SubclassSlot.DOMAIN2 to "law"),
                characterDeity = "Iomedae", onDismiss = {},
            ) { }
        }) { switchSection(Section.SPELLBOOK); openRoot(Screen.Decks) }
        shot("cs-phone-racepicker", 390, 844, overlay = { RacePickerDialog("dwarf", onDismiss = {}) { } }) { openRoot(Screen.Decks) }
        shot("cs-desktop-racepicker", 1440, 1000, overlay = { RacePickerDialog(null, onDismiss = {}) { } }) { openRoot(Screen.Decks) }
        val spellDb = (state.db as DbState.Ready).db
        shot("cs-phone-spirit", 390, 2900) { gameMode = false; characterTab = CharacterTab.SPELLS; openRoot(Screen.CharacterDetail(kaya.id)) }
        shot("cs-phone-spirit-game", 390, 2900) {
            gameMode = true
            setCast(kaya.id, cz.pfspells.data.Character.castKey("shaman", "spirit:1"), 1)
            characterTab = CharacterTab.SPELLS; openRoot(Screen.CharacterDetail(kaya.id))
        }
        shot("cs-phone-warpriest-subclass", 390, 844, overlay = {
            val b = state.character(brannoc.id)!!
            cz.pfspells.ui.SubclassDialog(state, spellDb, b, b.classes[0]) { }
        }) { gameMode = false; openRoot(Screen.Decks) }
        shot("cs-phone-inquisitor-subclass", 390, 844, overlay = {
            val b = state.character(brannoc.id)!!
            cz.pfspells.ui.SubclassDialog(state, spellDb, b, b.classes[1]) { }
        }) { openRoot(Screen.Decks) }
        shot("cs-phone-warpriest", 390, 1200) { openRoot(Screen.CharacterDetail(brannoc.id)) }
        shot("cs-phone-spirit-hexpicker", 390, 844, overlay = {
            val k = state.character(kaya.id)!!
            cz.pfspells.ui.ClassAbilityPickerDialog(state, k, k.classes[0], cz.pfspells.data.AbilityGroup.HEXES, onDismiss = {}) { }
        }) { openRoot(Screen.Decks) }
        shot("cs-phone-inquisition", 390, 844, overlay = {
            val b = state.character(brannoc.id)!!
            val e = b.classes[1].copy(subclass = mapOf(cz.pfspells.data.SubclassSlot.INQUISITION to "banishment"))
            cz.pfspells.ui.SubclassDialog(state, spellDb, b, e) { }
        }) { openRoot(Screen.Decks) }
        shot("cs-phone-witch-patron", 390, 2400) { characterTab = CharacterTab.SPELLS; openRoot(Screen.CharacterDetail(morgana.id)) }
        shot("cs-phone-shaman-subclass", 390, 1000, overlay = {
            val k = state.character(kaya.id)!!
            cz.pfspells.ui.SubclassDialog(state, spellDb, k, k.classes[0]) { }
        }) { openRoot(Screen.Decks) }
        shot("cs-phone-classpicker-witch", 390, 844, overlay = {
            ClassPickerDialog(
                "Nová postava", "Vytvořit", withName = false, subclasses = subIdx, initialClassId = "witch",
                initialSubclass = mapOf(cz.pfspells.data.SubclassSlot.PATRON to "agility"), onDismiss = {},
            ) { }
        }) { openRoot(Screen.Decks) }
        // nasazené předměty jako obrysy na postavičce
        val gear = state.createInventory("Výbava")
        fun eq(name: String, cat: cz.pfspells.data.ItemCategory, slot: cz.pfspells.data.ItemSlot?) =
            cz.pfspells.data.InvEntry(id = "eq-" + name.hashCode(), name = name, category = cat.name, equipped = true, slot = slot)
        state.updateInventory(gear.id) {
            it.copy(entries = listOf(
                eq("Helm of Comprehend Languages", cz.pfspells.data.ItemCategory.WONDROUS, cz.pfspells.data.ItemSlot.HEAD),
                eq("Cloak of Resistance +1", cz.pfspells.data.ItemCategory.WONDROUS, cz.pfspells.data.ItemSlot.SHOULDERS),
                eq("Amulet of Natural Armor +1", cz.pfspells.data.ItemCategory.WONDROUS, cz.pfspells.data.ItemSlot.NECK),
                eq("Full Plate", cz.pfspells.data.ItemCategory.ARMOR, cz.pfspells.data.ItemSlot.ARMOR),
                eq("Belt of Giant Strength +2", cz.pfspells.data.ItemCategory.WONDROUS, cz.pfspells.data.ItemSlot.BELT),
                eq("Bracers of Armor +1", cz.pfspells.data.ItemCategory.WONDROUS, cz.pfspells.data.ItemSlot.WRISTS),
                eq("Ring of Protection +1", cz.pfspells.data.ItemCategory.RING, cz.pfspells.data.ItemSlot.RING),
                eq("Boots of Elvenkind", cz.pfspells.data.ItemCategory.WONDROUS, cz.pfspells.data.ItemSlot.FEET),
                eq("Longsword", cz.pfspells.data.ItemCategory.WEAPON, cz.pfspells.data.ItemSlot.MAIN_HAND),
                eq("Heavy Steel Shield", cz.pfspells.data.ItemCategory.ARMOR, cz.pfspells.data.ItemSlot.SHIELD),
            ))
        }
        shot("cs-phone-gear-outlines", 390, 844) { openRoot(Screen.Decks); push(Screen.InventoryDetail(gear.id)) }
        shot("cs-desktop-gear-outlines", 1440, 1000) { openRoot(Screen.InventoryDetail(gear.id)) }
        val mage = state.createInventory("Mág")
        state.updateInventory(mage.id) {
            it.copy(entries = listOf(
                eq("Wizard's Hat", cz.pfspells.data.ItemCategory.WONDROUS, cz.pfspells.data.ItemSlot.HEAD),
                eq("Robe of Bones", cz.pfspells.data.ItemCategory.WONDROUS, cz.pfspells.data.ItemSlot.BODY),
                eq("Goggles of Night", cz.pfspells.data.ItemCategory.WONDROUS, cz.pfspells.data.ItemSlot.EYES),
                eq("Quarterstaff", cz.pfspells.data.ItemCategory.WEAPON, cz.pfspells.data.ItemSlot.MAIN_HAND),
                eq("Dagger", cz.pfspells.data.ItemCategory.WEAPON, cz.pfspells.data.ItemSlot.SHIELD),
                eq("Ring of Feather Falling", cz.pfspells.data.ItemCategory.RING, cz.pfspells.data.ItemSlot.RING),
                eq("Ring of Swimming", cz.pfspells.data.ItemCategory.RING, cz.pfspells.data.ItemSlot.RING),
            ))
        }
        shot("cs-desktop-gear-mage", 1440, 1000) { openRoot(Screen.InventoryDetail(mage.id)) }
        // zvířata: postavička podle tvaru (čtyřnožec / pták / had), sloty podle PF1, sedlo podle jména
        val horse = state.createCompanion(seelah.id, "Hrom", cz.pfspells.data.CompanionKind.MOUNT, "animal", 4,
            cz.pfspells.data.CreatureSize.LARGE, true, species = "horse", companionLevel = 4)
        val horseInv = state.createInventoryFor(horse.id)!!
        state.updateInventory(horseInv.id) {
            it.copy(entries = listOf(
                eq("Chain Barding", cz.pfspells.data.ItemCategory.ARMOR, cz.pfspells.data.ItemSlot.ARMOR),
                eq("Collar of the True Companion", cz.pfspells.data.ItemCategory.WONDROUS, cz.pfspells.data.ItemSlot.NECK),
                eq("Horseshoes of Speed", cz.pfspells.data.ItemCategory.WONDROUS, cz.pfspells.data.ItemSlot.FEET),
                eq("Military Saddle", cz.pfspells.data.ItemCategory.GEAR, null),
                eq("Saddlebags", cz.pfspells.data.ItemCategory.GEAR, null),
                eq("Bit and Bridle", cz.pfspells.data.ItemCategory.GEAR, null),
            ))
        }
        shot("cs-desktop-gear-horse", 1440, 1000) { openRoot(Screen.InventoryDetail(horseInv.id)) }
        shot("cs-phone-gear-horse", 390, 844) { openRoot(Screen.Decks); push(Screen.InventoryDetail(horseInv.id)) }
        val raven = state.createCompanion(morgana.id, "Krákor", cz.pfspells.data.CompanionKind.FAMILIAR, "animal", 1,
            cz.pfspells.data.CreatureSize.TINY, false, companionLevel = 10, familiar = "raven")
        val ravenInv = state.createInventoryFor(raven.id)!!
        state.updateInventory(ravenInv.id) {
            it.copy(entries = listOf(
                eq("Hat of Disguise", cz.pfspells.data.ItemCategory.WONDROUS, cz.pfspells.data.ItemSlot.HEAD),
                eq("Amulet of Natural Armor +1", cz.pfspells.data.ItemCategory.WONDROUS, cz.pfspells.data.ItemSlot.NECK),
                eq("Feather Step Slippers", cz.pfspells.data.ItemCategory.WONDROUS, cz.pfspells.data.ItemSlot.FEET),
            ))
        }
        shot("cs-desktop-gear-raven", 1440, 1000) { openRoot(Screen.InventoryDetail(ravenInv.id)) }
        val viper = state.createCompanion(morgana.id, "Syčák", cz.pfspells.data.CompanionKind.FAMILIAR, "animal", 1,
            cz.pfspells.data.CreatureSize.TINY, false, companionLevel = 10, familiar = "viper")
        val viperInv = state.createInventoryFor(viper.id)!!
        state.updateInventory(viperInv.id) {
            it.copy(entries = listOf(
                eq("Circlet of Persuasion", cz.pfspells.data.ItemCategory.WONDROUS, cz.pfspells.data.ItemSlot.HEAD),
                eq("Amulet of Natural Armor +1", cz.pfspells.data.ItemCategory.WONDROUS, cz.pfspells.data.ItemSlot.NECK),
            ))
        }
        shot("cs-desktop-gear-viper", 1440, 1000) { openRoot(Screen.InventoryDetail(viperInv.id)) }
        shot("desktop-classpicker-wizard", 1440, 1000, overlay = {
            ClassPickerDialog("New character", "Create", withName = true, subclasses = subIdx, initialClassId = "wizard", onDismiss = {}) { }
        }) { changeLang(Lang.EN); openRoot(Screen.Decks) }
        node.removeNode()
    }
}
