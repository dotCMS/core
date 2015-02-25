package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.startup.AbstractJDBCStartupTask;
import com.dotmarketing.util.Logger;

import java.util.List;

/**
 * Created by Oscar Arrieta on 11/18/14.
 *
 * Task to create new table for Html Pages Integrity Results.
 */
public class Task03065AddHtmlPageIR extends AbstractJDBCStartupTask {

    /**
     * By Default tasks only execute once.  If you have a task that needs to execute more then once use this method.
     * In this case we need to check if htmlpages_ir table already exists,
     * could have been created in 2.5.7 (Task01097AddHtmlPageIR.java).
     *
     * @return
     */

    private String languageIdColumn = DbConnectionFactory.isH2()||DbConnectionFactory.isMySql()?" language_id bigint "
            :DbConnectionFactory.isPostgres()?" language_id int8 "
            :DbConnectionFactory.isOracle()?" language_id number(19,0) "
            :DbConnectionFactory.isMsSql()?" language_id numeric(19,0) ":"";

    private final String NON_ORACLE_SQL =
            "create table htmlpages_ir(html_page varchar(255), local_working_inode varchar(36), local_live_inode varchar(36)," +
            "remote_working_inode varchar(36), remote_live_inode varchar(36), " +
            "local_identifier varchar(36), remote_identifier varchar(36), endpoint_id varchar(36), " + languageIdColumn + "," +
            "PRIMARY KEY (local_working_inode, language_id, endpoint_id)); ";

    private final String ORACLE_SQL =
            "create table htmlpages_ir(html_page varchar2(255), local_working_inode varchar2(36), local_live_inode varchar2(36)," +
            "remote_working_inode varchar2(36), remote_live_inode varchar2(36), " +
            "local_identifier varchar2(36), remote_identifier varchar2(36), endpoint_id varchar2(36), " + languageIdColumn + "," +
            "PRIMARY KEY (local_working_inode, language_id, endpoint_id)); ";
;

    @Override
    public boolean forceRun() {
        try {
            DotConnect dc=new DotConnect();
            dc.setSQL("select * from htmlpages_ir");
            dc.loadResult();
            return false;
        }
        catch(Exception ex) {
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
     * This is a list of tables which will get the constraints dropped prior to the task executing and then get recreated afer the execution of the DB Specific SQL
     *
     * @return
     */
    @Override
    protected List<String> getTablesToDropConstraints() {
        return null;
    }

}
