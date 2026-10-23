package com.mrfuzzihead.advancedsolarpanels;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.Test;

import com.mrfuzzihead.advancedsolarpanels.network.PacketChangeState;

class PacketChangeStateTest {

    @Test
    void roundTripsCoordinatesEventAndNbt() throws Throwable {
        NBTTagCompound data = new NBTTagCompound();
        data.setInteger("marker", 42);
        PacketChangeState source = packet(3, 4, 5, 7, 9, data);

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        source.write(new DataOutputStream(buffer));

        PacketChangeState decoded = new PacketChangeState();
        decoded.read(new DataInputStream(new ByteArrayInputStream(buffer.toByteArray())));

        assertEquals(3, field(decoded, "x"));
        assertEquals(4, field(decoded, "y"));
        assertEquals(5, field(decoded, "z"));
        assertEquals(7, field(decoded, "dimID"));
        assertEquals(9, field(decoded, "eventID"));
        assertEquals(42, ((NBTTagCompound) field(decoded, "nbtData")).getInteger("marker"));
    }

    @Test
    void roundTripsNullNbt() throws Throwable {
        PacketChangeState source = packet(0, 0, 0, 0, 1, null);

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        source.write(new DataOutputStream(buffer));

        PacketChangeState decoded = new PacketChangeState();
        decoded.read(new DataInputStream(new ByteArrayInputStream(buffer.toByteArray())));

        assertNull(field(decoded, "nbtData"));
    }

    @Test
    void rejectsOversizedNbtLength() throws Throwable {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(buffer);
        out.writeInt(0);
        out.writeInt(0);
        out.writeInt(0);
        out.writeInt(0);
        out.writeInt(0);
        out.writeInt((1 << 20) + 1);
        out.flush();

        PacketChangeState decoded = new PacketChangeState();
        assertThrows(
            IOException.class,
            () -> decoded.read(new DataInputStream(new ByteArrayInputStream(buffer.toByteArray()))));
    }

    private static PacketChangeState packet(int x, int y, int z, int dim, int event, NBTTagCompound data)
        throws Exception {
        PacketChangeState packet = new PacketChangeState();
        set(packet, "x", x);
        set(packet, "y", y);
        set(packet, "z", z);
        set(packet, "dimID", dim);
        set(packet, "eventID", event);
        set(packet, "nbtData", data);
        return packet;
    }

    private static void set(Object object, String name, Object value) throws Exception {
        Field field = object.getClass()
            .getDeclaredField(name);
        field.setAccessible(true);
        field.set(object, value);
    }

    private static Object field(Object object, String name) throws Exception {
        Field field = object.getClass()
            .getDeclaredField(name);
        field.setAccessible(true);
        return field.get(object);
    }
}
