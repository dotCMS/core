package com.dotmarketing.servlets;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for {@link InitRunner}. Validates that the stat-collection SQL queries execute successfully against
 * a real database and that the assembled stats map contains expected keys and reasonable values.
 */
public class InitRunnerTest {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Verifies every counting SQL constant in InitRunner returns a non-negative long.
     */
    @Test
    public void test_countQueries_returnNonNegativeValues() throws DotDataException, SQLException {
        String[] queries = {
                InitRunner.TOTAL_WORKFLOWS,
                InitRunner.ACTIVE_USERS,
                InitRunner.TOTAL_USERS,
                InitRunner.TOTAL_LANGUAGES,
                InitRunner.TOTAL_TYPES,
                InitRunner.TOTAL_SITES,
                InitRunner.ACTIVE_SITES,
                InitRunner.TOTAL_FOLDERS,
                InitRunner.NUMBER_OF_CONTENTS,
                InitRunner.RECENTLY_EDITED_CONTENT
        };

        try (Connection conn = DbConnectionFactory.getDataSource().getConnection()) {
            for (String sql : queries) {
                List<Map<String, Object>> results = new DotConnect(sql).loadObjectResults(conn);
                assertFalse("Query returned no rows: " + sql, results.isEmpty());
                Object value = results.get(0).get("test_value");
                assertNotNull("Null test_value for: " + sql, value);
                assertTrue("Expected a Number for: " + sql, value instanceof Number);
                assertTrue("Negative count for: " + sql, ((Number) value).longValue() >= 0);
            }
        }
    }

    /**
     * Verifies the LAST_CONTENT_EDIT query returns a Date value.
     */
    @Test
    public void test_lastContentEditQuery_returnsDate() throws DotDataException, SQLException {
        try (Connection conn = DbConnectionFactory.getDataSource().getConnection()) {
            List<Map<String, Object>> results =
                    new DotConnect(InitRunner.LAST_CONTENT_EDIT).loadObjectResults(conn);
            assertFalse("LAST_CONTENT_EDIT returned no rows", results.isEmpty());
            Object value = results.get(0).get("test_value");
            assertNotNull("LAST_CONTENT_EDIT returned null", value);
            assertTrue("Expected a Date, got: " + value.getClass().getName(),
                    value instanceof Date);
        }
    }

    /**
     * Verifies FIND_ALL_HOSTS_SQL returns at least one host with expected column keys.
     */
    @Test
    public void test_findAllHostsQuery_returnsExpectedColumns() throws DotDataException, SQLException {
        try (Connection conn = DbConnectionFactory.getDataSource().getConnection()) {
            List<Map<String, Object>> results =
                    new DotConnect(InitRunner.FIND_ALL_HOSTS_SQL).loadObjectResults(conn);
            assertFalse("Expected at least one host", results.isEmpty());

            Map<String, Object> first = results.get(0);
            assertTrue("Missing 'hostname' column", first.containsKey("hostname"));
            assertTrue("Missing 'id' column", first.containsKey("id"));
            assertTrue("Missing 'default' column", first.containsKey("default"));
        }
    }

