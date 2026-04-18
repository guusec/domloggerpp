# DOMLogger++ Burp - Quick Start Guide

Get up and running in 5 minutes!

## 🎯 Installation (2 minutes)

### 1. Build the Extension

```bash
cd domloggerpp-burp

# If you have Gradle installed:
gradle build

# If you don't have Gradle, install it first:
# macOS: brew install gradle
# Linux: sudo apt-get install gradle
# Windows: Download from https://gradle.org/install/
```

**Output**: `build/libs/domloggerpp-burp-1.0.0.jar`

### 2. Load into Burp Suite

1. Open Burp Suite
2. Go to **Extensions** tab
3. Click **Add**
4. Select **Extension type**: Java
5. Browse to `build/libs/domloggerpp-burp-1.0.0.jar`
6. Click **Next**

✅ You should see "Extension loaded successfully!" in the Output tab

## 🌐 Browser Setup (2 minutes)

### 1. Install DOMLogger++ Extension

**Firefox**: 
https://addons.mozilla.org/en-US/firefox/addon/domloggerpp

**Chrome**: 
https://chromewebstore.google.com/detail/domlogger++/lkpfjhmpbmpflldmdpdoabimdbaclolp

### 2. Configure Webhook

1. Click the DOMLogger++ extension icon
2. Go to Settings/Options
3. Find the **Webhook** or **Burp Suite** section
4. Enable webhook
5. Enter URL: `http://localhost:8089/webhook`
6. Save

✅ The webhook server is automatically started when the extension loads

## 🚀 First Use (1 minute)

### 1. Browse Through Burp

1. Set your browser to use Burp's proxy (usually `127.0.0.1:8080`)
2. Visit a website with JavaScript (e.g., https://example.com)
3. Switch to Burp Suite
4. Click the **DOMLogger++** tab
5. Go to the **Dashboard**

✅ You should see findings appearing in the table!

### 2. Try the Search

In the search bar, try:

```
sink.tag.eq:"XSS"
```

Press Enter or click Search.

✅ Only XSS-related findings should appear

## 🤖 Optional: AI Setup (Optional)

Want AI-powered exploitability scoring?

### 1. Get API Key

1. Visit https://openrouter.ai/
2. Sign up for an account
3. Go to API Keys section
4. Create a new API key (starts with `sk-or-`)

### 2. Configure in Burp

1. Go to **DOMLogger++** → **AI** tab
2. Paste your API key
3. Click **Test** to verify
4. Select a model (e.g., `openai/gpt-3.5-turbo`)
5. Click **Save Configuration**

### 3. Score Findings

1. Go to **Dashboard**
2. Select one or more findings
3. Click **AI Score Selected** (or press `Ctrl+I`)
4. Wait a few seconds
5. Scores (1-5) appear in the **AI Score** column

✅ 5 = Critical, 1 = Very Low

## 📊 Key Features to Try

### Search Syntax

```
# Find innerHTML sinks
sink.sink.cont:"innerHTML"

# XSS in eval()
sink.tag.eq:"XSS" AND sink.sink.eq:"eval"

# Data containing "alert"
sink.data.like:"%alert%"

# Complex queries
(sink.type.eq:"function" OR sink.type.eq:"attribute") AND sink.tag.eq:"XSS"
```

### Recording Sessions

1. Click **Start Recording**
2. Browse and test your target
3. Click **Stop Recording**
4. All findings from that session are isolated

### Enhance Stack Traces

1. Select a finding
2. Click **Enhance Trace** (or press `Ctrl+T`)
3. View the enhanced trace with actual source code

## ⌨️ Essential Shortcuts

| Shortcut | Action |
|----------|--------|
| `Ctrl+F` | Focus search |
| `Ctrl+R` | Refresh |
| `Ctrl+A` | Select all |
| `Ctrl+I` | AI score |
| `Ctrl+T` | Enhance trace |
| `Alt+1` | Dashboard |
| `Alt+2` | AI settings |

## 🐛 Troubleshooting

### "No findings appearing"

✅ Checklist:
- [ ] Browser extension installed?
- [ ] Webhook enabled in extension settings?
- [ ] Using Burp's proxy?
- [ ] Visiting JavaScript-heavy pages?
- [ ] Check Burp's Output console for webhook logs

### "Port 8089 already in use"

Solution: Restart Burp Suite, or check what's using port 8089:

```bash
# macOS/Linux
lsof -i :8089

# Windows
netstat -ano | findstr :8089
```

### "AI scoring not working"

✅ Checklist:
- [ ] Valid OpenRouter API key?
- [ ] AI enabled in settings?
- [ ] Model selected?
- [ ] Check Burp's Errors console for API errors

## 📚 Learn More

- **Full Documentation**: See [README.md](README.md)
- **Contributing**: See [CONTRIBUTING.md](CONTRIBUTING.md)
- **Search Syntax**: All operators in README
- **GitHub**: https://github.com/kevin-mizu/domloggerpp-burp

## 💡 Pro Tips

1. **Use recording sessions** when testing specific features
2. **Set up custom AI prompts** for recurring vulnerability patterns
3. **Export findings** regularly for reporting
4. **Enhance traces** before deep analysis
5. **Use keyboard shortcuts** - much faster than clicking

Happy hunting! 🎯
