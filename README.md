# Tornado Mod (Forge, Minecraft 1.20.1)

Adds tornadoes rated **EF0 to EF5** that wander across the land, drag entities into a spiral, lift them,
tear blocks out of the world and fling them as debris. (Forge port of the Fabric 1.21.1 version.)

## Features

- **Tornado entity** with six strengths. Stronger = wider, taller, faster, longer-lived and able to tear harder blocks:
  EF0 rips leaves/glass/plants, EF2 takes wood and stone bricks, EF4+ scours the ground itself, EF5 can remove obsidian.
- **Wind and lift:** entities near the funnel are spun inward and carried upward. EF2+ also hurt anything near the core.
  Creative and spectator players are ignored.
- **Tornado Wand:** right-click to summon one where you look, sneak + right-click to change the strength.
  Craft it with: feather (top), lightning rod (middle), stick (bottom).
- **Natural spawns:** during Overworld thunderstorms a tornado can form 50-90 blocks from a random player.
  Players within 100 blocks get a red action-bar warning.
- **Commands** (op level 2):
  - `/tornado spawn [rating 0-5] [x y z]` (random rating if omitted)
  - `/tornado clear` removes every tornado
  - `/tornado storm` starts a thunderstorm
- **Game rules:** `tornadoSpawns` (natural formation) and `tornadoDestruction` (block damage), both default `true`.

## Easiest: free cloud build

No installs needed. See `GET-THE-JAR-FREE.md`: upload the project to a free GitHub account and it builds the jar for you.

## Building on your own computer

You need **JDK 17**.

1. This project has no `gradle-wrapper.jar` (it can't be generated offline). Either:
   - open the folder in IntelliJ IDEA or Eclipse (they import Gradle projects automatically), or
   - install Gradle 8.8 and run `gradle wrapper --gradle-version 8.8` once in this folder.
2. Run `./gradlew build` (Windows: `gradlew.bat build`). The jar appears in `build/libs/tornadomod-1.0.0.jar`.
3. Test in a dev client with `./gradlew runClient`.
4. To play: drop the jar into the `mods` folder of a **Forge 47.x** install of Minecraft 1.20.1. No other mods needed.

Quick test in a creative world: `/tornado spawn 3`, then watch from a distance.

## Tuning

Everything is in `TornadoEntity.java`: `MAX_HARDNESS` (what each rating can destroy), `baseRadius()`,
`funnelHeight()`, the wind speeds in `pullEntities()`, and the lifetime in `serverTick()`.
Spawn frequency and distance are in `TornadoSpawner.java`.

## Notes

- The funnel is drawn with particles, not a 3D model.
- The wand texture is a generated placeholder. Replace `assets/tornadomod/textures/item/tornado_wand.png` (16x16) with your own.
- Uses official Mojang mappings. If a Forge version bump breaks compilation, paste the error and it can be fixed quickly.
