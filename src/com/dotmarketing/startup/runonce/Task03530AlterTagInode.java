package com.dotmarketing.startup.runonce;

import com.dotmarketing.startup.AbstractJDBCStartupTask;

import java.util.List;

/**
 * @author Jonathan Gamba
 *         Date: 2/5/16
 */
public class Task03530AlterTagInode extends AbstractJDBCStartupTask {

    @Override
    public boolean forceRun () {
        return true;
    }

    /**
     * The SQL for Postgres
     *
     * @return
     */
    @Override
    public String getPostgresScript () {
        return "ALTER TABLE tag_inode ADD COLUMN field_var_name varchar(255) DEFAULT '';\n" +
                "ALTER TABLE tag_inode DROP CONSTRAINT tag_inode_pkey;\n" +
                "ALTER TABLE tag_inode ADD PRIMARY KEY (tag_id, inode, field_var_name);";
    }

    /**
     * The SQL for MySQL
     *
     * @return
     */
    @Override
    public String getMySQLScript () {
        return "ALTER TABLE tag_inode ADD field_var_name varchar(255) DEFAULT '';\n" +
                "ALTER TABLE tag_inode DROP PRIMARY KEY;\n" +
                "ALTER TABLE tag_inode ADD PRIMARY KEY (tag_id, inode, field_var_name);";
    }

    /**
     * The SQL for Oracle
     *
     * @return
     */
    @Override
    public String getOracleScript () {
        return "ALTER TABLE tag_inode ADD field_var_name varchar2(255) DEFAULT '';\n" +
                "ALTER TABLE tag_inode DROP CONSTRAINT pk_tag_inode;\n" +
                "ALTER TABLE tag_inode ADD PRIMARY KEY (tag_id, inode, field_var_name);";
    }

    /**
     * The SQL for MSSQL
     *
     * @return
     */
    @Override
    public String getMSSQLScript () {
        return "ALTER TABLE tag_inode ADD field_var_name varchar(255) DEFAULT '';\n" +
                "ALTER TABLE tag_inode DROP CONSTRAINT pk_tag_inode;\n" +
                "ALTER TABLE tag_inode ADD PRIMARY KEY (tag_id, inode, field_var_name);";
    }

    /**
     * The SQL for H2
     *
     * @return
     */
    @Override
    public String getH2Script () {
        return "ALTER TABLE tag_inode ADD COLUMN field_var_name varchar(255) DEFAULT '';\n" +
                "ALTER TABLE tag_inode DROP CONSTRAINT pk_tag_inode;\n" +
                "ALTER TABLE tag_inode ADD PRIMARY KEY (tag_id, inode, field_var_name);";
    }

    @Override
    protected List<String> getTablesToDropConstraints () {
        return null;
    }

}