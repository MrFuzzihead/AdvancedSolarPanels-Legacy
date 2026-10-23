package com.mrfuzzihead.advancedsolarpanels.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

public class PacketGUIPressButton extends IPacket {

    public int dimID;
    public int x;
    public int y;
    public int z;
    public int buttonID;
    private EntityPlayer sender;

    @Override
    public void setSender(EntityPlayer sender) {
        this.sender = sender;
    }

    @Override
    public void read(DataInputStream bytes) throws Throwable {
        this.dimID = bytes.readInt();
        this.x = bytes.readInt();
        this.y = bytes.readInt();
        this.z = bytes.readInt();
        this.buttonID = bytes.readInt();
    }

    @Override
    public void write(DataOutputStream bytes) throws Throwable {
        bytes.writeInt(this.dimID);
        bytes.writeInt(this.x);
        bytes.writeInt(this.y);
        bytes.writeInt(this.z);
        bytes.writeInt(this.buttonID);
    }

    public static void issue(TileEntity te, int buttonID) {
        PacketGUIPressButton pgpb = new PacketGUIPressButton();
        pgpb.x = te.xCoord;
        pgpb.y = te.yCoord;
        pgpb.z = te.zCoord;
        pgpb.dimID = te.getWorldObj().provider.dimensionId;
        pgpb.buttonID = buttonID;
        ASPPacketHandler.sendToServer(pgpb);
    }

    public static void issue(ChunkCoordinates te, int dimID, int buttonID) {
        PacketGUIPressButton pgpb = new PacketGUIPressButton();
        pgpb.x = te.posX;
        pgpb.y = te.posY;
        pgpb.z = te.posZ;
        pgpb.dimID = dimID;
        pgpb.buttonID = buttonID;
        ASPPacketHandler.sendToServer(pgpb);
    }

    @Override
    public void execute() {
        final int dimensionId = this.dimID;
        final int tileX = this.x;
        final int tileY = this.y;
        final int tileZ = this.z;
        final int button = this.buttonID;
        ASPPacketHandler.scheduleServer(new Runnable() {

            @Override
            public void run() {
                try {
                    WorldServer world = DimensionManager.getWorld(dimensionId);
                    if (world == null) {
                        return;
                    }
                    if (!(sender instanceof EntityPlayerMP) || sender.worldObj.provider.dimensionId != dimensionId
                        || sender.getDistanceSq(tileX + 0.5D, tileY + 0.5D, tileZ + 0.5D) > 64.0D) {
                        return;
                    }
                    TileEntity tileEntity = world.getTileEntity(tileX, tileY, tileZ);
                    if (tileEntity instanceof IHasButton) {
                        ((IHasButton) tileEntity).handleButtonClick(button);
                    }
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
            }
        });
    }
}
