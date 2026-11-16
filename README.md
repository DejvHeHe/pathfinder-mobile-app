# PF1 Spellbook

Offline aplikace pro hráče **Pathfinder 1e**: skládání spellbooků („decků“) podle classy a úrovně postavy
a vedení **inventáře** s itemy z databáze. Bez účtu a bez serveru, všechno se ukládá lokálně v zařízení.

Běží na **Androidu, iOS, desktopu** (Windows, macOS, Linux) **a v prohlížeči** (Kotlin/Wasm). Je to jedna sdílená aplikace
v Kotlin Multiplatform + Compose Multiplatform.

| | |
|---|---|
| Spelly | 3 030 spellů (Archives of Nethys), filtr podle classy/úrovně, detail se všemi údaji |
| Decky | 26 sesílacích class, automatický strop spell levelu, export/import |
| Inventář | ~6 060 itemů (d20pfsrd + Foundry VTT PF1), měšec, nosnost podle STR, poznámky, export/import |
| Aktualizace | spelly i itemy jdou tlačítkem stáhnout znovu ze zdroje |
| Vzhled | design „Linkovaný zápisník“, mobil i třísloupcové široké okno |

## Stažení

Hotové buildy (Android `.apk`, Windows `.msi`, macOS `.dmg`, Linux `.deb`, web `.zip`) jsou na stránce
**[Releases](https://github.com/DejvHeHe/pathfinder-mobile-app/releases)**. Webovou verzi jde nasadit i Dockerem:

```bash
docker compose up -d      # 127.0.0.1:8888 pro vaši reverzní proxy – viz docs/deploy.md
```

## Rychlý start

```bash
# JDK 17+ (na vývojovém PC: %USERPROFILE%\.jdks\ms-21.0.10)
./gradlew :composeApp:run              # desktop
./gradlew :composeApp:installDebug     # Android na připojený telefon / emulátor
./gradlew :composeApp:wasmJsBrowserDevelopmentRun   # web na http://localhost:8080
./gradlew :composeApp:desktopTest      # testy
```

iOS: otevřít `iosApp/iosApp.xcodeproj` v Xcode na Macu a spustit (podrobnosti v [docs/build.md](docs/build.md)).

## Dokumentace

| Soubor | Pro koho | O čem |
|---|---|---|
| [docs/user-guide.md](docs/user-guide.md) | hráč | jak appku používat: decky, inventář, export, aktualizace, skryté ceny |
| [docs/architecture.md](docs/architecture.md) | vývojář | jak to funguje uvnitř: vrstvy, stav, navigace, UI, platformy |
| [docs/data.md](docs/data.md) | vývojář | odkud jsou spelly a itemy, formáty, aktualizace, pravidla PF1 v kódu |
| [docs/build.md](docs/build.md) | vývojář | build pro jednotlivé platformy, testy, náhledy designu, regenerace dat |
| [docs/deploy.md](docs/deploy.md) | vývojář / správce | vydání verze na GitHub Releases (CI), podepsání APK, web v Dockeru, HTTPS |
| [CLAUDE.md](CLAUDE.md) | AI asistent | stručné poznámky, rozhodnutí a TODO pro Claude Code |

## Licence dat

Herní obsah Pathfinderu je © Paizo Inc. a používá se v souladu s Open Game License / Paizo Community Use Policy.
Spelly pochází z projektu [SpellTable-pf1e](https://github.com/iociveteres/SpellTable-pf1e) (MIT, data z Archives of Nethys),
itemy z [Vireche/randomitemgenerator](https://github.com/Vireche/randomitemgenerator) (d20pfsrd) a
[Foundry VTT PF1](https://gitlab.com/foundryvtt_pathfinder1e/foundryvtt-pathfinder1) (OGL).
Fonty Alegreya, Alegreya Sans a IBM Plex Mono jsou pod SIL Open Font License (viz `licenses/`).
