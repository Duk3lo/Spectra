package org.astral.spectyle.hytale.to_asset;

import com.google.gson.GsonBuilder;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.*;
import java.util.Locale;

public final class AssetPackBuilder {
    private final AssetPackConfig config;
    private final AssetPackPaths paths;
    private final JsonFiles jsonFiles;
    private final String pluginVersion;

    public AssetPackBuilder(@NotNull Path modsDir, @NotNull String pluginVersion, @NotNull AssetPackConfig config) {
        this.config = config;
        this.paths = new AssetPackPaths(modsDir, config.packFolderName());
        this.jsonFiles = new JsonFiles(new GsonBuilder().setPrettyPrinting().create());
        this.pluginVersion = pluginVersion.isBlank() ? "dev" : pluginVersion.trim();
    }

    public void ensureManifest() throws IOException {
        Files.createDirectories(paths.packRoot());

        AssetPackManifestJson desired = AssetPackManifestJson.create(config, pluginVersion);

        if (Files.exists(paths.manifestPath())) {
            try {
                AssetPackManifestJson existing = jsonFiles.read(paths.manifestPath(), AssetPackManifestJson.class);
                if (desired.matches(existing)) {
                    return;
                }
            } catch (Exception ignored) {}
        }

        jsonFiles.writeJsonSafely(paths.manifestPath(), desired);
    }

    public @NotNull BuiltCustomSound buildCustomSound(
            @NotNull Path localOgg,
            @NotNull String triggerName,
            @NotNull String renamedOggFileName
    ) throws IOException {

        ensureManifest();

        String soundEventId = toPascal(triggerName);
        BuiltPaths built = copyOgg(localOgg, renamedOggFileName);

        SoundEventJson soundEventJson = SoundEventJson.create(built.relative(), built.absolute());
        jsonFiles.writeJsonSafely(paths.soundEventFile(soundEventId), soundEventJson);

        return new BuiltCustomSound(
                soundEventId,
                renamedOggFileName,
                paths.packRoot(),
                built.absolute(),
                built.relative()
        );
    }

    private @NotNull BuiltPaths copyOgg(Path sourceOgg, String targetFileName) throws IOException {
        Path dst = paths.commonSoundsDir().resolve(targetFileName);

        Files.createDirectories(dst.getParent());
        Files.copy(sourceOgg, dst, StandardCopyOption.REPLACE_EXISTING);

        String relative = "Sounds/Spectyle/" + targetFileName;

        return new BuiltPaths(dst, relative);
    }

    private record BuiltPaths(Path absolute, String relative) {}

    private static @NotNull String toPascal(String name) {
        String cleaned = name == null ? "" : name.replaceAll("[^a-zA-Z0-9_\\-]+", "_");
        String[] parts = cleaned.split("[_\\-]+");

        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            sb.append(Character.toUpperCase(p.charAt(0)));
            if (p.length() > 1) {
                sb.append(p.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return sb.isEmpty() ? "Sound" : sb.toString();
    }

    public AssetPackPaths paths() {
        return paths;
    }

    public record AssetPackConfig(
            String packFolderName,
            String groupName,
            String packName,
            String authorName,
            String description
    ) {
        public AssetPackConfig {
            if (packFolderName == null || packFolderName.isBlank()) packFolderName = "SpectyleAssets";
            if (groupName == null || groupName.isBlank()) groupName = "Astral";
            if (packName == null || packName.isBlank()) packName = "SpectyleAssets";
            if (authorName == null || authorName.isBlank()) authorName = "Duk3lo";
            if (description == null || description.isBlank()) {
                description = "asset Spectyle";
            }
        }
    }

    public record BuiltCustomSound(
            String soundEventId,
            String fileName,
            Path packRoot,
            Path absoluteOggPath,
            String relativeOggPath
    ) {}

    public static final class AssetPackPaths {
        private final Path packRoot;

        public AssetPackPaths(@NotNull Path modsDir, String packFolderName) {
            this.packRoot = modsDir.resolve(packFolderName);
        }

        public Path packRoot() {
            return packRoot;
        }

        public @NotNull Path manifestPath() {
            return packRoot.resolve("manifest.json");
        }

        public @NotNull Path commonSoundsDir() {
            return packRoot.resolve("Common").resolve("Sounds").resolve("Spectyle");
        }

        public @NotNull Path soundEventDir() {
            return packRoot.resolve("Server")
                    .resolve("Audio")
                    .resolve("SoundEvents")
                    .resolve("Beat");
        }

        public @NotNull Path soundEventFile(String soundEventId) {
            return soundEventDir().resolve(soundEventId + ".json");
        }
    }

    public SoundEventJson readSoundEventJson(@NotNull String soundEventId) throws IOException {
        return jsonFiles.read(paths.soundEventFile(soundEventId), SoundEventJson.class);
    }
}