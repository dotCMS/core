package com.dotmarketing.startup.runonce;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration test for {@link Task261103RemoveContainersLegacyPortletFromLayouts}: the legacy
 * Containers portlet is removed from layouts, the Angular one is left alone, and running the task
 * a second time (as installs that already ran it under its old number, 261023, will) is harmless.
 */
public class Task261103RemoveContainersLegacyPortletFromLayoutsTest {

    private static final String LAYOUT_ID = "task261103-layout";
    private static final String LEGACY_ROW = "task261103-legacy-row";
    private static final String ANGULAR_ROW = "task261103-angular-row";

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    @Before
    public void createLayout() throws DotDataException {
        new DotConnect()
                .setSQL("insert into cms_layout (id, layout_name, description, tab_order) values (?, ?, ?, ?)")
                .addParam(LAYOUT_ID)
                .addParam("Task261103 test layout")
                .addParam("Layout used by Task261103RemoveContainersLegacyPortletFromLayoutsTest")
                .addParam(0)
                .loadResult();
    }

    @After
    public void cleanUp() throws DotDataException {
        new DotConnect()
                .setSQL("delete from cms_layouts_portlets where layout_id = ?")
                .addParam(LAYOUT_ID)
                .loadResult();
        new DotConnect()
                .setSQL("delete from cms_layout where id = ?")
                .addParam(LAYOUT_ID)
                .loadResult();
    }

    /**
     * Method to test: {@link Task261103RemoveContainersLegacyPortletFromLayouts#forceRun()}
     * Given Scenario: a layout lists the legacy Containers portlet
     * ExpectedResult: the task needs to run
     */
    @Test
    public void forceRunIsTrueWhenALayoutListsTheLegacyPortlet() throws DotDataException {
        insert(LEGACY_ROW, "containers-legacy");

        assertTrue(new Task261103RemoveContainersLegacyPortletFromLayouts().forceRun());
    }

    /**
     * Method to test: {@link Task261103RemoveContainersLegacyPortletFromLayouts#forceRun()}
     * Given Scenario: no layout lists the legacy Containers portlet, only the Angular one
     * ExpectedResult: the task has nothing to do
     */
    @Test
    public void forceRunIsFalseWhenNoLayoutListsTheLegacyPortlet() throws DotDataException {
        insert(ANGULAR_ROW, "containers");

        assertFalse(new Task261103RemoveContainersLegacyPortletFromLayouts().forceRun());
    }

    /**
     * Method to test: {@link Task261103RemoveContainersLegacyPortletFromLayouts#executeUpgrade()}
     * Given Scenario: a layout lists both the legacy and the Angular Containers portlets
     * ExpectedResult: only the legacy row is deleted, and running the task again does nothing
     */
    @Test
    public void executeUpgradeRemovesOnlyTheLegacyPortlet() throws Exception {
        insert(LEGACY_ROW, "containers-legacy");
        insert(ANGULAR_ROW, "containers");

        final Task261103RemoveContainersLegacyPortletFromLayouts task =
                new Task261103RemoveContainersLegacyPortletFromLayouts();
        task.executeUpgrade();
        task.executeUpgrade();

        assertEquals(0, count("containers-legacy"));
        assertEquals(1, countRow(ANGULAR_ROW));
        assertFalse(task.forceRun());
    }

    /**
     * Method to test: {@link Task261103RemoveContainersLegacyPortletFromLayouts#forceRun()} and
     * {@link Task261103RemoveContainersLegacyPortletFromLayouts#executeUpgrade()}
     * Given Scenario: the task already ran once (it shipped as Task261023 and was renumbered, so
     * installs that applied it under the old number run it again under the new one)
     * ExpectedResult: the second pass reports nothing to do, raises no error, and leaves the
     * layout rows exactly as the first pass did: no legacy row and a single Angular row
     */
    @Test
    public void runningTheTaskASecondTimeIsANoOp() throws Exception {
        insert(LEGACY_ROW, "containers-legacy");
        insert(ANGULAR_ROW, "containers");
        final Task261103RemoveContainersLegacyPortletFromLayouts task =
                new Task261103RemoveContainersLegacyPortletFromLayouts();

        // first pass, as it ran under the old number
        assertTrue(task.forceRun());
        task.executeUpgrade();
        assertEquals(0, count("containers-legacy"));
        assertEquals(1, countRow(ANGULAR_ROW));

        // second pass, under the new number
        assertFalse(task.forceRun());
        task.executeUpgrade();
        assertEquals(0, count("containers-legacy"));
        assertEquals(1, countRow(ANGULAR_ROW));
        assertEquals(1, countLayoutRows());
    }

    private static void insert(final String id, final String portletId) throws DotDataException {
        new DotConnect()
                .setSQL("insert into cms_layouts_portlets (id, layout_id, portlet_id, portlet_order) values (?, ?, ?, ?)")
                .addParam(id)
                .addParam(LAYOUT_ID)
                .addParam(portletId)
                .addParam(1)
                .loadResult();
    }

    private static int count(final String portletId) throws DotDataException {
        return new DotConnect()
                .setSQL("select count(*) as count from cms_layouts_portlets where portlet_id = ?")
                .addParam(portletId)
                .getInt("count");
    }

    private static int countLayoutRows() throws DotDataException {
        return new DotConnect()
                .setSQL("select count(*) as count from cms_layouts_portlets where layout_id = ?")
                .addParam(LAYOUT_ID)
                .getInt("count");
    }

    private static int countRow(final String id) throws DotDataException {
        return new DotConnect()
                .setSQL("select count(*) as count from cms_layouts_portlets where id = ?")
                .addParam(id)
                .getInt("count");
    }
}
