package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.startup.AbstractJDBCStartupTask;
import com.google.common.annotations.VisibleForTesting;
import io.vavr.Lazy;
import io.vavr.control.Try;
import java.util.List;

/**
 * 1. Adds `mod_date` column to `relationship` table
 * 2. Copies `inode.idate` to new `relationship.mod_date`
 * 3. Drops FK from `relationship` to `inode`
 */
public class Task210816DeInodeRelationship extends AbstractJDBCStartupTask {

    private final DotDatabaseMetaData dotDatabaseMetaData = new DotDatabaseMetaData();
    private final String COPY_RELATIONSHIP_MOD_DATE_FROM_INODE = "UPDATE relationship SET mod_date = i.idate FROM inode i WHERE relationship.inode = i.inode;";//mssql, postgtres
    private final String COPY_RELATIONSHIP_MOD_DATE_FROM_INODE_MYSQL = "UPDATE relationship r, inode i SET r.mod_date = i.idate WHERE r.inode = i.inode;";
    private final String COPY_RELATIONSHIP_MOD_DATE_FROM_INODE_ORACLE = "update RELATIONSHIP r set r.mod_date = (select idate from inode i where r.inode = i.inode);";
    private final String DELETE_RELATIONSHIPS_FROM_INODE_BY_TYPE = "DELETE FROM inode WHERE type = 'relationship';";
    private final String DELETE_RELATIONSHIPS_FROM_INODE_BY_JOIN = "DELETE FROM inode where exists(select 1 from relationship r where r.inode = inode.inode);";
    private final Lazy<com.dotmarketing.common.db.ForeignKey> FK = Lazy.of(this::findRelationshipInodeFK);
    private final Lazy<Boolean> HAS_MOD_DATE = Lazy.of(this::hasModDateColumn);

    @Override
    public boolean forceRun() {
        return !HAS_MOD_DATE.get() || FK.get()!=null;
    }

    /**
     * Returns the upgrade SQL query for PostgreSQL.
     *
     * @return The SQL statement(s).
     */
    @Override
    public String getPostgresScript() {
        return  getAddModDateSQL()
                + COPY_RELATIONSHIP_MOD_DATE_FROM_INODE
                + getRemoveFKSQL()
                + DELETE_RELATIONSHIPS_FROM_INODE_BY_TYPE
                + DELETE_RELATIONSHIPS_FROM_INODE_BY_JOIN;

    }

    /**
     * Returns the upgrade SQL query for MySQL.
     *
     * @return The SQL statement(s).
     */
    @Override
    public String getMySQLScript() {
        return  getAddModDateSQL()
                + COPY_RELATIONSHIP_MOD_DATE_FROM_INODE_MYSQL
                + getRemoveFKSQL()
                + DELETE_RELATIONSHIPS_FROM_INODE_BY_TYPE
                + DELETE_RELATIONSHIPS_FROM_INODE_BY_JOIN;
    }

    /**
     * Returns the upgrade SQL query for Oracle.
     *
     * @return The SQL statement(s).
     */
    @Override
    public String getOracleScript() {
        return  getAddModDateSQL()
                + COPY_RELATIONSHIP_MOD_DATE_FROM_INODE_ORACLE
                + getRemoveFKSQL()
                + DELETE_RELATIONSHIPS_FROM_INODE_BY_TYPE
                + DELETE_RELATIONSHIPS_FROM_INODE_BY_JOIN;
    }

    /**
     * Returns the upgrade SQL query for Microsoft SQL Server.
     *
     * @return The SQL statement(s).
     */
    @Override
    public String getMSSQLScript() {
        return  getAddModDateSQL()
                + COPY_RELATIONSHIP_MOD_DATE_FROM_INODE
                + getRemoveFKSQL()
                + DELETE_RELATIONSHIPS_FROM_INODE_BY_TYPE
                + DELETE_RELATIONSHIPS_FROM_INODE_BY_JOIN;
    }

    private String getAddModDateSQL() {
        String addModDateSQL = "";

        if(!HAS_MOD_DATE.get()) {
            if(DbConnectionFactory.isPostgres()) {
                addModDateSQL = "ALTER TABLE relationship ADD mod_date timestamp;";
            } else if(DbConnectionFactory.isMySql()) {
                addModDateSQL = "ALTER TABLE relationship ADD mod_date datetime;";
            } else if(DbConnectionFactory.isMsSql()) {
                addModDateSQL = "ALTER TABLE relationship ADD mod_date datetime null;";
            } else if(DbConnectionFactory.isOracle()) {
                addModDateSQL = "ALTER TABLE relationship ADD mod_date date;";
            }
        }

        return addModDateSQL;
    }

    private String getRemoveFKSQL() {
        String removeFK = "";

        if(FK.get()!=null) {
            removeFK = DbConnectionFactory.isMySql() ? "ALTER TABLE relationship DROP FOREIGN KEY " + FK.get().fkName() + ";"
                    : "ALTER TABLE relationship DROP CONSTRAINT " + FK.get().fkName() + ";";
        }
        return removeFK;
    }

    @VisibleForTesting
    com.dotmarketing.common.db.ForeignKey findRelationshipInodeFK() {
        return dotDatabaseMetaData
                .findForeignKeys("relationship", "inode",
                        List.of("inode"), List.of("inode"));
    }

    @VisibleForTesting
    boolean hasModDateColumn() {
        return Try.of(()->dotDatabaseMetaData
                .hasColumn("relationship", "mod_date")).getOrElse(false);
    }

    @Override
    protected List<String> getTablesToDropConstraints() {
        return null;
    }
}
