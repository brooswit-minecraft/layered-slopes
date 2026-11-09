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

`LayerMathTest` covers the pure layer arithmetic. Block placement/
stacking/shape/drops/pathfinding behavior matches Dynamic Terrain's
pre-split implementation unchanged (same classes, moved verbatim aside
from the package and import rename) — distinguishing automated coverage
from actual in-game playtest: **in-game playtest of this extraction has
not been performed**; that remains a follow-up, same as for Dynamic
Terrain's adapted side (MINECRAFT-499).

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
