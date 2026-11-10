package org.careamics.fiji.gui.bmz;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.careamics.fiji.gui.bmz.AuthorDialog.AuthorData;


public class ExportDialog extends JDialog {

    public static final String BMZ_EXPORT = "BMZ_EXPORT";

    public static final String MODEL_NAME = "model_name";
    public static final String GENERAL_DESCRIPTION = "general_description";
    public static final String DATA_DESCRIPTION = "data_description";
    public static final String AUTHORS = "authors";
    public static final String COVER_IMAGE = "cover_image";

    private static final String TITLE = "CAREamics";
    private static final int MIN_MODEL_NAME_LENGTH = 5;

    private final String defaultCoverImage;
    private String uploadedCoverImage;
    private final HashMap<String, Object> modelSpecs = new HashMap<>();
    private final List<AuthorData> authors = new ArrayList<>();

    private JTextField nameTextField;
    private JTextArea descriptionTextArea;
    private JTextArea dataDescriptionTextArea;
    private DefaultListModel<String> authorsListModel;
    private JList<String> authorsList;
    private JLabel imageLabel;

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        // Dialog frame = new Dialog((Frame) null);
        ExportDialog dialog = new ExportDialog(frame, null, null);
        dialog.setVisible(true);
    }

    public ExportDialog(JFrame parent, String defaultCoverImage, HashMap<String, Object> modelSpecs) {
        super(parent, "BMZ Export", true);
        this.defaultCoverImage = defaultCoverImage;
        if (modelSpecs != null) {
            this.modelSpecs.putAll(modelSpecs);
        }

        setContentPane(createPanel());
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        pack();
        setLocationRelativeTo(parent);

        showCoverImage(defaultCoverImage);
    }

    private JPanel createPanel() {
        nameTextField = new JTextField();
        nameTextField.setText((String) modelSpecs.get(MODEL_NAME));
        nameTextField.setToolTipText("At least 5 characters");

        descriptionTextArea = createTextArea(5, "Enter a short description of your model.");
        descriptionTextArea.setText((String) modelSpecs.get(GENERAL_DESCRIPTION));
        
        dataDescriptionTextArea = createTextArea(4,
            "Describe the data that you used for training the model.\n"
                + "Use Markdown formatting and must include a 'Validation' header "
                + "(like ### Validation) describing validation metrics & methods.");

        authorsListModel = new DefaultListModel<>();
        authorsList = new JList<>(authorsListModel);
        JScrollPane authorsScroll = new JScrollPane(authorsList);
        authorsScroll.setPreferredSize(new Dimension(300, 60));

        JButton addAuthorButton = new JButton("Add");
        addAuthorButton.addActionListener(e -> openNewAuthor());
        JButton editAuthorButton = new JButton("Edit");
        editAuthorButton.addActionListener(e -> openEditAuthor());
        JButton removeAuthorButton = new JButton("Remove");
        removeAuthorButton.addActionListener(e -> removeAuthor());

        JPanel authorButtons = new JPanel(new GridLayout(0, 1, 0, 5));
        authorButtons.add(addAuthorButton);
        authorButtons.add(editAuthorButton);
        authorButtons.add(removeAuthorButton);

        JPanel authorsPanel = new JPanel(new BorderLayout(5, 0));
        authorsPanel.add(authorsScroll, BorderLayout.CENTER);
        authorsPanel.add(authorButtons, BorderLayout.EAST);

        imageLabel = new JLabel();
        JScrollPane imageScroll = new JScrollPane(imageLabel);
        imageScroll.setPreferredSize(new Dimension(300, 200));
        imageScroll.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        JButton defaultCoverButton = new JButton("Default Cover Image");
        defaultCoverButton.addActionListener(e -> {
            uploadedCoverImage = null;
            showCoverImage(defaultCoverImage);
        });
        JButton uploadCoverButton = new JButton("Upload Cover Image");
        uploadCoverButton.addActionListener(e -> uploadCoverImage());

        JPanel coverButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        coverButtons.add(defaultCoverButton);
        coverButtons.add(uploadCoverButton);

        JPanel coverPanel = new JPanel(new BorderLayout(0, 5));
        coverPanel.add(imageScroll, BorderLayout.CENTER);
        coverPanel.add(coverButtons, BorderLayout.SOUTH);

        JButton exportButton = new JButton("Export");
        exportButton.setMnemonic(KeyEvent.VK_E);
        exportButton.addActionListener(e -> submit());
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setMnemonic(KeyEvent.VK_C);
        cancelButton.addActionListener(e -> dispose());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        buttonPanel.add(exportButton);
        buttonPanel.add(cancelButton);

        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        addRow(mainPanel, 0, "Model Name:", nameTextField);
        addRow(mainPanel, 1, "General Description:", new JScrollPane(descriptionTextArea));
        addRow(mainPanel, 2, "Data Description:", new JScrollPane(dataDescriptionTextArea));
        addRow(mainPanel, 3, "Authors:", authorsPanel);
        addRow(mainPanel, 4, "Cover Image:", coverPanel);

        GridBagConstraints buttonConstraints = new GridBagConstraints();
        buttonConstraints.gridx = 0;
        buttonConstraints.gridy = 5;
        buttonConstraints.gridwidth = 2;
        buttonConstraints.anchor = GridBagConstraints.EAST;
        buttonConstraints.insets = new Insets(10, 0, 0, 0);
        mainPanel.add(buttonPanel, buttonConstraints);

        return mainPanel;
    }

    private static JTextArea createTextArea(int rows, String tooltip) {
        JTextArea area = new JTextArea(rows, 30);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setToolTipText(tooltip);
        return area;
    }

    private static void addRow(JPanel panel, int row, String label, Component field) {
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = row;
        labelConstraints.anchor = GridBagConstraints.NORTHWEST;
        labelConstraints.insets = new Insets(4, 4, 4, 8);
        panel.add(new JLabel(label), labelConstraints);

        GridBagConstraints fieldConstraints = new GridBagConstraints();
        fieldConstraints.gridx = 1;
        fieldConstraints.gridy = row;
        fieldConstraints.weightx = 1.0;
        fieldConstraints.fill = GridBagConstraints.BOTH;
        fieldConstraints.insets = new Insets(4, 0, 4, 0);
        panel.add(field, fieldConstraints);
    }

    private void openNewAuthor() {
        AuthorDialog dialog = new AuthorDialog(ExportDialog.this);
        dialog.addPropertyChangeListener(
            AuthorDialog.AUTHOR_READY, evt -> addAuthor((AuthorData) evt.getNewValue()));
        dialog.setVisible(true);
    }

    private void openEditAuthor() {
        int index = authorsList.getSelectedIndex();
        if (index < 0) {
            return;
        }
        AuthorDialog dialog = new AuthorDialog(ExportDialog.this, authors.get(index));
        dialog.addPropertyChangeListener(
            AuthorDialog.AUTHOR_READY, evt -> updateAuthor(index, (AuthorData) evt.getNewValue()));
        dialog.setVisible(true);
    }

    private void removeAuthor() {
        int index = authorsList.getSelectedIndex();
        if (index < 0) {
            return;
        }
        int reply = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to remove the selected author?",
            TITLE,
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
        if (reply == JOptionPane.YES_OPTION) {
            authors.remove(index);
            authorsListModel.remove(index);
        }
    }

    private void addAuthor(AuthorData author) {
        authors.add(author);
        authorsListModel.addElement(author.getName());
    }

    private void updateAuthor(int index, AuthorData author) {
        authors.set(index, author);
        authorsListModel.set(index, author.getName());
    }

    private void uploadCoverImage() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Cover Image");
        chooser.setFileFilter(new FileNameExtensionFilter(
            "Image Files (*.png, *.jpg, *.bmp)", "png", "jpg", "bmp"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            uploadedCoverImage = chooser.getSelectedFile().getAbsolutePath();
            showCoverImage(uploadedCoverImage);
        }
    }

    private void showCoverImage(String path) {
        BufferedImage image = null;
        if (path != null) {
            try {
                image = ImageIO.read(new File(path));
            } catch (IOException ignored) {
                // Unreadable image: leave the preview empty.
            }
        }
        imageLabel.setIcon(image != null ? new ImageIcon(image) : null);
    }

    private String getCoverImage() {
        return uploadedCoverImage != null ? uploadedCoverImage : defaultCoverImage;
    }

    private void submit() {
        if (nameTextField.getText().trim().length() < MIN_MODEL_NAME_LENGTH) {
            showError("Model name must be at least 5 characters long!");
            return;
        }
        if (descriptionTextArea.getText().trim().isEmpty()) {
            showError("General description is required!");
            return;
        }
        if (dataDescriptionTextArea.getText().trim().isEmpty()) {
            showError("Data description is required!");
            return;
        }
        if (authors.isEmpty()) {
            showError("At least one author is required!");
            return;
        }
        String coverImage = getCoverImage();
        if (coverImage == null || coverImage.isEmpty()) {
            showError("A cover image is required!");
            return;
        }

        HashMap<String, Object> formData = new HashMap<>();
        formData.put(MODEL_NAME, nameTextField.getText());
        formData.put(GENERAL_DESCRIPTION, descriptionTextArea.getText());
        formData.put(DATA_DESCRIPTION, dataDescriptionTextArea.getText());
        formData.put(COVER_IMAGE, coverImage);
        // convert authors to a list of maps
        ArrayList<HashMap<String, Object>> _authors = new ArrayList<>();
        for (AuthorData a:authors) {
            _authors.add(a.toMap());
        }
        formData.put(AUTHORS, _authors);

        firePropertyChange(BMZ_EXPORT, null, formData);
        dispose();
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, TITLE, JOptionPane.ERROR_MESSAGE);
    }
}
