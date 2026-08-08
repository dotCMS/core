package com.dotmarketing.startup.runonce;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.exception.DotRuntimeException;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.SQLException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Verifies that {@link Task260615AlterClusterIdLength} widens {@code dot_cluster.cluster_id}
 * to 255 chars and is idempotent.
 */
public class Task260615AlterClusterIdLengthTest {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    private void setLength(final String length) {
        try {
            new DotConnect().executeStatement(
                    "ALTER TABLE dot_cluster ALTER COLUMN cluster_id type varchar (" + length + ")");
        } catch (SQLException e) {
            throw new DotRuntimeException(e);
        }
    }

    @Test
    public void test_executeUpgrade_widens_cluster_id() throws Exception {
        final DotDatabaseMetaData meta = new DotDatabaseMetaData();
        final Task260615AlterClusterIdLength task = new Task260615AlterClusterIdLength();

        // Start narrow
        setLength("36");
        assertTrue("Task should run when column is not yet 255", task.forceRun());

        task.executeUpgrade();
        assertEquals("255", meta.getModifiedColumnLength("dot_cluster", "cluster_id").get("field_length"));

        // Idempotent: already 255 -> no-op
        assertFalse("Task should not re-run once column is 255", task.forceRun());
    }
}
