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
        return "create table htmlpages_ir(html_page varchar(255), local_inode varchar(36), remote_inode varchar(36), " +
                "local_identifier varchar(36), remote_identifier varchar(36), endpoint_id varchar(36), " +
                "PRIMARY KEY (local_inode, endpoint_id));";
    }

    /**
     * The SQL MySQL
     *
     * @return
     */
    @Override
    public String getMySQLScript() {
        return "create table htmlpages_ir(html_page varchar(255), local_inode varchar(36), remote_inode varchar(36), " +
                "local_identifier varchar(36), remote_identifier varchar(36), endpoint_id varchar(36), " +
                "PRIMARY KEY (local_inode, endpoint_id));";
    }

    /**
     * The SQL for Oracle
     *
     * @return
     */
    @Override
    public String getOracleScript() {
        return "create table htmlpages_ir(html_page varchar2(255), local_inode varchar2(36), remote_inode varchar2(36), " +
                "local_identifier varchar2(36), remote_identifier varchar2(36), endpoint_id varchar2(36), " +
                "PRIMARY KEY (local_inode, endpoint_id))";
    }

    /**
     * The SQL for MSSQL
     *
     * @return
     */
    @Override
    public String getMSSQLScript() {
        return "create table htmlpages_ir(html_page varchar(255), local_inode varchar(36), remote_inode varchar(36), " +
                "local_identifier varchar(36), remote_identifier varchar(36), endpoint_id varchar(36), " +
                "PRIMARY KEY (local_inode, endpoint_id));";
    }

    /**
     * The SQL for H2
     *
     * @return
     */
    @Override
    public String getH2Script() {
        return "create table htmlpages_ir(html_page varchar(255), local_inode varchar(36), remote_inode varchar(36), " +
                "local_identifier varchar(36), remote_identifier varchar(36), endpoint_id varchar(36), " +
                "PRIMARY KEY (local_inode, endpoint_id));";
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
