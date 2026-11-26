# Releases and deployment

[Čeština](../deploy.md) | **English**

## Builds on GitHub (Releases)

Finished builds are not committed to git (the repo would grow by ~150 MB per version); they go to the repository's
**Releases** page: `https://github.com/DejvHeHe/pathfinder-mobile-app/releases`.
They are created automatically by the `.github/workflows/release.yml` workflow.

### Releasing a new version

```bash
git tag v1.0.0
git push origin v1.0.0
```

GitHub Actions then builds in parallel (~15 min):

| File in the Release | Platform | Runner |
|---|---|---|
| `PF-Spellbook-1.0.0.apk` | Android | ubuntu |
| `PF-Spellbook-1.0.0.msi` | Windows installer | windows |
| `PF-Spellbook-1.0.0.dmg` | macOS (unsigned) | macos |
| `PF-Spellbook-1.0.0.deb` | Linux (Debian/Ubuntu) | ubuntu |
| `PF-Spellbook-1.0.0-web.zip` | static website | ubuntu |
| Docker image `ghcr.io/dejvhehe/pf-spellbook-web:1.0.0` + `:latest` (workflow `docker.yml`) | web for a server | ubuntu |

The version from the tag goes into the app (`-PappVersion` → generated `AppVersion.kt`), the Android `versionCode` = the workflow run number.
On Android and desktop the app looks in the latest Release for a file ending in `.apk` / `.msi` / `.dmg` / `.deb` (update check,
`data/AppUpdate.kt`) – so keep the file extensions in the Release. The release must be neither a *draft* nor a *pre-release*,
otherwise the GitHub API `releases/latest` does not return it.
The desktop installers bundle their own Java – users do not need to install anything else.

**A manual run** (Actions → Release → Run workflow, enter a version) only builds the files as run *artifacts* – it creates
neither a Release nor a Docker image. Useful for testing.

`.github/workflows/ci.yml` additionally runs tests and the Android, web and iOS framework builds (macOS runner) on every
push to `main` and on pull requests. `.github/workflows/docker.yml` builds the web Docker image on every push to `main`
with the `:edge` tag, and `:X.Y.Z` + `:latest` on a version tag.

### iOS
Not part of the Release – installing outside the App Store requires signing with an Apple Developer account (99 USD/year)
or building with Xcode for your own iPhone (free account, signature valid for 7 days). On an iPhone you can use the
**web version** instead.

### Signing the Android APK (recommended)
Without setup, the release APK is signed with a random debug key of the CI runner – it installs, but **updating to a newer
version then requires uninstalling** (and losing data – run Export all first). A stable key:

1. Create a keystore once (and **keep it safe** – without it you can no longer release updates):
   ```bash
   keytool -genkeypair -v -keystore pf-spellbook.jks -alias pfspells -keyalg RSA -keysize 4096 -validity 10000
   ```
2. On GitHub: *Settings → Secrets and variables → Actions → New repository secret*:

   | Secret | Value |
   |---|---|
   | `ANDROID_KEYSTORE_BASE64` | output of `base64 -w0 pf-spellbook.jks` (PowerShell: `[Convert]::ToBase64String([IO.File]::ReadAllBytes("pf-spellbook.jks"))`) |
   | `ANDROID_KEYSTORE_PASSWORD` | keystore password |
   | `ANDROID_KEY_ALIAS` | `pfspells` |
   | `ANDROID_KEY_PASSWORD` | key password |

Locally the same variables (`ANDROID_KEYSTORE_FILE` = path to the `.jks` + passwords) sign `./gradlew :composeApp:assembleRelease`.

### macOS
The `.dmg` is neither signed nor notarized → on first launch: right-click the app → **Open** → confirm.

## Web on your own server (behind your own nginx)

Files: `Dockerfile`, `compose.yaml` (pulls the ready image), `compose.build.yaml` (optional build on the server),
`docker/nginx.conf` (server inside the container), `docker/proxy-nginx.example.conf` (example for **your** nginx),
`.dockerignore`.

Your nginx on the host handles the domain and HTTPS. There are two options for the app:

### A) Container + proxy from your nginx
The container holds only the built app and a minimal static server (nginx-alpine, ~20 MB RAM) and listens
**only on `127.0.0.1:8888`** – from outside it is reachable only through your nginx.

**The image is built on GitHub, the server only pulls it** – compiling to WebAssembly needs ~4 GB RAM and several cores,
which a small VPS cannot handle (it stops responding). That is why `compose.yaml` deliberately has no `build` section.

