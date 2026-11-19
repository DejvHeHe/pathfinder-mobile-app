# Data

**Čeština** | [English](en/data.md)

Všechna data jsou **přibalená v appce** (`composeApp/src/commonMain/composeResources/files/`), takže appka funguje offline.
Část jde tlačítkem v appce stáhnout znovu ze zdroje.

| Soubor | Obsah | Zdroj | Aktualizace v appce |
|---|---|---|---|
| `SpellList.json` (6 MB) | 3 040 záznamů → 3 030 spellů s class listem | [SpellTable-pf1e](https://github.com/iociveteres/SpellTable-pf1e) `data/SpellList.json` (MIT, data z Archives of Nethys) | ano |
| `ItemsBase.json` (0,7 MB) | 1 541 položek: výstroj, oblečení, nástroje, jídlo, alchymie, byliny, zvířata, zbroje a štíty | [Foundry VTT PF1](https://gitlab.com/foundryvtt_pathfinder1e/foundryvtt-pathfinder1) `packs/items`, `packs/armors-and-shields` (OGL) | ne (jen přibalené) |
| `Weapons.csv` | 243 zbraní | [Vireche/randomitemgenerator](https://github.com/Vireche/randomitemgenerator) `data/weapons.csv` (d20pfsrd/AoN) | ano |
| `Ammunition.csv` | 36 druhů munice | tamtéž `data/ammunition.csv` | ano |
| `MagicItems.csv` (4 MB) | 4 241 magických itemů | tamtéž `data/magic_items_full - Updated 19Jan2020.csv` (d20pfsrd „magic_items_full“) | ano |

## Spelly

### Formát zdroje
Pole JSON objektů, mimo jiné:
`Name`, `School`, `Subschool`, `Descriptors`, `Casting time`, `Components`, `Range`, `Effect`, `Target`, `Duration`,
`Saving throw`, `Spell Resistance`, `Description` (odstavce oddělené `\n`), `Short description`, `Mythic`, `Source`,
`Url aon` a `access_ways.Level` = seznam `[spell list, level]`, např. `["sorcerer/wizard", 3]`.

### Převod (`SpellParser`)
- Hodnoty `"None"`, `"NULL"` a prázdné → `null`.
- `levels`: mapa spell list → level; výjimky ve zdroji se sjednotí (`wizard` → `sorcerer/wizard`, `oracle` → `cleric/oracle`,
  `summoner (unchained)` → `unchained summoner`). Při duplicitě platí nižší level.
- Spelly bez class listu (jen SLA, domény) se přeskočí.
- `id` = slug jména (`Abadar's Truthtelling` → `abadar-s-truthtelling`), kolize dostanou `-2`, `-3`.
- Parser odmítne soubor s ≤ 100 spelly (ochrana proti stažení nesmyslu).
- `pfsLegal` z pole `PFS legal` (~340 spellů není povolených v Pathfinder Society; chybí-li, bere se jako povolené).

Ze zdroje se čtou i domény, subdomény, mystéria, bloodline a elementární školy (`access_ways.*` → `Spell.subLevels`,
klíč „domain:fire“ → spell level). Zdroj má chyby, které parser opravuje: mezery a velká písmena („ war“, „Plant“),
překlepy („aristocra“, „kyton (evil, kaw)“), varianty „(u)“ a „juju(PZO…)“, slepené záznamy („Elemental School air“
v bloodline) se zahodí. Výsledek: 37 domén, 113 subdomén, 34 mystérií, 25 bloodlin, 7 elementárních škol.

| Classa | Podtřída | Bonusová kouzla |
|---|---|---|
| cleric | 2 domény (+ subdoména ke každé) | doménový slot: 1 kouzlo domény na spell level (`ClassEntry.bonusSpells`) |
| druid | doména Nature Bond – Air, Animal, Earth, Fire, Plant, Water, Weather (+ subdoména, ne Metal) | doménový slot jako cleric |
| oracle | mystérium | známé kouzlo levelu N od úrovně 2N |
| sorcerer | bloodline | známé kouzlo levelu N od úrovně 2N + 1 |
| wizard | škola / elementární škola + opoziční školy | +1 slot na spell level pro kouzla školy; opoziční kouzla s upozorněním (2 sloty) |

Subdoména nahrazuje kouzla své domény na levelech, které uvádí. Vazbu subdoména → doména zdroj neobsahuje – je ručně
v `Subclasses.kt` (`SUBDOMAIN_DOMAINS`, podle tabulky [aonprd.com/ClericDomains.aspx](https://aonprd.com/ClericDomains.aspx)).
Nabízí se jen subdomény zvolené domény (archon, azata, devil… patří ke dvěma doménám); změna domény zruší subdoménu,
která k nové doméně nepatří. Boreal a sleep v tabulce AoN nejsou, takže se nenabízí. Patron (witch), spirit (shaman), bloodline bloodragera a psychic discipline ve zdroji nejsou.

## Itemy

### Model (`Item`)
Společná pole: `name`, `category` (`ItemCategory`), `subtitle` („Martial · One-handed“, „Medium armor“, „Wondrous Item · neck“),
`priceGp`, `weightLb`, `description`, `source`. Podle druhu:
- zbraně: `proficiency`, `damageSmall`, `damageMedium`, `critical`, `range`, `damageType` (B/P/S → Bludgeoning/Piercing/Slashing), `special`
- zbroje: `armorBonus`, `maxDex`, `armorCheck`, `spellFailure`
- magické: `aura`, `casterLevel`, `slot`, `requirements`

`summary` je krátký text do inventáře: `1d8 / 19-20/×2`, `+6 AC, ACP −5`.

### Kategorie
`WEAPON, AMMO, ARMOR, GEAR, ALCHEMICAL, MOUNT, POTION, SCROLL, WAND, RING, ROD, STAFF, WONDROUS, MAGIC_WEAPON, MAGIC_ARMOR, ARTIFACT, OTHER`
(pořadí = pořadí skupin v inventáři).

| Zdroj | Mapování |
|---|---|
| Foundry složky | `adventuring-gear`, `clothing`, `food-and-drink`, `tools-and-skill-kits`, `animal-gear`, `material-components`, `miscellaneous` → GEAR (se subkategorií); `alchemical-items`, `herbs` → ALCHEMICAL; `animals-and-mounts` → MOUNT; `armor-*`, `shields` → ARMOR. Složka `magic-items` se přeskakuje (magické itemy jsou z CSV). |
| `Weapons.csv` | WEAPON (`is_ammunition` → AMMO), typ ruky z `is_light/one_handed/two_handed/ranged/unarmed` |
| `MagicItems.csv` sloupec `Group` | Wondrous Item → WONDROUS, Ring, Rod, Staff, Wand, Potion (+ názvy „Potion…“, „Oil of…“) → POTION, Weapon/Ammunition/Legendary Weapon/Shadow Piercing → MAGIC_WEAPON, Armor → MAGIC_ARMOR, Artifact → ARTIFACT, ostatní (Cursed, Magical Tattoo) → OTHER |

Aktuální počty: WEAPON 243, AMMO 36, ARMOR 70, GEAR 1379, ALCHEMICAL 72, MOUNT 20, POTION 9, WAND 1, RING 228, ROD 235,
STAFF 107, WONDROUS 2505, MAGIC_WEAPON 478, MAGIC_ARMOR 250, ARTIFACT 331, OTHER 96 – **celkem 6 060**.

Lektvary, svitky a hůlky v PF1 vznikají ze spellů, proto jich v databázi skoro není; v appce je pro ně „Custom item“.

### Převod (`ItemParser`)
- `Csv` – vlastní parser podle RFC 4180 (uvozovky, `""`, nové řádky uvnitř pole, BOM).
- `decodeText` – UTF-8, při neplatné sekvenci Windows-1252 (`weapons.csv` je v cp1252; rozbité apostrofy se opraví).
- Ceny: `PriceValue` nebo text „1,500 gp“ / „5 sp“ / „1 cp“ → gp jako `Double`. Váhy: „4 lbs.“, „1/2 lb.“ → lb.
- `id` = `kategorie-slug` (`weapon-longsword`, `magic-bag-of-holding-minor`), duplicity `-2`…
- `combine` spojí všechny zdroje a seřadí podle jména.

### Sestavení přibalených dat – `tools/build_items.py`
1. Stáhne ZIP archivy `packs/items` a `packs/armors-and-shields` z GitLab API Foundry PF1, převede YAML
   (cena ve správné měně → gp, váha, popis z HTML na text, AC/max Dex/ACP/ASF u zbrojí) do `ItemsBase.json`.
2. Stáhne `weapons.csv`, `ammunition.csv` a `magic_items_full…csv`, uloží je jako UTF-8; magické itemy ořízne
   jen na sloupce, které appka čte (originál má 10 MB kvůli HTML sloupci `FullText`).

```bash
pip install pyyaml
python tools/build_items.py
```

URL v `build_items.py` (`CSV_SOURCES`) a v `ItemParser` (`WEAPONS_URL`, `AMMO_URL`, `MAGIC_URL`) musí odpovídat.

### Aktualizace v appce (`ItemRepository.download`)
Stáhne tři CSV z GitHubu (`raw.githubusercontent.com/Vireche/randomitemgenerator/master/data/…`), naparsuje je
(validace), uloží do `AppFiles` (`items_weapons.csv`, `items_ammunition.csv`, `items_magic.csv`) a znovu sestaví databázi.
`ItemsBase.json` (Foundry) zůstává vždy přibalený – je to stovky malých YAML souborů a běžné vybavení PF1 se nemění.

## Pravidla PF1 v kódu

### Spell levely podle úrovně (`CasterClasses.kt`)

| Progrese | Classy | Max spell level na úrovni L |
|---|---|---|
| PREPARED_9 | wizard, cleric, druid, witch, shaman | `min(9, (L+1)/2)` |
| SPONTANEOUS_9 | sorcerer, oracle, psychic, arcanist | `L<4 → 1`, jinak `min(9, L/2)` |
| SIX | bard, skald, inquisitor, magus, alchemist, investigator, summoner (+unchained), warpriest, hunter, mesmerist, occultist, spiritualist | `min(6, (L+2)/3)` |
| FOUR | paladin, antipaladin, ranger, bloodrager | `L<4 → žádné`, jinak `min(4, (L-1)/3)` |
| FOUR_KNACKS | medium | `L<4 → 0` (jen knacky), jinak `min(4, (L-1)/3)` |

- Level 0 (cantripy/orisony/knacks) mají všichni kromě FOUR, alchemista a investigatora.
- Classy s více listy berou nižší level: hunter = hunter/druid/ranger, warpriest = warpriest/cleric,
  skald = skald/bard, investigator = investigator/alchemist, arcanist = sorcerer/wizard, oracle = cleric/oracle.
- Medium (Occult Adventures, Table 1-2): knacky od 1. úrovně, 1st na 4, 2nd na 7, 3rd na 10, 4th na 13.
- Vynechané: adept (NPC classa). Podtřídy viz výše (patroni, spirity zatím ne).

### Lektvary, svitky a hůlky ze spellu (`SpellItemKind`)
Cena podle Core Rulebooku: lektvar 50 gp × SL × CL (max 3. level), svitek 25 gp × SL × CL, hůlka 750 gp × SL × CL
(max 4. level, 50 nábojů); spell 0. levelu se počítá jako ½. Výchozí CL = minimum pro daný level (1, 1, 3, 5, 7 …),
level podle zvoleného spell listu. Drahé materiální složky appka jen připomene, k ceně je nepřičítá.

### Classy pro multiclass (`ClassTable.kt`)
`python tools/build_classes.py` (pyyaml) vygeneruje `data/ClassTable.kt` z Foundry VTT PF1 `packs/classes`:
49 class (základní, hybridní, occult, unchained a NPC) s progresí BAB (high = L, med = ¾ L, low = ½ L), dobrými
záchranami (2 + L/2, jinak L/3), HD a skill body. Multiclass = součet za každou classu. Prestižní classy zatím ne.

### Rasy (`RaceTable.kt`)
`python tools/build_races.py` (pyyaml) vygeneruje `data/RaceTable.kt` z Foundry VTT PF1 `packs/races`: 80 ras s velikostí,
základní rychlostí, rasovými úpravami atributů, typem a podtypy tvora. Human, half-elf a half-orc nemají ve Foundry pevné
úpravy – mají „+2 k jednomu atributu“ (`anyBonus`). Skupiny podle Advanced Race Guide: základní (7), významné (featured, 16),
neobvyklé (14), ostatní (zbytek – Bestiary, Alien…), seznamy jsou ve skriptu.

- `Character.abilities` jsou **základní** hodnoty, výsledné `Character.scores` = základ + `RaceInfo.mod` (u `anyBonus`
  podle `Character.raceBonus`). Výsledné hodnoty používají prerekvizity, sloty, schopnosti i nosnost; STR zadaná
  v inventáři se přepočte zpět na základ.
- Rasový bonusový feat (`RaceInfo.bonusFeats`): human 1, half-elf 1 (Adaptability – Skill Focus) → `FeatSlots.race`.
- Velikost rasy se propíše do propojeného inventáře (`Inventory.size`, nosnost) – při výběru rasy i při propojení.
- Rasa jako prerekvizita featu: text, který je jménem / id rasy nebo podtypem (elf, human, orc, dwarf, gnome, halfling),
  případně s „subtype“ nebo upřesněním v závorce. Rasa splňuje, když se jménem shoduje nebo má daný podtyp
  (`RaceInfo.countsAs`): half-elf platí jako elf i člověk, half-orc jako orc, drow jako elf, svirfneblin jako gnóm.
  „Human (Varisian)“ s rasou human = ověř ručně, s jinou rasou = nesplněno. Rasové traity („halfling jinx trait“) se neověřují.

### Skilly (`Skills.kt`)
26 skillů Core Rulebooku (Knowledge rozdělená na 10 specializací), id jako ve Foundry VTT PF1 (`disableDevice`,
`knowledge.arcana`); Craft / Perform / Profession mají druhy – klíč ranků `craft:alchemy`. Class skills jsou v `ClassTable.kt`
(`ClassInfo.classSkills` z Foundry `packs/classes`, „knowledge“ = všechny specializace; Background skills artistry / lore
se vynechávají), rasové bonusy a class skills v `RaceTable.kt` (`skillMods`, `classSkills`).

- Celkem (`skillTotal`) = ranky + mod. atributu (`scores`) + 3 za class skill s ≥ 1 rankem + rasový bonus;
  „trained only“ bez ranků = nelze použít. ACP zbroje se nepočítá.
- Skill body (`skillPoints`) = Σ úroveň classy × max(1, body classy + mod. INT) + člověk 1 / úroveň + `extraSkillPoints`
  (oblíbená classa, traity – ručně). Používá se aktuální INT (zvýšení INT během úrovní se nerozlišuje).
- Prerekvizity: „Acrobatics 5 ranks“, „5 ranks in Sense Motive“, „Knowledge (arcana, …, or religion) 5 ranks“,
  „Craft (any) 1 rank“, „1 rank in at least one Knowledge skill“, „Knowledge (nature) or Survival 5 ranks“ (číslo
  z poslední části platí pro všechny). Postava bez zadaných ranků → „ověř ručně“.

### Božstva (`DeityTable.kt`)
`python tools/build_deities.py [stránka.html]` vygeneruje `data/DeityTable.kt` z tabulky
[aonprd.com/DeitiesByGroup.aspx](https://aonprd.com/DeitiesByGroup.aspx): 463 božstev (jméno, panteon, přesvědčení,
oblasti, domény, oblíbená zbraň). Postava ukládá jméno (`Character.deity`), `DeityCatalog.get` ho najde bez ohledu na
velikost písmen; jiný text = vlastní božstvo (jméno se porovnává volně, přesvědčení je neznámé). Klerik s božstvem
ze seznamu volí domény jen z `DeityInfo.domains` a subdomény jen z `DeityInfo.subdomains` (ze stránek jednotlivých božstev
`DeityDisplay.aspx`, ~460 požadavků po 1 s, cache `build/deity-cache`; subdomény podmíněné traitem, „*“, se počítají).
`subdomains = null` = stránka chybí → nabídnou se všechny subdomény domény.

### Featy (`Feats.json`)
`python tools/build_feats.py` stáhne přehled [aonprd.com/Feats.aspx](https://aonprd.com/Feats.aspx) (jméno, prerekvizity,
shrnutí, PFS) a detail každého featu (typy, zdroj, flavor, sekce Benefit / Normal / Special…). ~3 400 požadavků po 1 s
(AoN odpovídá pomalu → ~3 h), stránky se cachují v `build/feat-cache`, takže přerušený běh pokračuje. Značky AoN ve jménech
(`*` mythic verze, `⊤`, `†`) se odstraní. Featy appka **neaktualizuje** (tolik požadavků z aplikace není rozumné).

Prerekvizity (`FeatPrereqs`): text se rozdělí podle čárek a středníků mimo závorky; ověřují se atributy („Str 13“),
BAB, úroveň postavy, úroveň classy („fighter level 4th“), caster level (nejvyšší úroveň sesílací classy), „ability to cast
Nth-level spells“, rasu (viz výše, když je u postavy vybraná) a featy postavy (u „Weapon Focus (longsword)“ stačí mít
Weapon Focus; „Weapon Focus with the chosen weapon“ a jména s přilepenou zkratkou knihy – „Improved Dirty TrickAPG“ –
se taky najdou). „—“ ve zdroji = feat bez prerekvizit (248 featů). „A or B“ = stačí jedna; společná přípona / předpona
platí pro všechny části („good or lawful alignment“, „Animal or Plant domain“, „Knowledge (nature) or Survival 5 ranks“).
Nic se nevynucuje.

Odvozená pravidla (`PrereqRules.kt`):
- **class features** – tabulka schopnost → classa a úroveň (channel energy, rage, ki pool, wild shape, sneak attack…,
  ~100 položek), u „channel energy 3d6“ / „sneak attack +2d6“ počet kostek; volitelné schopnosti (zvířecí společník
  druida, pozitivní / negativní energie klerika) = „?“. Schopnost z archetypu appka nezná → potvrdit ručně.
- **přesvědčení** (`Character.alignment`) – „good alignment“, „nonlawful“, „chaotic neutral“…; **božstvo** (`deity` = jméno
  ze seznamu nebo vlastní text) – „worshiper of Iomedae“, „must worship a deity“, „worshiper of a good deity“,
  „alignment must match / be within one step of your deity's“ (přesvědčení božstva ze seznamu),
- **velikost, typ a podtyp** podle rasy – „Small size or smaller“, „humanoid“, „giant subtype“; **Hit Dice** = úroveň,
  **základní záchrany** („base Fortitude save bonus +3“),
- **proficience** se zbrojí / štíty / martial zbraněmi z class (`ClassInfo.armorProf/weaponProf` z Foundry) a featů
  (Armor Proficiency…),
- **kouzla** – arcane / divine / psychic sesilatel, „ability to cast 3rd-level divine spells“, „ability to cast cleric spells“,
  konkrétní kouzlo („ability to cast dispel magic“ – je na spell listu sesílající classy a úroveň stačí),
- **domény** klerika / druida (i přes subdoménu), **typ featu** („any teamwork feat“),
- **rasové traity** – výchozí traity ras (stonecunning, keen senses…), darkvision / low-light vision z Foundry;
  když je rasa nemá, zůstane „?“ (mohla je získat alternativním traitem).

**Ruční potvrzení** (`Character.confirmedPrereqs`): co appka neověří nebo považuje za nesplněné, potvrdí uživatel
v detailu featu tlačítkem „Mám“ – platí pro všechny featy se stejným textem prerekvizity (export ho přenáší).

Podíl featů s „?“ (postava člověk se zadanými skilly, přesvědčením a božstvem): původně 2 475 → 974.

Počet featů (`featSlots`): 1. a každá lichá úroveň postavy + bonusové featy class (fighter 1 + sudé úrovně, wizard 5/10/15/20,
monk 1/2/6/10/14/18, magus 5/11/17, warpriest a inquisitor/hunter (teamwork) každá 3., brawler 2/5/8…, swashbuckler
a gunslinger každá 4., cavalier/samurai 6/12/18, ranger 2/6/10/14/18, sorcerer 7/13/19, bloodrager 6/9/12/15/18)
+ ruční „Navíc“ (human, traity…).

### Sloty a spells known (`CastingTable.kt`)
`python tools/build_casting.py` (jen standardní knihovna, ~27 požadavků po 1 s) stáhne stránky class z Archives of Nethys
a z jejich tabulek vygeneruje `data/CastingTable.kt`: pro každou z 27 sesílajících class spells per day a spells known
(arcanist: spells prepared). Řádek = úroveň classy 1–20, hodnota pro spell level 0–9, `-` = classa level nemá
(u spontánních tříd triky „libovolně“). Testy (`SlotTableTest`) hlídají shodu s progresí spell levelů a vzorky z CRB.

- `CastingKind`: PREPARED (limit = kopie podle slotů), SPONTANEOUS (limit = známá kouzla, sesílá ze slotů),
  HYBRID (arcanist – limit připravených, sesílá ze slotů). Spontánní/arcanist mají každé kouzlo jen jednou.
- Bonusová kouzla z atributu sesílání (CRB tab. 1–3): od modifikátoru ≥ spell level `(mod − level) / 4 + 1`, triky nemají.
  Kouzlo levelu N vyžaduje atribut ≥ 10 + N (UI jen upozorní). Occultist nemá tabulku known (kouzla dávají implementy).

### Nosnost (`CarryingCapacity`)
Heavy load podle STR 1–29 z Core Rulebooku (10 → 100 lb, 18 → 300 lb, 29 → 1 400 lb), nad 29 každých +10 STR ×4.
Light = ⌊heavy/3⌋, medium = ⌊heavy·2/3⌋. Mince: 50 ks = 1 lb.

Násobitel podle velikosti tvora (`CreatureSize`, uložený v inventáři jako `size` + `quadruped`):

| Velikost | Fine | Diminutive | Tiny | Small | Medium | Large | Huge | Gargantuan | Colossal |
|---|---|---|---|---|---|---|---|---|---|
| dvounohý | ×1/8 | ×1/4 | ×1/2 | ×3/4 | ×1 | ×2 | ×4 | ×8 | ×16 |
| čtyřnohý | ×1/4 | ×1/2 | ×3/4 | ×1 | ×1½ | ×3 | ×6 | ×12 | ×24 |

Postihy zatížení (`Load`): medium – max Dex +3, check penalty −3, běh ×4; heavy – max Dex +1, check −6, běh ×3;
obojí rychlost 30 → 20 ft (20 → 15 ft). Přetížení – jen 5 ft za kolo, bez Dex bonusu k AC.
