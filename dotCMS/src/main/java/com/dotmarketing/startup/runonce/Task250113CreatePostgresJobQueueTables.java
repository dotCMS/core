package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Upgrade task to create the necessary tables and indexes for the Postgres Job Queue
 * implementation. This task creates the job_queue, job, and job_history tables along with their
 * associated indexes.
 */
public class Task250113CreatePostgresJobQueueTables implements StartupTask {

    @Override
    public boolean forceRun() {
        return true;
    }

    /**
     * Executes the upgrade task, creating the necessary tables and indexes for the Job Queue.
     *
     * @throws DotDataException    if a data access error occurs.
     * @throws DotRuntimeException if a runtime error occurs.
     */
    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {

        final Connection connection = DbConnectionFactory.getConnection();
        final DotDatabaseMetaData databaseMetaData = new DotDatabaseMetaData();

        try {

            // job_queue table
            if (!databaseMetaData.tableExists(connection, "job_queue")) {
                createJobQueueTable();
                createJobQueueTableIndexes();
            }

            // job table
            if (!databaseMetaData.tableExists(connection, "job")) {
                createJobTable();
                createJobTableIndexes();
            }

            // job_history table
            if (!databaseMetaData.tableExists(connection, "job_history")) {
                createJobHistoryTable();
                createJobHistoryTableIndexes();
            }
        } catch (SQLException e) {
            Logger.error("Error creating job queue tables", e);
            throw new DotRuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Creates the job_queue table.
     *
     * @throws DotDataException if an error occurs while creating the table.
     */
    private void createJobQueueTable() throws DotDataException {
        try {
            new DotConnect().executeStatement(
                    "CREATE TABLE job_queue (" +
                            "id VARCHAR(255) PRIMARY KEY, " +
                            "queue_name VARCHAR(255) NOT NULL, " +
                            "state VARCHAR(50) NOT NULL, " +
                            "priority INTEGER DEFAULT 0, " +
                            "created_at timestamptz NOT NULL)"
            );
        } catch (Exception ex) {
            throw new DotDataException(ex.getMessage(), ex);
        }
    }

    /**
     * Creates the necessary indexes for the job_queue table.
     *
     * @throws DotDataException if an error occurs while creating the indexes.
     */
    private void createJobQueueTableIndexes() throws DotDataException {
        try {
            new DotConnect().executeStatement(
                    "CREATE INDEX idx_job_queue_status ON job_queue (state)");
            new DotConnect().executeStatement(
                    "CREATE INDEX idx_job_queue_priority_created_at ON job_queue (priority DESC, created_at ASC)");
        } catch (Exception ex) {
            throw new DotDataException(ex.getMessage(), ex);
        }
    }

    /**
     * Creates the job table.
     *
     * @throws DotDataException if an error occurs while creating the table.
     */
    private void createJobTable() throws DotDataException {
        try {
            new DotConnect().executeStatement(
                    "CREATE TABLE job (" +
                            "id VARCHAR(255) PRIMARY KEY, " +
                            "queue_name VARCHAR(255) NOT NULL, " +
                            "state VARCHAR(50) NOT NULL, " +
                            "parameters JSONB NOT NULL, " +
                            "result JSONB, " +
                            "progress FLOAT DEFAULT 0, " +
                            "created_at timestamptz NOT NULL, " +
                            "updated_at timestamptz NOT NULL, " +
                            "started_at timestamptz, " +
                            "completed_at timestamptz, " +
                            "execution_node VARCHAR(255), " +
                            "retry_count INTEGER DEFAULT 0)"
            );
        } catch (Exception ex) {
            throw new DotDataException(ex.getMessage(), ex);
        }
    }

    /**
     * Creates the necessary indexes for the job table.
     *
     * @throws DotDataException if an error occurs while creating the indexes.
     */
    private void createJobTableIndexes() throws DotDataException {
        try {
            new DotConnect().executeStatement(
                    "CREATE INDEX idx_job_parameters ON job USING GIN (parameters)");
            new DotConnect().executeStatement(
                    "CREATE INDEX idx_job_result ON job USING GIN (result)");
            new DotConnect().executeStatement("CREATE INDEX idx_job_status ON job (state)");
            new DotConnect().executeStatement(
                    "CREATE INDEX idx_job_created_at ON job (created_at)");
        } catch (Exception ex) {
            throw new DotDataException(ex.getMessage(), ex);
        }
    }

    /**
     * Creates the job_history table.
     *
     * @throws DotDataException if an error occurs while creating the table.
     */
    private void createJobHistoryTable() throws DotDataException {
        try {
            new DotConnect().executeStatement(
                    "CREATE TABLE job_history (" +
                            "id VARCHAR(255) PRIMARY KEY, " +
                            "job_id VARCHAR(255) NOT NULL, " +
                            "state VARCHAR(50) NOT NULL, " +
                            "execution_node VARCHAR(255), " +
                            "created_at timestamptz NOT NULL, " +
                            "result JSONB, " +
                            "FOREIGN KEY (job_id) REFERENCES job (id))"
            );
        } catch (Exception ex) {
            throw new DotDataException(ex.getMessage(), ex);
        }
    }

    /**
     * Creates the necessary indexes for the job_history table.
     *
     * @throws DotDataException if an error occurs while creating the indexes.
     */
    private void createJobHistoryTableIndexes() throws DotDataException {
        try {
            new DotConnect().executeStatement(
                    "CREATE INDEX idx_job_history_job_id ON job_history (job_id)");
            new DotConnect().executeStatement(
                    "CREATE INDEX idx_job_history_job_id_state ON job_history (job_id, state)");
        } catch (Exception ex) {
            throw new DotDataException(ex.getMessage(), ex);
        }
    }

}