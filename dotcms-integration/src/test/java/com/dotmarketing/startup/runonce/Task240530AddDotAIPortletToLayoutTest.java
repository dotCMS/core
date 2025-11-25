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
 * Test adding the dotAI portlet to the layout
 * @author dsilvam
 */
public class Task240530AddDotAIPortletToLayoutTest {

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    private void dropAppPortlet(final DotConnect dotConnect) {
        try {
            final String dropAppPortletSQL = "delete from cms_layouts_portlets where portlet_id = 'dotai'";
            dotConnect.executeStatement(dropAppPortletSQL);
        } catch (Exception e) {
            Logger.info(Task240530AddDotAIPortletToLayoutTest.class, () -> "Failed deleting the portlet_id dotai");
        }
    }

    /**
     * Method to test: executeUpgrade
     * Given Scenario: testing when the dotAI has been and hasn't been added to the layout
     * ExpectedResult: after running the task, the dotAI should be added to the layout
     *
     *
     * @throws DotDataException
     */
    @Test
    public void test_upgradeTask_success() throws DotDataException {
        final DotConnect dotConnect = new DotConnect();
        dropAppPortlet(dotConnect);
        final Task240530AddDotAIPortletToLayout task = new Task240530AddDotAIPortletToLayout();
        assertTrue(task.forceRun());//True because the dotAI does not exist
        task.executeUpgrade();
        assertFalse(task.forceRun());//False because the dotAI exists

        dotConnect.setSQL("select count(portlet_id) as count from cms_layouts_portlets where portlet_id = 'dotai'");
        assertTrue("There can only be 1.", dotConnect.getInt("count") > 0);
    }

}
