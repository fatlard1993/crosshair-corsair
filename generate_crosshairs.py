#!/usr/bin/env python3
"""Draw the crosshair shapes: seventeen fifteen-by-fifteen sprites, white on nothing.

White, because colour is the config's business: the game tints a sprite as it draws it, and a
white sprite takes any tint exactly. Fifteen wide because vanilla's crosshair is, and every
shape here has to sit in the same frame it does so swapping between them moves nothing.

Deterministic; run it after changing a shape.
"""
import os
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "src/client/resources/assets/crosshair-corsair-justfatlard/textures/gui/sprites/crosshair")
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
    "cross": cross(0),
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

if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    for name, image in SHAPES.items():
        image.save(os.path.join(OUT, name + ".png"))
    print(f"  {len(SHAPES)} crosshair shapes -> {os.path.relpath(OUT, HERE)}")
