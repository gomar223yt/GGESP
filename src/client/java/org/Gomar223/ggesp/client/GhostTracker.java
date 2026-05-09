package org.Gomar223.ggesp.client;

import net.minecraft.util.math.Vec3d;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class GhostTracker {
    private static final long EXPIRE_MS = 8_000;
    private static final int MAX_GHOSTS = 64;

    private static final Map<Long, GhostPosition> ghosts = new ConcurrentHashMap<>();
    private static long nextId = 0;

    private static volatile boolean enabled = true;

    private GhostTracker() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static void addGhost(double x, double y, double z, String source) {
        if (!enabled) return;

        long now = System.currentTimeMillis();
        for (GhostPosition existing : ghosts.values()) {
            if (existing.pos.squaredDistanceTo(x, y, z) < 4.0) {
                existing.timestamp = now;
                existing.source = source;
                return;
            }
        }

        ghosts.put(nextId++, new GhostPosition(new Vec3d(x, y, z), now, source));
        if (ghosts.size() > MAX_GHOSTS) {
            removeOldestGhost();
        }
    }

    private static void removeOldestGhost() {
        Long oldestId = null;
        long oldestTimestamp = Long.MAX_VALUE;

        for (Map.Entry<Long, GhostPosition> entry : ghosts.entrySet()) {
            long timestamp = entry.getValue().timestamp;
            if (timestamp < oldestTimestamp) {
                oldestTimestamp = timestamp;
                oldestId = entry.getKey();
            }
        }

        if (oldestId != null) {
            ghosts.remove(oldestId);
        }
    }

    public static void tick() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<Long, GhostPosition>> it = ghosts.entrySet().iterator();
        while (it.hasNext()) {
            if (now - it.next().getValue().timestamp > EXPIRE_MS) {
                it.remove();
            }
        }
    }

    public static Collection<GhostPosition> getGhosts() {
        return ghosts.values();
    }

    public static void clear() {
        ghosts.clear();
    }

    public static class GhostPosition {
        public Vec3d pos;
        public long timestamp;
        public String source;

        public GhostPosition(Vec3d pos, long timestamp, String source) {
            this.pos = pos;
            this.timestamp = timestamp;
            this.source = source;
        }

        public float getAlpha() {
            long age = System.currentTimeMillis() - timestamp;
            return Math.max(0.0F, 1.0F - (float) age / EXPIRE_MS);
        }
    }
}
