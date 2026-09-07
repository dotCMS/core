package com.dotmarketing.startup.runonce;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDUtil;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Verifies that the {@link Task260505AddPluginsPortletToMenu} recovery task runs as expected.
 *
 * <p>Unlike its companion {@link Task260320AddPluginsPortletToMenuTest}, this class cannot rely on
 * the integration starter seeding the legacy {@code dynamic-plugins} row — current starters
 * ({@code empty_20260331} and later) ship the corrected state with {@code plugins} already in the
 * layout. To reproduce the broken-window scenario the recovery task is built for, each test sets
 * up a {@code dynamic-plugins} anchor in the {@code system} layout and then deletes any
 * {@code plugins} row, putting the DB in the same shape an affected customer's instance is in.
 *
 * @author hassandotcms
 * @since May 5th, 2026
 */
public class Task260505AddPluginsPortletToMenuTest {

    /** Matches {@link com.dotmarketing.util.PortletID#PLUGINS} */
    private static final String PLUGINS_PORTLET_ID = "plugins";
    /** Matches {@link com.dotmarketing.util.PortletID#DYNAMIC_PLUGINS} */
    private static final String DYNAMIC_PLUGINS_PORTLET_ID = "dynamic-plugins";

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Reproduces the broken-window install state: a {@code dynamic-plugins} row in the {@code system}
     * layout, no {@code plugins} row. Idempotent — if {@code dynamic-plugins} is already there
     * (e.g. from a previous test or an older starter) we leave it alone.
     */
    @Before
    public void setUpBrokenWindowState() throws DotDataException {
        deletePluginsPortlet();

        final int existingDynamic = new DotConnect()
                .setSQL("SELECT COUNT(*) AS count FROM cms_layouts_portlets WHERE portlet_id = ?")
                .addParam(DYNAMIC_PLUGINS_PORTLET_ID)
                .getInt("count");
        if (existingDynamic > 0) {
            return;
        }

        final String systemLayoutId = new DotConnect()
                .setSQL("SELECT id FROM cms_layout WHERE LOWER(layout_name) = 'system'")
                .getString("id");
        Assume.assumeTrue(
                "Integration DB must have a 'system' layout to host the dynamic-plugins anchor",
                systemLayoutId != null && !systemLayoutId.isEmpty());

        new DotConnect()
                .setSQL("INSERT INTO cms_layouts_portlets(id, layout_id, portlet_id, portlet_order) " +
                        "VALUES (?, ?, ?, ?)")
                .addParam(UUIDUtil.uuid())
                .addParam(systemLayoutId)
                .addParam(DYNAMIC_PLUGINS_PORTLET_ID)
                .addParam(99)
                .loadResult();
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link Task260505AddPluginsPortletToMenu#executeUpgrade()}</li>
     *     <li><b>Given scenario:</b> The {@code plugins} portlet is missing while
     *     {@code dynamic-plugins} remains as the legacy anchor (the broken-window state).</li>
     *     <li><b>Expected result:</b> {@code forceRun()} is true, the task adds the portlet, then
     *     {@code forceRun()} is false.</li>
     * </ul>
     */
    @Test
    public void upgradeTaskExecution() throws Exception {
        final Task260505AddPluginsPortletToMenu upgradeTask = new Task260505AddPluginsPortletToMenu();

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
     *     <li><b>Method to test:</b> {@link Task260505AddPluginsPortletToMenu#executeUpgrade()}</li>
     *     <li><b>Given scenario:</b> The task runs successfully.</li>
     *     <li><b>Expected result:</b> {@code dynamic-plugins} remains untouched in
     *     {@code cms_layouts_portlets} so that a version rollback finds it and the old
     *     {@code portlet.xml} renders the JSP portlet without any DB repair.</li>
     * </ul>
     */
    @Test
    public void dynamicPluginsIsPreservedForRollbackSafety() throws DotDataException {
        new Task260505AddPluginsPortletToMenu().executeUpgrade();

        final int count = new DotConnect()
                .setSQL("SELECT COUNT(*) AS count FROM cms_layouts_portlets WHERE portlet_id = ?")
                .addParam(DYNAMIC_PLUGINS_PORTLET_ID)
                .getInt("count");
        assertTrue(
                "'dynamic-plugins' must remain in the layout after the upgrade so a rollback restores the JSP portlet",
                count > 0);
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link Task260505AddPluginsPortletToMenu#forceRun()}</li>
     *     <li><b>Given scenario:</b> Both {@code dynamic-plugins} and {@code plugins} are present
     *     in the layout (the steady state after a successful upgrade).</li>
     *     <li><b>Expected result:</b> {@code forceRun()} returns {@code false} — the presence of
     *     {@code dynamic-plugins} alone must not trigger a re-run.</li>
     * </ul>
     */
    @Test
    public void forceRunReturnsFalseWhenBothPortletsCoexist() throws DotDataException {
        new Task260505AddPluginsPortletToMenu().executeUpgrade();

        assertFalse(
                "forceRun() must return false when both portlets are present — " +
                        "dynamic-plugins presence alone must not trigger a re-run",
                new Task260505AddPluginsPortletToMenu().forceRun());
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link Task260505AddPluginsPortletToMenu#executeUpgrade()}</li>
     *     <li><b>Given scenario:</b> The task runs twice in a row.</li>
     *     <li><b>Expected result:</b> No exception; second run is a no-op for inserts.</li>
     * </ul>
     */
    @Test
    public void checkUpgradeTaskIdempotency() throws DotDataException {
        final Task260505AddPluginsPortletToMenu task = new Task260505AddPluginsPortletToMenu();

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
