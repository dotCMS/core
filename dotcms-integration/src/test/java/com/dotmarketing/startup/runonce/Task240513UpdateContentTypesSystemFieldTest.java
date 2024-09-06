package com.dotmarketing.startup.runonce;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.SQLException;

import static org.junit.Assert.*;

public class Task240513UpdateContentTypesSystemFieldTest {

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @Test
    public void testExecuteUpgrade() throws DotDataException, SQLException, DotSecurityException {
        //Update all content types to system = true
        final DotConnect dc = new DotConnect();
        dc.setSQL("update structure set system = true");
        dc.loadResult();
        //Flush cache to get latest changes
        CacheLocator.getContentTypeCache2().clearCache();
        //Select all content types to check if all of them are system = false
        dc.setSQL("select count(*) from structure where system = false");
        int count = dc.loadInt("count");
        //Check that count is 0
        assertEquals("Content Types are system = false",0, count);
        //Run UT
        final Task240513UpdateContentTypesSystemField upgradeTask = new Task240513UpdateContentTypesSystemField();
        upgradeTask.executeUpgrade();
        //Flush cache to get latest changes
        CacheLocator.getContentTypeCache2().clearCache();
        //Check that content types are system = false
        dc.setSQL("select count(*) from structure where system = false");
        count = dc.loadInt("count");
        //Check that count is greater than 0
        assertTrue("Content Types are system = false", count > 0);
        //Select Content Types that are system = true
        dc.setSQL("select count(*) from structure where system = true");
        count = dc.loadInt("count");
        //Check that count is 3
        assertEquals("Some Content Types are system = true",3, count);
    }
}
