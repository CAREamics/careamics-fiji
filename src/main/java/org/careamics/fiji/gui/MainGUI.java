package org.careamics.fiji.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeListener;

import javax.swing.*;
import javax.swing.event.SwingPropertyChangeSupport;

import org.careamics.fiji.Config;


public class MainGUI extends JFrame {
    public static final String CONFIGREADY = "configReady";
    public static final String CANCELREQUESTED = "cancelRequested";

    protected JTextField experimentNameField;
    protected JTextField axesField;
    protected JCheckBox patch3DCheckBox;
    protected JSpinner patchYXSpin;
    protected JSpinner patchZSpin;
    protected JSpinner batchSizeSpin;
    protected JSpinner numEpochsSpin;
    protected JSpinner numStepsSpin;
    protected JProgressBar mainProgressBar;
    protected JProgressBar subProgressBar;
    protected JButton runButton;
    protected JButton stopButton;

    protected Config config;
    protected String axes = "XY";
    protected String dataType = "array";
    protected String imgName;
    protected int numChannels;
    protected int numSlices;
    protected int numFrames;

    private SwingPropertyChangeSupport pcSupport = new SwingPropertyChangeSupport(this);


    public static void main(final String[] args) {
        final MainGUI mainGUI = new MainGUI();
    }

    public MainGUI() {
        this(null, 1, 1, 1);
    }

    public MainGUI(
        String imgName,
        int numChannels,
        int numSlices,
        int numFrames
    ) {
        super("CAREamics");
        setAlwaysOnTop(true);
        
        // image name and dimensions
        this.imgName = imgName;
        this.numChannels = numChannels;
        this.numSlices = numSlices;
        this.numFrames = numFrames;
        
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
        
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(5, 50, 5, 50);
        gbc.gridy++;
        gbc.weighty = 2.0;
        mainPanel.add(createConfigPanel(), gbc);

        gbc.anchor = GridBagConstraints.SOUTH;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 10, 5, 10);
        gbc.gridy++;
        gbc.weighty = 0.05;
        mainPanel.add(createBottomPanel(), gbc);

        add(mainPanel, BorderLayout.CENTER);


        this.runButton.addActionListener(e -> {
            this.runButton.setEnabled(false);
            this.stopButton.setEnabled(true);

            this.config = createConfig();
            // dispatch the config event
            pcSupport.firePropertyChange(CONFIGREADY, null, this.config);
        });

        this.stopButton.addActionListener(e -> {
            this.stopButton.setEnabled(false);
            this.mainProgressBar.setIndeterminate(true);
            this.mainProgressBar.setString("Stopping...");
            
            pcSupport.firePropertyChange(CANCELREQUESTED, null, null);
        });

        setSize(420, 540);
        // pack();
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

    public void reset() {
        this.runButton.setEnabled(true);
        this.stopButton.setEnabled(false);
        
        this.mainProgressBar.setIndeterminate(false);
        this.mainProgressBar.setValue(0);
        this.mainProgressBar.setString("0%");
        this.subProgressBar.setValue(0);
        this.subProgressBar.setString("0%");
    }

    protected Config createConfig() {
        Config config = new Config();
        config.experimentName = this.experimentNameField.getText();
        config.batchSize = (Integer) this.batchSizeSpin.getValue();
        config.numEpochs = (Integer) this.numEpochsSpin.getValue();
        config.numSteps = (Integer) this.numStepsSpin.getValue();
        config.dataType = this.dataType;
        // axes
        config.axes = this.axesField.getText().toUpperCase() + "YX";
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
        config.patchSize = patchSize;
        // config.augmentations = new String[]{};
        return config;
    }

    protected JPanel createTitlePanel() {
        // Create the title panel
        JPanel titlePanel = new JPanel();
        
        titlePanel.setBackground(Color.decode("#2a343d"));        
        ImageIcon banner = new ImageIcon(getClass().getClassLoader().getResource("banner_careamics.png"));
        int w = banner.getIconWidth();
        int h = banner.getIconHeight();
        
        JLabel titleLabel = new JLabel(banner, SwingConstants.CENTER);
        titlePanel.setPreferredSize(new Dimension(w, h));
        titlePanel.setLayout(new BorderLayout());
        titlePanel.add(titleLabel, BorderLayout.CENTER);

        return titlePanel;
    }

    protected JPanel createConfigPanel() {
        JPanel configPanel = new JPanel();
        configPanel.setLayout(new GridBagLayout());
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.weighty = 1.0;
        
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
        String ex_name = this.imgName == null ? "n2v" : this.imgName + "_n2v";
        this.experimentNameField = new JTextField(ex_name);
        configPanel.add(this.experimentNameField, gbc);
    
        // axes
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weightx = 0.1;
        configPanel.add(new JLabel("Input Axes:", SwingConstants.RIGHT), gbc);
        // XY as a fixed label
        gbc.insets = new Insets(0, 3, 0, 0);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 1;
        gbc.weightx = 0.8;
        configPanel.add(new JLabel("XY"), gbc);
        // C Z T
        String extraAxes = "";
        if (this.numChannels > 1) {
            extraAxes += "C";
        }
        if (this.numSlices > 1) {
            extraAxes += "Z";
        }
        if (this.numFrames > 1) {
            extraAxes += "T";
        }
        this.axesField = new JTextField(extraAxes);
        this.axesField.setToolTipText("only valid characters: [C, Z, T]");
        // input validation
        ArrayList<Character> validAxes = new ArrayList<Character>(Arrays.asList('C', 'Z', 'T'));
        this.axesField.addKeyListener(new KeyAdapter() {
            public void keyTyped(KeyEvent e) {
                char _input = Character.toUpperCase(e.getKeyChar());
                if (!validAxes.contains(_input)) {
                    e.consume(); // ignore invalid key presses
                } else if (axesField.getText().toUpperCase().contains(String.valueOf(_input))) {
                    e.consume(); // ignore duplicate key presses
                }
            }
            // to upper case
            public void keyReleased(KeyEvent e) {
                String text = axesField.getText().toUpperCase();
                axesField.setText(text);
            }
        });

        gbc.insets = new Insets(0, 25, 0, 0);
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.2;
        gbc.gridx = 1;
        configPanel.add(this.axesField, gbc);
    
        // YX Patch Size
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weightx = 0.1;
        configPanel.add(new JLabel("XY Patch Size:", SwingConstants.RIGHT), gbc);
        
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
        this.mainProgressBar.setMaximum(1);
        panel.add(this.mainProgressBar, gbc);

        gbc.gridy++;
        this.subProgressBar = new JProgressBar(0, 100);
        this.subProgressBar.setStringPainted(true);
        this.subProgressBar.setMaximum(1);
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
        this.stopButton = new JButton("Stop");
        this.stopButton.setEnabled(false);
        panel.add(this.stopButton, gbc);

        return panel;
    }

}
