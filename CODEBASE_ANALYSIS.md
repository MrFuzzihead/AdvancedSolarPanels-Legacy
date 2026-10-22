# AdvancedSolarPanels-Legacy — Codebase Review, IC2 Parity Analysis, and Test Plan

**Reviewed tree:** `main` working tree (initial commit `ed4b3d5` plus uncommitted source)
**Target:** Minecraft 1.7.10, Forge 10.13.4.1614, IndustrialCraft² 2.2.828-experimental
**Current source root:** `src/main/java/com/mrfuzzihead/advancedsolarpanels` (the `advsolar → com.mrfuzzihead.advancedsolarpanels` rename landed while this review was being written)
**Reference sources:**
- IC2 `2.2.828-experimental:dev`: `TileEntitySolarGenerator`, `TileEntityBaseGenerator`, `ItemArmorSolarHelmet`, `EnergyNet`, decompiled from the exact dependency used by this build.
- Upstream `AdvancedSolarPanel_1.7.10_Edition.jar` (v3.5.1): decompiled and diffed against this tree. The port is a decompile + MCP-name cleanup + package rename; no functional changes were found in that diff except the package strings and formatting.
- `NAMESPACE_CHANGE.md` in this repository.
- GitHub code search and the production pack at `FuzzCraft-Legacy/minecraft/mods`.

**Method:** static review of 45 Java files (~5,000 lines), resource/lang audit, source/metadata validation, build check, `NAMESPACE_CHANGE.md` verification, decompiled IC2/upstream comparison, and a formula-level simulation of the 1.7.10 sun/light math.

> **First-batch fixes implemented after this review:** the NEI compile dependency was corrected, the MT Core reflector recipe now uses a fresh stack, packets are scheduled onto the game thread (server task queue + client task scheduler), `Thread.getAllStackTraces()` was removed, the MT GUI recipe-index crash and static caption leak were fixed, MT recipes are validated (non-negative energy, 1–64 stack sizes), the MT energy buffer/progress is persisted and capped, `getDemandedEnergy` accounts for buffered energy, output-blocked injection is rejected, metadata/config/divide-by-zero/NPE guards were added, panel charge slots now use IC2-style simulated charge acceptance (electric, not full, not stacked), panel drops now use IC2’s wrench/casing behavior, `modId`/`modName` metadata now match `@Mod`, metal block sounds were set on the machines, and a 44-test JUnit baseline was added (`./gradlew build` passes).
>
>
> **Second-batch changes implemented:** real panel source/charge tiers (1/2/3/5), MT sink tier EV (4),
> MT GUI recipe data sync via description packets, MT recipe indexing and NBT-aware matching,
> dynamic ore-dictionary input matching, container change-only sync, panel shift-click validation and
> range fixes, QGen max-packet cap / GUI layout / redstone indicator / shift-click cleanup, and solar
> helmet parity (stateless IC2 sun visibility, panel tiers, tooltip, inventory sync) plus the new
> helmet charging modes (chest-first/self-overflow by default, legacy all-armor/inventory via config)
> and graceful light scaling (full day, 2/3 dawn/dusk, 1/3 night/rain). Solar *panel* generation rules
> remain deferred.
>
> The line references below were captured before the Spotless formatting pass; use the class/method names and paths as the authoritative anchors.

**Severity legend:** **S1** crash / data loss / build blocker / legal, **S2** gameplay and IC2-parity, **S3** UI/UX, **S4** robustness / performance / maintainability, **S5** dead code / hygiene.

---

## 0. Executive summary

This is a functional decompile of Advanced Solar Panels 3.5.1, not a clean reimplementation. It has the upstream feature set and several upstream bugs, plus a few problems introduced by the port/build setup. The highest-impact findings are:

1. **(Fixed in the first batch)** The project initially did not compile because `client/nei/MTRecipeHandler` and `NEIASPConfig` import `codechicken.nei.*`, while `dependencies.gradle` declared NEI as `runtimeOnlyNonPublishable`. It now uses `devOnlyNonPublishable`; `./gradlew build` succeeds. See §2.1.
2. **The MT Core recipe bug from `NAMESPACE_CHANGE.md` is still present.** The recipe passes IC2’s shared `reactorReflectorThick` stack directly to the recipe, which HodgePodge patches in GTNH. After the package rename, HodgePodge’s mixin target is gone, so it only logs a WARN and the bug is unfixed. See §2.1 and `NAMESPACE_CHANGE.md §3`.
3. **`PacketChangeState.execute()` and `PacketGUIPressButton.execute()` run on the Netty thread, not the game thread.** They mutate tile entities and call `world.markBlockForUpdate` off-thread. `TileEntityMolecularTransformer.onServerEvent` additionally calls `Thread.getAllStackTraces()` on every packet. This is a serious race/crash source and a likely cause of the well-known “Molecular Transformer crashes the client” report. See §2.1/B4.
4. **The Molecular Transformer GUI indexes the client recipe list with a server-supplied index** (`MTRecipeManager.transformerRecipes.get(lastRecipeNumber)`). If the client config differs, the list is empty, or GT5/another mod cleared the list, the GUI throws `IndexOutOfBoundsException`. The static `guiTextList` also grows on every GUI open and leaks the GUI/tile-entity/world through its non-static `captionRecord`. See §2.1/B5.
5. **The MT recipe format accepts zero/negative stack sizes and negative energy.** `oredict:ingotIron:0` creates a zero-input recipe; `canSmelt()` consumes 0 and emits output forever. Negative stack sizes increase the input stack; negative energy creates EU. Non-oredict stack sizes are clamped but oredict ones are not. See §2.1/B6.
6. **The solar panels do not follow IC2’s solar rules.** ASP uses `isDaytime()` (effectively sky light ≥ 12) and a flat `genDay`/`genNight`, while IC2 uses `skyLight > 4`, linear `skyLight/15` scaling, and a desert/rain predicate. Night generation and rain generation are intentional ASP features, but the day/twilight distribution and the weather/biome rules differ. See §3.
7. **The energy tiers are not IC2-like.** All panels and the Quantum Generator return `Integer.MAX_VALUE` from `getSourceTier()`; panels charge items at tier `Integer.MAX_VALUE`; the MT returns `Integer.MAX_VALUE` from `getSinkTier()`. This bypasses tier restrictions, and with IC2’s optional linear transfer model `getPowerFromTier(Integer.MAX_VALUE)` is `Infinity`, which can poison the energy-net matrix. See §3.2.
8. **The Quantum Generator’s `maxPacketSize` is stored and displayed nowhere and never used.** The GUI can change `production` but not the packet size; `getOfferedEnergy()` returns the full production as one packet. The advertised “5120 EU in 10×512 packets” behavior is not implemented. See §2.2/B9.
9. **The solar helmet stores generation state on the shared `Item` instance**, not per `ItemStack`/player: `ticker`, `sunIsUp`, `skyIsVisible`, `wetBiome`, `noSunWorld`, `generating`. On a server with multiple wearers, one player’s visibility state drives another player’s charge tick. See §2.2/B10.
10. **Metadata/docs were inconsistent.** `gradle.properties` had `modId = advancedsolarpanels` while `@Mod(modid = "AdvancedSolarPanel")`; this is now corrected to `AdvancedSolarPanel`, and `modName` matches the display name. `README.md` has since been rewritten for ASP. See §2.5 and §5.
11. **The package rename has more external consumers than `NAMESPACE_CHANGE.md` found.** The report correctly identified HodgePodge, NEI, and the GTNH web map. GitHub code search additionally shows GT5-Unofficial imports `advsolar.utils.MTRecipeManager` (compile-time) and NewHorizonsCoreMod’s coremod transformer targets `advsolar.common.AdvancedSolarPanel` and injects references to `advsolar/utils/MTRecipeManager` and `advsolar/api/MTAPI`. The target pack does not contain those jars, so the immediate pack is unaffected, but the GTNH ecosystem is not. See §6.

The code is small and fixable. The recommended order is: make it compile, fix the MT Core recipe, fix the MT GUI/packet crashes and recipe validation, correct metadata/licensing, then do the IC2-parity pass and add the test suite described in §7.

---

## 1. Codebase map

