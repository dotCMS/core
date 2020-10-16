package com.dotmarketing.startup.runonce;

import static org.junit.Assert.assertTrue;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.exception.DotDataException;
import java.sql.SQLException;
import org.junit.BeforeClass;
import org.junit.Test;

public class Task201016UpdatePrimaryKeyLengthIdentifierTableTest {

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to Test: {@link Task201016UpdatePrimaryKeyLengthIdentifierTable#executeUpgrade()}
     * When: Run the Upgrade Task
     * Should: Update the length of the identifier table's id
     * @throws SQLException
     * @throws DotDataException
     */
    @Test
    public void test_upgradeTask_success() throws DotDataException {
        final Task201016UpdatePrimaryKeyLengthIdentifierTable task = new Task201016UpdatePrimaryKeyLengthIdentifierTable();
        task.executeUpgrade();
        assertTrue(task.forceRun());//Verify the column length
    }
}
