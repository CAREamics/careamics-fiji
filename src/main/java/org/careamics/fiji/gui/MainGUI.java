package org.careamics.fiji.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
// import java.beans.PropertyChangeListener;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
// import javax.swing.event.SwingPropertyChangeSupport;

import ij.ImagePlus;
import ij.WindowManager;

import org.careamics.fiji.Config;


public class MainGUI extends JFrame {
    public static final String START_TRAINING = "startTraining";
    public static final String START_PREDICTION = "startPrediction";
    public static final String CANCEL_REQUESTED = "cancelRequested";

    protected JTextField experimentNameField;
    protected AxesComboBox axesCombobox;
    protected JCheckBox patch3DCheckBox;
    protected JSpinner patchYXSpin;
    protected JSpinner patchZSpin;
    protected JSpinner batchSizeSpin;
    protected JSpinner numEpochsSpin;
    protected JSpinner numStepsSpin;
    protected JProgressBar trainProgressBar;
    protected JProgressBar valProgressBar;
    protected JButton trainButton;
    protected JButton trainStopButton;

    protected JCheckBox tilingCheckBox;
    protected JSpinner xyTileSizeSpin;
    protected JSpinner zTileSizeSpin;
    protected JSpinner predBatchSizeSpin;
    protected JCheckBox pred3DCheckBox;
    protected JProgressBar predProgressBar;
    protected JButton predictionButton;
    protected JButton predictionStopButton;
    
    protected Config config;
    protected String axes = "XY";
    protected String imgName;
    protected Integer numChannels;
    protected Integer numSlices;
    protected Integer numFrames;
    
    protected InputDataPanel trainDataPanel;
    protected InputDataPanel predictionDataPanel;
    protected boolean modelFromDisk;
    protected String checkpointPath;
    protected final JFileChooser checkpointChooser = new JFileChooser();


    public static void main(final String[] args) {
        final MainGUI mainGUI = new MainGUI();
    }

    public MainGUI() {
        super("CAREamics");
        // setAlwaysOnTop(true);
        
        JPanel mainPanel = new JPanel(new GridBagLayout());
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 0;
        mainPanel.add(createTitlePanel(), gbc);
        
        // create tabs for input and config panels
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Training", createTrainingPanel());
        tabbedPane.addTab("Prediction", createPredictionPanel());
        gbc.gridy++;
        gbc.weighty = 1.0;
        mainPanel.add(tabbedPane, gbc);
        
        setLayout(new GridBagLayout());
        GridBagConstraints mainGBC = new GridBagConstraints();
        mainGBC.anchor = GridBagConstraints.NORTH;
        mainGBC.fill = GridBagConstraints.BOTH;
        mainGBC.gridx = 0;
        mainGBC.gridy = 0;
        mainGBC.weightx = 1.0;
        mainGBC.weighty = 1.0;
        
        add(mainPanel, mainGBC);
        
        // actions
        // train
        this.trainButton.addActionListener(e -> {
            this.trainButton.setEnabled(false);
            this.trainStopButton.setEnabled(true);
        
            this.config = createTrainConfig();
            // dispatch the config event
            this.firePropertyChange(START_TRAINING, null, this.config);
        });
        
        this.trainStopButton.addActionListener(e -> {
            this.trainStopButton.setEnabled(false);
            this.trainProgressBar.setIndeterminate(true);
            this.trainProgressBar.setString("Stopping...");
            
            this.firePropertyChange(CANCEL_REQUESTED, null, null);
        });
        
        this.trainDataPanel.addPropertyChangeListener(evt -> {
            if (evt.getPropertyName() == InputDataPanel.IMAGE_SELECTED) {
                selectedImageChanged((String) evt.getNewValue());
            
            } else if (evt.getPropertyName() == InputDataPanel.FROM_DISK) {
                if ((Boolean) evt.getNewValue()) {
                    fromDiskSelected();
                }
            }
        });

        // prediction
        this.predictionButton.addActionListener(e -> {
            this.predictionButton.setEnabled(false);
            this.predictionStopButton.setEnabled(true);
            
            HashMap<String, Object> predConfig = createPredictionConfig();
            this.firePropertyChange(START_PREDICTION, null, predConfig);
        });

        this.predictionStopButton.addActionListener(e -> {
            this.predictionStopButton.setEnabled(false);
            this.predProgressBar.setIndeterminate(true);
            this.predProgressBar.setString("Stopping...");
            
            this.firePropertyChange(CANCEL_REQUESTED, null, null);
        });
        
        setSize(430, 705);
        // pack();
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setVisible(true);
    }

