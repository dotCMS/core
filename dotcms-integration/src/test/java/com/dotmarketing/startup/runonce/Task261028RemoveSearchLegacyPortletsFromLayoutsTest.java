package com.dotmarketing.startup.runonce;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.UUIDUtil;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Verifies that {@link Task261028RemoveSearchLegacyPortletsFromLayouts} takes the legacy ES Search
 * and legacy Query Tool portlets out of the layouts and leaves every other portlet alone.
 *
 * <p>{@code cms_layouts_portlets.layout_id} is a foreign key to {@code cms_layout}, so each test
 * hangs its rows on a layout that already exists in the integration database.
 */
public class Task261028RemoveSearchLegacyPortletsFromLayoutsTest {

    private static final String ES_SEARCH_LEGACY = "es-search-legacy";
    private static final String QUERY_TOOL_LEGACY = "query-tool-legacy";

    private String layoutId;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Starts every test with no legacy entry in any layout, and picks the layout that will host
     * the entries the test adds.
     */
    @Before
    public void setUp() throws DotDataException {
        deleteLegacyPortlets();

        layoutId = new DotConnect()
                .setSQL("SELECT id FROM cms_layout ORDER BY id")
                .getString("id");
        Assume.assumeTrue("The integration DB must have at least one layout",
                layoutId != null && !layoutId.isEmpty());
    }

    /** Removes the entries a test added, so no other test sees them. */
    @After
    public void tearDown() throws DotDataException {
        deleteLegacyPortlets();
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link Task261028RemoveSearchLegacyPortletsFromLayouts#forceRun()}</li>
     *     <li><b>Given scenario:</b> No layout lists a legacy portlet.</li>
     *     <li><b>Expected result:</b> The task does not run.</li>
     * </ul>
     */
    @Test
    public void forceRunIsFalseWhenNoLayoutListsALegacyPortlet() {
        assertFalse("There is nothing to remove, so the task must not run",
                new Task261028RemoveSearchLegacyPortletsFromLayouts().forceRun());
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link Task261028RemoveSearchLegacyPortletsFromLayouts#executeUpgrade()}</li>
     *     <li><b>Given scenario:</b> A layout lists the legacy ES Search portlet and another lists
     *     the legacy Query Tool portlet, as an admin can do by hand.</li>
     *     <li><b>Expected result:</b> {@code forceRun()} is true, the task removes both entries,
     *     then {@code forceRun()} is false.</li>
     * </ul>
     */
    @Test
    public void upgradeTaskRemovesBothLegacyPortlets() throws DotDataException {
        addToLayout(ES_SEARCH_LEGACY);
        addToLayout(QUERY_TOOL_LEGACY);

        final Task261028RemoveSearchLegacyPortletsFromLayouts task =
                new Task261028RemoveSearchLegacyPortletsFromLayouts();

        assertTrue("A legacy portlet is in a layout, so the task must run", task.forceRun());
        task.executeUpgrade();

        assertEquals("The legacy ES Search portlet must be gone", 0, countPortlet(ES_SEARCH_LEGACY));
        assertEquals("The legacy Query Tool portlet must be gone", 0, countPortlet(QUERY_TOOL_LEGACY));
        assertFalse("Everything was removed, so the task must not run again", task.forceRun());
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link Task261028RemoveSearchLegacyPortletsFromLayouts#executeUpgrade()}</li>
     *     <li><b>Given scenario:</b> Layouts hold the legacy portlets next to the portlets that
     *     replaced them and the rest of the menu.</li>
     *     <li><b>Expected result:</b> Only the legacy entries are deleted; every other entry is
     *     still there.</li>
     * </ul>
     */
    @Test
    public void upgradeTaskKeepsEveryOtherPortlet() throws DotDataException {
        addToLayout(ES_SEARCH_LEGACY);
        addToLayout(QUERY_TOOL_LEGACY);
        final int othersBefore = countOtherPortlets();
        assertTrue("The layouts must hold other portlets for this test to prove anything", othersBefore > 0);

        new Task261028RemoveSearchLegacyPortletsFromLayouts().executeUpgrade();

        assertEquals("Only the legacy portlets may be removed", othersBefore, countOtherPortlets());
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link Task261028RemoveSearchLegacyPortletsFromLayouts#executeUpgrade()}</li>
     *     <li><b>Given scenario:</b> The task already ran once.</li>
     *     <li><b>Expected result:</b> Running it again does not fail and changes nothing.</li>
     * </ul>
     */
    @Test
    public void upgradeTaskCanRunTwice() throws DotDataException {
        addToLayout(ES_SEARCH_LEGACY);
        final Task261028RemoveSearchLegacyPortletsFromLayouts task =
                new Task261028RemoveSearchLegacyPortletsFromLayouts();

        task.executeUpgrade();
        final int othersAfterFirstRun = countOtherPortlets();
        task.executeUpgrade();

        assertEquals(0, countPortlet(ES_SEARCH_LEGACY));
        assertEquals(othersAfterFirstRun, countOtherPortlets());
    }

    private void addToLayout(final String portletId) throws DotDataException {
        final int nextOrder = new DotConnect()
                .setSQL("SELECT MAX(portlet_order) AS portlet_order FROM cms_layouts_portlets WHERE layout_id = ?")
                .addParam(layoutId)
                .getInt("portlet_order") + 1;

        new DotConnect()
                .setSQL("INSERT INTO cms_layouts_portlets(id, layout_id, portlet_id, portlet_order) VALUES (?, ?, ?, ?)")
                .addParam(UUIDUtil.uuid())
                .addParam(layoutId)
                .addParam(portletId)
                .addParam(nextOrder)
                .loadResult();
    }

    private int countPortlet(final String portletId) throws DotDataException {
        return new DotConnect()
                .setSQL("SELECT COUNT(portlet_id) AS count FROM cms_layouts_portlets WHERE portlet_id = ?")
                .addParam(portletId)
                .getInt("count");
    }

    private int countOtherPortlets() throws DotDataException {
        return new DotConnect()
                .setSQL("SELECT COUNT(portlet_id) AS count FROM cms_layouts_portlets WHERE portlet_id NOT IN (?, ?)")
                .addParam(ES_SEARCH_LEGACY)
                .addParam(QUERY_TOOL_LEGACY)
                .getInt("count");
    }

    private void deleteLegacyPortlets() throws DotDataException {
        new DotConnect()
                .setSQL("DELETE FROM cms_layouts_portlets WHERE portlet_id IN (?, ?)")
                .addParam(ES_SEARCH_LEGACY)
                .addParam(QUERY_TOOL_LEGACY)
                .loadResult();
    }
}
