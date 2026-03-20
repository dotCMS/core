package com.dotmarketing.startup.runonce;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Verifies that the {@link Task260320AddPluginsPortletToMenu} upgrade task runs as expected.
 *
 * @author Humberto Morera
 * @since Mar 20th, 2026
 */
public class Task260320AddPluginsPortletToMenuTest {

    /** Matches {@link com.dotmarketing.util.PortletID#PLUGINS} */
    private static final String PLUGINS_PORTLET_ID = "plugins";
    /** Matches {@link com.dotmarketing.util.PortletID#PLUGINS_LEGACY} */
    private static final String PLUGINS_LEGACY_PORTLET_ID = "plugins-legacy";
    /** Matches {@link com.dotmarketing.util.PortletID#DYNAMIC_PLUGINS} */
    private static final String DYNAMIC_PLUGINS_PORTLET_ID = "dynamic-plugins";

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Requires {@code plugins-legacy} or {@code dynamic-plugins} in {@code cms_layouts_portlets},
     * which a normal starter-backed integration DB provides.
     */
    @Before
    public void assumePluginsLegacyMenuExists() {
        final int legacyOrDynamic = new DotConnect()
                .setSQL("SELECT COUNT(*) AS count FROM cms_layouts_portlets WHERE portlet_id = ? OR portlet_id = ?")
                .addParam(PLUGINS_LEGACY_PORTLET_ID)
                .addParam(DYNAMIC_PLUGINS_PORTLET_ID)
                .getInt("count");
        Assume.assumeTrue(
                "Integration DB must include plugins-legacy or dynamic-plugins in a layout for this test",
                legacyOrDynamic > 0);
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link Task260320AddPluginsPortletToMenu#executeUpgrade()}</li>
     *     <li><b>Given scenario:</b> The {@code plugins} portlet is removed from all layouts.</li>
     *     <li><b>Expected result:</b> {@code forceRun()} is true, the task adds the portlet, then
     *     {@code forceRun()} is false.</li>
     * </ul>
     */
    @Test
    public void upgradeTaskExecution() throws Exception {
        deletePluginsPortlet();

        final Task260320AddPluginsPortletToMenu upgradeTask = new Task260320AddPluginsPortletToMenu();

        assertTrue(
                "The 'plugins' portlet was explicitly deleted before, so the UT must run",
                upgradeTask.forceRun());
        upgradeTask.executeUpgrade();
        assertFalse(
                "The 'plugins' portlet has already been added, so the UT must NOT run again",
                upgradeTask.forceRun());
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link Task260320AddPluginsPortletToMenu#executeUpgrade()}</li>
     *     <li><b>Given scenario:</b> The task runs twice in a row.</li>
     *     <li><b>Expected result:</b> No exception; second run is a no-op for inserts.</li>
     * </ul>
     */
    @Test
    public void checkUpgradeTaskIdempotency() throws DotDataException {
        deletePluginsPortlet();
        final Task260320AddPluginsPortletToMenu task = new Task260320AddPluginsPortletToMenu();

        task.executeUpgrade();
        task.executeUpgrade();

        assertFalse("After running twice, forceRun() should return false", task.forceRun());
    }

    private void deletePluginsPortlet() {
        try {
            new DotConnect()
                    .setSQL("DELETE FROM cms_layouts_portlets WHERE portlet_id = ?")
                    .addParam(PLUGINS_PORTLET_ID)
                    .loadResult();
        } catch (final Exception e) {
            Logger.info(this, "Failed deleting the portlet_id " + PLUGINS_PORTLET_ID);
        }
    }
}
