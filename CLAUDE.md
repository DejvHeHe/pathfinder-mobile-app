# CLAUDE.md – PF1 Spellbook (Kotlin Multiplatform)

Poznámky pro Claude Code: kontext projektu, rozhodnutí, stav a pravidla spolupráce.
- Podrobná dokumentace: `README.md` (anglicky, hlavní – open-source styl, badge, screenshoty z `docs/images/`) + `README.cs.md` (česky),
  `CONTRIBUTING.md`, `docs/` (česky) + `docs/en/` (anglicky) – při změně upravit obě jazykové verze (user-guide, architecture, data, build, deploy, ios-install) – při změnách ji udržuj aktuální.
- Plánovaná práce: **`TODO.md`** (hlavní vylepšení + nápady) – hotové body odškrtávej.

## Pravidla spolupráce
- Uživatel komunikuje **česky**; **UI je dvojjazyčné EN / CZ** (přepínač v hlavičce) – každý text UI patří do `Strings.kt`
  (`EnStrings` + `CsStrings`), žádné řetězce natvrdo v obrazovkách. Data (spelly, itemy) zůstávají anglicky; classy a školy se překládají přes `Strings`.
  Komentáře v kódu můžou být česky.
- **Necommitovat ani nepushovat bez výslovného požádání.** Když požádá: commit do `main` (jeho workflow), tag `vX.Y.Z` jen na požádání.
- Uživatel má **vlastní VPS** (Ubuntu 24.04, Docker, vlastní nginx jako reverzní proxy, Umami na jiné doméně).
  Na VPS se **nic nekompiluje** (build ji položil) – web jen z hotového image z GHCR.
- Při úpravě UI ověřit náhledy (`PreviewRender`) a u nových znaků glyfy ve fontech (web nemá fallback fonty).

## Zadání (od uživatele)
- KMP appka **bez loginu**, vše **lokálně** (žádný vlastní server, data uživatelů jen v zařízení/prohlížeči).
- **Spellbook:** výběr spellů pro **Pathfinder 1e**; **decky** (postava) – podle **classy a úrovně** se nabízí jen dostupné spelly.
  Každý spell má popis + herní údaje. Spelly z **existující databáze**, **tlačítkem jdou stáhnout aktuální** (offline přibalená verze).
- **Inventář:** itemy z databáze (taky aktualizovatelné), mince, nosnost, poznámky, custom itemy, export/import.
  **Ceny nad 200 gp jsou skryté („??? gp“), 10 rychlých ťuknutí kamkoli na obrazovku je odhalí/skryje** (jen do zavření appky).
- Export/import decků i inventářů, design „Linkovaný zápisník“ podle návrhu uživatele, platformy Android, iOS, desktop, web.
- Rozhraní v **angličtině i češtině**, přepínač „EN | CZ“ v hlavičce (výchozí podle jazyka systému, volba se pamatuje).

## Stav (27. 11. 2026)
- ✅ 27. 11. 2026 (nevydáno): **archetypy z AoN** – `tools/build_archetypes.py` (tabulky `Archetypes.aspx?Class=X` + stránky
  archetypů, cache `build/archetype-cache`, ~25 min) → `files/Archetypes.json` (líně, `ensureArchetypes`). `ClassEntry.archetypes`
  (`ArchetypePick`), `ArchetypesLink` pod classou + `ArchetypesDialog`, konflikty `Archetypes.conflicts` (stejná položka `replaces`),
  přísný režim `StrictBlock.ArchetypeConflict`, export `ClassDto.archetypes`; unchained → archetypy základní classy, NPC žádné.
  **Mechanika** (`ArchetypeEffects.kt`): z `replaces` automaticky – předvyplněné schopnosti, podtřída (`ClassEntry.subclassKind`),
  triky, počty hexů / revelations / bonusových featů podle úrovní; ručně `ARCHETYPE_MODS` (`ArchetypeMods.kt`, 75 archetypů) – bez
  sesílání (`ClassEntry.caster`), o kouzlo méně (`ClassEntry.levelSlots`), jedna doména, doména paladina, nové schopnosti, bonusové featy.
  Místo `CasterClasses.get(e.classId)` / `subclassType(e.classId)` / `cls.levelSlots` u classy postavy používat `e.caster` /
  `e.subclassKind` / `e.levelSlots(cls, …)`. „Spells“ v Replaces automaticky nic nedělá (většinou změněné sesílání).
  Jiné sesílání `CastingChange` (spell list, atribut, druh, tabulka jiné classy přes `CasterClass.tableId`, tradice), kouzla navíc
  (`Archetype.spells` z generátoru, ~260; `CasterClass.extraSpells`), `ClassEntry.subclassLevel` (Sacred Servant −3),
  Crossblooded `SubclassSlot.BLOODLINE2`. Tradice pro prerekvizity z `CasterClass.tradition`. Testy 155 zelené.
- ✅ **Vydáno `v1.5.0`** (26. 11. 2026): schopnosti class z Foundry, alternativní rasové traity, podtřídy patron / spirit /
  bloodline bloodragera / disciplína / blessingy / doména a inquisition inquisitora, schopnosti podtříd z AoN, wandering spirit,
  bonusové featy bloodline – workflowy Release, Docker i CI zelené (ověřeno 26. 11. 2026).
- ✅ **Vydáno `v1.6.1`** (27. 11. 2026): menu „Databáze a aktualizace“, sekce aktualizace appky nahoře. Release, Docker zelené,
  APK podepsané stabilním klíčem. **Aktualizace v1.6.0 → v1.6.1 přímo z appky na telefonu uživatel otestoval – funguje.**
  Otevřené: Play Protect označil APK za škodlivé (nejspíš kvůli `REQUEST_INSTALL_PACKAGES` + novému klíči) – uživatel zatím
  oprávnění nechává; desktop po „Stáhnout instalátor“ appku nezavře (nápad: tlačítko „Stáhnout a zavřít appku“).
