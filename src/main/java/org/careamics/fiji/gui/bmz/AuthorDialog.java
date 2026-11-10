package org.careamics.fiji.gui.bmz;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;


public class AuthorDialog extends JDialog {

    public static final String AUTHOR_READY = "authorReady";

    private JTextField nameTextField;
    private JTextField emailTextField;
    private JTextField affiliationTextField;
    private JTextField gitUserTextField;
    private JTextField orcidTextField;
    private JButton submitButton;
    private JButton cancelButton;

    private AuthorData author;

    public static void main(String[] args) {
        // JFrame frame = new JFrame();
        Dialog frame = new Dialog((Frame) null);
        AuthorDialog authorDialog = new AuthorDialog(frame);
        authorDialog.setVisible(true);
    }

    public AuthorDialog(Dialog parent) {
        this(parent, null);
    }

    public AuthorDialog(Dialog parent, AuthorData author) {
        super(parent, "Author", true);
        this.author = author;

        JPanel mainPanel = createAuthorPanel();
        // layoutComponents();

        // Set values for editing mode
        if (this.author != null) {
            populateFields();
        }

        setContentPane(mainPanel);
        setMinimumSize(new Dimension(380, 250));
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(parent);
    }

    /**
     * Initializes all UI components.
     */
    private JPanel createAuthorPanel() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 5, 0, 5);
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy = 0;

        nameTextField = new JTextField();
        nameTextField.setToolTipText("Name of the author (Cannot have / or \\).");
        mainPanel.add(new JLabel("Name (required):"), gbc);
        gbc.gridx++;
        mainPanel.add(nameTextField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        mainPanel.add(new JLabel("Email:"), gbc);
        emailTextField = new JTextField();
        gbc.gridx++;
        mainPanel.add(emailTextField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        mainPanel.add(new JLabel("Affiliation:"), gbc);
        affiliationTextField = new JTextField();
        gbc.gridx++;
        mainPanel.add(affiliationTextField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        mainPanel.add(new JLabel("Github User:"), gbc);
        gitUserTextField = new JTextField();
        gitUserTextField.setToolTipText("Github username");
        gbc.gridx++;
        mainPanel.add(gitUserTextField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        mainPanel.add(new JLabel("ORCID:"), gbc);
        orcidTextField = new JTextField();
        orcidTextField.setToolTipText(
            "ORCID: Open Researcher and Contributor ID. A persistent digital identifier for researchers."
        );
        gbc.gridx++;
        mainPanel.add(orcidTextField, gbc);
    
        // buttons
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridBagLayout());
    
        submitButton = new JButton("Save");
        submitButton.addActionListener(e -> submitAuthor());
        gbc.insets = new Insets(5, 15, 0, 15);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridwidth = 1;
        gbc.weightx = 1.0;
        gbc.gridx = 0;
        gbc.gridy++;
        buttonPanel.add(submitButton, gbc);
    
        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dispose());
        gbc.gridx = 1;
        buttonPanel.add(cancelButton, gbc);
    
        // Add button panel to main panel
        gbc.insets = new Insets(10, 5, 0, 5);
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = 1;
        gbc.gridx = 1;
        gbc.gridy++;
        mainPanel.add(buttonPanel, gbc);
        
        return mainPanel;
    }

    /**
     * Populates form fields with existing author data (editing mode).
     */
    private void populateFields() {
        if (author == null) return;

        nameTextField.setText(author.getName() != null ? author.getName() : "");
        emailTextField.setText(author.getEmail() != null ? author.getEmail() : "");
        affiliationTextField.setText(
            author.getAffiliation() != null ? author.getAffiliation() : ""
        );
        gitUserTextField.setText(
            author.getGithubUser() != null ? author.getGithubUser() : ""
        );
        orcidTextField.setText(
            author.getOrcid() != null ? author.getOrcid() : ""
        );
    }

    /**
     * Validates and submits the author data.
     */
    private void submitAuthor() {
        Map<String, String> authorData = new HashMap<>();
        authorData.put("name", getValue(nameTextField.getText()));
        authorData.put("email", getValue(emailTextField.getText()));
        authorData.put("affiliation", getValue(affiliationTextField.getText()));
        authorData.put("github_user", getValue(gitUserTextField.getText()));
        authorData.put("orcid", getValue(orcidTextField.getText()));

        try {
            // Validate author data
            AuthorData validatedAuthor = validateAuthor(authorData);
            this.firePropertyChange(AUTHOR_READY, null, validatedAuthor);
            dispose();
        } catch (AuthorValidationException ex) {
            JOptionPane.showMessageDialog(
                this,
                "Validation Error: " + ex.getMessage(),
                "Invalid Author Data",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    /**
     * Converts an empty or whitespace string to null.
     *
     * @param str the input string
     * @return null if string is empty or whitespace, otherwise the trimmed string
     */
    private String getValue(String str) {
        if (str == null || str.trim().isEmpty()) {
            return null;
        }
        return str.trim();
    }

    /**
     * Validates author data.
     *
     * @param authorData the author data map
     * @return validated AuthorData object
     * @throws AuthorValidationException if validation fails
     */
    private AuthorData validateAuthor(Map<String, String> authorData)
        throws AuthorValidationException {
        String name = authorData.get("name");

        // Name is required
        if (name == null || name.trim().isEmpty()) {
            throw new AuthorValidationException("Name is required");
        }

        // Name cannot contain forward slash or backslash
        if (name.contains("/") || name.contains("\\")) {
            throw new AuthorValidationException(
                "Name cannot contain / or \\ characters"
            );
        }

        return new AuthorData(
            name,
            authorData.get("email"),
            authorData.get("affiliation"),
            authorData.get("github_user"),
            authorData.get("orcid")
        );
    }

    /**
     * Exception thrown when author validation fails.
     */
    public static class AuthorValidationException extends Exception {
        public AuthorValidationException(String message) {
            super(message);
        }

        public AuthorValidationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Data class representing author information.
     */
    public static class AuthorData {
        private final String name;
        private final String email;
        private final String affiliation;
        private final String githubUser;
        private final String orcid;

        public AuthorData(
            String name,
            String email,
            String affiliation,
            String githubUser,
            String orcid
        ) {
            this.name = name;
            this.email = email;
            this.affiliation = affiliation;
            this.githubUser = githubUser;
            this.orcid = orcid;
        }

        public HashMap<String, Object> toMap() {
            HashMap<String, Object> map = new HashMap<>();
            map.put("name", name);
            map.put("email", email);
            map.put("affiliation", affiliation);
            map.put("github_user", githubUser);
            map.put("orcid", orcid);
            return map;
        }

        public String getName() {
            return name;
        }

        public String getEmail() {
            return email;
        }

        public String getAffiliation() {
            return affiliation;
        }

        public String getGithubUser() {
            return githubUser;
        }

        public String getOrcid() {
            return orcid;
        }

        @Override
        public String toString() {
            return "AuthorData{" +
                "name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", affiliation='" + affiliation + '\'' +
                ", githubUser='" + githubUser + '\'' +
                ", orcid='" + orcid + '\'' +
                '}';
        }
    }
}
