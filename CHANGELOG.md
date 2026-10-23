# Changelog

## Unreleased

### Added

- NEI integration now registers the Molecular Transformer recipe/usage handler, tab information,
  catalyst icon, overlay identifier, and localized labels through NEI's API. Stale `advsolar.*`
  entries in external NEI CSVs are ignored instead of requiring a manual CSV edit.
- Quantum Generator packet-size controls in the GUI (`-100`/`+100`, Shift for larger steps), plus a
  QGen item tooltip showing configured production and packet-size defaults.
- Molecular Transformer item tooltip.
- `IMTRecipeManager` now exposes `removeMTRecipe`, `clearMTRecipes`, and a read-only
  `getMTRecipes()` view. New methods have no-op/empty defaults for source and binary compatibility.
- Typed, validated `ASPConfig` model with section comments and detection/logging of obsolete
  `*GenNight` keys.
- Tests for MT API mutation, MT recipe parser edge cases, MT state/description packets, QGen
  clamps/NBT/offered energy, container slot ranges and transfers, packet NBT/codec round-trips,
  item-stack/slot helpers, numeric formatting, and QGen tooltips.

### Changed

- Recipe registration moved from post-init to `FMLLoadCompleteEvent` so cross-mod items and
  ore-dictionary entries are available.
- Hybrid/Ultimate upgrade recipes are only registered when all panels they depend on are enabled.
- GUI button packets now validate the sender, dimension, and a 64-block interaction distance before
  touching a tile entity; packet handling remains scheduled on the game thread.
- MT recipe index invalidation now happens on every recipe mutation.
- Molecular Transformer NEI labels use the existing localization keys.
- Renderer core color is cached instead of allocating a `Color` every frame.

### Removed

- `NAMESPACE_CHANGE.md` (the migration is complete; compatibility status is summarized in
  `README.md` and `CODEBASE_ANALYSIS.md`).
- Unused `GuiHandler`, `RecipeRecord`, dead tile-entity fields/methods, the unused renderer
  `rotateBlock` path, `MTRecipeConfig.doDebug`, and `BlockMolecularTransformer.getBlockEntity()`.

### Compatibility

- `modid` remains `AdvancedSolarPanel`; registry names and tile-entity IDs are unchanged.
- Old `advsolar.*` internals are not provided as shims. Builds that target those internals (for
  example GT5-Unofficial/NewHorizonsCoreMod) are outside this fork's supported compatibility scope.
- Version metadata remains `Tags.VERSION`, as requested.

### Validation

- `./gradlew test --offline`: **76 tests, 0 failures**.
- `./gradlew build --offline` and Spotless pass.
- In-game validation still covers rendering, NEI layout, multiplayer helmet state, and energy-net
  behavior.
