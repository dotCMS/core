package com.dotmarketing.startup.runonce;

import com.dotmarketing.startup.AbstractJDBCStartupTask;

import java.util.List;

/**
 * @author Jonathan Gamba
 *         Date: 1/27/16
 */
public class Task03520AlterTagsForPersonas extends AbstractJDBCStartupTask {

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
        return "ALTER TABLE tag ADD COLUMN persona BOOLEAN DEFAULT FALSE;\n" +
                "ALTER TABLE tag ADD COLUMN mod_date TIMESTAMP;\n" +
                "ALTER TABLE tag_inode ADD COLUMN mod_date TIMESTAMP;\n" +
                "CREATE INDEX tag_is_persona_index ON tag(persona);";
    }

    /**
     * The SQL for MySQL
     *
     * @return
     */
    @Override
    public String getMySQLScript () {
        return "ALTER TABLE tag ADD persona BOOLEAN DEFAULT FALSE;\n" +
                "ALTER TABLE tag ADD mod_date DATETIME;\n" +
                "ALTER TABLE tag_inode ADD mod_date DATETIME;\n" +
                "CREATE INDEX tag_is_persona_index ON tag(persona);";
    }

    /**
     * The SQL for Oracle
     *
     * @return
     */
    @Override
    public String getOracleScript () {
        return "ALTER TABLE tag ADD persona number(1,0) default 0;\n" +
                "ALTER TABLE tag ADD mod_date DATE;\n" +
                "ALTER TABLE tag_inode ADD mod_date DATE;\n" +
                "CREATE INDEX tag_is_persona_index ON tag(persona);";
    }

    /**
     * The SQL for MSSQL
     *
     * @return
     */
    @Override
    public String getMSSQLScript () {
        return "ALTER TABLE tag ADD persona TINYINT DEFAULT 0;\n" +
                "ALTER TABLE tag ADD mod_date DATETIME NULL;\n" +
                "ALTER TABLE tag_inode ADD mod_date DATETIME NULL;\n" +
                "CREATE INDEX tag_is_persona_index ON tag(persona);";
    }

    /**
     * The SQL for H2
     *
     * @return
     */
    @Override
    public String getH2Script () {
        return "ALTER TABLE tag ADD persona BOOLEAN DEFAULT FALSE;\n" +
                "ALTER TABLE tag ADD mod_date TIMESTAMP;\n" +
                "ALTER TABLE tag_inode ADD mod_date TIMESTAMP;\n" +
                "CREATE INDEX tag_is_persona_index ON tag(persona);";
    }

    @Override
    protected List<String> getTablesToDropConstraints () {
        return null;
    }

}
