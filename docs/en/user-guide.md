# User guide

[Čeština](../user-guide.md) | **English**

The app has two parts, switched with the **Spellbook | Inventory** tabs: on a phone they are under the
"PF1 Spellbook" title, on desktop in the top bar.

The UI is in **English or Czech** – switch with the **EN | CZ** button in the header (next to the title on a phone,
in the top bar on desktop). On first launch it follows the system language and your choice is remembered.
Names and descriptions of spells and items stay in the original (English); in Czech, classes and schools of magic
are shown in Czech (the class list also shows the English original, e.g. "Kouzelník (Wizard)").

## Screen layout

- **Phone (narrow window):** one screen after another, back with the arrow at top left
  (on Android also with the system Back button). The main action is the green button at bottom right.
- **Desktop / tablet in landscape (from 1000 px):** three columns:
  1. left – the list of characters or inventories,
  2. middle – the open character / inventory / picker,
  3. right – spell or item details (close with × or the **Esc** key).

## Spellbook

### Character
1. **New character** → name, race and deity (optional), class and level. The class picker has a search box, classes are
   grouped by spell levels (9 / 6 / 4 / no spells / NPC) and a preview of spell levels for the chosen level is shown below.
   Spellcasting classes carry tags for the kind of magic (*arcane / divine / psychic / extracts*) and the casting style
   (*prepared / spontaneous*, the arcanist both).
   If the chosen class has a subclass (cleric, druid, oracle, sorcerer, wizard), its choice appears right under the level –
   domains, mystery, bloodline or school. It is optional and can be set later.
2. **Multiclass:** in the character detail, panel **Classes** → **Add class** (non-casters too – fighter, rogue,
   swashbuckler…); the subclass of the added class can be chosen in the same dialog.
   Each class level is changed with a stepper, 20 levels in total at most; a class can be removed (×) together with its spells.
3. **Race** is chosen when creating the character or later at the top of the Classes panel (80 races in groups: core,
   featured, uncommon, other, with size, speed and ability modifiers). The race:
   - **adds its ability modifiers** – you enter base scores, the tiles show the result including race
     (with a breakdown below); human, half-elf and half-orc choose which ability gets +2,
   - gives humans and half-elves a **bonus feat** (Skill Focus for half-elves),
   - checks racial feat prerequisites and its size is copied to the carrying capacity of the linked inventory.
   **Alternate racial traits** (below the race, ~630 from Archives of Nethys): the dialog shows the race's default traits,
   the chosen alternate ones (× removes) and a list to add from – search, "Replaces: …", PFS, tap for text and source.
   A replaced default trait is struck through and stops counting for feat prerequisites (Halfling Jinx → no
   "halfling luck"); a human without Bonus Feat / Skilled loses the bonus feat / extra skill points. Two traits replacing
   the same trait are reported (strict mode does not allow it). Traits that change ability scores (Dual Talent, Keen
   Kitsune, Orc Atavism…) are applied to the abilities; where an ability is chosen, the trait has a picker and the race
   panel warns until it is set. Changing the race clears the choice; export carries it.
4. Below the classes is a summary – character level, **BAB** and saves (summed over all classes) – and the
   **abilities** STR–CHA (tap to edit, the modifier is shown next to the score).
5. Each spellcasting class has its own spell section with an **Add spells** button.

The old "decks" were converted to characters (with all their spells) on the first launch of the new version.

### Character inventory
- **Inventory** section in the character detail: **Link inventory** (an existing one) or **New inventory** (named after the character).
- A linked inventory takes **Strength for carrying capacity from the character** – changing STR on the character or the
  Strength stepper in the inventory updates both. The character shows a summary (weight, load), **Open** goes to the
  inventory, × unlinks it.
- An inventory belongs to at most one character (linking it to another character unlinks it from the first one);
  a shared "Party stash" can stay unlinked.
- **Equipped gear**: an inventory item's detail has an **Equipment** section – **Equipped**, a slot (head, headband, neck,
  belt, ring…) and **bonuses** (ability, save, AC or skill with a bonus type). Known magic items get the bonus prefilled from
  their name (Headband of Vast Intelligence, Belt of Giant Strength, Cloak of Resistance, Ring of Protection, Amulet of
  Natural Armor, Bracers of Armor…); add others with **Add bonus**.
