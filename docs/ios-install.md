# Instalace na iPhone (vlastní build přes Xcode)

**Čeština** | [English](en/ios-install.md)

Apple neumožňuje šířit hotový instalační soubor bez placeného vývojářského účtu, ale každý s **Macem**
si appku může sestavit a nahrát na svůj iPhone sám – stačí **bezplatné Apple ID**.

> Pozn.: build na Macu je vyzkoušený. Když přesto něco selže, pošlete chybovou hlášku z Xcode
> (Report navigator – ikona bubliny vlevo) a opravíme to.
>
> Alternativa bez Macu: **webová verze** v Safari (iOS 18.2+) → Sdílet → *Přidat na plochu*.

## Co je potřeba

- Mac s macOS 14 nebo novějším
- **Xcode** (zdarma z App Store, ~10 GB)
- **Java 17+** – např. [Temurin 21](https://adoptium.net/) (`.pkg` instalátor), nebo `brew install --cask temurin@21`
- iPhone s iOS 15+ a USB kabel
- Apple ID (stačí běžné, bezplatné)

Android SDK **není** potřeba.

## Postup

1. **Stáhnout projekt**
   ```bash
   git clone https://github.com/DejvHeHe/pathfinder-mobile-app.git
   cd pathfinder-mobile-app
   ```
2. **Přidat Apple ID do Xcode:** Xcode → *Settings* → *Accounts* → **+** → Apple ID.
3. **Vyplnit Team ID:** v Accounts klikněte na svůj účet → tým *(Personal Team)* – zkopírujte jeho ID
   (10 znaků, např. `AB12CD34EF`) do souboru `iosApp/Configuration/Config.xcconfig`:
   ```
   TEAM_ID=AB12CD34EF
   ```
   Tím dostane appka i vlastní bundle ID, takže ji může stavět víc lidí najednou.
4. **Otevřít projekt:** dvojklik na `iosApp/iosApp.xcodeproj`.
   V *Signing & Capabilities* (cíl `iosApp`) by měl být vybraný váš tým – když ne, vyberte ho ručně.
5. **Připravit iPhone:**
   - připojit kabelem, na telefonu potvrdit *Důvěřovat tomuto počítači*,
   - zapnout **Režim vývojáře**: *Nastavení → Soukromí a zabezpečení → Režim vývojáře* → zapnout → restart telefonu
     (volba se objeví až po prvním připojení k Xcode).
6. **Spustit:** nahoře v Xcode vyberte jako cíl svůj iPhone a stiskněte **▶**.
   První build trvá **10–20 minut** (stahuje a kompiluje Kotlin část appky), další už jsou rychlé.
7. **Povolit vývojáře na telefonu:** při prvním spuštění iPhone hlásí *Nedůvěryhodný vývojář* →
   *Nastavení → Obecné → Správa VPN a zařízení* → vaše Apple ID → **Důvěřovat**.

## Omezení bezplatného účtu

- Podpis platí **7 dní** – pak appka nejde spustit. Stačí telefon znovu připojit a v Xcode dát ▶
  (data – postavy a inventáře – zůstanou, pokud appku nesmažete).
- Najednou můžete mít z bezplatného účtu nainstalované max. 3 vlastní appky.
- Pro jistotu občas *Export all* (menu ⋮) – kdyby se appka smazala, data jdou zpět importovat.

## Aktualizace na novou verzi

```bash
git pull
```
a v Xcode znovu **▶**.

## Když něco nefunguje

| Hláška | Řešení |
|---|---|
| `Unable to locate a Java Runtime` / `JAVA_HOME is not set` | nainstalovat Javu 17+ (viz výše). Xcode nevidí proměnné z Terminálu – případně do `~/.gradle/gradle.properties` přidat `org.gradle.java.home=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home` |
| `Failed to register bundle identifier` / `… is not available` | vyplnit `TEAM_ID` v `Config.xcconfig` (krok 3) |
| `No Account for Team` / `Signing requires a development team` | v *Signing & Capabilities* vybrat tým (krok 4) |
| `Developer Mode disabled` | zapnout Režim vývojáře (krok 5) |
| `Untrusted Developer` | povolit v Nastavení (krok 7) |
| chyba ve fázi *Compile Kotlin Framework* | celý výpis z Report navigatoru poslat autorovi |
