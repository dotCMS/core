package com.dotmarketing.startup.runonce;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.LocalTransaction;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class Task210520UpdateAnonymousEmailTest {

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @Test
    public void testExecuteUpgrade() throws Exception {

        LocalTransaction.wrapReturnWithListeners(()->
            new DotConnect().executeUpdate("UPDATE user_ SET emailaddress = ? where emailaddress = ?",
                Task210520UpdateAnonymousEmail.OLD_ANONYMOUS_EMAIL, UserAPI.CMS_ANON_USER_EMAIL)
        );


        LocalTransaction.wrapReturnWithListeners(()-> {
            final Task210520UpdateAnonymousEmail upgradeTask = new Task210520UpdateAnonymousEmail();
            assertTrue(upgradeTask.forceRun());
            upgradeTask.executeUpgrade();
            assertFalse(upgradeTask.forceRun());
            return null;
        });

    }
}
