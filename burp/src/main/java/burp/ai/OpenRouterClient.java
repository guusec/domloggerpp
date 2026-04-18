package burp.ai;

import burp.IBurpExtenderCallbacks;
import burp.models.AIConfig;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Client for OpenRouter AI API integration
 */
public class OpenRouterClient {
    private static final String OPENROUTER_BASE_URL = "https://openrouter.ai/api/v1";
    private static final String KEY_ENDPOINT = OPENROUTER_BASE_URL + "/key";
    private static final String CHAT_ENDPOINT = OPENROUTER_BASE_URL + "/chat/completions";
    private static final String MODELS_ENDPOINT = OPENROUTER_BASE_URL + "/models";

    private final OkHttpClient httpClient;
    private final Gson gson;
    private final IBurpExtenderCallbacks callbacks;

    public OpenRouterClient(IBurpExtenderCallbacks callbacks) {
        this.callbacks = callbacks;
        this.gson = new Gson();
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();
    }

    /**
     * Verify API key validity
     */
    public ApiKeyVerificationResult verifyApiKey(String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return new ApiKeyVerificationResult(false, "API key is required", null);
        }

        // Basic format validation
        if (!apiKey.startsWith("sk-or-") || apiKey.length() < 20) {
            return new ApiKeyVerificationResult(false,
                "Invalid API key format. OpenRouter API keys should start with \"sk-or-\" and be at least 20 characters long",
                null);
        }

        Request request = new Request.Builder()
            .url(KEY_ENDPOINT)
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .get()
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String responseBody = response.body().string();
                JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();
                JsonObject keyData = jsonResponse.getAsJsonObject("data");

                String label = keyData.has("label") ? keyData.get("label").getAsString() : "unknown";
                double usage = keyData.has("usage") ? keyData.get("usage").getAsDouble() : 0.0;

