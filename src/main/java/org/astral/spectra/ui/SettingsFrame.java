package org.astral.spectra.ui;

import org.astral.spectra.audio.api.AudioAPI;
import org.astral.spectra.audio.engine.AudioEngine;
import org.astral.spectra.config.AudioConfig;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public final class SettingsFrame extends JDialog {

    private static final Color BG = new Color(18, 18, 24);
    private static final Color CARD = new Color(28, 28, 36);
    private static final Color TEXT = new Color(235, 235, 245);
    private static final Color MUTED = new Color(160, 160, 175);
    private static final Color ACCENT = new Color(0, 255, 204);

    private final JComboBox<Integer> fftCombo;
    private final JSlider barsSlider;
    private final JSlider attackSlider;
    private final JSlider decaySlider;

    private final JSlider bassThresholdSlider;
    private final JSlider snareThresholdSlider;
    private final JSlider hatThresholdSlider;
    private final JSlider bassCooldownSlider;
    private final JSlider snareCooldownSlider;
    private final JSlider hatCooldownSlider;

    public SettingsFrame(Frame owner, @NotNull AudioEngine engine) {
        super(owner, "⚙ Engine Settings", true);
        AudioConfig currentConfig = engine.getConfig();

        setSize(420, 620);
        setMinimumSize(new Dimension(380, 400));
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());
        getContentPane().setBackground(BG);

        fftCombo = new JComboBox<>(new Integer[]{512, 1024, 2048, 4096});
        fftCombo.setSelectedItem(currentConfig.getVisualizer().getFftSize());
        styleComponent(fftCombo);

        barsSlider = createSlider(16, 128, currentConfig.getVisualizer().getNumBars());
        attackSlider = createSlider(0, 100, (int)(currentConfig.getSmoothing().getAttack() * 100));
        decaySlider = createSlider(0, 100, (int)(currentConfig.getSmoothing().getDecay() * 100));

        bassThresholdSlider = createSlider(0, 300, (int)(currentConfig.getBeatDetection().getBassJumpThreshold() * 100));
        snareThresholdSlider = createSlider(0, 300, (int)(currentConfig.getBeatDetection().getSnareJumpThreshold() * 100));
        hatThresholdSlider = createSlider(0, 300, (int)(currentConfig.getBeatDetection().getHatJumpThreshold() * 100));

        bassCooldownSlider = createSlider(0, 500, (int)currentConfig.getBeatDetection().getBassCooldownMs());
        snareCooldownSlider = createSlider(0, 500, (int)currentConfig.getBeatDetection().getSnareCooldownMs());
        hatCooldownSlider = createSlider(0, 500, (int)currentConfig.getBeatDetection().getHatCooldownMs());

        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(CARD);
        tabs.setForeground(TEXT);
        tabs.setBorder(new EmptyBorder(10, 10, 10, 10));

        tabs.addTab("Visualizer", wrapInScrollPane(buildVisualizerPanel()));
        tabs.addTab("Smoothing", wrapInScrollPane(buildSmoothingPanel()));
        tabs.addTab("Beat Detect", wrapInScrollPane(buildBeatPanel()));

        add(tabs, BorderLayout.CENTER);
        add(buildFooter(engine, currentConfig), BorderLayout.SOUTH);
    }

    private @NotNull JScrollPane wrapInScrollPane(@NotNull JPanel content) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(CARD);
        wrapper.add(content, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(wrapper);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(CARD);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        scrollPane.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = MUTED;
                this.trackColor = BG;
            }

            @Override
            protected JButton createDecreaseButton(int orientation) { return createZeroButton(); }

            @Override
            protected JButton createIncreaseButton(int orientation) { return createZeroButton(); }

            private @NotNull JButton createZeroButton() {
                JButton jbutton = new JButton();
                jbutton.setPreferredSize(new Dimension(0, 0));
                jbutton.setMinimumSize(new Dimension(0, 0));
                jbutton.setMaximumSize(new Dimension(0, 0));
                return jbutton;
            }
        });

        return scrollPane;
    }

    private @NotNull JPanel buildFooter(@NotNull AudioEngine engine, AudioConfig currentConfig) {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 15));
        footer.setBackground(BG);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, CARD));

        JButton btnApply = createStyledButton();

        btnApply.addActionListener(e -> {
            AudioConfig newConfig = new AudioConfig();
            newConfig.getGeneral().setCurrentVolume(AudioAPI.getVolume());
            newConfig.getGeneral().setUpdateRateMs(currentConfig.getGeneral().getUpdateRateMs());

            Integer selected = (Integer) fftCombo.getSelectedItem();
            newConfig.getVisualizer().setFftSize(selected != null ? selected : 1024);
            newConfig.getVisualizer().setNumBars(barsSlider.getValue());
            newConfig.getSmoothing().setAttack(attackSlider.getValue() / 100f);
            newConfig.getSmoothing().setDecay(decaySlider.getValue() / 100f);

            newConfig.getBeatDetection().setBassJumpThreshold(bassThresholdSlider.getValue() / 100f);
            newConfig.getBeatDetection().setSnareJumpThreshold(snareThresholdSlider.getValue() / 100f);
            newConfig.getBeatDetection().setHatJumpThreshold(hatThresholdSlider.getValue() / 100f);

            newConfig.getBeatDetection().setBassCooldownMs(bassCooldownSlider.getValue());
            newConfig.getBeatDetection().setSnareCooldownMs(snareCooldownSlider.getValue());
            newConfig.getBeatDetection().setHatCooldownMs(hatCooldownSlider.getValue());

            engine.reloadConfiguration(newConfig);
            dispose();
        });

        footer.add(btnApply);
        return footer;
    }

    private @NotNull JPanel buildVisualizerPanel() {
        JPanel p = createTabPanel();
        p.add(createLabeledRow(fftCombo));
        p.add(Box.createVerticalStrut(20));
        p.add(createSliderRow("Band Count (Bars):", barsSlider, " bars"));
        return p;
    }

    private @NotNull JPanel buildSmoothingPanel() {
        JPanel p = createTabPanel();
        p.add(createSliderRow("Attack Speed (Rise):", attackSlider, "%"));
        p.add(Box.createVerticalStrut(20));
        p.add(createSliderRow("Decay Speed (Fall):", decaySlider, "%"));
        return p;
    }

    private @NotNull JPanel buildBeatPanel() {
        JPanel p = createTabPanel();

        JLabel lblThresh = new JLabel("⚡ Jump Thresholds (Sensibility)");
        lblThresh.setForeground(ACCENT);
        p.add(lblThresh);
        p.add(Box.createVerticalStrut(5));
        p.add(createSliderRow("Bass / Kick:", bassThresholdSlider, "%"));
        p.add(createSliderRow("Snare / Mid:", snareThresholdSlider, "%"));
        p.add(createSliderRow("Hat / High:", hatThresholdSlider, "%"));

        p.add(Box.createVerticalStrut(15));

        JLabel lblCool = new JLabel("⏱ Cooldowns (Recovery Time)");
        lblCool.setForeground(ACCENT);
        p.add(lblCool);
        p.add(Box.createVerticalStrut(5));
        p.add(createSliderRow("Bass Cooldown:", bassCooldownSlider, " ms"));
        p.add(createSliderRow("Snare Cooldown:", snareCooldownSlider, " ms"));
        p.add(createSliderRow("Hat Cooldown:", hatCooldownSlider, " ms"));

        return p;
    }

    private @NotNull JPanel createTabPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(CARD);
        p.setBorder(new EmptyBorder(15, 20, 15, 20));
        return p;
    }

    private @NotNull JSlider createSlider(int min, int max, int val) {
        JSlider s = new JSlider(min, max, val);
        s.setOpaque(false);
        s.setForeground(ACCENT);
        s.setBorder(new EmptyBorder(12, 0, 0, 0));
        return s;
    }

    private void styleComponent(@NotNull JComponent c) {
        c.setBackground(BG);
        c.setForeground(TEXT);
        c.setFont(new Font("SansSerif", Font.PLAIN, 14));
    }

    private @NotNull JPanel createLabeledRow(JComponent control) {
        JPanel row = new JPanel(new BorderLayout(10, 10));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JLabel lblTitle = new JLabel("FFT Precision (Quality):");
        lblTitle.setForeground(TEXT);
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 13));

        row.add(lblTitle, BorderLayout.WEST);
        row.add(control, BorderLayout.CENTER);
        return row;
    }

    private @NotNull JPanel createSliderRow(String title, @NotNull JSlider slider, String suffix) {
        JPanel row = new JPanel(new BorderLayout(5, 5));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JLabel lblTitle = new JLabel(title);
        lblTitle.setForeground(TEXT);
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 12));

        JLabel lblValue = new JLabel(slider.getValue() + suffix, SwingConstants.RIGHT);
        lblValue.setForeground(MUTED);
        lblValue.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblValue.setPreferredSize(new Dimension(65, 20));

        slider.addChangeListener(e -> lblValue.setText(slider.getValue() + suffix));

        top.add(lblTitle, BorderLayout.WEST);
        top.add(lblValue, BorderLayout.EAST);

        row.add(top, BorderLayout.NORTH);
        row.add(slider, BorderLayout.CENTER);
        return row;
    }

    private @NotNull JButton createStyledButton() {
        JButton btn = new JButton("Apply & Close") {
            @Override
            protected void paintComponent(@NotNull Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isPressed()) g2.setColor(SettingsFrame.ACCENT.darker().darker());
                else if (getModel().isRollover()) g2.setColor(SettingsFrame.ACCENT.darker());
                else g2.setColor(SettingsFrame.ACCENT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(Color.BLACK);
                g2.setFont(new Font("SansSerif", Font.BOLD, 14));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2, (getHeight() + fm.getAscent()) / 2 - 2);
                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(140, 35));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }
}