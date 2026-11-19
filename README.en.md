# PF1 Spellbook

[Čeština](README.md) | **English**

An offline companion app for **Pathfinder 1e** players: **characters** (multiclass, subclasses, spellbook, daily slots,
skills, feats, abilities) and an **inventory** backed by an item database. No account and no server – everything is stored
locally on your device.

Runs on **Android, iOS, desktop** (Windows, macOS, Linux) **and in the browser** (Kotlin/Wasm) – one shared
Kotlin Multiplatform + Compose Multiplatform app.

| | |
|---|---|
| Spells | 3,030 spells (Archives of Nethys), filtered by class and level, full details |
| Characters | 80 races (ability modifiers, size), 49 classes (multiclass, BAB, saves), alignment, 463 deities, subclasses (domains and subdomains of your deity, mysteries, bloodlines, arcane schools), export / import |
| Spellcasting | spells per day and spells known from the AoN class tables, ability bonus slots, game mode with casting and resting |
| Skills | 26 skills, ranks, class skills, skill points, ability and racial bonuses |
| Feats | 3,442 feats (Archives of Nethys), automatic prerequisite checks (abilities, skills, class features, race, alignment, deity, proficiencies…) ✓ / ✗ / ?, manual confirmation, feat count by level and race, custom feats |
| Abilities | Smite Evil, Rage, Ki Pool… with uses per day by level, custom counters |
| Inventory | ~6,060 items (d20pfsrd + Foundry VTT PF1), your own item catalog, potions / scrolls / wands from spells, coins, carrying capacity, notes, export / import |
| Updates | spell and item databases can be re-downloaded from their sources with one tap |
| Look | "Ruled notebook" design, phone layout and a three-column wide layout |
| Language | English and Czech UI (EN / CZ switch in the header) |

## Download

Ready-made builds (Android `.apk`, Windows `.msi`, macOS `.dmg`, Linux `.deb`, web `.zip`) are on the
**[Releases](https://github.com/DejvHeHe/pathfinder-mobile-app/releases)** page. The web version can also be deployed with Docker:

```bash
docker compose pull && docker compose up -d   # 127.0.0.1:8888 for your reverse proxy – see docs/en/deploy.md
```

## Quick start

```bash
# JDK 17+
./gradlew :composeApp:run              # desktop
./gradlew :composeApp:installDebug     # Android on a connected phone / emulator
./gradlew :composeApp:wasmJsBrowserDevelopmentRun   # web on http://localhost:8080
./gradlew :composeApp:desktopTest      # tests
```

iOS: build the app yourself on a Mac with Xcode and a free Apple ID – step-by-step guide:
[docs/en/ios-install.md](docs/en/ios-install.md).

## Documentation

| File | For | About |
|---|---|---|
| [docs/en/user-guide.md](docs/en/user-guide.md) | players | how to use the app: characters, spells, skills, feats, inventory, export, updates, hidden prices |
| [docs/en/architecture.md](docs/en/architecture.md) | developers | how it works inside: layers, state, navigation, UI, platforms |
| [docs/en/data.md](docs/en/data.md) | developers | data sources and formats, updates, PF1 rules in code |
| [docs/en/build.md](docs/en/build.md) | developers | building each platform, tests, design previews, regenerating data |
| [docs/en/ios-install.md](docs/en/ios-install.md) | Mac users | installing on your own iPhone with Xcode (free Apple ID) |
| [docs/en/deploy.md](docs/en/deploy.md) | developers / admins | releases on GitHub (CI), APK signing, web in Docker, HTTPS |

## License

Copyright (C) 2026 David Sobíšek. The source code is licensed under the **GNU GPL v3** – see [`LICENSE`](LICENSE).

The game content (spells, feats, items…) is **not** under the GPL – it remains under the Open Game License v1.0a and the
Paizo Community Use Policy (so the app is free and must never be charged for). Data sources, the OGL text with
Section 15, the Paizo notice, fonts and libraries: [`NOTICE.md`](NOTICE.md). The same is in the app under
menu → **About & licenses**.

*This app uses trademarks and/or copyrights owned by Paizo Inc., used under Paizo's Community Use Policy
([paizo.com/licenses/communityuse](https://paizo.com/licenses/communityuse)). We are expressly prohibited from charging you
to use or access this content. This app is not published, endorsed, or specifically approved by Paizo.
For more information about Paizo Inc. and Paizo products, visit [paizo.com](https://paizo.com).*
