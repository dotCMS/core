package com.dotmarketing.startup.runonce;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.LocalTransaction;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class Task230922AddPublishDateToContentletVersionInfoTest {

    @BeforeClass
    public static void publishDateColumnExists() throws Exception {
        IntegrationTestInitService.getInstance().init();
        LocalTransaction.wrap(Task230922AddPublishDateToContentletVersionInfoTest::dropPublishDateColumnIfExists);
    }

    private static void dropPublishDateColumnIfExists() {
        try {
            final Connection connection = DbConnectionFactory.getConnection();
            final DotDatabaseMetaData databaseMetaData = new DotDatabaseMetaData();
            final boolean publishColumnExists = databaseMetaData.hasColumn(
                    "contentlet_version_info", "publish_date");
            if (publishColumnExists) {
                databaseMetaData.dropColumn(connection,
                        "contentlet_version_info", "publish_date");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testExecuteUpgrade() throws Exception {

        final Task230922AddPublishDateToContentletVersionInfo task230922AddPublishDateToContentletVersionInfo =
                new Task230922AddPublishDateToContentletVersionInfo();
        final DotDatabaseMetaData databaseMetaData = new DotDatabaseMetaData();

        // check that the column doesn't exist before the upgrade
        final boolean columnExists = databaseMetaData.hasColumn(
                "contentlet_version_info", "publish_date");
        assertFalse(columnExists);

        // execute upgrade and check if the column exists after the upgrade
        assertTrue(task230922AddPublishDateToContentletVersionInfo.forceRun());

        LocalTransaction.wrap(task230922AddPublishDateToContentletVersionInfo::executeUpgrade);

        final boolean columnExistsAfterUpgrade = databaseMetaData.hasColumn(
                "contentlet_version_info", "publish_date");
        assertTrue(columnExistsAfterUpgrade);

    }
}
