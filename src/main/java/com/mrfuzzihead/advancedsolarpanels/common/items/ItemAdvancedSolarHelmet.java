package com.mrfuzzihead.advancedsolarpanels.common.items;

import java.util.List;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.ISpecialArmor;

import com.mrfuzzihead.advancedsolarpanels.AdvancedSolarPanel;
import com.mrfuzzihead.advancedsolarpanels.utils.HelmetRules;
import com.mrfuzzihead.advancedsolarpanels.utils.SolarMath;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ic2.api.item.ElectricItem;
import ic2.api.item.IElectricItem;
import ic2.api.item.IMetalArmor;

public class ItemAdvancedSolarHelmet extends ItemArmor implements IElectricItem, IMetalArmor, ISpecialArmor {

    private double maxCharge;
    private double transferLimit;
    private int tier;
    private int genDay;
    private int solarType;
    private int energyPerDamage;
    private double damageAbsorptionRatio;
    private double baseAbsorptionRatio;

    public ItemAdvancedSolarHelmet(ArmorMaterial par2EnumArmorMaterial, int par3, int par4, int htype) {
        super(par2EnumArmorMaterial, par3, par4);
        this.solarType = htype;
        this.maxCharge = 1_000_000.0D;
        this.transferLimit = 3_000.0D;
        this.tier = HelmetRules.tier(htype);

        if (this.solarType == 1) {
            this.genDay = AdvancedSolarPanel.advGenDay;
            this.maxCharge = 1_000_000.0D;
            this.transferLimit = 3_000.0D;
            this.energyPerDamage = 800;
            this.damageAbsorptionRatio = 0.9D;
            this.baseAbsorptionRatio = 0.15D;
        } else if (this.solarType == 2) {
            this.genDay = AdvancedSolarPanel.hGenDay;
            this.maxCharge = 10_000_000.0D;
            this.transferLimit = 10_000.0D;
            this.energyPerDamage = 2_000;
            this.damageAbsorptionRatio = 1.0D;
            this.baseAbsorptionRatio = 0.15D;
        } else if (this.solarType == 3) {
            this.genDay = AdvancedSolarPanel.uhGenDay;
            this.maxCharge = 10_000_000.0D;
            this.transferLimit = 10_000.0D;
            this.energyPerDamage = 2_000;
            this.damageAbsorptionRatio = 1.0D;
            this.baseAbsorptionRatio = 0.15D;
        }

        this.setCreativeTab(AdvancedSolarPanel.ic2Tab);
        this.setMaxDamage(27);
    }

    @Override
    public void onArmorTick(World worldObj, EntityPlayer player, ItemStack itemStack) {
        if (worldObj.isRemote) {
            return;
        }

        if (HelmetRules.hasAirSupply(this.solarType)) {
            int airLevel = player.getAir();
            if (ElectricItem.manager.canUse(itemStack, 1_000.0D) && airLevel < 100) {
                player.setAir(airLevel + 200);
                ElectricItem.manager.use(itemStack, 1_000.0D, (EntityPlayer) null);
            }
        }

        int xCoord = MathHelper.floor_double(player.posX);
        int yCoord = MathHelper.floor_double(player.posY);
        int zCoord = MathHelper.floor_double(player.posZ);
        double factor = SolarMath.generationFactor(worldObj, xCoord, yCoord, zCoord);
        if (factor <= 0.0D) {
            return;
        }

        double available = this.genDay * factor;
        double used;
        if (AdvancedSolarPanel.solarHelmetLegacyCharging) {
            used = this.chargeLegacy(player, available);
        } else {
            used = this.chargeChestThenHelmet(player, itemStack, available);
        }

        if (used > 0.0D && player.openContainer != null) {
            player.openContainer.detectAndSendChanges();
        }
    }

    /** Original ASP behavior: charge all armor (including the helmet), then the main inventory. */
    private double chargeLegacy(EntityPlayer player, double available) {
        double remaining = available;
        for (ItemStack armor : player.inventory.armorInventory) {
            if (remaining <= 0.0D) {
                break;
            }
            if (armor != null && armor.getItem() instanceof IElectricItem) {
                remaining -= ElectricItem.manager.charge(armor, remaining, 4, false, false);
            }
        }
        for (ItemStack stack : player.inventory.mainInventory) {
            if (remaining <= 0.0D) {
                break;
            }
            if (stack != null && stack.getItem() instanceof IElectricItem) {
                remaining -= ElectricItem.manager.charge(stack, remaining, 4, false, false);
            }
        }
        return available - remaining;
    }

    /** IC2-style behavior with a small ASP extension: chest first, then overflow to the helmet. */
    private double chargeChestThenHelmet(EntityPlayer player, ItemStack helmet, double available) {
        double remaining = available;
        ItemStack chest = player.inventory.armorInventory[HelmetRules.CHEST_ARMOR_SLOT];
        if (chest != null && chest != helmet && chest.getItem() instanceof IElectricItem) {
            remaining -= ElectricItem.manager
                .charge(chest, remaining, HelmetRules.ANY_CHARGE_TIER, HelmetRules.IGNORE_TRANSFER_LIMIT, false);
        }
        if (remaining > 0.0D && helmet.getItem() instanceof IElectricItem) {
            remaining -= ElectricItem.manager
                .charge(helmet, remaining, HelmetRules.ANY_CHARGE_TIER, HelmetRules.IGNORE_TRANSFER_LIMIT, false);
        }
        return available - remaining;
    }

