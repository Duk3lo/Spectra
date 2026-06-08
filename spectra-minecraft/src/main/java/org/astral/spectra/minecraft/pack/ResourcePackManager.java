package org.astral.spectra.minecraft.pack;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Comparator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class ResourcePackManager {
    private final Plugin plugin;
    private final Path soundsDir;
    private final Path packFile;
    private String currentHash = "";
    private String sourceStateHash = "";

    public ResourcePackManager(@NonNull Plugin plugin) {
        this.plugin = plugin;
        this.soundsDir = plugin.getDataFolder().toPath().resolve("sounds");
        this.packFile = plugin.getDataFolder().toPath().resolve("pack.zip");
    }

    public boolean buildPack() {
        try {
            if (!Files.exists(soundsDir)) Files.createDirectories(soundsDir);
            File[] oggFiles = soundsDir.toFile().listFiles((_, name) -> name.toLowerCase().endsWith(".ogg"));
            if (oggFiles == null) oggFiles = new File[0];

            Arrays.sort(oggFiles, Comparator.comparing(File::getName));
            StringBuilder stateBuilder = new StringBuilder();
            for (File f : oggFiles) {
                stateBuilder.append(f.getName()).append(":").append(f.length()).append(":").append(f.lastModified()).append(";");
            }
            String newStateHash = stateBuilder.toString();

            if (newStateHash.equals(sourceStateHash) && Files.exists(packFile)) {
                return false;
            }
            this.sourceStateHash = newStateHash;

            if (oggFiles.length == 0) {
                Files.deleteIfExists(packFile);
                this.currentHash = "";
                return true;
            }

            try (FileOutputStream fos = new FileOutputStream(packFile.toFile());
                 ZipOutputStream zos = new ZipOutputStream(fos)) {
                String mcmeta = "{\"pack\":{\"pack_format\":15,\"description\":\"Spectra Audio\"}}";
                addToZip(zos, "pack.mcmeta", mcmeta.getBytes());

                JsonObject soundsJson = new JsonObject();
                for (File file : oggFiles) {
                    String name = file.getName().replace(".ogg", "").toLowerCase().replaceAll("[^a-z0-9_]", "");
                    addToZip(zos, "assets/astral/sounds/" + name + ".ogg", Files.readAllBytes(file.toPath()));

                    JsonObject event = new JsonObject();
                    JsonArray array = new JsonArray();
                    array.add("astral:" + name);
                    event.add("sounds", array);
                    soundsJson.add(name, event);
                }
                addToZip(zos, "assets/astral/sounds.json", new Gson().toJson(soundsJson).getBytes());
            }
            this.currentHash = calculateSHA1(packFile);
            return true;

        } catch (Exception e) {
            plugin.getLogger().severe("Error building pack: " + e.getMessage());
            return false;
        }
    }

    private void addToZip(@NonNull ZipOutputStream zos, String path, byte[] data) throws IOException {
        ZipEntry entry = new ZipEntry(path);
        zos.putNextEntry(entry);
        zos.write(data);
        zos.closeEntry();
    }

    private @NonNull String calculateSHA1(Path file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        try (InputStream fis = Files.newInputStream(file)) {
            byte[] buffer = new byte[8192];
            int n;
            while ((n = fis.read(buffer)) != -1) digest.update(buffer, 0, n);
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : digest.digest()) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    public boolean hasPack() { return !currentHash.isEmpty() && Files.exists(packFile); }
    public String getCurrentHash() { return currentHash; }
    public Path getPackFile() { return packFile; }
}