package com.dotmarketing.startup.runonce;

import static org.junit.Assert.assertTrue;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.exception.DotDataException;
import org.junit.BeforeClass;
import org.junit.Test;

public class Task240306MigrateLegacyLanguageVariablesTest {

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @Test
    public void testExecuteUpgrade() throws DotDataException{
        final Task240306MigrateLegacyLanguageVariables upgradeTask = new Task240306MigrateLegacyLanguageVariables();
        upgradeTask.executeUpgrade();
        assertTrue(upgradeTask.forceRun());
    }


}
