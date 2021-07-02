package com.dotmarketing.db;

/**
 * Enum for the Database Types available
 * @author Andre Curione
 */
public enum DbType {

    MYSQL("MySQL"),
    POSTGRESQL("PostgreSQL"),
    ORACLE("Oracle"),
    MSSQL("Microsoft SQL Server");

    private String type;

    DbType(final String type) {
        this.type = type;
    }

    public String getDbType() {
        return type;
    }

    public static DbType getDbType(final String dbType) {
        DbType type;
        switch (dbType) {
            case "MySQL":
                type = DbType.MYSQL;
                break;
            case "Microsoft SQL Server":
                type = DbType.MSSQL;
                break;
            case "Oracle":
                type = DbType.ORACLE;
                break;
            default:
                type = DbType.POSTGRESQL;
                break;
        }
        return type;
    }

    @Override
    public String toString() {
        return getDbType();
    }
}
