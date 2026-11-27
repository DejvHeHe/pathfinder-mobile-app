# Build, tests and releases

[Čeština](../build.md) | **English**

## Requirements

| What | Needed for |
|---|---|
| JDK 17+ (21 recommended) | everything – Gradle and Kotlin |
| Android SDK (platform 35, build-tools 35) | Android build; path in `local.properties` (`sdk.dir=…`, not in git) |
| macOS + Xcode 15+ | iOS (the Kotlin/Native framework links only on a Mac) |
| Python 3 + `pyyaml` | only for regenerating bundled data (`tools/*.py`) |

If your system Java is older, point Gradle to a JDK 17+ first, e.g. on Windows:

```powershell
$env:JAVA_HOME = "$env:USERPROFILE\.jdks\ms-21.0.10"
```

## Desktop (Windows / macOS / Linux)

```bash
./gradlew :composeApp:run                              # run
./gradlew :composeApp:packageDistributionForCurrentOS  # installer for the current OS (.msi / .dmg / .deb)
```

The desktop version runs on the JVM, so the same code works on all three systems. An installer can only be built on
its own OS (`.msi` on Windows, `.dmg` on a Mac). The window is 1360×880 – from 1000 px width the three-column layout
is used; making the window narrower switches to the phone layout.

## Web (Kotlin/Wasm)

```bash
./gradlew :composeApp:wasmJsBrowserDevelopmentRun   # dev server that opens the browser (port 8080)
./gradlew :composeApp:wasmJsBrowserDistribution     # production build → composeApp/build/dist/wasmJs/productionExecutable/
```

- On the first build Gradle downloads Node.js and yarn (webpack) itself – it takes a few minutes.
- The result is a **static website** (~26 MB: 2× `.wasm`, `composeApp.js`, `index.html`, `composeResources/` with data and fonts).
  Upload it to any static hosting (GitHub Pages, Netlify, Cloudflare Pages…).
  The server must send `.wasm` as `application/wasm` (common hostings do).
- Trying the production build locally: `python -m http.server 8765` in the `productionExecutable` folder → http://localhost:8765
  (opening `index.html` directly from disk does not work – browsers do not load wasm from `file://`).
- The browser must support WasmGC: Chrome/Edge 119+, Firefox 120+, Safari 18.2+.
- Entry point `wasmJsMain/kotlin/cz/pfspells/main.kt` (`ComposeViewport`, Esc = back), page `wasmJsMain/resources/index.html`
  (a "Loading…" screen in the app's colors until the wasm loads).

## Android

```bash
./gradlew :composeApp:assembleDebug    # APK: composeApp/build/outputs/apk/debug/composeApp-debug.apk
./gradlew :composeApp:installDebug     # install on a connected device / emulator
```

`minSdk` 24 (Android 7), `targetSdk` 35. Permissions: only `INTERNET` (downloading updates).
A release build (`assembleRelease`) is signed with the key from `ANDROID_KEYSTORE_FILE/_PASSWORD`,
`ANDROID_KEY_ALIAS/_PASSWORD` (see [deploy.md](deploy.md#signing-the-android-apk-recommended)); without them with the debug key.
Minification is off.

## iOS

1. On a Mac install Xcode and JDK 17+ (Xcode does not inherit shell variables – if needed set
   `org.gradle.java.home` in `~/.gradle/gradle.properties`).
2. Open `iosApp/iosApp.xcodeproj`.
3. Fill in `TEAM_ID` in `iosApp/Configuration/Config.xcconfig`, or choose the team in *Signing & Capabilities*
   (a free Apple account is enough for your own iPhone, the signature lasts 7 days).
4. Run ▶. The "Compile Kotlin Framework" build phase calls `./gradlew :composeApp:embedAndSignAppleFrameworkForXcode`.

The Android SDK is not needed on a Mac – the Xcode build phase creates an empty folder when it is missing (like Docker).
`TEAM_ID` in `Config.xcconfig` is also appended to the bundle ID, so several people can build the app with free accounts.
Guide for players: [ios-install.md](ios-install.md).

On Windows the iOS Kotlin code can at least be compiled (without linking) – useful as a check:

```bash
./gradlew :composeApp:compileKotlinIosArm64 -Pkotlin.native.enableKlibsCrossCompilation=true
```

The Xcode project is hand-written from the KMP template; the app icon is still missing (Xcode only warns).

## Tests

```bash
./gradlew :composeApp:desktopTest
```

Results: `composeApp/build/test-results/desktopTest/*.xml`. An overview of the tests is in [architecture.md](architecture.md#tests).

## Design previews

Renders the main screens (phone 390×844 and desktop 1440×1000) and some dialogs to PNG – with sample characters and an
inventory in temporary storage, your real data is not touched:

```bash
./gradlew :composeApp:desktopTest --tests "cz.pfspells.PreviewRender" -Ppreviews=true
```

Output: `composeApp/build/previews/*.png`.

## Regenerating data

| What | How |
|---|---|
| Spells | download the current `SpellList.json` from [SpellTable-pf1e](https://github.com/iociveteres/SpellTable-pf1e/tree/main/data) into `composeResources/files/` |
| Items | `pip install pyyaml && python tools/build_items.py` |
| Feats | `python tools/build_feats.py` (~3,400 AoN pages, 1 per second – about 3 hours, resumable) |
| Class tables | `python tools/build_casting.py` (spells per day / known), `python tools/build_classes.py` (BAB, saves, class skills, proficiencies) |
| Races | `python tools/build_races.py [races.zip]` |
| Deities | `python tools/build_deities.py [DeitiesByGroup.html] [--cache DIR]` (~460 deity pages, 1 per second) |
| Racial traits | `python tools/build_racial_traits.py [--cache DIR]` (~80 race pages, 1 per second) |
| Animal companions, familiars | `python tools/build_companions.py [--cache DIR]` (2 d20pfsrd pages) |
| Eidolon | `python tools/build_eidolons.py [--cache DIR]` (2 d20pfsrd pages) |
| Prestige classes | `python tools/build_prestige_classes.py [--cache DIR]` (~120 pages, 1 per second) |
| Archetypes | `python tools/build_archetypes.py [--cache DIR]` (~1300 pages, 1 per second, ~25 min, resumable) |
| Class abilities, hexes, revelations | `python tools/build_class_abilities.py [class-abilities.zip] [sources.mjs]` |
| App icon | edit `art/icon.svg` (viewBox 100×100) → `python tools/build_icons.py [--chrome PATH]` (Pillow + Chrome/Chromium; writes the Android, desktop, iOS and web icons) |

If Python cannot verify a site's certificate, download the source with `curl` and pass the local file (see the script headers).
Then run the tests (`BundledDataTest`, `ItemDataTest`… check that the data loads).

## Versions

`-PappVersion=1.2.3` sets the version of the Android app and the desktop installers (default `1.0.0`),
`-PversionCode=N` the Android versionCode. CI takes them from the git tag – see [deploy.md](deploy.md).

## Pre-release checklist

- [ ] `desktopTest` green
- [ ] `assembleDebug` passes
- [ ] `wasmJsBrowserDistribution` passes and the web loads in a browser
- [ ] iOS: `compileKotlinIosArm64` (cross) passes, Xcode build on a Mac
- [ ] design previews checked
- [ ] `CLAUDE.md`, `README*.md` and `docs/` match the changes
- [ ] release: `git tag vX.Y.Z && git push origin vX.Y.Z` (see [deploy.md](deploy.md))
