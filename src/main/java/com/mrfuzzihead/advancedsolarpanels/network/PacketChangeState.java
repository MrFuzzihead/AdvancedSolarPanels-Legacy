package com.mrfuzzihead.advancedsolarpanels.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTSizeTracker;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class PacketChangeState extends IPacket {

    private static final int MAX_NBT_BYTES = 1 << 20;

    private int x;
    private int y;
    private int z;
    private int dimID;
    private int eventID;
    private NBTTagCompound nbtData;

    @Override
    public void read(DataInputStream bytes) throws Throwable {
        this.x = bytes.readInt();
        this.y = bytes.readInt();
        this.z = bytes.readInt();
        this.dimID = bytes.readInt();
        this.eventID = bytes.readInt();
        int length = bytes.readInt();
        if (length < -1 || length > MAX_NBT_BYTES) {
            throw new IOException("Invalid MT state NBT length: " + length);
        }
        if (length != -1) {
            byte[] data = new byte[length];
            bytes.read(data, 0, length);
            this.nbtData = CompressedStreamTools.func_152457_a(data, NBTSizeTracker.field_152451_a);
        } else {
            this.nbtData = null;
        }
    }

    @Override
    public void write(DataOutputStream bytes) throws Throwable {
        bytes.writeInt(this.x);
        bytes.writeInt(this.y);
        bytes.writeInt(this.z);
        bytes.writeInt(this.dimID);
        bytes.writeInt(this.eventID);
        if (this.nbtData == null) {
            bytes.writeInt(-1);
        } else {
            byte[] nbtBytes = CompressedStreamTools.compress(this.nbtData);
            if (nbtBytes.length > MAX_NBT_BYTES) {
                throw new IOException("MT state NBT is too large: " + nbtBytes.length);
            }
            bytes.writeInt(nbtBytes.length);
            bytes.write(nbtBytes);
        }
    }

    public static void issue(TileEntity te, int eventID, NBTTagCompound nbtData) {
        PacketChangeState changeState = new PacketChangeState();
        changeState.x = te.xCoord;
        changeState.y = te.yCoord;
        changeState.z = te.zCoord;
        changeState.dimID = te.getWorldObj() != null ? te.getWorldObj().provider.dimensionId : 0;
        changeState.eventID = eventID;
        changeState.nbtData = nbtData;
        ASPPacketHandler.sendToAllPlayers(changeState);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void execute() {
        final int tileX = this.x;
        final int tileY = this.y;
        final int tileZ = this.z;
        final int dimensionId = this.dimID;
        final int event = this.eventID;
        final NBTTagCompound data = this.nbtData;
        Minecraft.getMinecraft()
            .func_152344_a(new Runnable() {

                @Override
                public void run() {
                    World world = Minecraft.getMinecraft().theWorld;
                    if (world == null || world.provider.dimensionId != dimensionId) {
                        return;
                    }
                    TileEntity tileEntity = world.getTileEntity(tileX, tileY, tileZ);
                    if (tileEntity instanceof IReceiveServerEvents) {
                        ((IReceiveServerEvents) tileEntity).onServerEvent(event, data);
                    }
                }
            });
    }
}
