package burp.trace;

import burp.IBurpExtenderCallbacks;
import burp.IHttpRequestResponse;
import burp.IRequestInfo;
import burp.IResponseInfo;

import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Enhances stack traces with actual source code context from HTTP responses
 */
public class TraceEnhancer {
    private static final String BETTER_TRACE_MARKER = "--- BETTER TRACE ---\n";
    private static final int MAX_CONTEXT_WINDOW_SIZE = 1000;

    private final IBurpExtenderCallbacks callbacks;

    public TraceEnhancer(IBurpExtenderCallbacks callbacks) {
        this.callbacks = callbacks;
    }

    /**
     * Enhance a stack trace with code context
     */
    public String enhanceTrace(String trace) {
        if (trace == null || trace.trim().isEmpty()) {
            return trace;
        }

        // If already enhanced, extract original first
        String originalTrace = trace;
        if (trace.startsWith(BETTER_TRACE_MARKER)) {
            originalTrace = extractOriginalTrace(trace);
        }

        StringBuilder betterTrace = new StringBuilder(BETTER_TRACE_MARKER);
        Map<String, String> cachedResponses = new HashMap<>();

        String[] stackLines = originalTrace.trim().split("\n");
        for (String stackLine : stackLines) {
            stackLine = stackLine.trim();
            betterTrace.append("// ").append(stackLine).append("\n");

            try {
                StackLineInfo info = parseStackLine(stackLine);
                if (info == null || info.url == null || info.line == null || info.column == null) {
                    continue;
                }

                // Get response from cache or fetch it
                String responseBody = cachedResponses.get(info.url);
                if (responseBody == null) {
                    responseBody = getResponseBody(info.url);
                    if (responseBody != null) {
                        cachedResponses.put(info.url, responseBody);
                    }
                }

                if (responseBody != null && !responseBody.isEmpty()) {
                    String context = extractCodeContext(responseBody, info.line, info.column, info.funcName);
                    if (context != null && !context.isEmpty()) {
                        betterTrace.append(context).append("\n----\n");
                    }
                }
            } catch (Exception e) {
                callbacks.printError("Error parsing stack line: " + e.getMessage() + " - " + stackLine);
            }
        }

        return betterTrace.toString();
    }

    /**
     * Extract original trace from enhanced trace
     */
    private String extractOriginalTrace(String enhancedTrace) {
        StringBuilder original = new StringBuilder();
        String[] lines = enhancedTrace.split("\n");

        for (String line : lines) {
            // Original stack lines start with '// ' and contain '@' or 'at '
            if (line.startsWith("// ") && (line.contains("@") || line.contains("at "))) {
                original.append(line.substring(3)).append("\n");
            }
        }

        return original.toString();
    }

    /**
     * Parse a stack trace line to extract function name, URL, line, and column
     */
    private StackLineInfo parseStackLine(String stackLine) {
        StackLineInfo info = new StackLineInfo();

        try {
            if (stackLine.startsWith("at ")) {
                // Chrome/V8 format: "at functionName (http://example.com/script.js:15:234)"
                // or: "at http://example.com/script.js:15:234"
                String[] parts = stackLine.split(" ");
                String lastPart = parts[parts.length - 1];

                // Extract function name if present
                if (parts.length > 2) {
                    info.funcName = parts[1];
                }

                // Remove parentheses if present
                if (lastPart.startsWith("(") && lastPart.endsWith(")")) {
                    lastPart = lastPart.substring(1, lastPart.length() - 1);
                }

                info.url = parseUrlLineColumn(lastPart, info);
            } else {
                // Firefox format: "functionName@http://example.com/script.js:15:234"
                String[] parts = stackLine.split("@");
                if (parts.length == 2) {
                    info.funcName = parts[0].trim();
                    info.url = parseUrlLineColumn(parts[1].trim(), info);
                } else if (parts.length == 1) {
                    info.url = parseUrlLineColumn(parts[0].trim(), info);
                }
            }
        } catch (Exception e) {
            return null;
        }

        return info;
    }

    /**
     * Parse URL:line:column from a string
     */
    private String parseUrlLineColumn(String urlPart, StackLineInfo info) {
        // Split by ':' from right to left to get column, line, then URL
        String[] parts = urlPart.split(":");

        if (parts.length < 3) {
            return null;
        }

        // Last part is column
        info.column = parts[parts.length - 1];

        // Second to last is line
        info.line = parts[parts.length - 2];

        // Everything before is the URL
        StringBuilder urlBuilder = new StringBuilder();
        for (int i = 0; i < parts.length - 2; i++) {
            if (i > 0) {
                urlBuilder.append(":");
            }
            urlBuilder.append(parts[i]);
        }

        return urlBuilder.toString();
    }

