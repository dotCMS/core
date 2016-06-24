package com.dotmarketing.startup.runonce;

import java.util.List;

import com.dotmarketing.startup.AbstractJDBCStartupTask;

/**
 * Task to change password_ column type from VARCHAR(100) to TEXT in user_ table
 * 
 * @author Rogelio Blanco
 * @version 1.0
 * @since 12-15-2015
 */
public class Task03515AlterPasswordColumnFromUserTable extends AbstractJDBCStartupTask {

    @Override
    public boolean forceRun() {
        return true;
    }

    /**
     * The SQL for Postgres
     *
     * @return
     */
    @Override
    public String getPostgresScript() {
        return "ALTER TABLE user_ ALTER COLUMN password_ TYPE TEXT;";
    }

    /**
     * The SQL for MySQL
     *
     * @return
     */
    @Override
    public String getMySQLScript() {
        return "ALTER TABLE `user_` MODIFY COLUMN `password_` LONGTEXT NULL DEFAULT NULL;";
    }

    /**
     * The SQL for Oracle
     *
     * @return
     */
    @Override
    public String getOracleScript() {
        return "ALTER TABLE user_ ADD (tmpPassword_ NCLOB);" +
        "UPDATE user_ SET tmpPassword_ = password_;" + 
        "ALTER TABLE user_ DROP COLUMN password_;" + 
        "ALTER TABLE user_ RENAME COLUMN tmpPassword_ TO password_;";
    }

    /**
     * The SQL for MSSQL
     *
     * @return
     */
    @Override
    public String getMSSQLScript() {
        return "ALTER TABLE user_ ALTER COLUMN password_ TEXT NULL;";
    }

    /**
     * The SQL for H2
     *
     * @return
     */
    @Override
    public String getH2Script() {
        return "ALTER TABLE user_ ALTER COLUMN password_ TEXT NULL;";
    }

    @Override
    protected List<String> getTablesToDropConstraints() {
        return null;
    }

}