- ✅ **Vydáno `v1.6.0`** (27. 11. 2026): **aktualizace appky z GitHub Releases** – `data/AppUpdate.kt` (`AppUpdates`: API
  `releases/latest`, `parseVersion` / `isNewer` / `pickAsset` / `evaluate`; `expect val appUpdater`): Android stáhne APK do
  `cache/updates` a otevře instalátor (`AndroidAppUpdater`, FileProvider `${applicationId}.updates`, `REQUEST_INSTALL_PACKAGES`),
  desktop otevře stažení .msi / .dmg / .deb v prohlížeči, web a iOS nic. `APP_VERSION` / `IS_RELEASE_BUILD` generuje Gradle
  (`generateAppVersion`); automatická kontrola jen release build, max 1× za 24 h (`update_checked_at`, přeskočená verze
  `update_skipped_version`), banner `UpdateBanner`, sekce `AppUpdateSection` v Databázích. Soukromí v About doplněné.
  Instalace přes starou verzi na Androidu vyžaduje stabilní podpisový klíč (zatím debug klíč CI). Před instalací dialog
  `UpdateConfirmDialog` (stav `UpdateState.ReadyToInstall`, `PreparedUpdate`) s exportem postav / inventářů; Android po stažení
  porovná podpis APK s nainstalovanou verzí (`signatureMatches`) – při neshodě postup přes odinstalaci místo tlačítka Instalovat.
- ✅ **Vydáno `v1.4.0`** (24. 11. 2026): AC / dotyk / zaskočený, HP se zraněním, postih zbroje, metamagie u kouzel, doménové a bloodline schopnosti,
  inventář 2.0 podle návrhu v Claude Design (figurka se sloty, konflikty, záložky na mobilu), design brief `docs/design-brief.md`.
  Release a Docker zelené; CI testy padaly na kompilaci (`InventoryLinkTest` bez importu `scores`) – opraveno v `08289c8`,
  nové testy (DefenseTest, EquipmentTest, AnimalCompanionTest, StrictModeTest…) lokálně zelené (24. 11.); CI po pushi ověřit.
  Po vydání opraveno: export postavy nepřenášel `hpBonus` / `hpOverride` / `acMisc` (`CharacterDto`; zranění se záměrně neexportuje).
- ✅ 24. 11. 2026 (v1.5.0): **schopnosti class z Foundry** – `tools/build_class_abilities.py` (packs/class-abilities
  + názvy knih z `module/registry/sources.mjs`; Python cert → lokální ZIP stažený curlem) → `ClassAbilityTable.kt` (schopnosti domén,
  subdomén, 11 bloodline, škol kouzelníka s úrovní a vzorcem) + `files/ClassAbilities.json` (117 hexů, 303 zjevení s textem, líně).
  `FoundryFormula` vyhodnocuje vzorce Foundry; hexy / zjevení = `ClassEntry.abilities` (`AbilityPick`), panel `ClassAbilitiesPanel`,
  přísný režim `StrictRules.classAbilityBlock`, export v `ClassDto.abilities`. Testy 108+ zelené.
- ✅ 24. 11. 2026 (v1.5.0): **alternativní rasové traity z AoN** – `tools/build_racial_traits.py` (stránky ras, cache `build/race-cache`,
  Python AoN ověří) → `RacialTraitTable.kt` (výchozí traity 77 ras) + `files/RacialTraits.json` (632 alternativních, líně). `Character.racialTraits`
  (`RacialTraitPick`), dialog `RacialTraitsDialog` pod rasou, `PrereqRules.racialTrait` (vybraný ✓, nahrazený ✗), human Bonus Feat / Skilled,
  přísný režim `StrictBlock.RacialTraitReplaced`, export `CharacterDto.racialTraits`; úpravy atributů z traitů (`ABILITY_VARIANTS`,
  volba v `RacialTraitPick.abilities`, `Character.raceMod`; +2 k jednomu atributu se pak skryje – `RacialTraits.replacesRaceBonus`).
  Testy 115 zelené, náhledy Pip / Aria v `PreviewRender`. Další traity měnící atributy → doplnit ručně do `ABILITY_VARIANTS` (text AoN nejde spolehlivě číst).
  Desktop appku při nedostatku paměti systém zabíjí – znovu ji spouštět jen na požádání.
- ✅ 26. 11. 2026 (v1.5.0): **další podtřídy z Foundry** – `build_class_abilities.py` navíc generuje
  `SUBCLASS_SPELL_TABLE` (kouzla 51 patronů, 17 spiritů, 22 bloodline bloodragera, 23 disciplín z popisů, jména spárovaná se SpellList.json)
  a `BLESSING_DOMAINS` + schopnosti 47 blessingů (`blessing:x`). `SubclassType` PATRON / SPIRIT / BLOODRAGER_BLOODLINE / DISCIPLINE /
  BLESSINGS / INQUISITOR_DOMAIN, `SubclassIndex(spells, table)`, sloty `patron`, `spirit`, `discipline`, `blessing1/2` (bloodrager sdílí `bloodline`).
  Spirit magic v herním režimu pod klíčem `castKey(classId, "spirit:N")`. Warpriest / inquisitor podle božstva (`DEITY_DOMAIN_CLASSES`,
  `blessingAllowed`, `deityConflicts`). Dialog podtřídy ukazuje „Schopnosti podtřídy“. Testy 119 zelené, náhledy Kaya / Brannoc.
  Lokální ZIP Foundry pro generátor: curl do scratchpadu (Python neověří certifikát GitLabu).
