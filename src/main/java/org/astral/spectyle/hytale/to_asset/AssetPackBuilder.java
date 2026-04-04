package org.astral.spectyle.hytale.to_asset;

import com.google.gson.GsonBuilder;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.*;
import java.util.Locale;
import java.util.concurrent.*;

public final class AssetPackBuilder {
    private final AssetPackConfig config;
    private final AssetPackPaths paths;
    private final JsonFiles jsonFiles;
    private final String pluginVersion;

    private final Object importLock = new Object();

    public AssetPackBuilder(@NotNull Path modsDir, @NotNull String pluginVersion, @NotNull AssetPackConfig config) {
        this.config = config;
        this.paths = new AssetPackPaths(modsDir, config.packFolderName());
        this.jsonFiles = new JsonFiles(new GsonBuilder().setPrettyPrinting().create());
        this.pluginVersion = pluginVersion.isBlank() ? "dev" : pluginVersion.trim();
    }

    public void ensureStructure() throws IOException {
        Files.createDirectories(paths.packRoot());
        Files.createDirectories(paths.commonSoundsDir());
        Files.createDirectories(paths.soundEventDir());
        ensureManifest();
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

        synchronized (importLock) {
            ensureStructure();

            waitForStableFile(localOgg);

            String soundEventId = toPascal(triggerName);
            BuiltPaths built = copyOggAtomic(localOgg, renamedOggFileName);

            waitForStableFile(built.absolute());

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
    }

    private static final ScheduledExecutorService STABILITY_CHECKER =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "asset-file-stability-checker");
                t.setDaemon(true);
                return t;
            });

    private static final long STABILITY_TIMEOUT_MS = 2000L;
    private static final long STABILITY_POLL_MS = 100L;

    private void waitForStableFile(Path file) throws IOException {
        CompletableFuture<Void> future = new CompletableFuture<>();

        final long deadline = System.currentTimeMillis() + STABILITY_TIMEOUT_MS;
        final long[] lastSize = { -1L };
        final int[] stableChecks = { 0 };

        ScheduledFuture<?> task = STABILITY_CHECKER.scheduleAtFixedRate(() -> {
            try {
                if (System.currentTimeMillis() >= deadline) {
                    future.completeExceptionally(
                            new IOException("El archivo no se estabilizó a tiempo: " + file)
                    );
                    return;
                }

                if (Files.exists(file)) {
                    long size = Files.size(file);

                    if (size > 0 && size == lastSize[0]) {
                        stableChecks[0]++;
                        if (stableChecks[0] >= 2) {
                            future.complete(null);
                        }
                    } else {
                        stableChecks[0] = 0;
                        lastSize[0] = size;
                    }
                }
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        }, 0L, STABILITY_POLL_MS, TimeUnit.MILLISECONDS);

        try {
            future.get(STABILITY_TIMEOUT_MS + 500L, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted waiting for stable file: " + file, e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException io) throw io;
            throw new IOException("Error waiting for stable file: " + file, cause);
        } catch (TimeoutException e) {
            throw new IOException("Time exceeded waiting for stable file: " + file, e);
        } finally {
            task.cancel(true);
        }
    }

    private @NotNull BuiltPaths copyOggAtomic(Path sourceOgg, String targetFileName) throws IOException {
        Path dst = paths.commonSoundsDir().resolve(targetFileName);
        Files.createDirectories(dst.getParent());

        Path tmp = dst.resolveSibling(dst.getFileName().toString() + ".tmp");

        Files.copy(sourceOgg, tmp, StandardCopyOption.REPLACE_EXISTING);

        try {
            Files.move(tmp, dst, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, dst, StandardCopyOption.REPLACE_EXISTING);
        }

        String relative = "Sounds/" + targetFileName;
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
            return packRoot.resolve("Common").resolve("Sounds");
        }

        public @NotNull Path soundEventDir() {
            return packRoot.resolve("Server")
                    .resolve("Audio")
                    .resolve("SoundEvents");
        }

        public @NotNull Path soundEventFile(String soundEventId) {
            return soundEventDir().resolve(soundEventId + ".json");
        }
    }

    public SoundEventJson readSoundEventJson(@NotNull String soundEventId) throws IOException {
        return jsonFiles.read(paths.soundEventFile(soundEventId), SoundEventJson.class);
    }
}