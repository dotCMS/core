package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.startup.AbstractJDBCStartupTask;
import com.dotmarketing.util.Logger;

import java.sql.SQLException;
import java.util.List;

/**
 * Upgrade task used to remove the not-null constraint from `Company.mx` if needed
 */
public class Task211007RemoveNotNullConstraintFromCompanyMXColumn extends AbstractJDBCStartupTask {

    @Override
    public boolean forceRun() {
        try {
            return !new DotDatabaseMetaData().hasColumn("company", "mx") ;
        } catch (SQLException e) {
            Logger.error(this, e.getMessage(),e);
            return false;
        }
    }

    @Override
    public String getPostgresScript() {
        return "ALTER TABLE storage ADD COLUMN hash_ref varchar(64);";
    }

    @Override
    public String getMySQLScript() {
        return "ALTER TABLE Company MODIFY mx varchar(100) default 'dotcms.com';";
    }

    @Override
    public String getOracleScript() {
        return "ALTER TABLE Company MODIFY mx varchar2(100) default 'dotcms.com';";
    }

    @Override
    public String getMSSQLScript() {
        return "ALTER TABLE Company ALTER COLUMN mx NVARCHAR(100) default 'dotcms.com';";
    }

    @Override
    protected List<String> getTablesToDropConstraints() {
        return null;
    }
}
