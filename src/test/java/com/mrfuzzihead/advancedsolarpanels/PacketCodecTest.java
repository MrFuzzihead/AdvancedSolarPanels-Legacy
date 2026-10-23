package com.mrfuzzihead.advancedsolarpanels;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.mrfuzzihead.advancedsolarpanels.network.ASPChannelHandler;
import com.mrfuzzihead.advancedsolarpanels.network.ASPPacketHandler;
import com.mrfuzzihead.advancedsolarpanels.network.PacketGUIPressButton;
import com.mrfuzzihead.advancedsolarpanels.network.ServerTaskQueue;

import cpw.mods.fml.common.gameevent.TickEvent;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;

class PacketCodecTest {

    @Test
    void guiButtonPacketRoundTripsThroughCodec() throws Exception {
        if (!ASPPacketHandler.packetTypes.contains(PacketGUIPressButton.class)) {
            ASPPacketHandler.registerPacketType(PacketGUIPressButton.class);
        }
        ASPChannelHandler codec = new ASPChannelHandler();
        EmbeddedChannel channel = new EmbeddedChannel(codec);

        PacketGUIPressButton source = new PacketGUIPressButton();
        source.dimID = 7;
        source.x = 1;
        source.y = 2;
        source.z = 3;
        source.buttonID = 105;

        ChannelHandlerContext context = channel.pipeline()
            .context(codec);
        ByteBuf buffer = Unpooled.buffer();
        codec.encodeInto(context, source, buffer);
        PacketGUIPressButton decoded = new PacketGUIPressButton();
        codec.decodeInto(context, buffer, decoded);

        assertEquals(7, decoded.dimID);
        assertEquals(1, decoded.x);
        assertEquals(2, decoded.y);
        assertEquals(3, decoded.z);
        assertEquals(105, decoded.buttonID);

        new ServerTaskQueue().onServerTick(new TickEvent.ServerTickEvent(TickEvent.Phase.END));
        channel.finish();
    }
}
