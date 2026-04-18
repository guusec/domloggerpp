# DOMLogger++ Burp Suite Extension

<p align="center">
    <b>A powerful Burp Suite extension for monitoring, analyzing, and triaging JavaScript sink findings from the DOMLogger++ browser extension.</b>
</p>

## 📋 Overview

**DOMLogger++ Burp** is a comprehensive Burp Suite extension that provides 1:1 feature parity with the [DOMLogger++ Caido plugin](https://github.com/kevin-mizu/domloggerpp-caido). It receives JavaScript sink findings from the [DOMLogger++ browser extension](https://github.com/kevin-mizu/domloggerpp), stores them in a local SQLite database, and provides a powerful interface to search, analyze, and triage client-side vulnerabilities.

## ✨ Features

- ✅ **Real-time JavaScript sink monitoring** via DOMLogger++ browser extension webhook
- ✅ **Advanced search & filter syntax** with autocomplete suggestions  
- ✅ **AI-powered exploitability scoring** via OpenRouter (100+ models supported)
- ✅ **Custom AI user prompts** with conditional triggers and template variables
- ✅ **Enhanced stack traces** with actual source code context from proxy history
- ✅ **Project management** to organize findings by target
- ✅ **Recording sessions** to isolate findings during active testing
- ✅ **Bulk operations**: delete, export, and AI score findings
- ✅ **Keyboard shortcuts** for fast navigation and actions
- ✅ **Embedded webhook server** (no external dependencies)

## 🚀 Installation

### Option 1: Download Pre-built JAR

1. Download the latest `domloggerpp-burp-1.0.0.jar` from the [Releases](https://github.com/kevin-mizu/domloggerpp-burp/releases) page
2. In Burp Suite, go to **Extensions** → **Add**
3. Select the downloaded JAR file
4. The extension will load automatically

### Option 2: Build from Source

#### Prerequisites

- Java JDK 11 or higher
- Gradle 7.x or higher (or use included Gradle wrapper)

#### Build Steps

```bash
git clone https://github.com/kevin-mizu/domloggerpp-burp
cd domloggerpp-burp
./gradlew build
```

The built JAR will be located at: `build/libs/domloggerpp-burp-1.0.0.jar`

Load it into Burp Suite via **Extensions** → **Add** → Select JAR file.

## 📦 Browser Extension Setup

1. **Install DOMLogger++ browser extension:**
   - **Firefox**: https://addons.mozilla.org/en-US/firefox/addon/domloggerpp
   - **Chrome**: https://chromewebstore.google.com/detail/domlogger++/lkpfjhmpbmpflldmdpdoabimdbaclolp

2. **Configure the webhook:**
   - Open the DOMLogger++ extension settings
   - Navigate to the **Webhook** or **Burp Suite** section
   - Enable webhook integration
   - Enter the webhook URL: `http://localhost:8089/webhook`
   - Save settings

3. **Start browsing** through Burp's proxy - findings will appear in the Dashboard!

## 🖥️ Dashboard

The dashboard is your central hub for managing findings:

- **Search bar**: Filter findings using advanced syntax
- **Findings table**: Sortable, selectable table with all your findings
- **Detail panel**: View complete finding information, stack traces, and data
- **Bulk actions**: Refresh, export, delete, AI score, enhance traces

### Search Syntax

Filter findings using the powerful query syntax:

```
sink.field.operator:"value"
```

**Available fields**: `id`, `dupKey`, `debug`, `aiScore`, `alert`, `tag`, `type`, `date`, `href`, `frame`, `sink`, `data`, `trace`, `favorite`

**Operators**:
| Operator | Description |
|----------|-------------|
| `eq` | Equal |
| `ne` / `neq` | Not equal |
| `cont` | Contains |
| `ncont` | Not contains |
| `like` | SQL LIKE pattern (`%` wildcard) |
| `nlike` | SQL NOT LIKE |

**Logical operators**: `AND`, `OR`, and parentheses for grouping

**Examples**:
```
sink.tag.eq:"XSS" AND sink.data.like:"%test%"
sink.sink.cont:"innerHTML" OR sink.sink.cont:"outerHTML"
(sink.type.eq:"attribute" OR sink.type.eq:"function") AND sink.tag.eq:"XSS"
```

## 🤖 AI Configuration

The extension supports AI-powered exploitability scoring via [OpenRouter](https://openrouter.ai/):

1. **Get an OpenRouter API key**: https://openrouter.ai/
2. Navigate to the **AI** tab
3. Enter your API key
4. Click **Test** to verify
5. Select your preferred model (100+ options available)
6. Adjust temperature and thread count
7. Customize the system prompt if needed
8. Click **Save Configuration**

### AI Scoring

- Select findings in the dashboard
- Click **AI Score Selected** or press `Ctrl+I`
- Findings are queued and processed in the background
- Scores appear in the **AI Score** column (1-5 scale)

### Custom User Prompts

Create custom prompts that trigger based on conditions:

- Define prompt name and condition (using search syntax)
- Use template variables: `{sink}`, `{data}`, `{trace}`, `{href}`, `{type}`, etc.
- When a finding matches the condition, the custom prompt is used for AI analysis

## 📊 Enhanced Stack Traces

Extract actual source code from Burp's proxy history:

1. Select a finding
2. Click **Enhance Trace** or press `Ctrl+T`
3. The extension queries Burp's proxy history
4. Source code context is extracted and added to the trace
5. View the enhanced trace in the detail panel

## ⚙️ Settings

### Projects

- View all projects and their findings count
- Delete projects (and all associated findings)
- Projects are automatically created based on context

### Recording Sessions

- Click **Start Recording** to create a temporary project
- All findings during the session go to the temp project
- Click **Stop Recording** to end and delete the temp project
- Useful for isolating findings during specific tests

### Keyboard Shortcuts

**Search & Actions:**
| Shortcut | Action |
|----------|--------|
| `Ctrl + F` | Focus search input |
| `Ctrl + R` | Refresh data |
| `Ctrl + S` | Export findings |
| `Ctrl + Backspace` | Delete selected findings |
| `Ctrl + Space` | Toggle recording |
| `Ctrl + I` | Send to AI |
| `Ctrl + T` | Enhance stack trace |
| `Ctrl + A` | Select/Unselect all |

**Menu Navigation:**
| Shortcut | Action |
|----------|--------|
| `Alt + 1` | Go to Dashboard |
| `Alt + 2` | Go to AI |
| `Alt + 3` | Go to Tutorial |
| `Alt + 4` | Go to Settings |

## 🗄️ Data Storage

All data is stored in a local SQLite database:

- **Location**: `~/.burp/domloggerpp/domloggerpp.db`
- **Tables**: Projects, findings, AI configuration, user prompts
- **No external services** required (except OpenRouter for AI scoring)

## 🔧 Architecture

```
┌─────────────────────────────────────────────┐
│      DOMLogger++ Browser Extension          │
│   (Monitors JS sinks in real-time)          │
└────────────────┬────────────────────────────┘
                 │ HTTP POST
                 │ /webhook
                 ▼
┌─────────────────────────────────────────────┐
│         Webhook Server (Jetty)              │
│    Receives findings on port 8089           │
└────────────────┬────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────┐
│        SQLite Database Manager              │
│  - Projects    - AI Config                  │
│  - Findings    - User Prompts               │
└────┬──────────┬──────────────┬──────────────┘
     │          │              │
     ▼          ▼              ▼
┌─────────┐ ┌────────┐  ┌──────────────┐
│Dashboard│ │AI Panel│  │Trace Enhancer│
│         │ │        │  │              │
│ - Table │ │OpenRouter│  │Burp Proxy  │
│ - Search│ │Client  │  │History     │
│ - Detail│ │        │  │            │
└─────────┘ └────────┘  └──────────────┘
```

## 🆚 Comparison with Caido Plugin

This Burp Suite extension provides **1:1 feature parity** with the Caido plugin:

| Feature | Caido Plugin | Burp Extension |
|---------|--------------|----------------|
| Webhook Server | ✅ | ✅ |
| SQLite Database | ✅ | ✅ |
| Advanced Search Syntax | ✅ | ✅ |
| AI Integration (OpenRouter) | ✅ | ✅ |
| Custom User Prompts | ✅ | ✅ |
| Stack Trace Enhancement | ✅ | ✅ |
| Project Management | ✅ | ✅ |
| Recording Sessions | ✅ | ✅ |
| Keyboard Shortcuts | ✅ | ✅ |
| Bulk Operations | ✅ | ✅ |

## 🐛 Troubleshooting

### Webhook Server Fails to Start

**Issue**: Port 8089 already in use

**Solution**: 
- Check if another application is using port 8089
- Restart Burp Suite
- Modify the port in `BurpExtender.java` and rebuild

### No Findings Appearing

**Checklist**:
1. ✅ Browser extension installed and enabled
2. ✅ Webhook URL configured correctly in browser extension
3. ✅ Browsing through Burp's proxy
4. ✅ Webhook server running (check Burp's output console)
5. ✅ JavaScript execution happening on target pages

### AI Scoring Not Working

**Checklist**:
1. ✅ Valid OpenRouter API key configured
2. ✅ AI enabled in settings
3. ✅ Thread count > 0
4. ✅ Model selected
5. ✅ Check Burp's error console for API errors

### Enhanced Traces Empty

**Reason**: The JavaScript files need to pass through Burp's proxy first

**Solution**:
- Visit the target pages through Burp
- Wait for responses to be captured in proxy history
- Then enhance traces

## 📄 License

GPL-3.0 License - see [LICENSE](LICENSE) file for details.

## 🙏 Credits

- **Original Author**: [kevin-mizu](https://github.com/kevin-mizu)
- **DOMLogger++ Browser Extension**: https://github.com/kevin-mizu/domloggerpp
- **Caido Plugin**: https://github.com/kevin-mizu/domloggerpp-caido

## 🤝 Contributing

Contributions are welcome! Please feel free to submit issues or pull requests.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📧 Support

For issues, questions, or feature requests:
- **GitHub Issues**: https://github.com/kevin-mizu/domloggerpp-burp/issues
- **Original Project**: https://github.com/kevin-mizu/domloggerpp
- **Twitter**: [@kevin_mizu](https://twitter.com/kevin_mizu)