| File | Responsibility | Notes |
| --- | --- | --- |
| `AdvancedSolarPanel.java` | `@Mod` entry point; config, blocks/items, TE registration, all recipes, MT API init | 589 lines; config is read into 18 public static ints plus 11 private booleans; recipe registration is a long if/else tree |
| `common/tiles/TileEntitySolarPanel.java` | Base solar panel: energy source, 4 charge slots, sun/weather state, NBT, wrench | 407 lines; flat day/night generation; `Integer.MAX_VALUE` source/charge tier |
| `common/tiles/TileEntityAdvancedSolarPanel.java` etc. | Four tier constructors + `getInvName` overrides | `getInvName()` is not part of `IInventory` and is dead; `getInventoryName()` is inherited and hard-coded |
| `common/tiles/TileEntityQGenerator.java` | Admin “infinite” generator; GUI-editable production; redstone disable | `maxPacketSize` is dead; source tier MAX; no recipe |
| `common/tiles/TileEntityMolecularTransformer.java` | Recipe-driven energy sink, inventory, progress, active sync | 625 lines; complex state machine with several edge-case bugs |
| `common/tiles/TileEntityBase.java` | Common wrench/network base | Shadowed `facing`/`active` in subclasses; most network hooks unused |
| `common/blocks/BlockAdvSolarPanel.java` | Five-metadata block: ASP/HSP/USP/QSP/QGen; icons; drops; GUI | NPE in `isActive`; metadata array bounds; manual item drop |
| `common/blocks/BlockMolecularTransformer.java` | MT block; custom renderer; drops | `blockIcon` never assigned; NPE in `isActive`; cast in breakBlock |
| `common/items/ItemAdvanced.java` | 14 crafting-item metadata subtypes | No bounds checks; dead 1.6-style `getSubItems(int, ...)` |
| `common/items/ItemAdvSolarPanel.java` | Five block-item subtypes and rarities | No bounds checks |
| `common/items/ItemMolecularTransformer.java` | MT block item | Inherits null item icon due to block icon bug |
| `common/items/ItemAdvancedSolarHelmet.java` | Three solar helmets; IElectricItem, IMetalArmor, ISpecialArmor; charges inventory; air supply | Shared Item-instance state; self/armor charging order; tier/limit differences from IC2 |
| `common/container/ContainerAdvSolarPanel.java` | Four charge slots + player inventory; progress-bar sync; shift-click | All-items slots; off-by-one shift ranges; sends updates every tick |
| `common/container/ContainerMolecularTransformer.java` | Input/output slots + player inventory; 9 progress-bar channels | Sends some values every tick; 64-block interaction distance |
| `common/container/ContainerQGenerator.java` | Player inventory only; 3 progress-bar channels | No `transferStackInSlot`; sends every tick; GUI height mismatch |
| `client/gui/GuiAdvSolarPanel.java` | Panel GUI | Division by zero if maxStorage=0; no tooltips |
| `client/gui/GuiMolecularTransformer.java` | MT GUI, captions, progress, recipe display | Static list leak; recipe-list OOB; client/server recipe-index coupling |
| `client/gui/GuiQGenerator.java` | QGen GUI, production buttons | AWT KeyListener, dead text fields, no packet controls, inverted indicator |
| `client/gui/GuiHandler.java` | Duplicate of proxy GUI logic | Never registered; debug `System.out`; dead |
| `client/ASPClientProxy.java` / `common/ASPServerProxy.java` | Sided GUI/render/recipe setup | Reflection into IC2 for face mapping; `MTAPI.manager` initialized in postInit |
| `network/ASPChannelHandler.java` | FML packet codec; calls `execute()` in `decodeInto` | Runs packets on Netty thread |
| `network/ASPPacketHandler.java` | Channel registration and send helpers | `sendToAllPlayers` ignores dimensions; `sendToPlayer` unused |
| `network/PacketChangeState.java` | Active-state packet with optional NBT | Client-only `execute`; unbounded NBT length; no dimension |
| `network/PacketGUIPressButton.java` | QGen button packet | No distance/permission check; runs off-thread |
| `utils/MTRecipeConfig.java` | MT recipe file parser/writer | Whole-file try/catch; zero/negative stack exploit; ignores explicit sizes |
| `utils/MTRecipeManager.java` | Static MT recipe list; OreDict helpers; reflection for item names | No validation; static mutable list; GT5 clears it |
| `utils/MTRecipeRecord.java` | Input/output/energy record | No validation |
| `utils/RecipeRecord.java` | Duplicate of `MTRecipeRecord` | Unused |
| `utils/ItemStackUtil.java` | Item+damage (wildcard 32767) equality | Ignores NBT; null-item edge case |
| `utils/MySlot.java` | Output slot that rejects insertion | `Boolean` wrapper can be null |
| `api/ASPItemAPI.java` | Reflective access to public static ASP items | Fragile; new FQCN string |
| `api/MTAPI.java` / `api/IMTRecipeManager.java` | Third-party MT recipe API | `manager` is null until postInit; no clear/get/remove |
| `client/nei/MTRecipeHandler.java` / `NEIASPConfig.java` | NEI recipe/usage handler | Compile-scope bug; hard-coded English; overlay identifier missing |
| `client/renderers/tile/TileMolecularTransformerRenderer.java` | TESR model + animated core | Allocations per frame; unused `rotateBlock`; raw cache |
| `client/renderers/tile/ModelMolecularTransformer.java` | Static model | Fine |
| `client/renderers/block/BlockMolecularTransformerRenderer.java` | Inventory/world renderer | Renderer delegation; block icon bug affects particles |
| `resources/.../lang/en_US.lang` / `ru_RU.lang` | 33 keys each | Key sets match; `tile.AdvancedSolarPanel.name` is dead |
| `resources/mcmod.info` | Metadata | `${modId}` mismatch; `${modVersion}` vs hard-coded @Mod version |
| `README.md`, `LICENSE`, `NAMESPACE_CHANGE.md` | Docs/legal | README has been rewritten for ASP; namespace report needs an update |

---

## 2. Bugs

### 2.1 S1 — Build, crash, data-loss, and legal issues

#### B1. (Fixed) Build was broken: NEI classes were used at compile time but declared runtime-only

`dependencies.gradle:1-7`:

```groovy
api("net.industrial-craft:industrialcraft-2:2.2.828-experimental:dev")
runtimeOnlyNonPublishable("com.github.GTNewHorizons:NotEnoughItems:2.8.162-GTNH:dev")
```

`client/nei/MTRecipeHandler.java:7-11` imports `codechicken.lib.gui.GuiDraw`,
`codechicken.nei.PositionedStack`, and `TemplateRecipeHandler`; `NEIASPConfig.java:3-4` imports
`codechicken.nei.api.API` and `IConfigureNEI`.

`./gradlew compileJava --offline` originally produced 27 errors, all in those two files
(`package codechicken... does not exist` / `cannot find symbol`). The rest of the source compiles.

**Fix:** add NEI to the compile classpath. The GTNH convention plugin used here exposes
`devOnlyNonPublishable` (compile+runtime, not published), so the smallest change is:

```groovy
devOnlyNonPublishable("com.github.GTNewHorizons:NotEnoughItems:2.8.162-GTNH:dev")
```

or keep the existing runtime declaration and add the standard Gradle
`compileOnly("com.github.GTNewHorizons:NotEnoughItems:2.8.162-GTNH:dev")`. Note that this plugin
does **not** define `compileOnlyNonPublishable`; verified with an init-script dump of the project
configurations. If tests instantiate NEI classes, add `testCompileOnly(...)` / `testRuntimeOnly(...)`
(the built-in test configurations).

#### B2. (Fixed) MT Core recipe mutated IC2’s shared thick-neutron-reflector stack

`AdvancedSolarPanel.java:471` (formerly `common/AdvancedSolarPanel.java`):

```java
GameRegistry.addRecipe(itemMTCore, new Object[]{"MXM", "M M", "MXM",
    'M', itemIrradiantGlassPane, 'X', IC2Items.getItem("reactorReflectorThick")});
```

IC2 caches `reactorReflectorThick` and the recipe path mutates its damage (the known 10000/10001
durability bug). HodgePodge’s `MixinAdvancedSolarPanel` exists solely to replace this with a fresh
`new ItemStack(item, 1, 1)`. After the package rename, the mixin target
`advsolar/common/AdvancedSolarPanel` is gone; UniMixins logs one WARN and drops the mixin, so the
bug is unfixed even though HodgePodge is present. `NAMESPACE_CHANGE.md §3` documents this in detail.

**Fix (ASP-side, preferred):**

```java
ItemStack reflector = IC2Items.getItem("reactorReflectorThick");
if (reflector != null) {
    reflector = new ItemStack(reflector.getItem(), 1, 1);
    GameRegistry.addRecipe(itemMTCore, new Object[]{"MXM", "M M", "MXM",
        'M', itemIrradiantGlassPane, 'X', reflector});
} else {
    AdvancedSolarPanel.addLog("IC2 reactorReflectorThick is missing; MT Core recipe skipped.");
}
```

After this, HodgePodge’s fix is redundant; leave it enabled (it skips) or set
`config/hodgepodge.cfg B:fixMTCoreRecipe=false` to silence the WARN.

#### B3. (Fixed) Packets executed on the Netty thread

`network/ASPChannelHandler.java:30-43`:

```java
public void decodeInto(ChannelHandlerContext ctx, ByteBuf source, IPacket msg) {
    ...
    msg.read(new DataInputStream(bais));
    msg.execute();          // <- Netty thread
}
```

`PacketGUIPressButton.execute()` calls `DimensionManager.getWorld(dimID).getTileEntity(x,y,z)` and
`((IHasButton) te).handleButtonClick(...)`; `PacketChangeState.execute()` calls
`Minecraft.getMinecraft().theWorld.getTileEntity(...)` and `onServerEvent`, which mutates the TE and
calls `world.markBlockForUpdate`. Neither is thread-safe.

`TileEntityMolecularTransformer.onServerEvent` (`:543-552`) also calls
`Thread.getAllStackTraces()` on every packet. That captures every thread’s stack; it is a debug
leftover and an enormous per-packet cost.

**Fix:** enqueue the work onto the correct thread:

```java
// server: MinecraftServer.getServer().func_152344_a(() -> ...);
// client: Minecraft.getMinecraft().func_152344_a(() -> ...);
```

or use FML’s sided scheduler/`FMLCommonHandler.instance().getSidedDelegate()` equivalent. Remove
`Thread.getAllStackTraces()` entirely.

#### B4. (Fixed) MT GUI could crash with `IndexOutOfBoundsException`

`client/gui/GuiMolecularTransformer.java:98-101`:

```java
ItemStack inputStack  = MTRecipeManager.transformerRecipes.get(this.tileentity.lastRecipeNumber).inputStack;
ItemStack outputStack = MTRecipeManager.transformerRecipes.get(this.tileentity.lastRecipeNumber).outputStack;
...
MTRecipeManager.transformerRecipes.get(this.tileentity.lastRecipeNumber).energyPerOperation
```

`lastRecipeNumber` is supplied by the server through progress-bar channel 6. The client’s
`transformerRecipes` list is loaded from the client’s own config and can differ from the server’s,
can be empty, or can have been cleared by GT5. The result is an `IndexOutOfBoundsException` while
drawing the GUI. This matches the public “Molecular Transformer crashes the client” reports.

**Fix:** never index the client list with a server index. Either:
- sync `lastRecipeInput`, `lastRecipeOutput`, and `lastRecipeEnergyPerOperation` in the container
  description/packet and read those, or
- bounds-check and fall back to empty/placeholder text, and still guard the index.

#### B5. (Fixed) `GuiMolecularTransformer` leaked a GUI (and therefore a world) on every open

`GuiMolecularTransformer.java:21`:

```java
public static List<GuiMolecularTransformer.captionRecord> guiTextList = Lists.newArrayList();
```

`setCaptionText()` (`:33-61`) appends five entries every time a GUI is constructed. `captionRecord`
(`:152-155`) is a non-static inner class, so each entry retains its enclosing
`GuiMolecularTransformer`, which retains `tileentity` and `worldObj`. Opening the MT repeatedly grows
the static list forever and prevents old worlds/TEs from being collected. It also makes the caption
lookup depend on ever-more entries.

**Fix:** make `captionRecord` `static`, and either clear the list at the start of `setCaptionText`
or (better) initialize a `static final` list once / compute captions without a static collection.

#### B6. (Fixed) MT recipe parser accepted zero/negative stack sizes and negative energy

`utils/MTRecipeConfig.java:244-288` (`wrap`):

- Non-oredict path clamps `stackSize <= 0` to 1 (`:267-269`).
- The oredict path (`:249-262`) does **not** clamp. `oredict:ingotIron:0` creates an input stack of
  size 0. `canSmelt()` (`TileEntityMolecularTransformer.java:218`) checks
  `workSlots[0].stackSize < recipe.inputStack.stackSize`; `0 < 0` is false, so it consumes 0 and
  produces output every cycle. `oredict:ingotIron:-5` *increases* the input stack on every cycle.
- `parse` (`:186-193`) accepts any integer energy, including negative. With a negative
  `lastRecipeEnergyPerOperation`, `gainFuel` (`:234-271`) produces extra EU instead of consuming it.
- Oredict stack sizes above 64 make inputs/outputs unusable; output >64 never fits the output slot.

**Fix:** validate in one place and reuse both in `parse` and `MTRecipeManager.addMTRecipe`:
stack size `1..min(64, item.getMaxStackSize())`, energy `>= 0` (or `> 0` if zero-energy recipes are
not intentional), and reject nulls. Never trust the file/API.

#### B7. (Fixed) Metadata/damage outside 0..N could crash

