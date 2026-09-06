# Crosshair Corsair

Opinionated tweaks to the crosshair, to whatever it is pointing at - and to what happens when it
is pointing at nothing.

Client-side only. Nothing needs to be installed on the server.

## Reacharound placement

Bridging in vanilla goes: look down at the side of the block under your feet, place, look back up,
step forward, look down again. The block you want is never in doubt; the looking is ceremony.

With this on, standing at an edge with blocks in hand and looking ahead places the next block out
in front of you. An outline shows where it will land before you click.

It only ever fires when vanilla would do nothing at all - the crosshair has to be on empty air, the
destination has to be replaceable, and there has to be a real block to place against. Aim at
anything and the reacharound is not consulted.

`vertical` does the same thing overhead: look well up and it extends the ceiling above you instead
of the floor below. Off by default, since it is the more surprising of the two.

Standing on a top slab or an upside-down stair, the next one lands in the top half too, so a
bridge of slabs stays level instead of dropping half a block a step. When the block would land
but the server would refuse it - a mob standing in the space, a torch with no wall - the outline
turns `blockedColor` and the click does nothing, rather than the outline vanishing and leaving
you to wonder.

There is a key to switch the whole thing off and on, unbound until you bind it, for the build
where every click near an edge is a block you did not mean to place. It flips `enabled` in the
file and says which way it went over the hotbar.

## The crosshair

The crosshair says what a click would do. Nothing under it and nothing useful in hand, and it is
gone; look at a block and it is back; look at something you could use - a door, a chest, a
villager, a cow with wheat in your hand - and round brackets close around it; hold the right
tool for the block you are looking at and a dot sits in the middle.

What you hold changes its shape. A bow or crossbow draws a circle, a snowball a larger one, a
shield brackets, a block a square, and each of those only when it means something: a block shows
its square where it would actually go, a snowball its circle only with something to throw at.
When the click would be a reacharound, the crosshair says so instead: a line under it for a block
going at your feet, a caret for one going over your head.
Every state has its own shape and every shape can be changed: eighteen to choose from, and
`cross` is vanilla's own so the default look is exactly vanilla's.

The attack indicator stays where vanilla puts it, under the same option. Spectators and the debug
crosshair get vanilla's element untouched.

Pandorical's synced blocks tell the client whether the server answers their click, and the
crosshair believes them - which is the one thing a client can know exactly about a block it has
never seen the inside of.

## Selection box

Colour, opacity and line thickness for the block outline, or no outline at all. A blink, if you
want one: the opacity swings by `blinkAlpha` `blinkSpeed` times a second. And a break animation,
`breakAnimation`: as the block under the box is mined, the box can `shrink` to its centre, sink
`down` into the ground, or fade with `alpha`; `none` is vanilla's fixed box. Drawing the box
through walls is not offered: this version keeps the line pipeline that would need behind private
doors.

Defaults reproduce vanilla exactly, so installing the mod does not change how anything looks until
you ask it to. High contrast block outline, if you have it switched on, always wins - that is an
accessibility setting and a cosmetic preference does not get to overrule it.

## Config

Every setting but the colours is also in Pandorical's mod menu, under Crosshair Corsair, where
Pandorical is installed: a change there is written to the file, and a change to the file shows up
there. The colours are hex strings, which the menu has no control for.

`config/crosshair-corsair.json`, written out in full on first run. It is re-read about once a
second, so you can leave the game running while you tune a colour.

```json
{
  "selectionBox": {
    "enabled": true,
    "color": "#000000",
    "alpha": 102,
    "lineWidth": 0.0,
    "blinkAlpha": 0,
    "blinkSpeed": 1.0,
    "breakAnimation": "none"
  },
  "reacharound": {
    "enabled": true,
    "horizontal": true,
    "vertical": false,
    "showGhost": true,
    "ghostColor": "#FFFFFF",
    "ghostAlpha": 120,
    "blockedColor": "#FF5555"
  },
  "crosshair": {
    "enabled": true,
    "hideWhenIdle": true,
    "thirdPerson": false,
    "onBlock": true,
    "onInteractableBlock": true,
    "onEntity": true,
    "holdingTool": "always",
    "correctToolDot": true,
    "holdingMeleeWeapon": true,
    "meleeOnlyOnEntity": false,
    "holdingRangedWeapon": "always",
    "holdingThrowable": "interactable",
    "holdingShield": true,
    "holdingBlock": "interactable",
    "holdingBlockInOffhand": true,
    "holdingUsableItem": "interactable",
    "usableBrackets": true,
    "overrideColor": false,
    "color": "#FFFFFF",
    "blend": true,
    "styles": {
      "regular": "cross",
      "onBlock": "cross",
      "onInteractableBlock": "cross",
      "onEntity": "cross_open",
      "holdingTool": "cross",
      "holdingMeleeWeapon": "cross",
      "holdingRangedWeapon": "circle",
      "holdingThrowable": "circle_large",
      "holdingShield": "brackets",
      "holdingBlock": "square",
      "holdingUsableItem": "cross",
      "reacharoundFloor": "line_bottom",
      "reacharoundCeiling": "caret"
    }
  }
}
```

The held-item policies are words: `always`, `targeting` (something under the crosshair),
`interactable` (the item would do something to what is there), and for blocks `never`. Styles
are `cross`, `cross_open`, `cross_open_diagonal`, `cross_diagonal_small`, `circle`,
`circle_large`, `square`, `square_large`, `diamond`, `diamond_large`, `caret`, `dot`,
`brackets`, `brackets_top`, `brackets_bottom`, `brackets_round`, `lines`, `line_bottom`. With
`blend` on the crosshair inverts what is behind it, as vanilla's does; `overrideColor` paints it
`color` instead.

`lineWidth: 0` means "whatever the game would have used", which is not a constant - vanilla scales
it with the window so the outline keeps the same apparent weight at any resolution. Naming a number
opts out of that scaling.

Delete the file to get the defaults back.

## Development

Installing is in [DEVELOPMENT.md](DEVELOPMENT.md).

## License

MIT, see [LICENSE](LICENSE).
