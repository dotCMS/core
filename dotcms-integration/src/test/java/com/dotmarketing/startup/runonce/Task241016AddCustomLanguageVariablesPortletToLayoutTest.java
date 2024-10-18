package com.dotmarketing.startup.runonce;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.dotmarketing.startup.runonce.Task241016AddCustomLanguageVariablesPortletToLayout.LANGUAGE_VARIABLES_PORTLET_ID;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Verifies that the {@link Task241016AddCustomLanguageVariablesPortletToLayout} task works
 * correctly.
 *
 * @author Jose Castro
 * @since Oct 15th, 2024
 */
public class Task241016AddCustomLanguageVariablesPortletToLayoutTest {

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * <ul>
     *     <li><b>Method to test:
     *     </b>{@link Task241016AddCustomLanguageVariablesPortletToLayout#executeUpgrade()}</li>
     *     <li><b>Given Scenario: </b>Runs the UT.</li>
     *     <li><b>Expected Result: </b>The Language Variables portlet is added successfully.</li>
     * </ul>
     */
    @Test
    public void runUpgradeTask() throws DotDataException {
        // ╔══════════════════╗
        // ║  Initialization  ║
        // ╚══════════════════╝
        final DotConnect dotConnect = new DotConnect();
        this.deleteLanguageVariablesPortlet(dotConnect);
        final Task241016AddCustomLanguageVariablesPortletToLayout task =
                new Task241016AddCustomLanguageVariablesPortletToLayout();
        task.executeUpgrade();

        // ╔══════════════╗
        // ║  Assertions  ║
        // ╚══════════════╝
        assertFalse("The Language Variables portlet already exists, this must return 'false'"
                , task.forceRun());
    }

    /**
     * <ul>
     *     <li><b>Method to test:
     *     </b>{@link Task241016AddCustomLanguageVariablesPortletToLayout#executeUpgrade()}</li>
     *     <li><b>Given Scenario: </b>Runs the UT twice.</li>
     *     <li><b>Expected Result: </b>The UT must be executable as many times as desired without
     *     failure.</li>
     * </ul>
     */
    @Test
    public void checkUpgradeTaskIdempotency() throws DotDataException {
        // ╔══════════════════╗
        // ║  Initialization  ║
        // ╚══════════════════╝
        final DotConnect dotConnect = new DotConnect();
        this.deleteLanguageVariablesPortlet(dotConnect);
        final Task241016AddCustomLanguageVariablesPortletToLayout task =
                new Task241016AddCustomLanguageVariablesPortletToLayout();

        task.executeUpgrade();
        // Run again to verify idempotency; i.e., no Java exception/error is thrown
        task.executeUpgrade();
    }

    /**
     * Attempts to delete the custom Language Variable portlet to double-check that it doesn't exist
     * before the test begins.
     *
     * @param dotConnect The {@link DotConnect} object.
     */
    private void deleteLanguageVariablesPortlet(final DotConnect dotConnect) {
        try {
            dotConnect.setSQL("delete from cms_layouts_portlets where portlet_id = ?")
                    .addParam(LANGUAGE_VARIABLES_PORTLET_ID)
                    .loadResult();
        } catch (final Exception e) {
            Logger.info(this, "Failed deleting the portlet_id " + LANGUAGE_VARIABLES_PORTLET_ID);
        }
    }

}
