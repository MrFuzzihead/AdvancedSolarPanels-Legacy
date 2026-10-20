# Namespace Rename Impact Analysis

**Goal:** move the package namespace from `advsolar` to `com.mrfuzzihead.advancedsolarpanels`
**Target pack:** `C:\Users\Duncan\AppData\Roaming\PrismLauncher\instances\FuzzCraft-Legacy\minecraft\mods`
**Date analysed:** production modpack, 241 entries in `mods/` (plus `mods/1.7.10`, `mods/PPP`)

> **Verdict:** renaming *only* the packages (keeping `modid = "AdvancedSolarPanel"`) is safe for worlds,
> configs and other mods' recipes. Three external references exist, only **one** of which is a real
> behavioural dependency (HodgePodge), and that one degrades to a non-fatal warning if the bug is fixed
> in ASP itself.

---

## 1. Scan methodology

Every `.jar`/`.zip` under `mods/` (recursively) was opened with `zipfile`; all `.class` constant pools
were decompressed and searched case-insensitively for `advsolar`, and all non-asset resource entries
(`.json`, `.cfg`, `.csv`, `.txt`, `.info`, `.xml`, `.lang`, `.properties`, `.MF`, `.zs`, …) were
searched as raw bytes. Additionally, the whole instance directory (`config/`, `scripts/`, `journeymap/`,
`dynmap/`, …) was grepped. False positives were inspected at bytecode level.

Missing entry types were then checked separately: nested jars, `coremods/`, `nestedmods/` (both empty).

---

## 2. Results — real references to `advsolar`

