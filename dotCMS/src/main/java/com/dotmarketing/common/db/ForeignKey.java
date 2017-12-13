package com.dotmarketing.common.db;


import java.util.LinkedHashSet;
import java.util.Set;

public class ForeignKey {

    private final String primaryKeyTableName;
    private final Set<String> primaryKeyColumnNames = new LinkedHashSet<>();
    private final String foreignKeyTableName;
    private final Set<String> foreignKeyColumnNames = new LinkedHashSet<>();
    private final String foreignKeyName;

    public ForeignKey(String primaryKeyTableName, String foreignKeyTableName, String foreignKeyName) {
        this.primaryKeyTableName = primaryKeyTableName;
        this.foreignKeyTableName = foreignKeyTableName;
        this.foreignKeyName = foreignKeyName;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ForeignKey) {
            ForeignKey k=(ForeignKey)obj;
            if (!k.primaryKeyTableName.equalsIgnoreCase(primaryKeyTableName)) {
                return false;
            }
            if (!k.foreignKeyTableName.equalsIgnoreCase(foreignKeyTableName)) {
                return false;
            }
            if (!k.foreignKeyName.equalsIgnoreCase(foreignKeyName)) {
                return false;
            }
            return true;
        }
        return false;
    }


    /**
     * Adds a new value to the list of columns for the primary key.
     *
     * @param columnName
     *            - The name of the new column.
     */
    public void addPrimaryColumnName(final String columnName) {
        this.primaryKeyColumnNames.add(columnName);
    }

    /**
     * Adds a new value to the list of columns for the foreign key.
     *
     * @param columnName
     *            - The name of the new column.
     */
    public void addForeignColumnName(String columnName) {
        this.foreignKeyColumnNames.add(columnName);
    }

    @Override
    public String toString() {
        return "ForeignKey{" +
                "primaryKeyTableName='" + primaryKeyTableName + '\'' +
                ", primaryKeyColumnNames=" + primaryKeyColumnNames +
                ", foreignKeyTableName='" + foreignKeyTableName + '\'' +
                ", foreignKeyColumnNames=" + foreignKeyColumnNames +
                ", foreignKeyName='" + foreignKeyName + '\'' +
                '}';
    }

    public String fkName(){
        return foreignKeyName;
    }

    public String getPrimaryKeyTableName() {
        return primaryKeyTableName;
    }

    public Set<String> getPrimaryKeyColumnNames() {
        return primaryKeyColumnNames;
    }

    public String getForeignKeyTableName() {
        return foreignKeyTableName;
    }

    public Set<String> getForeignKeyColumnNames() {
        return foreignKeyColumnNames;
    }

    public String getForeignKeyName() {
        return foreignKeyName;
    }

}
