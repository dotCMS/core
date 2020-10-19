package com.dotmarketing.startup.runonce;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import java.sql.SQLException;
import org.junit.BeforeClass;
import org.junit.Test;

public class Task201013AddNewColumnsToIdentifierTableTest {

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    private void dropColumns() throws SQLException {
        final DotConnect dotConnect = new DotConnect();
        dotConnect.executeStatement("ALTER TABLE identifier DROP COLUMN owner");
        dotConnect.executeStatement("ALTER TABLE identifier DROP COLUMN create_date");
        dotConnect.executeStatement("ALTER TABLE identifier DROP COLUMN asset_subtype");
    }

    /**
     * Method to Test: {@link Task201013AddNewColumnsToIdentifierTable#executeUpgrade()}
     * When: Run the Upgrade Task
     * Should: Create columns owner, create_date and asset_subtype into the identifier table
     * @throws SQLException
     * @throws DotDataException
     */
    @Test
    public void test_upgradeTask_success() throws SQLException, DotDataException {
        dropColumns();
        final Task201013AddNewColumnsToIdentifierTable task = new Task201013AddNewColumnsToIdentifierTable();
        assertTrue(task.forceRun());//True because the column does not exists
        task.executeUpgrade();
        assertFalse(task.forceRun());//False because the column exists
    }

}
