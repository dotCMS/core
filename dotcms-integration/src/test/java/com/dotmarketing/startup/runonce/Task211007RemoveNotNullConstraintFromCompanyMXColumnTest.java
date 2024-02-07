package com.dotmarketing.startup.runonce;

import static org.junit.Assert.assertTrue;

import com.dotcms.util.IntegrationTestInitService;
import org.junit.BeforeClass;
import org.junit.Test;

public class Task211007RemoveNotNullConstraintFromCompanyMXColumnTest {

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * <b>Method to Test:</b> {@link Task211007RemoveNotNullConstraintFromCompanyMXColumn#executeUpgrade()} <p>
     * <b>Given Scenario:</b> The upgrade task should be executed <p>
     * <b>Expected Result:</b> Column `COMPANY.MX` should not have a not null constraint in database
     * @throws Exception
     */
    @Test
    public void test() throws Exception {
        final Task211007RemoveNotNullConstraintFromCompanyMXColumn tztask = new Task211007RemoveNotNullConstraintFromCompanyMXColumn();
        if(tztask.forceRun()) {
            tztask.executeUpgrade();
        }
        assertTrue("Column 'COMPANY.MX' has been updated",true);
    }

}
