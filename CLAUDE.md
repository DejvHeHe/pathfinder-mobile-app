# CLAUDE.md – PF1 Spellbook (Kotlin Multiplatform)

Poznámky pro Claude Code: kontext projektu, rozhodnutí, stav a co dál.
Podrobná dokumentace je v `README.md` a `docs/` (user-guide, architecture, data, build) – při změnách ji udržuj aktuální.

## Zadání (od uživatele)
- KMP appka **bez loginu**, vše **lokálně**.
- Výběr spellů pro **Pathfinder 1e**.
- **Decky** (postava/build) – podle **classy a úrovně** se nabízí jen dostupné spelly, ze kterých se deck skládá.
- Každý spell musí mít **popis + herní údaje** (casting time, components, range, duration, save, SR…).
- Spelly se berou z **existující databáze**, ne ručně psané; **tlačítkem jdou stáhnout aktuální** (offline funguje přibalená verze).
- Uživatel komunikuje česky, ale **UI appky je anglicky** (od 16. 11. 2026 na přání uživatele); data spellů anglicky (originál).

## Stack
- Kotlin 2.1.0, Compose Multiplatform 1.7.3, Material 3, AGP 8.7.3, Gradle wrapper 8.10.2
- kotlinx-serialization-json 1.7.3, kotlinx-coroutines 1.9.0
- `com.russhwolf:multiplatform-settings-no-arg` 1.3.0 – úložiště decků
- Ktor client 3.0.3 (OkHttp na Androidu i desktopu, Darwin na iOS) – stažení databáze
- `org.jetbrains.compose.material:material-icons-core` – ikony (jen core, ne extended)
- Targety: `androidTarget`, `jvm("desktop")`, `iosX64/iosArm64/iosSimulatorArm64` (framework `ComposeApp`, static), `wasmJs` (web, od 16. 11. 2026)
- Verze jsou v `gradle/libs.versions.toml`

## Stav
- ✅ Desktop: kompiluje, testy (`desktopTest`) zelené, appka startuje.
- ✅ Android: `assembleDebug` projde (na zařízení zatím nezkoušeno).
- 🟡 iOS: Kotlin kód ověřen cross-kompilací klibů na Windows
  (`./gradlew :composeApp:compileKotlinIosArm64 -Pkotlin.native.enableKlibsCrossCompilation=true`).
  Linkování frameworku a Xcode build vyžadují Mac – `iosApp/` je připravený, ale na Macu zatím nespuštěný.
- Build na tomto stroji: `JAVA_HOME` = `%USERPROFILE%\.jdks\ms-21.0.10` (systémová Java je 8). `local.properties` ukazuje na `%LOCALAPPDATA%\Android\Sdk`.

## Struktura
```
composeApp/src/commonMain/kotlin/cz/pfspells/
  App.kt                 téma (tmavé „arcane“, zlatá primary), loading/chyba DB, Router podle Screen
  AppState.kt            back stack, DbState (načtení/stažení spellů), filtry prohlížeče, decky (CRUD + persist)
  data/Model.kt          Spell (runtime), Deck (@Serializable)
  data/CasterClasses.kt  Progression, CasterClass, seznam 26 class
  data/SpellParser.kt    SpellList.json (formát SpellTable-pf1e) → List<Spell>
  data/Repositories.kt   SpellRepository (cache soubor → fallback resources; download), DeckStore (Settings)
  data/Platform.kt       expect AppFiles (textové soubory appky), createHttpClient(), nowDisplayText()
  ui/Common.kt           BackButton, SimpleDropdown, CountStepper, Badge, classLevelSummary
  ui/DeckListScreen.kt   seznam decků + dialog „Nový deck“, menu → kompendium / databáze
  ui/DeckDetailScreen.kt deck seskupený podle spell levelu, změna úrovně, přejmenování, smazání
  ui/SpellBrowserScreen.kt  picker (deckId != null) i kompendium (deckId == null); hledání, škola, classa, level
  ui/SpellDetailScreen.kt   všechny údaje + popis + mythic, odkaz na AoN, „Přidat do decku“
  ui/DatabaseScreen.kt      info o databázi, „Stáhnout aktuální databázi“, návrat k přibalené
composeApp/src/commonMain/composeResources/files/SpellList.json   přibalená data (~6 MB)
composeApp/src/{android,desktop,ios}Main/…/data/Platform.*.kt     actualy (filesDir / ~/.pfspells / Documents)
composeApp/src/androidMain/…/MainActivity.kt   AppFiles.init(context), systémové Zpět → state.pop()
composeApp/src/desktopMain/…/main.kt           okno 520×860, Esc → zpět
composeApp/src/iosMain/…/MainViewController.kt ComposeUIViewController { App(appState) }
composeApp/src/commonTest  pravidla class + parser;  src/desktopTest  ověření přibalených dat
iosApp/                    Xcode projekt (SwiftUI obal), viz sekce iOS
```

