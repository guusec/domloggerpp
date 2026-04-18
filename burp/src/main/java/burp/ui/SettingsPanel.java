package burp.ui;

import burp.IBurpExtenderCallbacks;
import burp.db.DatabaseManager;
import burp.models.Project;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Settings panel for project management and preferences
 */
public class SettingsPanel extends JPanel {
    private final DatabaseManager dbManager;
    private final IBurpExtenderCallbacks callbacks;

    private JTable projectsTable;
    private ProjectsTableModel tableModel;

    public SettingsPanel(DatabaseManager dbManager, IBurpExtenderCallbacks callbacks) {
        this.dbManager = dbManager;
        this.callbacks = callbacks;

        initializeUI();
        loadProjects();
    }

    private void initializeUI() {
        setLayout(new BorderLayout());

        JTabbedPane tabbedPane = new JTabbedPane();

        // Projects tab
        JPanel projectsPanel = new JPanel(new BorderLayout());

        tableModel = new ProjectsTableModel();
        projectsTable = new JTable(tableModel);
        JScrollPane tableScrollPane = new JScrollPane(projectsTable);
        projectsPanel.add(tableScrollPane, BorderLayout.CENTER);

        JPanel projectButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> loadProjects());
        projectButtonPanel.add(refreshButton);

        JButton deleteButton = new JButton("Delete Selected");
        deleteButton.addActionListener(e -> deleteSelectedProject());
        projectButtonPanel.add(deleteButton);

        projectsPanel.add(projectButtonPanel, BorderLayout.SOUTH);

        tabbedPane.addTab("Projects", projectsPanel);

        // Keyboard shortcuts tab
        JPanel shortcutsPanel = new JPanel(new BorderLayout());
        JTextArea shortcutsArea = new JTextArea();
        shortcutsArea.setEditable(false);
        shortcutsArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        shortcutsArea.setMargin(new Insets(10, 10, 10, 10));

        String shortcuts = "KEYBOARD SHORTCUTS\n\n" +
            "Search & Suggestions:\n" +
            "  Ctrl+F          Focus search input\n" +
            "  Esc             Exit search input\n\n" +
            "Actions:\n" +
            "  Ctrl+R          Refresh data\n" +
            "  Ctrl+S          Export/Download findings\n" +
            "  Ctrl+Backspace  Delete findings\n" +
            "  Ctrl+Space      Toggle recording\n" +
            "  Ctrl+I          Send to AI\n" +
            "  Ctrl+T          Enhance stack trace\n" +
            "  Ctrl+A          Select/Unselect all findings\n\n" +
            "Menu:\n" +
            "  Alt+1           Go to Dashboard\n" +
            "  Alt+2           Go to AI\n" +
            "  Alt+3           Go to Tutorial\n" +
            "  Alt+4           Go to Settings\n";

        shortcutsArea.setText(shortcuts);
        JScrollPane shortcutsScrollPane = new JScrollPane(shortcutsArea);
        shortcutsPanel.add(shortcutsScrollPane, BorderLayout.CENTER);

        tabbedPane.addTab("Keyboard Shortcuts", shortcutsPanel);

        add(tabbedPane, BorderLayout.CENTER);
    }

    private void loadProjects() {
        try {
            List<Project> projects = dbManager.getProjects();
            tableModel.setProjects(projects);
        } catch (Exception e) {
            callbacks.printError("Error loading projects: " + e.getMessage());
        }
    }

    private void deleteSelectedProject() {
        int selectedRow = projectsTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "No project selected");
            return;
        }

        Project project = tableModel.getProjects().get(selectedRow);

        int confirm = JOptionPane.showConfirmDialog(this,
            "Delete project '" + project.getName() + "'?\nThis will delete all findings in this project.",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                dbManager.deleteProject(project.getName());
                loadProjects();
                callbacks.printOutput("Deleted project: " + project.getName());
            } catch (Exception e) {
                callbacks.printError("Error deleting project: " + e.getMessage());
                JOptionPane.showMessageDialog(this, "Error deleting project: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // Table model
    private static class ProjectsTableModel extends AbstractTableModel {
        private final String[] columnNames = {"Name", "Findings Count", "Created At"};
        private List<Project> projects = new ArrayList<>();

        @Override
        public int getRowCount() {
            return projects.size();
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
            Project project = projects.get(rowIndex);
            switch (columnIndex) {
                case 0: return project.getName();
                case 1: return project.getRowCount();
                case 2: return project.getCreatedAt();
                default: return "";
            }
        }

        public void setProjects(List<Project> projects) {
            this.projects = projects;
            fireTableDataChanged();
        }

        public List<Project> getProjects() {
            return projects;
        }
    }
}
