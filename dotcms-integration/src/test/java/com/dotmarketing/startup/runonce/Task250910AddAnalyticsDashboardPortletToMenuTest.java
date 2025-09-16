package com.dotmarketing.startup.runonce;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.Connection;

import static com.dotmarketing.util.PortletID.ANALYTICS_DASHBOARD;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Verifies that the {@link Task250910AddAnalyticsDashboardPortletToMenu} Upgrade Task runs as
 * expected.
 *
 * @author Jose Castro
 * @since Sep 11th, 2025
 */
public class Task250910AddAnalyticsDashboardPortletToMenuTest {

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @Test
    public void upgradeTaskExecution() throws Exception {
        // ╔══════════════════╗
        // ║  Initialization  ║
        // ╚══════════════════╝
        new DotConnect()
                .setSQL("DELETE FROM cms_layouts_portlets WHERE portlet_id = ?")
                .addParam(ANALYTICS_DASHBOARD.toString())
                .loadResult();

        // ╔════════════════════════╗
        // ║  Generating Test data  ║
        // ╚════════════════════════╝
        final Task250910AddAnalyticsDashboardPortletToMenu upgradeTask = new Task250910AddAnalyticsDashboardPortletToMenu();

        // ╔══════════════╗
        // ║  Assertions  ║
        // ╚══════════════╝
        assertTrue("The 'Analytics Dashboard' was explicitly deleted before, so the UT must always run",
                upgradeTask.forceRun());
        upgradeTask.executeUpgrade();
        assertFalse("The 'Analytics Dashboard' has already been added, so the UT must NOT run again",
                upgradeTask.forceRun());
    }

}
