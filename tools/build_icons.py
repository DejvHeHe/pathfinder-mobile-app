#!/usr/bin/env python3
"""
Ikony appky ze zdroje `art/icon.svg` (viewBox 0 0 100 100) pro všechny platformy.

    python tools/build_icons.py [--chrome PATH]

Potřebuje Pillow a Chrome / Chromium (SVG se renderuje headless prohlížečem – ImageMagick ho kreslí špatně).
Vytvoří:
  composeApp/src/androidMain/res/drawable/ic_launcher_foreground.xml   adaptivní ikona (VectorDrawable z SVG)
  composeApp/src/androidMain/res/mipmap-anydpi-v26/ic_launcher(.xml|_round.xml)
  composeApp/src/androidMain/res/mipmap-*/ic_launcher(.png|_round.png)   Android 7 (API 24–25)
  composeApp/src/androidMain/res/values/ic_launcher_background.xml
  composeApp/icons/icon.ico | icon.icns | icon.png                      instalátory MSI / DMG / DEB
  composeApp/src/desktopMain/resources/icon.png                         ikona okna desktopu
  iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/AppIcon.png          iOS (1024, bez průhlednosti)
  composeApp/src/wasmJsMain/resources/favicon.svg | apple-touch-icon.png | icon-512.png   web
"""
import argparse
import shutil
import subprocess
import sys
import tempfile
import xml.etree.ElementTree as ET
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "art" / "icon.svg"
BG = "#2E2418"  # tmavé pozadí ikony (= Nb inkoust) – pozadí adaptivní ikony Androidu
NS = "{http://www.w3.org/2000/svg}"


def render(svg: Path, size: int, chrome: str) -> Image.Image:
    """SVG → čtvercový PNG přes headless Chrome (okno je vyšší – headless ubírá spodek viewportu)."""
    with tempfile.TemporaryDirectory() as tmp:
        html = Path(tmp) / "r.html"
        out = Path(tmp) / "out.png"
        html.write_text(
            f'<!doctype html><html><body style="margin:0"><img src="{svg.as_uri()}" '
            f'style="display:block;width:{size}px;height:{size}px"></body></html>'
        )
        subprocess.run(
            [chrome, "--headless=new", "--disable-gpu", "--hide-scrollbars",
             f"--window-size={size},{size + 300}", f"--screenshot={out}", html.as_uri()],
            check=True, capture_output=True, timeout=120,
        )
        return Image.open(out).convert("RGB").crop((0, 0, size, size))


def rounded(im: Image.Image, radius_frac: float, scale: float = 1.0) -> Image.Image:
    """Zakulacený čtverec (volitelně zmenšený na `scale` a vycentrovaný) s průhledným okolím."""
    size = im.width
    inner = round(size * scale)
    body = im.resize((inner, inner), Image.LANCZOS).convert("RGBA")
    mask = Image.new("L", (inner * 4, inner * 4), 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, inner * 4 - 1, inner * 4 - 1), round(inner * 4 * radius_frac), fill=255)
    body.putalpha(mask.resize((inner, inner), Image.LANCZOS))
    out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    off = (size - inner) // 2
    out.paste(body, (off, off), body)
    return out


def circle(im: Image.Image) -> Image.Image:
    size = im.width
    mask = Image.new("L", (size * 4, size * 4), 0)
    ImageDraw.Draw(mask).ellipse((0, 0, size * 4 - 1, size * 4 - 1), fill=255)
    out = im.convert("RGBA")
    out.putalpha(mask.resize((size, size), Image.LANCZOS))
    return out


# ---------- SVG → Android VectorDrawable ----------

def color(c: str) -> str:
    return c.upper()


def paint_attrs(el, ind: str) -> list:
    a = []
    fill = el.get("fill")
    if fill and fill != "none":
        a.append(f'android:fillColor="{color(fill)}"')
        if el.get("fill-opacity"):
            a.append(f'android:fillAlpha="{el.get("fill-opacity")}"')
    stroke = el.get("stroke")
    if stroke and stroke != "none":
        a.append(f'android:strokeColor="{color(stroke)}"')
        if el.get("stroke-opacity"):
            a.append(f'android:strokeAlpha="{el.get("stroke-opacity")}"')
        a.append(f'android:strokeWidth="{el.get("stroke-width", "1")}"')
        if el.get("stroke-linejoin"):
            a.append(f'android:strokeLineJoin="{el.get("stroke-linejoin")}"')
    return a


def path_data(el) -> str:
    tag = el.tag.replace(NS, "")
    f = lambda k, d="0": float(el.get(k, d))
    if tag == "rect":
        x, y, w, h = f("x"), f("y"), f("width"), f("height")
        return f"M{x:g},{y:g}h{w:g}v{h:g}h{-w:g}z"  # rx se zanedbává (rohy jsou mimo ikonu)
    if tag == "line":
        return f"M{f('x1'):g},{f('y1'):g}L{f('x2'):g},{f('y2'):g}"
    if tag == "ellipse":
        cx, cy, rx, ry = f("cx"), f("cy"), f("rx"), f("ry")
        return f"M{cx - rx:g},{cy:g}a{rx:g},{ry:g} 0 1,0 {2 * rx:g},0a{rx:g},{ry:g} 0 1,0 {-2 * rx:g},0z"
    if tag == "polygon":
        pts = el.get("points").split()
        return "M" + "L".join(pts) + "Z"
    raise ValueError(f"nepodporovaný prvek {tag}")


