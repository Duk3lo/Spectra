package org.astral.spectyle.hytale.events.schedulers.world;

import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.World;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public final class AudioBarsBlocks {

    private static final String BLOCK_HIGH = "Rock_Crystal_Blue_Block";

    private static final double FRONT_DISTANCE = 7.0;
    private static final double SPACING = 1.0;
    private static final int MAX_HEIGHT = 10;

    private static final Set<Vector3i> activeBlocks = new HashSet<>();

    private static Vector3d lockedPos = null;
    private static Vector3f lockedRot = null;

    private AudioBarsBlocks() {
    }

    public static void drawBlocksFront(@NotNull Vector3d playerPos,
                                       @NotNull Vector3f headRotation,
                                       float[] bars,
                                       @NotNull World world) {

        clearJustBlocks(world);

        if (bars == null || bars.length == 0) return;

        if (lockedPos == null) {
            lockedPos = new Vector3d(playerPos.getX(), playerPos.getY(), playerPos.getZ());
            lockedRot = new Vector3f(headRotation.getX(), headRotation.getY(), headRotation.getZ());
        }

        double yawDegrees = -lockedRot.getY();
        double yaw = Math.toRadians(yawDegrees);
        double forwardX = -Math.sin(yaw);
        double forwardZ = Math.cos(yaw);
        double rightX = Math.cos(yaw);
        double rightZ = Math.sin(yaw);

        double centerX = lockedPos.getX() + (forwardX * FRONT_DISTANCE);
        double centerZ = lockedPos.getZ() + (forwardZ * FRONT_DISTANCE);
        int baseY = (int) Math.floor(lockedPos.getY());

        double totalWidth = (bars.length - 1) * SPACING;
        double startOffset = -totalWidth / 2.0;

        for (int i = 0; i < bars.length; i++) {
            float value = Math.clamp(bars[i], 0f, 1f);
            if (value <= 0.05f) continue;

            double currentOffset = startOffset + (i * SPACING);
            int barX = (int) Math.round(centerX + (rightX * currentOffset));
            int barZ = (int) Math.round(centerZ + (rightZ * currentOffset));

            int blocksHigh = (int) Math.ceil(value * MAX_HEIGHT);

            for (int y = 0; y < blocksHigh; y++) {
                Vector3i pos = new Vector3i(barX, baseY + y, barZ);
                world.setBlock(pos.getX(), pos.getY(), pos.getZ(), BLOCK_HIGH);
                activeBlocks.add(pos);
            }
        }
    }

    private static void clearJustBlocks(@NotNull World world) {
        if (!activeBlocks.isEmpty()) {
            for (Vector3i pos : activeBlocks) {
                removeBlock(world, pos);
            }
            activeBlocks.clear();
        }
    }

    public static void stopAndReset(@NotNull World world) {
        clearJustBlocks(world);
        lockedPos = null;
        lockedRot = null;
    }

    private static void removeBlock(@NotNull World world, @NotNull Vector3i pos) {
        try {
            long chunkIndex = ChunkUtil.indexChunkFromBlock(pos.getX(), pos.getZ());
            var chunk = world.getChunk(chunkIndex);
            if (chunk != null) {
                chunk.setBlock(pos.getX(), pos.getY(), pos.getZ(), 0);
            }
        } catch (Exception ignored) {}
    }
}