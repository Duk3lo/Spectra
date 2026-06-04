package org.astral.spectra.minecraft.pack;

import com.sun.net.httpserver.HttpServer;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;

public final class PackServer {
    private HttpServer server;
    private final ResourcePackManager packManager;
    private final Plugin plugin;

    public PackServer(Plugin plugin, ResourcePackManager packManager) {
        this.plugin = plugin;
        this.packManager = packManager;
    }

    public void start(int port) {
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/pack.zip", exchange -> {
                File packFile = packManager.getPackFile().toFile();

                if (!packFile.exists()) {
                    String response = "Pack not found";
                    exchange.sendResponseHeaders(404, response.length());
                    OutputStream os = exchange.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                    return;
                }

                exchange.getResponseHeaders().set("Content-Type", "application/zip");
                exchange.sendResponseHeaders(200, packFile.length());

                try (OutputStream os = exchange.getResponseBody()) {
                    Files.copy(packFile.toPath(), os);
                }
            });

            server.setExecutor(null);
            server.start();
            plugin.getLogger().info("Servidor web iniciado en el puerto " + port);

        } catch (IOException e) {
            plugin.getLogger().severe("No se pudo iniciar el servidor web: " + e.getMessage());
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            plugin.getLogger().info("Servidor web detenido.");
        }
    }
}