package com.dotmarketing.startup.runonce;

import static org.junit.Assert.assertFalse;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.UtilMethods;
import java.util.List;
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

        final List results = new DotConnect()
                .setSQL("select inode from contentlet where title <> null").loadObjectResults();
        assertFalse(UtilMethods.isSet(results));
    }
}
