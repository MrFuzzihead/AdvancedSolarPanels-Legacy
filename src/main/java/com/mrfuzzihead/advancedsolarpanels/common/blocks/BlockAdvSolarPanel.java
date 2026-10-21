package com.mrfuzzihead.advancedsolarpanels.common.blocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import com.mrfuzzihead.advancedsolarpanels.AdvancedSolarPanel;
import com.mrfuzzihead.advancedsolarpanels.client.ASPClientProxy;
import com.mrfuzzihead.advancedsolarpanels.common.tiles.TileEntityAdvancedSolarPanel;
import com.mrfuzzihead.advancedsolarpanels.common.tiles.TileEntityBase;
import com.mrfuzzihead.advancedsolarpanels.common.tiles.TileEntityHybridSolarPanel;
import com.mrfuzzihead.advancedsolarpanels.common.tiles.TileEntityQGenerator;
import com.mrfuzzihead.advancedsolarpanels.common.tiles.TileEntityQuantumSolarPanel;
import com.mrfuzzihead.advancedsolarpanels.common.tiles.TileEntitySolarPanel;
import com.mrfuzzihead.advancedsolarpanels.common.tiles.TileEntityUltimateSolarPanel;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ic2.api.item.IC2Items;

public class BlockAdvSolarPanel extends BlockContainer {

    public static final int MAX_METADATA = 4;
    private static final int MAX_FACING = 5;

    public boolean qgActive;
    private IIcon[][] iconBuffer;

    public BlockAdvSolarPanel() {
        super(Material.iron);
        this.setHardness(3.0F);
        this.setStepSound(Block.soundTypeMetal);
        this.setCreativeTab(AdvancedSolarPanel.ic2Tab);
        this.qgActive = false;
    }

    private static int clampMetadata(int metadata) {
        return metadata >= 0 && metadata <= MAX_METADATA ? metadata : 0;
    }

    private static int clampSide(int side) {
        return side >= 0 && side <= 5 ? side : 0;
    }

    private static int clampFacing(int facing) {
        return facing >= 0 && facing <= MAX_FACING ? facing : 0;
    }

