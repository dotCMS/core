package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.startup.AbstractJDBCStartupTask;
import com.dotmarketing.util.Logger;
import io.vavr.control.Try;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

/**
 * 1. Adds `mod_date` column to `relationship` table
 * 2. Copies `inode.idate` to new `relationship.mod_date`
 * 3. Drops FK from `relationship` to `inode`
 */
public class Task210816DeInodeRelationship extends AbstractJDBCStartupTask {

    private final DotDatabaseMetaData dotDatabaseMetaData = new DotDatabaseMetaData();
    private final String COPY_RELATIONSHIP_MOD_DATE_FROM_INODE =
            "UPDATE relationship as r SET mod_date = i.idate FROM inode i WHERE r.inode = i.inode;";
    private final String DELETE_RELATIONSHIPS_FROM_INODE_BY_TYPE = "DELETE FROM inode WHERE type = 'relationship';";
    private final String DELETE_RELATIONSHIPS_FROM_INODE_BY_JOIN = "DELETE FROM inode where exists(select 1 from relationship r where r.inode = inode.inode);";


    @Override
    public boolean forceRun() {
        try {
            return !dotDatabaseMetaData.hasColumn("relationship", "mod_date")
                    || Try.of(()->dotDatabaseMetaData.getConstraints("relationship")).
                    getOrElse(Collections.emptyList())
                    .stream().anyMatch("fkf06476385fb51eb"::equals);
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
               + DELETE_RELATIONSHIPS_FROM_INODE_BY_TYPE
               + DELETE_RELATIONSHIPS_FROM_INODE_BY_JOIN
               + getRemoveFKSQL();
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
                + DELETE_RELATIONSHIPS_FROM_INODE_BY_TYPE
                + DELETE_RELATIONSHIPS_FROM_INODE_BY_JOIN
                + getRemoveFKSQL();
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
                + DELETE_RELATIONSHIPS_FROM_INODE_BY_TYPE
                + DELETE_RELATIONSHIPS_FROM_INODE_BY_JOIN
                + getRemoveFKSQL();
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
                + DELETE_RELATIONSHIPS_FROM_INODE_BY_TYPE
                + DELETE_RELATIONSHIPS_FROM_INODE_BY_JOIN
                + getRemoveFKSQL();
    }

    private String getRemoveFKSQL() {
        String removeFK = "";

        if(Try.of(()->dotDatabaseMetaData.getConstraints("relationship")).
                getOrElse(Collections.emptyList())
                .stream().anyMatch("fkf06476385fb51eb"::equals)) {
            removeFK = "ALTER TABLE relationship DROP CONSTRAINT fkf06476385fb51eb;";
        }
        return removeFK;
    }

    @Override
    protected List<String> getTablesToDropConstraints() {
        return null;
    }
}
