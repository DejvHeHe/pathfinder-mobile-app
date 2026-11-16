# Vydání a nasazení

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
| Docker image `ghcr.io/dejvhehe/pf-spellbook-web:1.0.0` + `:latest` | web pro server | ubuntu |

Verze z tagu se propíše do appky (`-PappVersion`), Android `versionCode` = číslo běhu workflow.
Desktopové instalátory obsahují vlastní Javu – uživatel nic dalšího neinstaluje.

**Ruční běh** (Actions → Release → Run workflow, zadat verzi) jen sestaví buildy jako *artifacts* běhu – Release ani Docker image nevytvoří.
Hodí se na vyzkoušení.

`.github/workflows/ci.yml` navíc při každém pushi do `main` a u pull requestů spustí testy, Android, web a iOS framework (macOS runner).

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

Soubory: `Dockerfile`, `compose.yaml`, `docker/nginx.conf` (server uvnitř kontejneru),
`docker/proxy-nginx.example.conf` (ukázka pro **vaši** nginx), `.dockerignore`.

Vaše nginx na hostiteli řeší doménu a HTTPS. Pro appku jsou dvě možnosti:

### A) Kontejner + proxy z vaší nginx
Kontejner obsahuje jen sestavenou appku a minimální statický server (nginx-alpine, ~20 MB RAM) a poslouchá
**jen na `127.0.0.1:8888`** – zvenku je dostupný pouze přes vaši nginx.

```bash
docker compose pull && docker compose up -d      # hotový image z GitHubu (vzniká při vydání verze)
# nebo
docker compose up -d --build                     # sestavit na serveru (~4 GB RAM, první build ~10 min)
```

Ve vaší nginx:

```nginx
location / {
    proxy_pass http://127.0.0.1:8888;
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-Proto $scheme;
}
```

Kontejner už posílá gzip a cache hlavičky. Aktualizace: `docker compose pull && docker compose up -d`.

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
| `TAG` | `latest` | verze image (a verze appky při buildu) |

### Image z GitHub Container Registry
Vzniká při vydání verze (`release.yml`, job `docker`). Balíčky v GHCR jsou **ve výchozím stavu soukromé** –
na GitHubu v *Packages → pf-spellbook-web → Package settings* nastavit **Public**, nebo se na serveru přihlásit:
`echo <token s read:packages> | docker login ghcr.io -u DejvHeHe --password-stdin`.

### Data uživatelů
Server **nic neukládá**. Decky a inventáře jsou v prohlížeči každého uživatele (localStorage/IndexedDB, vázané na adresu webu).
Kontejner je bezstavový – aktualizace ani smazání kontejneru data uživatelů neovlivní.
Při změně domény/adresy se ale data v prohlížeči „ztratí“ (jsou vázaná na původní adresu) – pomůže Export all / Import.
