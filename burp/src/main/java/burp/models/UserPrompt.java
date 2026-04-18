package burp.models;

import com.google.gson.annotations.SerializedName;

/**
 * User-defined AI prompt with conditional triggering
 */
public class UserPrompt {
    private Integer id;
    private String name;
    private String condition;
    private String prompt;
    private boolean enabled;

    @SerializedName("created_at")
    private String createdAt;

    public UserPrompt() {
        this.enabled = true;
    }

    // Getters and setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }

    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