    public void registerBlockIcons(IIconRegister par1IconRegister) {
        this.iconBuffer = new IIcon[5][12];
        this.iconBuffer[0][0] = par1IconRegister.registerIcon("advancedsolarpanel:asp_bottom");
        this.iconBuffer[0][1] = par1IconRegister.registerIcon("advancedsolarpanel:asp_top");
        this.iconBuffer[0][2] = par1IconRegister.registerIcon("advancedsolarpanel:asp_side");
        this.iconBuffer[0][3] = par1IconRegister.registerIcon("advancedsolarpanel:asp_side");
        this.iconBuffer[0][4] = par1IconRegister.registerIcon("advancedsolarpanel:asp_side");
        this.iconBuffer[0][5] = par1IconRegister.registerIcon("advancedsolarpanel:asp_side");
        this.iconBuffer[0][6] = par1IconRegister.registerIcon("advancedsolarpanel:asp_bottom");
        this.iconBuffer[0][7] = par1IconRegister.registerIcon("advancedsolarpanel:asp_top");
        this.iconBuffer[0][8] = par1IconRegister.registerIcon("advancedsolarpanel:asp_side");
        this.iconBuffer[0][9] = par1IconRegister.registerIcon("advancedsolarpanel:asp_side");
        this.iconBuffer[0][10] = par1IconRegister.registerIcon("advancedsolarpanel:asp_side");
        this.iconBuffer[0][11] = par1IconRegister.registerIcon("advancedsolarpanel:asp_side");
        this.iconBuffer[1][0] = par1IconRegister.registerIcon("advancedsolarpanel:hsp_bottom");
        this.iconBuffer[1][1] = par1IconRegister.registerIcon("advancedsolarpanel:hsp_top");
        this.iconBuffer[1][2] = par1IconRegister.registerIcon("advancedsolarpanel:hsp_side");
        this.iconBuffer[1][3] = par1IconRegister.registerIcon("advancedsolarpanel:hsp_side");
        this.iconBuffer[1][4] = par1IconRegister.registerIcon("advancedsolarpanel:hsp_side");
        this.iconBuffer[1][5] = par1IconRegister.registerIcon("advancedsolarpanel:hsp_side");
        this.iconBuffer[1][6] = par1IconRegister.registerIcon("advancedsolarpanel:hsp_bottom");
        this.iconBuffer[1][7] = par1IconRegister.registerIcon("advancedsolarpanel:hsp_top");
        this.iconBuffer[1][8] = par1IconRegister.registerIcon("advancedsolarpanel:hsp_side");
        this.iconBuffer[1][9] = par1IconRegister.registerIcon("advancedsolarpanel:hsp_side");
        this.iconBuffer[1][10] = par1IconRegister.registerIcon("advancedsolarpanel:hsp_side");
        this.iconBuffer[1][11] = par1IconRegister.registerIcon("advancedsolarpanel:hsp_side");
        this.iconBuffer[2][0] = par1IconRegister.registerIcon("advancedsolarpanel:usp_bottom");
        this.iconBuffer[2][1] = par1IconRegister.registerIcon("advancedsolarpanel:usp_top");
        this.iconBuffer[2][2] = par1IconRegister.registerIcon("advancedsolarpanel:usp_side");
        this.iconBuffer[2][3] = par1IconRegister.registerIcon("advancedsolarpanel:usp_side");
        this.iconBuffer[2][4] = par1IconRegister.registerIcon("advancedsolarpanel:usp_side");
        this.iconBuffer[2][5] = par1IconRegister.registerIcon("advancedsolarpanel:usp_side");
        this.iconBuffer[2][6] = par1IconRegister.registerIcon("advancedsolarpanel:usp_bottom");
        this.iconBuffer[2][7] = par1IconRegister.registerIcon("advancedsolarpanel:usp_top");
        this.iconBuffer[2][8] = par1IconRegister.registerIcon("advancedsolarpanel:usp_side");
        this.iconBuffer[2][9] = par1IconRegister.registerIcon("advancedsolarpanel:usp_side");
        this.iconBuffer[2][10] = par1IconRegister.registerIcon("advancedsolarpanel:usp_side");
        this.iconBuffer[2][11] = par1IconRegister.registerIcon("advancedsolarpanel:usp_side");
        this.iconBuffer[3][0] = par1IconRegister.registerIcon("advancedsolarpanel:qsp_bottom");
        this.iconBuffer[3][1] = par1IconRegister.registerIcon("advancedsolarpanel:qsp_top");
        this.iconBuffer[3][2] = par1IconRegister.registerIcon("advancedsolarpanel:qsp_side");
        this.iconBuffer[3][3] = par1IconRegister.registerIcon("advancedsolarpanel:qsp_side");
        this.iconBuffer[3][4] = par1IconRegister.registerIcon("advancedsolarpanel:qsp_side");
        this.iconBuffer[3][5] = par1IconRegister.registerIcon("advancedsolarpanel:qsp_side");
        this.iconBuffer[3][6] = par1IconRegister.registerIcon("advancedsolarpanel:qsp_bottom");
        this.iconBuffer[3][7] = par1IconRegister.registerIcon("advancedsolarpanel:qsp_top");
        this.iconBuffer[3][8] = par1IconRegister.registerIcon("advancedsolarpanel:qsp_side");
        this.iconBuffer[3][9] = par1IconRegister.registerIcon("advancedsolarpanel:qsp_side");
        this.iconBuffer[3][10] = par1IconRegister.registerIcon("advancedsolarpanel:qsp_side");
        this.iconBuffer[3][11] = par1IconRegister.registerIcon("advancedsolarpanel:qsp_side");
        this.iconBuffer[4][0] = par1IconRegister.registerIcon("advancedsolarpanel:qgen_a_side");
        this.iconBuffer[4][1] = par1IconRegister.registerIcon("advancedsolarpanel:qgen_a_side");
        this.iconBuffer[4][2] = par1IconRegister.registerIcon("advancedsolarpanel:qgen_a_side");
        this.iconBuffer[4][3] = par1IconRegister.registerIcon("advancedsolarpanel:qgen_a_side");
        this.iconBuffer[4][4] = par1IconRegister.registerIcon("advancedsolarpanel:qgen_a_side");
        this.iconBuffer[4][5] = par1IconRegister.registerIcon("advancedsolarpanel:qgen_a_side");
        this.iconBuffer[4][6] = par1IconRegister.registerIcon("advancedsolarpanel:qgen_d_side");
        this.iconBuffer[4][7] = par1IconRegister.registerIcon("advancedsolarpanel:qgen_d_side");
        this.iconBuffer[4][8] = par1IconRegister.registerIcon("advancedsolarpanel:qgen_d_side");
        this.iconBuffer[4][9] = par1IconRegister.registerIcon("advancedsolarpanel:qgen_d_side");
        this.iconBuffer[4][10] = par1IconRegister.registerIcon("advancedsolarpanel:qgen_d_side");
        this.iconBuffer[4][11] = par1IconRegister.registerIcon("advancedsolarpanel:qgen_d_side");
    }

    public IIcon getIcon(IBlockAccess world, int x, int y, int z, int blockSide) {
        if (this.iconBuffer == null) {
            return null;
        }
        int blockMeta = clampMetadata(world.getBlockMetadata(x, y, z));
        TileEntity te = world.getTileEntity(x, y, z);
        int facing = te instanceof TileEntityBase ? clampFacing(((TileEntityBase) te).getFacing()) : 0;
        int spriteIndex = ASPClientProxy.sideAndFacingToSpriteOffset[clampSide(blockSide)][facing];
        return isActive(world, x, y, z) ? this.iconBuffer[blockMeta][spriteIndex + 6]
            : this.iconBuffer[blockMeta][spriteIndex];
    }