                callbacks.printOutput(String.format("OpenRouter API key verified successfully. Label: %s, Usage: %.2f", label, usage));
                return new ApiKeyVerificationResult(true, "API key verified successfully", keyData.toString());
            } else {
                callbacks.printError("OpenRouter API returned status code: " + response.code());
                return new ApiKeyVerificationResult(false,
                    "OpenRouter API returned error: " + response.code(), null);
            }
        } catch (IOException e) {
            callbacks.printError("Failed to verify OpenRouter API token: " + e.getMessage());
            return new ApiKeyVerificationResult(false, "Network error: " + e.getMessage(), null);
        }
    }

    /**
     * Get available models from OpenRouter
     */
    public List<String> getAvailableModels(String apiKey) {
        List<String> models = new ArrayList<>();

        Request request = new Request.Builder()
            .url(MODELS_ENDPOINT)
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .get()
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String responseBody = response.body().string();
                JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();

                if (jsonResponse.has("data")) {
                    jsonResponse.getAsJsonArray("data").forEach(element -> {
                        JsonObject model = element.getAsJsonObject();
                        if (model.has("id")) {
                            models.add(model.get("id").getAsString());
                        }
                    });
                }
            }
        } catch (Exception e) {
            callbacks.printError("Failed to fetch models: " + e.getMessage());
        }

        // Add some popular defaults if the request failed
        if (models.isEmpty()) {
            models.add("openai/gpt-3.5-turbo");
            models.add("openai/gpt-4");
            models.add("openai/gpt-4-turbo");
            models.add("anthropic/claude-3-opus");
            models.add("anthropic/claude-3-sonnet");
            models.add("anthropic/claude-3-haiku");
            models.add("google/gemini-pro");
        }

        return models;
    }

    /**
     * Send a chat completion request
     */
    public ChatCompletionResult sendChatCompletion(AIConfig config, String userPrompt) {
        if (config.getOpenrouterApiKey() == null || config.getOpenrouterApiKey().trim().isEmpty()) {
            return new ChatCompletionResult(false, "API key is required", null, null);
        }

        if (userPrompt == null || userPrompt.trim().isEmpty()) {
            return new ChatCompletionResult(false, "User prompt is required", null, null);
        }

        // Build messages
        List<Map<String, String>> messages = new ArrayList<>();

        Map<String, String> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", config.getSystemPrompt() != null ? config.getSystemPrompt() : "You are a helpful assistant.");
        messages.add(systemMessage);

        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", userPrompt.trim());
        messages.add(userMessage);

        // Build request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", config.getSelectedModel());
        requestBody.put("messages", messages);
        requestBody.put("temperature", config.getTemperature());

        String jsonBody = gson.toJson(requestBody);

        Request request = new Request.Builder()
            .url(CHAT_ENDPOINT)
            .header("Authorization", "Bearer " + config.getOpenrouterApiKey())
            .header("Content-Type", "application/json")
            .header("HTTP-Referer", "https://github.com/kevin-mizu/domloggerpp-burp")
            .header("X-Title", "DOMLogger++ Burp")
            .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String responseBody = response.body().string();
                JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();

                String aiResponse = null;
                if (jsonResponse.has("choices") && jsonResponse.getAsJsonArray("choices").size() > 0) {
                    JsonObject choice = jsonResponse.getAsJsonArray("choices").get(0).getAsJsonObject();
                    if (choice.has("message")) {
                        JsonObject message = choice.getAsJsonObject("message");
                        if (message.has("content")) {
                            aiResponse = message.get("content").getAsString();
                        }
                    }
                }

                Map<String, Integer> usage = new HashMap<>();
                if (jsonResponse.has("usage")) {
                    JsonObject usageObj = jsonResponse.getAsJsonObject("usage");
                    if (usageObj.has("prompt_tokens")) {
                        usage.put("prompt_tokens", usageObj.get("prompt_tokens").getAsInt());
                    }
                    if (usageObj.has("completion_tokens")) {
                        usage.put("completion_tokens", usageObj.get("completion_tokens").getAsInt());
                    }
                    if (usageObj.has("total_tokens")) {
                        usage.put("total_tokens", usageObj.get("total_tokens").getAsInt());
                    }
                }

                if (aiResponse != null) {
                    callbacks.printOutput(String.format("AI response received (prompt: %d, completion: %d, total: %d tokens)",
                        usage.getOrDefault("prompt_tokens", 0),
                        usage.getOrDefault("completion_tokens", 0),
                        usage.getOrDefault("total_tokens", 0)));

                    return new ChatCompletionResult(true, "AI response received successfully", aiResponse, usage);
                } else {
                    return new ChatCompletionResult(false, "No response content received from AI", null, null);
                }
            } else {
                String errorBody = response.body() != null ? response.body().string() : "";
                callbacks.printError("OpenRouter API returned status code: " + response.code() + " > " + errorBody);
                return new ChatCompletionResult(false,
                    "OpenRouter API error: " + response.code() + " > " + errorBody, null, null);
            }
        } catch (IOException e) {
            callbacks.printError("Failed to send AI message: " + e.getMessage());
            return new ChatCompletionResult(false, "Network error: " + e.getMessage(), null, null);
        }
    }

    /**
     * Extract exploitability score from AI response
     * Expected format: {"score": N} where N is 1-5
     */
    public static String extractScore(String aiResponse) {
        if (aiResponse == null || aiResponse.trim().isEmpty()) {
            return "";
        }

        try {
            // Try to parse as JSON
            JsonObject json = JsonParser.parseString(aiResponse).getAsJsonObject();
            if (json.has("score")) {
                int score = json.get("score").getAsInt();
                if (score >= 1 && score <= 5) {
                    return String.valueOf(score);
                }
            }
        } catch (Exception e) {
            // If JSON parsing fails, try to extract score from text
            // Look for patterns like "score": 4 or {"score":3}
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\"score\"\\s*:\\s*(\\d+)");
            java.util.regex.Matcher matcher = pattern.matcher(aiResponse);
            if (matcher.find()) {
                int score = Integer.parseInt(matcher.group(1));
                if (score >= 1 && score <= 5) {
                    return String.valueOf(score);
                }
            }
        }

        return "";
    }

    // Result classes
    public static class ApiKeyVerificationResult {
        public final boolean success;
        public final String message;
        public final String userData;

        public ApiKeyVerificationResult(boolean success, String message, String userData) {
            this.success = success;
            this.message = message;
            this.userData = userData;
        }
    }

    public static class ChatCompletionResult {
        public final boolean success;
        public final String message;
        public final String response;
        public final Map<String, Integer> usage;

        public ChatCompletionResult(boolean success, String message, String response, Map<String, Integer> usage) {
            this.success = success;
            this.message = message;
            this.response = response;
            this.usage = usage;
        }
    }
}