- ✅ 26. 11. 2026 (v1.5.0): **schopnosti podtříd z AoN** – `tools/build_subclass_abilities.py` (stránky spiritů, bloodline
  bloodragera, disciplín, Inquisitions; cache `build/subclass-cache`, Python AoN ověří) → `SubclassAbilityTable.kt`
  (`AON_SUBCLASS_POWER_TABLE` 371 schopností, `DISCIPLINE_POOL_ABILITY`, `INQUISITION_DEITIES`) + `files/SubclassAbilities.json`
  (82 hexů duchů, `ClassAbility.spirits`, `AbilityRules.matchesSpirit`). Inquisitor: `SubclassSlot.INQUISITION` místo domény
  (`inquisitionAllowed`), psychic preset Phrenic Pool, `AbilityRules.slots(c, e)` + Extra Hex / Extra Revelation, kouzla patrona
  připravitelná (`ClassEntry.spellLevelFor` / `patronSpellLevel` – detail postavy, přísný režim, detail kouzla).
  Wandering spirit šamana (`SubclassSlot.WANDERING_SPIRIT`, klíč `wspirit:x` = schopnosti ducha posunuté na 4. / 12. / 20.,
  `AbilityRules.wanderingHexes`, `matchesSpirits`); `BLOODLINE_BONUS_FEATS` (bloodrager + čaroděj, `SubclassPowers.bonusFeats`),
  schopnosti bloodline čaroděje z AoN tam, kde je Foundry nemá (Foundry má přednost; air/earth/fire/water → elemental). Testy 125 zelené.
- Odloženo uživatelem: CMB / CMD a útoky zbraní (appka zatím útoky nemá).
- Design: inventář 2.0 je hotový podle plátna uživatele v Claude Design (artboardy `Inv2_*` na stránce „Inventory 2.0 – figurka
  a equip sloty“); další v plánu je redesign detailu postavy – čeká na návrh (brief v `docs/design-brief.md`).
- 🟡 **Tag `v1.3.0`** (24. 11. 2026): ikona appky, Umami události, no-cache pro web, štítky druhu magie na kartách class, přísný režim,
  mazlíčci a zvířecí společníci (17 zvířat CRB, progrese, rarita, pravidla návratu), nasazené vybavení s bonusy – workflowy Release a Docker zatím neověřeny.
  Na Linux PC uživatele (`/home/davids/…`) se Gradle nespouští bez požádání; push přes SSH (`git@github.com:DejvHeHe/pathfinder-mobile-app.git`), HTTPS nemá přihlášení.
  Spuštění appky na požádání: `JAVA_HOME=~/.jdks/temurin-21.0.10 ./gradlew :composeApp:run` (na pozadí; hláška „Cannot create Linux GL context“ = jen softwarové vykreslování).
- ✅ **Vydáno `v1.2.0`** (rasy, skills, božstva, odvozené prerekvizity featů, podtřídy v pickeru) – workflowy Release, Docker i CI zelené (ověřeno 24. 11. 2026).
- ✅ **Vydáno `v1.1.0`** (postavy, featy, podtřídy…) – uživatel otestoval, vše v pořádku.
- ✅ 19. 11. 2026: výběr podtřídy rovnou v `ClassPickerDialog` (nová postava i přidání classy; vlastní `Dialog` – na širokém okně
  dva sloupce, max 620 dp), subdomény jen ke zvolené doméně, druid jen domény Nature Bond; docs po přechodu decky → postavy aktualizované.
- ✅ 19. 11. 2026: **rasa postavy** (`Character.race`, 80 ras z Foundry, výběr v panelu class) – rasové prerekvizity featů,
  velikost → nosnost propojeného inventáře, v exportu postavy (i `raceBonus`), výběr i v dialogu nové postavy (`ClassPick`).
  `Character.abilities` = základ, výsledné `Character.scores` (+ rasa, human/half-elf/half-orc +2 podle volby); human a half-elf +1 feat.
- ✅ 19. 11. 2026: **skills** (`Character.skills`, class skills z Foundry, skill body, prerekvizity na ranky; bez zadaných
  ranků „?“). Opraveno: „—“ = feat bez prerekvizit, zkratky knih u jmen featů, „…, or X“. Featů s „?“: 2 475 → 1 728.
- ✅ 19. 11. 2026: odvozené prerekvizity (`PrereqRules`), `Character.alignment`, `deity`, `confirmedPrereqs` (tlačítko „Mám“),
  `ClassInfo.armorProf/weaponProf`, `RaceInfo.darkvision/lowLightVision`. Featů s „?“ u vyplněné postavy: 974.
  `tools/build_races.py [races.zip]` – lokální ZIP, když Python neověří certifikát GitLabu (curl funguje).
- ✅ 19. 11. 2026: božstvo ze seznamu (AoN, přesvědčení → „worshiper of a good deity“, „within one step of your deity“), vlastní text dál možný;
  klerik volí domény i subdomény jen svého božstva (`SubclassFields(deity = …)`, `DeityInfo.subdomains`; nesoulad → `ClassEntry.deityConflicts`, `DeityConflictWarning`), božstvo i v dialogu nové postavy (`ClassPick.deity`).
  `tools/build_deities.py [DeitiesByGroup.html] [--cache DIR]` – stránky božstev jde stáhnout curlem (`--rate 1/s -K cfg`), Python neověří certifikát.
- ✅ **Vydáno `v1.0.1`** (GitHub Release): APK, MSI, DMG, DEB, web zip – všechny joby workflowu Release zelené.
  (Od `v1.6.0` je APK podepsané stabilním klíčem ze secrets – starší verze s debug klíčem je nutné jednou přeinstalovat.)
  Tag `1.0.0` (bez „v“) je starý ruční release s debug APK – workflow na něj nereaguje.
- ✅ **Web běží na VPS uživatele** z image `ghcr.io/dejvhehe/pf-spellbook-web:edge` (`.env`: `TAG=edge` + `UMAMI_*`), za jeho nginx na `127.0.0.1:8888`.
  Image `:edge`, `:latest`, `:1.0.1` jsou veřejné.
