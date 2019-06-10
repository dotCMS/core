package com.dotmarketing.common.db;

import java.util.List;

public class PrimaryKey {

    private final String tableName;
    private final String keyName;
    private final List<String> columnNames;

    public PrimaryKey(final String tableName, final String keyName, final List<String> columnNames) {
        this.tableName = tableName;
        this.keyName = keyName;
        this.columnNames = columnNames;
    }

    public String getTableName() {
        return tableName;
    }

    public String getKeyName() {
        return keyName;
    }

    public List<String> getColumnNames() {
        return columnNames;
    }

    @Override
    public String toString() {
        return "PrimaryKey [tableName=" + tableName + ", keyName=" + keyName + ", columnNames=" + columnNames + "]";
    }
}
