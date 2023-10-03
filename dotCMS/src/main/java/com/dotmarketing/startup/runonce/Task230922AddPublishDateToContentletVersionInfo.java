package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;

import java.sql.SQLException;

/**
 * This Upgrade Task adds the publish_date and unpublish_date columns to the contentlet_version_info table
 */
public class Task230922AddPublishDateToContentletVersionInfo implements StartupTask {

    @Override
    public boolean forceRun() {
        return !hasPublishDateColumn();
    }

    @Override
    public void executeUpgrade() {
        if (forceRun()) {
            final DotConnect dotConnect = new DotConnect();
            try {
                Logger.info(this, "Adding the 'publish_date' and 'unpublish_date' columns to the 'contentlet_version_info' table");
                dotConnect.executeStatement(addPublishAndUnpublishDateColumns());
                Logger.info(this, "Setting the 'publish_date' column to the current version timestamp for existing content");
                dotConnect.executeStatement("UPDATE contentlet_version_info SET publish_date = version_ts WHERE live_inode IS NOT NULL");
            } catch (SQLException e) {
                throw new DotRuntimeException(e);
            }
        }
    }

        private String addPublishAndUnpublishDateColumns() {
            final String dataBaseFieldType = DbConnectionFactory.isMsSql() ? "DATETIMEOFFSET(3)" : "TIMESTAMPTZ";

            return String.format(
                    "ALTER TABLE contentlet_version_info ADD publish_date %s NULL, ADD unpublish_date %s NULL",
                    dataBaseFieldType, dataBaseFieldType);
        }

        private boolean hasPublishDateColumn() {
        final DotDatabaseMetaData databaseMetaData = new DotDatabaseMetaData();

        try {
            return databaseMetaData.hasColumn("contentlet_version_info", "publish_date");
        } catch (SQLException e) {
            return false;
        }
    }

}
