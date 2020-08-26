package com.dotmarketing.startup.runonce;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class Task05350AddDotSaltClusterColumnTest {

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    private void dropColumn(final DotConnect dotConnect) {
        try {
            final String dropColumnSQL = "ALTER TABLE dot_cluster DROP COLUMN cluster_salt";
            dotConnect.executeStatement(dropColumnSQL);
        } catch (Exception e) {
            Logger.info(Task05350AddDotSaltClusterColumnTest.class, () -> "Failed removing cluster_salt column. Maybe it didn't exist?");
        }
    }

    @Test
    public void test_upgradeTask_success() throws DotDataException {
        final DotConnect dotConnect = new DotConnect();
        dropColumn(dotConnect);
        final Task05350AddDotSaltClusterColumn task = new Task05350AddDotSaltClusterColumn();
        assertTrue(task.forceRun());//True because the column does not exists
        task.executeUpgrade();
        assertFalse(task.forceRun());//False because the column exists

        dotConnect.setSQL("SELECT cluster_id, cluster_salt FROM dot_cluster ");
        final List<Map<String, Object>> maps = dotConnect.loadObjectResults();
        assertEquals("There can only be 1.", 1, maps.size());
        final Map<String, Object> map = maps.get(0);
        Assert.assertNotNull(map.get("cluster_id"));
        Assert.assertNotNull(map.get("cluster_salt"));
    }

}
