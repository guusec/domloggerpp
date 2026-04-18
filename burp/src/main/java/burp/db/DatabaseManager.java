package burp.db;

import burp.models.*;
import burp.IBurpExtenderCallbacks;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages SQLite database operations for DOMLogger++ findings
 */
public class DatabaseManager {
    private Connection connection;
    private final IBurpExtenderCallbacks callbacks;
    private String currentProject = "default";
    private String tempProject = null;

    public DatabaseManager(IBurpExtenderCallbacks callbacks) throws SQLException {
        this.callbacks = callbacks;
        initializeDatabase();
    }

    /**
     * Initialize database connection and create tables
     */
    private void initializeDatabase() throws SQLException {
        try {
            // Load SQLite JDBC driver explicitly
            try {
                Class.forName("org.sqlite.JDBC");
                callbacks.printOutput("SQLite JDBC driver loaded successfully");
            } catch (ClassNotFoundException e) {
                callbacks.printError("Failed to load SQLite JDBC driver: " + e.getMessage());
                throw new SQLException("SQLite JDBC driver not found", e);
            }

            // Get Burp extension data directory
            File extensionDir = new File(System.getProperty("user.home"), ".burp/domloggerpp");
            if (!extensionDir.exists()) {
                extensionDir.mkdirs();
            }

            File dbFile = new File(extensionDir, "domloggerpp.db");
            String jdbcUrl = "jdbc:sqlite:" + dbFile.getAbsolutePath();

            callbacks.printOutput("Database location: " + dbFile.getAbsolutePath());

            connection = DriverManager.getConnection(jdbcUrl);
            connection.setAutoCommit(true);

            createTables();
            callbacks.printOutput("Database initialized successfully");
        } catch (SQLException e) {
            callbacks.printError("Database initialization failed: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Create all required database tables
     */
    private void createTables() throws SQLException {
        // Projects table
        executeUpdate(
            "CREATE TABLE IF NOT EXISTS projects (" +
            "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  name TEXT UNIQUE NOT NULL," +
            "  table_name TEXT UNIQUE NOT NULL," +
            "  created_at TEXT DEFAULT CURRENT_TIMESTAMP" +
            ")"
        );

        // AI configuration table
        executeUpdate(
            "CREATE TABLE IF NOT EXISTS ai_config (" +
            "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  openrouter_api_key TEXT NOT NULL," +
            "  system_prompt TEXT NOT NULL," +
            "  ai_enabled BOOLEAN DEFAULT 1," +
            "  selected_model TEXT DEFAULT 'openai/gpt-3.5-turbo'," +
            "  temperature REAL DEFAULT 0.7," +
            "  thread_count INTEGER DEFAULT 1," +
            "  created_at TEXT DEFAULT CURRENT_TIMESTAMP" +
            ")"
        );

        // User prompts table
        executeUpdate(
            "CREATE TABLE IF NOT EXISTS user_prompts (" +
            "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  name TEXT NOT NULL," +
            "  condition TEXT NOT NULL," +
            "  prompt TEXT NOT NULL," +
            "  enabled BOOLEAN DEFAULT 1," +
            "  created_at TEXT DEFAULT CURRENT_TIMESTAMP" +
            ")"
        );
    }

    /**
     * Get or create findings table for current project
     */
    public String getCurrentTableName() throws SQLException {
        // Check if temp project exists
        if (tempProject != null) {
            return tempProject;
        }

        String tableName = "findings_" + currentProject.replaceAll("[^a-zA-Z0-9]", "_");

        // Check if project exists
        String query = "SELECT table_name FROM projects WHERE name = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, currentProject);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getString("table_name");
            }
        }

        // Create new project and table
        createFindingsTable(tableName);
        String insert = "INSERT INTO projects (name, table_name) VALUES (?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(insert)) {
            stmt.setString(1, currentProject);
            stmt.setString(2, tableName);
            stmt.executeUpdate();
        }

        callbacks.printOutput("Created new project: " + currentProject + " with table: " + tableName);
        return tableName;
    }

