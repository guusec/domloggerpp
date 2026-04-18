package burp.ui;

import burp.IBurpExtenderCallbacks;
import burp.db.DatabaseManager;
import burp.ai.OpenRouterClient;
import burp.ai.AIProcessor;
import burp.trace.TraceEnhancer;
import burp.models.Finding;
import burp.models.AIConfig;
import burp.parser.QueryParser;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Main dashboard panel with findings table and detail view
 */
public class DashboardPanel extends JPanel {
    private final DatabaseManager dbManager;
    private final OpenRouterClient aiClient;
    private final AIProcessor aiProcessor;
    private final TraceEnhancer traceEnhancer;
    private final IBurpExtenderCallbacks callbacks;

    private JTextField searchField;
    private JTable findingsTable;
    private FindingsTableModel tableModel;
    private JTextArea detailArea;
    private JLabel statusLabel;
    private JButton refreshButton;
    private JButton exportButton;
    private JButton deleteButton;
    private JButton recordingButton;
    private JButton aiScoreButton;
    private JButton enhanceTraceButton;

    private int currentPage = 1;
    private int itemsPerPage = 50;
    private boolean isRecording = false;

    public DashboardPanel(DatabaseManager dbManager, OpenRouterClient aiClient, AIProcessor aiProcessor,
                          TraceEnhancer traceEnhancer, IBurpExtenderCallbacks callbacks) {
        this.dbManager = dbManager;
        this.aiClient = aiClient;
        this.aiProcessor = aiProcessor;
        this.traceEnhancer = traceEnhancer;
        this.callbacks = callbacks;

        initializeUI();
        loadFindings();

        // Check if recording session exists
        try {
            isRecording = dbManager.hasTempProject();
            updateRecordingButton();
        } catch (Exception e) {
            callbacks.printError("Error checking recording status: " + e.getMessage());
        }
    }

    private void initializeUI() {
        setLayout(new BorderLayout());

        // Top panel with search and actions
        JPanel topPanel = new JPanel(new BorderLayout());

        // Search panel
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.add(new JLabel("Search:"));

        searchField = new JTextField(40);
        searchPanel.add(searchField);

        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(e -> search());
        searchPanel.add(searchButton);

        topPanel.add(searchPanel, BorderLayout.NORTH);

        // Action buttons
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refresh());
        actionPanel.add(refreshButton);

        exportButton = new JButton("Export");
        exportButton.addActionListener(e -> exportFindings());
        actionPanel.add(exportButton);

        deleteButton = new JButton("Delete Selected");
        deleteButton.addActionListener(e -> deleteSelected());
        actionPanel.add(deleteButton);

        recordingButton = new JButton("Start Recording");
        recordingButton.addActionListener(e -> toggleRecording());
        actionPanel.add(recordingButton);

        aiScoreButton = new JButton("AI Score Selected");
        aiScoreButton.addActionListener(e -> scoreSelected());
        actionPanel.add(aiScoreButton);

        enhanceTraceButton = new JButton("Enhance Trace");
        enhanceTraceButton.addActionListener(e -> enhanceSelectedTrace());
        actionPanel.add(enhanceTraceButton);

        topPanel.add(actionPanel, BorderLayout.SOUTH);

        add(topPanel, BorderLayout.NORTH);

        // Center panel with split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

