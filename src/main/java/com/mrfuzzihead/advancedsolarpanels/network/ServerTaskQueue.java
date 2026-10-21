package com.mrfuzzihead.advancedsolarpanels.network;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * Executes work on the server thread. Network packets arrive on a Netty thread and must never touch
 * worlds or tile entities directly.
 */
public final class ServerTaskQueue {

    private static final Queue<Runnable> TASKS = new ConcurrentLinkedQueue<>();

    public ServerTaskQueue() {}

    public static void schedule(Runnable task) {
        if (task != null) {
            TASKS.add(task);
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Runnable task;
        while ((task = TASKS.poll()) != null) {
            try {
                task.run();
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }
    }
}
