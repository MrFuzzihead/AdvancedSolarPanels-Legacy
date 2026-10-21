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

| Panel | Metadata | Day output | Night/rain output | Internal storage | Max output |
| --- | ---: | ---: | ---: | ---: | ---: |
| Advanced Solar Panel | 0 | 8 EU/t | 1 EU/t | 32,000 EU | 32 EU/t |
| Hybrid Solar Panel | 1 | 64 EU/t | 8 EU/t | 100,000 EU | 128 EU/t |
| Ultimate Hybrid Solar Panel | 2 | 512 EU/t | 64 EU/t | 1,000,000 EU | 512 EU/t |
| Quantum Solar Panel | 3 | 4096 EU/t | 2048 EU/t | 10,000,000 EU | 8192 EU/t |

Each panel has four charge slots and can charge electric items while it generates. The slots accept only
IC2 electric items (`IElectricItem`) that can actually take a charge; full or stacked electric items are rejected, matching IC2's charge-slot behavior. The panels use ASP's classic rules: day/night/rain output is flat, and a panel only stops when it cannot see the sky.
The current implementation does **not** use IC2's sky-light scaling; see
[`CODEBASE_ANALYSIS.md`](CODEBASE_ANALYSIS.md) for the parity comparison and the planned
`useIC2SolarRules` option.

All values are configurable in `config/AdvancedSolarPanel.cfg` under `general`:
`AdvancedSPGenDay`, `AdvancedSPGenNight`, `AdvancedSPStorage`, `AdvancedSPOutput`, and the
corresponding `HybrydSP*`, `UltimateHSP*`, and `QuantumSP*` keys.

### Wrench behavior

Panels follow IC2 machine drops: breaking one with a pickaxe (or any non-wrench tool) drops a
**Basic Machine Casing**, while an IC2 wrench has a **90%** chance to preserve the panel. IC2's
electric wrench lossless mode forces a successful wrench drop. Charge-slot contents are dropped
separately. Use a wrench if you want the panel itself back.

## Solar helmets

| Helmet | Day/night generation | Max charge | Transfer limit | Tier | Air supply |
| --- | ---: | ---: | ---: | ---: | --- |
| Advanced Solar Helmet | 8 / 1 EU/t | 1,000,000 EU | 3,000 EU/t | 3 | No |
| Hybrid Solar Helmet | 64 / 8 EU/t | 10,000,000 EU | 10,000 EU/t | 4 | Yes |
| Ultimate Solar Helmet | 512 / 64 EU/t | 10,000,000 EU | 10,000 EU/t | 4 | Yes |

The helmets charge electric items in the player's armor and main inventory while the player is in
sunlight. The Hybrid and Ultimate helmets also consume 1,000 EU to replenish air when the player is
underwater. Helmet recipes use the matching solar panel and the IC2 nano/quantum helmet.

## Molecular Transformer

The Molecular Transformer converts items using EU. Recipes are stored in
`config/AdvancedSolarPanel_MTRecipes.cfg`; the file format is documented at the top of the generated
file:

```
inputItem:stackSize; outputItem:stackSize; energy
```

Inputs and outputs can be item ids (`minecraft:item_name-meta` or `modID:item_name-meta`) or ore
dictionary entries (`oredict:name`). A `#` starts a comment. Stack sizes are clamped to 1–64 and
negative energy is rejected. The machine has one input slot, one output slot, a progress display, and
a NEI recipe page.

## Quantum Generator

The Quantum Generator is an admin/map-maker block with **no crafting recipe**. It produces
configurable EU/t and is disabled by a redstone signal. Its defaults are set in
`config/AdvancedSolarPanel.cfg` under `quantum generator`:
`quantumGeneratorDefaultProduction` and `quantumGeneratorDefaultPacketSize`.

## Recipes

- The default recipe mode is the “hard” recipe set; enable `Enable hard recipes = false` for the
  simpler set.
- The Advanced Solar Panel uses an IC2 solar panel, advanced circuits/alloy, and the tier materials.
- Hybrid and Ultimate panels are upgraded from the previous tier.
- The Quantum Solar Panel requires the Quantum Core.
- Helmet recipes use the matching panel plus the IC2 nano/quantum helmet.
- Individual recipes can be disabled under `recipes settings`.
- OreDictionary names are used where possible (`ingotUranium`, `ingotIridium`, etc.).

## Configuration

`config/AdvancedSolarPanel.cfg` includes:

| Section | Examples | Purpose |
| --- | --- | --- |
| `general` | `AdvancedSPGenDay`, `HybrydSPOutput`, `QuantumSPStorage` | Per-tier generation, storage, and output |
| `recipes settings` | `Disable HybridSolarPanel recipe`, `Enable hard recipes` | Recipe toggles |
| `quantum generator` | `quantumGeneratorDefaultProduction`, `quantumGeneratorDefaultPacketSize` | QGen defaults |

Negative generation values are clamped to 0; storage and output values are clamped to at least 1.

## Building and testing

This project uses the GTNewHorizons 1.7.10 Gradle toolchain. The correct JDK is provisioned by
Gradle.

```bash
./gradlew build        # Windows: gradlew.bat build
./gradlew test         # unit tests
./gradlew runClient    # development client
./gradlew runServer    # development server
```

Built jars are placed in `build/libs/`. The current test suite covers solar generation math, MT recipe
validation, MT energy-buffer behavior, and namespace/lang resource invariants.

## Compatibility notes

- The mod id is `AdvancedSolarPanel` and existing registry names/worlds must not be renamed.
- The Molecular Transformer recipe file and NEI integration are documented in
  [`CODEBASE_ANALYSIS.md`](CODEBASE_ANALYSIS.md).
- GT5-Unofficial and NewHorizonsCoreMod reference the old `advsolar.*` internal classes; the
  compatibility impact of the package rename is covered in `CODEBASE_ANALYSIS.md` §6.

## Credits

- Original Advanced Solar Panels by SeNtiMeL, based on the original mod by Icedfire
- Textures by Krezzard
- 1.7.10 maintenance and port by MrFuzzihead and contributors
- Thanks to mak326428 for development help
