#!/bin/sh
# Vloží trackovací skript Umami do index.html při startu kontejneru (spouští ho entrypoint oficiálního nginx image).
# Nastavení přes proměnné prostředí (compose.yaml / .env):
#   UMAMI_SCRIPT_URL   např. https://umami.example.com/script.js
#   UMAMI_WEBSITE_ID   Website ID z Umami (Settings → Websites)
#   UMAMI_DOMAINS      volitelně, např. spellbook.example.com (počítat jen produkční doménu)
# Bez nich se nevloží nic. Vždy se vychází z původního index.html, takže změna proměnných + restart funguje.
set -eu

html=/usr/share/nginx/html/index.html
orig=/usr/share/nginx/html/.index.html.orig

[ -f "$orig" ] || cp "$html" "$orig"
cp "$orig" "$html"

if [ -n "${UMAMI_SCRIPT_URL:-}" ] && [ -n "${UMAMI_WEBSITE_ID:-}" ]; then
    tag="<script defer src=\"${UMAMI_SCRIPT_URL}\" data-website-id=\"${UMAMI_WEBSITE_ID}\""
    if [ -n "${UMAMI_DOMAINS:-}" ]; then
        tag="${tag} data-domains=\"${UMAMI_DOMAINS}\""
    fi
    tag="${tag}></script>"
    escaped=$(printf '%s' "$tag" | sed 's/[&|\\]/\\&/g')
    sed -i "s|<!--UMAMI-->|${escaped}|" "$html"
    echo "umami: tracking enabled (${UMAMI_SCRIPT_URL}, website ${UMAMI_WEBSITE_ID})"
else
    echo "umami: not configured (set UMAMI_SCRIPT_URL and UMAMI_WEBSITE_ID to enable)"
fi
