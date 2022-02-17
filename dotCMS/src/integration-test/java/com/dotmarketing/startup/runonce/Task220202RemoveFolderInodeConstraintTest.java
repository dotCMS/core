package com.dotmarketing.startup.runonce;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.common.db.ForeignKey;
import com.dotmarketing.exception.DotDataException;
import java.sql.SQLException;
import java.util.Arrays;
import org.junit.BeforeClass;
import org.junit.Test;

public class Task220202RemoveFolderInodeConstraintTest {

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @Test
    public void testUpgradeTask() throws DotDataException, SQLException {
        final DotDatabaseMetaData databaseMetaData = new DotDatabaseMetaData();

        final ForeignKey foreignKey = databaseMetaData.findForeignKeys("folder", "inode",
                Arrays.asList("inode"), Arrays.asList("inode"));

        //creates foreign key if not exists
        if (null == foreignKey) {
            new DotConnect().executeStatement(
                    "alter table folder add constraint fkb45d1c6e5fb51eb foreign key (inode) references inode;");

            //the foreign key exists before running the upgrade task
            assertNotNull(databaseMetaData.findForeignKeys("folder", "inode",
                    Arrays.asList("inode"), Arrays.asList("inode")));
        }

        final Task220202RemoveFolderInodeConstraint task = new Task220202RemoveFolderInodeConstraint();

        task.executeUpgrade();

        //the foreign key was successfully removed
        assertNull(databaseMetaData.findForeignKeys("folder", "inode",
                Arrays.asList("inode"), Arrays.asList("inode")));

    }

}
