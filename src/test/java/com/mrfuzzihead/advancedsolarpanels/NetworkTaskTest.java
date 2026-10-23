package com.mrfuzzihead.advancedsolarpanels;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import com.mrfuzzihead.advancedsolarpanels.network.PacketGUIPressButton;
import com.mrfuzzihead.advancedsolarpanels.network.ServerTaskQueue;

import cpw.mods.fml.common.gameevent.TickEvent;

class NetworkTaskTest {

    @Test
    void queuedTaskRunsOnTheServerTickEnd() {
        ServerTaskQueue queue = new ServerTaskQueue();
        AtomicBoolean ran = new AtomicBoolean(false);

        ServerTaskQueue.schedule(() -> ran.set(true));
        queue.onServerTick(new TickEvent.ServerTickEvent(TickEvent.Phase.START));
        assertTrue(!ran.get());

        queue.onServerTick(new TickEvent.ServerTickEvent(TickEvent.Phase.END));
        assertTrue(ran.get());
    }

    @Test
    void buttonPacketRoundTrips() throws Throwable {
        PacketGUIPressButton source = new PacketGUIPressButton();
        source.dimID = 7;
        source.x = 1;
        source.y = 2;
        source.z = 3;
        source.buttonID = 105;

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        source.write(new DataOutputStream(buffer));

        PacketGUIPressButton decoded = new PacketGUIPressButton();
        decoded.read(new DataInputStream(new ByteArrayInputStream(buffer.toByteArray())));

        assertEquals(7, decoded.dimID);
        assertEquals(1, decoded.x);
        assertEquals(2, decoded.y);
        assertEquals(3, decoded.z);
        assertEquals(105, decoded.buttonID);
    }
}