- `BlockAdvSolarPanel.getIcon(IBlockAccess,...,int blockSide)` (`:107-114`) uses `blockMeta` as
  `iconBuffer[blockMeta]`. A meta-5+ block (corrupted world, `/setblock`, another mod) throws
  `ArrayIndexOutOfBoundsException` during rendering.
- `BlockAdvSolarPanel.isActive` (`:120-122`) casts the TE to `TileEntityBase` without a null/type
  check. If the TE is missing (block break particles, chunk edge, worldgen), it throws NPE.
- `BlockMolecularTransformer.isActive` (`:47-49`) has the same null cast.
- `ItemAdvanced.getUnlocalizedName(ItemStack)` (`:26-28`) and `getIconFromDamage(int)` (`:30-32`)
  index arrays/lists without bounds checks. `/give ... asp_crafting_items 14` or a wildcard-damage
  query crashes item tooltip/icon rendering.
- `ItemAdvSolarPanel.getUnlocalizedName` (`:27-29`) has the same issue for block meta 5+.

**Fix:** centralize a `clampMetadata`/`nameForMeta` helper that clamps and logs; return a
non-null icon/name fallback; guard `isActive` with `instanceof TileEntityBase`.

#### B8. (Fixed) Divide-by-zero in the solar GUI

`TileEntitySolarPanel.gaugeEnergyScaled(int i)` (`:256-258`) is `storage * i / maxStorage`.
`maxStorage` comes from config; `AdvancedSPStorage=0` (or a negative value) makes
`GuiAdvSolarPanel` (`:42-44`) throw `ArithmeticException` when the panel GUI opens. Config also
accepts negative generation/output, which can make `storage` negative and `getOfferedEnergy()`
negative (the energy net assumes a non-negative source amount).

**Fix:** validate/clamp config at load (`gen >= 0`, `storage >= 1`, `output >= 1`), and make the
gauge return 0 when `maxStorage <= 0`.

#### B9. (Fixed) `MTRecipeConfig.parse` aborted the whole file on one malformed line

The entire read loop is inside one `try/catch (Throwable)` (`:137-242`). A `NumberFormatException`
from `wrap` (e.g. `oredict:ingotIron:notanumber`) or an I/O error skips every remaining recipe and
returns only the recipes parsed before the error. A leading BOM or a `version` line with leading
whitespace is not recognized and falls into recipe parsing.

**Fix:** parse line-by-line into a pure `RecipeSpec`; catch per-line; strip BOM/trim first; report
line numbers; use try-with-resources and UTF-8.

#### B10. (Fixed) MT energy buffer was not persisted and could be lost/overfilled

`TileEntityMolecularTransformer`:

- `energyBuffer` (`:72`) is not read/written in `readFromNBT`/`writeToNBT` (`:306-411`).
- `injectEnergy` (`:579-590`) casts `amount`/buffer math to `int`, discarding fractional EU.
- `getDemandedEnergy` (`:570-577`) ignores `energyBuffer`, so when a leftover buffer exists the
  machine can demand and accept the full remaining recipe energy again.
- While `waitOutputSlot` is true (output full), the machine returns `energyLeft` unchanged from
  `gainFuel`, so accepted energy sits in the buffer until the output clears; it is then carried into
  the next recipe. If the chunk unloads first, it is lost. The buffer has no upper bound.
- `updateProgress` (`:273-287`) divides by `lastRecipeEnergyPerOperation`; zero is harmless in float
  math but should be handled explicitly. Negative energy is a duplication bug (B6).

**Fix:** use `double` for buffer and packet accounting, persist it, bound it (e.g. one recipe's
worth), subtract it in `getDemandedEnergy`, and reject injection while output-blocked.

#### B11. (Metadata/docs fixed) Inconsistent metadata and documentation

- `LICENSE` is MIT, but the 1.7.10 jar this source was decompiled from
  (`AdvancedSolarPanel_1.7.10_Edition.jar`, JarviceOne’s unofficial CurseForge upload) is listed as
  **All Rights Reserved**. This is not the GPLv3 CompactSolars situation. Do not distribute until
  the license/permission is resolved; add the upstream notices/credits.
- `README.md` was the **CompactSolars** README and has been rewritten for ASP.
- `gradle.properties` now uses `modId = AdvancedSolarPanel`, matching `@Mod` and the `mcmod.info`
  substitution. `modName` now matches the in-code display name.
- `@Mod` now uses `Tags.VERSION` (a git-derived version), so existing worlds may log a version-change warning; pin a semantic version if that matters.

**Remaining:** use `Tags.VERSION` or a single version source so the `@Mod` version and build metadata
cannot diverge. The modid must stay `AdvancedSolarPanel` to preserve `AdvancedSolarPanel:*` registry
names and existing worlds.

### 2.2 S2 — Gameplay and IC2-parity bugs

#### B12. Panels do not use IC2’s sun/light rules

IC2 (`TileEntitySolarGenerator`):

```java
public boolean gainEnergy() {
    if (ticker++ % tickRate() == 0) updateSunVisibility();
    if (sunIsVisible) {
        storage += solarbasevalue * (getskylight(world,x,z) / 15.0F);
        return true;
    }
    return false;
}
public static boolean isSunVisible(World w,int x,int y,int z) {
    return getskylight(w,x,z) > 4
        && !w.provider.hasNoSky
        && w.canBlockSeeTheSky(x,y,z)
        && (w.getWorldChunkManager().getBiomeGenAt(x,z) instanceof BiomeGenDesert
            || (!w.isRaining() && !w.isThundering()));
}
```

ASP (`TileEntitySolarPanel.gainFuel`/`updateVisibility`, `:178-207`):

```java
if (sunIsUp && skyIsVisible) generating = genDay;
else if (skyIsVisible)     generating = genNight;
else                       generating = 0;

rainWeather = wetBiome && (world.isRaining() || world.isThundering());
sunIsUp = world.isDaytime() && !rainWeather;
skyIsVisible = world.canBlockSeeTheSky(x, y+1, z) && !noSunWorld;
```

Consequences:

- ASP switches to `genDay` only when `isDaytime()` is true. In 1.7.10
  `WorldProvider.isDaytime()` is `skylightSubtracted < 4`, i.e. top-of-world sky light ≈ 12–15.
  IC2 starts at sky light > 4 and scales linearly. At dawn/dusk ASP is on `genNight` while IC2 is on
  a 0.33–0.73 ramp.
- ASP gives full `genDay` at sky light 12–14 (IC2 gives 0.8–0.93).
- ASP gives `genNight` at sky light 5–11 and at sky light 4 (night). IC2 gives 0.33–0.73 at 5–11 and
  **0** at night. Night generation is intentional ASP design, but it is not IC2 parity.
- In a wet biome during rain/thunder, ASP gives `genNight`; IC2 gives **0** (non-desert). In a
  zero-rainfall non-desert biome (e.g. mesa/savanna in vanilla), ASP treats the biome as dry and can
  give full `genDay` during rain; IC2 only exempts `BiomeGenDesert`.
- ASP refreshes visibility every 128 ticks and on load; IC2 does the same tick rate and refreshes on
  load. Both can be stale for up to 6.4 s at a transition.

**Parity recommendation:** add a config option (`useIC2SolarRules`, default false to preserve ASP
behavior or true for parity). Implement IC2’s predicate and linear scaling with a `double` storage
or fractional accumulator. Keep `genNight` behind a separate `nightGeneration` option (default
upstream behavior) so players can choose.

#### B13. (Fixed) Energy source tier was `Integer.MAX_VALUE` instead of the panel tier

Panels previously returned `Integer.MAX_VALUE` from `getSourceTier()` (the QGen still does by
request). IC2’s base solar returns tier 1 and caps offered energy at
`EnergyNet.getPowerFromTier(tier)`.

Problems:

- The panel can feed any sink (LV panel → MFSU) and bypasses tier-based cable/sink expectations.
- With IC2’s `misc/useLinearTransferModel=true`, `getPowerFromTier(Integer.MAX_VALUE)` is
  `8 * 4^2147483647` = `Infinity`; `Grid.populateNetworkMatrix` uses that as a resistance/voltage,
  producing `Infinity`/`NaN` and broken energy-net calculations.
- The output is not validated against the tier. The defaults are consistent only by accident:
  Advanced 32 = tier 1 (32), Hybrid 128 = tier 2, Ultimate 512 = tier 3, Quantum 8192 = tier 5.

**Fix applied:** panels now carry tiers 1/2/3/5, return them from `getSourceTier()`, and cap
`getOfferedEnergy()` with `EnergyNet.getPowerFromTier(tier)`. Charge slots use the same tier. The MT
uses sink tier EV (4). The QGen remains at `Integer.MAX_VALUE` deliberately, per request.

#### B14. (Fixed) Charge slots accepted any item and charged at `Integer.MAX_VALUE`

`TileEntitySolarPanel.updateEntity` (`:155-172`) calls
`ElectricItem.manager.charge(slot, storage, Integer.MAX_VALUE, false, false)` for every slot;
`isItemValidForSlot` (`:380-382`) returns true for everything. This means:

- non-electric items can occupy the charge slots (no function, but confusing and hopper-insertable);
- an LV panel can charge an HV item;
- the charge tier does not match the panel tier.

**Fix applied:** `ContainerAdvSolarPanel` now uses `SlotElectricItem`, which mirrors IC2's
`InvSlotCharge.accepts`: it simulates a charge at the panel tier and rejects non-electric, full,
stacked, or tier-incompatible items. `isItemValidForSlot` applies the same check for hoppers.

#### B15. (Fixed) Wrench drop rate and pickaxe drops differed from IC2

`TileEntitySolarPanel.getWrenchDropRate()` now returns `0.9F`, matching IC2’s
`TileEntityBaseGenerator`. `BlockAdvSolarPanel.getDrops()` returns a Basic Machine Casing as its
first drop, and IC2’s wrench replaces that first drop with the panel on a successful roll. Pickaxe
breaks therefore return a casing; a failed wrench roll also returns a casing. Charge-slot contents
are dropped separately by `breakBlock`.

#### B16. (Fixed) Helmet generation state was shared across all helmets of the same tier

`ItemAdvancedSolarHelmet` (`:26-41`, `:86-157`) stores `ticker`, `generating`, `sunIsUp`,
`skyIsVisible`, `wetBiome`, `noSunWorld` on the `Item` singleton, not on the `ItemStack` or player.
`onArmorTick` is called for every wearer; each call increments the same `ticker` and overwrites the
shared visibility fields. With two players wearing the same helmet tier:

- the visibility refresh happens every `128 / numberOfWearers` ticks per player, and
- one player’s `sunIsUp`/`skyIsVisible` can be used to charge another player’s items.

**Fix applied:** the helmet is now stateless. It computes `SolarMath.isSunVisible` from the wearer's
position every tick and no longer stores ticker/visibility/build-up state on the Item singleton.

#### B17. (Fixed) Helmet charging model differed from IC2 and could self-charge

