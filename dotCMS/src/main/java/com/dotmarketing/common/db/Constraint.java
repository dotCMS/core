package com.dotmarketing.common.db;

public class Constraint {

    private final String table;
    private final boolean unique;
    private final String name;
    private final String columnName;

    public Constraint(final String table,
                      final boolean unique,
                      final String name,
                      final String columnName) {
        this.table = table;
        this.unique = unique;
        this.name = name;
        this.columnName = columnName;
    }

    public String getTable() {
        return table;
    }

    public boolean isUnique() {
        return unique;
    }

    public String getName() {
        return name;
    }

    public String getColumnName() {
        return columnName;
    }
}
