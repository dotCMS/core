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

    private String dbType;

    DbType(final String dbType) {
        this.dbType = dbType;
    }

    public String getDbType() {
        return dbType;
    }

    public static DbType getDbType(final String type) {
        switch (type) {
            case "PostgreSQL":
                return DbType.POSTGRESQL;
            case "MySQL":
                return DbType.MYSQL;
            case "Microsoft SQL Server":
                return DbType.MSSQL;
            case "Oracle":
                return DbType.ORACLE;
            default:
                return DbType.H2;
        }
    }

    @Override
    public String toString() {
        return getDbType();
    }
}
