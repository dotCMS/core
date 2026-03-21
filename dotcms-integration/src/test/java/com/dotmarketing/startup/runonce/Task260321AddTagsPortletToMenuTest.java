package com.dotmarketing.startup.runonce;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.dotmarketing.util.PortletID.CATEGORIES;
import static com.dotmarketing.util.PortletID.TAGS;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Verifies that the {@link Task260321AddTagsPortletToMenu} upgrade task runs as expected.
 *
 * @author Humberto Morera
 * @since Mar 21st, 2026
 */
public class Task260321AddTagsPortletToMenuTest {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * The task adds {@code tags} to the Content Types (or categories) layout only when it is absent
     * everywhere; that requires a resolvable target layout in the DB.
     */
    @Before
    public void assumeTargetLayoutExists() {
        final int contentTypes = new DotConnect()
                .setSQL("SELECT COUNT(*) AS count FROM cms_layout WHERE LOWER(layout_name) = 'content types'")
                .getInt("count");
        final int categoriesPortlet = new DotConnect()
                .setSQL("SELECT COUNT(*) AS count FROM cms_layouts_portlets WHERE portlet_id = ?")
                .addParam(CATEGORIES.toString())
                .getInt("count");
        Assume.assumeTrue(
                "Integration DB must have 'Content Types' layout or categories portlet for fallback",
                contentTypes > 0 || categoriesPortlet > 0);
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link Task260321AddTagsPortletToMenu#executeUpgrade()}</li>
     *     <li><b>Given scenario:</b> The {@code tags} portlet is removed from all layouts.</li>
     *     <li><b>Expected result:</b> {@code forceRun()} is true, the task restores/adds {@code tags},
     *     then {@code forceRun()} is false when no migration work remains.</li>
     * </ul>
     */
    @Test
    public void upgradeTaskExecution() throws Exception {
        deleteTagsPortlet();

        final Task260321AddTagsPortletToMenu upgradeTask = new Task260321AddTagsPortletToMenu();

        assertTrue(
                "The 'tags' portlet was explicitly deleted before, so the UT must run",
                upgradeTask.forceRun());
        upgradeTask.executeUpgrade();
        assertFalse(
                "After migration the task should not need to run again for the empty-tags case",
                upgradeTask.forceRun());
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link Task260321AddTagsPortletToMenu#executeUpgrade()}</li>
     *     <li><b>Given scenario:</b> The task runs twice in a row.</li>
     *     <li><b>Expected result:</b> No exception; second run does not duplicate {@code tags}.</li>
     * </ul>
     */
    @Test
    public void checkUpgradeTaskIdempotency() throws DotDataException {
        deleteTagsPortlet();
        final Task260321AddTagsPortletToMenu task = new Task260321AddTagsPortletToMenu();

        task.executeUpgrade();
        task.executeUpgrade();

        assertFalse("After running twice, forceRun() should return false", task.forceRun());
    }

    private void deleteTagsPortlet() {
        try {
            new DotConnect()
                    .setSQL("DELETE FROM cms_layouts_portlets WHERE portlet_id = ?")
                    .addParam(TAGS.toString())
                    .loadResult();
        } catch (final Exception e) {
            Logger.info(this, "Failed deleting the portlet_id " + TAGS.toString());
        }
    }
}
