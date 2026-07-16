package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Task251212AddVersionColumnIndicesTable is responsible for updating the database schema by
 * adding a new column and constraints to the "indicies" table. This task ensures that the
 * database supports tracking a unique combination of index type and version.
 *
 * Implements the {@code StartupTask} interface to define custom behavior for executing
 * database updates.
 */
public class Task251212AddVersionColumnIndicesTable implements StartupTask {

    private final DotDatabaseMetaData  helper = new DotDatabaseMetaData();

    /**
     * Determines whether the task should be forcibly executed by checking if the "index_version"
     * column exists in the "indicies" table. This method ensures the task runs only if
     * the column is not present, indicating that the schema update is necessary.
     *
     * @return true if the "index_version" column does not exist and the task should be forcibly executed;
     *         false if the column exists or an error occurs during the check.
     */
    @Override
    public boolean forceRun() {
        try {
            return !helper.hasColumn("indicies", "index_version");
        } catch (SQLException e) {
            Logger.error(this, e.getMessage(),e);
            return false;
        }
    }

    /**
     * Executes the database upgrade task for the "indicies" table. This method performs the following:
     *
     * 1. Sets the auto-commit mode on the database connection to true.
     * 2. Drops the existing uniqueness constraint on the "index_type" column, if it exists.
     * 3. Adds a new column "index_version" to the table if it does not already exist.
     * 4. Adds a new uniqueness constraint that ensures the combination of "index_type" and
     *    "index_version" is unique, provided it has not already been added.
     *
     * The method encapsulates database schema modifications necessary to update the table structure
     * and enforces new constraints as required by updated business or schema requirements.
     *
     * @throws DotDataException if a database error occurs while executing SQL statements or modifying the schema.
     * @throws DotRuntimeException if an unexpected runtime exception occurs during execution.
     */
    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        try {
            DbConnectionFactory.getConnection().setAutoCommit(true);
        } catch (SQLException e) {
            throw new DotDataException(e.getMessage(), e);
        }
        //Prior tho this change there was a constraint restricting uniqueness of index + type.
        //This is changing since now we need to be able to track a uniqueness index_type and index_version
        final DotConnect dropIndexStatement = new DotConnect().setSQL("ALTER TABLE indicies DROP CONSTRAINT IF EXISTS indicies_index_type_key");
        dropIndexStatement.loadResult();

        //Add the new column if not already defined
        final DotConnect addColumnStatement = new DotConnect().setSQL("ALTER TABLE indicies ADD COLUMN IF NOT EXISTS index_version varchar(16) NULL");
        addColumnStatement.loadResult();

        //This Constraint allows me to track uniqueness between index_type and index_version
        final List<String> constraints = helper.getConstraints("indicies").stream()
                .map(String::toLowerCase)
                .collect(Collectors.toList());
        if (!constraints.contains("uq_index_type_version")) {
            final DotConnect addIndexStatement = new DotConnect().setSQL(
                    "ALTER TABLE indicies ADD CONSTRAINT uq_index_type_version UNIQUE (index_type, index_version)");
            addIndexStatement.loadResult();
        }
    }


}
