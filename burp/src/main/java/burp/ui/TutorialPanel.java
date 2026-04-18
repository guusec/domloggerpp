package burp.ui;

import burp.IBurpExtenderCallbacks;
import burp.server.WebhookServer;

import javax.swing.*;
import java.awt.*;

/**
 * Tutorial panel with setup instructions
 */
public class TutorialPanel extends JPanel {
    private final WebhookServer webhookServer;
    private final IBurpExtenderCallbacks callbacks;

    public TutorialPanel(WebhookServer webhookServer, IBurpExtenderCallbacks callbacks) {
        this.webhookServer = webhookServer;
        this.callbacks = callbacks;

        initializeUI();
    }

    private void initializeUI() {
        setLayout(new BorderLayout());

        JTextArea tutorialArea = new JTextArea();
        tutorialArea.setEditable(false);
        tutorialArea.setLineWrap(true);
        tutorialArea.setWrapStyleWord(true);
        tutorialArea.setFont(new Font("SansSerif", Font.PLAIN, 14));
        tutorialArea.setMargin(new Insets(10, 10, 10, 10));

        String webhookUrl = "http://localhost:" + webhookServer.getPort() + "/webhook";

        String tutorial = "DOMLogger++ Burp Suite Extension - Setup Tutorial\n\n" +
            "=== Step 1: Install the DOMLogger++ Browser Extension ===\n\n" +
            "Install the DOMLogger++ browser extension from:\n" +
            "- Firefox: https://addons.mozilla.org/en-US/firefox/addon/domloggerpp\n" +
            "- Chrome: https://chromewebstore.google.com/detail/domlogger++/lkpfjhmpbmpflldmdpdoabimdbaclolp\n\n" +
            "=== Step 2: Configure the Browser Extension ===\n\n" +
            "1. Click on the DOMLogger++ extension icon in your browser\n" +
            "2. Go to the extension settings/options\n" +
            "3. Navigate to the 'Burp Suite' or 'Webhook' section\n" +
            "4. Enable the webhook integration\n" +
            "5. Enter the webhook URL: " + webhookUrl + "\n\n" +
            "=== Step 3: Configure AI (Optional) ===\n\n" +
            "1. Switch to the 'AI' tab in this extension\n" +
            "2. Get an API key from https://openrouter.ai/\n" +
            "3. Enter your OpenRouter API key\n" +
            "4. Select your preferred AI model\n" +
            "5. Click 'Test' to verify your API key\n" +
            "6. Click 'Save Configuration'\n\n" +
            "=== Step 4: Start Testing ===\n\n" +
            "1. Browse to your target application through Burp's proxy\n" +
            "2. The DOMLogger++ extension will automatically detect JavaScript sinks\n" +
            "3. Findings will appear in the Dashboard tab\n" +
            "4. Use the search bar to filter findings\n" +
            "5. Select findings to view detailed information\n\n" +
            "=== Features ===\n\n" +
            "Dashboard:\n" +
            "- View all JavaScript sink findings\n" +
            "- Advanced search syntax: sink.field.operator:\"value\"\n" +
            "- Keyboard shortcuts (Ctrl+F, Ctrl+R, Ctrl+S, etc.)\n" +
            "- Recording sessions to isolate findings\n" +
            "- Bulk operations: delete, export, AI score\n\n" +
            "AI Integration:\n" +
            "- Automatic exploitability scoring (1-5 scale)\n" +
            "- 100+ models supported via OpenRouter\n" +
            "- Custom prompts with template variables\n" +
            "- Threaded processing for faster analysis\n\n" +
            "Trace Enhancement:\n" +
            "- Extract actual source code from proxy history\n" +
            "- Context around the vulnerable code\n" +
            "- Better understanding of the execution flow\n\n" +
            "=== Webhook Status ===\n\n" +
            "Server: " + (webhookServer.isRunning() ? "RUNNING" : "STOPPED") + "\n" +
            "URL: " + webhookUrl + "\n" +
            "Port: " + webhookServer.getPort() + "\n\n" +
            "=== Support ===\n\n" +
            "For issues, feature requests, or questions:\n" +
            "- GitHub: https://github.com/kevin-mizu/domloggerpp-burp\n" +
            "- Original Project: https://github.com/kevin-mizu/domloggerpp\n\n";

        tutorialArea.setText(tutorial);
        tutorialArea.setCaretPosition(0);

        JScrollPane scrollPane = new JScrollPane(tutorialArea);
        add(scrollPane, BorderLayout.CENTER);
    }
}
