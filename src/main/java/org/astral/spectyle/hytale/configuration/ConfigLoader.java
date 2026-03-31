package org.astral.spectyle.hytale.configuration;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.util.Config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class ConfigLoader {

    private final JavaPlugin plugin;
    private final Map<String, Config<?>> configs = new HashMap<>();

    public ConfigLoader(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public ConfigLoader add(String name, Config<?> config) {
        configs.put(name, config);
        return this;
    }

    public void loadAll() {
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
}