| Jar | Kind | Reference | Severity |
|---|---|---|---|
| `hodgepodge-2.7.216.jar` | **Hard** | `com/mitchej123/hodgepodge/mixins/late/advancedsolarpanels/MixinAdvancedSolarPanel` uses `@Mixin(value = advsolar.common.AdvancedSolarPanel.class, remap = false)` and `@Slice(from = @At(value = "FIELD", target = "Ladvsolar/common/AdvancedSolarPanel;disableMolecularTransformerRecipe:Z"))` | **Blocking (see §3) — but only a WARN, not a crash** |
| `NotEnoughItems-2.8.162-GTNH.jar` | Soft | `assets/nei/csv/handlers.csv` + `assets/nei/csv/catalysts.csv` contain `advsolar.client.nei.MTRecipeHandler` | Cosmetic only (see §4) |
| `gtnh-web-map-0.4-beta-1.jar` | None | `renderdata/AdvancedSolarPanel-texture.txt`: `cls=advsolar.common.blocks.BlockAdvSolarPanel` — **inside a `#` comment line** | No impact (see §5) |
| `EnderIO-2.10.49.jar` | **False positive** | `advSolarPanel` is a local variable name in `crazypants/enderio/machine/MachineRecipes.class` (siblings: `solarPanel`, `vibSolarPanel`; the only IC type there is EnderIO's own `BlockSolarPanel`) | None |

**No jar in the pack references `advsolar.api.*`** (`ASPItemAPI`, `MTAPI`, `IMTRecipeManager`), i.e. there
are **no third-party compile-time consumers of the ASP API**. The API package can move freely.

### Non-jar references (all modid-based, not package-based)

| File | Content | Affected by package rename? |
|---|---|---|
| `config/AppliedEnergistics2/Facades.cfg` | `B:BlockAdvSolarPanel=false` … `B:BlockAdvSolarPanel.4=false` | No — registry names |
| `journeymap/colorpalette.json` | `"uid": "AdvancedSolarPanel:BlockAdvSolarPanel"` | No — registry names |
| `AmunRa-GC-0.8.14.jar` | `findItem("AdvancedSolarPanel", "asp_crafting_items", …)` + `after:AdvancedSolarPanel` in `@Mod` deps | No — modid |
| `gravisuiteneo-1.3.16.jar` | shield-helm whitelist `AdvancedSolarPanel:hybrid_solar_helmet`, `AdvancedSolarPanel:ultimate_solar_helmet` | No — modid |

### World / save safety

Tile entities are registered under **literal strings**, not class names (`AdvancedSolarPanel.java:186-193`):

```java
GameRegistry.registerTileEntity(TileEntityMolecularTransformer.class, "Molecular Transformer");
GameRegistry.registerTileEntity(TileEntityAdvancedSolarPanel.class, "Advanced Solar Panel");
GameRegistry.registerTileEntity(TileEntityHybridSolarPanel.class, "Hybrid Solar Panel");
GameRegistry.registerTileEntity(TileEntityUltimateSolarPanel.class, "Ultimate Hybrid Solar Panel");
GameRegistry.registerTileEntity(TileEntityQuantumSolarPanel.class, "Quantum Solar Panel");
GameRegistry.registerTileEntity(TileEntityQGenerator.class, "Quantum Generator");
```

Block/item registry names are `AdvancedSolarPanel:*`. **Existing worlds survive a package-only rename.**
This stops being true if the *modid* is changed.

---

## 3. HodgePodge (`fixMTCoreRecipe`) — will NOT crash, will skip

### How it is registered

`MixinAdvancedSolarPanel` is added dynamically by HodgePodge (not present in `mixins.hodgepodge*.json`):

```
MixinBuilder("MT Core recipe fix")
    .addCommonMixins("advancedsolarpanels.MixinAdvancedSolarPanel")
    .addRequiredMod(TargetedMod.ADVANCED_SOLAR_PANELS)   // enum argument = modid "AdvancedSolarPanel" only
    .addExcludedMod(TargetedMod.DREAMCRAFT)
    .setApplyIf(<FixesConfig.fixMTCoreRecipe>)
    .setPhase(LATE)
```

`TargetedMod.ADVANCED_SOLAR_PANELS` is constructed with a single argument — the modid
`"AdvancedSolarPanel"` (verified in the enum's `<clinit>`). Detection is therefore **modid-based**, not
class-based, so after a package rename the gate still passes and the mixin is still registered.

### What happens when the target class is gone

From `+unimixins-all-1.7.10-0.3.2.jar` → `org/spongepowered/asm/mixin/transformer/MixinInfo.class`:

```
getTargetClass(DeclaredTarget):
    ClassInfo info = ClassInfo.forName("advsolar/common/AdvancedSolarPanel")   // -> null
    if (info == null) {
        if (isVirtual()) logger.debug("Skipping virtual target ...");
        else handleTargetError("@Mixin target %s was not found %s", /*required*/ false);  // iconst_0
        return null;
    }

handleTargetError(String msg, boolean required):
    if (strict) { logger.error(msg); throw new InvalidMixinException(...); }
    logger.log(required ? DEBUG : WARN, msg);   // no throw in production

readTargetClasses(List):  // skips nulls, returns the (possibly empty) list
```

`strict` is only enabled via `-Dmixin.strict=true` (or an environment setting). This instance's JVM args
are `-XX:+UseZGC -XX:+ZGenerational` — strict mode is **off**.

**Consequence:** a single WARN line at startup, e.g.

```
@Mixin target advsolar.common.AdvancedSolarPanel was not found MixinAdvancedSolarPanel
(com.mitchej123.hodgepodge.mixins.late.advancedsolarpanels.MixinAdvancedSolarPanel)
from mixins.hodgepodge.late.json
```

…and the mixin is dropped. The game continues normally.

### What the mixin patches (to port the fix into ASP)

Upstream `MixinAdvancedSolarPanel.java`:

```java
@Mixin(value = AdvancedSolarPanel.class, remap = false)
public class MixinAdvancedSolarPanel {

    @ModifyExpressionValue(
            at = @At(
                    ordinal = 0,
                    target = "Lic2/api/item/IC2Items;getItem(Ljava/lang/String;)Lnet/minecraft/item/ItemStack;",
                    value = "INVOKE"),
            method = "afterModsLoaded",
            slice = @Slice(
                    from = @At(
                            opcode = Opcodes.GETSTATIC,
                            target = "Ladvsolar/common/AdvancedSolarPanel;disableMolecularTransformerRecipe:Z",
                            value = "FIELD")))
    private ItemStack hodgepodge$fixDamage(ItemStack original) {
        // Must be a new ItemStack so the field in Ic2Items is not modified
        return new ItemStack(original.getItem(), 1, 1);
    }
}
```

HodgePodge's config description: *"Fixes the damage of the Thick Neutron Reflectors in the MT Core
recipe (Advanced Solar Panels)"* → `config/hodgepodge.cfg:461-462`, `B:fixMTCoreRecipe=true`.

The targeted call site is `src/main/java/advsolar/common/AdvancedSolarPanel.java:548`:

```java
if (!disableMolecularTransformerRecipe) {
    GameRegistry.addRecipe(itemMTCore, new Object[]{"MXM", "M M", "MXM",
        'M', itemIrradiantGlassPane, 'X', IC2Items.getItem("reactorReflectorThick")});
```

The bug: the shared cached IC2 stack (`Ic2Items.reactorReflectorThick`, built in `ic2/core/init/BlocksItems`
as `new ItemStack(new ItemReactorReflector(reactorReflectorThick, 40000))`) is handed straight to the
recipe and gets mutated. HodgePodge replaces it with a **fresh** stack with damage `1`.

### Recommended ASP-side fix

```java
GameRegistry.addRecipe(itemMTCore, new Object[]{"MXM", "M M", "MXM",
    'M', itemIrradiantGlassPane,
    'X', new ItemStack(IC2Items.getItem("reactorReflectorThick").getItem(), 1, 1)});
```

After that, HodgePodge's mixin is redundant. Options for HodgePodge:

1. **Leave it alone** — the stale mixin logs one WARN and is skipped (current UniMixins behaviour).
2. Set `B:fixMTCoreRecipe=false` in `config/hodgepodge.cfg` — silences the WARN.
3. Patch/PR HodgePodge to update the FQCN (or make the target resolution optional).

Note: a "compat shim" (`advsolar.common.AdvancedSolarPanel extends <new class>`) does **not** work here;
the mixin would apply to the shim, where `afterModsLoaded` / `disableMolecularTransformerRecipe` are
inherited, and the injected fix would not affect the real logic.

---

## 4. NEI (`handlers.csv` / `catalysts.csv`) — what they do, config-only fix

Both files ship inside `NotEnoughItems-2.8.162-GTNH.jar` under `assets/nei/csv/`:

```
handlers.csv:  advsolar.client.nei.MTRecipeHandler,Advanced Solar Panels,AdvancedSolarPanel:BlockMolecularTransformer,,AdvancedSolarPanel,TRUE,,,68,,5,...
catalysts.csv: advsolar.client.nei.MTRecipeHandler,AdvancedSolarPanel:BlockMolecularTransformer,,AdvancedSolarPanel,TRUE,...
```

### Runtime behaviour

- `GuiRecipeTab.loadHandlerInfo()` parses `handlers.csv` into `handlerMap`.
  `IRecipeHandler.getHandlerId()` **defaults to `getClass().getName()`**, so rows are keyed by the
  handler's **FQCN**. `getHandlerInfo(handler)` looks up `(getHandlerId(), overlayIdentifier)` and
  falls back to `DEFAULT_HANDLER_INFO` when there is no match. This data drives the recipe tab's mod
  name, catalyst icon and tab ordering (`68`).
- `RecipeCatalysts` parses `catalysts.csv` and, for each row, does
  `Class.forName(fqcn).getConstructor().newInstance()` **only to derive the handler's recipe id**, then
  maps recipeId → catalyst stack. `ClassNotFoundException` is caught (row skipped).

### Impact of the rename

- The MT recipe tab still exists and works: `advsolar/client/nei/NEIASPConfig.java` registers it
  directly via `API.registerRecipeHandler` / `API.registerUsageHandler`.
- Lost: the "Advanced Solar Panels" handler info, the catalyst icon, tab ordering (`68`), and the
  Molecular Transformer block → handler catalyst association.
- Nothing crashes; no recipes disappear.

### Config-only fix (no jar patching)

In `config/NEI/client.cfg` (the `tools` block, ~lines 280-283):

```
tools
{
    catalyst_load_from_config=true
    handler_load_from_config=true
}
```

`NEIClientConfig.loadHandlersFromJar()` is compiled as `!getBooleanSetting("tools.handler_load_from_config")`.
When disabled, NEI reads from `NEIClientConfig.handlerFile` / `catalystFile`, which are
`new File(configDir, "handlers.csv"/"catalysts.csv")` with `configDir = <minecraft>/config/NEI/`.
If those files do not exist, NEI copies them out of the jar ("Config file doesn't exist, creating").

Steps:

1. Set both flags to `true`.
2. Launch once (or create `config/NEI/handlers.csv` / `config/NEI/catalysts.csv` manually from the jar).
3. Edit the first column to `com.mrfuzzihead.advancedsolarpanels.client.nei.MTRecipeHandler`
   (or append a second row for the new name; the stale row simply never matches).

Do not confuse this with `config/NEI/handlerordering.csv` (unrelated feature) or the empty
`config/NEI/handlers.patch`.

---

## 5. GTNH Web Map — no impact at all

`renderdata/AdvancedSolarPanel-texture.txt` inside `gtnh-web-map-0.4-beta-1.jar` maps blocks by
**block id**, not by class:

```
# %BlockAdvSolarPanel:* (BlockAdvSolarPanel), render=(STANDARD), opaque=true,cls=advsolar.common.blocks.BlockAdvSolarPanel
block:id=%BlockAdvSolarPanel,data=0,stdrot=true,face0=0:advancedsolarpanel/asp_bottom,...
block:id=%BlockAdvSolarPanel,data=1,stdrot=true,face0=0:advancedsolarpanel/hsp_bottom,...
block:id=%BlockAdvSolarPanel,data=2,...   (usp)
block:id=%BlockAdvSolarPanel,data=3,...   (qsp)
block:id=%BlockAdvSolarPanel,data=4,stdrot=true,face0-5=0:advancedsolarpanel/qgen_a_side
```

- The `cls=` text sits on a `#` **comment** line. Both `HDBlockModels` and `TexturePack` skip comment
  lines via `startsWith("#")` (verified against their constant pools / disassembly).
- A live `cls=` property only matters for `render=<CustomRenderer>` entries
  (`HDBlockModels$CustomBlockModel` → `Class.forName(...).newInstance()`, exceptions caught and logged
  as "Error loading custom renderer"). Here the entry is `render=(STANDARD)`.
- `modname:AdvancedSolarPanel`, `cfgfile:config/AdvancedSolarPanel.cfg` and
  `texturepath:assets/advancedsolarpanel/textures/blocks/` are modid/asset based — also unaffected.
- `deleted.txt` lists `renderdata/ic2-advsolarpanels-texture.txt` (an old filename); stale bookkeeping
  only.

**Nothing breaks and nothing needs patching.** For tidiness, drop a corrected copy at
`minecraft/dynmap/renderdata/AdvancedSolarPanel-texture.txt` — the bundled
`dynmap/renderdata/README` states: *"Most renderdata files are now internal - custom files can still be
placed in the dynmap/renderdata directory."*

---

## 6. Rename checklist

### Self-references inside the mod (must be updated)

| Location | Current |
|---|---|
| `src/main/java/advsolar/api/ASPItemAPI.java:16` | `Class.forName("advsolar.common.AdvancedSolarPanel")` |
| `src/main/java/advsolar/common/AdvancedSolarPanel.java:50-51` | `clientSide = "advsolar.client.ASPClientProxy"`, `serverSide = "advsolar.common.ASPServerProxy"` |
| every `package`/`import` | `advsolar.*` → new root |

`grep -rn '"advsolar' --include=*.java src` is the authoritative check — those three are currently the
only string-based ones.

### Build metadata (`gradle.properties` — still the CompactSolars template)

```
modName  = CompactSolars                                   -> AdvancedSolarPanels
modId    = CompactSolars                                   -> AdvancedSolarPanel   (MUST stay identical to @Mod(modid), or worlds break)
modGroup = com.mrfuzzihead.compactsolars                   -> com.mrfuzzihead.advancedsolarpanels
```

`modGroup` is only used for metadata substitution / mixin-JSON generation / run-task assertions
(nothing in `build.gradle.kts`/`settings.gradle.kts` references it directly), so it does **not** move
sources automatically.

### Do after the move

- [ ] Port the MT Core reflector fix (§3) — or keep HodgePodge enabled and accept `fixMTCoreRecipe=false`.
- [ ] Confirm the startup log shows only the one HodgePodge WARN; optionally launch with
      `-Dmixin.debug.verbose=true` to see the skip reason.
- [ ] Flip `tools.handler_load_from_config` / `catalyst_load_from_config` and fix the NEI CSV FQCN (§4).
- [ ] Optional: refresh the stale `cls=` comment in `dynmap/renderdata/` (§5).
- [ ] **Do not change `modid`** — `AdvancedSolarPanel:*` registry names appear in AmunRa, gravisuiteneo,
      AE2 facades and JourneyMap colour palettes.
