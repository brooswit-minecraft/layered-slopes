# layered-slopes

Layered floor/ceiling blocks extracted from Dynamic Terrain for NeoForge 1.21.1.

NeoForge 1.21.1, Java 21. Mod ID: `layeredslopes`. MIT licensed.

Ships the layered floor/ceiling block family, extracted from
[Dynamic Terrain](https://github.com/brooswit-minecraft/dynamic-terrain)
(MINECRAFT-499): `LayeredBlock`/`LayeredGrassBlock` (43 vanilla materials
plus layered grass), `LayerMath` (pure 1/16-layer arithmetic) and `Anchor`
(floor/ceiling). Covers normal block placement/stacking, block states,
shapes/collision, drops, client rendering and grass/snow/pathfinding
behavior. Grading tools, manual smoothing/terrain editing, world
generation, erosion/cave-ins and driving-surface physics are **not** part
of this mod — they stay in Dynamic Terrain, which depends on this mod's
block API instead of owning its own copy. A planned Smooth Slopes mod
(MINECRAFT-500) will extract world-generation smoothing the same way,
depending on this mod in turn.

## Install / upgrade pairing

Runs fully standalone: nothing here requires Dynamic Terrain or any
vehicle mod — it is the base layer other mods (Dynamic Terrain, the
planned Smooth Slopes) depend on, never the other way around.

**Compatibility contract — why existing worlds keep working:** every
layered block and item here is registered under the **pre-split
`dynamicterrain` registry namespace**, not `layeredslopes` (see
`LayeredSlopesMod.LEGACY_NAMESPACE`). A world that placed
`dynamicterrain:layered_dirt`, `dynamicterrain:layered_grass`, etc. before
this split keeps resolving every one of those blocks/items, with the same
`layers`/`anchor` (and `snowy` for grass) block-state properties, once
Dynamic Terrain is upgraded past this split **with Layered Slopes
installed alongside it** — this mod, not Dynamic Terrain, is now the one
that actually registers them. The same applies to data packs and tags
that reference those ids (e.g. the `minecraft:mineable/pickaxe` and
`minecraft:mineable/shovel` tag entries, and the slab<->layered recipes
under `data/dynamicterrain/recipe/`, all moved here unchanged). Upgrading
Dynamic Terrain past this split *without* installing Layered Slopes is not
a supported pairing: every `dynamicterrain:layered_*` block simply doesn't
come back, the same as removing any other mod that owns content in a
world — this mod declares no retroactive data migration because none is
needed; the registry name, property set and asset paths are byte-for-byte
unchanged from Dynamic Terrain's pre-split copy (verified against blob
`053f2699b6953378bf7e647ce424649eccf52e9c`, the pre-split
`LayeredBlock.java`).

## Test coverage — what's covered, what isn't

Headless (no game bootstrap): `LayerMathTest` covers the layer arithmetic
underlying shape/collision bounds and stacking limits. (`Anchor`/
`LayeredBlock`/`LayeredGrassBlock`/`ClientEvents` depend on Minecraft
classes this project's plain `test` sourceSet has no classpath access to
— confirmed live, not assumed — so they aren't reachable from there.)

Real runtime evidence: `LayeredBlockGameTests` (`ci.yml`'s "Run GameTests"
step, `./gradlew runGameTestServer` against a real NeoForge server with
only this mod installed — `scripts/check_gametest_count.py` fails closed
unless every named test below is confirmed to have actually run and
passed, so a crashed or silently-empty run cannot read as success):
- `placingAndStackingLayersUpdatesTheLayersProperty` — layers grow one at
  a time from 1 to 16 on a real registered block in a live level, a full
  block rejects further layers, and the collision shape reaches the top
  of the cell at 16 layers.
- `breakingALayeredBlockDropsOneItemPerLayer` — destroying a 5-layer
  block drops exactly 5 items.
- `legacyDynamicterrainRegistryNameStillResolves` — `dynamicterrain:layered_dirt`
  resolves, at runtime, to this mod's own registered `LayeredBlock` — the
  actual compatibility contract, not just the registration code reading
  that way.

The GameTest server actually booting with this mod installed is itself
standalone-load and classloading evidence. Dynamic Terrain's paired CI
build (see its `ci.yml`) compiles and runs its own tests against this
exact PR's build output — the automated evidence that the paired
adaptation compiles against this extraction's real classes, which is how
the stale `ClientEvents`/`LAYERED_GRASS` and
`LAYERED_SNOW_BLOCK` references this review flagged were actually caught.

**Still not covered, and not manually playtested in this PR:** render
(biome tint pixel output — needs a real client, not just the
`RegisterColorHandlersEvent` registration this mod wires up), mob
pathfinding over a layered slope, Dynamic Terrain *coinstalled* at
runtime (its own PR is where that's exercised, since it's the consumer),
and a representative legacy-world/save file loaded end-to-end rather than
the direct registry-resolution proof above. These remain manual/follow-up
per review guidance to keep this evidence bounded rather than building a
general test platform.

## Building

Requires a JDK 21 with `javac` on `JAVA_HOME`.

```sh
./gradlew build
```

## Releasing

Every PR that touches `src/` must add exactly one fragment under
`changelog.d/` (see `changelog.d/README.md`). Pushing to `main` with new
fragments tags a GitHub release automatically; nothing is committed back by
hand.
