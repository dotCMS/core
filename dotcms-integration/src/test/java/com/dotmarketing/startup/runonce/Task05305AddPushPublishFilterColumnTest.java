package com.dotmarketing.startup.runonce;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import com.dotmarketing.exception.DotDataException;
import java.sql.SQLException;
import org.junit.BeforeClass;
import org.junit.Test;

public class Task05305AddPushPublishFilterColumnTest {

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    private void dropColumn() throws SQLException {
        final String dropColumnSQL = "ALTER TABLE publishing_bundle DROP COLUMN filter_key";
        final DotConnect dotConnect = new DotConnect();
        dotConnect.executeStatement(dropColumnSQL);
    }

    @Test
    public void test_upgradeTask_success() throws SQLException, DotDataException {
        dropColumn();
        final Task05305AddPushPublishFilterColumn task = new Task05305AddPushPublishFilterColumn();
        assertTrue(task.forceRun());//True because the column does not exists
        task.executeUpgrade();
        assertFalse(task.forceRun());//False because the column exists
    }

}