## Vydání a nasazení
- Remote: `https://github.com/DejvHeHe/pathfinder-mobile-app` (uživatel commituje a pushuje sám – necommitovat bez požádání).
- `.github/workflows/release.yml`: tag `v*.*.*` → APK, MSI, DMG, DEB, web zip do GitHub Release + Docker image `ghcr.io/dejvhehe/pf-spellbook-web`.
  `ci.yml`: testy + Android + web + iOS framework na push/PR. Workflowy nebyly spuštěny na GitHubu – ověřeno jen lokálně
  (`assembleRelease`, `packageDistributionForCurrentOS` → .msi, YAML syntaxe).
- Verze: `-PappVersion`, `-PversionCode`; podpis APK přes env `ANDROID_KEYSTORE_FILE/_PASSWORD`, `ANDROID_KEY_ALIAS/_PASSWORD` (jinak debug klíč).
- Docker: `Dockerfile` (temurin 21 build → stage `export` = jen soubory, stage `runtime` = nginx 1.27), `compose.yaml` (127.0.0.1:8888 – výchozí PORT, image z GHCR nebo build). Uživatel má vlastní nginx na hostiteli jako reverzní proxy → `docker/proxy-nginx.example.conf`. Build funguje i v podsložce (ověřeno).
  Build v kontejneru bez Android SDK funguje díky prázdné složce v `ANDROID_HOME` (ověřeno lokálně). Docker build sám neověřen (Docker není na PC).
- `gradlew` má v gitu exec bit (100755). Podrobnosti: docs/deploy.md.

## Web (Kotlin/Wasm)
- `wasmJsMain`: `main.kt` (ComposeViewport, Esc = zpět), `Platform.wasmJs.kt` (AppFiles → IndexedDB přes `js()` + `Promise.await`,
  `HttpClient(Js)`, export = stažení Blobu, import = `<input type=file>`), `resources/index.html` (loading obrazovka).
- `AppFiles` je kvůli webu `suspend` na všech platformách.
- Build: `wasmJsBrowserDistribution` → `composeApp/build/dist/wasmJs/productionExecutable/` (~26 MB statický web). Ověřeno v headless Chrome.
- Web nemá fallback fonty → žádné symboly mimo přibalené fonty (kontrola glyfů: fontTools, viz docs/architecture.md).
- Port 8080 je na vývojovém PC obsazený jiným procesem → pro lokální test produkčního buildu použít jiný port (8765).

## Inventář (od 16. 11. 2026)
- Zadání uživatele: inventář podle designu (artboardy „Inventory – desktop/mobile“, „Add item – desktop/mobile“ na stejném plátně),
  itemy z databáze jako u spellů, aktualizace tlačítkem, export/import inventáře,
  **ceny nad 200 gp jsou skryté („??? gp“), 10 rychlých ťuknutí kamkoli na obrazovku je odhalí/skryje** (jen do zavření appky).
