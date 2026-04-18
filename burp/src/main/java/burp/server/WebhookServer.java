package burp.server;

import burp.IBurpExtenderCallbacks;
import burp.db.DatabaseManager;
import burp.models.Finding;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Embedded HTTP server to receive findings from DOMLogger++ browser extension
 */
public class WebhookServer {
    private Server server;
    private final DatabaseManager dbManager;
    private final IBurpExtenderCallbacks callbacks;
    private final Gson gson;
    private int port = 8089; // Default port matching common Caido configuration

    public WebhookServer(DatabaseManager dbManager, IBurpExtenderCallbacks callbacks) {
        this.dbManager = dbManager;
        this.callbacks = callbacks;
        this.gson = new Gson();
    }

    /**
     * Start the webhook server
     */
    public void start(int port) throws Exception {
        this.port = port;
        server = new Server(port);
        server.setHandler(new WebhookHandler());

        try {
            server.start();
            callbacks.printOutput("Webhook server started on http://localhost:" + port);
            callbacks.printOutput("Configure DOMLogger++ browser extension to send findings to: http://localhost:" + port + "/webhook");
        } catch (Exception e) {
            callbacks.printError("Failed to start webhook server: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Stop the webhook server
     */
    public void stop() {
        if (server != null) {
            try {
                server.stop();
                callbacks.printOutput("Webhook server stopped");
            } catch (Exception e) {
                callbacks.printError("Error stopping webhook server: " + e.getMessage());
            }
        }
    }

    /**
     * Check if server is running
     */
    public boolean isRunning() {
        return server != null && server.isRunning();
    }

    public int getPort() {
        return port;
    }

    /**
     * HTTP request handler for webhook endpoint
     */
    private class WebhookHandler extends AbstractHandler {
        @Override
        public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
            baseRequest.setHandled(true);

            // Set CORS headers for browser extension
            response.setHeader("Access-Control-Allow-Origin", "*");
            response.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            response.setHeader("Access-Control-Allow-Headers", "Content-Type");

            // Handle OPTIONS preflight request
            if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
                response.setStatus(HttpServletResponse.SC_OK);
                return;
            }

            // Handle POST to /webhook
            if ("/webhook".equals(target) && "POST".equalsIgnoreCase(request.getMethod())) {
                handleWebhook(request, response);
                return;
            }

            // Handle GET for health check
            if ("/health".equals(target) && "GET".equalsIgnoreCase(request.getMethod())) {
                response.setStatus(HttpServletResponse.SC_OK);
                response.setContentType("application/json");
                response.getWriter().write("{\"status\":\"ok\",\"service\":\"DOMLogger++ Burp\"}");
                return;
            }

            // Default 404
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Not found\"}");
        }

        /**
         * Handle webhook POST request with findings
         */
        private void handleWebhook(HttpServletRequest request, HttpServletResponse response) throws IOException {
            try {
                // Read request body
                StringBuilder requestBody = new StringBuilder();
                try (BufferedReader reader = request.getReader()) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        requestBody.append(line);
                    }
                }

                String body = requestBody.toString();
                if (body.isEmpty()) {
                    sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Empty request body");
                    return;
                }

                // Parse findings from JSON
                List<Finding> findings;
                try {
                    // Try to parse as array first
                    Finding[] findingsArray = gson.fromJson(body, Finding[].class);
                    findings = new ArrayList<>();
                    for (Finding f : findingsArray) {
                        findings.add(f);
                    }
                } catch (JsonSyntaxException e) {
                    // Try to parse as single finding
                    try {
                        Finding finding = gson.fromJson(body, Finding.class);
                        findings = new ArrayList<>();
                        findings.add(finding);
                    } catch (JsonSyntaxException e2) {
                        sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid JSON format");
                        return;
                    }
                }

                if (findings.isEmpty()) {
                    sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "No findings in request");
                    return;
                }

                // Add findings to database
                int added = dbManager.addFindings(findings);

                // Send success response
                response.setStatus(HttpServletResponse.SC_OK);
                response.setContentType("application/json");
                String responseJson = String.format(
                    "{\"success\":true,\"message\":\"Findings received\",\"added\":%d,\"total\":%d}",
                    added, findings.size()
                );
                response.getWriter().write(responseJson);

                callbacks.printOutput("Received " + findings.size() + " findings, added " + added + " new findings");

            } catch (Exception e) {
                callbacks.printError("Error handling webhook: " + e.getMessage());
                sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error: " + e.getMessage());
            }
        }

        /**
         * Send error response
         */
        private void sendErrorResponse(HttpServletResponse response, int status, String message) throws IOException {
            response.setStatus(status);
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":false,\"error\":\"" + message + "\"}");
        }
    }
}
