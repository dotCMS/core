package com.dotmarketing.startup.runonce;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.exception.DotDataException;
import java.sql.SQLException;
import org.junit.BeforeClass;
import org.junit.Test;

public class Task210319CreateStorageTableTest {
    @BeforeClass
    public static void prepare() throws Exception{
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @Test
    public void testExecuteUpgrade() throws DotDataException, SQLException {
        removeStorageTableReferences();
        final Task210319CreateStorageTable upgradeTask = new Task210319CreateStorageTable();
        assertTrue(upgradeTask.forceRun());
        upgradeTask.executeUpgrade();
        assertFalse(upgradeTask.forceRun());
    }

    @WrapInTransaction
    private void removeStorageTableReferences() throws SQLException {
        final DotConnect dotConnect = new DotConnect();
        final DotDatabaseMetaData metaData = new DotDatabaseMetaData();
        metaData.dropIndex("storage","idx_storage_hash");
        dotConnect.executeStatement("DROP TABLE storage_x_data;\n"
                                        + "DROP TABLE storage_data;\n"
                                        + "DROP TABLE storage;\n"
                                        + "DROP TABLE storage_group;\n");
    }
}