    public void updateProgress(String msg, long current, long maximum) {
        if (msg.toLowerCase().contains("train")) {
            this.trainProgressBar.setValue((int) current);
            this.trainProgressBar.setMaximum((int)maximum);
            this.trainProgressBar.setString(msg);
        } else {
            this.valProgressBar.setValue((int)current);
            this.valProgressBar.setMaximum((int)maximum);
            this.valProgressBar.setString(msg);
        }

        this.trainProgressBar.repaint();
        this.valProgressBar.repaint();
    }

    public void updatePredictionProgress(String msg, long current, long maximum) {
        this.predProgressBar.setValue((int) current);
        this.predProgressBar.setMaximum((int) maximum);
        this.predProgressBar.setString(msg);
        this.predProgressBar.repaint();
    }

    public void reset() {
        this.trainButton.setEnabled(true);
        this.trainStopButton.setEnabled(false);
        this.trainProgressBar.setIndeterminate(false);
        this.trainProgressBar.setValue(0);
        this.trainProgressBar.setString("0%");
        this.valProgressBar.setValue(0);
        this.valProgressBar.setString("0%");

        this.predictionButton.setEnabled(true);
        this.predictionStopButton.setEnabled(false);
        this.predProgressBar.setIndeterminate(false);
        this.predProgressBar.setValue(0);
        this.predProgressBar.setString("0%");
    }

    protected Config createTrainConfig() {
        Config config = new Config();
        config.experimentName = this.experimentNameField.getText();
        config.batchSize = (Integer) this.batchSizeSpin.getValue();
        config.numEpochs = (Integer) this.numEpochsSpin.getValue();
        config.numSteps = (Integer) this.numStepsSpin.getValue();
        // axes
        config.axes = this.axesCombobox.getSelectedItem().toString().toUpperCase();
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
        config.numChannels = this.numChannels;
        // config.augmentations = new String[]{};
        // train data source
        config.trainSource = this.trainDataPanel.selectedImage;
        config.fromDisk = this.trainDataPanel.fromDisk;
        if (this.trainDataPanel.fromDisk) {
            config.numChannels = null;
        }

        return config;
    }

    protected HashMap<String, Object> createPredictionConfig() {
        HashMap<String, Object> config = new HashMap<>();
        if (this.modelFromDisk) {
            config.put("checkpoint_path", this.checkpointPath);
        } else {
            config.put("checkpoint_path", null);
        }
        config.put("pred_source", this.predictionDataPanel.selectedImage);
        config.put("from_disk", this.predictionDataPanel.fromDisk);
        config.put("batch_size", this.predBatchSizeSpin.getValue());
        config.put("xy_tile_size", this.xyTileSizeSpin.getValue());
        if (this.pred3DCheckBox.isSelected()) {
            config.put("z_tile_size", this.zTileSizeSpin.getValue());
        } else {
            config.put("z_tile_size", null);
        }

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

    protected JPanel createTrainingPanel() {
        JPanel basePanel = new JPanel();
        basePanel.setLayout(new GridBagLayout());
        
        this.trainDataPanel = new InputDataPanel("Data");
        JPanel configPanel = createTrainingConfigPanel();
        JPanel progressPanel = createTrainingProgressPanel();
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 5, 0);
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 0.1;
        basePanel.add(this.trainDataPanel, gbc);
        
        gbc.insets = new Insets(0, 0, 5, 0);
        gbc.weighty = 0.8;
        gbc.gridy++;
        basePanel.add(configPanel, gbc);

        gbc.weighty = 0.1;
        gbc.gridy++;
        basePanel.add(progressPanel, gbc);
        
        return basePanel;
    }

    protected JPanel createPredictionPanel() {
        JPanel basePanel = new JPanel();
        basePanel.setLayout(new GridBagLayout());
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 5, 0);
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        // gbc.weighty = 0.2;
        basePanel.add(createPredictionCheckpointPanel(), gbc);
        
        this.predictionDataPanel = new InputDataPanel("Data");
        gbc.gridy++;
        basePanel.add(this.predictionDataPanel, gbc);

        gbc.gridy++;
        basePanel.add(createPredictionConfigPanel(), gbc);

        gbc.anchor = GridBagConstraints.SOUTH;
        gbc.gridy++;
        basePanel.add(createPredictionProgressPanel(), gbc);
        
