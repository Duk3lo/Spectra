package org.astral.spectyle.hytale.commands.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.DefaultArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.astral.spectyle.hytale.events.visuals.VisualizerManager;
import org.astral.spectyle.hytale.events.visuals.world.AudioBarsBlocks;
import org.jetbrains.annotations.NotNull;

public final class Visuals extends AbstractPlayerCommand {
    private final RequiredArg<String> type = withRequiredArg("type", "Type: Blocks or Particles", ArgTypes.STRING);
    private final DefaultArg<String> nameArg = withDefaultArg("name", "Name of the visualizer", ArgTypes.STRING, "", "");
    private final DefaultArg<Integer> height = withDefaultArg("height", "Max height of the visual", ArgTypes.INTEGER, 10, "10");
    private final DefaultArg<Float> width = withDefaultArg("width", "Spacing/width between bars", ArgTypes.FLOAT, 1.0f, "1.0");

    private final DefaultArg<String> xArg = withDefaultArg("x", "X Coord (~ for relative)", ArgTypes.STRING, "~", "~");
    private final DefaultArg<String> yArg = withDefaultArg("y", "Y Coord (~ for relative)", ArgTypes.STRING, "~", "~");
    private final DefaultArg<String> zArg = withDefaultArg("z", "Z Coord (~ for relative)", ArgTypes.STRING, "~", "~");

    public Visuals(@NotNull String name, @NotNull String description, boolean requiresConfirmation) {
        super(name, description, requiresConfirmation);

        this.addSubCommand(new AbstractPlayerCommand("stop", "Stops the active visuals", false) {

            private final DefaultArg<String> stopNameArg = withDefaultArg("name", "Name of visual to stop", ArgTypes.STRING, "", "");

            @Override
            protected void execute(@NotNull CommandContext ctx, @NotNull Store<EntityStore> store, @NotNull Ref<EntityStore> ref, @NotNull PlayerRef playerRef, @NotNull World world) {
                String nameToStop = stopNameArg.get(ctx);

                if (nameToStop == null || nameToStop.isBlank()) {
                    for (VisualizerManager.VisualizerData data : VisualizerManager.getAllGlobalData()) {
                        if ("blocks".equals(data.getType())) {
                            AudioBarsBlocks.stopAndReset(world, data);
                        }
                    }
                    VisualizerManager.stopAllGlobally();
                    ctx.sendMessage(Message.raw("All global visualizers have been stopped and cleared."));
                } else {
                    boolean found = false;
                    for (VisualizerManager.VisualizerData data : VisualizerManager.getAllGlobalData()) {
                        if (data.getName().equalsIgnoreCase(nameToStop)) {
                            if ("blocks".equals(data.getType())) {
                                AudioBarsBlocks.stopAndReset(world, data);
                            }
                            VisualizerManager.stop(data.getName());
                            ctx.sendMessage(Message.raw("Visualizer '" + data.getName() + "' stopped."));
                            found = true;
                            break;
                        }
                    }

                    if (!found) {
                        ctx.sendMessage(Message.raw("No visualizer found with the name: " + nameToStop));
                    }
                }
            }
        });
    }

    @Override
    protected void execute(@NotNull CommandContext ctx, @NotNull Store<EntityStore> store, @NotNull Ref<EntityStore> ref, @NotNull PlayerRef playerRef, @NotNull World world) {
        String visualType = type.get(ctx);
        String reqName = nameArg.get(ctx);

        if (reqName == null || reqName.isBlank()) {
            reqName = null;
        }

        if (visualType == null || visualType.isBlank()) {
            ctx.sendMessage(Message.raw("You must specify a visual type (Blocks or Particles)"));
            return;
        }

        int maxHeight = Math.clamp(height.get(ctx), 1, 50);
        float spacing = Math.clamp(width.get(ctx), 0.1f, 5.0f);

        Vector3d playerPos = playerRef.getTransform().getPosition();
        Vector3f rotation = playerRef.getHeadRotation();

        double finalX = parseCoordinate(xArg.get(ctx), playerPos.getX());
        double finalY = parseCoordinate(yArg.get(ctx), playerPos.getY());
        double finalZ = parseCoordinate(zArg.get(ctx), playerPos.getZ());
        Vector3d targetPosition = new Vector3d(finalX, finalY, finalZ);

        if (visualType.equalsIgnoreCase("Blocks") || visualType.equalsIgnoreCase("Particles")) {

            String assignedName = VisualizerManager.start(
                    reqName,
                    visualType.toLowerCase(),
                    maxHeight,
                    spacing,
                    targetPosition,
                    rotation
            );

            ctx.sendMessage(Message.raw(String.format("Started %s visualizer '%s' at %.1f, %.1f, %.1f! Height: %d, Width: %.1f",
                    visualType, assignedName, finalX, finalY, finalZ, maxHeight, spacing)));

        } else {
            ctx.sendMessage(Message.raw("Unknown visual type. Use 'Blocks' or 'Particles'."));
        }
    }

    private double parseCoordinate(String input, double playerCord) {
        if (input == null) return playerCord;
        if (input.startsWith("~")) {
            if (input.length() == 1) return playerCord;
            try {
                return playerCord + Double.parseDouble(input.substring(1));
            } catch (NumberFormatException e) {
                return playerCord;
            }
        }
        try {
            return Double.parseDouble(input);
        } catch (NumberFormatException e) {
            return playerCord;
        }
    }
}