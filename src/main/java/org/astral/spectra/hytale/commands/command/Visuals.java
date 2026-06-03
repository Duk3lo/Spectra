package org.astral.spectra.hytale.commands.command;

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
import org.astral.spectra.hytale.SpectraPlugin;
import org.astral.spectra.hytale.configuration.VisualsConfig;
import org.astral.spectra.hytale.events.visuals.VisualizerManager;
import org.astral.spectra.hytale.events.visuals.world.AudioBarsBlocks;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public final class Visuals extends AbstractPlayerCommand {
    private final RequiredArg<String> presetArg = withRequiredArg("preset", "Preset name from config", ArgTypes.STRING);
    private final DefaultArg<String> nameArg = withDefaultArg("name", "Name of the visualizer", ArgTypes.STRING, "", "");
    private final DefaultArg<String> shapeArg = withDefaultArg("shape", "Shape override (blank = preset)", ArgTypes.STRING, "", "");
    private final DefaultArg<Integer> height = withDefaultArg("height", "Max height (-1 for preset default)", ArgTypes.INTEGER, -1, "-1");
    private final DefaultArg<Float> width = withDefaultArg("width", "Spacing/line width (-1.0 for preset default)", ArgTypes.FLOAT, -1.0f, "-1.0");
    private final DefaultArg<Float> radius = withDefaultArg("radius", "Circle radius (-1.0 for preset default)", ArgTypes.FLOAT, -1.0f, "-1.0");

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
                        if (data.getPreset().isBlocks() || data.getPreset().isMixed()) {
                            AudioBarsBlocks.stopAndReset(world, data);
                        }
                    }
                    VisualizerManager.stopAllGlobally();
                    ctx.sendMessage(Message.raw("All global visualizers have been stopped and cleared."));
                    return;
                }

                boolean found = false;
                for (VisualizerManager.VisualizerData data : VisualizerManager.getAllGlobalData()) {
                    if (data.getName().equalsIgnoreCase(nameToStop)) {
                        if (data.getPreset().isBlocks() || data.getPreset().isMixed()) {
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
        });
    }

    @Override
    protected void execute(@NotNull CommandContext ctx, @NotNull Store<EntityStore> store, @NotNull Ref<EntityStore> ref, @NotNull PlayerRef playerRef, @NotNull World world) {
        String presetName = presetArg.get(ctx);
        String reqName = nameArg.get(ctx);
        String requestedShape = shapeArg.get(ctx);

        if (reqName == null || reqName.isBlank()) {
            reqName = null;
        }

        Map<String, VisualsConfig.VisualPreset> presets = SpectraPlugin.getInstance().getVisualsConfig().getPresetsMap();
        VisualsConfig.VisualPreset configPreset = null;
        String actualPresetName = presetName;

        for (Map.Entry<String, VisualsConfig.VisualPreset> entry : presets.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(presetName)) {
                configPreset = entry.getValue();
                actualPresetName = entry.getKey();
                break;
            }
        }

        if (configPreset == null) {
            String availablePresets = String.join(", ", presets.keySet());
            ctx.sendMessage(Message.raw("Error: The preset '" + presetName + "' was not found. Available: " + availablePresets));
            return;
        }

        int maxHeight = height.get(ctx) != -1 ? clampInt(height.get(ctx)) : configPreset.getMaxHeight();
        float spacing = width.get(ctx) != -1.0f ? clampFloat(width.get(ctx), 0.1f, 5.0f) : (float) configPreset.getSpacing();
        float circleRadius = radius.get(ctx) != -1.0f ? clampFloat(radius.get(ctx), 1.0f, 50.0f) : (float) configPreset.getRadius();

        VisualsConfig.VisualPreset finalPreset = new VisualsConfig.VisualPreset(configPreset);
        finalPreset.setSpacing(spacing);
        finalPreset.setMaxHeight(maxHeight);
        finalPreset.setRadius(circleRadius);

        if (requestedShape != null && !requestedShape.isBlank()) {
            finalPreset.setShape(requestedShape.toLowerCase());
        }

        Vector3d playerPos = playerRef.getTransform().getPosition();
        Vector3f rotation = playerRef.getHeadRotation();

        double finalX = parseCoordinate(xArg.get(ctx), playerPos.getX());
        double finalY = parseCoordinate(yArg.get(ctx), playerPos.getY());
        double finalZ = parseCoordinate(zArg.get(ctx), playerPos.getZ());
        Vector3d targetPosition = new Vector3d(finalX, finalY, finalZ);

        String assignedName = VisualizerManager.start(
                reqName,
                finalPreset,
                targetPosition,
                rotation
        );

        ctx.sendMessage(Message.raw(String.format(
                "Started %s visualizer '%s' (preset: %s) at %.1f, %.1f, %.1f! Height: %d, Spacing: %.1f, Radius: %.1f, Shape: %s",
                finalPreset.getRenderMode(),
                assignedName,
                actualPresetName,
                finalX, finalY, finalZ,
                maxHeight,
                spacing,
                circleRadius,
                finalPreset.getShape()
        )));
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

    private int clampInt(int value) {
        return Math.clamp(value, 1, 50);
    }

    private float clampFloat(float value, float min, float max) {
        return Math.clamp(value, min, max);
    }
}