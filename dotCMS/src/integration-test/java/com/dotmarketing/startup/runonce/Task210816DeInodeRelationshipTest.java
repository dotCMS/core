package com.dotmarketing.startup.runonce;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import io.vavr.control.Try;
import java.sql.SQLException;
import java.util.Collections;
import org.junit.BeforeClass;
import org.junit.Test;

public class Task210816DeInodeRelationshipTest {

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @Test
    public void testExecuteUpgrade() throws DotDataException, SQLException {
        //Remove column if exists
        final DotDatabaseMetaData dotDatabaseMetaData = new DotDatabaseMetaData();
        if (dotDatabaseMetaData.hasColumn("relationship", "mod_date")) {
            dotDatabaseMetaData
                    .dropColumn(DbConnectionFactory.getConnection(), "relationship", "mod_date");
        }

        final Task210816DeInodeRelationship upgradeTask = new Task210816DeInodeRelationship();
        assertTrue(upgradeTask.forceRun());
        upgradeTask.executeUpgrade();
        assertFalse(upgradeTask.forceRun()); // mod_date created and FK dropped
        assertTrue(new DotConnect().setSQL("SELECT * FROM inode where type = 'relationship'")
                .loadObjectResults().isEmpty());
        assertTrue(new DotConnect().setSQL("SELECT * FROM inode WHERE EXISTS(SELECT 1 FROM relationship r WHERE r.inode = inode.inode)")
                .loadObjectResults().isEmpty());

    }


}
