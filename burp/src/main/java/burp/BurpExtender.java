package burp;

import burp.db.DatabaseManager;
import burp.ai.OpenRouterClient;
import burp.ai.AIProcessor;
import burp.trace.TraceEnhancer;
import burp.server.WebhookServer;
import burp.ui.MainPanel;
import burp.models.AIConfig;

import javax.swing.*;
import java.awt.*;

/**
 * Main Burp Suite extension entry point for DOMLogger++
 */
public class BurpExtender implements IBurpExtender, ITab {
    private static final String EXTENSION_NAME = "DOMLogger++";
    private static final int DEFAULT_WEBHOOK_PORT = 8089;

    private IBurpExtenderCallbacks callbacks;
    private IExtensionHelpers helpers;

    private DatabaseManager dbManager;
    private OpenRouterClient aiClient;
    private AIProcessor aiProcessor;
    private TraceEnhancer traceEnhancer;
    private WebhookServer webhookServer;
    private MainPanel mainPanel;

    /**
     * Extension initialization
     */
    @Override
    public void registerExtenderCallbacks(IBurpExtenderCallbacks callbacks) {
        this.callbacks = callbacks;
        this.helpers = callbacks.getHelpers();

        // Set extension name
        callbacks.setExtensionName(EXTENSION_NAME);

        // Print banner
        printBanner();

        try {
            // Initialize database
            callbacks.printOutput("Initializing database...");
            dbManager = new DatabaseManager(callbacks);

            // Initialize AI client
            callbacks.printOutput("Initializing AI client...");
            aiClient = new OpenRouterClient(callbacks);

            // Initialize trace enhancer
            callbacks.printOutput("Initializing trace enhancer...");
            traceEnhancer = new TraceEnhancer(callbacks);

            // Initialize AI processor
            callbacks.printOutput("Initializing AI processor...");
            aiProcessor = new AIProcessor(dbManager, aiClient, callbacks);

            // Start AI processor if enabled
            AIConfig aiConfig = dbManager.getOrCreateAIConfig();
            if (aiConfig.isAiEnabled()) {
                aiProcessor.start(aiConfig.getThreadCount());
            }

            // Initialize webhook server
            callbacks.printOutput("Initializing webhook server...");
            webhookServer = new WebhookServer(dbManager, callbacks);

            // Start webhook server
            try {
                webhookServer.start(DEFAULT_WEBHOOK_PORT);
            } catch (Exception e) {
                callbacks.printError("Failed to start webhook server: " + e.getMessage());
                callbacks.printError("You can try restarting Burp or changing the port.");
            }

            // Create UI
            callbacks.printOutput("Creating user interface...");
            SwingUtilities.invokeLater(() -> {
                mainPanel = new MainPanel(dbManager, aiClient, aiProcessor, traceEnhancer, webhookServer, callbacks);

                // Add custom tab to Burp Suite UI
                callbacks.addSuiteTab(BurpExtender.this);

                callbacks.printOutput("Extension loaded successfully!");
                callbacks.printOutput("Configure the DOMLogger++ browser extension to send findings to:");
                callbacks.printOutput("  http://localhost:" + DEFAULT_WEBHOOK_PORT + "/webhook");
            });

        } catch (Exception e) {
            callbacks.printError("Extension initialization failed: " + e.getMessage());
            e.printStackTrace(new java.io.PrintWriter(callbacks.getStderr()));
        }

        // Register extension unload handler
        callbacks.registerExtensionStateListener(new IExtensionStateListener() {
            @Override
            public void extensionUnloaded() {
                cleanup();
            }
        });
    }

    /**
     * Get the extension tab caption
     */
    @Override
    public String getTabCaption() {
        return EXTENSION_NAME;
    }

    /**
     * Get the extension tab UI component
     */
    @Override
    public Component getUiComponent() {
        return mainPanel;
    }

    /**
     * Cleanup resources when extension is unloaded
     */
    private void cleanup() {
        callbacks.printOutput("Cleaning up extension resources...");

        try {
            // Stop AI processor
            if (aiProcessor != null) {
                aiProcessor.stop();
            }

            // Stop webhook server
            if (webhookServer != null) {
                webhookServer.stop();
            }

            // Close database
            if (dbManager != null) {
                dbManager.close();
            }

            callbacks.printOutput("Extension unloaded successfully");
        } catch (Exception e) {
            callbacks.printError("Error during cleanup: " + e.getMessage());
        }
    }

    /**
     * Print extension banner
     */
    private void printBanner() {
        callbacks.printOutput("");
        callbacks.printOutput("╔═══════════════════════════════════════════════════════════╗");
        callbacks.printOutput("║                                                           ║");
        callbacks.printOutput("║                    DOMLogger++ Burp                       ║");
        callbacks.printOutput("║                                                           ║");
        callbacks.printOutput("║     JavaScript Sink Detection & Analysis Extension       ║");
        callbacks.printOutput("║                                                           ║");
        callbacks.printOutput("║     Author: kevin-mizu                                    ║");
        callbacks.printOutput("║     GitHub: github.com/kevin-mizu/domloggerpp-burp        ║");
        callbacks.printOutput("║     Version: 1.0.0                                        ║");
        callbacks.printOutput("║                                                           ║");
        callbacks.printOutput("╚═══════════════════════════════════════════════════════════╝");
        callbacks.printOutput("");
    }
}
