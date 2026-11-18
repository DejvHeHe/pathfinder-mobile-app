# PF1 Spellbook

Offline aplikace pro hráče **Pathfinder 1e**: **postavy** (multiclass, podtřídy, spellbook, denní sloty, featy, schopnosti)
a vedení **inventáře** s itemy z databáze. Bez účtu a bez serveru, všechno se ukládá lokálně v zařízení.

Běží na **Androidu, iOS, desktopu** (Windows, macOS, Linux) **a v prohlížeči** (Kotlin/Wasm). Je to jedna sdílená aplikace
v Kotlin Multiplatform + Compose Multiplatform.

| | |
|---|---|
| Spelly | 3 030 spellů (Archives of Nethys), filtr podle classy/úrovně, detail se všemi údaji |
| Postavy | 49 class (multiclass, BAB, záchrany), atributy, podtřídy (domény, mystéria, bloodline, škola), export/import |
| Kouzla | sloty za den a známá kouzla podle tabulek AoN, bonus z atributu, herní režim s odškrtáváním a odpočinkem |
| Featy | 3 442 featů (Archives of Nethys), prerekvizity ✓ / ✗ / ?, počet featů podle úrovně, vlastní featy |
| Schopnosti | Smite Evil, Rage, Ki Pool… s počtem použití podle úrovně, vlastní počítadla |
| Inventář | ~6 060 itemů (d20pfsrd + Foundry VTT PF1), vlastní katalog, lektvary/svitky/hůlky ze spellů, měšec, nosnost, poznámky, export/import |
| Aktualizace | spelly i itemy jdou tlačítkem stáhnout znovu ze zdroje |
| Vzhled | design „Linkovaný zápisník“, mobil i třísloupcové široké okno |
| Jazyk | rozhraní anglicky i česky (přepínač EN / CZ v hlavičce) |

## Stažení

Hotové buildy (Android `.apk`, Windows `.msi`, macOS `.dmg`, Linux `.deb`, web `.zip`) jsou na stránce
**[Releases](https://github.com/DejvHeHe/pathfinder-mobile-app/releases)**. Webovou verzi jde nasadit i Dockerem:

```bash
docker compose pull && docker compose up -d   # 127.0.0.1:8888 pro vaši reverzní proxy – viz docs/deploy.md
```

## Rychlý start

```bash
# JDK 17+ (na vývojovém PC: %USERPROFILE%\.jdks\ms-21.0.10)
./gradlew :composeApp:run              # desktop
./gradlew :composeApp:installDebug     # Android na připojený telefon / emulátor
./gradlew :composeApp:wasmJsBrowserDevelopmentRun   # web na http://localhost:8080
./gradlew :composeApp:desktopTest      # testy
```

iOS: na Macu si appku sestaví každý sám přes Xcode s bezplatným Apple ID – návod krok za krokem: [docs/ios-install.md](docs/ios-install.md).

## Dokumentace

| Soubor | Pro koho | O čem |
|---|---|---|
| [docs/user-guide.md](docs/user-guide.md) | hráč | jak appku používat: postavy, kouzla, featy, inventář, export, aktualizace, skryté ceny |
| [docs/architecture.md](docs/architecture.md) | vývojář | jak to funguje uvnitř: vrstvy, stav, navigace, UI, platformy |
| [docs/data.md](docs/data.md) | vývojář | odkud jsou spelly a itemy, formáty, aktualizace, pravidla PF1 v kódu |
| [docs/build.md](docs/build.md) | vývojář | build pro jednotlivé platformy, testy, náhledy designu, regenerace dat |
| [docs/ios-install.md](docs/ios-install.md) | hráč s Macem | instalace na vlastní iPhone přes Xcode (bezplatné Apple ID) |
| [docs/deploy.md](docs/deploy.md) | vývojář / správce | vydání verze na GitHub Releases (CI), podepsání APK, web v Dockeru, HTTPS |
| [CLAUDE.md](CLAUDE.md) | AI asistent | stručné poznámky, rozhodnutí a TODO pro Claude Code |

## Licence

Copyright (C) 2026 David Sobíšek. Zdrojový kód je pod licencí **GNU GPL v3** – viz [`LICENSE`](LICENSE).

Herní obsah (spelly, itemy) pod GPL **není** – zůstává pod Open Game License v1.0a a Paizo Community Use Policy
(appka je proto zdarma a nesmí se zpoplatnit). Zdroje dat, text OGL se Section 15, Paizo notice, fonty a knihovny:
[`NOTICE.md`](NOTICE.md). Totéž je v appce v menu → **About & licenses**.

*This app uses trademarks and/or copyrights owned by Paizo Inc., used under Paizo's Community Use Policy
([paizo.com/licenses/communityuse](https://paizo.com/licenses/communityuse)). We are expressly prohibited from charging you
to use or access this content. This app is not published, endorsed, or specifically approved by Paizo.
For more information about Paizo Inc. and Paizo products, visit [paizo.com](https://paizo.com).*