- Sekce appky: `AppState.section` (SPELLBOOK / INVENTORY) – záložky `SectionTabs` (mobil pod nadpisem, desktop v hlavičce).
- Data itemů (`data/Items.kt`, `ItemParser.kt`, `ItemRepository.kt`), celkem ~6 060 itemů, parsování ~0,3 s, načítá se líně (`ensureItems`):
  - `ItemsBase.json` – běžné vybavení, zbroje, alchymie, zvířata z **Foundry VTT PF1** (OGL), jen přibalené.
  - `Weapons.csv`, `Ammunition.csv`, `MagicItems.csv` – d20pfsrd data z github.com/Vireche/randomitemgenerator; **appka je umí stáhnout**
    (Databases → Download latest item database), cache v `AppFiles` (`items_*.csv`), validace před uložením.
  - Vše se generuje nástrojem `tools/build_items.py` (pip install pyyaml); MagicItems.csv je ořezaný o sloupec FullText.
  - CSV parser `Csv` (RFC 4180), `decodeText` = UTF-8 s fallbackem na cp1252 (weapons.csv je v cp1252).
- Inventáře: `Inventory` (jméno, STR, mince pp/gp/sp/cp, poznámky, `InvEntry`), uložené pod `inventories_v1`.
  Položka nese vlastní jméno/cenu/váhu/info (nezávislé na verzi databáze), `itemId` jen odkazuje. Vlastní itemy (`itemId = null`).
  Nosnost `CarryingCapacity` (tabulka PF1, Medium, +10 STR = ×4), mince 50 ks = 1 lb.
- Export/import: `InventoryTransfer` (`pf-spellbook-inventories` v1), obecné dialogy `TextExportDialog` / `TextImportDialog`.
- Obrazovky: `ui/InventoryScreen.kt` (seznam, boční panel s měšcem/zatížením/kategoriemi, detail), `ui/ItemScreens.kt`
  (picker: kategorie, proficiency, max cena, vlastní item; mobil = bottom sheet, desktop = pravý panel; detail itemu/položky).
  `Screen.InventoryDetail`, `ItemPicker`, `ItemDetail` (aside jako SpellDetail; `showItem` nahrazuje detail).

