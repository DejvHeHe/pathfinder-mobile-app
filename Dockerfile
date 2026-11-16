# syntax=docker/dockerfile:1
#
# Webová verze PF1 Spellbook (Kotlin/Wasm) – sestavení + nginx.
#
#   docker compose up -d --build        → 127.0.0.1:8888 pro reverzní proxy (port změníte proměnnou PORT)
#
# Fáze 1 sestaví web Gradlem (Kotlin si sám stáhne Node.js, webpack a Binaryen), fáze 2 ho servíruje nginxem.
# Build potřebuje ~4 GB RAM a první trvá ~10 min; další díky cache závislostí výrazně kratší.
# Na slabý server je lepší hotový image z GitHub Container Registry (viz compose.yaml a docs/deploy.md).

ARG APP_VERSION=1.0.0

# ---------- 1) build ----------
FROM eclipse-temurin:21-jdk AS build
ARG APP_VERSION
WORKDIR /src

# Android cíl projektu potřebuje při konfiguraci jen existující SDK složku – pro web se nic z SDK nestahuje.
ENV ANDROID_HOME=/opt/android-sdk-empty \
    GRADLE_USER_HOME=/gradle-home
RUN mkdir -p "$ANDROID_HOME"

COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle
COPY composeApp/build.gradle.kts ./composeApp/
COPY composeApp/src ./composeApp/src

RUN --mount=type=cache,target=/gradle-home \
    chmod +x gradlew && \
    ./gradlew :composeApp:wasmJsBrowserDistribution \
        -PappVersion=${APP_VERSION} \
        -Dorg.gradle.jvmargs=-Xmx2g \
        -Pkotlin.compiler.execution.strategy=in-process \
        --no-daemon --console=plain

# ---------- 2a) jen sestavené soubory (bez serveru) ----------
#   docker build --target export --output type=local,dest=./web-dist .
# → složka web-dist/ se statickým webem, kterou může servírovat vlastní nginx přímo (viz docker/proxy-nginx.example.conf)
FROM scratch AS export
COPY --from=build /src/composeApp/build/dist/wasmJs/productionExecutable /

# ---------- 2b) runtime: minimální statický server, na který ukazuje vlastní reverzní proxy ----------
FROM nginx:1.27-alpine AS runtime
LABEL org.opencontainers.image.title="PF1 Spellbook (web)" \
      org.opencontainers.image.source="https://github.com/DejvHeHe/pathfinder-mobile-app"
COPY docker/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /src/composeApp/build/dist/wasmJs/productionExecutable /usr/share/nginx/html
EXPOSE 80
HEALTHCHECK --interval=30s --timeout=3s --retries=3 CMD wget -qO- http://127.0.0.1/healthz || exit 1
