# CLAUDE.md – PF1 Spellbook (Kotlin Multiplatform)

Poznámky pro Claude Code: kontext projektu, rozhodnutí, stav a pravidla spolupráce.
- Podrobná dokumentace: `README.md` a `docs/` (user-guide, architecture, data, build, deploy, ios-install) – při změnách ji udržuj aktuální.
- Plánovaná práce: **`TODO.md`** (hlavní vylepšení + nápady) – hotové body odškrtávej.

## Pravidla spolupráce
- Uživatel komunikuje **česky**; **UI appky je anglicky** (žádné české řetězce v UI), komentáře v kódu můžou být česky.
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

## Stav (17. 11. 2026)
- ✅ **Vydáno `v1.0.1`** (GitHub Release): APK, MSI, DMG, DEB, web zip – všechny joby workflowu Release zelené.
  APK je podepsané **dočasným debug klíčem CI** (secrets s keystorem zatím nenastavené → aktualizace APK vyžaduje přeinstalaci).
  Tag `1.0.0` (bez „v“) je starý ruční release s debug APK – workflow na něj nereaguje.
- ✅ **Web běží na VPS uživatele** z image `ghcr.io/dejvhehe/pf-spellbook-web:edge` (`.env`: `TAG=edge` + `UMAMI_*`), za jeho nginx na `127.0.0.1:8888`.
  Image `:edge`, `:latest`, `:1.0.1` jsou veřejné.
- ✅ Desktop: testy (`desktopTest`, 17 testů) zelené, appka běží. ✅ Android: release APK se staví (na zařízení zatím nezkoušeno).
- 🟡 iOS: Kotlin část ověřena cross-kompilací na Windows a v CI (`linkDebugFrameworkIosSimulatorArm64` na macOS runneru);
  Xcode build na skutečném Macu zatím nikdo nezkoušel (návod pro kamarády: `docs/ios-install.md`).
- Vývojový PC: Windows, `JAVA_HOME` = `%USERPROFILE%\.jdks\ms-21.0.10` (systémová Java je 8), `local.properties` → `%LOCALAPPDATA%\Android\Sdk`.
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
  AppState.kt             jediný stav appky: back stack Screen, Section, decky, inventáře, filtry, DbState/ItemDbState, skryté ceny
  data/Model.kt           Spell, Deck
  data/CasterClasses.kt   26 sesílajících class, progrese spell levelů, canCast
  data/SpellParser.kt     SpellList.json → Spell
  data/Repositories.kt    SpellRepository (cache → přibalená verze, download), DeckStore, SpellDatabase
  data/DeckTransfer.kt    export/import decků
  data/Items.kt           Item, ItemCategory, ItemDatabase, Inventory, InvEntry, CarryingCapacity
  data/ItemParser.kt      ItemsBase.json + CSV → Item; Csv parser; decodeText (UTF-8 / cp1252)
  data/ItemRepository.kt  ItemRepository, InventoryStore, InventoryTransfer
  data/Platform.kt        expect: AppFiles (suspend), createHttpClient, deckFileIo, nowDisplayText
  ui/Theme.kt             tokeny Nb.*, fonty, PfTheme, modifikátory (ruledPaper, dashedBorder…), NbIcons
  ui/Common.kt            NbScreen (kostra obrazovky) + společné komponenty
  ui/WideLayout.kt        třísloupcové rozložení (hlavička se záložkami | sidebar · hlavní · detail)
  ui/DeckListScreen.kt    úvod se záložkami Spellbook/Inventory, seznam decků, AppMenu (⋮), NewDeckDialog
  ui/DeckDetailScreen.kt, SpellBrowserScreen.kt (picker + kompendium), SpellDetailScreen.kt
  ui/InventoryCommon.kt   formát ceny/váhy, skryté ceny, mince, LoadBar, SectionTabs, dialogy čísla/textu
  ui/InventoryScreen.kt   seznam inventářů, InventorySidebar, detail inventáře
  ui/ItemScreens.kt       ItemPicker (mobil: bottom sheet), ItemDetail (item z DB / položka inventáře), CustomItemDialog
  ui/DeckTransferDialogs.kt  obecné TextExportDialog / TextImportDialog (decky i inventáře)
  ui/DatabaseScreen.kt    stav a aktualizace databáze spellů i itemů
  ui/AboutScreen.kt       About & licenses (GPL, Paizo CUP, OGL text)
