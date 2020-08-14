package com.dotmarketing.startup.runonce;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test adding the APPS portlet to the layout
 * @author jsanca
 */
public class Task537000AddAppsPortletToLayoutTest {

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    private void dropAppPortlet(final DotConnect dotConnect) {
        try {
            final String dropAppPortletSQL = "delete from cms_layouts_portlets where portlet_id = 'apps'";
            dotConnect.executeStatement(dropAppPortletSQL);
        } catch (Exception e) {
            Logger.info(Task537000AddAppsPortletToLayoutTest.class, () -> "Failed deleting the portlet_id apps");
        }
    }

    /**
     * Method to test: executeUpgrade
     * Given Scenario: testing when the apps has been and hasn't been added to the layout
     * ExpectedResult: after running the task, the apps should be added to the layout
     *
     *
     * @throws DotDataException
     */
    @Test
    public void test_upgradeTask_success() throws DotDataException {
        final DotConnect dotConnect = new DotConnect();
        dropAppPortlet(dotConnect);
        final Task537000AddAppsPortletToLayout task = new Task537000AddAppsPortletToLayout();
        assertTrue(task.forceRun());//True because the apps does not exists
        task.executeUpgrade();
        assertFalse(task.forceRun());//False because the apps exists

        dotConnect.setSQL("select count(portlet_id) as count from cms_layouts_portlets where portlet_id = 'apps'");
        assertTrue("There can only be 1.", dotConnect.getInt("count") > 0);
    }

}
