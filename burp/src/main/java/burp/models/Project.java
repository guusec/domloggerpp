package burp.models;

import com.google.gson.annotations.SerializedName;

/**
 * Represents a project for organizing findings
 */
public class Project {
    private Integer id;
    private String name;

    @SerializedName("table_name")
    private String tableName;

    @SerializedName("row_count")
    private int rowCount;

    @SerializedName("table_size")
    private long tableSize;

    @SerializedName("created_at")
    private String createdAt;

    // Getters and setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }

    public int getRowCount() { return rowCount; }
    public void setRowCount(int rowCount) { this.rowCount = rowCount; }

    public long getTableSize() { return tableSize; }
    public void setTableSize(long tableSize) { this.tableSize = tableSize; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