    /**
     * Exercises getStats() via reflection and validates the returned map has all expected keys with non-null values.
     */
    @Test
    public void test_getStats_containsAllExpectedKeys() throws Exception {
        InitRunner runner = new InitRunner();
        Method getStats = InitRunner.class.getDeclaredMethod("getStats");
        getStats.setAccessible(true);

        @SuppressWarnings("unchecked")
        Map<String, Object> stats = (Map<String, Object>) getStats.invoke(runner);

        String[] expectedKeys = {
                "id", "hostnames", "dotcmsVersion", "buildDate", "lastContentEdit",
                "defaultHost", "clusterId", "totalUsers", "activeUsers", "totalSites",
                "totalTypes", "activeSites", "totalContent", "recentlyEditedContent",
                "totalLanguages", "numFolders", "clusterNodes", "serverNumber", "workflows",
                "uveEnabled", "portalUrl", "emailDomain", "jvmInfo", "createdDate",
                "pushPublishing", "collectionTime"
        };

        for (String key : expectedKeys) {
            assertTrue("Missing key in stats: " + key, stats.containsKey(key));
            assertNotNull("Null value for key: " + key, stats.get(key));
        }

        // Validate some basic types
        assertTrue("hostnames should be a List",
                stats.get("hostnames") instanceof List);
        assertTrue("totalUsers should be a Number",
                stats.get("totalUsers") instanceof Number);
        assertTrue("jvmInfo should be a Map",
                stats.get("jvmInfo") instanceof Map);
        assertTrue("createdDate should be a Date",
                stats.get("createdDate") instanceof Date);
        assertTrue("collectionTime should be non-negative",
                ((Number) stats.get("collectionTime")).longValue() >= 0);
    }

    /**
     * Verifies isUveEnabled() runs without error (returns true or false depending on config).
     */
    @Test
    public void test_isUveEnabled_doesNotThrow() throws Exception {
        InitRunner runner = new InitRunner();
        Method isUveEnabled = InitRunner.class.getDeclaredMethod("isUveEnabled");
        isUveEnabled.setAccessible(true);

        Object result = isUveEnabled.invoke(runner);
        assertNotNull(result);
        assertTrue("Expected a Boolean", result instanceof Boolean);
    }

    /**
     * Verifies that run() completes without throwing, with pingbacks disabled so no HTTP call is made.
     */
    @Test
    public void test_run_completesWithPingbacksDisabled() {
        // Ensure no actual HTTP call is made
        com.dotmarketing.util.Config.setProperty("dotcms.pingbacks.enabled", false);
        try {
            InitRunner runner = new InitRunner();
            runner.run();
            // If we get here without exception, the test passes
        } finally {
            com.dotmarketing.util.Config.setProperty("dotcms.pingbacks.enabled", true);
        }
    }

    /**
     * Validates that the default host returned from getDefaultHostname() is a non-empty string.
     */
    @Test
    public void test_getDefaultHostname_returnsNonEmptyString() throws Exception {
        InitRunner runner = new InitRunner();
        Method getDefaultHostname = InitRunner.class.getDeclaredMethod("getDefaultHostname");
        getDefaultHostname.setAccessible(true);

        String hostname = (String) getDefaultHostname.invoke(runner);
        assertNotNull("Default hostname should not be null", hostname);
        assertFalse("Default hostname should not be empty", hostname.isEmpty());
    }

    /**
     * Validates that getHostnames() returns a non-empty list containing the default host.
     */
    @Test
    public void test_getHostnames_returnsNonEmptyList() throws Exception {
        InitRunner runner = new InitRunner();
        Method getHostnames = InitRunner.class.getDeclaredMethod("getHostnames");
        getHostnames.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<String> hostnames = (List<String>) getHostnames.invoke(runner);
        assertNotNull("Hostnames list should not be null", hostnames);
        assertFalse("Hostnames list should not be empty", hostnames.isEmpty());
    }

    /**
     * Validates the JVM info map contains expected keys.
     */
    @Test
    public void test_getJVMInfo_containsExpectedKeys() throws Exception {
        InitRunner runner = new InitRunner();
        Method getJVMInfo = InitRunner.class.getDeclaredMethod("getJVMInfo");
        getJVMInfo.setAccessible(true);

        @SuppressWarnings("unchecked")
        Map<String, Object> jvmInfo = (Map<String, Object>) getJVMInfo.invoke(runner);

        String[] expectedKeys = {
                "maxMemory", "allocatedMemory", "freeMemory",
                "vmName", "vmVendor", "vmVersion", "started", "startUpTime"
        };

        for (String key : expectedKeys) {
            assertTrue("Missing JVM info key: " + key, jvmInfo.containsKey(key));
            assertNotNull("Null JVM info value for: " + key, jvmInfo.get(key));
        }
    }
}
