package com.dotmarketing.startup.runonce;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDUtil;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Integration test for Task33063RemoveContentTypesLegacyPortletFromLayouts
 * Verifies that the legacy content-types portlet is properly removed from all layouts
 * while leaving the new content-types-angular portlet intact.
 * 
 * @author jsanca
 */
public class Task33063RemoveContentTypesLegacyPortletFromLayoutsTest {

    private static final String LEGACY_PORTLET_ID = "content-types";
    private static final String NEW_PORTLET_ID = "content-types-angular";

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to test: executeUpgrade
     * Given Scenario: Testing when legacy content-types portlets exist in layouts
     * ExpectedResult: After running the task, legacy portlets should be removed but new ones remain
     * 
     * @throws DotDataException
     * @throws SQLException
     */
    @Test
    public void test_upgradeTask_removesLegacyPortletsOnly() throws DotDataException, SQLException {
        final DotConnect dotConnect = new DotConnect();
        final Task33063RemoveContentTypesLegacyPortletFromLayouts task = 
            new Task33063RemoveContentTypesLegacyPortletFromLayouts();

        try {
            // Setup: Insert test legacy portlets
            insertTestLegacyPortlets(dotConnect);
            
            // Setup: Insert test new portlets (should remain untouched)
            insertTestNewPortlets(dotConnect);

            // Verify legacy portlets exist before cleanup
            assertTrue("Legacy portlets should exist before cleanup", hasLegacyPortlets(dotConnect));
            assertTrue("New portlets should exist before cleanup", hasNewPortlets(dotConnect));
            assertTrue("Task should need to run when legacy portlets exist", task.forceRun());

            // Execute the upgrade task
            task.executeUpgrade();

            // Verify results
            assertFalse("Legacy portlets should be removed after cleanup", hasLegacyPortlets(dotConnect));
            assertTrue("New portlets should remain after cleanup", hasNewPortlets(dotConnect));
            assertFalse("Task should not need to run after legacy portlets are removed", task.forceRun());

        } finally {
            // Cleanup: Remove any test data
            cleanupTestPortlets(dotConnect);
        }
    }

    /**
     * Method to test: forceRun when no legacy portlets exist
     * Given Scenario: No legacy content-types portlets in any layout
     * ExpectedResult: Task should not need to run
     * 
     * @throws DotDataException
     * @throws SQLException
     */
    @Test
    public void test_forceRun_returnsFalseWhenNoLegacyPortlets() throws DotDataException, SQLException {
        final DotConnect dotConnect = new DotConnect();
        final Task33063RemoveContentTypesLegacyPortletFromLayouts task = 
            new Task33063RemoveContentTypesLegacyPortletFromLayouts();

        try {
            // Ensure no legacy portlets exist
            removeLegacyPortlets(dotConnect);
            
            // Verify task doesn't need to run
            assertFalse("Task should not run when no legacy portlets exist", task.forceRun());

        } finally {
            cleanupTestPortlets(dotConnect);
        }
    }

    /**
     * Method to test: Multiple executions are safe (idempotent)
     * Given Scenario: Running the task multiple times
     * ExpectedResult: Should be safe and not cause errors
     * 
     * @throws DotDataException
     * @throws SQLException
     */
    @Test
    public void test_upgradeTask_idempotent() throws DotDataException, SQLException {
        final DotConnect dotConnect = new DotConnect();
        final Task33063RemoveContentTypesLegacyPortletFromLayouts task = 
            new Task33063RemoveContentTypesLegacyPortletFromLayouts();

        try {
            // Setup: Insert test legacy portlets
            insertTestLegacyPortlets(dotConnect);
            
            // First execution
            assertTrue("Task should need to run initially", task.forceRun());
            task.executeUpgrade();
            assertFalse("Task should not need to run after first execution", task.forceRun());
            
            // Second execution (should be safe)
            task.executeUpgrade();
            assertFalse("Task should still not need to run after second execution", task.forceRun());

        } finally {
            cleanupTestPortlets(dotConnect);
        }
    }

    // Helper methods

    private void insertTestLegacyPortlets(final DotConnect dotConnect) throws DotDataException {
        // Get existing layouts to add test portlets to
        dotConnect.setSQL("SELECT id FROM cms_layout LIMIT 2");
        List<Map<String, Object>> layouts = dotConnect.loadObjectResults();
        
        for (Map<String, Object> layout : layouts) {
            String layoutId = (String) layout.get("id");
            
            // Insert legacy portlet (will be removed by our task)
            dotConnect.setSQL("INSERT INTO cms_layouts_portlets(id, layout_id, portlet_id, portlet_order) VALUES(?, ?, ?, ?)")
                .addParam(UUIDUtil.uuid())
                .addParam(layoutId)
                .addParam(LEGACY_PORTLET_ID)
                .addParam(99) // High order number to avoid conflicts
                .loadResult();
        }
    }

    private void insertTestNewPortlets(final DotConnect dotConnect) throws DotDataException {
        // Get existing layouts to add test portlets to
        dotConnect.setSQL("SELECT id FROM cms_layout LIMIT 1");
        List<Map<String, Object>> layouts = dotConnect.loadObjectResults();
        
        if (!layouts.isEmpty()) {
            String layoutId = (String) layouts.get(0).get("id");
            
            // Insert new portlet (should remain untouched)
            dotConnect.setSQL("INSERT INTO cms_layouts_portlets(id, layout_id, portlet_id, portlet_order) VALUES(?, ?, ?, ?)")
                .addParam(UUIDUtil.uuid())
                .addParam(layoutId)
                .addParam(NEW_PORTLET_ID)
                .addParam(98) // High order number to avoid conflicts
                .loadResult();
        }
    }

    private boolean hasLegacyPortlets(final DotConnect dotConnect) throws DotDataException {
        dotConnect.setSQL("SELECT COUNT(*) as count FROM cms_layouts_portlets WHERE portlet_id = ?")
            .addParam(LEGACY_PORTLET_ID);
        return dotConnect.getInt("count") > 0;
    }

    private boolean hasNewPortlets(final DotConnect dotConnect) throws DotDataException {
        dotConnect.setSQL("SELECT COUNT(*) as count FROM cms_layouts_portlets WHERE portlet_id = ?")
            .addParam(NEW_PORTLET_ID);
        return dotConnect.getInt("count") > 0;
    }

    private void removeLegacyPortlets(final DotConnect dotConnect) throws SQLException {
        dotConnect.executeStatement("DELETE FROM cms_layouts_portlets WHERE portlet_id = '" + LEGACY_PORTLET_ID + "'");
    }

    private void cleanupTestPortlets(final DotConnect dotConnect) {
        try {
            // Remove test data - use high portlet_order to target our test data
            dotConnect.executeStatement("DELETE FROM cms_layouts_portlets WHERE portlet_id = '" + LEGACY_PORTLET_ID + "' AND portlet_order >= 98");
            dotConnect.executeStatement("DELETE FROM cms_layouts_portlets WHERE portlet_id = '" + NEW_PORTLET_ID + "' AND portlet_order >= 98");
        } catch (Exception e) {
            Logger.info(this.getClass(), "Failed cleaning up test portlets: " + e.getMessage());
        }
    }
}
