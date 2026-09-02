# Crosshair Corsair

Opinionated tweaks to whatever your crosshair is pointing at - and to what happens when it is
pointing at nothing.

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

## Selection box

Colour, opacity and line thickness for the block outline, or no outline at all.

Defaults reproduce vanilla exactly, so installing the mod does not change how anything looks until
you ask it to. High contrast block outline, if you have it switched on, always wins - that is an
accessibility setting and a cosmetic preference does not get to overrule it.

## Config

`config/crosshair-corsair.json`, written out in full on first run. It is re-read about once a
second, so you can leave the game running while you tune a colour.

```json
{
  "selectionBox": {
    "enabled": true,
    "color": "#000000",
    "alpha": 102,
    "lineWidth": 0.0
  },
  "reacharound": {
    "horizontal": true,
    "vertical": false,
    "showGhost": true,
    "ghostColor": "#FFFFFF",
    "ghostAlpha": 120
  }
}
```

`lineWidth: 0` means "whatever the game would have used", which is not a constant - vanilla scales
it with the window so the outline keeps the same apparent weight at any resolution. Naming a number
opts out of that scaling.

Delete the file to get the defaults back.

## Installation

Install client-side only. Nothing goes on the server, and a server never knows it is there. Version
targets live in `gradle.properties` (Minecraft, loader, Fabric API) and `fabric.mod.json` (Java).

## License

MIT, see [LICENSE](LICENSE).
