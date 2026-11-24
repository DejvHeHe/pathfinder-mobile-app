# Vydání a nasazení

**Čeština** | [English](en/deploy.md)

## Buildy na GitHubu (Releases)

Hotové buildy se nedávají do gitu (repo by s každou verzí narostlo o ~150 MB), ale na stránku
**Releases** repa: `https://github.com/DejvHeHe/pathfinder-mobile-app/releases`.
Vytváří je automaticky workflow `.github/workflows/release.yml`.

### Vydání nové verze

```bash
git tag v1.0.0
git push origin v1.0.0
```

GitHub Actions pak paralelně sestaví (~15 min):

| Soubor v Release | Platforma | Runner |
|---|---|---|
| `PF-Spellbook-1.0.0.apk` | Android | ubuntu |
| `PF-Spellbook-1.0.0.msi` | Windows instalátor | windows |
| `PF-Spellbook-1.0.0.dmg` | macOS (nepodepsané) | macos |
| `PF-Spellbook-1.0.0.deb` | Linux (Debian/Ubuntu) | ubuntu |
| `PF-Spellbook-1.0.0-web.zip` | statický web | ubuntu |
| Docker image `ghcr.io/dejvhehe/pf-spellbook-web:1.0.0` + `:latest` (workflow `docker.yml`) | web pro server | ubuntu |

Verze z tagu se propíše do appky (`-PappVersion`), Android `versionCode` = číslo běhu workflow.
Desktopové instalátory obsahují vlastní Javu – uživatel nic dalšího neinstaluje.

**Ruční běh** (Actions → Release → Run workflow, zadat verzi) jen sestaví buildy jako *artifacts* běhu – Release ani Docker image nevytvoří.
Hodí se na vyzkoušení.

`.github/workflows/ci.yml` navíc při každém pushi do `main` a u pull requestů spustí testy, Android, web a iOS framework (macOS runner).
`.github/workflows/docker.yml` při každém pushi do `main` sestaví Docker image webu s tagem `:edge`, při tagu verze `:X.Y.Z` a `:latest`.

### iOS
Do Release se nedává – instalace mimo App Store vyžaduje podepsání Apple Developer účtem (99 USD/rok)
nebo build z Xcode na vlastní iPhone (bezplatný účet, podpis na 7 dní). Na iPhonu lze místo toho použít **webovou verzi**.

### Podepsání Android APK (doporučeno)
Bez nastavení se release APK podepíše náhodným debug klíčem CI runneru – nainstalovat jde, ale **aktualizace na novější verzi
pak vyžaduje odinstalaci** (a tím ztrátu dat – předtím Export all). Stabilní klíč:

1. Jednou vytvořit keystore (a **bezpečně ho uschovat** – bez něj už nepůjde vydat aktualizaci):
   ```bash
   keytool -genkeypair -v -keystore pf-spellbook.jks -alias pfspells -keyalg RSA -keysize 4096 -validity 10000
   ```
2. Na GitHubu: *Settings → Secrets and variables → Actions → New repository secret*:

   | Secret | Hodnota |
   |---|---|
   | `ANDROID_KEYSTORE_BASE64` | obsah `base64 -w0 pf-spellbook.jks` (PowerShell: `[Convert]::ToBase64String([IO.File]::ReadAllBytes("pf-spellbook.jks"))`) |
   | `ANDROID_KEYSTORE_PASSWORD` | heslo keystoru |
   | `ANDROID_KEY_ALIAS` | `pfspells` |
   | `ANDROID_KEY_PASSWORD` | heslo klíče |

Lokálně stejné proměnné (`ANDROID_KEYSTORE_FILE` = cesta k `.jks` + hesla) podepíšou `./gradlew :composeApp:assembleRelease`.

### macOS
`.dmg` není podepsaný ani notarizovaný → při prvním spuštění: pravé tlačítko na appku → **Otevřít** → potvrdit.

## Web na vlastním serveru (za vlastní nginx)

Soubory: `Dockerfile`, `compose.yaml` (stažení hotového image), `compose.build.yaml` (volitelný build na serveru),
`docker/nginx.conf` (server uvnitř kontejneru),
`docker/proxy-nginx.example.conf` (ukázka pro **vaši** nginx), `.dockerignore`.

Vaše nginx na hostiteli řeší doménu a HTTPS. Pro appku jsou dvě možnosti:

### A) Kontejner + proxy z vaší nginx
Kontejner obsahuje jen sestavenou appku a minimální statický server (nginx-alpine, ~20 MB RAM) a poslouchá
**jen na `127.0.0.1:8888`** – zvenku je dostupný pouze přes vaši nginx.

**Image se sestavuje na GitHubu, server ho jen stáhne** – kompilace do WebAssembly potřebuje ~4 GB RAM a několik jader
a na malé VPS ji server nezvládne (přestane odpovídat). Proto `compose.yaml` záměrně nemá sekci `build`.