## Design „Linkovaný zápisník“
- Zdroj: designové plátno uživatele (claude.ai artifact 7a9f3af7…, artboardy „4a · Linkovaný zápisník“ mobil + desktop).
- `ui/Theme.kt`: tokeny `Nb.*` (papír #E6D8BF s linkami po 28 dp, panel #F2E8D5, inset #EADCC2, štítek #DCCBAA,
  inkoust #2E2418/#5E4D38, olivová #4E5D2B = hlavní akce, rezavá #8A4B2A = nadpisy sekcí/popisky, čárkované okraje #9C8662,
  linky #A8916A, radius 4 dp), `PfTheme` (M3 light scheme + fonty), modifikátory `ruledPaper`, `dashedBorder`, `dashedEdge`,
  `bottomRule`, tahové ikony `NbIcons` (SVG path z návrhu).
- Fonty (OFL, licence v `licenses/`): Alegreya Bold + Bold Italic (statické instance vyrobené z variabilního fontu přes
  `fonttools varLib.instancer … wght=700`, kvůli Androidu < 8), Alegreya Sans 400/500/700, IBM Plex Mono 400/500/600.
  Nadpisy = serif bold, sekce = serif bold italic rezavě, čísla a popisky = mono.
- `ui/Common.kt`: `NbScreen` (mobil: lišta + FAB; široké: velký nadpis + akce v řádku), `SectionHeader`, `CountStepper`,
  `NbPrimaryButton`/`NbFab`/`NbDashedButton`/`NbSquareButton`, `NbTag`, `NbPanel`, `NbFilterChip`, `NbSearchField`, `SimpleDropdown`.
- Adaptivní layout: šířka ≥ 1000 dp → `ui/WideLayout.kt` (hlavička s hledáním | decky · hlavní obrazovka · detail spellu).
  Sloupce se odvozují ze stejného back stacku (`openRoot`, `showSpell` nahrazuje detail, `popMain`). Desktop okno 1360×880.
- Náhledy designu: `./gradlew :composeApp:desktopTest --tests "cz.pfspells.PreviewRender" -Ppreviews=true` → `composeApp/build/previews/*.png`
  (vlastní dočasné úložiště decků, skutečná data nemění).

## Datový model
- `Spell` není serializovaný – vzniká parserem při každém startu (~0,35 s na desktopu, na `Dispatchers.Default`).
- `levels` klíče = názvy spell listů **ze zdroje** (lowercase): `sorcerer/wizard`, `cleric/oracle`, `druid`, `witch`, `shaman`, `psychic`, `bard`, `skald`, `inquisitor`, `magus`, `alchemist`, `investigator`, `summoner`, `unchained summoner`, `hunter`, `mesmerist`, `occultist`, `spiritualist`, `paladin`, `antipaladin`, `ranger`, `bloodrager`, `medium`, `adept`… Výjimky (`wizard`, `oracle`…) sjednocuje `SpellParser.normalizeListKey`.
- `description`: odstavce oddělené `\n` (`Spell.paragraphs`).
- `id` = slug jména (unikátní; kolize → `-2`). Decky odkazují na spelly přes id → po aktualizaci DB se párují podle jména.
- Decky: JSON pod klíčem `decks_v1` v `Settings()`. Při změně schématu zvýšit verzi klíče nebo napsat migraci.

## Export / import decků
- `data/DeckTransfer.kt`: JSON `{"format":"pf-spellbook-decks","version":1,"decks":[{"name","class","level","spells":[{"id","name","count"}]}]}`.
  Import bere i samotný deck bez obálky; spell hledá podle id, pak podle jména (case-insensitive); nenalezené hlásí a vynechá.
  Importované decky dostanou nové id, kolize jmen → „(import)“. Změna formátu → zvýšit `version` a umět číst starší.
- UI: `ui/DeckTransferDialogs.kt` (ExportDialog, ImportDialog). Detail decku → ikona Sdílet; seznam → menu ⋮ Importovat / Exportovat všechny.
- Platformy (`expect val deckFileIo`): desktop = AWT `FileDialog` uložit/otevřít; Android = share sheet (text) + `OpenDocument`
  (launcher registrovaný v `MainActivity.onCreate`); iOS = `UIActivityViewController`, otevření souboru zatím ne (jen schránka).

## Pravidla PF1 v kódu (`CasterClasses.kt`)
| Progression | Classy | Vzorec max spell levelu |
|---|---|---|
| PREPARED_9 | wizard, cleric, druid, witch, shaman | `min(9, (L+1)/2)` |
| SPONTANEOUS_9 | sorcerer, oracle, psychic, arcanist | `L<4 → 1`, jinak `min(9, L/2)` |
| SIX | bard, skald, inquisitor, magus, alchemist, investigator, summoner(+unch.), warpriest, hunter, mesmerist, occultist, spiritualist | `min(6, (L+2)/3)` |
| FOUR | paladin, antipaladin, ranger, bloodrager | `L<4 → -1 (žádné)`, jinak `min(4, (L-1)/3)` |

- Level 0 mají všichni kromě FOUR a alchemist/investigator (`hasCantrips = false`).
- Víc listů → bere se **nižší** level (hunter = hunter/druid/ranger, warpriest = warpriest/cleric, skald = skald/bard, investigator = investigator/alchemist).
- Medium a adept vynechány. Domény, mystéria, patroni, bloodline zatím **nejsou** (data ale ve zdroji jsou: `access_ways.Domain/Subdomain/Mystery/Bloodline/Elemental School`).

## Zdroj dat
- `SpellList.json` z https://github.com/iociveteres/SpellTable-pf1e (`data/SpellList.json`, MIT; data z Archives of Nethys, Paizo Community Use / OGL). 3 040 záznamů, 3 030 má class list.
- Stažení v appce: `SpellParser.SOURCE_URL` (raw GitHub, větev `main`). Stažený text se nejdřív celý naparsuje, teprve pak se uloží (`AppFiles` → `SpellList.json`); poškozená cache se při startu smaže a použije se přibalená verze.
- Původní plán (spell_full z d20pfsrd + `tools/import_spells.py`) opuštěn – AoN JSON je kompletnější a jde stahovat přímo.

## Navigace a stav
- Vlastní back stack v `AppState` (`mutableStateListOf<Screen>`), žádná navigační knihovna.
- `Screen`: `Decks`, `Compendium`, `Database`, `DeckDetail(deckId)`, `Picker(deckId)`, `SpellDetail(spellId, deckId?)`.
- Filtry a scroll prohlížeče žijí v `AppState` (přežijí detail a zpět); picker se resetuje při otevření pro jiný deck.
- Android: `configChanges` v manifestu + `AppState` v `by lazy` v Activity (přežije rotaci, ne process death – stack se resetuje, decky zůstanou).

## iOS (`iosApp/`)
- Ručně napsaný Xcode projekt podle KMP šablony (bez CocoaPods/SPM): target `iosApp`, produkt `PfSpells.app`, iOS ≥ 15.
- Build fáze „Compile Kotlin Framework“ volá `sh ./gradlew :composeApp:embedAndSignAppleFrameworkForXcode`
  (přes `sh`, aby nevadil chybějící exec bit z Windows); framework se hledá v `composeApp/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)`.
- `Configuration/Config.xcconfig`: `TEAM_ID` (prázdné → podepsat v Signing & Capabilities), `BUNDLE_ID`, `APP_NAME`.
- `ContentView.swift` → `MainViewControllerKt.MainViewController()`; `Info.plist` má `CADisableMinimumFrameDurationOnPhone` (120 Hz).
- Na Macu: JDK 17+ (Xcode nedědí shell env – případně `org.gradle.java.home` v `~/.gradle/gradle.properties`), `local.properties` s Android SDK pro iOS build potřeba není (Android target se ale konfiguruje – na Macu bez SDK je potřeba Android SDK doinstalovat, nebo dočasně vypnout `androidTarget`).
- iOS nemá systémové Zpět – navigace jen přes šipku v top baru (swipe-back gesto zatím není).
- AppIcon je prázdný (Xcode jen varuje).

## Příkazy
```bash
./gradlew :composeApp:run                              # desktop
./gradlew :composeApp:desktopTest                      # testy
./gradlew :composeApp:assembleDebug                    # Android APK (build/outputs/apk/debug)
./gradlew :composeApp:installDebug                     # Android na připojené zařízení
./gradlew :composeApp:packageDistributionForCurrentOS  # desktop instalátor
```

## Konvence
- Package `cz.pfspells`, **UI texty anglicky** (žádné české řetězce v UI); komentáře v kódu mohou zůstat česky.
- Jen common kód, kde to jde; žádné JVM-only API v `commonMain` (pozor např. na `toSortedMap`, `String.format`).
- Nepoužívat experimental `ExposedDropdownMenuBox` → `SimpleDropdown`.
- Material icons jen z icons-core.

## TODO / nápady
1. Vyzkoušet na Android zařízení / emulátoru; iOS build na Macu.
2. Spell sloty za den podle classy/úrovně + bonusové sloty z atributu; režim „hra“ s odškrtáváním a resetem po odpočinku.
3. Domény (cleric), mystéria (oracle), patroni (witch), bloodline (sorcerer), elemental school (wizard) – data jsou v `access_ways`.
4. Spontánní castery: „spells known“ místo počtu kopií.
6. Řazení, oblíbené; filtr PFS legal (pole `PFS legal` ve zdroji).
7. Ikona appky (teď systémová `ic_menu_agenda`).
