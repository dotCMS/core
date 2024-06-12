package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.StringUtils;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Task240606AddVariableColumnToWorkflow implements StartupTask {

    private final Set<String> workflowVariableNames = new HashSet<>();

    private static final Map<String, String> digitToLetter = new HashMap<>();

    static {
        digitToLetter.put("0", "zero");
        digitToLetter.put("1", "one");
        digitToLetter.put("2", "two");
        digitToLetter.put("3", "three");
        digitToLetter.put("4", "four");
        digitToLetter.put("5", "five");
        digitToLetter.put("6", "six");
        digitToLetter.put("7", "seven");
        digitToLetter.put("8", "eight");
        digitToLetter.put("9", "nine");
    }

    @Override
    public boolean forceRun() {
        return true;
    }

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {

        // Adding the variable_name column to the workflow_scheme table
        addColumn();

        // Populating the variable_name column
        new DotConnect().
                setSQL("SELECT * FROM workflow_scheme ORDER BY mod_date").
                loadObjectResults().
                forEach(this::updateRow);

        // Setting the variable_name column to be not null
        setNotNull();

        // Adding an index to the variable_name column
        addIndex();
    }

    /**
     * Adds a column named 'variable_name' to the 'workflow_scheme' table in the database.
     *
     * @throws DotDataException if an error occurs during the database operation
     */
    private void addColumn() throws DotDataException {
        try {
            new DotConnect().executeStatement("ALTER TABLE workflow_scheme "
                    + "ADD variable_name "
                    + "varchar(255) unique");
        } catch (Exception ex) {
            throw new DotDataException(ex.getMessage(), ex);
        }
    }

    /**
     * Updates a row in the workflow_scheme table with a new variable name based on the provided row
     * data.
     *
     * @param row a map containing the row data
     */
    private void updateRow(final Map<String, Object> row) {

        try {

            final var workflowName = (String) row.get("name");
            final var workflowId = (String) row.get("id");
            final var workflowVariableName = generateVariableName(workflowName);

            Logger.info(this,
                    String.format(
                            "Adding to workflow [%s] with name [%s] a new variable name: %s",
                            workflowId,
                            workflowName,
                            workflowVariableName
                    )
            );

            new DotConnect().setSQL("UPDATE workflow_scheme SET variable_name=? where id =?").
                    addParam(workflowVariableName).
                    addParam(workflowId).loadResult();

            workflowVariableNames.add(workflowVariableName);
        } catch (DotDataException e) {
            throw new DotRuntimeException(e);
        }
    }

    /**
     * Sets the 'variable_name' column in the 'workflow_scheme' table to be not null. This method
     * alters the column to enforce the not null constraint.
     *
     * @throws DotDataException if an error occurs during the database operation
     */
    private void setNotNull() throws DotDataException {
        try {
            new DotConnect().executeStatement("ALTER TABLE workflow_scheme "
                    + "ALTER COLUMN variable_name SET NOT NULL");
        } catch (Exception ex) {
            throw new DotDataException(ex.getMessage(), ex);
        }
    }

    /**
     * Adds an index on the 'variable_name' column in the 'workflow_scheme' table. This improves the
     * performance of queries that involve searching or sorting by the 'variable_name' column.
     *
     * @throws DotDataException if an error occurs during the database operation
     */
    private void addIndex() throws DotDataException {
        try {
            new DotConnect().executeStatement("CREATE INDEX idx_workflow_lower_variable_name "
                    + "ON workflow_scheme (LOWER(variable_name));");
        } catch (Exception ex) {
            throw new DotDataException(ex.getMessage(), ex);
        }
    }

    /**
     * Generates a variable name for a workflow scheme based on its name. The generated variable
     * name is unique among all the workflow schemes.
     * <p>
     * Most of the logic in this method is copied from
     * {@link com.dotcms.contenttype.business.ContentTypeFactoryImpl#suggestVelocityVar(String)}
     *
     * @param workflowName the name of the workflow scheme
     * @return the generated variable name
     * @throws DotDataException if an error occurs during the database operation
     */
    private String generateVariableName(final String workflowName) throws DotDataException {

        final String suggestedVariableName = convertToVariableName(workflowName, true);
        String variableName = suggestedVariableName;

        for (int i = 1; i < 10000; i++) {

            if (!workflowVariableNames.contains(variableName)) {
                return variableName;
            }

            variableName = suggestedVariableName + i;
        }

        throw new DotDataException(
                String.format(
                        "Unable to generate a workflow scheme variable name for [%s]",
                        workflowName
                )
        );
    }

    /**
     * Logic copied from
     * {@link com.dotmarketing.util.VelocityUtil#convertToVelocityVariable(String, boolean)}
     *
     * @param workflowName         the string to be processed
     * @param firstLetterUppercase if the first letter should be uppercase
     * @return the string converted to a valid variable name
     */
    private String convertToVariableName(final String workflowName, boolean firstLetterUppercase) {

        String variableToReturn = workflowName;

        // starts with number
        if (variableToReturn.matches("^\\d.*")) {
            variableToReturn = replaceStartingNumberWithWrittenNumber(variableToReturn);
        }

        // start with char different from "_A-Za-z"
        if (variableToReturn.matches("[^_A-Za-z].*")) {
            variableToReturn = variableToReturn.replaceAll("[^_0-9A-Za-z]", "_");
        }

        if (variableToReturn.matches("[a-zA-Z].*")) {
            variableToReturn = (firstLetterUppercase)
                    ? StringUtils.camelCaseUpper(variableToReturn)
                    : StringUtils.camelCaseLower(variableToReturn);
        }

        return variableToReturn;
    }

    /**
     * Logic copied from
     * {@link
     * com.dotcms.rendering.velocity.util.VelocityUtil#replaceStartingNumberWithWrittenNumber(String)}
     *
     * @param string the string to be processed
     * @return the string with the starting number replaced by the written number
     */
    private String replaceStartingNumberWithWrittenNumber(final String string) {

        final String subString = string.substring(0, 1);

        if (!subString.matches("[0-9]")) {
            return string;
        }

        return digitToLetter.get(subString) + string.substring(1);
    }

}