- ASP charges all armor slots (including the helmet itself) and then the whole main inventory.
  IC2’s solar helmet charges only the chestplate (`armorInventory[2]`).
- ASP passes tier 4 and respects transfer limits (`charge(..., 4, false, false)`); IC2 passes
  `Integer.MAX_VALUE` and ignores transfer limits.
- If the helmet is the only electric item, ASP’s generated EU charges the helmet itself, making it an
  energy buffer rather than armor support. If a chestplate is present it is charged first, so this
  is not always visible, but it is still a deviation.

**Fix applied:** by default helmets charge `armorInventory[2]` first, use `Integer.MAX_VALUE` tier,
ignore the chestplate transfer limit, then send overflow to the helmet itself, and call
`detectAndSendChanges()` after a successful charge. `general.solarHelmetLegacyCharging=true` restores
the original all-armor/main-inventory behavior. Hybrid/Ultimate air supply remains an ASP feature.

#### B18. (Fixed) Quantum Generator `maxPacketSize` was dead in the output path

`TileEntityQGenerator` stores `maxPacketSize` (`:36`, `:48`) and syncs it via
`ContainerQGenerator` (`:32-33`), but `getOfferedEnergy()` (`:232-235`) returns the full
`production` as one packet. The GUI has no max-packet buttons or fields (`GuiQGenerator.java:74-84`),
despite the upstream feature description (“output 5120 in 10×512 packets”). The fields
`maxPacketSizeEdit`/`productonEdit` are declared and never added to the GUI.

**Fix applied:** `getOfferedEnergy()` is now `min(production, maxPacketSize)`, making the field a
per-tick packet cap. The GUI displays both values, NBT defaults fall back to the config, and the
dead text fields were removed. True multi-packet-per-tick output would require custom energy-net
integration and is not implemented.

#### B19. (Fixed) QGen GUI/redstone indicator was inverted

`getActive()` (`:120-128`) sets `active = world.isBlockIndirectlyGettingPowered(...)`.
`getOfferedEnergy()` (`:232-235`) returns `production` only when `!active`, so redstone **disables**
the generator. `GuiQGenerator.drawGuiContainerBackgroundLayer` (`:55-57`) draws the “off” indicator
when `!active`, i.e. while the generator is actually producing. Fix the indicator (or rename the
state to `disabled`/`redstonePowered`).

#### B20. (Fixed) Molecular Transformer tier/overvoltage

`getSinkTier()` now returns EV (4), matching the EV transformer in its recipe. IC2’s standard
machines do not reject over-voltage in `injectEnergy`; the sink tier is used by the energy net. The
previous MAX tier was unsafe with the optional linear transfer model.

### 2.3 S3 — UI/UX and interaction bugs

#### B21. (Fixed) QGen GUI slots did not match the texture

`GuiQGenerator` keeps `ySize = 149`, and `ContainerQGenerator` now places the main inventory at
y=65/83/101 and the hotbar at y=123, matching the texture’s actual slot frames. The packet-size
value is displayed in the header, and player-inventory shift-click is implemented.

#### B22. (Fixed) Shift-click transfer bugs

`ContainerAdvSolarPanel.transferStackInSlot` (`:86-122`):

- The hotbar range is `par2 >= 31 && par2 < 39`; slot 39 (last hotbar slot) is excluded and falls
  into the final branch.
- The hotbar→inventory merge target is `0..30`, which excludes slot 30 (last main-inventory slot)
  and includes the four machine slots; it should merge into the main inventory (`4..31`) and then
  the machine slots if desired.
- The four machine slots are plain `Slot`s, so shift-click and hoppers can insert non-electric items.

`ContainerQGenerator` has **no** `transferStackInSlot` override, so shift-clicking does nothing.

**Fix applied:** panel ranges now use 0–3 machine / 4–30 main / 31–39 hotbar, and QGen gained
main↔hotbar transfer. Panel shift-click also checks `Slot.isItemValid` before merging into the
charge slots; vanilla `Container.mergeItemStack` does not consult slot validity itself.

#### B23. (Fixed) `GuiQGenerator` used AWT and stale keyboard state

`GuiQGenerator` implements `java.awt.event.KeyListener` (`:8-19`) but never registers it; it prints
key codes (`:87-89`) and imports AWT into a game GUI. Shift detection in `actionPerformed` uses
`Keyboard.getEventKey() == 42` (`:62`), which is stale during a mouse click; use
`GuiScreen.isShiftKeyDown()`. **Fix applied:** AWT, dead text fields, and the debug key listener were
removed; shift uses `GuiScreen.isShiftKeyDown()`.

#### B24. (Fixed) GUI/container sync sent every value every tick

- `ContainerAdvSolarPanel.detectAndSendChanges` sends 5 progress-bar updates per crafter per tick
  (`:46-62`), even when nothing changed. It has cached fields but never compares them.
- `ContainerMolecularTransformer.detectAndSendChanges` sends energy used, energy per operation, and
  input EU every tick (`:50-81`), plus conditional doWork/progress/recipe. Only the conditional
  values need sending.
- `ContainerQGenerator.detectAndSendChanges` sends production, max packet, and active every tick
  (`:27-36`); it has no initial `addCraftingToCrafters`.

**Fix applied:** all three containers now cache values, send initial values in
`addCraftingToCrafters`, and send only changed progress-bar channels.

#### B25. (Fixed) Interaction distance was 64 blocks for some machines

`TileEntitySolarPanel.isUseableByPlayer` (`:264-266`) and
`TileEntityMolecularTransformer.isUseableByPlayer` (`:440-442`) use
`player.getDistance(...) <= 64.0`, which is a **64-block** Euclidean radius. Vanilla/IC2 containers
use `getDistanceSq(...) <= 64.0` (8 blocks). `TileEntityQGenerator` correctly uses distance squared
(`:157-161`). Fix the two outliers.

#### B26. Hard-coded English strings and missing tooltips

- `GuiQGenerator` / `GuiMolecularTransformer` / containers use hard-coded “Power/Input/Output/
  Energy/Progress” text; `NEI` uses hard-coded English; `getInventoryName()` is hard-coded.
- Helmet tooltips are now implemented with IC2's `PowerOutput`, `PowerTier`, and `max` keys plus a
  description and air-supply line. Panels, the MT, and the QGen still lack full tooltips. IC2’s solar panel shows
  `Power Output: 8 EU/t max`; ASP should show day/night output, storage, tier, charge tier, and
  current status.
- `tile.AdvancedSolarPanel.name=New Solar Panel` is dead because the blocks never call
  `setUnlocalizedName`.

#### B27. NEI handler registration/transfer

`MTRecipeHandler.getRecipeId()` returns `"Molecular Transformer"`, but in GTNH NEI the overlay
identifier is `getOverlayIdentifier()` (default null). The `RecipeTransferRect` in
`loadTransferRects()` therefore does not get an overlay handler. The handler still appears and its
recipes work, but the “open GUI” transfer and the CSV-driven tab info/catalyst are incomplete. See
§6 for the stale FQCN in NEI’s `handlers.csv`/`catalysts.csv`.

### 2.4 S4 — Robustness, performance, and maintainability

- **Duplicate energy-net load events.** `validate()` calls `onLoaded()` unconditionally
  (`TileEntitySolarPanel:82-88`, QGen `:57-62`, MT `:91-96`); repeated `validate()` calls post
  `EnergyTileLoadEvent` again. Guard with `if (!addedToEnergyNet)`.
- **`initialized` is not reset on unload.** `onUnloaded()` sets `loaded=false` but leaves
  `initialized=true`; a reused/reloaded TE can keep stale sun state. Reset or recompute on load.
- **`markDirty` is not called when storage changes.** Only charge-slot changes mark dirty
  (`TileEntitySolarPanel:173`). Add `markDirty` on storage/generation changes.
- **`drawEnergy` does not clamp.** `storage -= amount` (`:400-402`) can go negative if the net
  draws more than offered; clamp to 0.
- **NBT values are not clamped.** `readFromNBT` accepts negative/oversized storage and invalid
  recipe state; clamp and validate. `MTRecipeConfig`/`TileEntityMolecularTransformer` should also
  validate loaded stack sizes.
- **Description packets carry the entire MT NBT.** `TileEntityMolecularTransformer.getDescriptionPacket`
  (`:559-564`) calls `writeToNBT` (inventory, recipe stacks, progress). `onDataPacket` only reads
  `active`. `setActive` also calls `markBlockForUpdate`, producing a full-NBT description packet on
  every state change, plus a `PacketChangeState` to all players. Send a small active/recipe packet.
- **`ASPPacketHandler.sendToAllPlayers` ignores dimensions.** `PacketChangeState` has no dimension
  field; a client in another dimension with a TE at the same coordinates could receive the event.
  Include `dimID` and check it, or target tracking players.
- **`PacketChangeState.read` allocates an unbounded NBT length.** A malicious client can send a
  large `length` and cause memory pressure. Cap it (e.g. 1 MiB) and validate.
- **`PacketGUIPressButton` has no distance/permission check.** Any client can alter any QGen in any
  loaded dimension if it knows the coordinates. Add a distance/`isUseableByPlayer` check and/or
  require the player who opened the GUI.
- **`MTRecipeConfig` uses platform-default charset and leaks readers on exceptions.** Use UTF-8 and
  try-with-resources.
- **`MTRecipeConfig.addDefaultRecipe` ignores its `inputStackSize`/`outputStackSize` arguments**
  (`:57-100`); it writes `ItemStack.stackSize` instead. Fix or remove the parameters.
- **`MTRecipeConfig.addDefaultRecipe` can NPE** if `getItemData` returns null (`formatItemData`).
- **`MTRecipeConfig.parse` only uses the first ore-dict output entry** (`:203-209`); other
  alternatives are silently ignored. Document or generate a recipe per output.
- **`MTRecipeManager.addMTRecipe` accepts nulls and invalid sizes/energy.** Validate as in B6.
- **`MTRecipeManager.transformerRecipes` is a public static mutable list.** GT5 clears it; external
  code can replace it. Expose a controlled API (`add/remove/clear/getImmutable`) and keep the field
  private/deprecated.
- **`MTAPI.manager` is null until postInit.** Other mods calling it during their preInit/init get
  NPE. Initialize the API object in preInit and only load the recipe file in postInit/loadComplete.
- **`MTRecipeManager.initRecipes` can NPE/SIOOBE on an unexpected config path.** It assumes
  `configFileName` is non-null and contains a `.`; guard for tests and other callers.
- **`Mess` of duplicated state fields**: `TileEntityBase.facing/active` are shadowed by
  `TileEntitySolarPanel.facing`, `TileEntityQGenerator.facing/active`, and MT’s `isActive`. This
  causes subtle bugs (e.g. `BlockMolecularTransformer.isActive` reads the base `active`, which is
  never set) and makes NBT/network code hard to reason about.
- **`getNetworkedFields()` returns an empty fixed list** while the classes implement
  `INetworkDataProvider`/`INetworkUpdateListener`; the network hooks are dead. Remove them or use
  them consistently.
