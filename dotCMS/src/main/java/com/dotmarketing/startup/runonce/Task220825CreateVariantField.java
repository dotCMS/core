package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.AbstractJDBCStartupTask;
import java.sql.SQLException;

public class Task220825CreateVariantField extends AbstractJDBCStartupTask {

    @Override
    public boolean forceRun() {
        return !hasVariantIdColumn();
    }

    private boolean hasVariantIdColumn() {
        final DotDatabaseMetaData databaseMetaData = new DotDatabaseMetaData();

        try {
            return databaseMetaData.hasColumn("contentlet_version_info", "variant_id");
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public String getPostgresScript() {
        return !hasVariantIdColumn() ? getCreateFieldStatement() + ";" + getUpdateStatement() + ";" : null;
    }

    @Override
    public String getMSSQLScript(){
        return !hasVariantIdColumn() ? getCreateFieldStatement() + ";" + getUpdateStatement() + ";" : null;
    }

    private String getCreateFieldStatement() {
        final String dataBaseFieldType = DbConnectionFactory.isMsSql() ? "NVARCHAR" : "varchar";

        return String.format(
                "ALTER TABLE contentlet_version_info ADD variant_id %s(255) default 'DEFAULT' not null",
                dataBaseFieldType);
    }

    private String getUpdateStatement() {
        return "UPDATE contentlet_version_info SET variant_id = 'DEFAULT'";
    }
}
