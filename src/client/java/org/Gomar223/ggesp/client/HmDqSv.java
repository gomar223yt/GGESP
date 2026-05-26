package org.Gomar223.ggesp.client;

public final class HmDqSv {
    private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);
    private static volatile boolean customLayersEnabled = false;

    private HmDqSv() {
    }

    public static void begin() {
        DEPTH.set(DEPTH.get() + 1);
    }

    public static void end() {
        int depth = DEPTH.get();
        if (depth <= 1) {
            DEPTH.remove();
        } else {
            DEPTH.set(depth - 1);
        }
    }

    public static boolean isActive() {
        return DEPTH.get() > 0;
    }

    public static boolean areCustomLayersEnabled() {
        return customLayersEnabled;
    }

    public static void setCustomLayersEnabled(boolean enabled) {
        customLayersEnabled = enabled;
    }
}