        return basePanel;
    }

    protected JPanel createTrainingConfigPanel() {
        JPanel configPanel = new JPanel();
        configPanel.setBorder(BorderFactory.createTitledBorder("Configuration"));
        configPanel.setLayout(new GridBagLayout());
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 5, 2, 5);
        gbc.weighty = 1.0;
        
        // Experiment Name
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.1;
        // gbc.ipady = 5;
        configPanel.add(new JLabel("Experiment Name:", SwingConstants.RIGHT), gbc);
        
        this.experimentNameField = new JTextField("n2v");
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.8;
        gbc.gridx = 1;
        gbc.ipady = 0;
        configPanel.add(this.experimentNameField, gbc);
    
        // axes
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 0.1;
        gbc.gridx = 0;
        gbc.gridy++;
        configPanel.add(new JLabel("Input Axes:", SwingConstants.RIGHT), gbc);
        
        this.axesCombobox = new AxesComboBox();
        this.axesCombobox.setSelectedIndex(0);
        
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.8;
        gbc.gridx = 1;
        configPanel.add(this.axesCombobox, gbc);
    
        // YX Patch Size
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 0.1;
        gbc.gridx = 0;
        gbc.gridy++;
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
        this.patchZSpin = new JSpinner(new SpinnerNumberModel(8, 4, 998, 2));
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

    protected JPanel createTrainingProgressPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 5, 0, 5);
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        
        this.trainProgressBar = new JProgressBar(0, 100);
        this.trainProgressBar.setStringPainted(true);
        this.trainProgressBar.setMaximum(1);
        panel.add(this.trainProgressBar, gbc);
        
        gbc.insets = new Insets(0, 5, 0, 5);
        gbc.gridy++;
        this.valProgressBar = new JProgressBar(0, 100);
        this.valProgressBar.setStringPainted(true);
        this.valProgressBar.setMaximum(1);
        panel.add(this.valProgressBar, gbc);
        
        this.trainButton = new JButton("Train");
        gbc.insets = new Insets(5, 35, 0, 35);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridwidth = 1;
        gbc.weightx = 1.0;
        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(this.trainButton, gbc);
        
        this.trainStopButton = new JButton("Stop");
        this.trainStopButton.setEnabled(false);
        gbc.gridx = 1;
        panel.add(this.trainStopButton, gbc);
        
        return panel;
    }

    protected JPanel createPredictionCheckpointPanel() {
        // model checkpoint selection panel
        JPanel checkpointPanel = new JPanel(new GridBagLayout());
        checkpointPanel.setBorder(BorderFactory.createTitledBorder("Model Checkpoint"));
        
        JRadioButton useTrainedModelRButton = new JRadioButton("From Trained Model");
        useTrainedModelRButton.setSelected(true);
        JRadioButton fromDiskRButton = new JRadioButton("Load Model from Disk");
        ButtonGroup checkpointGroup = new ButtonGroup();
        checkpointGroup.add(useTrainedModelRButton);
        checkpointGroup.add(fromDiskRButton);
        // add a textfield and a button to choose the checkpoint file from disk
        JTextField checkpointField = new JTextField();
        checkpointField.setEditable(false);
        JButton browseCheckpointButton = new JButton("Load");
        browseCheckpointButton.setEnabled(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 5, 2, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        checkpointPanel.add(useTrainedModelRButton, gbc);

        gbc.gridy++;
        checkpointPanel.add(fromDiskRButton, gbc);

        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.weightx = 1.0;
        checkpointPanel.add(checkpointField, gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 0.0;
        checkpointPanel.add(browseCheckpointButton, gbc);
        
        this.checkpointChooser.setMultiSelectionEnabled(false);
        this.checkpointChooser.setAcceptAllFileFilterUsed(false);
        this.checkpointChooser.setFileFilter(
            new FileNameExtensionFilter(
                "Model Checkpoint Files (.ckpt)",
                "ckpt"
            )
        );
        //actions
        fromDiskRButton.addChangeListener(e -> {
            browseCheckpointButton.setEnabled(fromDiskRButton.isSelected());
            this.modelFromDisk = fromDiskRButton.isSelected();
        });
        
        browseCheckpointButton.addActionListener(e -> {
            int returnVal = this.checkpointChooser.showOpenDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                this.checkpointPath = this.checkpointChooser.getSelectedFile().getAbsolutePath();
                checkpointField.setText(this.checkpointPath);
            }
        });

        return checkpointPanel;
    }

    protected JPanel createPredictionConfigPanel() {
        JPanel configPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        configPanel.setBorder(BorderFactory.createTitledBorder("Configuration"));
        
        // tile prediction checkbox
        this.tilingCheckBox = new JCheckBox("Tile Prediction");
        this.tilingCheckBox.setSelected(true);
        gbc.insets = new Insets(2, 5, 2, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.0;
        configPanel.add(tilingCheckBox, gbc);
        
        // XY tile size
        // gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(2,12, 2, 5);
        gbc.gridwidth = 2;
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weightx = 0.2;
        configPanel.add(
            new JLabel("XY Tile Size:", SwingConstants.LEFT),
            gbc
        );
        this.xyTileSizeSpin = new JSpinner(
            new SpinnerNumberModel(128, 4, Integer.MAX_VALUE, 2)
        );
        gbc.gridx = 1;
        gbc.weightx = 0.8;
        configPanel.add(this.xyTileSizeSpin, gbc);
        
        // Z tile size
        this.pred3DCheckBox = new JCheckBox("3D Prediction");
        gbc.gridx = 1;
        gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.weightx = 0.2;
        configPanel.add(this.pred3DCheckBox, gbc);
        
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weightx = 0.2;
        configPanel.add(
            new JLabel("Z Tile Size:", SwingConstants.LEFT),
            gbc
        );
        this.zTileSizeSpin = new JSpinner(
            new SpinnerNumberModel(8, 2, Integer.MAX_VALUE, 2)
        );
        this.zTileSizeSpin.setEnabled(false);
        gbc.gridx = 1;
        gbc.weightx = 0.8;
        configPanel.add(this.zTileSizeSpin, gbc);

        // batch size
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weightx = 0.2;
        configPanel.add(
            new JLabel("Batch Size:", SwingConstants.LEFT),
            gbc
        );
        this.predBatchSizeSpin = new JSpinner(
            new SpinnerNumberModel(1, 1, Integer.MAX_VALUE, 1)
        );
        gbc.gridx = 1;
        gbc.weightx = 0.8;
        configPanel.add(this.predBatchSizeSpin, gbc);
        
        // actions
        this.pred3DCheckBox.addActionListener(
            e -> {this.zTileSizeSpin.setEnabled(this.pred3DCheckBox.isSelected());}
        );
        this.tilingCheckBox.addActionListener(
            e -> {
                this.xyTileSizeSpin.setEnabled(this.tilingCheckBox.isSelected());
                this.pred3DCheckBox.setEnabled(this.tilingCheckBox.isSelected());
                this.zTileSizeSpin.setEnabled(this.tilingCheckBox.isSelected() && this.pred3DCheckBox.isSelected());
            }
        );

        return configPanel;
    }

    protected JPanel createPredictionProgressPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        // panel.setBorder(BorderFactory.createTitledBorder("Configuration"));
        gbc.insets = new Insets(2, 5, 5, 5);
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.gridx = 0;
        gbc.gridy = 0;

        this.predProgressBar = new JProgressBar();
        this.predProgressBar.setStringPainted(true);
        panel.add(this.predProgressBar, gbc);

        this.predictionButton = new JButton("Predict");
        gbc.insets = new Insets(5, 35, 0, 35);
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(this.predictionButton, gbc);

        this.predictionStopButton = new JButton("Stop");
        gbc.gridx++;
        panel.add(this.predictionStopButton, gbc);

        return panel;
    }

    private void selectedImageChanged(String imgName) {
        this.imgName = imgName;
        ImagePlus imgp = WindowManager.getImage(imgName);
        if (imgp == null) {
            return;
        }
        // width, height, nChannels, nSlices, nFrames
        int[] dims = imgp.getDimensions();
        this.numChannels = dims[2];
        this.numSlices = dims[3];
        this.numFrames = dims[4];
        System.out.println("Image dimensions: " + Arrays.toString(dims));

        String ex_name = imgName == null ? "n2v" : getImageStemName(imgName) + "_n2v";
        this.experimentNameField.setText(ex_name);

        // axes
        String extraAxes = "";
        if (this.numChannels != null && this.numChannels > 1) {
            extraAxes += "C";
        }
        if (this.numSlices != null && this.numSlices > 1) {
            extraAxes += "Z";
        }
        if (this.numFrames != null && this.numFrames > 1) {
            extraAxes += "T";
        }
        this.axesCombobox.setSelectedItem("XY" + extraAxes);
    }

    private void fromDiskSelected() {
        this.imgName = null;
        this.numChannels = null;
        this.numSlices = null;
        this.numFrames = null;
        this.experimentNameField.setText("n2v");
        this.axesCombobox.setSelectedIndex(0);
    }

    private String getImageStemName(String imgName) {
        // drop the file extension from the image name if it exists
        if (imgName.contains(".")) {
            return imgName.substring(0, imgName.lastIndexOf("."));
        }
        return imgName;
    }

}
