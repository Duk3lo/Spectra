package org.astral.spectra.minecraft.pack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NonNull;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class ResourcePackManager {
    private final Plugin plugin;
    private final Path soundsDir;
    private final Path packFile;
    private String currentHash = "";

    public ResourcePackManager(@NonNull Plugin plugin) {
        this.plugin = plugin;
        this.soundsDir = plugin.getDataFolder().toPath().resolve("sounds");
        this.packFile = plugin.getDataFolder().toPath().resolve("pack.zip");
    }

    public void buildPack() {
        try {
            if (!Files.exists(soundsDir)) {
                Files.createDirectories(soundsDir);
                plugin.getLogger().info("Carpeta 'sounds' creada. Coloca tus .ogg ahí y usa /spectra reload");
                return;
            }

            File[] oggFiles = soundsDir.toFile().listFiles((_, name) -> name.toLowerCase().endsWith(".ogg"));
            if (oggFiles == null || oggFiles.length == 0) {
                plugin.getLogger().warning("No hay archivos .ogg en la carpeta sounds. No se creará el paquete.");
                return;
            }

            plugin.getLogger().info("Construyendo Resource Pack dinámico...");

            try (FileOutputStream fos = new FileOutputStream(packFile.toFile());
                 ZipOutputStream zos = new ZipOutputStream(fos)) {

                // 1. Crear pack.mcmeta
                String mcmeta = "{\"pack\":{\"pack_format\":15,\"description\":\"Spectra Dynamic Pack\"}}";
                addToZip(zos, "pack.mcmeta", mcmeta.getBytes());

                JsonObject soundsJson = new JsonObject();

                for (File file : oggFiles) {
                    String soundName = file.getName().replace(".ogg", "").toLowerCase().replaceAll("[^a-z0-9_]", "");

                    String zipPath = "assets/astral/sounds/" + soundName + ".ogg";
                    addToZip(zos, zipPath, Files.readAllBytes(file.toPath()));

                    JsonObject soundEvent = new JsonObject();
                    soundEvent.addProperty("category", "master");
                    JsonArray soundArray = new JsonArray();
                    soundArray.add("astral:" + soundName);
                    soundEvent.add("sounds", soundArray);

                    soundsJson.add(soundName, soundEvent);
                }
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                addToZip(zos, "assets/astral/sounds.json", gson.toJson(soundsJson).getBytes());
            }
            this.currentHash = calculateSHA1(packFile);
            plugin.getLogger().info("¡Resource Pack generado con éxito! Hash: " + this.currentHash);

        } catch (Exception e) {
            plugin.getLogger().severe("Error construyendo el Resource Pack: " + e.getMessage());
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
            int n = 0;
            byte[] buffer = new byte[8192];
            while (n != -1) {
                n = fis.read(buffer);
                if (n > 0) {
                    digest.update(buffer, 0, n);
                }
            }
        }
        byte[] hashBytes = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public String getCurrentHash() {
        return currentHash;
    }

    public Path getPackFile() {
        return packFile;
    }
}