- **`isItemEqual` ignores NBT.** The MT treats two items with the same item+damage as equal even if
  their NBT differs. For output merging this can mix distinct NBT items; for recipe matching it
  ignores NBT entirely (the config format has no NBT support). Decide and document; use
  `ItemStack.areItemStackTagsEqual` where appropriate.
- **Automation can extract the MT input.** `canExtractItem` returns true for both slots
  (`:608-610`), and side 1 exposes slot 0. A hopper can pull the input out; only slot 1 (output)
  should be extractable. `slots_bottom = {1, 1}` is a typo for `{1}`.
- **`TileEntitySolarPanel.getSizeInventory()` and MT inventory slots are not marked dirty on
  direct TE writes** (only through `Slot.onSlotChanged`). Add `markDirty` in
  `setInventorySlotContents`.
- **`MySlot` uses a `Boolean` wrapper** (`utils/MySlot.java:8-17`); a null would NPE. Use primitive
  boolean.
- **`ItemStackUtil` returns true for two stacks with null items** (`first.getItem() == second.getItem()`
  when both null). Guard `getItem() != null`.
- **`BlockAdvSolarPanel.dropItems` reconstructs a stack manually** (`:190`) instead of `item.copy()`;
  it preserves NBT but loses any future ItemStack state and mutates the TE stack to size 0. Use
  `item.copy()`.
- **`TileMolecularTransformerRenderer` allocates `new Color(...)` and list keys every frame**;
  `Arrays.asList` is used as a cache key; `rotateBlock` is unused. Cache the color and use a
  primitive key.
- **`ModelMolecularTransformer` has a large amount of duplicated manual renderer setup**; not a
  bug, but it can be data-driven.
- **`getWrenchDrop` / `getWrenchDropRate`** are duplicated across the base and TEs; unify.
- **`BlockAdvSolarPanel.getIcon` calls `isActive` for every face**, which calls `getTileEntity`;
  cache or use the TE reference passed in.
- **`GuiMolecularTransformer` reads `MTRecipeManager.transformerRecipes` during rendering**; copy
  or cache the recipe data per open GUI and guard bounds.
- **`TileEntityQGenerator.getActive` calls `markBlockForUpdate` from `getOfferedEnergy`**, so the
  energy-net tick can trigger render updates. Compute and cache the redstone state on a tick.
- **`TileEntityQGenerator.readFromNBT` defaults missing production/maxPacket to 0**, leaving a
  legacy/corrupt QGen unable to generate until a GUI click; fall back to the config defaults.
- **`changeProductionOutput`/`changeMaxPacketSize` can overflow** (`int += 1000` repeatedly).
  Clamp to a configured maximum and use `long`/saturating math.
- **`ItemAdvancedSolarHelmet` does not sync the player inventory after charging**; IC2 calls
  `player.inventoryContainer.detectAndSendChanges()`. Add it for immediate client feedback.
- **`BlockAdvSolarPanel`/`BlockMolecularTransformer` have no `setUnlocalizedName`, no
  `setStepSound`, no harvest level, and no `setResistance`.** Add block metadata for proper WAILA,
  sounds, and mining behavior.
- **`AdvancedSolarPanel.getIC2Tab` uses `==` on strings** (`:123`) and has no fallback. Use
  `.equals("IC2")` or `IC2.tabIC2`, and fall back to a vanilla tab if IC2’s tab is absent.
- **`AdvancedSolarPanel`’s static `instance = new AdvancedSolarPanel()` plus `@Instance`** creates a
  spare instance; remove the initializer (FML injects it) or document.
- **Static `Random randomizer` is shared** across all TEs and is not thread-safe in theory; server
  thread only, but use `world.rand`/`ThreadLocalRandom`-style local values.
- **`final Side side` static** is captured at class load; `isSimulating()` uses the effective side
  per call. Prefer `!worldObj.isRemote` in TEs (MT uses `isSimulating`).
- **`BlockAdvSolarPanel.getSubBlocks` reads `iconBuffer.length`**; using a constant metadata count
  avoids a client-side dependency and protects against initialization order.
- **`GuiQGenerator.tex == null` is dead** (static final, never null); remove.
- **`ContainerQGenerator` has no `addCraftingToCrafters`**, so the first GUI tick can display zeros.
- **`MTRecipeHandler.loadCraftingRecipes(String, ...)` calls `outputId.equals(...)`**; guard null.
- **`GuiMolecularTransformer.parsingNumber`** does not handle null/non-digits; it is only called
  with numbers today, but it is public API for NEI/GUI; test it.

### 2.5 S5 — Dead code, duplication, and hygiene

These are safe to remove during the fix pass, but they obscure the real logic:

- `utils/RecipeRecord.java` duplicates `MTRecipeRecord`.
- `client/gui/GuiHandler.java` duplicates `ASPServerProxy`/`ASPClientProxy` and is never registered;
  it has a debug `System.out`.
- `TileEntitySolarPanel`: `created`, `solarType`, `fuel`, `gaugeFuelScaled`, `getMaxEnergyOutput`,
  `getInventoryName` fallback, `getNetworkedFields` (empty), duplicated `facing`; `intialize` typo.
- `TileEntityQGenerator`: `ticker`, `initialized`, `created`, `getInvName`, `getNetworkedFields`
  (empty), duplicated `facing`/`active`.
- `TileEntityMolecularTransformer`: `ticker`, `created`, `prevActiveState`, empty
  `updateVisibility`, `Boolean deactiveTimer`, `lastRecipeNumber` written but not read, unused
  `slots_*` duplicate.
- `TileEntityBase`: `prevActive`, `prevFacing`, network hooks that subclasses override to no-ops.
- `BlockAdvSolarPanel.qgActive`, `BlockMolecularTransformer.getBlockEntity()` (returns null).
- `ItemAdvanced.itemsCount`, `getSubItems(int, CreativeTabs, List)`, repeated
  `setUnlocalizedName` calls.
- `BlockMolecularTransformer.icon` vs the unused `blockIcon`.
- `TIleMolecularTransformerRenderer.rotateBlock` and unused variables (`float_texNudge`,
  `float_reciprocal`, etc.).
- `MTRecipeConfig.doDebug`, `SEPARATOR`, `configVersion` (not used for migration), `defaultLines`
  public mutable.
- `MTRecipeManager.defaultRecipeList`, `RecipeRecord`.
- `ASPPacketHandler.sendToPlayer` and `PacketGUIPressButton.issue(ChunkCoordinates, ...)` are
  unused.
- `AdvancedSolarPanel.CATEGORY_RECIPES`, `CATEGORY_QGENERATOR`, static `config` field, unused
  imports.
- `GuiMolecularTransformer.mc` field shadows `GuiContainer.mc`; `GuiQGenerator.mc` likewise.
- Assets: `textures/blocks/qsp_side_copy.png` is unused; `tile.AdvancedSolarPanel.name` is a dead
  lang key.

---

## 3. IC2 parity analysis

### 3.1 Reference behavior (IC2 2.2.828-experimental)

From the decompiled dependency:

```java
// TileEntitySolarGenerator
public TileEntitySolarGenerator() { super(1, 1, 2); }       // production=1, tier=1, storage=2
public boolean gainEnergy() {
    if (ticker++ % tickRate() == 0) updateSunVisibility();
    if (sunIsVisible) {
        storage += solarbasevalue * (getskylight(world,x,z) / 15.0F);
        return true;
    }
    return false;
}
public static boolean isSunVisible(World w,int x,int y,int z) {
    return getskylight(w,x,z) > 4
        && !w.provider.hasNoSky
        && w.canBlockSeeTheSky(x,y,z)
        && (w.getWorldChunkManager().getBiomeGenAt(x,z) instanceof BiomeGenDesert
            || (!w.isRaining() && !w.isThundering()));
}
private static int getskylight(World w,int x,int z) { return w.getBlockLightValue(x,255,z); }
public int tickRate() { return 128; }
```

From `TileEntityBaseGenerator`:

```java
public double getOfferedEnergy() { return Math.min(storage, power); } // power = 8 << tier*2
public int getSourceTier() { return tier; }                            // 1 for base solar
public void drawEnergy(double amount) { storage -= amount; }
public float getWrenchDropRate() { return 0.9F; }
// chargeSlot: ElectricItem.manager.charge(slot, storage, 1, false, false)
// maxStorage = 2 EU (short), storage is double
```

From `ItemArmorSolarHelmet`:

```java
if (armorInventory[2] != null && TileEntitySolarGenerator.isSunVisible(...)) {
    ElectricItem.manager.charge(armorInventory[2], 1.0, Integer.MAX_VALUE, true, false);
    player.inventoryContainer.detectAndSendChanges();
}
```

### 3.2 Side-by-side comparison

| Property | IC2 base solar | ASP panel | Parity? |
| --- | --- | --- | --- |
| Sun predicate | sky light > 4, has sky, can see sky, desert or no rain/thunder | `isDaytime()` (sky light ≈ 12+), has sky, can see sky; `wetBiome` rainfall > 0 decides rain | **No** |
| Output scaling | `solarBase * skyLight / 15` (double) | Flat `genDay`/`genNight` depending on day/visibility | **No** |
| Night output | 0 | `genNight` (1/8 of day; Quantum 1/2) | Intentional ASP feature, not IC2 |
| Rain output | 0 in non-desert; desert exempt | `genNight` in wet biomes; full `genDay` in zero-rainfall non-desert biomes | **No** |
| Twilight | 0.33–0.73 × day | `genNight` (1/8 day; Quantum 1/2) | **No** |
| Storage | 2 EU, `double` | 32,000 / 100,000 / 1,000,000 / 10,000,000 EU, `int` | Addon design; watch overflow |
| Output cap | `min(storage, powerFromTier(1)=32)`, production 1 | `min(production, storage)`; production 32/128/512/8192 | Different; no tier cap |
| Source tier | 1 | `Integer.MAX_VALUE` | **No** |
| Charge slot | 1 slot, `IElectricItem`-filtered, tier 1 | 4 slots, all items, tier MAX | **No** |
| Wrench drop | 0.9 | 1.0 | Minor |
| Active state | 256-tick smoothed activity meter (`delayActiveUpdate=true`) | Never set; block active texture unused | **No** |
| Tick rate | 128 | 128 | Yes |
| On-load refresh | Yes | Yes (`intialize`), but duplicate load events | Mostly |
| GUI | One slot; localized IC2 tooltip | Four slots; localized title/labels; no tooltips | Partial |
| Block behavior | IC2 machine block/casing conventions | Own block, hardness 3, manual drops | Addon design |
| Mt/other machines | IC2 machine tiers/buffers | QGen/MT MAX tiers, custom buffers | **No** |

### 3.3 Numeric day simulation