```bash
docker compose pull && docker compose up -d                      # poslední vydaná verze (:latest)
TAG=edge docker compose pull && TAG=edge docker compose up -d    # nejnovější kód z main (:edge)
```

`TAG` jde trvale nastavit v `.env` vedle `compose.yaml` (např. `TAG=edge`).

Kdo má dost výkonný server a chce stavět sám: `docker compose -f compose.yaml -f compose.build.yaml up -d --build`.

Ve vaší nginx:

```nginx
location / {
    proxy_pass http://127.0.0.1:8888;
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-Proto $scheme;
}
```

Kontejner už posílá gzip a cache hlavičky (`no-cache` s revalidací přes ETag – nová verze se v prohlížeči projeví hned po obnovení stránky). Aktualizace: `docker compose pull && docker compose up -d`.

### B) Čisté sestavení bez kontejneru
Docker se použije jen ke kompilaci, výsledné soubory servíruje vaše nginx přímo (žádný další proces na serveru):

```bash
docker build --target export --output type=local,dest=./web-dist .
# → web-dist/ : index.html, composeApp.js, *.wasm, composeResources/
```

nebo stačí rozbalit `PF-Spellbook-<verze>-web.zip` z GitHub Release. Ve vaší nginx pak `root` na tu složku,
`try_files $uri $uri/ /index.html;` a gzip – kompletní blok je v `docker/proxy-nginx.example.conf` (varianta B).
`.wasm` musí mít typ `application/wasm` (standardní `mime.types` nginx ≥ 1.21 ho obsahuje).

### Podsložka
Všechny cesty v buildu jsou relativní, appka běží i v podsložce, např. `https://example.com/spellbook/`
(ověřeno): `location /spellbook/ { proxy_pass http://127.0.0.1:8888/; }`, resp. `alias` u varianty B.

### Nastavení (`compose.yaml`, případně `.env` vedle něj)
| Proměnná | Výchozí | Význam |
|---|---|---|
| `PORT` | `8888` | port na `127.0.0.1` hostitele |
| `TAG` | `latest` | `latest` = poslední vydaná verze, `edge` = nejnovější `main`, nebo konkrétní `1.0.0` |

### Image z GitHub Container Registry
Vzniká na GitHubu (`docker.yml`): push do `main` → `:edge`, tag `v1.2.3` → `:1.2.3` + `:latest`.
Balíčky v GHCR jsou **ve výchozím stavu soukromé** –
na GitHubu v *Packages → pf-spellbook-web → Package settings* nastavit **Public**, nebo se na serveru přihlásit:
`echo <token s read:packages> | docker login ghcr.io -u DejvHeHe --password-stdin`.

### Analytika (Umami)
Kontejner umí při startu vložit trackovací skript [Umami](https://umami.is) do `index.html`
(skript `docker/40-umami.sh`, značka `<!--UMAMI-->` v `index.html`). ID se nedávají do repozitáře – nastavují se v `.env` na serveru:

```
UMAMI_SCRIPT_URL=https://umami.example.cz/script.js
UMAMI_WEBSITE_ID=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
UMAMI_DOMAINS=spellbook.example.cz        # volitelné – počítat jen produkční doménu
```

Pak `docker compose up -d` (kontejner se vytvoří znovu s novým nastavením). Bez proměnných se nic nevkládá.
Jedna instance Umami zvládne víc webů – v Umami stačí *Settings → Websites → Add website* a použít nové Website ID.

Appka je jednostránková (URL se nemění), Umami proto zaznamená jedno zobrazení stránky na návštěvu
(návštěvníci, země, zařízení). Navíc appka posílá anonymní **události** (`trackEvent` → `umami.track`, jen když je skript
Umami na stránce; v Umami *Events*). Posílá se jen druh akce a obecné údaje, nikdy jména, poznámky ani id:

| Událost | Data |
|---|---|
| `section` | `section` = `spellbook` / `inventory` (přepnutí záložky) |
| `character-create` | `class`, `level`, `race` |
| `inventory-create` | – |
| `import` | `kind` = `characters` / `inventories` / `feats`, `count` |
| `export` | `kind` = `characters` / `inventories` / `feats`, `method` = `copy` / `file` |
| `database-download` | `db` = `spells` / `items`, `ok` = `true` / `false` |
| `database-reset` | `db` = `spells` / `items` |
| `compendium-open`, `game-mode`, `rest`, `prices-revealed` | – |
| `strict-mode` | `on` = `true` / `false` |
| `companion-create` | `kind` (např. `animal_companion`, `mount`), `class`, `species` (např. `wolf`, `none`) |
| `language` | `lang` = `en` / `cs` |

### Data uživatelů
Server **nic neukládá**. Postavy a inventáře jsou v prohlížeči každého uživatele (localStorage/IndexedDB, vázané na adresu webu).
Kontejner je bezstavový – aktualizace ani smazání kontejneru data uživatelů neovlivní.
Při změně domény/adresy se ale data v prohlížeči „ztratí“ (jsou vázaná na původní adresu) – pomůže export všech postav a inventářů a import na nové adrese.
