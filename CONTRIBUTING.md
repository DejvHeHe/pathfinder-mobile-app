# Contributing

Thanks for your interest in PF1 Spellbook! Issues and pull requests are welcome – in English or Czech.

## Reporting bugs and ideas

Open an [issue](https://github.com/DejvHeHe/pathfinder-mobile-app/issues) with:
- the platform (Android / iOS / desktop / web) and app version (menu → **About & licenses**),
- what you did, what you expected and what happened,
- for data problems (a wrong spell, feat or prerequisite) the name of the spell / feat and, ideally, a link to
  [Archives of Nethys](https://aonprd.com).

## Development setup

```bash
git clone https://github.com/DejvHeHe/pathfinder-mobile-app.git
cd pathfinder-mobile-app
./gradlew :composeApp:run            # desktop is the quickest way to try changes
./gradlew :composeApp:desktopTest    # run the tests before opening a PR
```

You need JDK 17+; the Android SDK and Xcode only for those platforms. Details: [docs/en/build.md](docs/en/build.md),
architecture overview: [docs/en/architecture.md](docs/en/architecture.md).

## Guidelines

- **Shared code first** – almost everything lives in `composeApp/src/commonMain`; no JVM-only APIs there
  (`String.format`, `toSortedMap`…). Platform code goes to `expect`/`actual`.
- **Every UI text in both languages** – add it to `Strings.kt` (`interface Strings`, `EnStrings`, `CsStrings`), never hard-code
  strings in screens. Game data (spell, feat, item names) stays in English.
- **Web has no fallback fonts** – use only characters covered by the bundled fonts (Latin, Czech diacritics, common
  punctuation); for symbols use icons (`NbIcons`).
- **Saved data must keep loading** – new fields need default values; a breaking change needs a new storage key and a migration.
- **Design** – follow the "ruled notebook" tokens in `ui/Theme.kt`; check UI changes with the preview renderer:
  `./gradlew :composeApp:desktopTest --tests "cz.pfspells.PreviewRender" -Ppreviews=true` (PNGs in `composeApp/build/previews`).
- **Generated data** (`*Table.kt`, bundled JSON/CSV) is produced by the scripts in `tools/` – change the script, not the output.
- Add or update tests in `composeApp/src/commonTest` / `desktopTest`, and keep `docs/` (Czech) and `docs/en/` (English) in sync.

## Licensing

The code is GPL-3.0; by contributing you agree that your contribution is licensed under the same terms.
Game content is used under the OGL 1.0a and the Paizo Community Use Policy – the app must stay free (no fees, no ads),
and new data sources must be credited in `NOTICE.md`, the OGL Section 15 and the About screen.
