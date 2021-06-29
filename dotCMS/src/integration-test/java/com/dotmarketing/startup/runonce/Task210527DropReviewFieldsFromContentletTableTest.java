package com.dotmarketing.startup.runonce;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.exception.DotDataException;
import java.sql.SQLException;
import org.junit.BeforeClass;
import org.junit.Test;

public class Task210527DropReviewFieldsFromContentletTableTest {

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @Test
    public void testExecuteUpgrade() throws DotDataException, SQLException {
        //Add columns before running task
        addColumnsInDB();
        assertTrue(doColumnsExist());
        final Task210527DropReviewFieldsFromContentletTable upgradeTask = new Task210527DropReviewFieldsFromContentletTable();
        assertTrue(upgradeTask.forceRun());
        upgradeTask.executeUpgrade();
        assertFalse(upgradeTask.forceRun());
    }

    private boolean doColumnsExist() throws SQLException {
        final DotDatabaseMetaData dotDatabaseMetaData = new DotDatabaseMetaData();
        return dotDatabaseMetaData.hasColumn("contentlet", "last_review") &&
                dotDatabaseMetaData.hasColumn("contentlet", "next_review") &&
                dotDatabaseMetaData.hasColumn("contentlet", "review_interval");
    }

    private void addColumnsInDB() throws SQLException {

        if (!doColumnsExist()) {
            final DotConnect dotConnect = new DotConnect();
            dotConnect.executeStatement("ALTER TABLE contentlet ADD last_review varchar(255)");
            dotConnect.executeStatement("ALTER TABLE contentlet ADD next_review varchar(255)");
            dotConnect.executeStatement("ALTER TABLE contentlet ADD review_interval varchar(255)");
        }
    }
}
