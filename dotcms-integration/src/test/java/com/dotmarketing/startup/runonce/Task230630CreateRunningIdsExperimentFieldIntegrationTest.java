package com.dotmarketing.startup.runonce;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test of {@link Task230630CreateRunningIdsExperimentField}
 */
public class Task230630CreateRunningIdsExperimentFieldIntegrationTest {

    @BeforeClass
    public static  void runningIdsExists() throws Exception {

        // Setting web app environment
        IntegrationTestInitService.getInstance().init();

        assertTrue("The running_ids Fields must exists", isExistsRunningIds());
    }

    private static boolean isExistsRunningIds() throws SQLException {
        final Connection connection = DbConnectionFactory.getConnection();

        boolean existsRunningIds = false;
        final ResultSet resultSet = DotDatabaseMetaData.getColumnsMetaData(connection, "experiment");
        while(resultSet.next()){
            final String columnName = resultSet.getString("COLUMN_NAME");
            final String columnType = resultSet.getString(6);

            if (columnName.equals("running_ids")) {
                assertEquals(columnType, "jsonb");
                existsRunningIds = true;
            }
        }
        return existsRunningIds;
    }

    /**
     * Method to test: {@link Task230630CreateRunningIdsExperimentField#executeUpgrade()} and {@link Task230630CreateRunningIdsExperimentField#forceRun()}
     * When: The field does not exists
     * Should: Return false on the forceRun method and create the field on the executeUpgrade method
     */
    @Test
    public void mustCreateRunningIdsField() throws SQLException, DotDataException {
        cleanUp();
        assertFalse("The running_ids Fields must not exists", isExistsRunningIds());

        final Task230630CreateRunningIdsExperimentField task230630CreateRunningIdsExperimentField =
                new Task230630CreateRunningIdsExperimentField();

        assertTrue("The forceRun method must return true", task230630CreateRunningIdsExperimentField.forceRun());

        task230630CreateRunningIdsExperimentField.executeUpgrade();

        assertTrue("The running_ids Fields must exists", isExistsRunningIds());
        assertFalse("The forceRun method must return false", task230630CreateRunningIdsExperimentField.forceRun());

    }

    private static void cleanUp () throws SQLException {

        try {
            new DotConnect().executeStatement(
                    "ALTER TABLE experiment DROP COLUMN running_ids");
        } catch (SQLException e) {
            //ignore
        }
    }
}