- ✅ Desktop: testy (`desktopTest`, 100 testů) zelené (ověřeno 24. 11. 2026 – i nové testy z v1.4), appka běží.
- ✅ 18. 11. 2026: deck → **postava** (multiclass, podtřídy, sloty/known, herní režim, featy z AoN, schopnosti s počtem použití,
  propojení s inventářem, export/import postav a featů). Další kroky v `TODO.md` (rasy, skills, archetypy, prestižní classy).
- ✅ Android: release APK se staví, otestováno na skutečném zařízení.
- ✅ iOS: Kotlin část ověřena v CI (`linkDebugFrameworkIosSimulatorArm64` na macOS runneru), Xcode build na Macu otestován
  (návod pro kamarády: `docs/ios-install.md`).
- Vývojový PC: Windows, `JAVA_HOME` = `%USERPROFILE%\.jdks\temurin-21.0.11` (systémová Java je 8; Android Studio má JBR 25,
  které Gradle 8.10.2 neumí → Gradle JDK pro Studio v `.gradle/config.properties` `java.home`, mimo git), `local.properties` → `%LOCALAPPDATA%\Android\Sdk`.
  Docker ani `gh` nejsou nainstalované. Port 8080 drží služba NI Application Web Server (National Instruments) – pro lokální test webu jiný port.
## Stack
- Kotlin 2.1.0, Compose Multiplatform 1.7.3, Material 3, AGP 8.7.3, Gradle wrapper 8.10.2
- kotlinx-serialization-json 1.7.3, kotlinx-coroutines 1.9.0, multiplatform-settings-no-arg 1.3.0 (decky, inventáře)
- Ktor client 3.0.3 (OkHttp – Android/desktop, Darwin – iOS, Js – web), material-icons-core
- Targety: `androidTarget`, `jvm("desktop")`, `iosX64/iosArm64/iosSimulatorArm64` (framework `ComposeApp`, static), `wasmJs` (web)
- Verze v `gradle/libs.versions.toml`; verze appky `-PappVersion=X.Y.Z`, Android `-PversionCode=N` (CI z tagu / čísla běhu)

