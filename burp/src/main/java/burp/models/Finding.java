package burp.models;

import com.google.gson.annotations.SerializedName;

/**
 * Represents a JavaScript sink finding from the DOMLogger++ browser extension
 */
public class Finding {
    private Integer id;

    @SerializedName("dupKey")
    private String dupKey;

    private String debug;

    @SerializedName("notification")
    private boolean notification;

    private boolean alert;

    private String tag;

    private String type;

    private String date;

    private String href;

    private String frame;

    private String sink;

    private String data;

    private String trace;

    private Boolean favorite;

    @SerializedName("aiScore")
    private String aiScore;

    @SerializedName("promptId")
    private Integer promptId;

    // Constructors
    public Finding() {
        this.favorite = false;
        this.aiScore = "";
    }

    // Getters and setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getDupKey() { return dupKey; }
    public void setDupKey(String dupKey) { this.dupKey = dupKey; }

    public String getDebug() { return debug; }
    public void setDebug(String debug) { this.debug = debug; }

    public boolean isNotification() { return notification; }
    public void setNotification(boolean notification) { this.notification = notification; }

    public boolean isAlert() { return alert; }
    public void setAlert(boolean alert) { this.alert = alert; }

    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getHref() { return href; }
    public void setHref(String href) { this.href = href; }

    public String getFrame() { return frame; }
    public void setFrame(String frame) { this.frame = frame; }

    public String getSink() { return sink; }
    public void setSink(String sink) { this.sink = sink; }

    public String getData() { return data; }
    public void setData(String data) { this.data = data; }

    public String getTrace() { return trace; }
    public void setTrace(String trace) { this.trace = trace; }

    public Boolean getFavorite() { return favorite; }
    public void setFavorite(Boolean favorite) { this.favorite = favorite; }

    public String getAiScore() { return aiScore; }
    public void setAiScore(String aiScore) { this.aiScore = aiScore; }

    public Integer getPromptId() { return promptId; }
    public void setPromptId(Integer promptId) { this.promptId = promptId; }
}
