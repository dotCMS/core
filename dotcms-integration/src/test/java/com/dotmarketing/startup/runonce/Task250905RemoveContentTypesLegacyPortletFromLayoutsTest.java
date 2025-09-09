package com.dotmarketing.startup.runonce;

import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.util.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Integration test for Task250905RemoveContentTypesLegacyPortletFromLayouts
 * Tests that the startup task properly removes legacy content-types portlet from layouts.
 * 
 * @author Neeha Kethi
 */
public class Task250905RemoveContentTypesLegacyPortletFromLayoutsTest {

    @BeforeClass
    public static void prepare() throws Exception {
        // Initialize the H2 DB
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Test the forceRun method - should return true when legacy portlets exist
     */
    @Test
    public void test_forceRun_should_return_true_when_legacy_portlets_exist() throws DotDataException {
        // Setup: Insert a test legacy portlet entry
        new DotConnect()
            .setSQL("INSERT INTO cms_layouts_portlets (id, layout_id, portlet_id, portlet_order) VALUES (?, ?, ?, ?)")
            .addParam("test-legacy-id-1")
            .addParam("test-layout-id") 
            .addParam("content-types")
            .addParam(1)
            .loadResult();

        try {
            // Test: forceRun should return true when legacy portlets exist
            Task250905RemoveContentTypesLegacyPortletFromLayouts task = new Task250905RemoveContentTypesLegacyPortletFromLayouts();
            assertTrue("forceRun should return true when content-types portlets exist in layouts", 
                      task.forceRun());
        } finally {
            // Cleanup: Remove test data
            new DotConnect()
                .setSQL("DELETE FROM cms_layouts_portlets WHERE id = ?")
                .addParam("test-legacy-id-1")
                .loadResult();
        }
    }

    /**
     * Test the forceRun method - should return false when no legacy portlets exist
     */
    @Test
    public void test_forceRun_should_return_false_when_no_legacy_portlets_exist() throws DotDataException {
        // Ensure no legacy portlets exist
        new DotConnect()
            .setSQL("DELETE FROM cms_layouts_portlets WHERE portlet_id = ?")
            .addParam("content-types")
            .loadResult();

        // Test: forceRun should return false when no legacy portlets exist
        Task250905RemoveContentTypesLegacyPortletFromLayouts task = new Task250905RemoveContentTypesLegacyPortletFromLayouts();
        assertFalse("forceRun should return false when no content-types portlets exist in layouts", 
                   task.forceRun());
    }

    /**
     * Test the executeUpgrade method - should remove all legacy portlets
     */
    @Test
    public void test_executeUpgrade_should_remove_all_legacy_portlets() throws Exception {
        // Setup: Insert multiple test legacy portlet entries
        new DotConnect()
            .setSQL("INSERT INTO cms_layouts_portlets (id, layout_id, portlet_id, portlet_order) VALUES (?, ?, ?, ?)")
            .addParam("test-legacy-id-2")
            .addParam("test-layout-id-1") 
            .addParam("content-types")
            .addParam(1)
            .loadResult();

        new DotConnect()
            .setSQL("INSERT INTO cms_layouts_portlets (id, layout_id, portlet_id, portlet_order) VALUES (?, ?, ?, ?)")
            .addParam("test-legacy-id-3")
            .addParam("test-layout-id-2") 
            .addParam("content-types")
            .addParam(2)
            .loadResult();

        try {
            // Verify setup: Should have legacy portlets before execution
            int countBefore = new DotConnect()
                .setSQL("SELECT count(*) as count FROM cms_layouts_portlets WHERE portlet_id = ?")
                .addParam("content-types")
                .getInt("count");
            assertTrue("Should have legacy portlets before executeUpgrade", countBefore >= 2);

            // Test: Execute the upgrade task
            Task250905RemoveContentTypesLegacyPortletFromLayouts task = new Task250905RemoveContentTypesLegacyPortletFromLayouts();
            task.executeUpgrade();

            // Verify: No legacy portlets should exist after execution
            int countAfter = new DotConnect()
                .setSQL("SELECT count(*) as count FROM cms_layouts_portlets WHERE portlet_id = ?")
                .addParam("content-types")
                .getInt("count");
            assertEquals("All content-types portlets should be removed after executeUpgrade", 0, countAfter);

            // Verify: forceRun should return false after execution
            assertFalse("forceRun should return false after executeUpgrade removes all legacy portlets", 
                       task.forceRun());

        } finally {
            // Cleanup: Ensure test data is removed
            new DotConnect()
                .setSQL("DELETE FROM cms_layouts_portlets WHERE id IN (?, ?)")
                .addParam("test-legacy-id-2")
                .addParam("test-legacy-id-3")
                .loadResult();
        }
    }

    /**
     * Test the executeUpgrade method is idempotent - can run multiple times safely
     */
    @Test
    public void test_executeUpgrade_is_idempotent() throws Exception {
        // Setup: Insert a test legacy portlet entry
        new DotConnect()
            .setSQL("INSERT INTO cms_layouts_portlets (id, layout_id, portlet_id, portlet_order) VALUES (?, ?, ?, ?)")
            .addParam("test-legacy-id-4")
            .addParam("test-layout-id-3") 
            .addParam("content-types")
            .addParam(1)
            .loadResult();

        try {
            Task250905RemoveContentTypesLegacyPortletFromLayouts task = new Task250905RemoveContentTypesLegacyPortletFromLayouts();

            // First execution
            task.executeUpgrade();
            int countAfterFirst = new DotConnect()
                .setSQL("SELECT count(*) as count FROM cms_layouts_portlets WHERE portlet_id = ?")
                .addParam("content-types")
                .getInt("count");

            // Second execution (should not cause errors)
            task.executeUpgrade();
            int countAfterSecond = new DotConnect()
                .setSQL("SELECT count(*) as count FROM cms_layouts_portlets WHERE portlet_id = ?")
                .addParam("content-types")
                .getInt("count");

            // Both should result in 0 legacy portlets
            assertEquals("First execution should remove all legacy portlets", 0, countAfterFirst);
            assertEquals("Second execution should maintain 0 legacy portlets", 0, countAfterSecond);
            assertEquals("Multiple executions should be idempotent", countAfterFirst, countAfterSecond);

        } finally {
            // Cleanup: Remove any remaining test data
            new DotConnect()
                .setSQL("DELETE FROM cms_layouts_portlets WHERE id = ?")
                .addParam("test-legacy-id-4")
                .loadResult();
        }
    }

    /**
     * Test that other portlets are not affected by the task
     */
    @Test
    public void test_executeUpgrade_preserves_other_portlets() throws Exception {
        // Setup: Insert test entries for both legacy and modern portlets
        new DotConnect()
            .setSQL("INSERT INTO cms_layouts_portlets (id, layout_id, portlet_id, portlet_order) VALUES (?, ?, ?, ?)")
            .addParam("test-legacy-id-5")
            .addParam("test-layout-id-4") 
            .addParam("content-types")
            .addParam(1)
            .loadResult();

        new DotConnect()
            .setSQL("INSERT INTO cms_layouts_portlets (id, layout_id, portlet_id, portlet_order) VALUES (?, ?, ?, ?)")
            .addParam("test-modern-id-1")
            .addParam("test-layout-id-4") 
            .addParam("content-types-angular")
            .addParam(2)
            .loadResult();

        try {
            // Verify setup
            int legacyCountBefore = new DotConnect()
                .setSQL("SELECT count(*) as count FROM cms_layouts_portlets WHERE portlet_id = ?")
                .addParam("content-types")
                .getInt("count");
            int modernCountBefore = new DotConnect()
                .setSQL("SELECT count(*) as count FROM cms_layouts_portlets WHERE portlet_id = ?")
                .addParam("content-types-angular")
                .getInt("count");

            assertTrue("Should have legacy portlet before execution", legacyCountBefore >= 1);
            assertTrue("Should have modern portlet before execution", modernCountBefore >= 1);

            // Test: Execute the upgrade task
            Task250905RemoveContentTypesLegacyPortletFromLayouts task = new Task250905RemoveContentTypesLegacyPortletFromLayouts();
            task.executeUpgrade();

            // Verify results
            int legacyCountAfter = new DotConnect()
                .setSQL("SELECT count(*) as count FROM cms_layouts_portlets WHERE portlet_id = ?")
                .addParam("content-types")
                .getInt("count");
            int modernCountAfter = new DotConnect()
                .setSQL("SELECT count(*) as count FROM cms_layouts_portlets WHERE portlet_id = ?")
                .addParam("content-types-angular")
                .getInt("count");

            assertEquals("Legacy portlets should be removed", 0, legacyCountAfter);
            assertEquals("Modern portlets should be preserved", modernCountBefore, modernCountAfter);

        } finally {
            // Cleanup
            new DotConnect()
                .setSQL("DELETE FROM cms_layouts_portlets WHERE id IN (?, ?)")
                .addParam("test-legacy-id-5")
                .addParam("test-modern-id-1")
                .loadResult();
        }
    }
}
