package org.careamics.fiji.gui;

import ij.gui.GenericDialog;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;


public class MainGUI extends JFrame {
    protected JSpinner numEpochsSpin;

    public Integer numEpochs;

    public static void main(final String[] args) {
        final MainGUI mainGUI = new MainGUI();
    }


    public MainGUI() {
        super("CAREamics Configuration");
        setAlwaysOnTop(true);
        // super(parent, modal);
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

        add(mainPanel, BorderLayout.CENTER);

        // add "Go" button
        JButton goButton = new JButton("Go");
        goButton.addActionListener(e -> {
            // Handle the "Go" button click event here
            this.numEpochs = (Integer) numEpochsSpin.getValue();
            System.out.println("Number of Epochs: " + this.numEpochs);
            // close the GUI
            dispose();
        });
        add(goButton, BorderLayout.SOUTH);

        pack();
        setSize(420, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setVisible(true);


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
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0.1;
        
        configPanel.add(new JLabel("Number of Epochs:", SwingConstants.RIGHT), gbc);
        
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.8;
        this.numEpochsSpin = new JSpinner(new SpinnerNumberModel(30, 1, 999, 1));
        configPanel.add(this.numEpochsSpin, gbc);
        
        return configPanel;
    }


}
