package com.dotmarketing.startup.runonce;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;

import java.sql.SQLException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class Task230328AddMarkedForDeletionColumnTest {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    @Test
    public void TestUpgradeTask() throws SQLException {
        final DotConnect dotConnect = new DotConnect();
        Task230328AddMarkedForDeletionColumn task = new Task230328AddMarkedForDeletionColumn();
        if(!task.forceRun()){
            String alterTable = "ALTER TABLE structure DROP COLUMN marked_for_deletion";
            dotConnect.executeStatement(alterTable);
        }
        task.executeUpgrade();
        Assert.assertFalse(task.forceRun());
    }


}
