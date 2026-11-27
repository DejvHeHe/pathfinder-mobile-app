# Build, testy a vydání

**Čeština** | [English](en/build.md)

## Požadavky

| Co | Kde je potřeba |
|---|---|
| JDK 17+ (doporučeno 21) | všude – Gradle i Kotlin |
| Android SDK (platform 35, build-tools 35) | Android build; cesta v `local.properties` (`sdk.dir=…`, soubor není v gitu) |
| macOS + Xcode 15+ | iOS (Kotlin/Native framework se linkuje jen na Macu) |
| Python 3 + `pyyaml` | jen pro přegenerování dat itemů (`tools/build_items.py`) |

Na vývojovém PC (Windows) je systémová Java 8, proto před Gradlem:

```powershell
$env:JAVA_HOME = "$env:USERPROFILE\.jdks\ms-21.0.10"
```

## Desktop (Windows / macOS / Linux)

```bash
./gradlew :composeApp:run                              # spustit
./gradlew :composeApp:packageDistributionForCurrentOS  # instalátor pro aktuální OS (.msi / .dmg / .deb)
```

Desktopová verze běží na JVM, takže stejný kód funguje na všech třech systémech. Instalátor pro daný OS se ale dá
sestavit jen na tom OS (`.msi` na Windows, `.dmg` na Macu). Okno má 1360×880 – od šířky 1000 px se ukáže třísloupcové
rozložení; zúžením okna se přepne na mobilní.

## Web (Kotlin/Wasm)

```bash
./gradlew :composeApp:wasmJsBrowserDevelopmentRun   # vývojový server s automatickým otevřením prohlížeče (port 8080)
./gradlew :composeApp:wasmJsBrowserDistribution     # produkční build → composeApp/build/dist/wasmJs/productionExecutable/
```

- Při prvním buildu si Gradle sám stáhne Node.js a yarn (webpack) – trvá to pár minut.
- Výsledek je **statický web** (~26 MB: 2× `.wasm`, `composeApp.js`, `index.html`, `composeResources/` s daty a fonty).
  Stačí ho nahrát na libovolný statický hosting (GitHub Pages, Netlify, Cloudflare Pages…).
  Server musí posílat `.wasm` s typem `application/wasm` (běžné hostingy to dělají).
- Lokální vyzkoušení produkčního buildu: `python -m http.server 8765` ve složce `productionExecutable` → http://localhost:8765
  (otevřít `index.html` přímo ze souboru nefunguje – prohlížeč wasm z `file://` nenačte).
- Prohlížeč musí podporovat WasmGC: Chrome/Edge 119+, Firefox 120+, Safari 18.2+.
- Vstupní bod `wasmJsMain/kotlin/cz/pfspells/main.kt` (`ComposeViewport`, Esc = zpět), stránka `wasmJsMain/resources/index.html`
  (úvodní „Loading…“ obrazovka v barvách appky, než se načte wasm).

## Android

```bash
./gradlew :composeApp:assembleDebug    # APK: composeApp/build/outputs/apk/debug/composeApp-debug.apk
./gradlew :composeApp:installDebug     # nainstalovat na připojené zařízení / emulátor
```