    public IIcon getIcon(int blockSide, int blockMeta) {
        if (this.iconBuffer == null) {
            return null;
        }
        int meta = clampMetadata(blockMeta);
        return this.iconBuffer[meta][ASPClientProxy.sideAndFacingToSpriteOffset[clampSide(blockSide)][3]];
    }

    public static boolean isActive(IBlockAccess world, int x, int y, int z) {
        TileEntity te = world.getTileEntity(x, y, z);
        return te instanceof TileEntityBase && ((TileEntityBase) te).getActive();
    }

    public void breakBlock(World world, int i, int j, int k, Block par5, int par6) {
        TileEntity tileentity = world.getTileEntity(i, j, k);
        if (tileentity instanceof TileEntitySolarPanel && !(tileentity instanceof TileEntityQGenerator)) {
            this.dropItems((TileEntitySolarPanel) tileentity, world);
        }

        world.removeTileEntity(i, j, k);
        super.breakBlock(world, i, j, k, par5, par6);
    }

    /**
     * Mimics IC2 machines: a non-wrench break drops a Basic Machine Casing, while the IC2 wrench
     * replaces the first drop with {@link TileEntitySolarPanel#getWrenchDrop(EntityPlayer)}. The
     * charge-slot contents are appended separately.
     */
    @Override
    public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int metadata, int fortune) {
        ArrayList<ItemStack> drops = new ArrayList<>();
        ItemStack casing = IC2Items.getItem("machine");
        if (casing == null) {
            AdvancedSolarPanel.addLog("IC2 Basic Machine Casing is missing; using a panel drop instead.");
            drops.add(new ItemStack(this, 1, clampMetadata(metadata)));
        } else {
            drops.add(casing.copy());
        }

        // Charge-slot contents are dropped by breakBlock(), which runs on both the normal-break and
        // wrench paths. Do not add them here or they would be duplicated.
        return drops;
    }

    public int quantityDropped(Random random) {
        return 1;
    }

    public int damageDropped(int i) {
        return clampMetadata(i);
    }

    public TileEntity getBlockEntity(int i) {
        switch (clampMetadata(i)) {
            case 0:
                return new TileEntityAdvancedSolarPanel();
            case 1:
                return new TileEntityHybridSolarPanel();
            case 2:
                return new TileEntityUltimateSolarPanel();
            case 3:
                return new TileEntityQuantumSolarPanel();
            case 4:
                return new TileEntityQGenerator();
            default:
                return new TileEntityAdvancedSolarPanel();
        }
    }

    public boolean onBlockActivated(World world, int i, int j, int k, EntityPlayer player, int s, float f1, float f2,
        float f3) {
        if (player.isSneaking()) {
            return false;
        } else if (world.isRemote) {
            return true;
        } else {
            TileEntity tileentity = world.getTileEntity(i, j, k);
            if (tileentity != null) {
                player.openGui(AdvancedSolarPanel.instance, 1, world, i, j, k);
            }

            return true;
        }
    }

    private void dropItems(TileEntitySolarPanel tileentity, World world) {
        Random rand = new Random();
        if (tileentity instanceof IInventory) {
            IInventory inventory = (IInventory) tileentity;

            for (int i = 0; i < inventory.getSizeInventory(); i++) {
                ItemStack item = inventory.getStackInSlot(i);
                if (item != null && item.stackSize > 0) {
                    float rx = rand.nextFloat() * 0.8F + 0.1F;
                    float ry = rand.nextFloat() * 0.8F + 0.1F;
                    float rz = rand.nextFloat() * 0.8F + 0.1F;
                    EntityItem entityItem = new EntityItem(
                        world,
                        tileentity.xCoord + rx,
                        tileentity.yCoord + ry,
                        tileentity.zCoord + rz,
                        item.copy());

                    float factor = 0.05F;
                    entityItem.motionX = rand.nextGaussian() * factor;
                    entityItem.motionY = rand.nextGaussian() * factor + 0.2F;
                    entityItem.motionZ = rand.nextGaussian() * factor;
                    world.spawnEntityInWorld(entityItem);
                    item.stackSize = 0;
                }
            }
        }
    }

    public TileEntity createNewTileEntity(World var1, int var2) {
        return this.getBlockEntity(var2);
    }

    @SideOnly(Side.CLIENT)
    public void getSubBlocks(Item item, CreativeTabs tab, List subItems) {
        for (int ix = 0; ix <= MAX_METADATA; ix++) {
            subItems.add(new ItemStack(this, 1, ix));
        }
    }
}
