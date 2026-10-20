package com.mrfuzzihead.advancedsolarpanels.network;

import com.google.common.collect.Lists;
import cpw.mods.fml.common.network.FMLEmbeddedChannel;
import cpw.mods.fml.common.network.FMLOutboundHandler;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.FMLOutboundHandler.OutboundTarget;
import cpw.mods.fml.relauncher.Side;
import io.netty.channel.ChannelHandler;
import java.util.EnumMap;
import java.util.List;
import net.minecraft.entity.player.EntityPlayer;

public class ASPPacketHandler {
   public static List<Class<? extends IPacket>> packetTypes = Lists.newArrayList();
   private static EnumMap<Side, FMLEmbeddedChannel> channels;

   public static void load() {
      registerPacketType(PacketGUIPressButton.class);
      registerPacketType(PacketChangeState.class);
      channels = NetworkRegistry.INSTANCE.newChannel("AdvSolarPanels", new ChannelHandler[]{new ASPChannelHandler()});
   }

   public static void registerPacketType(Class<? extends IPacket> ptype) {
      packetTypes.add(ptype);
   }

   public static void sendToAllPlayers(IPacket packet) {
      channels.get(Side.SERVER).attr(FMLOutboundHandler.FML_MESSAGETARGET).set(OutboundTarget.ALL);
      channels.get(Side.SERVER).writeOutbound(new Object[]{packet});
   }

   public static void sendToServer(IPacket packet) {
      channels.get(Side.CLIENT).attr(FMLOutboundHandler.FML_MESSAGETARGET).set(OutboundTarget.TOSERVER);
      channels.get(Side.CLIENT).writeOutbound(new Object[]{packet});
   }

   public static void sendToPlayer(EntityPlayer ep, IPacket packet) {
      channels.get(Side.SERVER).attr(FMLOutboundHandler.FML_MESSAGETARGET).set(OutboundTarget.PLAYER);
      channels.get(Side.SERVER).attr(FMLOutboundHandler.FML_MESSAGETARGETARGS).set(ep);
      channels.get(Side.SERVER).writeOutbound(new Object[]{packet});
   }
}
