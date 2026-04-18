package org.astral.spectyle.hytale.events.visuals.world;
import org.jetbrains.annotations.NotNull;

public final class Adjust {

    public record Direction(double forwardX, double forwardZ, double leftX, double leftZ) {}

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
}