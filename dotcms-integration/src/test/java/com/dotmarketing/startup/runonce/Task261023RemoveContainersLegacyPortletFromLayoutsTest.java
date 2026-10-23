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
 * Integration test for {@link Task261023RemoveContainersLegacyPortletFromLayouts}: the legacy
 * Containers portlet is removed from layouts and the Angular one is left alone.
 */
public class Task261023RemoveContainersLegacyPortletFromLayoutsTest {

    private static final String LAYOUT_ID = "task261023-layout";
    private static final String LEGACY_ROW = "task261023-legacy-row";
    private static final String ANGULAR_ROW = "task261023-angular-row";

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    @Before
    public void createLayout() throws DotDataException {
        new DotConnect()
                .setSQL("insert into cms_layout (id, layout_name, description, tab_order) values (?, ?, ?, ?)")
                .addParam(LAYOUT_ID)
                .addParam("Task261023 test layout")
                .addParam("Layout used by Task261023RemoveContainersLegacyPortletFromLayoutsTest")
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
     * Method to test: {@link Task261023RemoveContainersLegacyPortletFromLayouts#forceRun()}
     * Given Scenario: a layout lists the legacy Containers portlet
     * ExpectedResult: the task needs to run
     */
    @Test
    public void forceRunIsTrueWhenALayoutListsTheLegacyPortlet() throws DotDataException {
        insert(LEGACY_ROW, "containers-legacy");

        assertTrue(new Task261023RemoveContainersLegacyPortletFromLayouts().forceRun());
    }

    /**
     * Method to test: {@link Task261023RemoveContainersLegacyPortletFromLayouts#forceRun()}
     * Given Scenario: no layout lists the legacy Containers portlet, only the Angular one
     * ExpectedResult: the task has nothing to do
     */
    @Test
    public void forceRunIsFalseWhenNoLayoutListsTheLegacyPortlet() throws DotDataException {
        insert(ANGULAR_ROW, "containers");

        assertFalse(new Task261023RemoveContainersLegacyPortletFromLayouts().forceRun());
    }

    /**
     * Method to test: {@link Task261023RemoveContainersLegacyPortletFromLayouts#executeUpgrade()}
     * Given Scenario: a layout lists both the legacy and the Angular Containers portlets
     * ExpectedResult: only the legacy row is deleted, and running the task again does nothing
     */
    @Test
    public void executeUpgradeRemovesOnlyTheLegacyPortlet() throws Exception {
        insert(LEGACY_ROW, "containers-legacy");
        insert(ANGULAR_ROW, "containers");

        final Task261023RemoveContainersLegacyPortletFromLayouts task =
                new Task261023RemoveContainersLegacyPortletFromLayouts();
        task.executeUpgrade();
        task.executeUpgrade();

        assertEquals(0, count("containers-legacy"));
        assertEquals(1, countRow(ANGULAR_ROW));
        assertFalse(task.forceRun());
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

    private static int countRow(final String id) throws DotDataException {
        return new DotConnect()
                .setSQL("select count(*) as count from cms_layouts_portlets where id = ?")
                .addParam(id)
                .getInt("count");
    }
}
