package com.dotmarketing.db;

/**
 * Enum for the Database Types available
 * @author Andre Curione
 */
public enum DbType {

    MYSQL("MySQL"),
    POSTGRESQL("PostgreSQL"),
    ORACLE("Oracle"),
    MSSQL("Microsoft SQL Server"),
    H2("H2");

    private String type;

    DbType(final String type) {
        this.type = type;
    }

    public String getDbType() {
        return type;
    }

    public static DbType getDbType(final String dbType) {
        DbType type = null;
        switch (dbType) {
            case "PostgreSQL":
                type = DbType.POSTGRESQL;
            case "MySQL":
                type = DbType.MYSQL;
            case "Microsoft SQL Server":
                type = DbType.MSSQL;
            case "Oracle":
                type = DbType.ORACLE;
            case "H2":
                type = DbType.H2;
        }
        return type;
    }

    @Override
    public String toString() {
        return getDbType();
    }
}
