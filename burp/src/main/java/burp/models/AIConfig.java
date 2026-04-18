package burp.models;

import com.google.gson.annotations.SerializedName;

/**
 * AI configuration for OpenRouter integration
 */
public class AIConfig {
    private Integer id;

    @SerializedName("openrouter_api_key")
    private String openrouterApiKey;

    @SerializedName("system_prompt")
    private String systemPrompt;

    @SerializedName("ai_enabled")
    private boolean aiEnabled;

    @SerializedName("selected_model")
    private String selectedModel;

    private double temperature;

    @SerializedName("thread_count")
    private int threadCount;

    @SerializedName("created_at")
    private String createdAt;

    // Default constructor with sensible defaults
    public AIConfig() {
        this.aiEnabled = true;
        this.selectedModel = "openai/gpt-3.5-turbo";
        this.temperature = 0.7;
        this.threadCount = 1;
        this.systemPrompt = getDefaultSystemPrompt();
    }

    public static String getDefaultSystemPrompt() {
        return "You are an AI security analyst specializing in client-side code vulnerabilities. " +
               "Your task is to assess the exploitability of potential issues described in data.\n\n" +
               "Instructions:\n\n" +
               "1. You will receive plain text describing a potential client-side vulnerability.\n" +
               "2. Evaluate the exploitability of the issue.\n" +
               "3. Return a JSON object containing only the exploitability score as an integer from 1 to 5. " +
               "Use this exact format:\n" +
               "\"\"\"\n{\n  \"score\": X\n}\n\"\"\"\n\n" +
               "1=Very Low: Highly unlikely to be a true vulnerability.\n" +
               "2=Low: Some chance of being vulnerable, but uncommon.\n" +
               "3=Medium: May be vulnerable under certain conditions.\n" +
               "4=High: Likely to be a true vulnerability.\n" +
               "5=Very High: Almost certainly a true vulnerability.\n\n" +
               "4. Do not include reasoning, explanations, or any additional text.\n" +
               "5. Respond only with the JSON. Example:\n" +
               "\"\"\"\n{\n\"score\": 4\n}\n\"\"\"";
    }

    // Getters and setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getOpenrouterApiKey() { return openrouterApiKey; }
    public void setOpenrouterApiKey(String openrouterApiKey) { this.openrouterApiKey = openrouterApiKey; }

    public String getSystemPrompt() { return systemPrompt; }
    public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }

    public boolean isAiEnabled() { return aiEnabled; }
    public void setAiEnabled(boolean aiEnabled) { this.aiEnabled = aiEnabled; }

    public String getSelectedModel() { return selectedModel; }
    public void setSelectedModel(String selectedModel) { this.selectedModel = selectedModel; }

    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }

    public int getThreadCount() { return threadCount; }
    public void setThreadCount(int threadCount) { this.threadCount = threadCount; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
