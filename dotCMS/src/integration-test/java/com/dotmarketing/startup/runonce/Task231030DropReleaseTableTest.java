package com.dotmarketing.startup.runonce;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import java.sql.SQLException;
import org.junit.BeforeClass;
import org.junit.Test;

public class Task231030DropReleaseTableTest {

    private static final String RELEASE_TABLE_CREATE_SCRIPT = "CREATE TABLE IF NOT EXISTS Release_ (\n"
            + "   releaseId varchar(100) not null primary key,\n"
            + "   createDate timestamptz null,\n"
            + "   modifiedDate timestamptz null,\n"
            + "   buildNumber integer null,\n"
            + "   buildDate timestamptz null\n"
            + ");";


    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
        createReleaseTableIfNotExists();
    }

    private static void createReleaseTableIfNotExists() throws SQLException {
        new DotConnect().executeStatement(RELEASE_TABLE_CREATE_SCRIPT);
    }

    /**
     * Method to Test: {@link Task231030DropReleaseTable#executeUpgrade()}
     * When: Run the Upgrade Task
     * Should: Drop Release_ table
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void testUpgradeTask() throws DotDataException {

        final Task231030DropReleaseTable task = new Task231030DropReleaseTable();

        assertTrue(task.forceRun());
        task.executeUpgrade();
        assertFalse(task.forceRun());
    }

}
