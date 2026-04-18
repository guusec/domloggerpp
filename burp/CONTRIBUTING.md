# Contributing to DOMLogger++ Burp

Thank you for your interest in contributing to DOMLogger++ Burp! This document provides guidelines for contributing to the project.

## Development Setup

### Prerequisites

- Java JDK 11 or higher
- Gradle 7.x or higher
- Burp Suite Professional or Community Edition
- Git

### Building the Project

1. Clone the repository:
```bash
git clone https://github.com/kevin-mizu/domloggerpp-burp
cd domloggerpp-burp
```

2. Build the project:
```bash
./gradlew build
```

3. The JAR file will be generated in `build/libs/domloggerpp-burp-1.0.0.jar`

### Development Workflow

1. Load the JAR into Burp Suite:
   - Go to **Extensions** → **Add**
   - Select the built JAR file
   - Check Burp's **Output** and **Errors** consoles for logs

2. Make your changes in the source code

3. Rebuild and reload the extension:
```bash
./gradlew build
```
   - Remove the old extension from Burp
   - Add the newly built JAR

## Code Structure

```
src/main/java/burp/
├── BurpExtender.java          # Main extension entry point
├── models/                    # Data models
│   ├── Finding.java
│   ├── AIConfig.java
│   ├── UserPrompt.java
│   └── Project.java
├── db/                        # Database layer
│   └── DatabaseManager.java
├── server/                    # HTTP webhook server
│   └── WebhookServer.java
├── parser/                    # Search query parser
│   └── QueryParser.java
├── ai/                        # AI integration
│   ├── OpenRouterClient.java
│   └── AIProcessor.java
├── trace/                     # Stack trace enhancement
│   └── TraceEnhancer.java
└── ui/                        # Swing UI components
    ├── MainPanel.java
    ├── DashboardPanel.java
    ├── AIConfigPanel.java
    ├── TutorialPanel.java
    └── SettingsPanel.java
```

## Coding Guidelines

### Java Style

- Use 4 spaces for indentation (no tabs)
- Follow standard Java naming conventions:
  - Classes: `PascalCase`
  - Methods: `camelCase`
  - Constants: `UPPER_SNAKE_CASE`
- Add JavaDoc comments for public methods and classes
- Keep methods focused and single-purpose

### Database Changes

- All database schema changes must include migration logic
- Test migrations with existing databases
- Document schema changes in commit messages

### UI Development

- Use Swing components consistently
- Test UI on both light and dark themes
- Ensure keyboard shortcuts don't conflict with Burp's defaults
- Add tooltips for non-obvious controls

### Testing

Before submitting a PR:

1. **Manual Testing**:
   - Load the extension in Burp Suite
   - Test all major features:
     - Webhook receiving findings
     - Search functionality
     - AI scoring (with valid API key)
     - Trace enhancement
     - Project management
     - Recording sessions

2. **Error Handling**:
   - Test with invalid inputs
   - Check error messages are user-friendly
   - Verify errors are logged to Burp's console

3. **Performance**:
   - Test with large datasets (1000+ findings)
   - Check memory usage
   - Verify UI remains responsive

## Submitting Changes

1. **Fork the repository**

2. **Create a feature branch**:
```bash
git checkout -b feature/your-feature-name
```

3. **Make your changes**:
   - Write clear, descriptive commit messages
   - Follow the coding guidelines above
   - Test thoroughly

4. **Commit your changes**:
```bash
git commit -m "Add feature: your feature description"
```

5. **Push to your fork**:
```bash
git push origin feature/your-feature-name
```

6. **Open a Pull Request**:
   - Provide a clear description of the changes
   - Reference any related issues
   - Include screenshots for UI changes
   - List any breaking changes

## Pull Request Guidelines

- **Title**: Clear and descriptive (e.g., "Add export to JSON functionality")
- **Description**: Explain what changed and why
- **Breaking Changes**: Clearly mark any breaking changes
- **Testing**: Describe how you tested the changes
- **Screenshots**: Include for UI changes

## Bug Reports

When reporting bugs, please include:

- **Burp Suite version**
- **Java version** (`java -version`)
- **Extension version**
- **Steps to reproduce**
- **Expected behavior**
- **Actual behavior**
- **Screenshots** (if applicable)
- **Console output** (Burp's Output and Errors tabs)

## Feature Requests

We welcome feature requests! Please:

1. Check if the feature already exists
2. Search existing issues to avoid duplicates
3. Describe the use case clearly
4. Explain why this would be useful

## Questions?

- **GitHub Issues**: For bug reports and feature requests
- **Discussions**: For questions and general discussion
- **Twitter**: [@kevin_mizu](https://twitter.com/kevin_mizu)

## License

By contributing, you agree that your contributions will be licensed under the GPL-3.0 License.
