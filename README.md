# AdvancedSolarPanels-Legacy

Advanced Solar Panels for **Minecraft 1.7.10 (Forge)**. This is an add-on for
[IndustrialCraft²](https://www.curseforge.com/minecraft/mc-mods/industrial-craft) that adds
upgraded solar panels, solar helmets, a Molecular Transformer, and an admin-only Quantum Generator.

## Requirements

| Dependency | Version |
| --- | --- |
| Minecraft | 1.7.10 |
| Minecraft Forge | 10.13.4.1614 (or compatible 1.7.10 Forge) |
| IndustrialCraft² | 2.2.828-experimental or newer |

## Solar panels

| Panel | Metadata | Tier | Day output | Dusk/dawn | Night/rain | Internal storage | Max output |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Advanced Solar Panel | 0 | 1 | 8 EU/t | 2 EU/t | 1 EU/t | 32,000 EU | 32 EU/t |
| Hybrid Solar Panel | 1 | 2 | 64 EU/t | 16 EU/t | 8 EU/t | 100,000 EU | 128 EU/t |
| Ultimate Hybrid Solar Panel | 2 | 3 | 512 EU/t | 128 EU/t | 64 EU/t | 1,000,000 EU | 512 EU/t |
| Quantum Solar Panel | 3 | 5 | 4096 EU/t | 1024 EU/t | 512 EU/t | 10,000,000 EU | 8192 EU/t |

Each panel has four charge slots and can charge electric items while it generates. The slots accept only
IC2 electric items (`IElectricItem`) that can actually take a charge at the panel's tier; full or stacked electric items are rejected, matching IC2's charge-slot behavior.

Panels use IC2's sky-light classification with graceful ASP output factors. The per-tier `*GenDay`
config value is the 100% rate; the other outputs scale from it. Fractional EU is accumulated between
ticks.

| Condition | Output |
| --- | ---: |
| Clear full day | 100% |
| Clear dawn/dusk | 1/4 |
| Clear night | 1/8 |
| Rain, day | 1/4 |
| Rain, dawn/dusk | 1/8 |
| Rain, night | 0 |
| Thunder, day | 1/8 |
| Thunder, dawn/dusk/night | 0 |
| Desert | Weather is ignored; clear-weather factors apply |
| Blocked sky / no-sky dimension | 0 |

Thunder takes precedence over rain if both are active. The phase is based on the time-of-day sky
light, not the weather-darkened light level.

All rates are configurable in `config/AdvancedSolarPanel.cfg` under `general`:
`AdvancedSPGenDay`, `AdvancedSPStorage`, `AdvancedSPOutput`, and the corresponding
`HybrydSP*`, `UltimateHSP*`, and `QuantumSP*` keys. `*GenDay` is always the 100% clear-day rate;
the obsolete `*GenNight` keys are detected, logged, and ignored.

### Wrench behavior

Panels follow IC2 machine drops: breaking one with a pickaxe (or any non-wrench tool) drops a
**Basic Machine Casing**, while an IC2 wrench has a **90%** chance to preserve the panel. IC2's
electric wrench lossless mode forces a successful wrench drop. Charge-slot contents are dropped
separately. Use a wrench if you want the panel itself back.

## Solar helmets

| Helmet | Sunlight charge | Max charge | Helmet transfer limit | Tier | Air supply |
| --- | ---: | ---: | ---: | ---: | --- |
| Advanced Solar Helmet | 8 EU/t | 1,000,000 EU | 3,000 EU/t | 1 | No |
| Hybrid Solar Helmet | 64 EU/t | 10,000,000 EU | 10,000 EU/t | 2 | Yes |
| Ultimate Solar Helmet | 512 EU/t | 10,000,000 EU | 10,000 EU/t | 3 | Yes |

By default, the helmets follow IC2's solar-helmet targeting: charge the **chestplate first**, accept
**any tier**, ignore the chestplate's transfer limit, and send any overflow to the helmet itself.
They sync the player inventory after charging. Setting `general.solarHelmetLegacyCharging=true`
restores the original ASP behavior: charge all armor slots (including the helmet) and then the main
inventory, using the original tier/transfer-limit rules.

Helmets use the same weather/light factor table as the panels.

Deserts ignore the weather penalty, matching IC2. The Hybrid and Ultimate helmets also consume 1,000 EU
from their own internal buffer to replenish underwater air. Helmet tiers match their corresponding
panel tiers, and the tooltip shows the charge rate, tier, and description. Helmet recipes use the
matching solar panel and the IC2 nano/quantum helmet.

## Molecular Transformer

The Molecular Transformer converts items using EU. Recipes are stored in
`config/AdvancedSolarPanel_MTRecipes.cfg`; the file format is documented at the top of the generated
file:

```
inputItem:stackSize; outputItem:stackSize; energy
```

Inputs and outputs can be item ids (`minecraft:item_name-meta` or `modID:item_name-meta`) or ore
dictionary entries (`oredict:name`). Ore-dictionary inputs are matched dynamically, so entries added
by other mods later are accepted. A `#` starts a comment. Stack sizes are clamped to 1–64 and negative
energy is rejected. If an ore-dictionary output has several entries, the first registered entry is
used as the displayed/output stack.

The machine has one input slot, one output slot, a progress display, a tooltip, and a NEI recipe page.
The NEI page is registered through NEI's API: tab icon, catalyst, overlay identifier, localized
labels, and recipe/usage lookup are provided by the mod. Existing NEI `handlers.csv`/`catalysts.csv`
entries that still use the old `advsolar.*` class name are ignored; no CSV edit is required.

## Quantum Generator

The Quantum Generator is an admin/map-maker block with **no crafting recipe**. It produces
configurable EU/t and is disabled by a redstone signal. Its defaults are set in
`config/AdvancedSolarPanel.cfg` under `quantum generator`:
`quantumGeneratorDefaultProduction` and `quantumGeneratorDefaultPacketSize`. The packet size is a
per-tick cap, so the offered output is `min(production, maxPacketSize)`.

The GUI has production and packet-size buttons (use Shift for ×10), shows the current values, and
the item tooltip lists the configured defaults. Button changes are validated on the server, including
the sender's dimension and a 64-block interaction distance.

## Recipes

- The default recipe mode is the “hard” recipe set; enable `Enable hard recipes = false` for the
  simpler set.
- The Advanced Solar Panel uses an IC2 solar panel, advanced circuits/alloy, and the tier materials.
- Hybrid and Ultimate panels are upgraded from the previous tier.
- The Quantum Solar Panel requires the Quantum Core.
- Helmet recipes use the matching panel plus the IC2 nano/quantum helmet.
- Individual recipes can be disabled under `recipes settings`. Upgrade recipes are only registered
  when every panel they depend on is enabled.
- OreDictionary names are used where possible (`ingotUranium`, `ingotIridium`, etc.). IC2 item
  lookups that are optional (compressor inputs and the MT Core reflector) are guarded, so a missing
  item skips only that recipe instead of creating an invalid recipe.
- Recipes are registered on `FMLLoadCompleteEvent`, after other mods have finished registering their
  items and ore-dictionary entries.

## Molecular Transformer API

`com.mrfuzzihead.advancedsolarpanels.api.MTAPI.manager` is initialized during pre-init, so other mods
can add recipes during post-init. `IMTRecipeManager` exposes `addMTRecipe`, `removeMTRecipe`,
`clearMTRecipes`, and a read-only `getMTRecipes()` view. The new methods have no-op/empty defaults,
so existing third-party implementations remain source- and binary-compatible.

## Configuration

`config/AdvancedSolarPanel.cfg` is loaded into a validated, typed model and saved with section
comments. It includes:

| Section | Examples | Purpose |
| --- | --- | --- |
| `general` | `AdvancedSPGenDay`, `HybrydSPOutput`, `QuantumSPStorage`, `solarHelmetLegacyCharging` | Per-tier output/storage and helmet charging mode |
| `recipes settings` | `Disable HybridSolarPanel recipe`, `Enable hard recipes` | Recipe toggles |
| `quantum generator` | `quantumGeneratorDefaultProduction`, `quantumGeneratorDefaultPacketSize` | QGen defaults |

Negative generation values are clamped to 0; storage, output, and QGen values are clamped to at
least 1. The old `*GenNight` keys are not migrated because generation is now derived from the
100% day rate.

## Building and testing

This project uses the GTNewHorizons 1.7.10 Gradle toolchain. The correct JDK is provisioned by
Gradle.

```bash
./gradlew build        # Windows: gradlew.bat build
./gradlew test         # unit tests
./gradlew runClient    # development client
./gradlew runServer    # development server
```

Built jars are placed in `build/libs/`. The current test suite has 76 tests covering solar generation
math, helmet tiers/charge rules/tooltips, MT recipe validation/parser/API/NBT state, MT
energy-buffer behavior, QGen clamping/NBT/offered energy, container slot ranges and transfers,
packet NBT/codec round-trips, item-stack/slot helpers, and namespace/lang resource invariants. Rendering, NEI layout, multiplayer helmet state, and actual energy-net
behavior still need in-game/manual validation.

## Compatibility notes

- The mod id is `AdvancedSolarPanel` and existing registry names/worlds must not be renamed.
- The source package is `com.mrfuzzihead.advancedsolarpanels`; the old `advsolar.*` internals are not
  provided as compatibility shims. GT5-Unofficial/NewHorizonsCoreMod builds that target those internals
  are outside this fork's supported compatibility scope.
- `NAMESPACE_CHANGE.md` has been removed; its completed migration notes are summarized in
  [`CHANGELOG.md`](CHANGELOG.md) and [`CODEBASE_ANALYSIS.md`](CODEBASE_ANALYSIS.md).

## Credits

- Original Advanced Solar Panels by SeNtiMeL, based on the original mod by Icedfire
- Textures by Krezzard
- 1.7.10 maintenance and port by MrFuzzihead and contributors
- Thanks to mak326428 for development help