composeApp/src/commonMain/composeResources/
  files/  SpellList.json, ItemsBase.json, Weapons.csv, Ammunition.csv, MagicItems.csv, OGL.txt
  font/   Alegreya Bold (+Italic), Alegreya Sans, IBM Plex Mono
composeApp/src/androidMain   MainActivity (AppFiles.init, AndroidDeckFileIo.register, Zpět → pop), Platform/DeckFileIo actualy
composeApp/src/desktopMain   main.kt (okno 1360×880, Esc → zpět), actualy (~/.pfspells, AWT FileDialog)
composeApp/src/iosMain       MainViewController.kt, actualy (Documents, UIActivityViewController)
composeApp/src/wasmJsMain    main.kt (ComposeViewport), Platform.wasmJs.kt (IndexedDB, Blob/input file), resources/index.html
composeApp/src/commonTest, desktopTest   testy + PreviewRender (náhledy do PNG)
iosApp/                Xcode projekt (SwiftUI obal)
tools/build_items.py   sestavení přibalených dat itemů (Python + pyyaml; jen pro vývojáře)
docker/, Dockerfile, compose.yaml, compose.build.yaml   web v kontejneru
.github/workflows/     ci.yml, release.yml, docker.yml
```

## Navigace a stav
- Vlastní back stack `AppState.stack` (`sealed interface Screen`), žádná navigační knihovna:
  `Decks` (kořen, obsah podle `section`), `Compendium`, `Database`, `About`, `DeckDetail`, `Picker`, `SpellDetail`,
  `InventoryDetail`, `ItemPicker`, `ItemDetail`. `SpellDetail` a `ItemDetail` jsou „aside“ (pravý panel na širokém okně).
- `push`, `pop`, `openRoot` (stack `[Decks, x]` – boční panel), `showSpell`/`showItem` (nahradí otevřený detail), `popMain`.
- Široké okno (≥ 1000 dp) odvozuje sloupce ze stejného stacku → změna velikosti okna za běhu funguje.
- `section` (SPELLBOOK / INVENTORY) přepíná `SectionTabs`; `switchSection` vrací stack na kořen.
- Filtry a scroll prohlížečů žijí v `AppState`. Android: `configChanges` + `AppState` v `by lazy` (přežije rotaci, ne process death).

## Data
- **Spelly:** `SpellList.json` z [SpellTable-pf1e](https://github.com/iociveteres/SpellTable-pf1e) (MIT, data z AoN), 3 030 spellů,
  parsování ~0,35 s při startu. `levels` = názvy spell listů ze zdroje (`sorcerer/wizard`, `cleric/oracle`…), výjimky sjednocuje
  `SpellParser.normalizeListKey`. `id` = slug jména. Ve zdroji jsou i `access_ways.Domain/Subdomain/Mystery/Bloodline/Elemental School`
  (zatím nevyužité – viz TODO archetypy).
- **Itemy (~6 060):** `ItemsBase.json` (Foundry VTT PF1 – gear, zbroje, alchymie, zvířata; jen přibalené) +
  `Weapons.csv`, `Ammunition.csv`, `MagicItems.csv` (d20pfsrd přes Vireche/randomitemgenerator; **appka je umí stáhnout**).
  Načítají se líně (`ensureItems`). Přegenerování: `python tools/build_items.py`.
- Stažené verze se ukládají jako originální soubory do `AppFiles` (validace před uložením, poškozená cache → přibalená verze).
- **Decky** `decks_v1`, **inventáře** `inventories_v1` (JSON v `Settings`). Položka inventáře nese vlastní kopii jména/ceny/váhy,
  `itemId` jen odkazuje (custom item = `itemId null`, patří jen do svého inventáře). Změna schématu → nová verze klíče + migrace.
- Export/import: `pf-spellbook-decks` v1, `pf-spellbook-inventories` v1 (párování podle id, pak jména; importované se přidají jako nové).

## Pravidla PF1 v kódu
| Progression | Classy | Max spell level na úrovni L |
|---|---|---|
| PREPARED_9 | wizard, cleric, druid, witch, shaman | `min(9, (L+1)/2)` |
| SPONTANEOUS_9 | sorcerer, oracle, psychic, arcanist | `L<4 → 1`, jinak `min(9, L/2)` |
| SIX | bard, skald, inquisitor, magus, alchemist, investigator, summoner (+unch.), warpriest, hunter, mesmerist, occultist, spiritualist | `min(6, (L+2)/3)` |
| FOUR | paladin, antipaladin, ranger, bloodrager | `L<4 → žádné`, jinak `min(4, (L-1)/3)` |

- V nabídce class jsou **jen sesilatelé** (26); classy bez spellů (Swashbuckler, Fighter…) chybí záměrně – viz TODO multiclass.
  Medium a Adept vynechány. Cantripy nemají FOUR, alchemist, investigator. Víc spell listů → nižší level.
- Nosnost `CarryingCapacity`: tabulka PF1 pro Medium tvora, +10 STR = ×4, mince 50 ks = 1 lb.

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
- Tlačítko Zpět prohlížeče zatím navigaci neovládá.

## Vydání a nasazení
- Remote `https://github.com/DejvHeHe/pathfinder-mobile-app` (veřejné).
- `ci.yml`: push do main / PR → testy, Android, web, iOS framework (macOS runner).
- `release.yml`: tag `v*.*.*` → APK, MSI, DMG, DEB, web zip do GitHub Release. Ruční spuštění = jen artifacts, bez Release.
- `docker.yml`: push do main → `ghcr.io/dejvhehe/pf-spellbook-web:edge`; tag → `:X.Y.Z` + `:latest`.
- `Dockerfile`: temurin 21 build (prázdný `ANDROID_HOME` stačí) → stage `export` (jen soubory) / `runtime` (nginx 1.27 + `docker/nginx.conf`).
- `compose.yaml`: jen pull (žádný `build:`, žádný `pull_policy`), `127.0.0.1:${PORT:-8888}`, env `TAG`, `UMAMI_*`.
  Build na serveru jen vědomě přes `compose.build.yaml`. Ukázka pro hostitelskou nginx: `docker/proxy-nginx.example.conf`.
