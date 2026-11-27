<img src="art/icon.svg" width="96" align="right" alt="Ikona PF1 Spellbook">

# PF1 Spellbook

[English](README.md) | **Čeština**

Offline aplikace pro hráče **Pathfinder 1e**: **postavy** (multiclass, podtřídy, spellbook, denní sloty, featy, schopnosti)
a vedení **inventáře** s itemy z databáze. Bez účtu a bez serveru, všechno se ukládá lokálně v zařízení.

Běží na **Androidu, iOS, desktopu** (Windows, macOS, Linux) **a v prohlížeči** (Kotlin/Wasm). Je to jedna sdílená aplikace
v Kotlin Multiplatform + Compose Multiplatform.

| | |
|---|---|
| Spelly | 3 030 spellů (Archives of Nethys), filtr podle classy/úrovně, detail se všemi údaji |
| Postavy | 80 ras (úpravy atributů, velikost), 49 class (multiclass, BAB, záchrany), přesvědčení, 463 božstev, podtřídy (domény a subdomény podle boha, mystéria, bloodline, škola), export/import |
| Kouzla | sloty za den a známá kouzla podle tabulek AoN, bonus z atributu, herní režim s odškrtáváním a odpočinkem |
| Dovednosti | 26 skillů, ranky, class skills, skill body, bonusy z atributů a rasy |
| Featy | 3 442 featů (Archives of Nethys), automatické ověření prerekvizit (atributy, skilly, class features, rasa, přesvědčení, božstvo, proficience…) ✓ / ✗ / ?, ruční potvrzení, počet featů podle úrovně a rasy, vlastní featy |
| Schopnosti | Smite Evil, Rage, Ki Pool, schopnosti domén / bloodline / škol všech úrovní… s počtem použití podle úrovně, vlastní počítadla |
| Rasové traity | ~630 alternativních traitů z Archives of Nethys; nahrazené výchozí traity přestanou platit pro prerekvizity |
| Prestižní classy | 118 z Archives of Nethys; ověření požadavků, zvýšení sesílací úrovně vybrané classy |
| Archetypy | ~1 300 archetypů z Archives of Nethys s plným textem; nahrazené schopnosti, podtřídy a sesílání se podle nich upraví |
| Hexy a zjevení | ~120 hexů čarodějnice a šamana, ~300 zjevení orákula s plným textem, počet podle úrovně classy |
| Inventář | ~6 060 itemů (d20pfsrd + Foundry VTT PF1), vlastní katalog, lektvary/svitky/hůlky ze spellů, měšec, nosnost, poznámky, export/import |
| Aktualizace | spelly i itemy jdou tlačítkem stáhnout znovu ze zdroje |
| Vzhled | design „Linkovaný zápisník“, mobil i třísloupcové široké okno |
| Jazyk | rozhraní anglicky i česky (přepínač EN / CZ v hlavičce) |

> **AI-first / „vibe-coded“ projekt.** Téměř všechen kód, nástroje pro data i dokumentaci napsala AI
> ([Claude Code](https://claude.com/claude-code) od Anthropicu) – autor navrhl appku, řídil každou funkci,
> kontroloval a testoval výsledky na skutečných zařízeních. Commity psané s AI mají řádek `Co-Authored-By: Claude`.
> Hlášení chyb jsou vítaná – některé okrajové případy pravidel PF1 můžou být ještě špatně.

## Soukromí

Appka **nesbírá žádné osobní údaje** – žádný účet, sledování ani reklamy. Postavy a inventáře zůstávají ve vašem
zařízení (nebo prohlížeči) a opustí ho jen při exportu. Funguje úplně offline, na internet jde jen při stažení
aktualizované databáze kouzel nebo předmětů. Hostovaná webová verze může počítat anonymní návštěvy a akce
v appce (Umami bez cookies – jen druh akce, nikdy vaše data). Podrobnosti v appce v menu → **About & licenses**.

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
| [docs/en/](docs/en/) | – | anglické verze všech dokumentů |
| [CONTRIBUTING.md](CONTRIBUTING.md) | přispěvatel | jak hlásit chyby a posílat úpravy (anglicky) |
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
