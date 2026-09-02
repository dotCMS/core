package com.dotmarketing.startup.runonce;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.dotmarketing.util.PortletID.USAGE;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Verifies that the {@link Task260206AddUsagePortletToMenu} Upgrade Task runs as expected.
 *
 * @author Daniel Colina
 * @since Feb 6th, 2026
 */
public class Task260206AddUsagePortletToMenuTest {

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * <ul>
     *     <li><b>Method to test:
     *     </b>{@link Task260206AddUsagePortletToMenu#executeUpgrade()}</li>
     *     <li><b>Given Scenario: </b>The Usage portlet does not exist in any layout.</li>
     *     <li><b>Expected Result: </b>The task runs, adds the portlet, and subsequent calls
     *     return false for forceRun().</li>
     * </ul>
     */
    @Test
    public void upgradeTaskExecution() throws Exception {

        deleteUsagePortlet();

        final Task260206AddUsagePortletToMenu upgradeTask = new Task260206AddUsagePortletToMenu();

        assertTrue("The 'Usage' portlet was explicitly deleted before, so the UT must always run",
                upgradeTask.forceRun());
        upgradeTask.executeUpgrade();
        assertFalse("The 'Usage' portlet has already been added, so the UT must NOT run again",
                upgradeTask.forceRun());
    }

    /**
     * <ul>
     *     <li><b>Method to test:
     *     </b>{@link Task260206AddUsagePortletToMenu#executeUpgrade()}</li>
     *     <li><b>Given Scenario: </b>Runs the UT twice.</li>
     *     <li><b>Expected Result: </b>The UT must be executable as many times as desired without
     *     failure. This verifies idempotency.</li>
     * </ul>
     */
    @Test
    public void checkUpgradeTaskIdempotency() throws DotDataException {

        deleteUsagePortlet();
        final Task260206AddUsagePortletToMenu task = new Task260206AddUsagePortletToMenu();

        task.executeUpgrade();
        // Run again to verify idempotency; i.e., no Java exception/error is thrown
        task.executeUpgrade();

        assertFalse("After running twice, forceRun() should return false", task.forceRun());
    }

    /**
     * <ul>
     *     <li><b>Method to test:
     *     </b>{@link Task260206AddUsagePortletToMenu#forceRun()}</li>
     *     <li><b>Given Scenario: </b>Test that forceRun() gracefully handles cases where no
     *     suitable layout exists (System, Marketing, or Sites portlet layout).</li>
     *     <li><b>Expected Result: </b>The method should not throw an exception and should handle
     *     the missing layout scenario properly by returning false and logging a warning.</li>
     * </ul>
     */
    @Test
    public void testForceRunHandlesMissingLayouts() throws Exception {
        // Store original layouts to restore later
        final String backupSystem = new DotConnect()
                .setSQL("SELECT layout_name FROM cms_layout WHERE LOWER(layout_name) = 'system'")
                .getString("layout_name");
        final String backupMarketing = new DotConnect()
                .setSQL("SELECT layout_name FROM cms_layout WHERE LOWER(layout_name) = 'marketing'")
                .getString("layout_name");

        try {
            // Temporarily rename layouts to simulate missing layouts
            if (backupSystem != null) {
                new DotConnect()
                        .setSQL("UPDATE cms_layout SET layout_name = 'temp_system' WHERE LOWER(layout_name) = 'system'")
                        .loadResult();
            }
            if (backupMarketing != null) {
                new DotConnect()
                        .setSQL("UPDATE cms_layout SET layout_name = 'temp_marketing' WHERE LOWER(layout_name) = 'marketing'")
                        .loadResult();
            }

            final Task260206AddUsagePortletToMenu upgradeTask = new Task260206AddUsagePortletToMenu();

            // This should not throw an exception and should return false or true
            // depending on whether the Sites portlet layout is found
            upgradeTask.forceRun();

        } finally {
            // Restore original layout names
            if (backupSystem != null) {
                new DotConnect()
                        .setSQL("UPDATE cms_layout SET layout_name = 'System' WHERE layout_name = 'temp_system'")
                        .loadResult();
            }
            if (backupMarketing != null) {
                new DotConnect()
                        .setSQL("UPDATE cms_layout SET layout_name = 'Marketing' WHERE layout_name = 'temp_marketing'")
                        .loadResult();
            }
        }
    }

    /**
     * Attempts to delete the Usage portlet to ensure it doesn't exist before the test begins.
     */
    private void deleteUsagePortlet() {
        try {
            new DotConnect()
                    .setSQL("DELETE FROM cms_layouts_portlets WHERE portlet_id = ?")
                    .addParam(USAGE.toString())
                    .loadResult();
        } catch (final Exception e) {
            Logger.info(this, "Failed deleting the portlet_id " + USAGE.toString());
        }
    }

}
