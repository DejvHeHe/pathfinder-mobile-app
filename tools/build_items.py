#!/usr/bin/env python3
"""
Builds the bundled item data for the app (composeApp/src/commonMain/composeResources/files/):

  ItemsBase.json   mundane gear, armor & shields – converted from the Foundry VTT PF1 system packs (OGL)
                   https://gitlab.com/foundryvtt_pathfinder1e/foundryvtt-pathfinder1 (packs/items, packs/armors-and-shields)
  Weapons.csv      mundane weapons      } d20pfsrd / AoN data as CSV from github.com/Vireche/randomitemgenerator
  Ammunition.csv   mundane ammunition   } (the app downloads the same files when updating the item database,
  MagicItems.csv   magic items          }  see ItemParser.kt – keep the URLs in sync)

MagicItems.csv is trimmed to the columns the app reads (the upstream file has a huge FullText HTML column).

Usage:  pip install pyyaml  &&  python tools/build_items.py
"""
import csv
import html
import io
import json
import re
import sys
import urllib.request
import zipfile
from pathlib import Path

import yaml

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "composeApp/src/commonMain/composeResources/files"

FOUNDRY_PROJECT = "16774935"  # foundryvtt_pathfinder1e/foundryvtt-pathfinder1
FOUNDRY_ZIP = "https://gitlab.com/api/v4/projects/%s/repository/archive.zip?sha=master&path=packs/%s"
VIRECHE = "https://raw.githubusercontent.com/Vireche/randomitemgenerator/master/data/"
CSV_SOURCES = {
    "Weapons.csv": VIRECHE + "weapons.csv",
    "Ammunition.csv": VIRECHE + "ammunition.csv",
    "MagicItems.csv": VIRECHE + "magic_items_full%20-%20Updated%2019Jan2020.csv",
}
MAGIC_COLUMNS = ["Name", "Aura", "CL", "Slot", "Price", "Weight", "Description", "Requirements", "Group",
                 "Source", "PriceValue", "WeightValue", "BaseItem"]

# Foundry folder -> (app category, subcategory)
FOLDERS = {
    "adventuring-gear": ("GEAR", None),
    "clothing": ("GEAR", "Clothing"),
    "food-and-drink": ("GEAR", "Food & drink"),
    "tools-and-skill-kits": ("GEAR", "Tools & kits"),
    "animal-gear": ("GEAR", "Animal gear"),
    "material-components": ("GEAR", "Material components"),
    "miscellaneous": ("GEAR", None),
    "herbs": ("ALCHEMICAL", "Herbs"),
    "alchemical-items": ("ALCHEMICAL", None),
    "animals-and-mounts": ("MOUNT", None),
    "armor-light": ("ARMOR", "Light armor"),
    "armor-medium": ("ARMOR", "Medium armor"),
    "armor-heavy": ("ARMOR", "Heavy armor"),
    "armor-unique": ("ARMOR", "Special armor"),
    "shields": ("ARMOR", "Shield"),
}
SKIP_FOLDERS = {"magic-items"}  # magic items come from MagicItems.csv
UNIT = {"gp": 1.0, "sp": 0.1, "cp": 0.01, "pp": 10.0}


def fetch(url: str) -> bytes:
    req = urllib.request.Request(url, headers={"User-Agent": "pf-spellbook-build"})
    with urllib.request.urlopen(req, timeout=120) as r:
        return r.read()


def html_to_text(s: str) -> str:
    s = re.sub(r"(?i)<br\s*/?>", "\n", s)
    s = re.sub(r"(?i)</(p|li|h\d|tr|div)>", "\n", s)
    s = re.sub(r"<[^>]+>", "", s)
    s = re.sub(r"@\w+\[[^\]]*\]\{([^}]*)\}", r"\1", s)  # Foundry links @UUID[...]{Label}
    s = html.unescape(s)
    lines = [re.sub(r"[ \t]+", " ", l).strip() for l in s.split("\n")]
    return "\n".join(l for l in lines if l)


def num(x):
    try:
        v = float(x)
        return int(v) if v == int(v) else round(v, 4)
    except (TypeError, ValueError):
        return None


def foundry_items():
    out = []
    for pack in ("items", "armors-and-shields"):
        data = fetch(FOUNDRY_ZIP % (FOUNDRY_PROJECT, pack))
        z = zipfile.ZipFile(io.BytesIO(data))
        for name in z.namelist():
            if not name.endswith(".yaml"):
                continue
            rel = name.split("/packs/" + pack + "/", 1)[-1]
            parts = rel.split("/")
            if parts[-1].startswith("_"):
                continue  # folder metadata
            folder = parts[0] if len(parts) > 1 else "miscellaneous"
            if folder in SKIP_FOLDERS:
                continue
            cat, sub = FOLDERS.get(folder, ("GEAR", None))
            d = yaml.safe_load(z.read(name))
            s = d.get("system") or {}
            price = s.get("price") or {}
            gp = num(price.get("base"))
            if gp is not None:
                gp = num(gp * UNIT.get(price.get("unit") or "gp", 1.0))
            desc = s.get("description") or {}
            text = html_to_text(desc.get("value") or desc.get("unidentified") or "")
            armor = s.get("armor") or {}
            item = {
                "name": d["name"],
                "cat": cat,
                "sub": sub,
                "price": gp,
                "weight": num((s.get("weight") or {}).get("value")),
                "desc": text,
                "source": ", ".join(x.get("id", "") for x in (s.get("sources") or []) if x.get("id")) or None,
            }
            if cat == "ARMOR":
                item.update({
                    "ac": num(armor.get("value")),
                    "maxDex": num(armor.get("dex")),
                    "acp": num(armor.get("acp")),
                    "asf": num(s.get("spellFailure")),
                })
            out.append({k: v for k, v in item.items() if v not in (None, "")})
    out.sort(key=lambda i: (i["cat"], i["name"].lower()))
    return out


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    base = foundry_items()
    (OUT / "ItemsBase.json").write_text(json.dumps(base, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
    print(f"ItemsBase.json: {len(base)} items")

    for fname, url in CSV_SOURCES.items():
        raw = fetch(url)
        try:
            text = raw.decode("utf-8")
        except UnicodeDecodeError:
            text = raw.decode("cp1252")
        if fname == "MagicItems.csv":
            csv.field_size_limit(sys.maxsize)
            rows = list(csv.DictReader(io.StringIO(text)))
            buf = io.StringIO()
            w = csv.DictWriter(buf, fieldnames=MAGIC_COLUMNS, extrasaction="ignore", lineterminator="\n")
            w.writeheader()
            for r in rows:
                w.writerow({k: (r.get(k) or "") for k in MAGIC_COLUMNS})
            text = buf.getvalue()
            print(f"{fname}: {len(rows)} rows")
        (OUT / fname).write_text(text, encoding="utf-8", newline="")
        print(f"{fname}: {len(text) // 1024} KB")


if __name__ == "__main__":
    main()
