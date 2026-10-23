package com.mrfuzzihead.advancedsolarpanels.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import net.minecraft.entity.player.EntityPlayer;

public abstract class IPacket {

    public abstract void read(DataInputStream var1) throws Throwable;

    public abstract void write(DataOutputStream var1) throws Throwable;

    public void execute() {}

    /** Set by the server-side channel codec so packet handlers can validate the sender. */
    public void setSender(EntityPlayer sender) {}
}
