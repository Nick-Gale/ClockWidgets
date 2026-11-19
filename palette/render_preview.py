#!/usr/bin/env python3
"""Render Thirds palette previews from tokens.json for visual review.

Draws each period's 8-section bar in three fill states (empty, half, full),
on both a dark and a light background, using the glass/fill/sheen tokens.
Output: palette/preview-dark.png and palette/preview-light.png
"""
import json
import re
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

HERE = Path(__file__).parent
TOKENS = json.loads((HERE / "tokens.json").read_text())

SECTIONS = 8
PILL_H = 28
PILL_W = 46
GAP = 6
PAD = 20
ROW_GAP = 30            # vertical gap between fill-state rows
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


def make_pill(pal, glass, hair, fill, sheen, frac):
    """Render one section pill at scale=3 as an RGBA image, then downscale.

    frac: 0 = empty glass, 1 = fully filled, fraction = partial from left.
    """
    s = 3  # supersample factor
    W, H = PILL_W * s, PILL_H * s
    r = (PILL_H // 2) * s
    im = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(im, "RGBA")
    box = [0, 0, W - 1, H - 1]

    # base glass
    d.rounded_rectangle(box, radius=r, fill=glass, outline=hair, width=1 * s)

    if frac > 0:
        # fill region (left portion), clipped to the pill
        fill_w = int(W * min(frac, 1.0))
        clip = Image.new("L", (W, H), 0)
        ImageDraw.Draw(clip).rounded_rectangle(box, radius=r, fill=255)
        colour = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        cd = ImageDraw.Draw(colour)
        cd.rectangle([0, 0, fill_w, H], fill=fill)
        # glassy sheen band over the top ~42% of the filled area
        cd.rectangle([0, 0, fill_w, int(H * 0.42)], fill=sheen)
        im.paste(colour, (0, 0), clip)

        # edge line at the leading fill boundary
        d.rounded_rectangle(box, radius=r, outline=None)
        if 0 < frac < 1 and fill_w < W - s:
            d.line([fill_w, s, fill_w, H - s], fill=sheen, width=1 * s)

    im = im.resize((PILL_W, PILL_H), Image.LANCZOS)
    return im


def draw_state_row(img, origin, scheme, appearance, state_label, fill_state):
    x, y = origin
    pal = TOKENS["periods"][scheme][appearance]
    glass = parse_rgba(TOKENS["glass"][appearance]["body"])
    hair = parse_rgba(TOKENS["glass"][appearance]["hairline"])
    fill = hexrgb(pal["fill"]) + (255,)
    sheen = hexrgb(pal["sheen"]) + (255,)

    d = ImageDraw.Draw(img, "RGBA")
    d.text((x, y - LABEL_H + 2), state_label, fill=LABEL_COL[appearance], font=font(11))
    for i in range(SECTIONS):
        frac = max(0.0, min(1.0, fill_state - i))
        pill = make_pill(pal, glass, hair, fill, sheen, frac)
        img.paste(pill, (x + i * (PILL_W + GAP), y), pill)


def render(appearance, filename):
    schemes = list(TOKENS["periods"].keys())
    states = [("empty", 0.0), ("half", 4.0), ("full", 8.0)]

    bar_w = SECTIONS * PILL_W + (SECTIONS - 1) * GAP
    block_h = len(states) * (LABEL_H + PILL_H + 6)
    W = PAD * 2 + bar_w + 120          # extra right gutter for scheme label
    H = PAD * 2 + len(schemes) * block_h + (len(schemes) - 1) * ROW_GAP

    img = Image.new("RGBA", (W, H), hexrgb(BG[appearance]) + (255,))
    d = ImageDraw.Draw(img, "RGBA")
    hf = font(13, bold=True)

    for si, scheme in enumerate(schemes):
        top = PAD + si * (block_h + ROW_GAP)
        for ci, (label, state) in enumerate(states):
            oy = top + ci * (LABEL_H + PILL_H + 6) + LABEL_H
            draw_state_row(img, (PAD, oy), scheme, appearance, label, state)
        # right-aligned scheme header, vertically centred on the block
        d.text((W - 108, top + block_h // 2 - 8), scheme, fill=LABEL_COL[appearance], font=hf)

    img.convert("RGB").save(HERE / filename)
    return HERE / filename


if __name__ == "__main__":
    for app in ("dark", "light"):
        print("wrote", render(app, f"preview-{app}.png"))
