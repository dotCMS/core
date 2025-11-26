package com.dotmarketing.startup.runonce;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.UUIDUtil;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class Task241015ReplaceLanguagesWithLocalesPortletTest {

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Given the locales portlets are inserted
     * When the upgrade task is executed
     * Then the locales portlets should be added to the layout
     * And the task should not need to be forced to run
     *
     * @throws SQLException if there is an SQL error
     * @throws DotDataException if there is a data access error
     */
    @Test
    public void test_upgradeTask_success() throws SQLException, DotDataException {
        final DotConnect dotConnect = new DotConnect();
        final Task241015ReplaceLanguagesWithLocalesPortlet task = new Task241015ReplaceLanguagesWithLocalesPortlet();

        insertLocalesPortlets(dotConnect);
        assertFalse(task.forceRun());

        deleteAnyLocalesPortlets(dotConnect);
        assertTrue(task.forceRun());

        task.executeUpgrade();
        assertFalse(task.forceRun());
    }

    /**
     * Scenario: Upgrade task execution when no languages portlets are present
     *
     * Given the locales portlets are not present
     * When the upgrade task is executed
     * Then the locales portlets should be added to the layout
     * And the task should not need to be forced to run
     *
     * @throws SQLException if there is an SQL error
     * @throws DotDataException if there is a data access error
     */
    @Test
    public void test_upgradeTask_noLanguages_success() throws SQLException, DotDataException {
        final DotConnect dotConnect = new DotConnect();
        final Task241015ReplaceLanguagesWithLocalesPortlet task = new Task241015ReplaceLanguagesWithLocalesPortlet();

        assertFalse(task.forceRun());

        deleteAnyLanguagesPortlets(dotConnect);
        deleteAnyLocalesPortlets(dotConnect);
        assertTrue(task.forceRun());

        task.executeUpgrade();
        assertFalse(task.forceRun());
    }

    private void insertLocalesPortlets(final DotConnect dotConnect) throws DotDataException {
        final List<Map<String, Object>> rows = dotConnect
                .setSQL("SELECT layout_id, portlet_order" +
                        " FROM cms_layouts_portlets" +
                        " WHERE portlet_id = ?" +
                        " ORDER BY portlet_order" +
                        " DESC LIMIT 1")
                .addParam(Task241015ReplaceLanguagesWithLocalesPortlet.LANGUAGES_PORTLET_ID)
                .loadObjectResults();
        if (rows.isEmpty()) {
            return;
        }

        final Map<String, Object> row = rows.get(0);
        dotConnect
                .setSQL("INSERT INTO cms_layouts_portlets(id, layout_id, portlet_id, portlet_order)" +
                        " VALUES(?, ?, ?, ?)")
                .addParam(UUIDUtil.uuid())
                .addParam(row.get("layout_id"))
                .addParam(Task241015ReplaceLanguagesWithLocalesPortlet.LOCALES_PORTLET_ID)
                .addParam(((Integer) row.get("portlet_order")) + 1)
                .loadResult();
    }

    private void deletePortlets(final DotConnect dotConnect, final String portletId) throws SQLException {
        dotConnect
                .executeStatement(String.format(
                        "DELETE FROM cms_layouts_portlets WHERE portlet_id = '%s'",
                        portletId));
    }

    private void deleteAnyLocalesPortlets(final DotConnect dotConnect) throws SQLException {
        deletePortlets(dotConnect, Task241015ReplaceLanguagesWithLocalesPortlet.LOCALES_PORTLET_ID);
    }

    private void deleteAnyLanguagesPortlets(final DotConnect dotConnect) throws SQLException {
        deletePortlets(dotConnect, Task241015ReplaceLanguagesWithLocalesPortlet.LANGUAGES_PORTLET_ID);
    }

}
