#!/usr/bin/env python3
"""Draw the crosshair shapes: seventeen fifteen-by-fifteen sprites, white on nothing.

White, because colour is the config's business: the game tints a sprite as it draws it, and a
white sprite takes any tint exactly. Fifteen wide because vanilla's crosshair is, and every
shape here has to sit in the same frame it does so swapping between them moves nothing.

Seventeen and not eighteen: CrosshairStyle.CROSS points at vanilla's own sprite, so drawing one
here would ship a file nothing can load. This script checks itself against that enum and fails
if the two ever disagree, and writes a contact sheet so the shapes can be looked at rather than
only counted.

Deterministic; run it after changing a shape.
"""
import os
import re
import sys
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
ASSETS = "src/client/resources/assets/crosshair-corsair-justfatlard/textures/gui/sprites/crosshair"
OUT = os.path.join(HERE, ASSETS)
STYLE_ENUM = os.path.join(
    HERE, "src/client/java/justfatlard/crosshair_corsair/crosshair/CrosshairStyle.java"
)
CONTACT_SHEET = os.path.join(HERE, "build", "crosshair-contact-sheet.png")

# The one style whose sprite is vanilla's, so this script must not draw it.
VANILLA_BACKED = {"cross"}

SIZE = 15
C = 7  # the centre pixel
WHITE = (255, 255, 255, 255)


def blank():
    image = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    return image, ImageDraw.Draw(image)


def line(draw, a, b):
    draw.line([a, b], fill=WHITE)


def cross(gap):
    image, draw = blank()
    for d in (-1, 1):
        line(draw, (C + d * (gap + 1), C), (C + d * 5, C))
        line(draw, (C, C + d * (gap + 1)), (C, C + d * 5))
    if gap == 0:
        draw.point((C, C), fill=WHITE)
    return image


def diagonal(inner, outer):
    image, draw = blank()
    for dx in (-1, 1):
        for dy in (-1, 1):
            line(draw, (C + dx * inner, C + dy * inner), (C + dx * outer, C + dy * outer))
    return image


def ring(r):
    image, draw = blank()
    draw.ellipse([C - r, C - r, C + r, C + r], outline=WHITE)
    return image


def square(r):
    image, draw = blank()
    draw.rectangle([C - r, C - r, C + r, C + r], outline=WHITE)
    return image


def diamond(r):
    image, draw = blank()
    draw.polygon([(C, C - r), (C + r, C), (C, C + r), (C - r, C)], outline=WHITE)
    return image


def caret():
    image, draw = blank()
    line(draw, (C, C - 3), (C - 4, C + 1))
    line(draw, (C, C - 3), (C + 4, C + 1))
    return image


def dot():
    image, draw = blank()
    draw.point((C, C), fill=WHITE)
    return image


def brackets(top=True, bottom=True):
    image, draw = blank()
    for dx in (-1, 1):
        x = C + dx * 5
        if top and bottom:
            line(draw, (x, C - 4), (x, C + 4))
        elif top:
            line(draw, (x, C - 4), (x, C - 2))
        else:
            line(draw, (x, C + 2), (x, C + 4))
        if top:
            line(draw, (x, C - 4), (x - dx * 2, C - 4))
        if bottom:
            line(draw, (x, C + 4), (x - dx * 2, C + 4))
    return image


def brackets_round():
    image, draw = blank()
    draw.arc([C - 6, C - 5, C - 1, C + 5], 110, 250, fill=WHITE)
    draw.arc([C + 1, C - 5, C + 6, C + 5], 290, 70, fill=WHITE)
    return image


def lines(top=True, sides=True, bottom=True):
    image, draw = blank()
    if top:
        line(draw, (C, C - 5), (C, C - 3))
    if bottom:
        line(draw, (C, C + 3), (C, C + 5))
    if sides:
        line(draw, (C - 5, C), (C - 3, C))
        line(draw, (C + 3, C), (C + 5, C))
    return image


SHAPES = {
    "cross_open": cross(2),
    "cross_open_diagonal": diagonal(3, 5),
    "cross_diagonal_small": diagonal(1, 3),
    "circle": ring(4),
    "circle_large": ring(6),
    "square": square(3),
    "square_large": square(5),
    "diamond": diamond(4),
    "diamond_large": diamond(6),
    "caret": caret(),
    "dot": dot(),
    "brackets": brackets(),
    "brackets_top": brackets(bottom=False),
    "brackets_bottom": brackets(top=False),
    "brackets_round": brackets_round(),
    "lines": lines(top=False),
    "line_bottom": lines(top=False, sides=False),
}


def declared_styles():
    """The style names CrosshairStyle.java declares, lowercased to their config names."""
    source = open(STYLE_ENUM, encoding="utf-8").read()
    body = source.split("public enum CrosshairStyle {", 1)[1].split("\n\tpublic final", 1)[0]
    return [name.lower() for name in re.findall(r"^\t([A-Z][A-Z0-9_]*)\s*[,;(]", body, re.M)]


def check_against_enum():
    """Fail loudly when the drawn set and the declared set drift apart."""
    declared = set(declared_styles())
    drawn = set(SHAPES)
    problems = []
    if not declared:
        problems.append(f"could not read any style names out of {os.path.relpath(STYLE_ENUM, HERE)}")
    for missing in sorted(declared - drawn - VANILLA_BACKED):
        problems.append(f"{missing}: declared in CrosshairStyle, not drawn here")
    for extra in sorted(drawn - declared):
        problems.append(f"{extra}: drawn here, not declared in CrosshairStyle")
    for shipped in sorted(drawn & VANILLA_BACKED):
        problems.append(f"{shipped}: uses vanilla's sprite, so drawing it ships a file nothing loads")
    return problems


def contact_sheet(scale=9, columns=5, pad=8, label_height=12):
    """One image of every shape, big enough to look at."""
    names = sorted(SHAPES)
    cell = SIZE * scale
    rows = (len(names) + columns - 1) // columns
    width = columns * (cell + pad) + pad
    height = rows * (cell + pad + label_height) + pad
    sheet = Image.new("RGBA", (width, height), (32, 32, 40, 255))
    draw = ImageDraw.Draw(sheet)
    for index, name in enumerate(names):
        column, row = index % columns, index // columns
        x = pad + column * (cell + pad)
        y = pad + row * (cell + pad + label_height)
        draw.rectangle([x - 1, y - 1, x + cell, y + cell], outline=(70, 70, 82, 255))
        shape = SHAPES[name].resize((cell, cell), Image.NEAREST)
        # As its own mask, or the transparent ground is copied over the sheet and every cell
        # comes out a solid block - which is what the sprite is not.
        sheet.paste(shape, (x, y), shape)
        draw.text((x, y + cell + 2), name, fill=(190, 190, 200, 255))
    return sheet


if __name__ == "__main__":
    problems = check_against_enum()
    if problems:
        print("crosshair shapes disagree with CrosshairStyle.java:", file=sys.stderr)
        for problem in problems:
            print(f"  {problem}", file=sys.stderr)
        raise SystemExit(1)

    os.makedirs(OUT, exist_ok=True)
    for name, image in SHAPES.items():
        image.save(os.path.join(OUT, name + ".png"))

    os.makedirs(os.path.dirname(CONTACT_SHEET), exist_ok=True)
    contact_sheet().save(CONTACT_SHEET)

    print(f"  {len(SHAPES)} crosshair shapes -> {ASSETS}")
    print(f"  contact sheet -> {os.path.relpath(CONTACT_SHEET, HERE)}")
