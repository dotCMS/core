package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.startup.AbstractJDBCStartupTask;
import com.dotmarketing.util.Logger;
import java.sql.SQLException;

/**
 * Adds the {@code style_properties} column to the {@code multi_tree} table. This JSONB/JSON
 * column stores style configuration properties for content placement within containers on pages,
 * allowing for flexible styling of individual content instances.
 *
 * @author Dario Daza
 * @since Nov 3rd, 2025
 */
public class Task251103AddStylePropertiesColumnInMultiTree extends AbstractJDBCStartupTask {

    @Override
    public boolean forceRun() {
        try {
            final DotDatabaseMetaData databaseMetaData = new DotDatabaseMetaData();
            return !databaseMetaData.hasColumn("multi_tree", "style_properties");
        } catch (SQLException e) {
            Logger.error(this, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public String getPostgresScript() {
        return "ALTER TABLE multi_tree ADD COLUMN IF NOT EXISTS style_properties JSONB";
    }

    @Override
    public String getMySQLScript() {
        return "ALTER TABLE multi_tree ADD COLUMN style_properties JSON";
    }

    @Override
    public String getOracleScript() {
        return "ALTER TABLE multi_tree ADD style_properties CLOB CHECK (style_properties IS JSON)";
    }

    @Override
    public String getMSSQLScript() {
        return "ALTER TABLE multi_tree ADD style_properties NVARCHAR(MAX)";
    }
}