    /**
     * Create a findings table
     */
    private void createFindingsTable(String tableName) throws SQLException {
        executeUpdate(
            "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
            "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  dupKey TEXT UNIQUE NOT NULL," +
            "  debug TEXT," +
            "  alert BOOLEAN," +
            "  tag TEXT," +
            "  type TEXT," +
            "  date TEXT," +
            "  href TEXT," +
            "  frame TEXT," +
            "  sink TEXT," +
            "  data TEXT," +
            "  trace TEXT," +
            "  favorite BOOLEAN DEFAULT 0," +
            "  aiScore TEXT DEFAULT ''," +
            "  promptId INTEGER" +
            ")"
        );
    }

    /**
     * Add findings to the database
     */
    public int addFindings(List<Finding> findings) throws SQLException {
        String tableName = getCurrentTableName();
        int added = 0;

        String sql = "INSERT OR IGNORE INTO " + tableName +
            " (dupKey, debug, alert, tag, type, date, href, frame, sink, data, trace) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            for (Finding finding : findings) {
                stmt.setString(1, finding.getDupKey());
                stmt.setString(2, finding.getDebug());
                stmt.setBoolean(3, finding.isAlert());
                stmt.setString(4, finding.getTag());
                stmt.setString(5, finding.getType());
                stmt.setString(6, finding.getDate());
                stmt.setString(7, finding.getHref());
                stmt.setString(8, finding.getFrame());
                stmt.setString(9, finding.getSink());
                stmt.setString(10, finding.getData());
                stmt.setString(11, finding.getTrace());

                int rowsAffected = stmt.executeUpdate();
                added += rowsAffected;
            }
        }

        return added;
    }

    /**
     * Get findings with optional filter and pagination
     */
    public List<Finding> getFindings(String whereClause, List<String> params, int page, int limit) throws SQLException {
        String tableName = getCurrentTableName();
        List<Finding> findings = new ArrayList<>();

        String sql = "SELECT * FROM " + tableName;
        if (whereClause != null && !whereClause.isEmpty()) {
            sql += " WHERE " + whereClause;
        }
        sql += " ORDER BY id DESC LIMIT ? OFFSET ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            int paramIndex = 1;
            if (params != null) {
                for (String param : params) {
                    stmt.setString(paramIndex++, param);
                }
            }
            stmt.setInt(paramIndex++, limit);
            stmt.setInt(paramIndex, (page - 1) * limit);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                findings.add(mapResultSetToFinding(rs));
            }
        }

        return findings;
    }

    /**
     * Get total count of findings with optional filter
     */
    public int getFindingsCount(String whereClause, List<String> params) throws SQLException {
        String tableName = getCurrentTableName();
        String sql = "SELECT COUNT(*) FROM " + tableName;
        if (whereClause != null && !whereClause.isEmpty()) {
            sql += " WHERE " + whereClause;
        }

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            if (params != null) {
                for (int i = 0; i < params.size(); i++) {
                    stmt.setString(i + 1, params.get(i));
                }
            }
            ResultSet rs = stmt.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /**
     * Delete findings by IDs
     */
    public int deleteFindings(List<Integer> ids) throws SQLException {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }

        String tableName = getCurrentTableName();
        String placeholders = String.join(",", ids.stream().map(id -> "?").toArray(String[]::new));
        String sql = "DELETE FROM " + tableName + " WHERE id IN (" + placeholders + ")";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            for (int i = 0; i < ids.size(); i++) {
                stmt.setInt(i + 1, ids.get(i));
            }
            return stmt.executeUpdate();
        }
    }

    /**
     * Update finding favorite status
     */
    public void updateFindingFavorite(int id, boolean favorite) throws SQLException {
        String tableName = getCurrentTableName();
        String sql = "UPDATE " + tableName + " SET favorite = ? WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setBoolean(1, favorite);
            stmt.setInt(2, id);
            stmt.executeUpdate();
        }
    }

    /**
     * Update finding AI score
     */
    public void updateFindingAIScore(int id, String aiScore, Integer promptId) throws SQLException {
        String tableName = getCurrentTableName();
        String sql = "UPDATE " + tableName + " SET aiScore = ?, promptId = ? WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, aiScore);
            if (promptId != null) {
                stmt.setInt(2, promptId);
            } else {
                stmt.setNull(2, Types.INTEGER);
            }
            stmt.setInt(3, id);
            stmt.executeUpdate();
        }
    }

    /**
     * Update finding trace
     */
    public void updateFindingTrace(int id, String trace) throws SQLException {
        String tableName = getCurrentTableName();
        String sql = "UPDATE " + tableName + " SET trace = ? WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, trace);
            stmt.setInt(2, id);
            stmt.executeUpdate();
        }
    }

    /**
     * Get or create AI configuration
     */
    public AIConfig getOrCreateAIConfig() throws SQLException {
        String query = "SELECT * FROM ai_config ORDER BY id DESC LIMIT 1";
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery(query);
            if (rs.next()) {
                return mapResultSetToAIConfig(rs);
            }
        }

        // Create default config
        AIConfig config = new AIConfig();
        String insert = "INSERT INTO ai_config (openrouter_api_key, system_prompt, ai_enabled, selected_model, temperature, thread_count) VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, "");
            stmt.setString(2, config.getSystemPrompt());
            stmt.setBoolean(3, true);
            stmt.setString(4, config.getSelectedModel());
            stmt.setDouble(5, config.getTemperature());
            stmt.setInt(6, config.getThreadCount());
            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                config.setId(rs.getInt(1));
            }
        }

        return config;
    }

    /**
     * Update AI configuration
     */
    public void updateAIConfig(AIConfig config) throws SQLException {
        String sql = "UPDATE ai_config SET openrouter_api_key = ?, system_prompt = ?, ai_enabled = ?, selected_model = ?, temperature = ?, thread_count = ? WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, config.getOpenrouterApiKey());
            stmt.setString(2, config.getSystemPrompt());
            stmt.setBoolean(3, config.isAiEnabled());
            stmt.setString(4, config.getSelectedModel());
            stmt.setDouble(5, config.getTemperature());
            stmt.setInt(6, config.getThreadCount());
            stmt.setInt(7, config.getId());
            stmt.executeUpdate();
        }
    }

    /**
     * Get all user prompts
     */
    public List<UserPrompt> getUserPrompts() throws SQLException {
        List<UserPrompt> prompts = new ArrayList<>();
        String query = "SELECT * FROM user_prompts ORDER BY id";

        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery(query);
            while (rs.next()) {
                prompts.add(mapResultSetToUserPrompt(rs));
            }
        }

        return prompts;
    }

    /**
     * Create user prompt
     */
    public UserPrompt createUserPrompt(UserPrompt prompt) throws SQLException {
        String sql = "INSERT INTO user_prompts (name, condition, prompt, enabled) VALUES (?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, prompt.getName());
            stmt.setString(2, prompt.getCondition());
            stmt.setString(3, prompt.getPrompt());
            stmt.setBoolean(4, prompt.isEnabled());
            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                prompt.setId(rs.getInt(1));
            }
        }

        return prompt;
    }

    /**
     * Update user prompt
     */
    public void updateUserPrompt(UserPrompt prompt) throws SQLException {
        String sql = "UPDATE user_prompts SET name = ?, condition = ?, prompt = ?, enabled = ? WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, prompt.getName());
            stmt.setString(2, prompt.getCondition());
            stmt.setString(3, prompt.getPrompt());
            stmt.setBoolean(4, prompt.isEnabled());
            stmt.setInt(5, prompt.getId());
            stmt.executeUpdate();
        }
    }

    /**
     * Delete user prompt
     */
    public void deleteUserPrompt(int id) throws SQLException {
        String sql = "DELETE FROM user_prompts WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    /**
     * Get all projects
     */
    public List<Project> getProjects() throws SQLException {
        List<Project> projects = new ArrayList<>();
        String query = "SELECT * FROM projects ORDER BY created_at DESC";

        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery(query);
            while (rs.next()) {
                Project project = new Project();
                project.setId(rs.getInt("id"));
                project.setName(rs.getString("name"));
                project.setTableName(rs.getString("table_name"));
                project.setCreatedAt(rs.getString("created_at"));

                // Get row count and size
                String tableName = project.getTableName();
                String countQuery = "SELECT COUNT(*) FROM " + tableName;
                try (Statement countStmt = connection.createStatement()) {
                    ResultSet countRs = countStmt.executeQuery(countQuery);
                    if (countRs.next()) {
                        project.setRowCount(countRs.getInt(1));
                    }
                }

                projects.add(project);
            }
        }

        return projects;
    }

    /**
     * Delete project
     */
    public void deleteProject(String name) throws SQLException {
        // Get table name first
        String query = "SELECT table_name FROM projects WHERE name = ?";
        String tableName = null;

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, name);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                tableName = rs.getString("table_name");
            }
        }

        if (tableName != null) {
            // Drop the findings table
            executeUpdate("DROP TABLE IF EXISTS " + tableName);

            // Remove from projects
            String delete = "DELETE FROM projects WHERE name = ?";
            try (PreparedStatement stmt = connection.prepareStatement(delete)) {
                stmt.setString(1, name);
                stmt.executeUpdate();
            }
        }
    }

    /**
     * Create temporary recording project
     */
    public void createTempProject() throws SQLException {
        String tableName = "findings_temp_recording";
        createFindingsTable(tableName);

        String sql = "INSERT OR REPLACE INTO projects (name, table_name) VALUES (?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, "temp_recording");
            stmt.setString(2, tableName);
            stmt.executeUpdate();
        }

        tempProject = tableName;
        callbacks.printOutput("Started recording session");
    }

    /**
     * Delete temporary recording project
     */
    public void deleteTempProject() throws SQLException {
        executeUpdate("DROP TABLE IF EXISTS findings_temp_recording");
        String delete = "DELETE FROM projects WHERE name = 'temp_recording'";
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(delete);
        }

        tempProject = null;
        callbacks.printOutput("Stopped recording session");
    }

    /**
     * Check if temp project exists
     */
    public boolean hasTempProject() throws SQLException {
        String query = "SELECT 1 FROM projects WHERE name = 'temp_recording'";
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery(query);
            return rs.next();
        }
    }

    // Helper methods
    private void executeUpdate(String sql) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sql);
        }
    }

    private Finding mapResultSetToFinding(ResultSet rs) throws SQLException {
        Finding finding = new Finding();
        finding.setId(rs.getInt("id"));
        finding.setDupKey(rs.getString("dupKey"));
        finding.setDebug(rs.getString("debug"));
        finding.setAlert(rs.getBoolean("alert"));
        finding.setTag(rs.getString("tag"));
        finding.setType(rs.getString("type"));
        finding.setDate(rs.getString("date"));
        finding.setHref(rs.getString("href"));
        finding.setFrame(rs.getString("frame"));
        finding.setSink(rs.getString("sink"));
        finding.setData(rs.getString("data"));
        finding.setTrace(rs.getString("trace"));
        finding.setFavorite(rs.getBoolean("favorite"));
        finding.setAiScore(rs.getString("aiScore"));

        int promptId = rs.getInt("promptId");
        if (!rs.wasNull()) {
            finding.setPromptId(promptId);
        }

        return finding;
    }

    private AIConfig mapResultSetToAIConfig(ResultSet rs) throws SQLException {
        AIConfig config = new AIConfig();
        config.setId(rs.getInt("id"));
        config.setOpenrouterApiKey(rs.getString("openrouter_api_key"));
        config.setSystemPrompt(rs.getString("system_prompt"));
        config.setAiEnabled(rs.getBoolean("ai_enabled"));
        config.setSelectedModel(rs.getString("selected_model"));
        config.setTemperature(rs.getDouble("temperature"));
        config.setThreadCount(rs.getInt("thread_count"));
        config.setCreatedAt(rs.getString("created_at"));
        return config;
    }

    private UserPrompt mapResultSetToUserPrompt(ResultSet rs) throws SQLException {
        UserPrompt prompt = new UserPrompt();
        prompt.setId(rs.getInt("id"));
        prompt.setName(rs.getString("name"));
        prompt.setCondition(rs.getString("condition"));
        prompt.setPrompt(rs.getString("prompt"));
        prompt.setEnabled(rs.getBoolean("enabled"));
        prompt.setCreatedAt(rs.getString("created_at"));
        return prompt;
    }

    public void setCurrentProject(String project) {
        this.currentProject = project;
    }

    public String getCurrentProject() {
        return tempProject != null ? "temp_recording" : currentProject;
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            callbacks.printError("Error closing database: " + e.getMessage());
        }
    }
}
