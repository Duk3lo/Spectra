package org.astral.spectra.hytale.configuration;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.util.Config;
import org.astral.spectra.hytale.to_asset.AssetPackBuilder;
import org.astral.spectra.hytale.to_asset.SoundEventJson;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class ConfigLoader {

    private static JavaPlugin plugin;
    private static final Map<String, Config<?>> configs = new HashMap<>();
    private static Path soundPath;
    private static AssetPackBuilder assetPack;

    public static void init(@NotNull JavaPlugin pluginInstance) {
        plugin = pluginInstance;
        initFolders();
        initAssetPack();
    }

    private static void initFolders() {
        try {
            soundPath = plugin.getDataDirectory().resolve("Sounds");
            Files.createDirectories(soundPath);

            plugin.getLogger().atInfo()
                    .log("Carpeta Sounds lista en: %s", soundPath);

        } catch (Exception e) {
            plugin.getLogger().atSevere()
                    .withCause(e)
                    .log("No se pudo inicializar carpetas");
        }
    }

    private static void initAssetPack() {
        try {
            AssetPackBuilder.AssetPackConfig config =
                    new AssetPackBuilder.AssetPackConfig(
                            "SpectyleAssets",
                            "Astral",
                            "Spectra.Assets",
                            "Duk3lo",
                            "Audio system pack"
                    );

            Path modsDir = Objects.requireNonNullElse(
                    plugin.getDataDirectory().getParent(),
                    plugin.getDataDirectory()
            );

            assetPack = new AssetPackBuilder(
                    modsDir,
                    "2026.03.26-89796e57b",
                    config
            );

            assetPack.ensureStructure();

            plugin.getLogger().atInfo().log(
                    "AssetPackBuilder inicializado en: %s",
                    assetPack.paths().packRoot()
            );

        } catch (Exception e) {
            plugin.getLogger().atSevere()
                    .withCause(e)
                    .log("Error inicializando AssetPackBuilder");
        }
    }

    public static AssetPackBuilder getAssetPack() {
        if (assetPack == null) {
            throw new IllegalStateException("AssetPackBuilder no ha sido inicializado");
        }
        return assetPack;
    }

    public static void add(String name, Config<?> config) {
        configs.put(name, config);
    }

    public static void loadAll() {
        if (plugin == null) {
            throw new IllegalStateException("ConfigLoader no ha sido inicializado");
        }

        for (Map.Entry<String, Config<?>> entry : configs.entrySet()) {
            String key = entry.getKey();
            String fileName = key + ".json";
            Config<?> config = entry.getValue();

            try {
                Path target = plugin.getDataDirectory().resolve(fileName);

                if (!Files.exists(target)) {
                    config.load()
                            .thenCompose(c -> config.save())
                            .join();

                    plugin.getLogger().atInfo()
                            .log(fileName + " creado en: %s", target.toString());
                } else {
                    config.load().join();

                    plugin.getLogger().atInfo()
                            .log(fileName + " cargado desde: %s", target.toString());
                }

            } catch (Exception e) {
                plugin.getLogger().atSevere()
                        .withCause(e)
                        .log("Error al manejar config: " + fileName);
            }
        }
    }

    public static SoundEventJson getSoundEventJson(String soundEventId) throws IOException {
        return getAssetPack().readSoundEventJson(soundEventId);
    }

    public static Path getSoundPath() {
        return soundPath;
    }
}