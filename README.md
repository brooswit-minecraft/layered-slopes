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

Automated, headless (no game/mod-loader bootstrap) coverage: `LayerMathTest`,
the layer arithmetic underlying shape/collision bounds and stacking
limits. It's the only class in this family free of Minecraft types by
design; `Anchor`/`LayeredBlock`/`LayeredGrassBlock`/`ClientEvents` all
depend on Minecraft classes (`StringRepresentable`, `Block`,
`RegisterColorHandlersEvent`, ...) that this project's plain `test`
sourceSet has no classpath access to (confirmed: adding a test that merely
references `Anchor` fails `compileTestJava` with "cannot access
StringRepresentable") — reaching them needs a running game/mod-loader
bootstrap this PR does not attempt, per review guidance not to invent a
large test platform here. Dynamic Terrain's paired CI build (see its
`ci.yml`) compiles and runs its own tests against this exact PR's build
output, which is the automated evidence that the paired adaptation
actually compiles against this extraction's real classes (not just the
unrelated `v0.1.0` scaffold) — including catching the stale
`ClientEvents`/`LAYERED_GRASS` reference this review flagged.

**Explicitly not covered by automation, and not manually playtested in
this PR** (per MINECRAFT-499's acceptance list): block
placement/stacking/drops/render/collision/pathfinding behavior in a
running game, standalone load, coinstalled load with Dynamic Terrain,
actual client/server mod-loader classloading, and legacy-world/save
upgrade. This mod's `LayeredBlock`/`LayeredGrassBlock`/`LayerMath`/`Anchor`
are moved verbatim from Dynamic Terrain's pre-split implementation (same
classes, package/import rename only), so their in-game behavior is
expected to be unchanged from that pre-split, previously-shipped code —
but that expectation is not itself test evidence. Establishing a
GameTest/runtime harness for this lineage (neither Dynamic Terrain nor
this mod has ever had one) is a bounded follow-up, not attempted in this
PR per review guidance to avoid inventing a large test platform here.

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
