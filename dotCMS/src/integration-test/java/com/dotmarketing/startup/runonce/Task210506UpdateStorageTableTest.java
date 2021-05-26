package com.dotmarketing.startup.runonce;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import java.sql.SQLException;
import org.junit.BeforeClass;
import org.junit.Test;

public class Task210506UpdateStorageTableTest {

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @Test
    public void testExecuteUpgrade() throws DotDataException, SQLException {
        //Remove column if exists
        final DotDatabaseMetaData dotDatabaseMetaData = new DotDatabaseMetaData();
        if (dotDatabaseMetaData.hasColumn("storage", "hash_ref")) {
            dotDatabaseMetaData
                    .dropColumn(DbConnectionFactory.getConnection(), "storage", "hash_ref");
        }

        final Task210506UpdateStorageTable upgradeTask = new Task210506UpdateStorageTable();
        assertTrue(upgradeTask.forceRun());
        upgradeTask.executeUpgrade();
        assertFalse(upgradeTask.forceRun());
    }
}