- **Inventory detail**: on a phone there are **Equipment** and **Items** tabs. Equipment = a figure with slots in two
  columns (head, headband, eyes, neck, shoulders, chest, body | armor, belt, wrists, hands, 2 rings, feet), below it the
  main and off hand and slotless items; dots on the silhouette mark what is worn. Tapping a slot opens a picker: which
  item works, **Move the others to Carried**, **Equip another item** (on desktop a filled slot opens the item detail).
  When a slot holds more items than it fits, only the first works – a red banner with **Resolve** says so.
  The item list is split into **Equipped** (slot badge) and **Carried**; on desktop the sidebar has an "Equipped" filter.
- Bonuses of **equipped** items in the linked inventory apply to the character: abilities (and with them bonus spells,
  skill points, prerequisites, carrying capacity) and skill totals; saves and AC are shown as an "Equipment: …" summary
  under the character's inventory. Bonuses of the same type don't stack (the highest applies); dodge, untyped and
  circumstance do. Two items in one slot (three rings) are flagged in the inventory.

### Companions
- **Companions** section in the character detail: **Add companion** – name, kind (animal companion, mount, familiar,
  eidolon, pet), creature type and hit dice (**Animal** – d8, ¾ BAB, good Fortitude and Reflex; **Magical Beast** – d10,
  full BAB), size and "four legs".