def convert_children(parent, ind: str, out: list):
    for el in parent:
        tag = el.tag.replace(NS, "")
        if tag == "defs":
            continue
        if tag == "g":
            attrs = []
            tr = el.get("transform", "")
            if tr.startswith("rotate("):
                deg, px, py = tr[len("rotate("):-1].replace(",", " ").split()
                attrs += [f'android:rotation="{deg}"', f'android:pivotX="{px}"', f'android:pivotY="{py}"']
            out.append(f"{ind}<group {' '.join(attrs)}>" if attrs else f"{ind}<group>")
            if el.get("clip-path"):
                out.append(f'{ind}    <clip-path android:pathData="M0,0h100v100h-100z" />')
            convert_children(el, ind + "    ", out)
            out.append(f"{ind}</group>")
            continue
        attrs = [f'android:pathData="{path_data(el)}"'] + paint_attrs(el, ind)
        out.append(f"{ind}<path\n{ind}    " + f"\n{ind}    ".join(attrs) + " />")


def vector_drawable(svg: Path) -> str:
    """Popředí adaptivní ikony: celý obrázek 100×100 zmenšený na 76 dp z 108 dp.

    Maska launcheru ukáže nejvýš prostředních 72 dp, takže okraje čtverce (76 dp) nejsou nikdy vidět
    a pozadí obrázku navazuje na barvu pozadí adaptivní ikony.
    """
    root = ET.parse(svg).getroot()
    vp = 100 * 108 / 76
    off = (vp - 100) / 2
    body = []
    convert_children(root, "        ", body)
    return (
        "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
        "<!-- GENEROVÁNO tools/build_icons.py z art/icon.svg – needitovat ručně -->\n"
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        '    android:width="108dp"\n    android:height="108dp"\n'
        f'    android:viewportWidth="{vp:.3f}"\n    android:viewportHeight="{vp:.3f}">\n'
        f'    <group android:translateX="{off:.3f}" android:translateY="{off:.3f}">\n'
        + "\n".join(body) + "\n    </group>\n</vector>\n"
    )


ADAPTIVE = """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
"""


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--chrome", default=None, help="cesta k Chrome / Chromium")
    args = ap.parse_args()
    chrome = args.chrome or next(
        (c for c in ("google-chrome", "chromium", "chromium-browser", "chrome") if shutil.which(c)), None)
    if not chrome:
        sys.exit("Chrome / Chromium nenalezen – zadejte --chrome PATH")

    big = render(SRC, 1024, chrome)

    # Android
    res = ROOT / "composeApp/src/androidMain/res"
    (res / "drawable").mkdir(parents=True, exist_ok=True)
    (res / "drawable/ic_launcher_foreground.xml").write_text(vector_drawable(SRC), encoding="utf-8")
    (res / "mipmap-anydpi-v26").mkdir(parents=True, exist_ok=True)
    for name in ("ic_launcher.xml", "ic_launcher_round.xml"):
        (res / "mipmap-anydpi-v26" / name).write_text(ADAPTIVE, encoding="utf-8")
    (res / "values").mkdir(parents=True, exist_ok=True)
    (res / "values/ic_launcher_background.xml").write_text(
        '<?xml version="1.0" encoding="utf-8"?>\n<resources>\n'
        f'    <color name="ic_launcher_background">{BG}</color>\n</resources>\n', encoding="utf-8")
    for dpi, px in (("mdpi", 48), ("hdpi", 72), ("xhdpi", 96), ("xxhdpi", 144), ("xxxhdpi", 192)):
        d = res / f"mipmap-{dpi}"
        d.mkdir(parents=True, exist_ok=True)
        rounded(big, 0.18, 0.92).resize((px, px), Image.LANCZOS).save(d / "ic_launcher.png", optimize=True)
        circle(big).resize((px, px), Image.LANCZOS).save(d / "ic_launcher_round.png", optimize=True)

    # desktop – instalátory a ikona okna
    icons = ROOT / "composeApp/icons"
    icons.mkdir(parents=True, exist_ok=True)
    win = rounded(big, 0.12)
    win.save(icons / "icon.ico", sizes=[(s, s) for s in (16, 24, 32, 48, 64, 128, 256)])
    rounded(big, 0.225, 824 / 1024).save(icons / "icon.icns")  # macOS: zakulacený čtverec s okrajem dle Apple
    win.resize((512, 512), Image.LANCZOS).save(icons / "icon.png", optimize=True)
    desk = ROOT / "composeApp/src/desktopMain/resources"
    desk.mkdir(parents=True, exist_ok=True)
    win.resize((256, 256), Image.LANCZOS).save(desk / "icon.png", optimize=True)

    # iOS – 1024 bez alfa kanálu (zakulacení dělá systém)
    big.save(ROOT / "iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/AppIcon.png", optimize=True)

    # web
    web = ROOT / "composeApp/src/wasmJsMain/resources"
    shutil.copyfile(SRC, web / "favicon.svg")
    big.resize((180, 180), Image.LANCZOS).save(web / "apple-touch-icon.png", optimize=True)
    big.resize((512, 512), Image.LANCZOS).save(web / "icon-512.png", optimize=True)
    print("hotovo")


if __name__ == "__main__":
    main()
