package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;

import java.sql.SQLException;

/**
 * This Upgrade Task adds the publish_date column to the contentlet_version_info table
 */
public class Task231109AddPublishDateToContentletVersionInfo implements StartupTask {

    private static final String ADD_PUBLISH_DATE_COLUMN =
            "ALTER TABLE contentlet_version_info ADD publish_date TIMESTAMPTZ NULL";

    private static final String UPDATE_PUBLISH_DATE_COLUMN =
            "UPDATE contentlet_version_info SET publish_date = version_ts WHERE live_inode IS NOT NULL";

    @Override
    public boolean forceRun() {
        return !hasPublishDateColumn();
    }

    @Override
    public void executeUpgrade() {
        if (forceRun()) {
            final DotConnect dotConnect = new DotConnect();
            try {
                Logger.info(this, "Adding the 'publish_date' column to the 'contentlet_version_info' table");
                dotConnect.executeStatement(ADD_PUBLISH_DATE_COLUMN);
                Logger.info(this, "Setting the 'publish_date' column to the current version timestamp for existing content");
                dotConnect.executeStatement(UPDATE_PUBLISH_DATE_COLUMN);
            } catch (SQLException e) {
                throw new DotRuntimeException(e);
            }
        }
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
