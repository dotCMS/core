package com.dotmarketing.startup.runonce;

import static org.junit.Assert.assertFalse;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import org.junit.BeforeClass;
import org.junit.Test;

public class Task210719CleanUpTitleFieldTest {

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @Test
    public void testExecuteUpgrade() throws DotDataException {

        final Task210719CleanUpTitleField upgradeTask = new Task210719CleanUpTitleField();
        upgradeTask.executeUpgrade();
        assertFalse(new DotConnect()
                .setSQL("select count(inode) as test from contentlet where title is not null")
                .getInt("test") > 0);
    }
}
