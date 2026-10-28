package org.careamics.fiji.gui;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
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

import ij.WindowManager;


public class InputDataPanel extends JPanel {
    public static final String FROM_DISK = "fromDisk";
    public static final String IMAGE_SELECTED = "imageSelected";
    
    public boolean fromDisk;
    public String selectedImage;
    protected final JFileChooser fileChooser = new JFileChooser();
    
    
    public InputDataPanel(String title) {
        if (title == null || title.isEmpty()) {
            title = "Data";
        }
        
        this.setBorder(BorderFactory.createTitledBorder(title));
        this.setLayout(new GridBagLayout());
        
        // add radiobuttons for selecting from opened images or from file
        JRadioButton selectImageRadio = new JRadioButton("Select Image", true);
        JRadioButton fromFileRadio = new JRadioButton("From File/Folder");
        ButtonGroup inputGroup = new ButtonGroup();
        inputGroup.add(selectImageRadio);
        inputGroup.add(fromFileRadio);
        
        // image source selection components
        JComboBox<String> imageSourceCombo = new JComboBox<>();
        for (String img_title : WindowManager.getImageTitles()) {
            imageSourceCombo.addItem(img_title);
        }
        imageSourceCombo.setSelectedIndex(-1);
        imageSourceCombo.setEditable(false);
        // button to refresh the list of currently opened images
        JButton refreshButton = new JButton("Refresh");
        
        // file selection components
        JTextField fileField = new JTextField();
        fileField.setEditable(false);
        JButton browseButton = new JButton("Browse");
        browseButton.setEnabled(false);
        
        JPanel imgSourcePanel = new JPanel(new BorderLayout());
        imgSourcePanel.add(selectImageRadio, BorderLayout.NORTH);
        imgSourcePanel.add(imageSourceCombo, BorderLayout.CENTER);
        imgSourcePanel.add(refreshButton, BorderLayout.EAST);
        
        GridBagConstraints gbc = new GridBagConstraints();
        // gbc.anchor = GridBagConstraints.NORTH;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(2, 5, 2, 5);
        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        this.add(imgSourcePanel, gbc);
        
        JPanel fileSourcePanel = new JPanel(new BorderLayout());
        fileSourcePanel.add(fromFileRadio, BorderLayout.NORTH);
        fileSourcePanel.add(fileField, BorderLayout.CENTER);
        fileSourcePanel.add(browseButton, BorderLayout.EAST);
        
        gbc.gridy++;
        this.add(fileSourcePanel, gbc);
        
        configureFileChooser();
        
        // actions
        refreshButton.addActionListener(e -> {
            imageSourceCombo.removeAllItems();
            for (String img_title : WindowManager.getImageTitles()) {
                imageSourceCombo.addItem(img_title);
            }
        });

        fromFileRadio.addChangeListener(e -> {
            browseButton.setEnabled(fromFileRadio.isSelected());
            imageSourceCombo.setEnabled(!fromFileRadio.isSelected());
            refreshButton.setEnabled(!fromFileRadio.isSelected());
            
            this.firePropertyChange(FROM_DISK, null, fromFileRadio.isSelected());
        });

        browseButton.addActionListener(e -> {
            int returnVal = this.fileChooser.showOpenDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                this.selectedImage = this.fileChooser.getSelectedFile().getAbsolutePath();
                this.fromDisk = true;
                fileField.setText(this.selectedImage);
            }
        });

        imageSourceCombo.addActionListener(e -> {
            String imgName = (String) imageSourceCombo.getSelectedItem();
            this.selectedImage = imgName;
            this.fromDisk = false;
            this.firePropertyChange(IMAGE_SELECTED, null, imgName);
        });
    }

    protected void configureFileChooser() {
        this.fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        this.fileChooser.setMultiSelectionEnabled(false);
        this.fileChooser.setAcceptAllFileFilterUsed(false);
        FileNameExtensionFilter imageFilter = new FileNameExtensionFilter(
            "Image Files (tif, jpg, png, czi, zarr)",
            "jpg", "jpeg", "png", "tif", "tiff", "czi", "zarr"
        );
        this.fileChooser.setFileFilter(imageFilter);
    }

}
