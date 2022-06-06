package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;

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
        DotConnect dc = new DotConnect();
        HibernateUtil.startTransaction();
        try {
            if (DbConnectionFactory.isMsSql() && !DbConnectionFactory.getAutoCommit()) {
                DbConnectionFactory.getConnection().setAutoCommit(true);
            }
            dc.setSQL(resolveAlterCommand(255)).loadResult();
        } catch (Exception e) {
            HibernateUtil.rollbackTransaction();
            Logger.error(this, e.getMessage(),e);
        }

        HibernateUtil.closeAndCommitTransaction();
    }

    static String resolveAlterCommand(int capacity) {
        return String.format(
                (DbConnectionFactory.isMsSql()
                        ? MSSQL_INCREASE_COL_CAPACITY_SQL
                        : PG_INCREASE_COL_CAPACITY_SQL),
                capacity);
    }

}