    @Override
    public boolean isMetalArmor(ItemStack itemstack, EntityPlayer player) {
        return true;
    }

    @Override
    public ArmorProperties getProperties(EntityLivingBase player, ItemStack armor, DamageSource source, double damage,
        int slot) {
        if (source.isUnblockable()) {
            return new ArmorProperties(0, 0.0D, 0);
        }
        double absorptionRatio = this.getBaseAbsorptionRatio() * this.getDamageAbsorptionRatio();
        int energyPerDamage = this.getEnergyPerDamage();
        int damageLimit = (int) (energyPerDamage > 0 ? 25.0D * ElectricItem.manager.getCharge(armor) / energyPerDamage
            : 0.0D);
        return new ArmorProperties(0, absorptionRatio, damageLimit);
    }

    @Override
    public int getArmorDisplay(EntityPlayer player, ItemStack armor, int slot) {
        return ElectricItem.manager.getCharge(armor) >= this.getEnergyPerDamage()
            ? (int) Math.round(20.0D * this.getBaseAbsorptionRatio() * this.getDamageAbsorptionRatio())
            : 0;
    }

    @Override
    public void damageArmor(EntityLivingBase entity, ItemStack stack, DamageSource source, int damage, int slot) {
        ElectricItem.manager
            .discharge(stack, damage * this.getEnergyPerDamage(), Integer.MAX_VALUE, true, false, false);
    }

    public int getEnergyPerDamage() {
        return this.energyPerDamage;
    }

    public double getDamageAbsorptionRatio() {
        return this.damageAbsorptionRatio;
    }

    private double getBaseAbsorptionRatio() {
        return this.baseAbsorptionRatio;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void getSubItems(Item item, CreativeTabs var2, List var3) {
        ItemStack charged = new ItemStack(this, 1);
        ElectricItem.manager.charge(charged, 2.147483647E9D, Integer.MAX_VALUE, true, false);
        var3.add(charged);
        var3.add(new ItemStack(this, 1, this.getMaxDamage()));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister iconRegister) {
        if (this.solarType == 1) {
            this.itemIcon = iconRegister.registerIcon("advancedsolarpanel:AdvSolarHelmet");
        } else if (this.solarType == 2) {
            this.itemIcon = iconRegister.registerIcon("advancedsolarpanel:HybridSolarHelmet");
        } else if (this.solarType == 3) {
            this.itemIcon = iconRegister.registerIcon("advancedsolarpanel:UltimateSolarHelmet");
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public String getArmorTexture(ItemStack stack, Entity entity, int slot, String type) {
        if (this.solarType == 1) {
            return "advancedsolarpanel:textures/armor/advancedSolarHelmet.png";
        } else if (this.solarType == 2) {
            return "advancedsolarpanel:textures/armor/hybridSolarHelmet.png";
        } else if (this.solarType == 3) {
            return "advancedsolarpanel:textures/armor/ultimateSolarHelmet.png";
        }
        return "";
    }

    @Override
    @SideOnly(Side.CLIENT)
    public EnumRarity getRarity(ItemStack var1) {
        if (this.solarType == 1) {
            return EnumRarity.uncommon;
        }
        return this.solarType == 2 ? EnumRarity.rare : EnumRarity.epic;
    }

    @Override
    @SuppressWarnings("unchecked")
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack itemstack, EntityPlayer player, List list, boolean advanced) {
        list.add(
            StatCollector.translateToLocal("ic2.item.tooltip.PowerOutput") + " "
                + this.genDay
                + " EU/t "
                + StatCollector.translateToLocal("ic2.item.tooltip.max"));
        list.add(StatCollector.translateToLocal("ic2.item.tooltip.PowerTier") + " " + this.tier);
        String descKey = "item.advancedSolarHelmet.desc";
        String description = StatCollector.translateToLocal(descKey);
        list.add(description.equals(descKey) ? "Charges the chestplate using available sunlight." : description);
        if (HelmetRules.hasAirSupply(this.solarType)) {
            String airKey = "item.advancedSolarHelmet.air";
            String airDescription = StatCollector.translateToLocal(airKey);
            list.add(airDescription.equals(airKey) ? "Also replenishes underwater air." : airDescription);
        }
    }

    @Override
    public boolean canProvideEnergy(ItemStack itemStack) {
        return false;
    }

    @Override
    public Item getChargedItem(ItemStack itemStack) {
        return this;
    }

    @Override
    public Item getEmptyItem(ItemStack itemStack) {
        return this;
    }

    @Override
    public double getMaxCharge(ItemStack itemStack) {
        return this.maxCharge;
    }

    @Override
    public int getTier(ItemStack itemStack) {
        return this.tier;
    }

    @Override
    public double getTransferLimit(ItemStack itemStack) {
        return this.transferLimit;
    }
}
