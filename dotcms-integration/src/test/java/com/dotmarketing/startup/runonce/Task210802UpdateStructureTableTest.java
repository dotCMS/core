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

public class Task210802UpdateStructureTableTest {

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @Test
    public void testExecuteUpgrade() throws DotDataException, SQLException {
        //Remove column if exists
        final DotDatabaseMetaData dotDatabaseMetaData = new DotDatabaseMetaData();
        if (dotDatabaseMetaData.hasColumn("structure", "icon")) {
            dotDatabaseMetaData
                    .dropColumn(DbConnectionFactory.getConnection(), "structure", "icon");
        }
        if (dotDatabaseMetaData.hasColumn("structure", "sort_order")) {
            dotDatabaseMetaData
                    .dropColumn(DbConnectionFactory.getConnection(), "structure", "sort_order");
        }

        final Task210802UpdateStructureTable upgradeTask = new Task210802UpdateStructureTable();
        assertTrue(upgradeTask.forceRun());
        upgradeTask.executeUpgrade();
        assertFalse(upgradeTask.forceRun());//columns were created
    }


}