- A companion has **its own sheet** like a character: abilities, feats (one per odd HD), skills (2 + Int per HD), trackers,
  its own inventory (size and four legs → carrying capacity – a horse's saddlebags) and it can take classes too
  (eidolon, NPC classes…).
- In the character list it sits indented under its owner; its sheet starts with a link to the owner and **Companion
  settings** (kind, size). Deleting a character keeps its companions – they become standalone characters.
- **Rarity** (common / uncommon / rare / unique) is just a tag for your own campaign – PF1 rules have none.
- **If it dies (return / replacement)**: text on the companion's sheet, prefilled with the official rule for its kind
  (animal companion 24 h of prayer, paladin's mount 30 days, familiar 1 week + ritual, eidolon the next day); write your
  own instead, **Default text** restores it.
- **Custom companion**: animal *Custom* with progression off = abilities, size, creature type and HD by hand.
- Exporting a character includes its companions (they stay linked after import).
- An **animal** from the list (17 Core Rulebook animal companions – wolf, horse, pony, dog, big cat, bear…) fills in the
  starting abilities, size, four legs, speed, attacks and special qualities. **Custom** = everything by hand.
- **Animal companion progression** (default for animal companions and mounts): from the **effective druid level**
  (suggested from the owner's classes – druid and hunter in full, ranger −3, for a mount also paladin and cavalier) the app
  derives HD, BAB, saves, feats, skill points, the **Str/Dex bonus**, **natural armor**, **bonus tricks** and special
  abilities (link, share spells, evasion, devotion, multiattack…). At 4th or 7th level the animal **advances** (e.g. a wolf
  at 7th level: Large, Str +8, Dex −2, Con +4, a stronger bite).
- Change the level with the stepper at the top of the companion's sheet; the bonuses are shown with the abilities
  ("Includes companion bonuses"). Add the **ability score increase** (+1 at 4th, 9th, 14th and 20th level) to the base
  score of your choice yourself.

### Subclasses (domains, bloodline, mystery, school)
- The subclass can be chosen already when creating the character or adding a class (see above).
- Change it later with the link under the class (e.g. **Domains: choose…**) – it opens the picker:
  - **Cleric**: 2 domains, each optionally with a subdomain (replaces some of the domain's spells) – only subdomains of
    the chosen domain are offered (e.g. for Fire: Arson, Ash, Smoke). If the character has a deity from the list, only its
    domains are offered (Iomedae: Glory, Good, Law, Sun, War) and only the subdomains the deity grants (Iomedae for Sun:
    Day, Light, Revelation); without a deity, or with a custom one, all of them. When a chosen domain / subdomain does not
    match the deity (e.g. after changing the deity), a red warning "Torag does not grant: Fire – choose again" appears under
    the class (tap it to open the picker).
  - **Druid**: one nature domain (Air, Animal, Earth, Fire, Plant, Water, Weather), optionally its subdomain (except Metal).
  - **Oracle**: mystery, **sorcerer**: bloodline.
  - **Witch**: patron, **shaman**: spirit, **bloodrager**: bloodline, **psychic**: discipline.
  - **Warpriest**: two blessings, **inquisitor**: one domain (optionally a subdomain) – both limited to the domains of a
    deity from the list (otherwise all); a mismatch with the deity shows the same red warning as for the cleric.
  - **Wizard**: specialization school (or universalist / elemental school) and opposition schools.
- The subclass dialog also lists the **Subclass powers** (domains, bloodlines, schools, blessings) with the level they are gained at.
- Cleric / druid: below the spells is a **Domain spells** section – one domain slot per spell level (count "x / 1").
- Oracle / sorcerer / bloodrager / psychic: **Mystery / Bloodline / Discipline spells** – known automatically from the
  matching level (bloodrager 7th, 10th, 13th, 16th; psychic 1st and then even levels), cast from slots in game mode.
- Witch: **Patron spells** from even levels – added to the familiar and prepared in normal slots: with +/− in the section
  (or Add in the spell detail); they then appear among the witch's spells at their level even if not on her spell list.
- An inquisitor can pick an **inquisition** instead of a domain (AoN, only inquisitions of her deity). Powers of spirits,
  bloodrager bloodlines, disciplines and inquisitions (AoN) are in the subclass dialog; those with uses also under Abilities.
  The psychic gets a **Phrenic Pool** counter by discipline (½ level + Wisdom / Charisma).
- A shaman can also pick a **wandering spirit**: from 4th level its spirit ability (greater from 12th, true from 20th) and its
  spells in spirit magic, from 6th level one of its hexes on top (two from 14th). It changes daily – switch it in the subclass dialog.
- Sorcerers and bloodragers see their **bloodline bonus feats** (AoN) under Feats and in the subclass dialog. Sorcerer bloodlines
  Foundry does not know (Aquatic, Accursed…) get their powers from AoN.
- Shaman: **Spirit magic** – one spontaneous cast per day on each spell level she can cast (game mode shows "x / 1 cast").
- Warpriest / inquisitor get no bonus spells, only powers (blessing uses share one pool – Blessings).
- A specialist wizard has "+1 school" at each spell level; spells of an opposition school have a rust ⚠ (preparing them
  takes 2 slots).
- The subclass is saved with the character and in exports.

### Limited-use abilities
- The **Limited-use abilities** section fills itself according to the character's classes: Smite Evil, Lay on Hands,
  Channel Energy, Rage, Bardic Performance, Ki Pool, Arcane Pool, Grit, Panache, Judgment, Challenge, Wild Shape, Fervor,
  Blessings, Bombs, Inspiration, Martial Flexibility, the wizard school power… The maximum is computed from class level and abilities.
- Subclasses add their limited-use powers, **including higher levels** – cleric and druid domains and subdomains
  (e.g. Fire Bolt 3 + Wisdom per day, Holy Lance from 8th level), sorcerer bloodlines (Draconic Claws, Breath Weapon
  from 9th level…) and the wizard school (Force Missile, Elemental Wall from 8th level). A subdomain replaces the power
  it changes. The name includes the subclass: "Fire Bolt (Fire)". The data comes from Foundry VTT; a subclass it does not
  know (some later bloodlines, Jungle, Inevitable) gets a generic 1st-level power (3 + Wisdom / Charisma per day).
- Chosen **hexes and revelations** with uses are added here automatically (see below).
- Hide an ability you do not need with ×, "Show hidden" brings it back.
- **Add counter** = your own entry (name, maximum, unit: per day / rounds per day / points, note) – racial abilities,
  magic items, wand charges…
- In **game mode** uses are ticked off (dots, − / + for large numbers), **Rest** restores everything.

### Hexes and revelations
- The witch and the shaman have a **Hexes** section in the character detail, the oracle a **Revelations** section.
  On the right is "chosen / available" by class level (witch 1 + 1 per even level, shaman at 2nd, 4th, 8th, 10th, 12th,
  16th, 18th and 20th, oracle at 1st, 3rd, 7th, 11th, 15th and 19th) plus one for each Extra Hex / Extra Revelation feat.
- The shaman can also pick the **hexes of her spirit** (AoN, ~80 hexes): they are at the top of the list, **Hexes of all
  spirits** shows the others too.
- **Add hex / Add revelation** opens the list from Foundry VTT (~120 hexes, ~300 revelations): search in name and text,
  each with its kind (hex, major hex, grand hex), Ex / Su / Sp, uses and minimum level. Tap to expand the full text
  and source. The oracle sees the revelations of their mystery, **All mysteries** shows the rest too.
- Major hexes come at 10th level, grand hexes at 18th, revelations by their text ("must be at least 7th level").
  Outside strict mode they can be chosen earlier (red "from level N"); strict mode allows neither more than available
  nor picks above the level.
- Tap a chosen hex to read its text, × removes it. Hexes and revelations are exported with the character.

### Defense and hit points
- The **Defense & hit points** section: **AC**, **touch** and **flat-footed** – 10 + armor + shield + Dex (limited by the
  max Dex of armor, shield or load) + size + natural armor (animals, Amulet of Natural Armor) + deflection + dodge + other
  bonuses from equipped gear + a manual **AC – other** (monk, feats, spells). A breakdown is shown below the tiles.
- Armor and shields from the database get their AC bonus, max Dex and armor check penalty filled in when added to an
  inventory – just equip them. The **armor check penalty** (armor + shield, or the worse load penalty) is applied to
  Strength and Dexterity skills.
- **Hit points**: 1st HD maximum, then average (half + 1), creature HD average; + Con per HD, Toughness and **extra HP**
  (favored class, traits). Enter rolled HP under **Adjust** (empty = automatic).
- The HP stepper: − takes damage, + heals; **Rest** heals 1 HP per character level.

### Feats
- The **Feats** section in the character detail shows "chosen / available": feats from levels (1st and every odd level) +
  class bonus feats (fighter, wizard, monk, magus…) + racial bonus feat (human, half-elf) + manual **Extra** (traits…).
- **Add feat** opens ~3,400 feats from Archives of Nethys: search, type, "Only feats I qualify for", PFS, full-text search.
  Each feat shows its prerequisite status: ✓ met, ✗ not met, ? check yourself. Race is checked when the character has one
  (a half-elf also counts as an elf and a human). The app never blocks anything – except in **strict mode**
  (menu ⋮ or the checkbox in the feat / spell picker): then you can't add a feat beyond your feat count or one with a ✗
  prerequisite ("?" is allowed), and only as many spells as the class may have on that spell level (prepared copies /
  spells known, the specialist's school slot; the casting ability must be high enough). Removing is always allowed and
  imports are not checked.
- Also checked: skill ranks, class features from classes and levels (channel energy, rage, ki pool…), **alignment** and
  **deity** (set at the top of the character panel under the race; the deity from a list of ~460 deities with alignment,
  pantheon and favored weapon – search also by area, e.g. "healing"; **Custom…** = homebrew deity as text), size and creature
  type from the race, Hit Dice, base saves, armor and weapon proficiencies, spellcasting (arcane / divine, a specific
  spell), domains and default racial traits.
- What the app cannot check (archetypes, special conditions) you confirm in the feat detail with
  **I have it** next to the prerequisite – the confirmation counts for every feat with the same prerequisite, **Undo** removes it.
- Tap a feat to open its detail with the full text and the prerequisites one by one. A character's feat can have a
  **note** (e.g. the chosen weapon for Weapon Focus).
- **Custom feat** = name + description (homebrew, racial abilities…), editable with the pencil in its detail.
- Feats from the database are not updated with a button (they are bundled with the app).
- **Metamagic**: a metamagic feat's detail shows how much it raises the spell slot; a character's spell detail has a
  **Metamagic** section with the resulting slot level for each of the character's metamagic feats (red when the character
  can't cast that level yet).

### Skills
- The **Skills** section in the character detail shows skills with ranks (total bonus and number of ranks) and, at top
  right, "spent / available" skill points.
- **Edit skills** shows all skills with rank steppers. Each shows its ability and "class skill" (a class skill of one of
  the classes or the race – +3 with at least 1 rank). Craft, Perform and Profession have kinds: **Add Craft** → "alchemy".
- Total = ranks + ability modifier (including race) + 3 for a class skill + racial bonus (e.g. elf +2 Perception).
  "—" = trained-only skill without ranks.
- Points: per class level the class's points + Int modifier (min. 1), human +1 per level; put the favored class bonus and
  others into **Extra**. Max. ranks in one skill = character level.
- Feat prerequisites like "Acrobatics 5 ranks" are checked once the character has at least one rank
  (until then "?", so a character without skills filled in does not get ✗ everywhere).

### Character spells
1. Each spellcasting class shows the range of spell levels it casts at its level (e.g. *Wizard 5 · spell levels 0–3*).
2. **Add spells** opens a picker that offers **only spells the class can cast at its level**:
   - search in names (optionally in descriptions too – "search descriptions too"),
   - school and spell level filters, sorting by name, level or school,
   - "hide added" hides spells the character already has, "favorites only" keeps favorites,
     "PFS legal only" only spells allowed in Pathfinder Society.
3. **+** next to a spell adds a copy; in the character adjust the counts with **− / +** (0 = remove).
   Spontaneous classes (sorcerer, bard, oracle…) and the arcanist have each spell once – removed with ×.
   Each spell level shows its limit on the right: "3 / 3+1 prepared" (base + ability bonus) or
   "2 / 4 known · 6+1 per day". Red = limit exceeded or casting ability too low.
4. **Game mode** (checkbox under the abilities) is for the table: preparing classes tick off cast copies
   (a cast spell is struck through), spontaneous classes and the arcanist have a row of slots per spell level and a
   **Cast** button at each spell. Cantrips are unlimited. **Rest** restores everything. Cast spells are saved with the character.
5. Tap a spell to open its detail: casting time, components, range, duration, save, SR, full description,
   mythic version, source and a link to Archives of Nethys.

**Favorites:** the star in the spell detail adds it to favorites (for all characters and the compendium).
Favorite spells have a small star in the lists.

You can change a class level at any time. If you lower it and a spell is no longer available,
it stays with the character with a ⚠ warning icon.

### Compendium
The book icon (phone) / **Compendium** button (desktop) – all spells with class, school and level filters.
On desktop you also get to the compendium by typing into the top search box.

## Inventory

### An inventory = the possessions of one character (or a group)
1. **New inventory** → name (e.g. "Elara" or "Party stash").
2. **Purse** – PP / GP / SP / CP coins; tap a coin to enter the amount.
3. **Load** – total weight including coins (50 coins = 1 lb) and the load by **Strength** (Core Rulebook table):
   - a bar split into light / medium / heavy sections with limit marks and the current weight,
   - a colored status label (**Light** olive, **Medium** ochre, **Heavy** rust, **Overloaded** red)
     with the penalties below (max Dex, check penalty, speed, run),
   - **Size** (creature size) and **Four legs** (quadruped – horse, dog) adjust the capacity,
   - when adding an item (or changing STR, coins…) moves the character to a heavier load, the app warns you.
4. Items are grouped by category (Weapons, Armor & shields, Adventuring gear, Potions…), each with
   quantity (− / +), weight, price and a short note (e.g. *1d8 / 19-20/×2*, *+6 AC, ACP −5*).
5. **Notes** – free text (keys, debts, where the chest is hidden…).

On desktop there is also a category list on the left – tap one to show only it. The top "Search item…" box filters the open inventory.

### Adding items – **Add item**
- Search in ~6,000 items, **category** filter, **Simple / Martial / Exotic** for weapons, and **max price** in gp.
- **+** adds one piece straight away. Tapping a row opens the detail (a bottom sheet on a phone) where you set the
  **quantity and a note** (e.g. *masterwork, found in crypt*) and press **Add to inventory**.
- **Custom item** – something that is not in the database (loot, special gear…): name, category, price, weight, short info.
  With **Save to my items** checked it is also saved to your own catalog.
- **My items** – your own catalog shared by all inventories: whatever you create once, you add anywhere with one **+**
  (custom items are on top in "All"). An item can be removed from the catalog in its detail; a custom inventory entry can be saved to the catalog.
- **From spell** – a potion, scroll or wand from a spell: choose the kind, spell, spell list (sets the level) and caster level,
  the app computes the price by the rules (potion 50 gp × level × CL, scroll 25 gp × …, wand 750 gp × … with 50 charges).

Tap an inventory entry to open its detail: change quantity, note, remove; custom items can be edited too.

### Hidden prices of expensive items
Items over **200 gp** are visible as usual, but their price only shows as **"??? gp"**
(and an inventory with such an item shows its total value as "worth ≈ ???").

**Reveal:** tap **10 times quickly anywhere on the screen** (at most ~0.7 s between taps).
The message *All prices revealed* appears. Another 10 taps hide the prices again (*High prices hidden*).
Revealing lasts only until the app is closed.

## Export and import

Works for characters, feats and inventories. The result is readable text (JSON) you can send any way you like.

| Action | Where |
|---|---|
| Export one character / inventory | **Share** icon (up arrow) in its detail |
| Export all | menu **⋮ → Export all characters / inventories** |
| Import | menu **⋮ → Import character / inventory** |
| Export / import feats only | **Export / Import** buttons in the character's Feats section |

- A **character** is exported whole: race, alignment, deity, classes and levels, abilities, skills, spells, feats (custom ones and
  notes too), confirmed prerequisites, "Extra", counters, defense settings (extra HP, rolled HP, AC – other) and companions.
  If it has a linked inventory, check **Include linked inventory** to bundle it (equipped gear included) –
  after import it is created and linked. Spells cast in game mode and damage taken are not exported.
- **Feats** are imported into the open character (added to the existing ones); feats the character already has and cannot
  take more than once are skipped. Feat import also accepts a full character export (it takes the feats from it).
- Old deck exports (from version 1.0) can still be imported – a deck becomes a character with one class.

- **Export:** *Copy* (to the clipboard) and *Save to file* (desktop) or *Share* (Android, iOS).
- **Import:** *Paste from clipboard* or *Open file* (desktop, Android), or paste the text by hand.
  You see a preview before confirming; spells missing from the database are listed and skipped.
  Imported characters/inventories are added as new ones (with "(imported)" when the name already exists) – nothing is overwritten.

## Database updates

Menu **⋮ → Spell & item database / update**:

- **Download latest spell database** – downloads the current spell list.
- **Download latest item database** – downloads the current weapon, ammunition and magic item tables.
- **Restore bundled version** – returns to the version bundled with the app.

Pathfinder 1e is a finished system, so updates mostly bring fixes. Characters and inventories stay –
spells are matched by id and name, inventory entries carry their own name, price and weight.

## Where my data lives

Everything stays on the device:

| Platform | Characters, inventories | Downloaded databases |
|---|---|---|
| Android | the app's SharedPreferences | the app's private folder |
| iOS | NSUserDefaults | the app's Documents folder |
| Desktop | Java Preferences (Windows: registry `HKCU\Software\JavaSoft\Prefs`) | `~/.pfspells/` |
| Web | the browser's `localStorage` (for that address) | the browser's IndexedDB |

Uninstalling the app (Android/iOS) or clearing the site data in the browser deletes your data – back it up with
**⋮ → Export all characters / inventories**. On the web, data is tied to the browser and the page address.

## Web version

The app also runs in the browser (Chrome/Edge 119+, Firefox 120+, Safari 18.2+). The first load downloads ~26 MB, later
loads come from the cache. Export saves a file to Downloads, import opens a file picker. Esc and the browser's Back
button return to the previous screen.