Assumptions: clear overworld (no rain/thunder), no storage cap/backpressure, all offered energy
consumed, one full 24,000-tick day, vanilla `WorldProvider` sun-brightness formula. ASP:
`isDaytime()` ticks use `genDay`, all other sky-visible ticks use `genNight`; IC2: sky light > 4
ticks use `skyLight/15` and night is 0.

| Tier | ASP day/night EU/t | ASP EU/day | IC2 1 EU/t panel EU/day | Ratio to 1 IC2 panel | Equivalent IC2 panels by day output | Ratio to that many panels |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| Advanced | 8 / 1 | 115,567 | 14,153.8 | 8.17× | 8 | 1.020× |
| Hybrid | 64 / 8 | 924,536 | 14,153.8 | 65.32× | 64 | 1.020× |
| Ultimate | 512 / 64 | 7,396,288 | 14,153.8 | 522.57× | 512 | 1.020× |
| Quantum | 4096 / 2048 | 75,941,888 | 14,153.8 | 5,365.48× | 4096 | 1.310× |

Sky-light distribution used: 15 for 12,081 ticks; 12–14 for ~1,000 ticks; 5–11 for ~2,276 ticks;
4 for 8,661 ticks. ASP “daytime” ticks are 13,081 and “other” 10,919 in this model.

**Interpretation:**

- Advanced/Hybrid/Ultimate produce about **2% more total EU/day** than the same number of full-time
  IC2 panels, but the distribution is wrong: full output only while `isDaytime()` is true, night
  output when IC2 has none, and `genNight` instead of a ramp at twilight.
- Quantum produces about **31% more** than 4096 IC2 panels because its night rate is half of its day
  rate rather than 1/8.
- During rain in a wet biome, ASP produces `genNight` while IC2 produces **0** (non-desert). During
  rain in a zero-rainfall non-desert biome, ASP can produce full `genDay` while IC2 produces 0.
- The total-EU table hides the most visible difference: at sunrise/sunset and in weather, the panel
  output jumps between 0, `genNight`, and `genDay` instead of following light.

### 3.4 What “functional parity” should mean for this addon

For an addon machine, parity does not have to mean identical output values — ASP is intentionally
stronger and has night generation, four charge slots, and helmets. It should mean the *rules* are
recognizable:

- sky/height/weather/biome predicate;
- light-scaled output (or a clearly documented alternative);
- tier-appropriate source and charge tiers;
- proper energy-net lifecycle and no MAX-tier hacks;
- wrench/charge/harvest behavior consistent with IC2;
- active state and GUI information that match the actual machine state.

A config switch is the safest way to preserve upstream behavior while offering parity:
`general.useIC2SolarRules=false` (default) and `general.nightGeneration=true` (default) with a
`/config` comment explaining the difference. That way existing packs keep 8/1 night behavior, and
parity packs can set `true/false`.

---

## 4. Features suggested to add

### P0 — Correctness and compatibility (do before new content)

1. **Build fix:** NEI compile scope (`devOnlyNonPublishable` or `compileOnly` + the existing
   `runtimeOnlyNonPublishable`; §2.1/B1).
2. **MT Core reflector fix** (§2.1/B2), then optionally ask HodgePodge to set
   `B:fixMTCoreRecipe=false` or update its target.
3. **Thread-safe packets + MT GUI safety** (§2.1/B3–B5).
4. **Recipe validation and buffer accounting** (§2.1/B6, B10).
5. **Metadata/README/license/modid corrections** (§2.1/B7, B11).
6. **A typed, validated config** (ranges, comments, migration): generation, storage, output, charge
   tier, source tier, wrench parity, MT sink tier, QGen caps, recipe toggles. Replace the 12
   private booleans with a small `RecipeConfig`/`BalanceConfig`.
7. **IC2-parity option** for panels and helmets:
   - `useIC2SolarRules` (sky light >4, linear scaling, desert/rain predicate, double storage);
   - `nightGeneration` (default true; false for strict IC2);
   - `chargeTier`/`chargeTarget` for panels/helmets;
   - `sourceTierMode = auto|fixed` and validate output vs tier.
8. **API compatibility:** expose `IMTRecipeManager` methods for `add`, `remove`, `clear`, `get`
   (immutable view), initialize `MTAPI.manager` in preInit, and document load order. Add deprecated
   `advsolar.*` shims for GT5/NHCoreMod if GTNH compatibility is a goal.
9. **A real test suite** (§7) and CI `./gradlew test`.

### P1 — Gameplay/UI features

10. **Panel tooltips/WAILA/TOP:** day/night output, current generation, storage/max, tier, charge
    tier, sun/rain/blocked reason, wrench hint.
11. **Panel GUI:** show tier, charge tier, weather/blocked reason; add an energy-bar tooltip; add a
    “night generation” indicator; localize inventory name.
12. **QGen overhaul:** max-packet semantics (or remove the field), GUI fields/buttons for
    production and packet size, redstone mode (disable vs enable), bounds/clamps, NBT defaults,
    permission/distance checks, localized labels.
13. **MT feature completion:** sync input/output/energy to the client without recipe-index
    coupling; show “output blocked”; persist and cap the energy buffer; correct sink tier; add
    comparator output/redstone control; expose a recipe API for CraftTweaker/GroovyScript; add
    NEI overlay identifier/catalyst and localized labels.
14. **Helmet parity/config:** per-stack state (fix multiplayer), chest/armor/inventory target,
    tier/limit behavior, self-charge exclusion, tooltip (charge rate, tier, air supply), inventory
    sync, optional confusion mechanic for IC2 parity (off by default).
15. **Containers:** filtered slots, correct shift-click ranges, `addCraftingToCrafters`, sync only on
    change, 8-block interaction distance.
16. **Recipes:** OreDict variants for all inputs/outputs (`solarPanel`, `transformerLV/MV/HV`,
    `plateIron`, `ingotIridium`, `circuitAdvanced`, etc.), null-safe `IC2Items` lookups, recipe-mode
    enum, consistent disable flags (especially the unconditional reverse hybrid/ultimate recipes),
    optional duplicate-recipe cleanup, and a `FMLLoadCompleteEvent` pass for cross-mod inputs.
17. **NEI/GTNH integrations:** fix the CSV FQCN (config-only or shim), add `getOverlayIdentifier`,
    localize recipe text, register the handler only when NEI is loaded. Add WAILA/TOP providers and
    a JourneyMap/GTNH web-map check.
18. **Advancements/guide:** a small `Advancement` tree or in-game guide entry for tiers, helmets,
    MT recipes, and the QGen admin tool.

### P2 — Polish and performance

19. Remove dead code listed in §2.5; unify `facing`/`active` state; use `markDirty` consistently.
20. Cache renderer colors/texture sizes; avoid per-frame `new Color`/`Arrays.asList`.
21. Add sound (IC2 metal block), harvest level, resistance, and proper block names.
22. Add a `CHANGELOG.md`, update `NAMESPACE_CHANGE.md`, and replace the README with an ASP-specific
    one (features, config, recipes, MT file format, QGen admin note, compatibility, building,
    license/credits).
23. Optional: a “balance mode” preset (ASP classic vs IC2 parity vs GTNH) to avoid a wall of
    individual config keys.

---

## 5. Documentation, metadata, and naming

### 5.1 README

`README.md` was originally the README for **CompactSolars-Legacy** (LV/MV/HV arrays,
`config/CompactSolars.cfg`, unified tile entities, wrench parity). It has been rewritten for ASP and
now documents:

- the four panel tiers (day/night/rain output, storage, max output, four charge slots);
- the three helmets (generation, charge capacity, tier, air supply);
- the Molecular Transformer and its recipe-file format;
- the admin-only Quantum Generator and redstone behavior;
- the main config sections and recipe modes;
- build/test commands and compatibility notes.

Keep it in sync with the code and with `CODEBASE_ANALYSIS.md`.

### 5.2 Mod metadata

| Source | Value | Problem |
| --- | --- | --- |
| `@Mod(modid=...)` | `AdvancedSolarPanel` | Must stay; registry names and worlds depend on it |
| `gradle.properties modId` | `AdvancedSolarPanel` | Matches `@Mod` after the first-batch fix |
| `mcmod.info modid` | `${modId}` → `AdvancedSolarPanel` | Matches after the first-batch fix |
| `@Mod(version=...)` | `Tags.VERSION` | Git-derived version; existing worlds log a version-change warning |
| `@Mod(name=...)` | `Advanced Solar Panels` | `modName` now matches the display name |
| `@Mod(dependencies=...)` | `required-after:IC2; after:gregtech_addon; after:AppliedEnergistics;` | Trailing semicolon; optional deps not in `mcmod.info` |
| `mcmod.info` authors | MrFuzzihead, SeNtiMeL, Icedfire, JarviceOne | Good; keep |
| `mcmod.info` license | none | Add the resolved license/notice |

**Do not change `modid`** to lowercase. Change `gradle.properties`/metadata to match the code, or
document the toolchain exception.

### 5.3 License

- The 1.7.10 unofficial jar’s CurseForge page says **All Rights Reserved**.
- This repository’s `LICENSE` is MIT and the imported source has no headers.
- The sibling CompactSolars project is GPLv3, but that does **not** apply to Advanced Solar Panels.
- Distribution of a decompiled All-Rights-Reserved jar as MIT is a legal risk. Resolve permission or
  adopt the correct license before publishing; restore upstream notices and attribution.

### 5.4 Language/resource audit

- `en_US.lang` and `ru_RU.lang` both have 33 keys and the same key set (good).
- `tile.AdvancedSolarPanel.name=New Solar Panel` is dead; either set block unlocalized names or
  remove it.
- `itemIrradiantGlassPane.name` exists, but `AdvancedSolarPanel` calls
  `setUnlocalizedName("itemIrradiantGlassPlane")` (Plane vs Pane); the `ItemAdvanced` name list uses
  the correct “Pane”, so display works, but the base item name is misleading.
- Every icon string has a matching texture except the MT block icon path (`blockIcon` is null; see
  §2.1/B7-related). `qsp_side_copy.png` is unused.
- `GUIQuantumGenerator.png` is 190×149 opaque content but the container draws slots down to y=186;
  see B21.

---

## 6. `NAMESPACE_CHANGE.md` review

### 6.1 What the report already covered (and is now done or still pending)

| Item | Report | Current status |
| --- | --- | --- |
| Package rename plan `advsolar → com.mrfuzzihead.advancedsolarpanels` | Yes | **Done** in source: `grep -rn advsolar src` is empty; `@SidedProxy` and `ASPItemAPI` strings point to the new package |
| World/save safety (literal TE IDs, `AdvancedSolarPanel:*` registry names) | Yes | Confirmed; do not change modid |
| HodgePodge `fixMTCoreRecipe` mixin | Yes | **Still a problem**: after the rename the target is gone, mixin skipped, MT Core bug unfixed |
| NEI `handlers.csv`/`catalysts.csv` FQCN | Yes | **Still pending**; config-only workaround documented |
| GTNH web map `cls=` comment | Yes | No impact; optional refresh |
| EnderIO false positive | Yes | No impact |
| AmunRa/gravisuiteneo modid refs | Yes | No impact |
| AE2 facades/JourneyMap registry names | Yes | No impact |
| `modGroup` metadata | Yes | `com.mrfuzzihead.advancedsolarpanels` matches |
| `modId` must stay `AdvancedSolarPanel` | Yes | **Fixed in the first batch**: `gradle.properties` now uses `AdvancedSolarPanel` |
| `grep -rn '"advsolar' --include=*.java src` authoritative | Yes | Now returns zero; keep as a regression test |