## Struktura
```
composeApp/src/commonMain/kotlin/cz/pfspells/
  App.kt                  PfTheme, mobil (<1000 dp → Router) vs. široké okno (WideLayout), detektor 10 ťuknutí, toast
  AppState.kt             jediný stav appky: back stack Screen, Section, decky, inventáře, filtry, DbState/ItemDbState, skryté ceny, jazyk
  Strings.kt              texty UI: interface Strings, EnStrings, CsStrings, Lang, LocalStrings / `str`
  data/Model.kt           Spell, Deck (jen migrace a starý exportní formát)
  data/Character.kt       Character (+ mazlíček: ownerId, CompanionKind, size, quadruped), ClassEntry, Abilities, FeatPick, Spellbook (pohled postava + classa), CharacterStore + migrace
  data/AnimalCompanions.kt  CompanionProgression (tabulka CRB podle efektivní úrovně druida), SpeciesCatalog (17 zvířat CRB, vylepšení na 4./7. úrovni),
                          `Character.withCompanion`, companionMod (do `scores`), naturalArmor, suggestedCompanionLevel
  data/Equipment.kt       nasazené vybavení: ItemSlot, ItemBonus (BonusTarget/BonusType), stackBonuses (pravidla PF1), ItemBonusGuess (podle jména),
                          zbroj/štít: AC + MAX_DEX + ARMOR_CHECK (ItemBonusGuess.of(Item)), zatížení → loadBonuses (UNTYPED) v `Character.gear`
                          `InvEntry.equipped/slot/bonuses` → `Character.gear` (AppState.updateInventory/linkInventory) → gearMod (do `scores`), gearSkill, gearSave, gearAc
  data/Defense.kt         AC / dotyk / zaskočený (`defense()`, sizeAcMod), HP (`maxHp`, `currentHp`; Character.hpBonus/hpOverride/damage/acMisc)
  data/Metamagic.kt       metamagické featy: `Metamagic.levels` (zvýšení slotu z textu featu), `ofCharacter`
  data/StrictMode.kt      přísný režim: StrictRules.spellBlock / featBlock → StrictBlock (AppState.strictMode, Settings `strict_mode`)
  data/CasterClasses.kt   27 sesílajících class, progrese spell levelů, canCast, CastingKind (prepared/spontánní/arcanist), MagicTradition (arcane/divine/psychic/extrakty – štítky v pickeru), atribut sesílání, sloty (LevelSlots, bonusSpells)
  data/CastingTable.kt    GENEROVANÉ `tools/build_casting.py` z tabulek class na AoN (spells per day, spells known / arcanist prepared)
  data/ClassInfo.kt       ClassInfo (BAB, záchrany, HD), ClassCatalog, ClassGroup, Character.stats(), abilityMod
  data/ClassTable.kt      GENEROVANÉ `tools/build_classes.py` z Foundry packs/classes (49 class vč. nesesílacích a NPC)
  data/Deities.kt        DeityInfo, DeityCatalog, `Character.deityInfo`; data/DeityTable.kt GENEROVANÉ `tools/build_deities.py` (AoN, 463 božstev)
  ui/DeityUi.kt          DeityPickerDialog (seznam + vlastní text)
  data/PrereqRules.kt    odvozené prerekvizity featů (class features z class a úrovní, přesvědčení, božstvo, velikost/typ, HD, záchrany, proficience, kouzla, domény, rasové traity)
  data/Skills.kt         SkillInfo, Skills (26 skillů, id jako Foundry, Craft/Perform/Profession „craft:alchemy“), isClassSkill, skillTotal, skillPoints
  data/Races.kt          RaceInfo, RaceGroup, RaceCatalog, `Character.raceInfo`; data/RaceTable.kt GENEROVANÉ `tools/build_races.py` (Foundry packs/races, 80 ras)
  data/RacialTraits.kt   alternativní rasové traity: RacialTrait, RacialTraitPick, RacialTraits (defaults, replaced, conflicts), keepsRacialTrait;
                         data/RacialTraitTable.kt GENEROVANÉ `tools/build_racial_traits.py` (AoN); ui/RacialTraitsUi.kt RacialTraitsLink + RacialTraitsDialog
  data/Subclasses.kt     podtřídy: SubclassKind/Type/Slot, SUBDOMAIN_DOMAINS (subdoména → doména, AoN), DRUID_DOMAINS, withSubclass, domainSpells, bonusSpellsByLevel, školní slot a opoziční školy kouzelníka,
                         patron / spirit / bloodline bloodragera / disciplína (SUBCLASS_SPELL_TABLE), blessingy warpriesta (BLESSINGS, blessingAllowed)
  data/CharacterTransfer.kt  export/import postav (`pf-spellbook-characters`) a featů (`pf-spellbook-feats`), import starých decků
  data/ClassFeatures.kt  FeaturePresets (Smite Evil, Rage, Ki Pool… – vzorce max použití), Resource, Character.resources()
                         (+ schopnosti podtříd z `SubclassPowers`, vybrané hexy / zjevení s počtem použití)
  data/ClassAbilities.kt SubclassPower / SubclassPowers.keysOf (podtřída → klíče `domain:fire`, `subdomain:archon@good`, `bloodline:x`,
                         `school:element:fire`), FoundryFormula (vzorce Foundry), ClassAbility / AbilityPick / AbilityRules (počty hexů a zjevení),
                         ClassAbilityRepository (ClassAbilities.json, líně – AppState.ensureClassAbilities)
  data/ClassAbilityTable.kt  GENEROVANÉ `tools/build_class_abilities.py` (Foundry packs/class-abilities)
  data/SubclassAbilityTable.kt  GENEROVANÉ `tools/build_subclass_abilities.py` (AoN: spirity, bloodline bloodragera, disciplíny, inquisitions)
  data/Archetypes.kt     archetypy class (Archetype, ArchetypePick, ArchetypeRepository – Archetypes.json líně, Archetypes.conflicts / baseClass / supports);
                         ui/ArchetypesUi.kt ArchetypesLink (pod classou, souhrn efektů archetypeEffectText) + ArchetypesDialog
  data/ArchetypeEffects.kt  mechanika archetypů (ArchetypeRules.effect → ArchetypeEffect; ClassEntry.caster / subclassKind / levelSlots)
  data/ArchetypeMods.kt  ručně zadané efekty známých archetypů (ARCHETYPE_MODS – doplňovat podle textu AoN)
  data/Feats.kt          Feat, FeatDatabase, FeatRepository (Feats.json, líně), FeatPrereqs (✓/✗/? z textu prerekvizit), featSlots (počet featů)
  data/CustomItems.kt     vlastní katalog itemů (CustomItem, CustomItemStore), SpellItemKind (lektvar/svitek/hůlka ze spellu)
  data/SpellParser.kt     SpellList.json → Spell
  data/Repositories.kt    SpellRepository (cache → přibalená verze, download), DeckStore, SpellDatabase
  data/DeckTransfer.kt    export/import decků
  data/Items.kt           Item, ItemCategory, ItemDatabase, Inventory, InvEntry, CarryingCapacity
  data/ItemParser.kt      ItemsBase.json + CSV → Item; Csv parser; decodeText (UTF-8 / cp1252)
  data/ItemRepository.kt  ItemRepository, InventoryStore, InventoryTransfer
  data/Platform.kt        expect: AppFiles (suspend), createHttpClient, deckFileIo, nowDisplayText, currentTimeMillis, trackEvent (Umami, jen web)
  data/AppUpdate.kt       kontrola aktualizací z GitHub Releases (AppUpdates, expect appUpdater); AppVersion.kt GENEROVANÉ Gradlem (generateAppVersion)
  ui/UpdateUi.kt          AppUpdateSection (obrazovka Databáze), UpdateBanner (po automatické kontrole)
  ui/Theme.kt             tokeny Nb.*, fonty, PfTheme, modifikátory (ruledPaper, dashedBorder…), NbIcons
  ui/Common.kt            NbScreen (kostra obrazovky) + společné komponenty
  ui/WideLayout.kt        třísloupcové rozložení (hlavička se záložkami | sidebar · hlavní · detail)
  ui/DeckListScreen.kt    úvod se záložkami Spellbook/Inventory, seznam postav, AppMenu (⋮), nová postava přes ClassPickerDialog
  ui/DeckDetailScreen.kt (detail postavy: classy, atributy, kouzla po classách), ui/ClassPicker.kt (výběr classy + podtřídy), SpellBrowserScreen.kt (picker + kompendium), SpellDetailScreen.kt
  ui/SkillsUi.kt         SkillsPanel (přehled skillů s ranky / úprava všech)
  ui/RaceUi.kt           RacePickerDialog, raceDetail (velikost · rychlost · úpravy atributů)
  ui/SubclassUi.kt       souhrn podtřídy, `SubclassFields` (sdílí picker classy i `SubclassDialog`), sekce bonusových kouzel (doménový slot, mystérium, bloodline)
  ui/ResourcesUi.kt      sekce „Schopnosti s počtem použití“ (předvyplněné + vlastní počítadla `Tracker`)
  ui/ClassAbilitiesUi.kt ClassAbilitiesPanel (hexy / zjevení u witch, shaman, oracle), ClassAbilityPickerDialog (hledání, mystéria, text)
  ui/FeatScreens.kt      FeatsPanel (sekce v detailu postavy), FeatPickerScreen, FeatDetailScreen, CustomFeatDialog
  ui/DefenseUi.kt        DefensePanel (AC, HP se stepperem zranění, nastavení)
  ui/EquipmentBoard.kt   inventář 2.0 (návrh „Inventory 2.0 – figurka a equip sloty“ v Claude Design): EquipmentBoard (sloty kolem siluety,
                         stavy dlaždic), SlotConflictBanners, SlotSheet (mobil bottom sheet / desktop dialog), SlotBadge
  ui/EquipmentUi.kt      EquipmentSection (detail položky inventáře), BonusDialog, souhrn vybavení postavy
  ui/CompanionUi.kt      CompanionsPanel (mazlíčci v detailu postavy), CompanionHeader (list mazlíčka), CompanionDialog
  ui/InventoryCommon.kt   formát ceny/váhy, skryté ceny, mince, LoadBar, SectionTabs, dialogy čísla/textu
  ui/InventoryScreen.kt   seznam inventářů, InventorySidebar, detail inventáře
  ui/ItemScreens.kt       ItemPicker (mobil: bottom sheet), ItemDetail (item z DB / položka inventáře), CustomItemDialog
  ui/DeckTransferDialogs.kt  obecné TextExportDialog / TextImportDialog (decky i inventáře)
  ui/DatabaseScreen.kt    stav a aktualizace databáze spellů i itemů
  ui/AboutScreen.kt       About & licenses (GPL, Paizo CUP, OGL text)
composeApp/src/commonMain/composeResources/
  files/  SpellList.json, Feats.json (featy z AoN, jen přibalené), ClassAbilities.json (hexy a zjevení z Foundry), SubclassAbilities.json (hexy duchů z AoN), Archetypes.json (archetypy z AoN), RacialTraits.json (alternativní rasové traity z AoN), ItemsBase.json, Weapons.csv, Ammunition.csv, MagicItems.csv, OGL.txt
  font/   Alegreya Bold (+Italic), Alegreya Sans, IBM Plex Mono
composeApp/src/androidMain   MainActivity (AppFiles.init, AndroidDeckFileIo.register, Zpět → pop), Platform/DeckFileIo actualy
composeApp/src/desktopMain   main.kt (okno 1360×880, Esc → zpět), actualy (~/.pfspells, AWT FileDialog)
composeApp/src/iosMain       MainViewController.kt, actualy (Documents, UIActivityViewController)
composeApp/src/wasmJsMain    main.kt (ComposeViewport), Platform.wasmJs.kt (IndexedDB, Blob/input file), resources/index.html
composeApp/src/commonTest, desktopTest   testy + PreviewRender (náhledy do PNG)
iosApp/                Xcode projekt (SwiftUI obal)
tools/build_items.py   sestavení přibalených dat itemů (Python + pyyaml; jen pro vývojáře)
tools/build_feats.py   Feats.json z AoN (~3 400 stránek, 1 req/s, cache build/feat-cache – běh ~3 h, přerušitelný)
tools/build_casting.py, tools/build_classes.py, tools/build_races.py   CastingTable.kt (AoN), ClassTable.kt a RaceTable.kt (Foundry)
tools/build_class_abilities.py [zip] [sources.mjs]   ClassAbilityTable.kt + ClassAbilities.json (Foundry packs/class-abilities)
tools/build_racial_traits.py [--cache DIR]   RacialTraitTable.kt + RacialTraits.json (AoN stránky ras, ~80 req po 1 s)
tools/build_archetypes.py [--cache DIR]   Archetypes.json (AoN tabulky archetypů + stránky, ~1 300 req po 1 s)
tools/build_subclass_abilities.py [--cache DIR]   SubclassAbilityTable.kt + SubclassAbilities.json (AoN spirity, bloodline bloodragera, disciplíny, inquisitions)
docker/, Dockerfile, compose.yaml, compose.build.yaml   web v kontejneru
.github/workflows/     ci.yml, release.yml, docker.yml
```

