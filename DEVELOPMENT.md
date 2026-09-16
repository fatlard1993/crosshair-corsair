# Crosshair Corsair - Development Guide

For what the mod is and how it plays, see [README.md](README.md).

## Installation

Install client-side only. Nothing goes on the server, and a server never knows it is there.
Version targets live in `gradle.properties` (Minecraft, loader, Fabric API) and `fabric.mod.json`
(Java).

## Building

Pandorical is **optional at runtime and required at build time**, and the difference matters.
`PandoricalSettings` and `PandoricalBlocks` name Pandorical types directly, so javac needs its jar;
at runtime both sit behind `FabricLoader.isModLoaded` and the classes that touch it are never
loaded without it. That is why `fabric.mod.json` only suggests it and the mod genuinely runs
standalone.

So the repo does not build on its own:

```sh
git clone https://github.com/fatlard1993/pandorical ../pandorical
mc-build pandorical      # or: ./gradlew build -p ../pandorical
./gradlew build
```

The version is read from what Pandorical declares, never pinned here: Gradle never clears
`build/libs`, so after a version bump the old and new jars sit side by side and a hand-written name
keeps compiling green against a jar an old build left behind. The rule lives once, in
`../pandorical/gradle/dependent.gradle`, and every dependent in the suite applies it.

`mc-build pandorical` rebuilds this mod along with Pandorical's other dependents. It finds them by
reading each repo's `src/*/resources/fabric.mod.json` - `src/client` for a client-only mod like
this one, not just `src/main`.

## The sprites

`generate_crosshairs.py` draws the seventeen shapes into
`src/client/resources/assets/.../textures/gui/sprites/crosshair/`. Run it after changing one.

It draws seventeen and not eighteen because `CrosshairStyle.CROSS` points at vanilla's own sprite,
so a cross drawn here would ship a file nothing can load. The script checks itself against that
enum and fails if the two drift apart in either direction, and writes
`build/crosshair-contact-sheet.png` so the shapes can be looked at rather than only counted.

```sh
python3 generate_crosshairs.py
```

## What a green build does not tell you

There are no automated tests, and a green build means it compiles. The mixins bind at runtime
against exact target names (`Minecraft.startUseItem`, `MultiPlayerGameMode.destroyProgress` and
`destroyBlockPos`), and `"required": true` with `defaultRequire: 1` makes a stale target a crash at
launch rather than a quiet degrade. Only starting the game surfaces that.

`./gradlew runClient` in a window that is not minimized. Reaching the title screen already proves
every mixin target, the version predicate, and that all seventeen sprites load. The rest wants a
world: hold a block at the edge of a drop, look ahead, and check the ghost lands where the block
does.
