package burp.ui;

import burp.IBurpExtenderCallbacks;
import burp.db.DatabaseManager;
import burp.ai.OpenRouterClient;
import burp.ai.AIProcessor;
import burp.models.AIConfig;
import burp.models.UserPrompt;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * AI configuration panel
 */
public class AIConfigPanel extends JPanel {
    private final DatabaseManager dbManager;
    private final OpenRouterClient aiClient;
    private final AIProcessor aiProcessor;
    private final IBurpExtenderCallbacks callbacks;

    private JTextField apiKeyField;
    private JTextArea systemPromptArea;
    private JCheckBox aiEnabledCheckbox;
    private JComboBox<String> modelComboBox;
    private JSpinner temperatureSpinner;
    private JSpinner threadCountSpinner;
    private JButton saveButton;
    private JButton testButton;

    public AIConfigPanel(DatabaseManager dbManager, OpenRouterClient aiClient, AIProcessor aiProcessor,
                         IBurpExtenderCallbacks callbacks) {
        this.dbManager = dbManager;
        this.aiClient = aiClient;
        this.aiProcessor = aiProcessor;
        this.callbacks = callbacks;

        initializeUI();
        loadConfig();
    }

    private void initializeUI() {
        setLayout(new BorderLayout());

        // Main form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        // AI Enabled
        gbc.gridx = 0;
        gbc.gridy = row;
        formPanel.add(new JLabel("AI Enabled:"), gbc);

        gbc.gridx = 1;
        aiEnabledCheckbox = new JCheckBox();
        formPanel.add(aiEnabledCheckbox, gbc);

        row++;

        // API Key
        gbc.gridx = 0;
        gbc.gridy = row;
        formPanel.add(new JLabel("OpenRouter API Key:"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        apiKeyField = new JTextField(40);
        formPanel.add(apiKeyField, gbc);

        gbc.gridx = 3;
        gbc.gridwidth = 1;
        testButton = new JButton("Test");
        testButton.addActionListener(e -> testApiKey());
        formPanel.add(testButton, gbc);

        row++;

        // Model
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        formPanel.add(new JLabel("Model:"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        modelComboBox = new JComboBox<>(new String[]{
            "openai/gpt-3.5-turbo",
            "openai/gpt-4",
            "openai/gpt-4-turbo",
            "anthropic/claude-3-opus",
            "anthropic/claude-3-sonnet",
            "anthropic/claude-3-haiku"
        });
        modelComboBox.setEditable(true);
        formPanel.add(modelComboBox, gbc);

        row++;

        // Temperature
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        formPanel.add(new JLabel("Temperature:"), gbc);

        gbc.gridx = 1;
        temperatureSpinner = new JSpinner(new SpinnerNumberModel(0.7, 0.0, 2.0, 0.1));
        formPanel.add(temperatureSpinner, gbc);

        row++;

        // Thread Count
        gbc.gridx = 0;
        gbc.gridy = row;
        formPanel.add(new JLabel("Thread Count:"), gbc);

        gbc.gridx = 1;
        threadCountSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));
        formPanel.add(threadCountSpinner, gbc);

        row++;

        // System Prompt
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 4;
        formPanel.add(new JLabel("System Prompt:"), gbc);

        row++;

        gbc.gridy = row;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        systemPromptArea = new JTextArea(10, 60);
        systemPromptArea.setLineWrap(true);
        systemPromptArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(systemPromptArea);
        formPanel.add(scrollPane, gbc);

        add(formPanel, BorderLayout.CENTER);

        // Bottom button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        saveButton = new JButton("Save Configuration");
        saveButton.addActionListener(e -> saveConfig());
        buttonPanel.add(saveButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void loadConfig() {
        try {
            AIConfig config = dbManager.getOrCreateAIConfig();

            aiEnabledCheckbox.setSelected(config.isAiEnabled());
            apiKeyField.setText(config.getOpenrouterApiKey());
            systemPromptArea.setText(config.getSystemPrompt());

            // Set model
            modelComboBox.setSelectedItem(config.getSelectedModel());

            // Set temperature
            temperatureSpinner.setValue(config.getTemperature());

            // Set thread count
            threadCountSpinner.setValue(config.getThreadCount());

        } catch (Exception e) {
            callbacks.printError("Error loading AI config: " + e.getMessage());
        }
    }

    private void saveConfig() {
        try {
            AIConfig config = dbManager.getOrCreateAIConfig();

            config.setAiEnabled(aiEnabledCheckbox.isSelected());
            config.setOpenrouterApiKey(apiKeyField.getText());
            config.setSystemPrompt(systemPromptArea.getText());
            config.setSelectedModel((String) modelComboBox.getSelectedItem());
            config.setTemperature((Double) temperatureSpinner.getValue());
            config.setThreadCount((Integer) threadCountSpinner.getValue());

            dbManager.updateAIConfig(config);

            // Restart AI processor with new thread count
            if (aiProcessor.isRunning()) {
                aiProcessor.stop();
            }
            if (config.isAiEnabled()) {
                aiProcessor.start(config.getThreadCount());
            }

            JOptionPane.showMessageDialog(this, "Configuration saved successfully");
            callbacks.printOutput("AI configuration updated");

        } catch (Exception e) {
            callbacks.printError("Error saving AI config: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error saving configuration: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void testApiKey() {
        String apiKey = apiKeyField.getText();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter an API key", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        testButton.setEnabled(false);
        testButton.setText("Testing...");

        new SwingWorker<OpenRouterClient.ApiKeyVerificationResult, Void>() {
            @Override
            protected OpenRouterClient.ApiKeyVerificationResult doInBackground() {
                return aiClient.verifyApiKey(apiKey);
            }

            @Override
            protected void done() {
                try {
                    OpenRouterClient.ApiKeyVerificationResult result = get();
                    if (result.success) {
                        JOptionPane.showMessageDialog(AIConfigPanel.this,
                            "API key verified successfully!",
                            "Success",
                            JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(AIConfigPanel.this,
                            "API key verification failed: " + result.message,
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(AIConfigPanel.this,
                        "Error testing API key: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                } finally {
                    testButton.setEnabled(true);
                    testButton.setText("Test");
                }
            }
        }.execute();
    }
}
