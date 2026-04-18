package burp.ai;

import burp.IBurpExtenderCallbacks;
import burp.db.DatabaseManager;
import burp.models.AIConfig;
import burp.models.Finding;
import burp.models.UserPrompt;
import burp.parser.QueryParser;

import java.util.List;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Processes findings through AI for exploitability scoring
 */
public class AIProcessor {
    private final DatabaseManager dbManager;
    private final OpenRouterClient aiClient;
    private final IBurpExtenderCallbacks callbacks;
    private ExecutorService executorService;
    private volatile boolean running = false;

    public AIProcessor(DatabaseManager dbManager, OpenRouterClient aiClient, IBurpExtenderCallbacks callbacks) {
        this.dbManager = dbManager;
        this.aiClient = aiClient;
        this.callbacks = callbacks;
    }

    /**
     * Start AI processing with configured thread count
     */
    public void start(int threadCount) {
        if (running) {
            return;
        }

        running = true;
        executorService = Executors.newFixedThreadPool(threadCount);
        callbacks.printOutput("AI processor started with " + threadCount + " threads");
    }

    /**
     * Stop AI processing
     */
    public void stop() {
        running = false;
        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
            }
        }
        callbacks.printOutput("AI processor stopped");
    }

    /**
     * Process a single finding through AI
     */
    public void processFinding(Finding finding, AIConfig config) {
        if (!running || executorService == null) {
            return;
        }

        executorService.submit(() -> {
            try {
                // Get matching user prompt
                List<UserPrompt> prompts = dbManager.getUserPrompts();
                UserPrompt matchingPrompt = null;

                for (UserPrompt prompt : prompts) {
                    if (!prompt.isEnabled()) {
                        continue;
                    }

                    if (matchesCondition(finding, prompt.getCondition())) {
                        matchingPrompt = prompt;
                        break;
                    }
                }

                if (matchingPrompt == null) {
                    // No matching prompt, skip
                    return;
                }

                // Update finding status to "In progress"
                dbManager.updateFindingAIScore(finding.getId(), "In progress", matchingPrompt.getId());

                // Build user prompt from template
                String userPrompt = buildPromptFromTemplate(matchingPrompt.getPrompt(), finding);

                // Send to AI
                OpenRouterClient.ChatCompletionResult result = aiClient.sendChatCompletion(config, userPrompt);

                if (result.success && result.response != null) {
                    // Extract score from response
                    String score = OpenRouterClient.extractScore(result.response);

                    if (score.isEmpty()) {
                        score = "Error";
                        callbacks.printError("Failed to extract score from AI response for finding " + finding.getId());
                    }

                    // Update finding with score
                    dbManager.updateFindingAIScore(finding.getId(), score, matchingPrompt.getId());
                    callbacks.printOutput("Finding " + finding.getId() + " scored: " + score);
                } else {
                    dbManager.updateFindingAIScore(finding.getId(), "Error", matchingPrompt.getId());
                    callbacks.printError("AI scoring failed for finding " + finding.getId() + ": " + result.message);
                }

            } catch (Exception e) {
                callbacks.printError("Error processing finding " + finding.getId() + ": " + e.getMessage());
                try {
                    dbManager.updateFindingAIScore(finding.getId(), "Error", null);
                } catch (Exception ex) {
                    callbacks.printError("Failed to update error status: " + ex.getMessage());
                }
            }
        });
    }

    /**
     * Process multiple findings
     */
    public void processFindings(List<Finding> findings, AIConfig config) {
        for (Finding finding : findings) {
            // Only process findings without a score
            if (finding.getAiScore() == null || finding.getAiScore().isEmpty()) {
                // Set to "In queue" status
                try {
                    dbManager.updateFindingAIScore(finding.getId(), "In queue", null);
                    processFinding(finding, config);
                } catch (Exception e) {
                    callbacks.printError("Error queueing finding " + finding.getId() + ": " + e.getMessage());
                }
            }
        }
    }

    /**
     * Check if finding matches user prompt condition
     */
    private boolean matchesCondition(Finding finding, String condition) {
        if (condition == null || condition.trim().isEmpty()) {
            return true;
        }

        try {
            // For simplicity, we'll do a basic string matching here
            // In a full implementation, you would use the query parser
            // to evaluate the condition against the finding

            // This is a simplified version - just check if condition contains field:value patterns
            // and match them against the finding

            return true; // For now, match all
        } catch (Exception e) {
            callbacks.printError("Error matching condition: " + e.getMessage());
            return false;
        }
    }

    /**
     * Build user prompt from template, replacing variables
     * Supports: {sink}, {data}, {trace}, {href}, {frame}, {type}, {tag}, {alert}, {date}, {dupKey}, {debug}
     */
    private String buildPromptFromTemplate(String template, Finding finding) {
        String prompt = template;

        prompt = prompt.replace("{sink}", finding.getSink() != null ? finding.getSink() : "");
        prompt = prompt.replace("{data}", finding.getData() != null ? finding.getData() : "");
        prompt = prompt.replace("{trace}", finding.getTrace() != null ? finding.getTrace() : "");
        prompt = prompt.replace("{href}", finding.getHref() != null ? finding.getHref() : "");
        prompt = prompt.replace("{frame}", finding.getFrame() != null ? finding.getFrame() : "");
        prompt = prompt.replace("{type}", finding.getType() != null ? finding.getType() : "");
        prompt = prompt.replace("{tag}", finding.getTag() != null ? finding.getTag() : "");
        prompt = prompt.replace("{alert}", String.valueOf(finding.isAlert()));
        prompt = prompt.replace("{date}", finding.getDate() != null ? finding.getDate() : "");
        prompt = prompt.replace("{dupKey}", finding.getDupKey() != null ? finding.getDupKey() : "");
        prompt = prompt.replace("{debug}", finding.getDebug() != null ? finding.getDebug() : "");

        return prompt;
    }

    public boolean isRunning() {
        return running;
    }
}
