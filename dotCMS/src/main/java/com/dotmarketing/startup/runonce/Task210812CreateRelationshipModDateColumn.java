package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.startup.AbstractJDBCStartupTask;
import com.dotmarketing.util.Logger;
import java.sql.SQLException;
import java.util.List;

/**
 * Upgrade task used to add column `mod_date` to `relationship` table
 */
public class Task210812CreateRelationshipModDateColumn extends AbstractJDBCStartupTask {

    private final DotDatabaseMetaData dotDatabaseMetaData = new DotDatabaseMetaData();
    private final String COPY_RELATIONSHIP_MOD_DATE_FROM_INODE =
            "UPDATE relationship as r SET mod_date = i.idate FROM inode i WHERE r.inode = i.inode;";
    private final String REMOVE_FK_TO_INODE = "ALTER TABLE relationship DROP CONSTRAINT fkf06476385fb51eb;";

    @Override
    public boolean forceRun() {
        try {
            return !dotDatabaseMetaData.hasColumn("relationship", "mod_date");
        } catch (SQLException e) {
            Logger.error(this, e.getMessage(),e);
            return false;
        }
    }

    /**
     * Returns the upgrade SQL query for PostgreSQL.
     *
     * @return The SQL statement(s).
     */
    @Override
    public String getPostgresScript() {
       return "ALTER TABLE relationship ADD mod_date timestamp;"
               + COPY_RELATIONSHIP_MOD_DATE_FROM_INODE
               + REMOVE_FK_TO_INODE;
    }

    /**
     * Returns the upgrade SQL query for MySQL.
     *
     * @return The SQL statement(s).
     */
    @Override
    public String getMySQLScript() {
        return "ALTER TABLE relationship ADD mod_date datetime;"
                + COPY_RELATIONSHIP_MOD_DATE_FROM_INODE
                + REMOVE_FK_TO_INODE;
    }

    /**
     * Returns the upgrade SQL query for Oracle.
     *
     * @return The SQL statement(s).
     */
    @Override
    public String getOracleScript() {
        return "ALTER TABLE relationship ADD mod_date date;"
                + COPY_RELATIONSHIP_MOD_DATE_FROM_INODE
                + REMOVE_FK_TO_INODE;
    }

    /**
     * Returns the upgrade SQL query for Microsoft SQL Server.
     *
     * @return The SQL statement(s).
     */
    @Override
    public String getMSSQLScript() {
        return "ALTER TABLE relationship ADD mod_date datetime null;"
                + COPY_RELATIONSHIP_MOD_DATE_FROM_INODE
                + REMOVE_FK_TO_INODE;
    }

    @Override
    protected List<String> getTablesToDropConstraints() {
        return null;
    }
}