- Umami: `docker/40-umami.sh` (nginx entrypoint) nahradí `<!--UMAMI-->` v index.html podle `UMAMI_SCRIPT_URL/WEBSITE_ID/DOMAINS`
  (hodnoty jen v `.env` na serveru, ne v repu). `.gitattributes` drží LF u `*.sh` a `gradlew`; `gradlew` má exec bit.
- Podpis APK: env `ANDROID_KEYSTORE_FILE/_PASSWORD`, `ANDROID_KEY_ALIAS/_PASSWORD` (v CI ze secrets) – zatím nenastaveno.
- iOS se do Release nedává (nutný Apple Developer účet) – kamarádi s Macem staví přes Xcode (`docs/ios-install.md`;
  bundle ID = `$(BUNDLE_ID)$(TEAM_ID)`, bez Android SDK si build fáze vytvoří prázdnou složku).

## Licence
- Kód **GPL-3.0** (`LICENSE`), Copyright (C) 2026 David Sobíšek (GPL kvůli datům z Foundry VTT PF1, které jsou GPL-3.0).
- Herní obsah: **OGL 1.0a + Paizo Community Use Policy** → appka musí zůstat **zdarma** (žádné poplatky ani reklamy).
- `NOTICE.md`, `licenses/OGL-1.0a.txt` (Section 15 zkrácená na hlavní knihy; kopie v `files/OGL.txt`), v appce About & licenses.
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
- Nepoužívat experimental `ExposedDropdownMenuBox` → `SimpleDropdown`; ikony jen icons-core / `NbIcons`.
