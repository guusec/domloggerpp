package burp.ui;

import burp.IBurpExtenderCallbacks;
import burp.db.DatabaseManager;
import burp.ai.OpenRouterClient;
import burp.ai.AIProcessor;
import burp.trace.TraceEnhancer;
import burp.server.WebhookServer;

import javax.swing.*;
import java.awt.*;

/**
 * Main tabbed panel for the extension
 */
public class MainPanel extends JPanel {
    private final DatabaseManager dbManager;
    private final OpenRouterClient aiClient;
    private final AIProcessor aiProcessor;
    private final TraceEnhancer traceEnhancer;
    private final WebhookServer webhookServer;
    private final IBurpExtenderCallbacks callbacks;

    private JTabbedPane tabbedPane;
    private DashboardPanel dashboardPanel;
    private AIConfigPanel aiConfigPanel;
    private TutorialPanel tutorialPanel;
    private SettingsPanel settingsPanel;

    public MainPanel(DatabaseManager dbManager, OpenRouterClient aiClient, AIProcessor aiProcessor,
                     TraceEnhancer traceEnhancer, WebhookServer webhookServer,
                     IBurpExtenderCallbacks callbacks) {
        this.dbManager = dbManager;
        this.aiClient = aiClient;
        this.aiProcessor = aiProcessor;
        this.traceEnhancer = traceEnhancer;
        this.webhookServer = webhookServer;
        this.callbacks = callbacks;

        initializeUI();
    }

    private void initializeUI() {
        setLayout(new BorderLayout());

        // Create tabbed pane
        tabbedPane = new JTabbedPane();

        // Create panels
        dashboardPanel = new DashboardPanel(dbManager, aiClient, aiProcessor, traceEnhancer, callbacks);
        aiConfigPanel = new AIConfigPanel(dbManager, aiClient, aiProcessor, callbacks);
        tutorialPanel = new TutorialPanel(webhookServer, callbacks);
        settingsPanel = new SettingsPanel(dbManager, callbacks);

        // Add tabs
        tabbedPane.addTab("Dashboard", dashboardPanel);
        tabbedPane.addTab("AI", aiConfigPanel);
        tabbedPane.addTab("Tutorial", tutorialPanel);
        tabbedPane.addTab("Settings", settingsPanel);

        add(tabbedPane, BorderLayout.CENTER);

        // Register keyboard shortcuts for tab navigation
        registerKeyboardShortcuts();
    }

    private void registerKeyboardShortcuts() {
        // Alt+1 = Dashboard, Alt+2 = AI, Alt+3 = Tutorial, Alt+4 = Settings
        InputMap inputMap = getInputMap(WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getActionMap();

        inputMap.put(KeyStroke.getKeyStroke("alt 1"), "switchToDashboard");
        actionMap.put("switchToDashboard", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                tabbedPane.setSelectedIndex(0);
            }
        });

        inputMap.put(KeyStroke.getKeyStroke("alt 2"), "switchToAI");
        actionMap.put("switchToAI", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                tabbedPane.setSelectedIndex(1);
            }
        });

        inputMap.put(KeyStroke.getKeyStroke("alt 3"), "switchToTutorial");
        actionMap.put("switchToTutorial", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                tabbedPane.setSelectedIndex(2);
            }
        });

        inputMap.put(KeyStroke.getKeyStroke("alt 4"), "switchToSettings");
        actionMap.put("switchToSettings", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                tabbedPane.setSelectedIndex(3);
            }
        });
    }

    public void refreshDashboard() {
        if (dashboardPanel != null) {
            dashboardPanel.refresh();
        }
    }
}
