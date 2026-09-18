package org.careamics.fiji.gui;

import ij.gui.GenericDialog;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.SwingPropertyChangeSupport;

import java.awt.*;
import java.beans.PropertyChangeListener;

import org.careamics.fiji.Config;


public class MainGUI extends JFrame {
    public static final String CONFIGREADY = "configReady";

    protected JTextField experimentNameField;
    protected JComboBox<String> axesCombo;
    protected JCheckBox patch3DCheckBox;
    protected JSpinner patchYXSpin;
    protected JSpinner patchZSpin;
    protected JSpinner batchSizeSpin;
    protected JSpinner numEpochsSpin;
    protected JSpinner numStepsSpin;
    protected JProgressBar mainProgressBar;
    protected JProgressBar subProgressBar;
    protected JButton runButton;
    protected JButton cancelButton;

    protected Config config;
    protected String dataType = "array";
    protected String name;
    protected int num_channels;
    protected int num_slices;
    protected int num_frames;

    private SwingPropertyChangeSupport pcSupport = new SwingPropertyChangeSupport(this);


    public static void main(final String[] args) {
        final MainGUI mainGUI = new MainGUI();
    }

    public MainGUI() {
        this(null, 1, 1, 1);
    }

    public MainGUI(
        String name,
        int num_channels,
        int num_slices,
        int num_frames
    ) {
        super("Configuration");
        setAlwaysOnTop(true);
        
        // image name and dimensions
        this.name = name;
        this.num_channels = num_channels;
        this.num_slices = num_slices;
        this.num_frames = num_frames;
        
        setLayout(new BorderLayout());
        
        JPanel mainPanel = new JPanel(new GridBagLayout());
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 0.1;
        mainPanel.add(createTitlePanel(), gbc);
        
        gbc.insets = new Insets(5, 50, 5, 50);
        gbc.gridy = 1;
        gbc.weighty = 0.85;
        mainPanel.add(createConfigPanel(), gbc);

        gbc.gridy = 2;
        gbc.weighty = 0.05;
        mainPanel.add(createBottomPanel(), gbc);

        add(mainPanel, BorderLayout.CENTER);


        this.runButton.addActionListener(e -> {
            this.runButton.setEnabled(false);
            
            createConfig();
            // dispatch the config event
            pcSupport.firePropertyChange(CONFIGREADY, null, this.config);
        });

        pack();
        setSize(420, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setVisible(true);


    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcSupport.removePropertyChangeListener(listener);
    }

    public void updateProgress(String msg, long current, long maximum) {
        if (msg.toLowerCase().contains("train")) {
            this.mainProgressBar.setValue((int) current);
            this.mainProgressBar.setMaximum((int)maximum);
            this.mainProgressBar.setString(msg);
        } else {
            this.subProgressBar.setValue((int)current);
            this.subProgressBar.setMaximum((int)maximum);
            this.subProgressBar.setString(msg);
        }

        this.mainProgressBar.repaint();
        this.subProgressBar.repaint();
    }

    protected void createConfig() {
        this.config = new Config();
        this.config.experiment_name = this.experimentNameField.getText();
        this.config.batchSize = (Integer) this.batchSizeSpin.getValue();
        this.config.numEpochs = (Integer) this.numEpochsSpin.getValue();
        this.config.numSteps = (Integer) this.numStepsSpin.getValue();
        this.config.dataType = this.dataType;
        // axes
        this.config.axes = "YX";
        // patch size
        int[] patchSize = new int[]{
            (Integer) this.patchYXSpin.getValue(),
            (Integer) this.patchYXSpin.getValue()
        };
        if (this.patchZSpin.isEnabled()) {
            patchSize = new int[]{
                (Integer) this.patchZSpin.getValue(),
                (Integer) this.patchYXSpin.getValue(),
                (Integer) this.patchYXSpin.getValue()
            };
        }
        this.config.patchSize = patchSize;
        // this.config.augmentations = new String[]{};
    }

    protected JPanel createTitlePanel() {
        // Create the title panel
        JPanel titlePanel = new JPanel();
        titlePanel.setBackground(Color.decode("#2a343d"));        
        // String text = "<html><div style='text-align: center; font-size: 15px;'>"
        //         + "<span style='color: #b57a32;'>CAREamics </span>" + "<span style='color: #c8613c;'>Fiji</span>";
        ImageIcon banner = new ImageIcon(getClass().getClassLoader().getResource("banner_careamics.png"));
        JLabel titleLabel = new JLabel(banner, SwingConstants.CENTER);
        // JLabel titleLabel = new JLabel(text, SwingConstants.CENTER);
        // titleLabel.setFont(new Font("mono", Font.BOLD, 24));
        // final Integer w = ImageIcon.getIconWidth();
        titlePanel.setPreferredSize(new Dimension(420, 147));
        titlePanel.setLayout(new BorderLayout());
        titlePanel.add(titleLabel, BorderLayout.CENTER);

        return titlePanel;
    }

    protected JPanel createConfigPanel() {
        JPanel configPanel = new JPanel();
        configPanel.setLayout(new GridBagLayout());
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 0, 0);

        // Experiment Name
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.1;
        configPanel.add(new JLabel("Experiment Name:", SwingConstants.RIGHT), gbc);
        
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.8;
        String ex_name = this.name == null ? "n2v" : this.name + "_n2v";
        this.experimentNameField = new JTextField(ex_name);
        configPanel.add(this.experimentNameField, gbc);

        // YX Patch Size
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weightx = 0.1;
        configPanel.add(new JLabel("YX Patch Size:", SwingConstants.RIGHT), gbc);
        
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.8;
        this.patchYXSpin = new JSpinner(new SpinnerNumberModel(64, 8, 998, 2));
        configPanel.add(this.patchYXSpin, gbc);

        // 3D Checkbox
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 1;
        gbc.gridy++;
        gbc.weightx = 0.8;
        this.patch3DCheckBox = new JCheckBox("3D Patching");
        // disable Z Patch Size if 3D Patching is not selected
        this.patch3DCheckBox.addActionListener(e -> this.patchZSpin.setEnabled(this.patch3DCheckBox.isSelected()));
        configPanel.add(this.patch3DCheckBox, gbc);

        // Z Patch Size
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weightx = 0.1;
        configPanel.add(new JLabel("Z Patch Size:", SwingConstants.RIGHT), gbc);
        
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.8;
        this.patchZSpin = new JSpinner(new SpinnerNumberModel(8, 8, 998, 2));
        this.patchZSpin.setEnabled(false);
        configPanel.add(this.patchZSpin, gbc);

        // Batch Size
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weightx = 0.1;
        configPanel.add(new JLabel("Batch Size:", SwingConstants.RIGHT), gbc);
        
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.8;
        this.batchSizeSpin = new JSpinner(new SpinnerNumberModel(8, 1, 999, 1));
        configPanel.add(this.batchSizeSpin, gbc);

        // Number of Epochs
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weightx = 0.1;
        
        configPanel.add(new JLabel("Number of Epochs:", SwingConstants.RIGHT), gbc);
        
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.8;
        this.numEpochsSpin = new JSpinner(new SpinnerNumberModel(30, 1, 999, 1));
        configPanel.add(this.numEpochsSpin, gbc);

        // Number of Steps
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 0.1;
        configPanel.add(new JLabel("Number of Steps:", SwingConstants.RIGHT), gbc);

        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.8;
        this.numStepsSpin = new JSpinner(new SpinnerNumberModel(100, 1, 999, 1));
        configPanel.add(this.numStepsSpin, gbc);
        
        return configPanel;
    }

    protected JPanel createBottomPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(2, 5, 2, 5);

        this.mainProgressBar = new JProgressBar(0, 100);
        this.mainProgressBar.setStringPainted(true);
        panel.add(this.mainProgressBar, gbc);

        gbc.gridy++;
        this.subProgressBar = new JProgressBar(0, 100);
        this.subProgressBar.setStringPainted(true);
        panel.add(this.subProgressBar, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(15, 5, 2, 5);
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.5;
        gbc.gridx = 0;
        this.runButton = new JButton("Run");
        panel.add(this.runButton, gbc);

        gbc.gridx = 1;
        this.cancelButton = new JButton("Cancel");
        panel.add(this.cancelButton, gbc);

        return panel;
    }

}
