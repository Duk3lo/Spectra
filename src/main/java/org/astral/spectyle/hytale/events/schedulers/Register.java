package org.astral.spectyle.hytale.events.schedulers;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

public final class Register {

    private static final Map<String, ScheduledFuture<?>> REGISTER = new ConcurrentHashMap<>();

    private Register() {
    }

    public static void register(String id, ScheduledFuture<?> scheduledFuture) {
        ScheduledFuture<?> old = REGISTER.put(id, scheduledFuture);
        if (old != null && !old.isDone()) {
            old.cancel(true);
        }
    }

    public static ScheduledFuture<?> getScheduler(String id) {
        return REGISTER.get(id);
    }

    public static void removeScheduler(String id) {
        ScheduledFuture<?> future = REGISTER.remove(id);
        if (future != null && !future.isDone()) {
            future.cancel(true);
        }
    }

    public static boolean isRunning(String id) {
        ScheduledFuture<?> future = REGISTER.get(id);
        return future != null && !future.isDone();
    }
}