## Navigace a stav
- Vlastní back stack `AppState.stack` (`sealed interface Screen`), žádná navigační knihovna:
  `Decks` (kořen, obsah podle `section`), `Compendium`, `Database`, `About`, `CharacterDetail`, `Picker(charId, classId)`, `SpellDetail(spellId, charId, classId)`,
  `InventoryDetail`, `ItemPicker`, `ItemDetail`, `FeatPicker(charId)`, `FeatDetail(featId, charId, pick)`. `SpellDetail`, `ItemDetail` a `FeatDetail` jsou „aside“ (pravý panel na širokém okně).
- `push`, `pop`, `openRoot` (stack `[Decks, x]` – boční panel), `showSpell`/`showItem` (nahradí otevřený detail), `popMain`.
- Široké okno (≥ 1000 dp) odvozuje sloupce ze stejného stacku → změna velikosti okna za běhu funguje.
- `section` (SPELLBOOK / INVENTORY) přepíná `SectionTabs`; `switchSection` vrací stack na kořen.
- Jazyk: `AppState.lang` (`Settings` klíč `ui_lang`, výchozí `Lang.system()` – cs/sk → CZ), `changeLang`; `App` poskytuje
  `LocalStrings`, v composable `str.xxx`, mimo composable funkce dostávají `Strings` parametrem. Hlášky v `AppState` jsou
  `UiMessage` (lambda nad `Strings`), chyby importu typované `ImportError`, `Load` a `SaveAction` jako enumy – text dodá UI.
- Postava ↔ inventář: `Character.inventoryId`; `linkInventory`, `createInventoryFor`, `setInventoryStrength` – STR se zrcadlí do `Inventory.strength` (nosnost), smazání inventáře postavu odpojí.
- Počítadla: `Character.trackers` (vlastní) + `FeaturePresets` podle class (skryté v `hiddenPresets`); použití v `castToday` pod „res:<id>“ → Odpočinek je vynuluje.
- Herní režim: `AppState.gameMode` (jen UI), seslané v `Character.castToday` – klíče `Character.castKey(classId, spellId)` (prepared kopie) a `slotKey(classId, level)` (spontánní/arcanist sloty), `rest()` vymaže.
- Filtry a scroll prohlížečů žijí v `AppState`. Android: `configChanges` + `AppState` v `by lazy` (přežije rotaci, ne process death).