`minSdk` 24 (Android 7), `targetSdk` 35. Oprávnění: jen `INTERNET` (stahování aktualizací).
Release build (`assembleRelease`) se podepíše klíčem z proměnných `ANDROID_KEYSTORE_FILE/_PASSWORD`,
`ANDROID_KEY_ALIAS/_PASSWORD` (postup v [deploy.md](deploy.md#podepsání-android-apk-doporučeno)); bez nich debug klíčem.
Minifikace je vypnutá.

## iOS

1. Na Macu nainstalovat Xcode a JDK 17+ (Xcode nedědí proměnné z shellu – případně nastavit
   `org.gradle.java.home` v `~/.gradle/gradle.properties`).
2. Otevřít `iosApp/iosApp.xcodeproj`.
3. V `iosApp/Configuration/Config.xcconfig` vyplnit `TEAM_ID`, nebo vybrat tým v *Signing & Capabilities*
   (na vlastní iPhone stačí bezplatný Apple účet, podpis platí 7 dní).
4. Spustit ▶. Build fáze „Compile Kotlin Framework“ zavolá `./gradlew :composeApp:embedAndSignAppleFrameworkForXcode`.

Android SDK na Macu není potřeba – build fáze v Xcode si při jeho absenci vytvoří prázdnou složku (stejně jako Docker).
`TEAM_ID` v `Config.xcconfig` se připojuje i k bundle ID, takže appku může s bezplatným účtem stavět víc lidí. Návod pro hráče: [ios-install.md](ios-install.md).

Na Windows se dá iOS Kotlin kód aspoň zkompilovat (bez linkování) – hodí se jako kontrola:

```bash
./gradlew :composeApp:compileKotlinIosArm64 -Pkotlin.native.enableKlibsCrossCompilation=true
```

Projekt Xcode je napsaný ručně podle KMP šablony; ikona appky zatím chybí (Xcode jen varuje).

## Testy

```bash
./gradlew :composeApp:desktopTest
```

Výsledky: `composeApp/build/test-results/desktopTest/*.xml`. Přehled testů je v [architecture.md](architecture.md#testy).

## Náhledy designu

Vykreslí hlavní obrazovky (mobil 390×844 i desktop 1440×1000) do PNG – s ukázkovými postavami a inventářem (a dialogem výběru classy s podtřídou)
v dočasném úložišti, skutečná data se nezmění:

```bash
./gradlew :composeApp:desktopTest --tests "cz.pfspells.PreviewRender" -Ppreviews=true
```

Výstup: `composeApp/build/previews/*.png`.

## Přegenerování dat

| Co | Jak |
|---|---|
| Spelly | stáhnout aktuální `SpellList.json` ze [SpellTable-pf1e](https://github.com/iociveteres/SpellTable-pf1e/tree/main/data) do `composeResources/files/` |
| Itemy | `pip install pyyaml && python tools/build_items.py` |
| Featy | `python tools/build_feats.py` (~3 400 stránek AoN po 1 s – asi 3 h, přerušitelné) |
| Tabulky class | `python tools/build_casting.py` (kouzla za den / známá), `python tools/build_classes.py` (BAB, záchrany, class skills, proficience) |
| Rasy | `python tools/build_races.py [races.zip]` |
| Božstva | `python tools/build_deities.py [DeitiesByGroup.html] [--cache DIR]` (~460 stránek božstev po 1 s) |
| Rasové traity | `python tools/build_racial_traits.py [--cache DIR]` (~80 stránek ras po 1 s) |
| Archetypy | `python tools/build_archetypes.py [--cache DIR]` (~1300 stránek po 1 s, ~25 min, přerušitelný) |
| Schopnosti class, hexy, zjevení | `python tools/build_class_abilities.py [class-abilities.zip] [sources.mjs]` |
| Ikona appky | upravit `art/icon.svg` (viewBox 100×100) → `python tools/build_icons.py [--chrome PATH]` (Pillow + Chrome/Chromium; vytvoří ikony Androidu, desktopu, iOS i webu) |

Když Python neověří certifikát webu, stáhněte zdroj přes `curl` a předejte lokální soubor (viz hlavička skriptů).
Pak spustit testy (`BundledDataTest`, `ItemDataTest`… ověří, že data jdou načíst).

## Verze

`-PappVersion=1.2.3` nastaví verzi Android appky i desktopových instalátorů (bez parametru `1.0.0`),
`-PversionCode=N` Android versionCode. CI je bere z git tagu – viz [deploy.md](deploy.md).

## Kontrola před vydáním

- [ ] `desktopTest` zelené
- [ ] `assembleDebug` projde
- [ ] `wasmJsBrowserDistribution` projde a web se v prohlížeči načte
- [ ] iOS: `compileKotlinIosArm64` (cross) projde, na Macu build v Xcode
- [ ] náhledy designu prohlédnuté
- [ ] `CLAUDE.md` a `docs/` odpovídají změnám
- [ ] vydání: `git tag vX.Y.Z && git push origin vX.Y.Z` (viz [deploy.md](deploy.md))
