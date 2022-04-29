package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.startup.StartupTask;

/**
 * Increase the PUBLISHING_PUSHED_ASSETS.ASSET_ID column capacity to 255 since after pushing plugins we can have long
 * names stored in this column.
 */
public class Task220413IncreasePublishedPushedAssetIdCol implements StartupTask {

    static final String PG_INCREASE_COL_CAPACITY_SQL =
            "ALTER TABLE publishing_pushed_assets ALTER COLUMN asset_id TYPE VARCHAR(%d)";
    static final String MSSQL_INCREASE_COL_CAPACITY_SQL =
            "ALTER TABLE publishing_pushed_assets ALTER COLUMN asset_id NVARCHAR(%d) NOT NULL";

    @Override
    public boolean forceRun() {
        return true;
    }

    @Override
    public void executeUpgrade() throws DotDataException {
        new DotConnect()
                .setSQL(resolveAlterCommand(255))
                .loadResult();
    }

    static String resolveAlterCommand(int capacity) {
        return String.format(
                (DbConnectionFactory.isMsSql()
                        ? MSSQL_INCREASE_COL_CAPACITY_SQL
                        : PG_INCREASE_COL_CAPACITY_SQL),
                capacity);
    }

}
