package org.careamics.fiji.gui;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileNameExtensionFilter;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;


public class InputDataPanel extends JPanel {
    
    public boolean fromFile;
    public String selectedFile;
    public final JFileChooser fileChooser = new JFileChooser();

    public InputDataPanel(String title) {
        if (title == null || title.isEmpty()) {
            title = "Data";
        }
        
        this.setBorder(BorderFactory.createTitledBorder(title));
        this.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.gridwidth = 2;
        gbc.gridheight = 1;
        
        // train image
        // add radiobuttons for selecting current image or from file
        JRadioButton currentImageRadio = new JRadioButton("Current Image", true);
        JRadioButton fromFileRadio = new JRadioButton("From File/Folder");
        ButtonGroup inputGroup = new ButtonGroup();
        inputGroup.add(currentImageRadio);
        inputGroup.add(fromFileRadio);
        JPanel radioPanel = new JPanel(new BorderLayout());
        radioPanel.add(currentImageRadio, BorderLayout.WEST);
        radioPanel.add(fromFileRadio, BorderLayout.EAST);
        
        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.weighty = 0.2;
        gbc.ipady = 5;
        this.add(radioPanel, gbc);
        
        // add select input image file
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.weighty = 0.1;
        gbc.ipady = 0;
        this.add(new JLabel("Selected Image:", SwingConstants.LEFT), gbc);
        
        JTextField fileField = new JTextField();
        fileField.setEditable(false);
        JButton browseButton = new JButton("Browse");
        browseButton.setEnabled(false);
        JPanel filePanel = new JPanel(new BorderLayout());
        filePanel.add(fileField);
        filePanel.add(browseButton, BorderLayout.EAST);
        
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.weighty = 0.7;
        this.add(filePanel, gbc);
        
        // actions
        fromFileRadio.addChangeListener(e -> {
            browseButton.setEnabled(fromFileRadio.isSelected());
            this.fromFile = fromFileRadio.isSelected();
        });
        browseButton.addActionListener(e -> {
            int returnVal = this.fileChooser.showOpenDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                this.selectedFile = this.fileChooser.getSelectedFile().getAbsolutePath();
                fileField.setText(this.selectedFile);
            }
        });
    }

    protected void configfileChooser() {
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        fileChooser.setMultiSelectionEnabled(false);
        fileChooser.setAcceptAllFileFilterUsed(false);
        FileNameExtensionFilter imageFilter = new FileNameExtensionFilter(
            "Image Files (tif, jpg, png, czi, zarr)",
            "jpg", "jpeg", "png", "tif", "tiff", "czi", "zarr"
        );
        fileChooser.setFileFilter(imageFilter);
    }

}
