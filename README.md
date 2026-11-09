# layered-slopes

Layered floor/ceiling blocks extracted from Dynamic Terrain for NeoForge 1.21.1.

NeoForge 1.21.1, Java 21. Mod ID: `layeredslopes`. MIT licensed.

**Current state: scaffold only.** The mod registers and loads; add gameplay
logic in `src/main/java/io/github/brooswitminecraft/layeredslopes/LayeredSlopesMod.java`
and beyond.

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