## Data
- **Spelly:** `SpellList.json` z [SpellTable-pf1e](https://github.com/iociveteres/SpellTable-pf1e) (MIT, data z AoN), 3 030 spellů,
  parsování ~0,35 s při startu. `levels` = názvy spell listů ze zdroje (`sorcerer/wizard`, `cleric/oracle`…), výjimky sjednocuje
  `SpellParser.normalizeListKey`. `id` = slug jména. Ze zdroje se čtou i `access_ways.Domain/Subdomain/Mystery/Bloodline/Elemental School`
  → `Spell.subLevels` („domain:fire“ → level, `SpellParser.subclassKey` opravuje chyby zdroje), `SpellDatabase.subclasses` (`SubclassIndex`).
- **Itemy (~6 060):** `ItemsBase.json` (Foundry VTT PF1 – gear, zbroje, alchymie, zvířata; jen přibalené) +
  `Weapons.csv`, `Ammunition.csv`, `MagicItems.csv` (d20pfsrd přes Vireche/randomitemgenerator; **appka je umí stáhnout**).
  Načítají se líně (`ensureItems`). Přegenerování: `python tools/build_items.py`.
- Stažené verze se ukládají jako originální soubory do `AppFiles` (validace před uložením, poškozená cache → přibalená verze).
- **Postavy** `characters_v1` (při prvním spuštění migrace z `decks_v1`, starý klíč zůstává jako záloha), **inventáře** `inventories_v1` (JSON v `Settings`). Položka inventáře nese vlastní kopii jména/ceny/váhy,
  `itemId` jen odkazuje (custom item = `itemId null`, patří jen do svého inventáře). Změna schématu → nová verze klíče + migrace.
- Export/import (`data/CharacterTransfer.kt`): `pf-spellbook-characters` v1 (classy, atributy, spelly, featy, volitelně `inventory`), `pf-spellbook-feats` v1 (jen featy),
  starý `pf-spellbook-decks` v1 jde dál importovat (→ postava s jednou classou), `pf-spellbook-inventories` v1. Párování podle id, pak jména; importované se přidají jako nové.

## Pravidla PF1 v kódu
| Progression | Classy | Max spell level na úrovni L |
|---|---|---|
| PREPARED_9 | wizard, cleric, druid, witch, shaman | `min(9, (L+1)/2)` |
| SPONTANEOUS_9 | sorcerer, oracle, psychic, arcanist | `L<4 → 1`, jinak `min(9, L/2)` |
| SIX | bard, skald, inquisitor, magus, alchemist, investigator, summoner (+unch.), warpriest, hunter, mesmerist, occultist, spiritualist | `min(6, (L+2)/3)` |
| FOUR | paladin, antipaladin, ranger, bloodrager | `L<4 → žádné`, jinak `min(4, (L-1)/3)` |
| FOUR_KNACKS | medium | `L<4 → 0` (jen knacky), jinak `min(4, (L-1)/3)` |

- Ve výběru je 49 class (`ClassCatalog`), sesílání řeší 27 `CasterClasses`; classy bez spellů jen pro multiclass (BAB, záchrany).
  Navíc `ClassCatalog.creatures` = rasové HD tvorů pro mazlíčky (`animal`, `magical_beast`, skupina CREATURE) – ručně, ne z Foundry.
- Mazlíček = `Character` s `ownerId` (`AppState.createCompanion`, `companionsOf`, `rootCharacters`); smazání vlastníka ho osamostatní,
  export ho přibalí s `owner` = pořadí vlastníka v souboru. Velikost: `Character.creatureSize` (mazlíček `size`, jinak rasa).
  Adept (NPC classa) nemá spell list. Cantripy nemají FOUR, alchemist, investigator. Víc spell listů → nižší level.
- Nosnost `CarryingCapacity`: tabulka PF1, +10 STR = ×4, mince 50 ks = 1 lb; násobitel podle `CreatureSize`
  (dvounohý / čtyřnohý, uložen v `Inventory.size` + `quadruped`). `Load` nese postihy (max Dex, ACP, běh).
  `AppState.updateInventory` při zhoršení zatížení ukáže toast (`showToast`, `UiMessage`).
- Vlastní katalog itemů: `AppState.customItems` (Settings `custom_items_v1`), v pickeru „My items“ a nahoře v „All“;
  id itemu `my-<id>` (`Item.isCustom`, `AppState.findItem`). Do inventáře jde jako položka bez `itemId` (nezávislá na katalogu).
- Lektvar / svitek / hůlka ze spellu: `SpellItemKind` (50 / 25 / 750 gp × SL × CL, 0. level ½, max 3 / 9 / 4), `ui/SpellItemDialog.kt`.
- iOS: zpět tažením od levého okraje (`edgeSwipeBack` expect/actual, `Modifier.swipeFromEdgeBack` v `App.kt`).
- Spelly: `pfsLegal` ze zdroje; prohlížeč umí filtr PFS, oblíbené a řazení `SpellSort` (název / úroveň / škola).
  Oblíbené: `AppState.favorites` (Settings `favorite_spells_v1`, id oddělená čárkou), společné pro všechny decky.
- Classy a školy: data anglicky, v UI přes `Strings.className` / `school` / `subschool` / `spellListName`
  (CZ mapy v `Strings.kt`); `Strings.nameOf(cls)` v `ui/Common.kt`.

## Design „Linkovaný zápisník“
- Zdroj: designové plátno uživatele v Claude Design (artboardy „4a · Linkovaný zápisník“ mobil + desktop, „Inventory“, „Add item“).
- `ui/Theme.kt`: papír #E6D8BF s linkami po 28 dp, panel #F2E8D5, inset #EADCC2, štítek #DCCBAA, inkoust #2E2418/#5E4D38,
  olivová #4E5D2B = hlavní akce, rezavá #8A4B2A = nadpisy sekcí, čárkované okraje #9C8662, radius 4 dp.
- Fonty (OFL): Alegreya Bold/Bold Italic (statické instance z variabilního fontu přes fonttools), Alegreya Sans, IBM Plex Mono.
- Náhledy: `./gradlew :composeApp:desktopTest --tests "cz.pfspells.PreviewRender" -Ppreviews=true` → `composeApp/build/previews/*.png`
  (dočasné úložiště, skutečná data nemění).

## Web (Kotlin/Wasm)
- `AppFiles` → IndexedDB (localStorage má limit ~5 MB), `HttpClient(Js)`, export = stažení Blobu, import = `<input type=file>`, Esc = zpět.
- Build `wasmJsBrowserDistribution` → `composeApp/build/dist/wasmJs/productionExecutable/` (~26 MB, relativní cesty – funguje i v podsložce).
- **Bez fallback fontů** → v UI textech jen znaky z přibalených fontů (kontrola fontTools; „⋮“ v textu nepoužívat).
  Čeština (háčky, čárky, „ “, –, …) ve všech fontech je – ověřeno.
- Tlačítko Zpět prohlížeče = `pop()`: `syncBrowserHistory` v `wasmJsMain/main.kt` drží v historii jeden záznam
  na úroveň stacku (`pushState` s indexem, při zmenšení stacku `history.go(-n)`, Vpřed se vrací zpět).

## Vydání a nasazení
- Remote `https://github.com/DejvHeHe/pathfinder-mobile-app` (veřejné).
- `ci.yml`: push do main / PR → testy, Android, web, iOS framework (macOS runner).
- `release.yml`: tag `v*.*.*` → APK, MSI, DMG, DEB, web zip do GitHub Release. Ruční spuštění = jen artifacts, bez Release.
- `docker.yml`: push do main → `ghcr.io/dejvhehe/pf-spellbook-web:edge`; tag → `:X.Y.Z` + `:latest`.
- `Dockerfile`: temurin 21 build (prázdný `ANDROID_HOME` stačí) → stage `export` (jen soubory) / `runtime` (nginx 1.27 + `docker/nginx.conf` – gzip, index.html, .js/.wasm a composeResources `Cache-Control: no-cache` s revalidací přes ETag).
- `compose.yaml`: jen pull (žádný `build:`, žádný `pull_policy`), `127.0.0.1:${PORT:-8888}`, env `TAG`, `UMAMI_*`.
  Build na serveru jen vědomě přes `compose.build.yaml`. Ukázka pro hostitelskou nginx: `docker/proxy-nginx.example.conf`.
- Umami: `docker/40-umami.sh` (nginx entrypoint) nahradí `<!--UMAMI-->` v index.html podle `UMAMI_SCRIPT_URL/WEBSITE_ID/DOMAINS`
  (hodnoty jen v `.env` na serveru, ne v repu). Události z appky: `trackEvent(name, data)` (no-op mimo web) → `umami.track`;
  posílat jen druh akce a obecné údaje (nikdy jména, texty, id), seznam událostí v `docs/deploy.md` – nová událost → doplnit tam.
  `.gitattributes` drží LF u `*.sh` a `gradlew`; `gradlew` má exec bit.
- Podpis APK: env `ANDROID_KEYSTORE_FILE/_PASSWORD`, `ANDROID_KEY_ALIAS/_PASSWORD` (v CI ze secrets) – secrets nastavené (27. 11. 2026), keystore `pf-spellbook.jks` jen u uživatele (alias `pfspells`, CN=DejvHeHE); otisk SHA-256 certifikátu `f39818b489c14991eae01b8b9c15ca9d787e876361bfb792f6e888629ce1a64e` (ověření: `apksigner verify --print-certs`).
- iOS se do Release nedává (nutný Apple Developer účet) – kamarádi s Macem staví přes Xcode (`docs/ios-install.md`;
  bundle ID = `$(BUNDLE_ID)$(TEAM_ID)`, bez Android SDK si build fáze vytvoří prázdnou složku).

## Licence
- Kód **GPL-3.0** (`LICENSE`), Copyright (C) 2026 David Sobíšek (GPL kvůli datům z Foundry VTT PF1, které jsou GPL-3.0).
- Herní obsah: **OGL 1.0a + Paizo Community Use Policy** → appka musí zůstat **zdarma** (žádné poplatky ani reklamy).
- About & licenses začíná sekcí **Soukromí** (žádné osobní údaje, vše lokálně, síť jen při stažení databází, web může mít Umami) – při přidání čehokoli síťového ji aktualizovat.
- `licenses/MIT-SpellTable-pf1e.txt` (MIT zdroje kouzel – text je i v About). README má poznámku „AI-first / vibe-coded“.
- `NOTICE.md`, `licenses/OGL-1.0a.txt` (Section 15: 21 hlavních knih s autory – z Paizo PRD a d20pfsrd, ostatní souhrnně; kopie v `files/OGL.txt`), v appce About & licenses.
- Nový zdroj dat → doplnit do `NOTICE.md`, OGL Section 15 i `AboutScreen.kt`.

## Příkazy
```bash
./gradlew :composeApp:run                                   # desktop
./gradlew :composeApp:desktopTest                           # testy (+ -Ppreviews=true pro náhledy)
./gradlew :composeApp:assembleDebug                         # Android APK
./gradlew :composeApp:wasmJsBrowserDistribution             # web
./gradlew :composeApp:compileKotlinIosArm64 -Pkotlin.native.enableKlibsCrossCompilation=true   # kontrola iOS kódu na Windows
./gradlew :composeApp:packageDistributionForCurrentOS       # desktop instalátor
git tag vX.Y.Z && git push origin vX.Y.Z                    # vydání (jen na požádání uživatele)
```

## Konvence
- Package `cz.pfspells`; v `commonMain` žádné JVM-only API (`toSortedMap`, `String.format`…).
- Nepoužívat experimental `ExposedDropdownMenuBox` → `SimpleDropdown`; ikony jen icons-core / `NbIcons`