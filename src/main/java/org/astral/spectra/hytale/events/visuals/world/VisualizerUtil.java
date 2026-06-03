package org.astral.spectra.hytale.events.visuals.world;

import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.World;
import org.astral.spectra.hytale.configuration.VisualsConfig.VisualPreset;
import org.astral.spectra.hytale.events.visuals.VisualizerManager.VisualizerData;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public final class VisualizerUtil {

    private VisualizerUtil() {}

    public record Direction(double forwardX, double forwardZ, double leftX, double leftZ) {}
    public record Position2D(int x, int z) {}
    public record LinearSetup(Direction dir, double centerX, double centerZ, int baseY, double startOffset) {}
    public record PulseRingPoint(float value, double angle, double radius, int x, int z) {}

    public static @NotNull Direction calculateDirections(float rotY) {
        double yawDegrees = -rotY;
        double snappedYawDegrees = Math.round(yawDegrees / 90.0) * 90.0;
        double yaw = Math.toRadians(snappedYawDegrees);

        double forwardX = Math.round(-Math.sin(yaw));
        double forwardZ = Math.round(Math.cos(yaw));

        double leftX = Math.round(-Math.cos(yaw));
        double leftZ = Math.round(-Math.sin(yaw));

        return new Direction(forwardX, forwardZ, leftX, leftZ);
    }

    public static float clamp01(float v) {
        return Math.clamp(v, 0f, 1f);
    }

    public static double getLinearStartOffset(int barCount, double spacing) {
        return -((barCount - 1) * spacing) / 2.0;
    }

    public static @NotNull LinearSetup calculateLinearSetup(@NotNull VisualizerData data, int barCount, double distanceOffset) {
        Direction dir = calculateDirections(data.getRot().getY());
        double centerX = data.getPos().getX() + (dir.forwardX() * distanceOffset);
        double centerZ = data.getPos().getZ() + (dir.forwardZ() * distanceOffset);
        int baseY = (int) Math.floor(data.getPos().getY());
        double startOffset = getLinearStartOffset(barCount, data.getPreset().getSpacing());

        return new LinearSetup(dir, centerX, centerZ, baseY, startOffset);
    }

    public static @NotNull Position2D getLinearBarPosition(double centerX, double centerZ, @NotNull Direction dir, double startOffset, int index, double spacing) {
        double currentOffset = startOffset + (index * spacing);
        int barX = (int) Math.round(centerX + (dir.leftX() * currentOffset));
        int barZ = (int) Math.round(centerZ + (dir.leftZ() * currentOffset));
        return new Position2D(barX, barZ);
    }

    public static @NotNull Position2D getCircleBarPosition(@NotNull VisualizerData data, int index, int barCount, double radius) {
        double angleStep = (Math.PI * 2.0) / barCount;
        double startAngle = -Math.PI / 2.0 + Math.toRadians(data.getRot().getY());
        double angle = startAngle + (index * angleStep);

        int x = (int) Math.round(data.getPos().getX() + Math.cos(angle) * radius);
        int z = (int) Math.round(data.getPos().getZ() + Math.sin(angle) * radius);
        return new Position2D(x, z);
    }

    public static @NotNull PulseRingPoint getPulseRingPoint(@NotNull VisualizerData data,
                                                            float value,
                                                            int index,
                                                            int barCount,
                                                            double baseRadius,
                                                            double spread,
                                                            long timeMillis) {
        value = clamp01(value);

        double angle = (Math.PI * 2.0 * index) / barCount;
        double pulse = Math.sin(timeMillis * 0.004 + index * 0.35) * 0.6;
        double radius = baseRadius + (value * spread) + pulse;

        int x = (int) Math.round(data.getPos().getX() + Math.cos(angle) * radius);
        int z = (int) Math.round(data.getPos().getZ() + Math.sin(angle) * radius);

        return new PulseRingPoint(value, angle, radius, x, z);
    }

    public static void drawVerticalBar(@NotNull World world,
                                       @NotNull Set<Vector3i> activeBlocks,
                                       @NotNull VisualPreset preset,
                                       int barX,
                                       int baseY,
                                       int barZ,
                                       int blocksHigh) {
        for (int y = 0; y < blocksHigh; y++) {
            Vector3i pos = new Vector3i(barX, baseY + y, barZ);

            if (!isAir(world, pos)) continue;

            String blockId = pickBlockForHeight(preset, y, blocksHigh);
            world.setBlock(pos.getX(), pos.getY(), pos.getZ(), blockId);
            activeBlocks.add(pos);
        }
    }

    public static String pickBlockForHeight(@NotNull VisualPreset preset, int y, int blocksHigh) {
        if (y == blocksHigh - 1 && preset.getHitBlock() != null && !preset.getHitBlock().isBlank()) {
            return preset.getHitBlock();
        }
        if ((y & 1) == 1 && preset.getAccentBlock() != null && !preset.getAccentBlock().isBlank()) {
            return preset.getAccentBlock();
        }
        return preset.getMainBlock();
    }

    public static boolean isAir(@NotNull World world, @NotNull Vector3i pos) {
        try {
            long chunkIndex = ChunkUtil.indexChunkFromBlock(pos.getX(), pos.getZ());
            var chunk = world.getChunk(chunkIndex);
            if (chunk != null) {
                return chunk.getBlock(pos.getX(), pos.getY(), pos.getZ()) == 0;
            }
        } catch (Exception ignored) {}
        return false;
    }

    public static void removeBlock(@NotNull World world, @NotNull Vector3i pos) {
        try {
            long chunkIndex = ChunkUtil.indexChunkFromBlock(pos.getX(), pos.getZ());
            var chunk = world.getChunk(chunkIndex);
            if (chunk != null) {
                chunk.setBlock(pos.getX(), pos.getY(), pos.getZ(), 0);
            }
        } catch (Exception ignored) {}
    }
}