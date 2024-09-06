package com.dotmarketing.startup.runonce;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.datagen.WorkflowDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.AlreadyExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import java.sql.SQLException;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test class for {@link Task240606AddVariableColumnToWorkflow}.
 * <p>
 * This test class contains test cases to validate the addition of the variable column to the
 * workflow_scheme table and to handle scenarios where workflows with duplicate names exist.
 */
public class Task240606AddVariableColumnToWorkflowTest {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to test: {@link Task240606AddVariableColumnToWorkflow#executeUpgrade()}
     * <p>
     * Given Scenario: The column does not exist in the workflow_scheme table
     * <p>
     * ExpectedResult: The column variable_name is added and populated correctly
     *
     * @throws DotDataException if a data access error occurs.
     */
    @Test
    public void test_upgradeTask_success() throws DotDataException {

        // Make sure the column does not exist
        dropVariableColumn();

        final var upgradeTask = new Task240606AddVariableColumnToWorkflow();
        upgradeTask.executeUpgrade();

        // Make sure the column was added and populated
        validateColumnData();
    }

    /**
     * Method to test: {@link Task240606AddVariableColumnToWorkflow#executeUpgrade()}
     * <p>
     * Given Scenario: Multiple workflows with the same name exist
     * <p>
     * ExpectedResult: The variable names are generated correctly as testScheme, testScheme1,
     * testScheme2, and testScheme3
     *
     * @throws DotDataException      if a data access error occurs.
     * @throws DotSecurityException  if a security error occurs.
     * @throws AlreadyExistException if an existing entity conflicts with the operation.
     */
    @Test
    public void test_validate_duplicated_names()
            throws DotDataException, DotSecurityException, AlreadyExistException {

        // Creating test workflows with the same name
        final var workflow1 = new WorkflowDataGen().name("testScheme").nextPersisted();
        final var workflow2 = new WorkflowDataGen().name("testScheme").nextPersisted();
        final var workflow3 = new WorkflowDataGen().name("testScheme").nextPersisted();
        final var workflow4 = new WorkflowDataGen().name("testScheme").nextPersisted();

        try {
            // Make sure the column does not exist
            dropVariableColumn();

            final var upgradeTask = new Task240606AddVariableColumnToWorkflow();
            upgradeTask.executeUpgrade();

            // Make sure the column was added and populated
            validateColumnData();

            // Validate we created properly the variable_name
            var expectedVariableNames = new String[]{
                    "TestScheme",
                    "TestScheme1",
                    "TestScheme2",
                    "TestScheme3"
            };
            for (String variableName : expectedVariableNames) {
                validateVariableName(variableName);
            }

        } finally {
            cleanUp(workflow1, workflow2, workflow3, workflow4);
        }
    }

    /**
     * Drops the variable_name column from the workflow_scheme table.
     * <p>
     * This method ensures that the column does not exist before the upgrade task is executed.
     *
     * @throws DotDataException if a data access error occurs.
     */
    private void dropVariableColumn() throws DotDataException {

        try {
            DbConnectionFactory.getConnection().setAutoCommit(true);
        } catch (SQLException e) {
            throw new DotDataException(e.getMessage(), e);
        }

        try {
            final DotConnect dotConnect = new DotConnect();
            dotConnect.setSQL("ALTER TABLE workflow_scheme DROP COLUMN variable_name");
            dotConnect.loadResult();
        } finally {
            DbConnectionFactory.closeSilently();
        }
    }

    /**
     * Validates that the variable_name column exists and is populated with non-null values.
     * <p>
     * This method checks that the variable_name column was added to the workflow_scheme table and
     * that it contains valid data.
     *
     * @throws DotDataException if a data access error occurs.
     */
    private void validateColumnData() throws DotDataException {

        final DotConnect dotConnect = new DotConnect();
        var results = dotConnect.setSQL("SELECT * FROM workflow_scheme").loadObjectResults();

        // Make sure we have the right number of hosts
        assertFalse(results.isEmpty());

        results.forEach(rowMap -> {
            assertTrue(rowMap.containsKey("variable_name"));
            assertNotNull(rowMap.get("variable_name"));
        });
    }

    /**
     * Validates that a specific variable name exists in the workflow_scheme table.
     *
     * @param variableName the variable name to validate.
     */
    private void validateVariableName(final String variableName) {

        final DotConnect dotConnect = new DotConnect();
        dotConnect.setSQL("SELECT count(*) FROM workflow_scheme WHERE variable_name = ?");
        dotConnect.addParam(variableName);

        assertEquals(1, dotConnect.getInt("count"));
    }

    /**
     * Cleans up the test workflows created during the test cases.
     *
     * @param workflowSchemes the workflows to be removed.
     */
    private void cleanUp(final WorkflowScheme... workflowSchemes)
            throws DotDataException, DotSecurityException, AlreadyExistException {
        for (WorkflowScheme testScheme : workflowSchemes) {
            APILocator.getWorkflowAPI().archive(testScheme, APILocator.systemUser());
            APILocator.getWorkflowAPI().deleteScheme(testScheme, APILocator.systemUser());
        }
    }

}
