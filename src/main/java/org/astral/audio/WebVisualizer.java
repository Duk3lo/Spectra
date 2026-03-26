package org.astral.audio;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class WebVisualizer {

    private HttpServer server;
    private final List<OutputStream> clients = new CopyOnWriteArrayList<>();

    private final String engineName;
    private final int startPort;
    private final VolumeCallback volumeCallback;

    public interface VolumeCallback {
        void onVolumeChange(float level);
    }

    public WebVisualizer(String engineName, int startPort, VolumeCallback volumeCallback) {
        this.engineName = engineName;
        this.startPort = startPort;
        this.volumeCallback = volumeCallback;
    }

    public void start() {
        int port = startPort;
        boolean started = false;

        while (!started && port <= (startPort + 10)) {
            try {
                server = HttpServer.create(new InetSocketAddress(port), 0);
                started = true;
            } catch (Exception e) {
                port++;
            }
        }

        if (!started) return;

        try {
            server.createContext("/", new HtmlHandler());
            server.createContext("/stream", new StreamHandler());
            server.createContext("/volume", new VolumeHandler());

            server.setExecutor(null);
            server.start();

            String url = "http://localhost:" + port;
            System.out.println("\u001B[32m[" + engineName + "] Servidor web iniciado en " + url + "\u001B[0m");

            abrirNavegador(url);

        } catch (Exception e) {
            System.err.println("Error en la carga web: " + e);
        }
    }

    private void abrirNavegador(String url) {
        String os = System.getProperty("os.name").toLowerCase();
        try {
            if (os.contains("win")) {
                Runtime.getRuntime().exec(new String[]{"cmd", "/c", "start", url});
            } else if (os.contains("mac")) {
                Runtime.getRuntime().exec(new String[]{"open", url});
            } else {
                Runtime.getRuntime().exec(new String[]{"xdg-open", url});
            }
        } catch (Exception e) {
            System.out.println("\u001B[33mAbre tu navegador y entra a: " + url + "\u001B[0m");
        }
    }

    public void stop() {
        if (server != null) {
            System.out.println("\u001B[33m[" + engineName + "] Desconectando clientes web y apagando servidor...\u001B[0m");
            for (OutputStream os : clients) {
                try { os.close(); } catch (IOException ignored) {}
            }
            clients.clear();
            server.stop(0);
            server = null;
        }
    }

    public void update(float[] bars, float[] beatIntensity, float energy, int combo, float speed, boolean isPaused, float currentSecs, float totalSecs) {
        if (clients.isEmpty() || server == null) return;

        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"bars\":[");
        for (int i = 0; i < bars.length; i++) {
            sb.append(String.format(java.util.Locale.US, "%.3f", bars[i]));
            if (i < bars.length - 1) sb.append(",");
        }
        sb.append("],\"intensities\":[");
        for (int i = 0; i < beatIntensity.length; i++) {
            sb.append(String.format(java.util.Locale.US, "%.3f", beatIntensity[i]));
            if (i < beatIntensity.length - 1) sb.append(",");
        }
        sb.append("],\"energy\":").append(String.format(java.util.Locale.US, "%.3f", energy));
        sb.append(",\"combo\":").append(combo);
        sb.append(",\"speed\":").append(String.format(java.util.Locale.US, "%.2f", speed));
        sb.append(",\"paused\":").append(isPaused);
        sb.append(",\"current\":").append(String.format(java.util.Locale.US, "%.1f", currentSecs));
        sb.append(",\"total\":").append(String.format(java.util.Locale.US, "%.1f", totalSecs));
        sb.append("}");

        String payload = "data: " + sb + "\n\n";
        byte[] bytes = payload.getBytes();

        for (OutputStream os : clients) {
            try {
                os.write(bytes);
                os.flush();
            } catch (IOException e) {
                clients.remove(os);
            }
        }
    }

    class VolumeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            if (query != null && query.startsWith("level=")) {
                try {
                    float level = Float.parseFloat(query.split("=")[1]);
                    if (volumeCallback != null) {
                        volumeCallback.onVolumeChange(level);
                    }
                } catch (Exception ignored) {}
            }
            exchange.sendResponseHeaders(200, 0);
            exchange.close();
        }
    }

    class StreamHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
            exchange.getResponseHeaders().add("Cache-Control", "no-cache");
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(200, 0);
            clients.add(exchange.getResponseBody());
        }
    }

    class HtmlHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String html = getHtmlContent().replace("BEAT ENGINE PRO", engineName.toUpperCase());
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, html.getBytes(StandardCharsets.UTF_8).length);
            OutputStream os = exchange.getResponseBody();
            os.write(html.getBytes(StandardCharsets.UTF_8));
            os.close();
        }

        private String getHtmlContent() {
            return """
                    <!DOCTYPE html>
                    <html lang='es'>
                    <head>
                        <meta charset='UTF-8'>
                        <title>BEAT ENGINE PRO - Visualizer</title>
                        <style>
                            body { background-color: #0d0d12; color: #fff; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100vh; margin: 0; overflow: hidden; }
                            h1 { margin-bottom: 5px; color: #00ffcc; text-shadow: 0 0 10px #00ffcc; font-size: 24px; z-index: 10; }
                   \s
                            .container { display: flex; align-items: flex-end; justify-content: center; gap: 4px; height: 300px; padding: 20px; background: rgba(255,255,255,0.05); border-radius: 15px; box-shadow: 0 10px 30px rgba(0,0,0,0.5); border: 1px solid rgba(255,255,255,0.1); width: 95%; max-width: 1400px; z-index: 10;}
                            .bar-wrapper { display: flex; flex-direction: column; align-items: center; justify-content: flex-end; width: 100%; height: 100%; }
                   \s
                            .bar { width: 100%; background-color: #444; border-radius: 3px 3px 0 0; transition: height 0.03s ease-out, background-color 0.1s, box-shadow 0.1s; height: 5%; }
                   \s
                            .label { margin-top: 10px; font-size: 11px; font-weight: bold; color: #aaa; }
                            .stats { display: flex; gap: 40px; margin-top: 30px; padding: 20px; background: rgba(0,0,0,0.3); border-radius: 10px; width: 80%; max-width: 1000px; justify-content: space-around; z-index: 10; align-items: center;}
                            .stat-box { text-align: center; }
                            .stat-value { font-size: 24px; font-weight: bold; color: #fff; margin-top: 5px; text-shadow: 0 0 10px rgba(255,255,255,0.5); }
                            .stat-title { font-size: 12px; color: #aaa; }
                            #status { position: absolute; top: 20px; left: 20px; color: #ff3366; font-weight: bold; }
                            #overlay { display: none; position: absolute; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0,0,0,0.9); z-index: 999; flex-direction: column; align-items: center; justify-content: center; }
                            .vol-cont { margin-top: 20px; background: rgba(0,0,0,0.4); padding: 15px 30px; border-radius: 50px; border: 1px solid rgba(0,255,204,0.3); display: flex; align-items: center; gap: 15px; z-index: 10;}
                            input[type=range] { cursor: pointer; accent-color: #00ffcc; width: 200px; }
                            .progress-wrapper { width: 90%; max-width: 1200px; margin-top: 15px; display: flex; flex-direction: column; align-items: center; z-index: 10;}
                            .progress-container { width: 100%; height: 6px; background: rgba(255,255,255,0.1); border-radius: 3px; overflow: hidden; }
                            .progress-fill { height: 100%; width: 0%; background: linear-gradient(90deg, #00ffcc, #33ccff); box-shadow: 0 0 10px #00ffcc; transition: width 0.1s linear; }
                            .time-text { margin-top: 8px; font-size: 14px; font-weight: bold; color: #00ffcc; text-shadow: 0 0 5px #00ffcc; font-family: monospace; letter-spacing: 1px; }
                   \s
                            /* --- NUEVOS ESTILOS PARA LOS INDICADORES SEPARADOS --- */
                            .rhythm-panel { display: flex; justify-content: space-around; gap: 10px; margin-top: 8px; }
                            .rhythm-indicator {
                                font-size: 18px;
                                font-weight: 900;
                                text-transform: uppercase;
                                padding: 5px 10px;
                                border-radius: 5px;
                                background: rgba(0,0,0,0.6);
                                border: 1px solid rgba(255,255,255,0.1);
                                opacity: 0.3;
                                transition: opacity 0.05s ease-out, background-color 0.1s;
                                display: inline-block;
                            }
                            #kick-text { color: #ff3366; }
                            #snare-text { color: #00ffcc; }
                            #hat-text { color: #33ccff; }
                        </style>
                    </head>
                    <body>
                        <div id='overlay'><h1 style='color:#ff3366; text-shadow: 0 0 20px #ff3366; font-size: 50px;'>JUEGO CERRADO</h1><p>Esta pestaña se puede cerrar de forma segura...</p></div>
                        <div id='status'>EN PAUSA</div>
                        <h1>BEAT ENGINE PRO</h1>
                   \s
                        <div class='container' id='bars-container'></div>
                   \s
                        <div class='progress-wrapper'>
                            <div class='progress-container'>
                                <div class='progress-fill' id='progress-fill'></div>
                            </div>
                            <div class='time-text' id='time-text'>00:00 / 00:00</div>
                        </div>
                   \s
                        <div class='stats'>
                            <div class='stat-box'><div class='stat-title'>ENERGÍA GLOBAL</div><div class='stat-value' id='energy-val'>0%</div></div>
                   \s
                            <!-- AQUI ESTÁN LOS INDICADORES SEPARADOS -->
                            <div class='stat-box' style='min-width: 250px;'>
                                <div class='stat-title'>MONITOR DE RITMO MULTI-BANDA</div>
                                <div class='rhythm-panel'>
                                    <div id='kick-text' class='rhythm-indicator'>KICK</div>
                                    <div id='snare-text' class='rhythm-indicator'>SNARE</div>
                                    <div id='hat-text' class='rhythm-indicator'>HAT</div>
                                </div>
                            </div>
                   \s
                            <div class='stat-box'><div class='stat-title'>RACHA HITS</div><div class='stat-value' id='combo-val' style='color:#ff007f;'>0x</div></div>
                            <div class='stat-box'><div class='stat-title'>VELOCIDAD</div><div class='stat-value' id='speed-val'>1.00x</div></div>
                        </div>
                   \s
                        <div class='vol-cont'>
                            <span class='stat-title'>VOLUMEN</span>
                            <input type='range' id='vol-slider' min='0' max='1' step='0.01' value='0.1'>
                            <span id='vol-label' class='stat-value' style='font-size: 16px; min-width: 45px;'>10%</span>
                        </div>
                   \s
                        <script>
                            const container = document.getElementById('bars-container');
                            const kickText = document.getElementById('kick-text');
                            const snareText = document.getElementById('snare-text');
                            const hatText = document.getElementById('hat-text');
                           \s
                            let barElements = [];
                            let isInitialized = false;
                   \s
                            const source = new EventSource('/stream');
                   \s
                            source.onerror = function(event) {
                                 source.close();
                                 document.getElementById('overlay').style.display = 'flex';
                                 setTimeout(() => window.close(), 1500);
                            };
                   \s
                            const slider = document.getElementById('vol-slider');
                            const volLabel = document.getElementById('vol-label');
                            slider.oninput = function() {
                                const val = this.value;
                                volLabel.innerText = Math.round(val * 100) + '%';
                                fetch('/volume?level=' + val);
                            };
                            fetch('/volume?level=0.1');
                   \s
                            function formatTime(secs) {
                                if (isNaN(secs) || secs < 0) return "00:00";
                                let m = Math.floor(secs / 60);
                                let s = Math.floor(secs % 60);
                                return (m < 10 ? "0" + m : m) + ":" + (s < 10 ? "0" + s : s);
                            }
                   \s
                            source.onmessage = function(event) {
                                const data = JSON.parse(event.data);
                                const numBars = data.bars.length;
                   \s
                                if (!isInitialized) {
                                    for(let i=0; i<numBars; i++) {
                                        let wrap = document.createElement('div'); wrap.className = 'bar-wrapper';
                                        let bar = document.createElement('div'); bar.className = 'bar';
                                        let lbl = document.createElement('div'); lbl.className = 'label';\s
                                        lbl.innerText = (i + 1);
                                        wrap.appendChild(bar); wrap.appendChild(lbl);
                                        container.appendChild(wrap); barElements.push(bar);
                                    }
                                    isInitialized = true;
                                }
                   \s
                                document.getElementById('status').innerText = data.paused ? '⏸ EN PAUSA' : '▶ REPRODUCIENDO';
                                document.getElementById('status').style.color = data.paused ? '#ff3366' : '#00ffcc';
                                document.getElementById('energy-val').innerText = Math.round(data.energy * 100) + '%';
                                document.getElementById('combo-val').innerText = data.combo + 'x';
                                document.getElementById('speed-val').innerText = data.speed.toFixed(2) + 'x';
                   \s
                                let percent = (data.current / data.total) * 100;
                                if (isNaN(percent)) percent = 0;
                                document.getElementById('progress-fill').style.width = percent + '%';
                                document.getElementById('time-text').innerText = formatTime(data.current) + " / " + formatTime(data.total);
                   \s
                                // --- SEPARAMOS LA DETECCIÓN EN 3 VARIABLES ---
                                let maxBassHit = 0, maxSnareHit = 0, maxHighHit = 0;
                               \s
                                for(let i=0; i<5; i++) maxBassHit = Math.max(maxBassHit, data.intensities[i] || 0);
                                for(let i=5; i<17; i++) maxSnareHit = Math.max(maxSnareHit, data.intensities[i] || 0);
                                for(let i=17; i<numBars; i++) maxHighHit = Math.max(maxHighHit, data.intensities[i] || 0);
                               \s
                                // --- ANIMACIÓN DEL KICK (ROJO) ---
                                kickText.style.opacity = 0.2 + (maxBassHit * 0.8);
                                kickText.style.textShadow = maxBassHit > 0.4 ? `0 0 ${maxBassHit * 15}px #ff3366` : 'none';
                                let kScale = 1 + (maxBassHit * 0.25);
                                let kX = maxBassHit > 0.85 ? (Math.random() * 6 - 3) : 0;
                                let kY = maxBassHit > 0.85 ? (Math.random() * 6 - 3) : 0;
                                kickText.style.transform = `translate(${kX}px, ${kY}px) scale(${kScale})`;
                                kickText.style.backgroundColor = maxBassHit > 0.85 ? 'rgba(255, 51, 102, 0.2)' : 'rgba(0,0,0,0.6)';
                                kickText.style.color = maxBassHit > 0.85 ? '#ffffff' : '#ff3366';

                                // --- ANIMACIÓN DEL SNARE (CYAN) ---
                                snareText.style.opacity = 0.2 + (maxSnareHit * 0.8);
                                snareText.style.textShadow = maxSnareHit > 0.4 ? `0 0 ${maxSnareHit * 15}px #00ffcc` : 'none';
                                let sScale = 1 + (maxSnareHit * 0.20);
                                let sX = maxSnareHit > 0.85 ? (Math.random() * 4 - 2) : 0;
                                let sY = maxSnareHit > 0.85 ? (Math.random() * 4 - 2) : 0;
                                snareText.style.transform = `translate(${sX}px, ${sY}px) scale(${sScale})`;
                                snareText.style.backgroundColor = maxSnareHit > 0.85 ? 'rgba(0, 255, 204, 0.2)' : 'rgba(0,0,0,0.6)';
                                snareText.style.color = maxSnareHit > 0.85 ? '#ffffff' : '#00ffcc';

                                // --- ANIMACIÓN DEL HI-HAT (AZUL) ---
                                hatText.style.opacity = 0.2 + (maxHighHit * 0.8);
                                hatText.style.textShadow = maxHighHit > 0.4 ? `0 0 ${maxHighHit * 15}px #33ccff` : 'none';
                                let hScale = 1 + (maxHighHit * 0.15);
                                let hX = maxHighHit > 0.85 ? (Math.random() * 2 - 1) : 0;
                                let hY = maxHighHit > 0.85 ? (Math.random() * 2 - 1) : 0;
                                hatText.style.transform = `translate(${hX}px, ${hY}px) scale(${hScale})`;
                                hatText.style.backgroundColor = maxHighHit > 0.85 ? 'rgba(51, 204, 255, 0.2)' : 'rgba(0,0,0,0.6)';
                                hatText.style.color = maxHighHit > 0.85 ? '#ffffff' : '#33ccff';
                   \s
                                // Dibuja las barras
                                for(let i=0; i<numBars; i++) {
                                    let intensity = data.intensities[i];
                   \s
                                    let baseH = Math.pow(Math.min(1.0, data.bars[i]), 1.5) * 55;
                                    let extraH = Math.pow(intensity, 1.2) * 45;\s
                                    let h = baseH + extraH;
                   \s
                                    if (h < 2) h = 2;
                                    if (h > 100) h = 100;
                   \s
                                    barElements[i].style.height = h + '%';
                   \s
                                    let color, shadow;
                                    let isBass = i < (numBars * 0.20);\s
                                    let isMid = i < (numBars * 0.60);
                   \s
                                    if (intensity > 0.8) { color = '#ffffff'; shadow = '0 0 15px #fff'; }
                                    else if (intensity > 0.4) {
                                        if (isBass) { color = '#ff3366'; shadow = '0 0 15px #ff3366'; }
                                        else if (isMid) { color = '#00ffcc'; shadow = '0 0 15px #00ffcc'; }
                                        else { color = '#33ccff'; shadow = '0 0 15px #33ccff'; }
                                    } else {
                                        if (isBass) color = '#881133';
                                        else if (isMid) color = '#006655';
                                        else color = '#115588';
                                        shadow = 'none';
                                    }
                                    barElements[i].style.backgroundColor = color;
                                    barElements[i].style.boxShadow = shadow;
                                }
                            };
                        </script>
                    </body>
                    </html>""";
        }
    }
}