### 6.2 What the report missed (or became true during the rename)

1. **GT5-Unofficial compile-time dependency.** GitHub shows
   `import advsolar.utils.MTRecipeManager;` and `MTRecipeManager.transformerRecipes.clear();` in
   `gtPlusPlus/xmod/gregtech/loaders/recipe/RecipeLoaderMolecularTransformer.java` (and several
   forks/mirrors). This is not the `advsolar.api.*` package the report scanned for; it is an internal
   class. If a GT5/GTNH pack is a target, the package rename breaks that class load unless a shim or
   upstream patch is added.
2. **NewHorizonsCoreMod coremod transformer.** `AdvancedSolarPanelTransformer.targetedClasses()`
   returns `advsolar.common.AdvancedSolarPanel`; it injects
   `advsolar/utils/MTRecipeManager` and `advsolar/api/MTAPI` references. After the rename it silently
   matches nothing, so its recipe cleanup does not run. This is not a crash, but it is a behavioral
   change for GTNH.
3. **HodgePodge migration.** The report recommends porting the reflector fix; that is still the
   right fix, and it is now required because the mixin target no longer exists. A compatibility
   subclass shim does not work for the same reason the report gives.
4. **NEI CSV has two consumers:** `handlers.csv` (tab info/catalyst/order) and `catalysts.csv`
   (catalyst mapping). The report covers this; add the new FQCN or a shim.
5. **Metadata mismatch:** the report’s checklist says `modId = AdvancedSolarPanel`; this was fixed
   in the first batch (`gradle.properties` now matches `@Mod`).
6. **`@SidedProxy`/`ASPItemAPI`/all imports are updated**, so the only remaining in-mod string
   references are the new package; good.
7. **External scan scope:** the report’s scan of the FuzzCraft pack found no `advsolar.utils`; that
   is correct for that pack, but the project is used in GTNH packs where GT5/NHCoreMod exist. Add a
   note that the scan is pack-specific.

### 6.3 Updated namespace checklist

- [x] Move source directories and package declarations to
  `com.mrfuzzihead.advancedsolarpanels.*`.
- [x] Update `@SidedProxy` strings.
- [x] Update `ASPItemAPI` `Class.forName`.
- [x] Fix `gradle.properties modId` to `AdvancedSolarPanel` (or document why it differs) and verify
  `mcmod.info` substitution.
- [ ] Port the MT Core reflector fix (§2.1/B2).
- [ ] Decide whether to ship `advsolar.*` compatibility shims for GT5/NHCoreMod (or patch those
  projects / document the break).
- [ ] Update NEI CSV FQCN or ship a compat `advsolar.client.nei.MTRecipeHandler` shim.
- [ ] Leave HodgePodge enabled; expect one WARN. Optionally set `B:fixMTCoreRecipe=false` after the
  ASP fix.
- [ ] Optional Dynmap `cls=` comment refresh.
- [ ] Keep `modid = AdvancedSolarPanel`; do not change registry names/TE IDs.

---

## 7. Unit test plan

### 7.1 Strategy

The code is heavily coupled to Minecraft/IC2/FML statics, so “full coverage outside the game” needs
two layers:

1. **Pure JUnit 5 tests** for extracted math/parsers/state machines. These run in CI with no game
   bootstrap and are the bulk of the coverage.
2. **MC-class smoke tests** using bare `new Item()`/`new ItemStack(...)`/block/TE instances (the
   sibling AdvancedMachines tests prove this works without registries), plus Mockito for
   `World`/`IBlockAccess`/`EntityPlayer`/`IElectricItem` where necessary.
3. **Resource/metadata tests** that read `src/main/resources` and the project files (lang keys,
   textures, modid consistency, no `advsolar` strings).
4. **Integration/manual checks** for rendering, GUI layout, NEI overlays, energy-net behavior, and
   multiplayer; these cannot be fully unit tested without a game.

### 7.2 Refactoring seams to add first

These are small, behavior-preserving extractions that make the rest testable:

| Seam | From | Why |
| --- | --- | --- |
| `SolarMath` | `TileEntitySolarPanel.updateVisibility/gainFuel`, helmet copy | IC2 predicate/light scaling in one testable place; mirror the sibling CompactSolars `SolarMath` |
| `SolarTier` enum/record | Tier constructors, `ItemAdvancedSolarPanel`, `BlockAdvSolarPanel`, recipes | metadata clamping, names, output/storage/tier, source/charge tiers |
| `EnergyBuffer` | panel storage/charge + MT `energyBuffer` | clamp/overflow/fractional EU tests |
| `MtRecipeParser` (`RecipeSpec`) | `MTRecipeConfig.parse/wrap` | pure line parsing without registries/logging; test zero/negative/malformed |
| `MtProcessing` state machine | `TileEntityMolecularTransformer.updateEntity/gainFuel/canSmelt/injectEnergy` | completion/output-full/energy-buffer tests without a world |
| `QGeneratorSettings`/`QGenLogic` | QGen production/packet/button/redstone | clamp/overflow/inversion tests |
| `HelmetRules` | helmet constructor/gainFuel/charge target | per-tier stats, sun rules, target order |
| `ContainerTransferPolicy` | the three container `transferStackInSlot`s | slot-range/off-by-one tests |
| `PacketIO` static read/write | `Packet*` | DataStream round-trip without FML channels |
| `ModMetadata`/`LangKeys` checks | resources | detect missing assets/keys/strings |

Keep the TE classes as thin adapters over these helpers, so the NBT/GUI/network code remains in the
TE but the decisions are pure.

### 7.3 Test dependencies

Current `dependencies.gradle` already has JUnit 5. Add:

```groovy
testImplementation("org.mockito:mockito-inline:4.11.0") // Java 8; mock final ItemStack/World if needed
// NEI only if tests instantiate NEI classes (use the standard Gradle test configurations):
testCompileOnly("com.github.GTNewHorizons:NotEnoughItems:2.8.162-GTNH:dev")
testRuntimeOnly("com.github.GTNewHorizons:NotEnoughItems:2.8.162-GTNH:dev")
```

(Mockito 5+ requires a newer runtime; use Mockito 4.11 unless the test toolchain is known to be 17+.)
`junit-jupiter` includes params. Netty is already on the Minecraft test classpath; use
`EmbeddedChannel` for codec tests if desired.

### 7.4 Test matrix

| Test class | Target | Key cases | Type | Priority |
| --- | --- | --- | --- | --- |
| `SolarMathTest` | extracted `SolarMath` | skyLight 0–15; `>4` threshold; hasSky/canSeeSky; desert/rain/thunder; linear 5/12/15; base 0/negative/NaN | pure | P0 |
| `SolarGenerationTest` | `SolarMath` + tier data | day vs night vs twilight; ASP night mode; `genDay/genNight`; blocked sky; no-sky world; rain in wet/dry/desert; stale refresh boundary | pure | P0 |
| `SolarTierTest` | `SolarTier` | metadata 0..4; invalid meta clamp; names; day/night/storage/output; source/charge tier; output ≤ tier power; legacy names | pure | P0 |
| `SolarConfigTest` | config sanitizers | storage 0/negative → clamp; output 0/negative; generation negative; max int overflow; NaN/double base; recipe-mode booleans | pure | P0 |
| `PanelEnergyTest` | `EnergyBuffer`/TE helpers | add at cap, overflow, partial charge, `getOfferedEnergy`, `drawEnergy` clamp, gauge scale/0, NBT clamp | pure/MC | P1 |
| `MtRecipeParserTest` | `MtRecipeParser` | valid comments; inline comments; version; blank/BOM; oredict; meta; stack size 0/negative/>64; energy 0/negative/overflow; missing mod; multi-output; CRLF; malformed `:`; line numbers | pure/temp files | P0 |
| `MtRecipeManagerTest` | `MTRecipeManager` | add copies input/output (no alias); rejects null/zero/negative; oredict expansion; duplicate detection; `clear/get` API; init with null/odd config path | pure/MC | P1 |
| `MtProcessingTest` | `MtProcessing` | canSmelt exact/NBT mismatch; input/output stack sizes; output full/incompatible; completion; progress 0/50/100; zero/negative energy; buffer cap; demand/injection remainder; output-blocked behavior | pure | P0 |
| `MtNbtTest` | MT NBT helper | round-trip inventory/recipe/progress; energy buffer; legacy missing keys; invalid recipe index; stack size clamp | pure/MC | P1 |
| `QGenLogicTest` | `QGenLogic` | production/packet clamps; overflow; button deltas (1–12, 101–112); redstone disable/enable; `getOfferedEnergy` cap; NBT defaults | pure/MC | P1 |
| `HelmetRulesTest` | `HelmetRules` | per-tier maxCharge/transfer/tier/energyPerDamage/ratio; day/night/rain; charge target order; self-charge exclusion; air supply thresholds | pure/MC | P1 |
| `ContainerTransferTest` | `ContainerTransferPolicy` | panel: 0–3 ↔ 4–39; main 4–30 ↔ hotbar 31–39; slot 39; MT: 0–1 ↔ 2–28 ↔ 29–37; QGen no-op; merge full/partial | pure | P2 |
| `ItemStackUtilTest` | `ItemStackUtil` | nulls; same item/meta; different; wildcard 32767; null item; NBT ignored | pure/MC | P2 |
| `ParsingNumberTest` | `GuiMolecularTransformer.parsingNumber` | 0/999/1000/1234567; negative; empty; non-digit (current method assumes digits); long strings | pure | P2 |
| `SlotRulesTest` | `MySlot` | true/false/null Boolean; stack filters | pure | P2 |
| `PanelMetadataTest` | `BlockAdvSolarPanel`/`ItemAdvSolarPanel`/`ItemAdvanced` | `createNewTileEntity` 0..4; invalid meta; names for 0..N; invalid name/icon fallback; sub-block count | MC smoke | P1 |
| `BlockIconTest` | icon registration | every `registerIcon` path exists; `getIcon` fallback non-null; MT `blockIcon` set; no NPE when TE null (mock `IBlockAccess`) | MC smoke | P2 |
| `PacketIoTest` | `PacketChangeState`/`PacketGUIPressButton` | write/read round-trip; -1 NBT; max/zero length; dimension field; button IDs; truncated stream | pure | P1 |
| `ChannelCodecTest` | `ASPChannelHandler` | encode/decode; discriminator mapping; malformed payload; `execute` scheduling (assert enqueued, not run inline) | Netty/pure | P2 |
| `LangResourceTest` | lang files | en/ru key sets equal; every item/block/helmet name has a key; no dead `tile.*`; GUI keys present | resource | P1 |
| `TextureResourceTest` | assets | every icon string maps to a PNG; GUI/model/armor textures exist; no unused PNG (warning) | resource | P2 |
| `NamespaceTest` | source tree | no `advsolar` strings; every `.java` package matches its path; `@SidedProxy`/`Class.forName` classes exist; `@Mod.modid` == `gradle.properties modId`; TE IDs unchanged | resource/static | P0 |
| `ItemStackSmokeTest` | MC classes | `new Item()`, `new ItemStack(...)`, block/TE construction; no registry needed | MC smoke | P1 |

