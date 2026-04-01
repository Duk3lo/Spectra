package org.astral.spectyle.ui;

import org.astral.spectyle.audio.api.AudioAPI;
import org.astral.spectyle.audio.engine.AudioEngine;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

public final class AudioControlFrame extends JFrame {

    private static final Color BG = new Color(18, 18, 24);
    private static final Color CARD = new Color(28, 28, 36);
    private static final Color CARD_2 = new Color(34, 34, 44);
    private static final Color TEXT = new Color(235, 235, 245);
    private static final Color MUTED = new Color(160, 160, 175);
    private static final Color ACCENT = new Color(255, 51, 102);
    private static final Color ACCENT_2 = new Color(0, 255, 204);

    private final AudioEngine engine;

    private final JSlider seekSlider = new JSlider(0, 1000, 0);
    private final JSlider volumeSlider = new JSlider(0, 100, 100);
    private final JLabel timeLabel = new JLabel("00:00 / 00:00", SwingConstants.RIGHT);
    private final JLabel statusLabel = new JLabel("Listo", SwingConstants.LEFT);

    private final JLabel volumeValue = new JLabel("100%", SwingConstants.RIGHT);

    private boolean isDragging = false;
    private boolean isSyncingVolume = false;

    public AudioControlFrame(AudioEngine engine) {
        this.engine = engine;

        configureLookAndFeel();

        setTitle("Spectyle Control Panel");
        setSize(620, 360);
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

        JPanel root = new JPanel(new BorderLayout(16, 16));
        root.setBackground(BG);
        root.setBorder(new EmptyBorder(16, 16, 16, 16));
        setContentPane(root);

        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildCenter(), BorderLayout.CENTER);
        root.add(buildBottom(), BorderLayout.SOUTH);

        styleSeekSlider(seekSlider);
        styleVolumeSlider(volumeSlider);

        int initialVol = Math.round(AudioAPI.getVolume() * 100f);
        volumeSlider.setValue(initialVol);
        volumeValue.setText(initialVol + "%");

        startUpdateTimer();
    }

    private @NotNull JPanel buildHeader() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG);

        JLabel title = new JLabel("Spectyle");
        title.setForeground(TEXT);
        title.setFont(new Font("SansSerif", Font.BOLD, 22));

        JLabel status = new JLabel("● LIVE");
        status.setForeground(ACCENT_2);

        panel.add(title, BorderLayout.WEST);
        panel.add(status, BorderLayout.EAST);

        return panel;
    }

    private @NotNull JPanel buildCenter() {
        JPanel card = new JPanel(new BorderLayout(10, 10));
        card.setBackground(CARD);
        card.setBorder(new EmptyBorder(15, 15, 15, 15));

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
        controls.setBorder(new EmptyBorder(10, 10, 10, 10));

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
        volumePanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel volumeLabel = new JLabel("Volume");
        volumeLabel.setForeground(TEXT);

        volumeValue.setForeground(MUTED);

        volumeSlider.addChangeListener(e -> {
            if (isSyncingVolume) return;
            int v = volumeSlider.getValue();
            volumeValue.setText(v + "%");
            engine.setVolume(v / 100f);
        });

        volumePanel.add(volumeLabel, BorderLayout.WEST);
        volumePanel.add(volumeValue, BorderLayout.EAST);
        volumePanel.add(volumeSlider, BorderLayout.SOUTH);

        bottom.add(controls);
        bottom.add(volumePanel);

        return bottom;
    }

    private void openFileChooser() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            engine.playSong(
                    file.toPath(),
                    () -> System.out.println("Playing: " + file.getName()),
                    0,
                    java.util.concurrent.TimeUnit.MILLISECONDS
            );
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
        new Timer(100, e -> {
            if (!isDragging && AudioAPI.getTotalTime() > 0) {
                float current = AudioAPI.getCurrentTime();
                float total = AudioAPI.getTotalTime();

                int progress = (int) ((current / total) * 1000);
                seekSlider.setValue(Math.clamp(progress, 0, 1000));

                timeLabel.setText(formatTime(current) + " / " + formatTime(total));
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

    private @NotNull JButton createStyledButton(String text, @NotNull Color color) {
        JButton btn = new JButton(text);
        btn.setFocusPainted(false);
        btn.setForeground(Color.WHITE);
        btn.setBackground(color.darker());
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void styleSeekSlider(@NotNull JSlider s) {
        s.setOpaque(false);
    }

    private void styleVolumeSlider(@NotNull JSlider s) {
        s.setOpaque(false);
    }

    private void configureLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
    }

    private @NotNull String formatTime(float seconds) {
        int mins = (int) (seconds / 60);
        int secs = (int) (seconds % 60);
        return String.format("%02d:%02d", mins, secs);
    }
}