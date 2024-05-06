package com.dotmarketing.startup.runonce;


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

public class Task220214AddOwnerAndIDateToFolderTableTest {

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    private void dropColumn(final DotConnect dotConnect) {

        Logger.debug(this, "Prepping for testing `add` column.");
        try {
                DbConnectionFactory.getConnection().setAutoCommit(true);
                String dropColumnSQL = "ALTER TABLE folder DROP COLUMN owner";
                dotConnect.executeStatement(dropColumnSQL);

                dropColumnSQL = "ALTER TABLE folder DROP COLUMN idate";
                dotConnect.executeStatement(dropColumnSQL);

        } catch (SQLException e) {
            Logger.warn(Task220214AddOwnerAndIDateToFolderTableTest.class, e.getMessage(), e);
        }
    }

    /**
     * Given scenario: We drop the columns if it already exists. Run the upgrade task
     * Expected Results: The columns must be there
     * @throws DotDataException
     */
    @Test
    public void Test_UpgradeTask_Success() throws DotDataException, SQLException {

        final DotConnect dotConnect = new DotConnect();
        dropColumn(dotConnect);
        final Task220214AddOwnerAndIDateToFolderTable task = new Task220214AddOwnerAndIDateToFolderTable();
        assertTrue(task.forceRun());
        task.executeUpgrade();

        final DotDatabaseMetaData databaseMetaData = new DotDatabaseMetaData();
        assertTrue(databaseMetaData.hasColumn("folder", "owner"));
        assertTrue(databaseMetaData.hasColumn("folder", "idate"));
    }
}
