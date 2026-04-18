package org.astral.spectyle.hytale.events.visuals.world;

import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.World;
import org.astral.spectyle.hytale.events.visuals.VisualizerManager.VisualizerData;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public final class AudioBarsBlocks {

    private static final String BLOCK_HIGH = "Rock_Crystal_Blue_Block";
    private static final double FRONT_DISTANCE = 0.0;

    private AudioBarsBlocks() {}

    public static void drawBlocksFront(@NotNull VisualizerData data,
                                       float[] bars,
                                       @NotNull World world) {

        Set<Vector3i> activeBlocks = data.getActiveBlocks();
        clearJustBlocks(world, activeBlocks);

        if (bars == null || bars.length == 0) return;

        Vector3d lockedPos = data.getPos();
        Vector3f lockedRot = data.getRot();

        Adjust.Direction dir = Adjust.calculateDirections(lockedRot.getY());

        double centerX = lockedPos.getX() + (dir.forwardX() * FRONT_DISTANCE);
        double centerZ = lockedPos.getZ() + (dir.forwardZ() * FRONT_DISTANCE);
        int baseY = (int) Math.floor(lockedPos.getY());

        double totalWidth = (bars.length - 1) * data.getSpacing();
        double startOffset = -totalWidth / 2.0;

        for (int i = 0; i < bars.length; i++) {
            float value = Math.clamp(bars[i], 0f, 1f);
            if (value <= 0.05f) continue;

            double currentOffset = startOffset + (i * data.getSpacing());

            int barX = (int) Math.round(centerX + (dir.leftX() * currentOffset));
            int barZ = (int) Math.round(centerZ + (dir.leftZ() * currentOffset));

            int blocksHigh = (int) Math.ceil(value * data.getMaxHeight());

            for (int y = 0; y < blocksHigh; y++) {
                Vector3i pos = new Vector3i(barX, baseY + y, barZ);

                if (isAir(world, pos)) {
                    world.setBlock(pos.getX(), pos.getY(), pos.getZ(), BLOCK_HIGH);
                    activeBlocks.add(pos);
                }
            }
        }
    }

    private static boolean isAir(@NotNull World world, @NotNull Vector3i pos) {
        try {
            long chunkIndex = ChunkUtil.indexChunkFromBlock(pos.getX(), pos.getZ());
            var chunk = world.getChunk(chunkIndex);
            if (chunk != null) {
                return chunk.getBlock(pos.getX(), pos.getY(), pos.getZ()) == 0;
            }
        } catch (Exception ignored) {}

        return false;
    }

    private static void clearJustBlocks(@NotNull World world, @NotNull Set<Vector3i> activeBlocks) {
        if (!activeBlocks.isEmpty()) {
            for (Vector3i pos : activeBlocks) {
                removeBlock(world, pos);
            }
            activeBlocks.clear();
        }
    }

    public static void stopAndReset(@NotNull World world, @NotNull VisualizerData data) {
        clearJustBlocks(world, data.getActiveBlocks());
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