    /**
     * Get response body for a URL from Burp's proxy history
     */
    private String getResponseBody(String urlString) {
        try {
            URL url = new URL(urlString);
            String host = url.getHost();
            String path = url.getPath();
            if (path.isEmpty()) {
                path = "/";
            }

            // Search through proxy history
            IHttpRequestResponse[] proxyHistory = callbacks.getProxyHistory();

            for (IHttpRequestResponse item : proxyHistory) {
                if (item.getResponse() == null) {
                    continue;
                }

                IRequestInfo requestInfo = callbacks.getHelpers().analyzeRequest(item);
                URL itemUrl = requestInfo.getUrl();

                if (itemUrl.getHost().equals(host) && itemUrl.getPath().equals(path)) {
                    // Found matching request
                    byte[] response = item.getResponse();
                    IResponseInfo responseInfo = callbacks.getHelpers().analyzeResponse(response);
                    int bodyOffset = responseInfo.getBodyOffset();

                    byte[] bodyBytes = Arrays.copyOfRange(response, bodyOffset, response.length);
                    return new String(bodyBytes);
                }
            }

            // Also check site map
            IHttpRequestResponse[] siteMap = callbacks.getSiteMap(urlString);
            if (siteMap != null && siteMap.length > 0) {
                IHttpRequestResponse item = siteMap[0];
                if (item.getResponse() != null) {
                    byte[] response = item.getResponse();
                    IResponseInfo responseInfo = callbacks.getHelpers().analyzeResponse(response);
                    int bodyOffset = responseInfo.getBodyOffset();

                    byte[] bodyBytes = Arrays.copyOfRange(response, bodyOffset, response.length);
                    return new String(bodyBytes);
                }
            }

        } catch (Exception e) {
            callbacks.printError("Error getting response for URL " + urlString + ": " + e.getMessage());
        }

        return null;
    }

    /**
     * Extract code context around a specific line and column
     */
    private String extractCodeContext(String responseBody, String lineStr, String columnStr, String funcName) {
        try {
            int lineNumber = Integer.parseInt(lineStr);
            int columnNumber = Integer.parseInt(columnStr);

            String[] lines = responseBody.split("\n");
            if (lineNumber > lines.length || lineNumber < 1) {
                return null;
            }

            String targetLine = lines[lineNumber - 1];
            int lineStartIndex = getLineStartIndex(responseBody, lineNumber);
            int charIndex = lineStartIndex + columnNumber - 1;

            return extractCodeContextAtIndex(responseBody, charIndex, funcName);

        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Get the character index where a line starts in the response body
     */
    private int getLineStartIndex(String responseBody, int lineNumber) {
        int index = 0;
        int currentLine = 1;

        while (currentLine < lineNumber && index < responseBody.length()) {
            if (responseBody.charAt(index) == '\n') {
                currentLine++;
            }
            index++;
        }

        return index;
    }

    /**
     * Extract code context around a character index
     */
    private String extractCodeContextAtIndex(String responseBody, int charIndex, String funcName) {
        int startIndex = Math.max(0, charIndex - MAX_CONTEXT_WINDOW_SIZE);
        int endIndex = Math.min(responseBody.length(), charIndex + MAX_CONTEXT_WINDOW_SIZE);

        // Find the opening brace of the function
        int braceCount = 0;
        int searchStart = Math.max(startIndex, charIndex - MAX_CONTEXT_WINDOW_SIZE);

        for (int i = charIndex; i >= searchStart; i--) {
            char ch = responseBody.charAt(i);
            if (ch == '}') {
                braceCount++;
            } else if (ch == '{') {
                braceCount--;
                if (braceCount == -1) {
                    startIndex = i;
                    // Try to find the opening parenthesis for function arguments
                    int paramsIndex = responseBody.lastIndexOf('(', i);
                    if (paramsIndex != -1 && paramsIndex >= searchStart) {
                        startIndex = paramsIndex;
                    }
                    break;
                }
            }
        }

        // Find the closing brace of the function
        braceCount = 0;
        int searchEnd = Math.min(endIndex, charIndex + MAX_CONTEXT_WINDOW_SIZE);

        for (int i = charIndex; i < searchEnd; i++) {
            char ch = responseBody.charAt(i);
            if (ch == '{') {
                braceCount++;
            } else if (ch == '}') {
                braceCount--;
                if (braceCount == -1) {
                    endIndex = i + 1;
                    break;
                }
            }
        }

        return responseBody.substring(startIndex, endIndex).trim();
    }

    /**
     * Helper class to hold parsed stack line information
     */
    private static class StackLineInfo {
        String funcName;
        String url;
        String line;
        String column;
    }
}
