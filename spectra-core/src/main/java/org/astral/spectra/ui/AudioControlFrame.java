package org.astral.spectra.ui;

import org.astral.spectra.audio.api.AudioAPI;
import org.astral.spectra.audio.engine.AudioEngine;
import org.astral.spectra.logging.EngineLogger;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;

public final class AudioControlFrame extends JFrame {

    private static final Color BG = new Color(18, 18, 24);
    private static final Color CARD = new Color(28, 28, 36);
    private static final Color CARD_2 = new Color(34, 34, 44);
    private static final Color TEXT = new Color(235, 235, 245);
    private static final Color MUTED = new Color(160, 160, 175);
    private static final Color ACCENT = new Color(255, 51, 102);
    private static final Color ACCENT_2 = new Color(0, 255, 204);

    private static String DEFAULT_WEB_URL = "http://localhost:8080";
    
    public static void setDefaultWebUrl(String url){
        DEFAULT_WEB_URL = url;
    }

    private final AudioEngine engine;
    private final EngineLogger logger;

    private final JSlider seekSlider = new JSlider(0, 1000, 0);
    private final JSlider volumeSlider = new JSlider(0, 100, 100);
    private final JLabel timeLabel = new JLabel("00:00 / 00:00", SwingConstants.RIGHT);
    private final JLabel statusLabel = new JLabel("Ready", SwingConstants.LEFT);
    private final JLabel volumeValue = new JLabel("100%", SwingConstants.RIGHT);

    private final RhythmicBar rhythmBar = new RhythmicBar();

    private boolean isDragging = false;
    private boolean isSyncingVolume = false;

