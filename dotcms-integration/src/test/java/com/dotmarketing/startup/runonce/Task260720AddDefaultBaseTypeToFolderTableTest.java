package com.dotmarketing.startup.runonce;


import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import java.sql.SQLException;
import org.junit.BeforeClass;
import org.junit.Test;

public class Task260720AddDefaultBaseTypeToFolderTableTest {

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    private void dropColumn(final DotConnect dotConnect) {

        Logger.debug(this, "Prepping for testing `add` column.");
        try {
            DbConnectionFactory.getConnection().setAutoCommit(true);
            final String dropColumnSQL = "ALTER TABLE folder DROP COLUMN default_base_type";
            dotConnect.executeStatement(dropColumnSQL);
        } catch (SQLException e) {
            Logger.warn(Task260720AddDefaultBaseTypeToFolderTableTest.class, e.getMessage(), e);
        }
    }

    /**
     * Given scenario: We drop the column if it already exists, then run the upgrade task.
     * Expected Results: The column must be present, the task is idempotent (forceRun is false once
     * the column exists) and re-running the upgrade does not fail.
     */
    @Test
    public void Test_UpgradeTask_Success() throws DotDataException, SQLException {

        final DotConnect dotConnect = new DotConnect();
        dropColumn(dotConnect);
        final Task260720AddDefaultBaseTypeToFolderTable task =
                new Task260720AddDefaultBaseTypeToFolderTable();
        assertTrue(task.forceRun());
        task.executeUpgrade();

        final DotDatabaseMetaData databaseMetaData = new DotDatabaseMetaData();
        assertTrue(databaseMetaData.hasColumn("folder", "default_base_type"));

        // Idempotent: once the column exists, forceRun must be false
        assertFalse(task.forceRun());
    }
}
