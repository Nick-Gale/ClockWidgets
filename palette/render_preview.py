#!/usr/bin/env python3
"""Render Thirds palette previews from tokens.json for visual review.

Mirrors exactly how the platform widgets draw: glass pill (body + hairline
rim, rounded by shape.cornerRatio) with an inset rounded fill capsule,
sheen band on the fill's top and a specular streak on every pill.
Geometry comes from tokens.json "shape" ratios (aspect, gapRatio,
cornerRatio, fillInsetRatio), so this preview *is* the design under review.

Draws each period's 8-section bar in three fill states (empty, half, full)
on both a dark and a light background.
Output: palette/preview-dark.png and palette/preview-light.png
"""
import json
import re
from pathlib import Path
from PIL import Image, ImageChops, ImageDraw, ImageFont

HERE = Path(__file__).parent
TOKENS = json.loads((HERE / "tokens.json").read_text())
SHAPE = TOKENS["shape"]

SECTIONS = 8
PILL_H = 28
PILL_W = round(PILL_H * SHAPE["aspect"])
GAP = max(1, round(PILL_W * SHAPE["gapRatio"]))
RADIUS = round(PILL_H * SHAPE["cornerRatio"])
INSET = round(PILL_H * SHAPE["fillInsetRatio"])
PAD = 20
ROW_GAP = 30
LABEL_H = 18

BG = {"dark": "#15161A", "light": "#EDF0F4"}
LABEL_COL = {"dark": (170, 176, 188, 255), "light": (96, 100, 108, 255)}


def parse_rgba(s):
    m = re.match(r"rgba\((\d+),\s*(\d+),\s*(\d+),\s*([\d.]+)\)", s)
    r, g, b, a = m.groups()
    return (int(r), int(g), int(b), int(round(float(a) * 255)))


def hexrgb(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def font(size, bold=False):
    name = "DejaVuSans-Bold.ttf" if bold else "DejaVuSans.ttf"
    p = Path("/usr/share/fonts/truetype/dejavu") / name
    if p.exists():
        return ImageFont.truetype(str(p), size)
    return ImageFont.load_default()


def make_pill(glass, hair, fill, sheen, edge, frac):
    """One pill at 3x supersampling, drawn like the platform renderers."""
    s = 3
    W, H = PILL_W * s, PILL_H * s
    r = RADIUS * s
    inset = INSET * s
    im = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(im, "RGBA")
    box = [0, 0, W - 1, H - 1]

    # glass body + hairline rim
    d.rounded_rectangle(box, radius=r, fill=glass, outline=hair, width=1 * s)

    # inset fill capsule (left-anchored, grows with frac)
    if frac > 0:
        inner_w = (W - 2 * inset) * min(frac, 1.0)
        inner = [inset, inset, inset + max(inner_w, inset), H - inset]
        ir = min((H - 2 * inset) // 2, inner_w / 2)
        cd = ImageDraw.Draw(im, "RGBA")
        cd.rounded_rectangle(inner, radius=ir, fill=fill)

        # sheen band on the fill capsule's top (alpha-composite, clipped)
        band_h = int((H - 2 * inset) * SHAPE["sheenFraction"])
        band = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        ImageDraw.Draw(band).rectangle([0, 0, W, inset + band_h], fill=sheen)
        clip = Image.new("L", (W, H), 0)
        ImageDraw.Draw(clip).rounded_rectangle(inner, radius=ir, fill=255)
        band.putalpha(ImageChops.multiply(band.split()[3], clip))
        im = Image.alpha_composite(im, band)

    # specular streak near the top inside every pill (glass cue)
    spec = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    sd = ImageDraw.Draw(spec)
    sbox = [inset, inset, W - inset - 1, inset + (H - 2 * inset) // 3]
    sd.rounded_rectangle(sbox, radius=(H - 2 * inset) // 3,
                         fill=(255, 255, 255, 22))
    im.paste(spec, (0, 0), spec)

    im = im.resize((PILL_W, PILL_H), Image.LANCZOS)
    return im


def draw_state_row(img, origin, scheme, appearance, state_label, fill_state):
    x, y = origin
    pal = TOKENS["periods"][scheme][appearance]
    glass = parse_rgba(TOKENS["glass"][appearance]["body"])
    hair = parse_rgba(TOKENS["glass"][appearance]["hairline"])
    fill = hexrgb(pal["fill"]) + (255,)
    sheen = hexrgb(pal["sheen"]) + (255,)
    edge = hexrgb(pal["edge"]) + (255,)

    d = ImageDraw.Draw(img, "RGBA")
    d.text((x, y - LABEL_H + 2), state_label, fill=LABEL_COL[appearance], font=font(11))
    for i in range(SECTIONS):
        frac = max(0.0, min(1.0, fill_state - i))
        pill = make_pill(glass, hair, fill, sheen, edge, frac)
        img.paste(pill, (x + i * (PILL_W + GAP), y), pill)


def render(appearance, filename):
    schemes = list(TOKENS["periods"].keys())
    states = [("empty", 0.0), ("half", 4.0), ("full", 8.0)]

    bar_w = SECTIONS * PILL_W + (SECTIONS - 1) * GAP
    block_h = len(states) * (LABEL_H + PILL_H + 6)
    W = PAD * 2 + bar_w + 120
    H = PAD * 2 + len(schemes) * block_h + (len(schemes) - 1) * ROW_GAP

    img = Image.new("RGBA", (W, H), hexrgb(BG[appearance]) + (255,))
    d = ImageDraw.Draw(img, "RGBA")
    hf = font(13, bold=True)

    for si, scheme in enumerate(schemes):
        top = PAD + si * (block_h + ROW_GAP)
        for ci, (label, state) in enumerate(states):
            oy = top + ci * (LABEL_H + PILL_H + 6) + LABEL_H
            draw_state_row(img, (PAD, oy), scheme, appearance, label, state)
        d.text((W - 108, top + block_h // 2 - 8), scheme, fill=LABEL_COL[appearance], font=hf)

    img.convert("RGB").save(HERE / filename)
    return HERE / filename


if __name__ == "__main__":
    for app in ("dark", "light"):
        print("wrote", render(app, f"preview-{app}.png"))