    public AudioControlFrame(AudioEngine engine, EngineLogger logger) {
        this.engine = engine;
        this.logger = logger;

        configureLookAndFeel();

        setTitle("Spectra Control Panel");
        setSize(620, 300);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                engine.shutdown();
                System.exit(0);
            }
        });

        setLocationRelativeTo(null);
        setResizable(false);

        JPanel root = new JPanel(new BorderLayout(16, 10));
        root.setBackground(BG);
        root.setBorder(new EmptyBorder(12, 12, 12, 12));
        setContentPane(root);

        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildCenter(), BorderLayout.CENTER);
        root.add(buildBottom(), BorderLayout.SOUTH);

        styleSlider(seekSlider);
        styleSlider(volumeSlider);

        int initialVol = Math.round(AudioAPI.getVolume() * 100f);
        volumeSlider.setValue(initialVol);
        volumeValue.setText(initialVol + "%");

        startUpdateTimer();
    }

    private @NotNull JPanel buildHeader() {
        JPanel panel = new JPanel(new BorderLayout(12, 0));
        panel.setBackground(BG);

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);

        JLabel title = new JLabel("Spectra");
        title.setForeground(TEXT);
        title.setFont(new Font("SansSerif", Font.BOLD, 22));

        JPanel tinyMeterRow = new JPanel(new BorderLayout(8, 0));
        tinyMeterRow.setOpaque(false);
        tinyMeterRow.setBorder(new EmptyBorder(4, 0, 0, 0));

        JLabel meterLabel = new JLabel("RHYTHM");
        meterLabel.setForeground(MUTED);
        meterLabel.setFont(new Font("Monospaced", Font.BOLD, 11));

        rhythmBar.setPreferredSize(new Dimension(110, 5));
        rhythmBar.setMinimumSize(new Dimension(80, 5));
        rhythmBar.setMaximumSize(new Dimension(140, 5));

        tinyMeterRow.add(meterLabel, BorderLayout.WEST);
        tinyMeterRow.add(rhythmBar, BorderLayout.CENTER);

        left.add(title);
        left.add(tinyMeterRow);

        JPanel centerHeader = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        centerHeader.setOpaque(false);

        JLabel urlLabel = new JLabel(" 🌐 Web ");
        urlLabel.setOpaque(true);
        urlLabel.setBackground(CARD);
        urlLabel.setForeground(ACCENT_2);
        urlLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        urlLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CARD_2, 1),
                new EmptyBorder(4, 10, 4, 10)
        ));
        urlLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        urlLabel.setToolTipText("Open Visualizer: " + DEFAULT_WEB_URL);

        urlLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                openBrowser();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                urlLabel.setBackground(CARD_2);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                urlLabel.setBackground(CARD);
            }
        });

        centerHeader.add(urlLabel);

        JPanel rightSide = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        rightSide.setOpaque(false);

        JLabel status = new JLabel("● LIVE");
        status.setForeground(ACCENT_2);

        JButton btnSettings = new JButton("⚙");
        btnSettings.setFont(new Font("SansSerif", Font.PLAIN, 18));
        btnSettings.setForeground(MUTED);
        btnSettings.setContentAreaFilled(false);
        btnSettings.setBorderPainted(false);
        btnSettings.setFocusPainted(false);
        btnSettings.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnSettings.addActionListener(e -> new SettingsFrame(this, engine).setVisible(true));

        rightSide.add(status);
        rightSide.add(btnSettings);

        panel.add(left, BorderLayout.WEST);
        panel.add(centerHeader, BorderLayout.CENTER);
        panel.add(rightSide, BorderLayout.EAST);
        return panel;
    }

    private void openBrowser() {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                if (desktop.isSupported(Desktop.Action.BROWSE)) {
                    desktop.browse(URI.create(AudioControlFrame.DEFAULT_WEB_URL));
                    return;
                }
            }
        } catch (Exception e) {
            logger.warn("[AudioControlFrame] Desktop browse failed, trying fallback: " + e.getMessage());
        }

        String os = System.getProperty("os.name", "").toLowerCase();

        try {
            ProcessBuilder pb;
            if (os.contains("win")) {
                pb = new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", AudioControlFrame.DEFAULT_WEB_URL);
            } else if (os.contains("mac")) {
                pb = new ProcessBuilder("open", AudioControlFrame.DEFAULT_WEB_URL);
            } else {
                pb = new ProcessBuilder("xdg-open", AudioControlFrame.DEFAULT_WEB_URL);
            }
            pb.start();
        } catch (IOException ex) {
            logger.error("[AudioControlFrame] Could not open browser for: " + AudioControlFrame.DEFAULT_WEB_URL, ex);
        } catch (Exception ex) {
            logger.error("[AudioControlFrame] Unexpected error while opening browser", ex);
        }
    }

    private @NotNull JPanel buildCenter() {
        JPanel card = new JPanel(new BorderLayout(10, 8));
        card.setBackground(CARD);
        card.setBorder(new EmptyBorder(10, 12, 10, 12));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        statusLabel.setForeground(ACCENT_2);
        timeLabel.setForeground(TEXT);
        top.add(statusLabel, BorderLayout.WEST);
        top.add(timeLabel, BorderLayout.EAST);

        seekSlider.addChangeListener(e -> {
            if (seekSlider.getValueIsAdjusting()) {
                isDragging = true;
                return;
            }
            if (isDragging) {
                float percentage = seekSlider.getValue() / 1000f;
                long targetMs = (long) (percentage * AudioAPI.getTotalTime() * 1000L);
                engine.seekTo(targetMs);
                isDragging = false;
            }
        });

        card.add(top, BorderLayout.NORTH);
        card.add(seekSlider, BorderLayout.CENTER);
        return card;
    }

    private @NotNull JPanel buildBottom() {
        JPanel bottom = new JPanel(new GridLayout(1, 2, 10, 0));
        bottom.setBackground(BG);

        JPanel controls = new JPanel(new GridLayout(1, 3, 10, 0));
        controls.setBackground(CARD_2);
        controls.setBorder(new EmptyBorder(8, 10, 8, 10));

        JButton btnLoad = createStyledButton("Open", ACCENT);
        JButton btnPlayPause = createStyledButton("Play/Pause", ACCENT_2);
        JButton btnStop = createStyledButton("Stop", Color.GRAY);

        btnLoad.addActionListener(e -> openFileChooser());
        btnPlayPause.addActionListener(e -> togglePlayback());
        btnStop.addActionListener(e -> engine.stopSong());

        controls.add(btnLoad);
        controls.add(btnPlayPause);
        controls.add(btnStop);

        JPanel volumePanel = new JPanel(new BorderLayout(5, 5));
        volumePanel.setBackground(CARD_2);
        volumePanel.setBorder(new EmptyBorder(8, 10, 8, 10));

        JLabel volLbl = new JLabel("Volume");
        volLbl.setForeground(TEXT);
        volumeValue.setForeground(MUTED);

        volumeSlider.addChangeListener(e -> {
            if (isSyncingVolume) return;
            int v = volumeSlider.getValue();
            volumeValue.setText(v + "%");
            engine.setVolume(v / 100f);
        });

        volumePanel.add(volLbl, BorderLayout.WEST);
        volumePanel.add(volumeValue, BorderLayout.EAST);
        volumePanel.add(volumeSlider, BorderLayout.SOUTH);

        bottom.add(controls);
        bottom.add(volumePanel);
        return bottom;
    }

    private @NotNull JButton createStyledButton(String text, @NotNull Color color) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(@NotNull Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (getModel().isPressed()) g2.setColor(color.darker());
                else if (getModel().isRollover()) g2.setColor(color.brighter());
                else g2.setColor(color);

                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);

                g2.setColor(Color.WHITE);
                g2.setFont(getFont().deriveFont(Font.BOLD));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2,
                        (getHeight() + fm.getAscent()) / 2 - 2);

                g2.dispose();
            }
        };

        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void openFileChooser() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            engine.playSong(file.toPath(), null);
            statusLabel.setText("Playing: " + file.getName());
        }
    }

    private void togglePlayback() {
        if (AudioAPI.isPlaying()) {
            engine.pauseSong();
            statusLabel.setText("Paused");
        } else {
            engine.resumeSong();
            statusLabel.setText("Playing");
        }
    }

    private void startUpdateTimer() {
        new Timer(50, e -> {
            if (!isDragging && AudioAPI.getTotalTime() > 0) {
                float cur = AudioAPI.getCurrentTime();
                float tot = AudioAPI.getTotalTime();
                seekSlider.setValue((int) ((cur / tot) * 1000));
                timeLabel.setText(formatTime(cur) + " / " + formatTime(tot));
            }

            if (!volumeSlider.getValueIsAdjusting()) {
                int vol = Math.round(AudioAPI.getVolume() * 100f);
                if (volumeSlider.getValue() != vol) {
                    isSyncingVolume = true;
                    volumeSlider.setValue(vol);
                    isSyncingVolume = false;
                }
                volumeValue.setText(vol + "%");
            }
        }).start();
    }

    private static float getRawTarget() {
        float kick = clamp01(AudioAPI.getKickIntensity());
        float snare = clamp01(AudioAPI.getSnareIntensity());
        float hat = clamp01(AudioAPI.getHatIntensity());
        float energy = clamp01(AudioAPI.getGlobalEnergy());

        kick = kick * kick;
        snare = snare * snare;
        hat = hat * hat;

        float peakHit = Math.max(kick * 0.90f, Math.max(snare * 0.65f, hat * 0.40f));
        float baseEnergy = (energy * energy) * 0.15f;

        return clamp01(peakHit + baseEnergy);
    }

    private void styleSlider(@NotNull JSlider s) {
        s.setOpaque(false);
    }

    private void configureLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
    }

    private @NotNull String formatTime(float seconds) {
        return String.format("%02d:%02d", (int) (seconds / 60), (int) (seconds % 60));
    }

    private static float clamp01(float v) {
        return Math.clamp(v, 0f, 1f);
    }

    private static class RhythmicBar extends JPanel {
        private float currentValue = 0f;

        private static final Color TRACK = new Color(40, 40, 52);
        private static final Color START = new Color(0, 255, 204);
        private static final Color MID = new Color(255, 204, 0);
        private static final Color END = new Color(255, 51, 102);

        public RhythmicBar() {
            setOpaque(false);
            setPreferredSize(new Dimension(110, 5));
            setMinimumSize(new Dimension(80, 5));
            setMaximumSize(new Dimension(140, 5));

            new Timer(12, e -> {
                float targetValue = getRawTarget();

                if (targetValue > currentValue) {
                    currentValue += (targetValue - currentValue) * 0.55f;
                } else {
                    currentValue += (targetValue - currentValue) * 0.12f;
                }

                if (Math.abs(currentValue - targetValue) < 0.005f) {
                    currentValue = targetValue;
                }

                repaint();
            }).start();
        }

        private @NotNull Color mix(@NotNull Color a, @NotNull Color b, float t) {
            t = clamp01(t);
            float it = 1.0f - t;
            int r = Math.round(a.getRed() * it + b.getRed() * t);
            int g = Math.round(a.getGreen() * it + b.getGreen() * t);
            int bl = Math.round(a.getBlue() * it + b.getBlue() * t);
            return new Color(r, g, bl);
        }

        private @NotNull Color getDynamicColor(float val) {
            if (val < 0.45f) {
                return mix(START, MID, val / 0.45f);
            }
            return mix(MID, END, (val - 0.45f) / 0.55f);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            g2.setColor(TRACK);
            g2.fillRoundRect(0, 0, w, h, h, h);

            int fillW = Math.max(1, Math.round(w * currentValue));
            Color c = getDynamicColor(currentValue);

            GradientPaint gp = new GradientPaint(0, 0, START, fillW, 0, c);
            g2.setPaint(gp);
            g2.fillRoundRect(0, 0, fillW, h, h, h);

            g2.dispose();
        }
    }
}