```bash
docker compose pull && docker compose up -d                      # latest released version (:latest)
TAG=edge docker compose pull && TAG=edge docker compose up -d    # newest code from main (:edge)
```

`TAG` can be set permanently in a `.env` next to `compose.yaml` (e.g. `TAG=edge`).

If your server is powerful enough and you want to build yourself: `docker compose -f compose.yaml -f compose.build.yaml up -d --build`.

In your nginx:

```nginx
location / {
    proxy_pass http://127.0.0.1:8888;
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-Proto $scheme;
}
```

The container already sends gzip and cache headers (`no-cache` with ETag revalidation – a new version shows up as soon as the page is reloaded). Updating: `docker compose pull && docker compose up -d`.

### B) Plain build without a container
Docker is used only for compiling; your nginx serves the resulting files directly (no extra process on the server):

```bash
docker build --target export --output type=local,dest=./web-dist .
# → web-dist/ : index.html, composeApp.js, *.wasm, composeResources/
```

or just unzip `PF-Spellbook-<version>-web.zip` from a GitHub Release. In your nginx set `root` to that folder,
`try_files $uri $uri/ /index.html;` and gzip – the full block is in `docker/proxy-nginx.example.conf` (option B).
`.wasm` must be served as `application/wasm` (the standard `mime.types` of nginx ≥ 1.21 includes it).

### Subfolder
All paths in the build are relative, the app also runs in a subfolder, e.g. `https://example.com/spellbook/`
(verified): `location /spellbook/ { proxy_pass http://127.0.0.1:8888/; }`, or `alias` for option B.

### Settings (`compose.yaml`, or a `.env` next to it)
| Variable | Default | Meaning |
|---|---|---|
| `PORT` | `8888` | port on the host's `127.0.0.1` |
| `TAG` | `latest` | `latest` = latest release, `edge` = newest `main`, or a specific `1.0.0` |

### Image from GitHub Container Registry
Built on GitHub (`docker.yml`): push to `main` → `:edge`, tag `v1.2.3` → `:1.2.3` + `:latest`.
GHCR packages are **private by default** – on GitHub set *Packages → pf-spellbook-web → Package settings* to **Public**,
or log in on the server: `echo <token with read:packages> | docker login ghcr.io -u DejvHeHe --password-stdin`.

### Analytics (Umami)
On start the container can insert the [Umami](https://umami.is) tracking script into `index.html`
(script `docker/40-umami.sh`, marker `<!--UMAMI-->` in `index.html`). The IDs are not in the repository – they are set in
`.env` on the server:

```
UMAMI_SCRIPT_URL=https://umami.example.com/script.js
UMAMI_WEBSITE_ID=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
UMAMI_DOMAINS=spellbook.example.com        # optional – count only the production domain
```

Then `docker compose up -d` (the container is recreated with the new settings). Without the variables nothing is inserted.
One Umami instance can track several sites – in Umami use *Settings → Websites → Add website* and the new Website ID.

The app is a single page (the URL does not change), so Umami records one page view per visit
(visitors, countries, devices). In addition the app sends anonymous **events** (`trackEvent` → `umami.track`, only when the
Umami script is on the page; see *Events* in Umami). Only the kind of action and generic values are sent – never names, notes or ids:

| Event | Data |
|---|---|
| `section` | `section` = `spellbook` / `inventory` (tab switch) |
| `character-create` | `class`, `level`, `race` |
| `inventory-create` | – |
| `import` | `kind` = `characters` / `inventories` / `feats`, `count` |
| `export` | `kind` = `characters` / `inventories` / `feats`, `method` = `copy` / `file` |
| `database-download` | `db` = `spells` / `items`, `ok` = `true` / `false` |
| `database-reset` | `db` = `spells` / `items` |
| `compendium-open`, `game-mode`, `rest`, `prices-revealed` | – |
| `strict-mode` | `on` = `true` / `false` |
| `companion-create` | `kind` (e.g. `animal_companion`, `mount`), `class`, `species` (e.g. `wolf`, `none`) |
| `language` | `lang` = `en` / `cs` |

### User data
The server **stores nothing**. Characters and inventories live in each user's browser (localStorage/IndexedDB, tied to the
site address). The container is stateless – updating or deleting it does not affect user data.
When the domain/address changes, the data in the browser is "lost" (it is tied to the old address) – export all characters
and inventories and import them at the new address.