        // Findings table
        tableModel = new FindingsTableModel();
        findingsTable = new JTable(tableModel);
        findingsTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        findingsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        findingsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showFindingDetails();
            }
        });

        // Set column widths
        findingsTable.getColumnModel().getColumn(0).setPreferredWidth(50);  // ID
        findingsTable.getColumnModel().getColumn(1).setPreferredWidth(80);  // AI Score
        findingsTable.getColumnModel().getColumn(2).setPreferredWidth(60);  // Fav
        findingsTable.getColumnModel().getColumn(3).setPreferredWidth(100); // Tag
        findingsTable.getColumnModel().getColumn(4).setPreferredWidth(100); // Type
        findingsTable.getColumnModel().getColumn(5).setPreferredWidth(200); // Sink
        findingsTable.getColumnModel().getColumn(6).setPreferredWidth(300); // Data
        findingsTable.getColumnModel().getColumn(7).setPreferredWidth(300); // Href
        findingsTable.getColumnModel().getColumn(8).setPreferredWidth(150); // Date

        JScrollPane tableScrollPane = new JScrollPane(findingsTable);
        splitPane.setTopComponent(tableScrollPane);

        // Detail panel
        detailArea = new JTextArea();
        detailArea.setEditable(false);
        detailArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane detailScrollPane = new JScrollPane(detailArea);
        splitPane.setBottomComponent(detailScrollPane);

        splitPane.setDividerLocation(300);
        add(splitPane, BorderLayout.CENTER);

        // Bottom status panel
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusLabel = new JLabel("Ready");
        statusPanel.add(statusLabel);
        add(statusPanel, BorderLayout.SOUTH);

        // Register keyboard shortcuts
        registerKeyboardShortcuts();
    }

    private void registerKeyboardShortcuts() {
        InputMap inputMap = getInputMap(WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getActionMap();

        // Ctrl+F = Focus search
        inputMap.put(KeyStroke.getKeyStroke("control F"), "focusSearch");
        actionMap.put("focusSearch", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                searchField.requestFocus();
            }
        });

        // Ctrl+R = Refresh
        inputMap.put(KeyStroke.getKeyStroke("control R"), "refresh");
        actionMap.put("refresh", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refresh();
            }
        });

        // Ctrl+S = Export
        inputMap.put(KeyStroke.getKeyStroke("control S"), "export");
        actionMap.put("export", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                exportFindings();
            }
        });

        // Ctrl+Backspace = Delete
        inputMap.put(KeyStroke.getKeyStroke("control BACK_SPACE"), "delete");
        actionMap.put("delete", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteSelected();
            }
        });

        // Ctrl+Space = Toggle recording
        inputMap.put(KeyStroke.getKeyStroke("control SPACE"), "toggleRecording");
        actionMap.put("toggleRecording", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleRecording();
            }
        });

        // Ctrl+I = AI Score
        inputMap.put(KeyStroke.getKeyStroke("control I"), "aiScore");
        actionMap.put("aiScore", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                scoreSelected();
            }
        });

        // Ctrl+T = Enhance trace
        inputMap.put(KeyStroke.getKeyStroke("control T"), "enhanceTrace");
        actionMap.put("enhanceTrace", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enhanceSelectedTrace();
            }
        });

        // Ctrl+A = Select all
        inputMap.put(KeyStroke.getKeyStroke("control A"), "selectAll");
        actionMap.put("selectAll", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                findingsTable.selectAll();
            }
        });
    }

    public void loadFindings() {
        SwingUtilities.invokeLater(() -> {
            try {
                String query = searchField.getText().trim();
                String whereClause = "";
                List<String> params = new ArrayList<>();

                if (!query.isEmpty()) {
                    QueryParser.SqlResult result = QueryParser.conditionToSql(query);
                    whereClause = result.whereClause;
                    params = result.params;
                }

                List<Finding> findings = dbManager.getFindings(whereClause, params, currentPage, itemsPerPage);
                int total = dbManager.getFindingsCount(whereClause, params);

                tableModel.setFindings(findings);
                statusLabel.setText(String.format("Showing %d findings (Page %d, Total: %d)",
                    findings.size(), currentPage, total));

            } catch (Exception e) {
                callbacks.printError("Error loading findings: " + e.getMessage());
                statusLabel.setText("Error loading findings: " + e.getMessage());
            }
        });
    }

    private void search() {
        currentPage = 1;
        loadFindings();
    }

    public void refresh() {
        loadFindings();
    }

    private void showFindingDetails() {
        int selectedRow = findingsTable.getSelectedRow();
        if (selectedRow < 0) {
            detailArea.setText("");
            return;
        }

        Finding finding = tableModel.getFindings().get(selectedRow);
        StringBuilder details = new StringBuilder();

        details.append("=== FINDING DETAILS ===\n\n");
        details.append("ID: ").append(finding.getId()).append("\n");
        details.append("Tag: ").append(finding.getTag()).append("\n");
        details.append("Type: ").append(finding.getType()).append("\n");
        details.append("Sink: ").append(finding.getSink()).append("\n");
        details.append("Date: ").append(finding.getDate()).append("\n");
        details.append("Favorite: ").append(finding.getFavorite()).append("\n");
        details.append("AI Score: ").append(finding.getAiScore()).append("\n\n");

        details.append("URL: ").append(finding.getHref()).append("\n");
        details.append("Frame: ").append(finding.getFrame()).append("\n\n");

        details.append("=== DATA ===\n");
        details.append(finding.getData()).append("\n\n");

        details.append("=== STACK TRACE ===\n");
        details.append(finding.getTrace()).append("\n");

        detailArea.setText(details.toString());
        detailArea.setCaretPosition(0);
    }

    private void exportFindings() {
        // Simple CSV export
        try {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Export Findings");
            int result = fileChooser.showSaveDialog(this);

            if (result == JFileChooser.APPROVE_OPTION) {
                // Export logic here
                JOptionPane.showMessageDialog(this, "Export functionality to be implemented");
            }
        } catch (Exception e) {
            callbacks.printError("Export error: " + e.getMessage());
        }
    }

    private void deleteSelected() {
        int[] selectedRows = findingsTable.getSelectedRows();
        if (selectedRows.length == 0) {
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
            "Delete " + selectedRows.length + " finding(s)?",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                List<Integer> ids = new ArrayList<>();
                for (int row : selectedRows) {
                    ids.add(tableModel.getFindings().get(row).getId());
                }

                dbManager.deleteFindings(ids);
                loadFindings();
                statusLabel.setText("Deleted " + ids.size() + " findings");
            } catch (Exception e) {
                callbacks.printError("Error deleting findings: " + e.getMessage());
            }
        }
    }

    private void toggleRecording() {
        try {
            if (isRecording) {
                dbManager.deleteTempProject();
                isRecording = false;
                statusLabel.setText("Recording stopped");
            } else {
                dbManager.createTempProject();
                isRecording = true;
                statusLabel.setText("Recording started");
            }
            updateRecordingButton();
            loadFindings();
        } catch (Exception e) {
            callbacks.printError("Error toggling recording: " + e.getMessage());
        }
    }

    private void updateRecordingButton() {
        if (isRecording) {
            recordingButton.setText("Stop Recording");
            recordingButton.setBackground(Color.RED);
            recordingButton.setForeground(Color.WHITE);
        } else {
            recordingButton.setText("Start Recording");
            recordingButton.setBackground(null);
            recordingButton.setForeground(null);
        }
    }

    private void scoreSelected() {
        int[] selectedRows = findingsTable.getSelectedRows();
        if (selectedRows.length == 0) {
            JOptionPane.showMessageDialog(this, "No findings selected");
            return;
        }

        try {
            AIConfig config = dbManager.getOrCreateAIConfig();
            if (config.getOpenrouterApiKey() == null || config.getOpenrouterApiKey().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please configure OpenRouter API key in the AI tab");
                return;
            }

            List<Finding> findings = new ArrayList<>();
            for (int row : selectedRows) {
                findings.add(tableModel.getFindings().get(row));
            }

            aiProcessor.processFindings(findings, config);
            statusLabel.setText("Queued " + findings.size() + " findings for AI scoring");

        } catch (Exception e) {
            callbacks.printError("Error scoring findings: " + e.getMessage());
        }
    }

    private void enhanceSelectedTrace() {
        int selectedRow = findingsTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "No finding selected");
            return;
        }

        try {
            Finding finding = tableModel.getFindings().get(selectedRow);
            String enhancedTrace = traceEnhancer.enhanceTrace(finding.getTrace());

            dbManager.updateFindingTrace(finding.getId(), enhancedTrace);
            finding.setTrace(enhancedTrace);

            showFindingDetails();
            statusLabel.setText("Trace enhanced for finding " + finding.getId());

        } catch (Exception e) {
            callbacks.printError("Error enhancing trace: " + e.getMessage());
        }
    }

    // Table model
    private static class FindingsTableModel extends AbstractTableModel {
        private final String[] columnNames = {"ID", "AI Score", "Fav", "Tag", "Type", "Sink", "Data", "Href", "Date"};
        private List<Finding> findings = new ArrayList<>();

        @Override
        public int getRowCount() {
            return findings.size();
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Finding finding = findings.get(rowIndex);
            switch (columnIndex) {
                case 0: return finding.getId();
                case 1: return finding.getAiScore();
                case 2: return finding.getFavorite() ? "★" : "";
                case 3: return finding.getTag();
                case 4: return finding.getType();
                case 5: return truncate(finding.getSink(), 50);
                case 6: return truncate(finding.getData(), 50);
                case 7: return truncate(finding.getHref(), 50);
                case 8: return finding.getDate();
                default: return "";
            }
        }

        private String truncate(String str, int maxLen) {
            if (str == null) return "";
            return str.length() > maxLen ? str.substring(0, maxLen) + "..." : str;
        }

        public void setFindings(List<Finding> findings) {
            this.findings = findings;
            fireTableDataChanged();
        }

        public List<Finding> getFindings() {
            return findings;
        }
    }
}