### 7.5 Concrete test sketches

**`SolarMathTest` (mirror the sibling CompactSolars project):**

```java
class SolarMathTest {
    @Test void ic2SunPredicate() {
        assertFalse(SolarMath.isSunVisible(4, false, true, false, false, false));
        assertTrue (SolarMath.isSunVisible(5, false, true, false, false, false));
        assertFalse(SolarMath.isSunVisible(15, true, true, false, false, false));
        assertFalse(SolarMath.isSunVisible(15, false, false, false, false, false));
        assertFalse(SolarMath.isSunVisible(15, false, true, true, false, false)); // rain
        assertTrue (SolarMath.isSunVisible(15, false, true, true, false, true));  // desert
    }

    @Test void linearOutput() {
        assertEquals(0.0, SolarMath.perPanelOutput(4, 1.0), 1e-9);
        assertEquals(1.0 / 3.0, SolarMath.perPanelOutput(5, 1.0), 1e-9);
        assertEquals(0.8, SolarMath.perPanelOutput(12, 1.0), 1e-9);
        assertEquals(1.0, SolarMath.perPanelOutput(15, 1.0), 1e-9);
    }
}
```

**`MtRecipeParserTest`:**

```java
@ParameterizedTest
@CsvSource({
    "minecraft:iron_ingot:1; minecraft:gold_ingot:1; 5000, 1",
    "oredict:ingotIron:2; oredict:ingotGold:1; 0, 2",       // expansion count depends on ore dict
    "oredict:ingotIron:0; minecraft:gold_ingot:1; 5000, 0", // must be rejected
    "minecraft:iron_ingot:1; minecraft:gold_ingot:1; -5, 0",
    "minecraft:iron_ingot:1; minecraft:gold_ingot:1; nope, 0",
    "# comment, 0"
})
void parsesSafely(String line, int expected) { ... }
```

**`MtProcessingTest`:**

```java
@Test void completesAndEmitsOutput() {
    MtProcessing p = new MtProcessing();
    p.start(recipe(input(1), output(1), 100));
    assertEquals(40, p.inject(40));   // accepted
    assertEquals(0, p.tick());        // 40 used
    assertEquals(60, p.inject(60));   // accepted
    p.tick();
    assertTrue(p.hasOutput());
    assertEquals(0, p.inject(1));     // rejected while output full
}
```

**`PacketIoTest`:**

```java
@Test void roundTripsButtonPacket() throws Throwable {
    PacketGUIPressButton in = new PacketGUIPressButton();
    in.dimID = 0; in.x = 1; in.y = 2; in.z = 3; in.buttonID = 105;
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    in.write(new DataOutputStream(out));
    PacketGUIPressButton decoded = new PacketGUIPressButton();
    decoded.read(new DataInputStream(new ByteArrayInputStream(out.toByteArray())));
    assertEquals(105, decoded.buttonID);
}
```

**`NamespaceTest`:**

```java
@Test void noLegacyPackageStrings() throws IOException {
    Files.walk(Path.of("src/main/java"))
        .filter(p -> p.toString().endsWith(".java"))
        .forEach(p -> assertFalse(read(p).contains("advsolar"), p.toString()));
}

@Test void modIdMatchesGradle() throws IOException {
    String gradle = Files.readString(Path.of("gradle.properties"));
    assertTrue(gradle.contains("modId = AdvancedSolarPanel"));
}
```

**`LangResourceTest`:**

```java
@Test void enAndRuHaveSameKeys() {
    assertEquals(readKeys("en_US.lang"), readKeys("ru_RU.lang"));
}

@Test void everyItemNameHasALangKey() {
    for (var name : ItemNames.ALL) assertTrue(enKeys.contains(name + ".name"));
}
```

### 7.6 Coverage targets

- **100%** of extracted pure helpers (`SolarMath`, `SolarTier`, parser, clamps, `parsingNumber`,
  `ItemStackUtil`, transfer policies).
- **≥80% branch** on `MtProcessing`, `EnergyBuffer`, `QGenLogic`, `HelmetRules`.
- **Round-trip** all NBT and packet serializers.
- **Resource tests** for every icon/lang/name/metadata entry.
- **Not unit-testable (manual/in-game):** actual rendering, TESR animation, GUI layout, NEI overlay,
  game-thread scheduling, energy-net behavior with cables/sinks, multiplayer state, world save
  migration, recipe registration conflicts. Use a `runClient`/`runServer` smoke checklist and, if
  desired, a GTNH-style integration test mod that boots a server world and asserts energy flow.

### 7.7 Commands

```bash
./gradlew compileJava --offline     # compiles (NEI is now on the compile classpath)
./gradlew test                      # after fixing compile + adding tests
./gradlew test --tests '*Solar*'    # focused
./gradlew runClient                 # manual GUI/render/multiplayer checks
./gradlew runServer                 # dedicated-server smoke
```

Add a CI matrix step for `./gradlew test`; the existing `.github/workflows/build-and-test.yml`
already delegates to the GTNH reusable workflow.

---

## 8. Recommended implementation order

### Phase 0 — Make it shippable

1. Fix the NEI dependency scope (B1).
2. Fix the MT Core reflector recipe (B2).
3. Fix packet thread dispatch, remove `Thread.getAllStackTraces`, guard the MT GUI recipe index,
   and fix the static caption leak (B3–B5).
4. Validate MT recipes/energy and cap/persist the buffer (B6, B10).
5. Fix metadata/div0/NPE crashes (B7, B8).
6. Correct `gradle.properties`/`mcmod.info`/README/license; use `Tags.VERSION` (B11, §5).
7. Add the baseline pure tests and `NamespaceTest`/`LangResourceTest`.

### Phase 1 — IC2 parity and gameplay

8. Add `SolarMath` + `SolarTier`; add `useIC2SolarRules`/`nightGeneration` config.
9. Replace MAX source/charge/sink tiers with real tiers; validate output vs tier.
10. Fix QGen max-packet/redstone/GUI/bounds; add a real packet-size UI or remove the feature.
11. Fix helmet per-stack state, charge target/tier, inventory sync, tooltip.
12. Fix MT output-blocked demand/buffer/NBT, sink tier, automation extract, and GUI sync.
13. Fix container ranges/filters/sync frequency and QGen GUI dimensions.

### Phase 2 — Polish, docs, tests, integrations

14. Tooltips, WAILA/TOP, NEI overlay/catalyst/localization, block sounds/harvest.
15. Recipe OreDict coverage, consistent flags, null-safe IC2 lookups, `FMLLoadCompleteEvent`.
16. API `add/remove/clear/get`, preInit manager, GT5/NHCoreMod shims or documented break.
17. Remove dead code, cache renderer allocations, update README/NAMESPACE/CHANGELOG.
18. Expand test suite and CI; run `runClient`/`runServer` smoke tests.

---

## 9. Appendix: evidence and references

### 9.1 Build check

Before the first batch:

```text
$ ./gradlew compileJava --offline
...
C:\...\client\nei\MTRecipeHandler.java:7: error: package codechicken.lib.gui does not exist
C:\...\client\nei\MTRecipeHandler.java:8: error: package codechicken.nei does not exist
...
27 errors
FAILURE: Build failed with an exception.
```

After the first batch:

```text
$ ./gradlew build --offline
...
BUILD SUCCESSFUL
```

The test baseline currently has 44 passing tests across ten classes:
`SolarPanelGenerationTest`, `SolarTierTest`, `SolarMathTest`, `HelmetRulesTest`, `HelmetItemTest`,
`MTRecipeValidationTest`, `MolecularTransformerLogicTest`, `NetworkTaskTest`, `SlotElectricItemTest`, and
`NamespaceResourceTest`.

### 9.2 Upstream/IC2 files decompiled for this review

- `ic2/core/block/generator/tileentity/TileEntitySolarGenerator.java`
- `ic2/core/block/generator/tileentity/TileEntityBaseGenerator.java`
- `ic2/core/block/generator/container/ContainerSolarGenerator.java`
- `ic2/core/item/armor/ItemArmorSolarHelmet.java`
- `ic2/core/energy/EnergyNetLocal.java`, `EnergyNetGlobal.java`, `Grid.java`
- `ic2/core/block/TileEntityBlock.java`, `BlockMultiID.java`

### 9.3 External code-search evidence (outside the target pack)

- `GTNewHorizons/GT5-Unofficial`, `gtPlusPlus/xmod/gregtech/loaders/recipe/RecipeLoaderMolecularTransformer.java`:
  `import advsolar.utils.MTRecipeManager;`, `MTRecipeManager.transformerRecipes.clear();`
- `GTNewHorizons/NewHorizonsCoreMod`, `AdvancedSolarPanelTransformer.java`:
  `targetedClasses() { return new String[] { "advsolar.common.AdvancedSolarPanel" }; }` and injects
  `advsolar/utils/MTRecipeManager`, `advsolar/api/MTAPI`.
- `GTNewHorizons/Hodgepodge`, `MixinAdvancedSolarPanel.java`: targets
  `advsolar.common.AdvancedSolarPanel`; the report quotes the mixin.
- The target FuzzCraft pack’s scan found no `advsolar.utils` references; GT5/NHCoreMod are not in
  that pack.

### 9.4 Public bug reports matching the code findings

- “Advanced Solar Panels — Molecular Transformer Crashing Client” (client crashes while the server
  keeps processing): consistent with the off-thread packet execution and the client recipe-index
  coupling.
- MT Core reflector durability 10000 vs 10001: consistent with the shared IC2 stack mutation and
  HodgePodge fix.
- Quantum Solar Panel “no cable can transfer the output” / over-tier packet: consistent with the
  MAX source tier and the unimplemented `maxPacketSize`.

### 9.5 Assumptions and limits

- The numeric day simulation assumes clear weather, no storage cap, and all offered energy consumed;
  it is a parity illustration, not a gameplay measurement.
- IC2 behavior is from the exact `2.2.828-experimental` jar in this build; other IC2 forks/versions
  may differ.
- The production pack scan in `NAMESPACE_CHANGE.md` is pack-specific; GTNH ecosystem consumers were
  checked separately via GitHub and may not matter if this fork targets only FuzzCraft.
- No rendering/profiling was performed; renderer/GUI findings are static and should be confirmed in
  `runClient` after the build is fixed.
