package org.astral.spectyle.hytale.to_asset;

import com.google.gson.Gson;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public final class JsonFiles {
    private final Gson gson;

    public JsonFiles(Gson gson) {
        this.gson = gson;
    }

    public void writeJsonSafely(@NotNull Path path, Object obj) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        Path tmp = path.resolveSibling(path.getFileName().toString() + ".tmp");
        String json = gson.toJson(obj);

        Files.writeString(
                tmp,
                json,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        );

        try {
            Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public <T> T read(Path path, Class<T> type) throws IOException {
        String json = Files.readString(path, StandardCharsets.UTF_8);
        return gson.fromJson(json, type);
    }
}