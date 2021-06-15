package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;
import java.sql.SQLException;

public class Task210527DropReviewFieldsFromContentletTable implements StartupTask {
    @Override
    public boolean forceRun() {
        try {
            final DotDatabaseMetaData dbMetadata = new DotDatabaseMetaData();
            return dbMetadata.hasColumn("contentlet", "last_review") &&
                    dbMetadata.hasColumn("contentlet", "next_review") &&
                    dbMetadata.hasColumn("contentlet", "review_interval");
        } catch (SQLException e) {
            Logger.error(this, e.getMessage(),e);
            return false;
        }
    }

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        try {
            final DotConnect dotConnect = new DotConnect();
            dotConnect.executeStatement("ALTER TABLE contentlet DROP COLUMN last_review;");
            dotConnect.executeStatement("ALTER TABLE contentlet DROP COLUMN next_review;");
            dotConnect.executeStatement("ALTER TABLE contentlet DROP COLUMN review_interval;");
        } catch (SQLException exception) {
            throw new DotDataException(exception.getMessage(),exception);
        }
    }
}
