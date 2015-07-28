package com.dotmarketing.startup.runonce;

import java.util.List;

import com.dotcms.integritycheckers.IntegrityType;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.startup.AbstractJDBCStartupTask;

/**
 * Task to create new table for File Assets Integrity Results.
 * 
 * @author Rogelio Blanco
 * @version 1.0
 * @since 06-10-2015
 * 
 */
public class Task03140AddFileAssetsIntegrityResultTable extends AbstractJDBCStartupTask {
    private final String LANGUAGE_ID_COLUMN = DbConnectionFactory.isH2()
            || DbConnectionFactory.isMySql() ? " language_id bigint " : DbConnectionFactory
            .isPostgres() ? " language_id int8 "
            : DbConnectionFactory.isOracle() ? " language_id number(19,0) " : DbConnectionFactory
                    .isMsSql() ? " language_id numeric(19,0) " : "";

    private final String NON_ORACLE_SQL = new StringBuilder("create table ")
            .append(IntegrityType.FILEASSETS.getResultsTableName())
            .append(" (").append(IntegrityType.FILEASSETS.getFirstDisplayColumnLabel())
            .append(" varchar(255), local_working_inode varchar(36), local_live_inode varchar(36), ")
            .append("remote_working_inode varchar(36), remote_live_inode varchar(36), ")
            .append("local_identifier varchar(36), remote_identifier varchar(36), endpoint_id varchar(36), ")
            .append(LANGUAGE_ID_COLUMN)
            .append(", PRIMARY KEY (local_working_inode, language_id, endpoint_id));").toString();

    private final String ORACLE_SQL = NON_ORACLE_SQL.replaceAll("varchar\\(", "varchar2\\(");

    @Override
    public boolean forceRun() {
        try {
            DotConnect dc = new DotConnect();
            dc.setSQL("SELECT * FROM " + IntegrityType.FILEASSETS.getResultsTableName());
            dc.loadResult();
            return false;
        } catch (Exception ex) {
            return true;
        }
    }

    /**
     * The SQL for Postgres
     *
     * @return
     */
    @Override
    public String getPostgresScript() {
        return NON_ORACLE_SQL;
    }

    /**
     * The SQL MySQL
     *
     * @return
     */
    @Override
    public String getMySQLScript() {
        return NON_ORACLE_SQL;
    }

    /**
     * The SQL for Oracle
     *
     * @return
     */
    @Override
    public String getOracleScript() {
        return ORACLE_SQL;
    }

    /**
     * The SQL for MSSQL
     *
     * @return
     */
    @Override
    public String getMSSQLScript() {
        return NON_ORACLE_SQL;
    }

    /**
     * The SQL for H2
     *
     * @return
     */
    @Override
    public String getH2Script() {
        return NON_ORACLE_SQL;
    }

    /**
     * This is a list of tables which will get the constraints dropped prior to
     * the task executing and then get recreated afer the execution of the DB
     * Specific SQL
     *
     * @return
     */
    @Override
    protected List<String> getTablesToDropConstraints() {
        return null;
    }

}
