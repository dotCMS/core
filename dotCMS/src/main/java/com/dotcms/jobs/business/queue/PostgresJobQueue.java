package com.dotcms.jobs.business.queue;

import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.job.JobPaginatedResult;
import com.dotcms.jobs.business.job.JobState;
import com.dotcms.jobs.business.queue.error.JobLockingException;
import com.dotcms.jobs.business.queue.error.JobNotFoundException;
import com.dotcms.jobs.business.queue.error.JobQueueDataException;
import com.dotcms.jobs.business.queue.error.JobQueueException;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.jonpeterson.jackson.module.versioning.VersioningModule;
import io.vavr.Lazy;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * PostgreSQL implementation of the JobQueue interface. This class provides concrete implementations
 * for managing jobs using a PostgreSQL database.
 *
 * <p>The PostgresJobQueue handles all database operations related to job management, including:
 * <ul>
 *   <li>Creating new jobs and adding them to the queue</li>
 *   <li>Retrieving jobs by various criteria (ID, status, date range)</li>
 *   <li>Updating job status and progress</li>
 *   <li>Managing job lifecycle (queueing, processing, completion, failure)</li>
 *   <li>Implementing job locking mechanism for concurrent processing</li>
 * </ul>
 *
 * <p>This implementation uses SQL queries optimized for PostgreSQL, including the use of
 * `SELECT FOR UPDATE SKIP LOCKED` for efficient job queue management in concurrent environments.
 *
 * <p>Note: This class assumes the existence of appropriate database tables (job_queue, job,
 * job_history) as defined in the database schema. Ensure that these tables are properly
 * set up before using this class.
 *
 * @see JobQueue
 * @see Job
 * @see JobState
 */
public class PostgresJobQueue implements JobQueue {

    private static final String CREATE_JOB_QUEUE_QUERY = "INSERT INTO job_queue "
            + "(id, queue_name, state, created_at) VALUES (?, ?, ?, ?)";

    private static final String CREATE_JOB_QUERY = "INSERT INTO job "
            + "(id, queue_name, state, parameters, created_at, execution_node, updated_at) "
            + "VALUES (?, ?, ?, ?::jsonb, ?, ?, ?)";

    private static final String CREATE_JOB_HISTORY_QUERY =
            "INSERT INTO job_history "
                    + "(id, job_id, state, execution_node, created_at) "
                    + "VALUES (?, ?, ?, ?, ?)";

    private static final String SELECT_JOB_BY_ID_QUERY = "SELECT * FROM job WHERE id = ?";

    private static final String UPDATE_AND_GET_NEXT_JOB_WITH_LOCK_QUERY =
            "UPDATE job_queue SET state = ? "
                    + "WHERE id = (SELECT id FROM job_queue WHERE state = ? "
                    + "ORDER BY priority DESC, created_at ASC LIMIT 1 FOR UPDATE SKIP LOCKED) "
                    + "RETURNING *";

    private static final String GET_ACTIVE_JOBS_QUERY =
            "WITH total AS (SELECT COUNT(*) AS total_count " +
                    "    FROM job WHERE queue_name = ? AND state IN (?, ?) " +
                    "), " +
                    "paginated_data AS (SELECT * " +
                    "    FROM job WHERE queue_name = ? AND state IN (?, ?) " +
                    "    ORDER BY created_at LIMIT ? OFFSET ? " +
                    ") " +
                    "SELECT p.*, t.total_count FROM total t LEFT JOIN paginated_data p ON true";

    private static final String GET_COMPLETED_JOBS_QUERY =
            "WITH total AS (SELECT COUNT(*) AS total_count " +
                    "    FROM job WHERE queue_name = ? AND state = ? AND completed_at BETWEEN ? AND ? " +
                    "), " +
                    "paginated_data AS (SELECT * FROM job " +
                    "    WHERE queue_name = ? AND state = ? AND completed_at BETWEEN ? AND ? " +
                    "    ORDER BY completed_at DESC LIMIT ? OFFSET ? " +
                    ") " +
                    "SELECT p.*, t.total_count FROM total t LEFT JOIN paginated_data p ON true";

    private static final String GET_FAILED_JOBS_QUERY =
            "WITH total AS (" +
                    "    SELECT COUNT(*) AS total_count FROM job " +
                    "    WHERE state = ? " +
                    "), " +
                    "paginated_data AS (" +
                    "    SELECT * " +
                    "    FROM job WHERE state = ? " +
                    "    ORDER BY updated_at DESC " +
                    "    LIMIT ? OFFSET ? " +
                    ") " +
                    "SELECT p.*, t.total_count " +
                    "FROM total t " +
                    "LEFT JOIN paginated_data p ON true";

    private static final String GET_JOBS_QUERY =
            "WITH total AS (" +
                    "    SELECT COUNT(*) AS total_count " +
                    "    FROM job " +
                    "), " +
                    "paginated_data AS (" +
                    "    SELECT * " +
                    "    FROM job " +
                    "    ORDER BY created_at DESC " +
                    "    LIMIT ? OFFSET ? " +
                    ") " +
                    "SELECT p.*, t.total_count " +
                    "FROM total t " +
                    "LEFT JOIN paginated_data p ON true";

    private static final String GET_UPDATED_JOBS_SINCE_QUERY = "SELECT * FROM job "
            + "WHERE id = ANY(?) AND updated_at > ?";

    private static final String UPDATE_JOBS_QUERY = "UPDATE job SET state = ?, progress = ?, "
            + "updated_at = ?, started_at = ?, completed_at = ?, execution_node = ?, retry_count = ?, "
            + "result = ?::jsonb WHERE id = ?";

    private static final String INSERT_INTO_JOB_HISTORY_QUERY =
            "INSERT INTO job_history "
                    + "(id, job_id, state, execution_node, created_at, result) "
                    + "VALUES (?, ?, ?, ?, ?, ?::jsonb)";

    private static final String DELETE_JOB_FROM_QUEUE_QUERY = "DELETE FROM job_queue WHERE id = ?";

    private static final String PUT_JOB_BACK_TO_QUEUE_QUERY = "INSERT INTO job_queue "
            + "(id, queue_name, state, priority, created_at) "
            + "VALUES (?, ?, ?, ?, ?) ON CONFLICT (id) DO UPDATE SET state = ?, priority = ?";

    private static final String UPDATE_JOB_PROGRESS_QUERY =
            "UPDATE job SET progress = ?, updated_at = ?"
                    + " WHERE id = ?";

    private static final String HAS_JOB_BEEN_IN_STATE_QUERY = "SELECT "
            + "EXISTS (SELECT 1 FROM job_history WHERE job_id = ? AND state = ?)";

    private static final String COLUMN_TOTAL_COUNT = "total_count";

    /**
     * Jackson mapper configuration and lazy initialized instance.
     */
    private final Lazy<ObjectMapper> objectMapper = Lazy.of(() -> {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.registerModule(new Jdk8Module());
        mapper.registerModule(new GuavaModule());
        mapper.registerModule(new JavaTimeModule());
        mapper.registerModule(new VersioningModule());
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        return mapper;
    });

    @Override
    public String createJob(final String queueName, final Map<String, Object> parameters)
            throws JobQueueException {

        final String serverId = APILocator.getServerAPI().readServerId();

        try {

            final String jobId = UUID.randomUUID().toString();

            final var parametersJson = objectMapper.get().writeValueAsString(parameters);
            final var now = Timestamp.valueOf(LocalDateTime.now());
            final var jobState = JobState.PENDING.name();

            // Insert into job table
            new DotConnect().setSQL(CREATE_JOB_QUERY)
                    .addParam(jobId)
                    .addParam(queueName)
                    .addParam(jobState)
                    .addParam(parametersJson)
                    .addParam(now)
                    .addParam(serverId)
                    .addParam(now)
                    .loadResult();

            // Creating the jobqueue entry
            new DotConnect().setSQL(CREATE_JOB_QUEUE_QUERY)
                    .addParam(jobId)
                    .addParam(queueName)
                    .addParam(jobState)
                    .addParam(now)
                    .loadResult();

            // Creating job_history entry
            new DotConnect().setSQL(CREATE_JOB_HISTORY_QUERY)
                    .addParam(UUID.randomUUID().toString())
                    .addParam(jobId)
                    .addParam(jobState)
                    .addParam(serverId)
                    .addParam(now)
                    .loadResult();

            return jobId;
        } catch (JsonProcessingException e) {
            Logger.error(this, "Failed to serialize job parameters", e);
            throw new JobQueueException("Failed to serialize job parameters", e);
        } catch (DotDataException e) {
            Logger.error(this, "Database error while creating job", e);
            throw new JobQueueDataException("Database error while creating job", e);
        }
    }

    @Override
    public Job getJob(final String jobId) throws JobNotFoundException, JobQueueDataException {

        try {
            DotConnect dc = new DotConnect();
            dc.setSQL(SELECT_JOB_BY_ID_QUERY);
            dc.addParam(jobId);

            List<Map<String, Object>> results = dc.loadObjectResults();
            if (!results.isEmpty()) {
                return DBJobTransformer.toJob(results.get(0));
            }

            Logger.warn(this, "Job with id: " + jobId + " not found");
            throw new JobNotFoundException(jobId);
        } catch (DotDataException e) {
            Logger.error(this, "Database error while fetching job", e);
            throw new JobQueueDataException("Database error while fetching job", e);
        }
    }

    @Override
    public JobPaginatedResult getActiveJobs(final String queueName, final int page,
            final int pageSize) throws JobQueueDataException {

        try {

            DotConnect dc = new DotConnect();
            dc.setSQL(GET_ACTIVE_JOBS_QUERY);
            dc.addParam(queueName);
            dc.addParam(JobState.PENDING.name());
            dc.addParam(JobState.RUNNING.name());
            dc.addParam(queueName);  // Repeated for paginated_data CTE
            dc.addParam(JobState.PENDING.name());
            dc.addParam(JobState.RUNNING.name());
            dc.addParam(pageSize);
            dc.addParam((page - 1) * pageSize);

            return jobPaginatedResult(page, pageSize, dc);
        } catch (DotDataException e) {
            Logger.error(this, "Database error while fetching active jobs", e);
            throw new JobQueueDataException("Database error while fetching active jobs", e);
        }
    }

    @Override
    public JobPaginatedResult getCompletedJobs(final String queueName,
            final LocalDateTime startDate,
            final LocalDateTime endDate, final int page, final int pageSize)
            throws JobQueueDataException {

        try {
            DotConnect dc = new DotConnect();
            dc.setSQL(GET_COMPLETED_JOBS_QUERY);
            dc.addParam(queueName);
            dc.addParam(JobState.COMPLETED.name());
            dc.addParam(Timestamp.valueOf(startDate));
            dc.addParam(Timestamp.valueOf(endDate));
            dc.addParam(queueName);  // Repeated for paginated_data CTE
            dc.addParam(JobState.COMPLETED.name());
            dc.addParam(Timestamp.valueOf(startDate));
            dc.addParam(Timestamp.valueOf(endDate));
            dc.addParam(pageSize);
            dc.addParam((page - 1) * pageSize);

            return jobPaginatedResult(page, pageSize, dc);
        } catch (DotDataException e) {
            Logger.error(this, "Database error while fetching completed jobs", e);
            throw new JobQueueDataException("Database error while fetching completed jobs", e);
        }
    }

    @Override
    public JobPaginatedResult getJobs(final int page, final int pageSize)
            throws JobQueueDataException {

        try {
            DotConnect dc = new DotConnect();
            dc.setSQL(GET_JOBS_QUERY);
            dc.addParam(pageSize);
            dc.addParam((page - 1) * pageSize);

            return jobPaginatedResult(page, pageSize, dc);
        } catch (DotDataException e) {
            Logger.error(this, "Database error while fetching jobs", e);
            throw new JobQueueDataException("Database error while fetching jobs", e);
        }
    }

    @Override
    public JobPaginatedResult getFailedJobs(final int page, final int pageSize)
            throws JobQueueDataException {

        try {
            DotConnect dc = new DotConnect();
            dc.setSQL(GET_FAILED_JOBS_QUERY);
            dc.addParam(JobState.FAILED.name());
            dc.addParam(JobState.FAILED.name());  // Repeated for paginated_data CTE
            dc.addParam(pageSize);
            dc.addParam((page - 1) * pageSize);

            return jobPaginatedResult(page, pageSize, dc);
        } catch (DotDataException e) {
            Logger.error(this, "Database error while fetching failed jobs", e);
            throw new JobQueueDataException("Database error while fetching failed jobs", e);
        }
    }

    @Override
    public void updateJobStatus(final Job job) throws JobQueueDataException {

        final String serverId = APILocator.getServerAPI().readServerId();

        try {

            DotConnect dc = new DotConnect();
            dc.setSQL(UPDATE_JOBS_QUERY);
            dc.addParam(job.state().name());
            dc.addParam(job.progress());
            dc.addParam(Timestamp.valueOf(LocalDateTime.now()));
            dc.addParam(job.startedAt().map(Timestamp::valueOf).orElse(null));
            dc.addParam(job.completedAt().map(Timestamp::valueOf).orElse(null));
            dc.addParam(serverId);
            dc.addParam(job.retryCount());
            dc.addParam(job.result().map(r -> {
                try {
                    return objectMapper.get().writeValueAsString(r);
                } catch (Exception e) {
                    Logger.error(this, "Failed to serialize job result", e);
                    return null;
                }
            }).orElse(null));
            dc.addParam(job.id());
            dc.loadResult();

            // Update job_history
            DotConnect historyDc = new DotConnect();
            historyDc.setSQL(INSERT_INTO_JOB_HISTORY_QUERY);
            historyDc.addParam(UUID.randomUUID().toString());
            historyDc.addParam(job.id());
            historyDc.addParam(job.state().name());
            historyDc.addParam(serverId);
            historyDc.addParam(Timestamp.valueOf(LocalDateTime.now()));
            historyDc.addParam(job.result().map(r -> {
                try {
                    return objectMapper.get().writeValueAsString(r);
                } catch (Exception e) {
                    Logger.error(this, "Failed to serialize job result for history", e);
                    return null;
                }
            }).orElse(null));
            historyDc.loadResult();

            // Remove from job_queue if completed, failed, or canceled
            if (job.state() == JobState.COMPLETED
                    || job.state() == JobState.FAILED
                    || job.state() == JobState.CANCELED) {
                removeJobFromQueue(job.id());
            }
        } catch (DotDataException e) {
            Logger.error(this, "Database error while updating job status", e);
            throw new JobQueueDataException("Database error while updating job status", e);
        }
    }

    @Override
    public List<Job> getUpdatedJobsSince(final Set<String> jobIds, final LocalDateTime since)
            throws JobQueueDataException {

        try {
            if (jobIds.isEmpty()) {
                return Collections.emptyList();
            }

            DotConnect dc = new DotConnect();
            dc.setSQL(GET_UPDATED_JOBS_SINCE_QUERY);
            dc.addParam(jobIds.toArray(new String[0]));
            dc.addParam(Timestamp.valueOf(since));

            List<Map<String, Object>> results = dc.loadObjectResults();
            return results.stream().map(DBJobTransformer::toJob).collect(Collectors.toList());
        } catch (DotDataException e) {
            Logger.error(this, "Database error while fetching updated jobs", e);
            throw new JobQueueDataException("Database error while fetching updated jobs", e);
        }
    }

    @Override
    public void putJobBackInQueue(final Job job) throws JobQueueDataException {

        try {
            DotConnect dc = new DotConnect();
            dc.setSQL(PUT_JOB_BACK_TO_QUEUE_QUERY);
            dc.addParam(job.id());
            dc.addParam(job.queueName());
            dc.addParam(JobState.PENDING.name());
            dc.addParam(0); // Default priority
            dc.addParam(Timestamp.valueOf(LocalDateTime.now()));
            dc.addParam(JobState.PENDING.name());
            dc.addParam(0); // Default priority
            dc.loadResult();
        } catch (DotDataException e) {
            Logger.error(this, "Database error while putting job back in queue", e);
            throw new JobQueueDataException(
                    "Database error while putting job back in queue", e
            );
        }
    }

    @Override
    public Job nextJob() throws JobQueueDataException, JobLockingException {

        try {
            DotConnect dc = new DotConnect();
            dc.setSQL(UPDATE_AND_GET_NEXT_JOB_WITH_LOCK_QUERY);
            dc.addParam(JobState.RUNNING.name());
            dc.addParam(JobState.PENDING.name());

            List<Map<String, Object>> results = dc.loadObjectResults();
            if (!results.isEmpty()) {

                // Fetch full job details from the job table
                String jobId = (String) results.get(0).get("id");
                return getJob(jobId);
            }

            return null;
        } catch (DotDataException e) {
            Logger.error(this, "Database error while fetching next job", e);
            throw new JobQueueDataException("Database error while fetching next job", e);
        } catch (JobNotFoundException e) {
            Logger.error(this, "Job not found while fetching next job", e);
            throw new JobQueueDataException("Job not found while fetching next job", e);
        } catch (Exception e) {
            Logger.error(this, "Error while locking next job", e);
            throw new JobLockingException("Error while locking next job: " + e.getMessage());
        }
    }

    @Override
    public void updateJobProgress(final String jobId, final float progress)
            throws JobQueueDataException {

        try {
            DotConnect dc = new DotConnect();
            dc.setSQL(UPDATE_JOB_PROGRESS_QUERY);
            dc.addParam(progress);
            dc.addParam(Timestamp.valueOf(LocalDateTime.now()));
            dc.addParam(jobId);
            dc.loadResult();
        } catch (DotDataException e) {
            Logger.error(this, "Database error while updating job progress", e);
            throw new JobQueueDataException("Database error while updating job progress", e);
        }
    }

    @Override
    public void removeJobFromQueue(final String jobId) throws JobQueueDataException {

        try {
            DotConnect dc = new DotConnect();
            dc.setSQL(DELETE_JOB_FROM_QUEUE_QUERY);
            dc.addParam(jobId);
            dc.loadResult();
        } catch (DotDataException e) {
            Logger.error(this, "Database error while removing job", e);
            throw new JobQueueDataException("Database error while removing job", e);
        }
    }

    @Override
    public boolean hasJobBeenInState(String jobId, JobState state) throws JobQueueDataException {

        try {
            DotConnect dc = new DotConnect();
            dc.setSQL(HAS_JOB_BEEN_IN_STATE_QUERY);
            dc.addParam(jobId);
            dc.addParam(state.name());
            List<Map<String, Object>> results = dc.loadObjectResults();

            if (!results.isEmpty()) {
                return (Boolean) results.get(0).get("exists");
            }

            return false;
        } catch (Exception e) {
            Logger.error(this, "Error checking job state history", e);
            throw new JobQueueDataException("Error checking job state history", e);
        }
    }

    /**
     * Helper method to create a JobPaginatedResult from a DotConnect query result.
     *
     * @param page     The current page number
     * @param pageSize The number of items per page
     * @param dc       The DotConnect instance with the query results
     * @return A JobPaginatedResult instance
     * @throws DotDataException If there is an error loading the query results
     */
    private static JobPaginatedResult jobPaginatedResult(
            int page, int pageSize, DotConnect dc) throws DotDataException {

        final var results = dc.loadObjectResults();

        long totalCount = 0;
        List<Job> jobs = new ArrayList<>();

        if (!results.isEmpty()) {
            totalCount = ((Number) results.get(0).get(COLUMN_TOTAL_COUNT)).longValue();
            jobs = results.stream()
                    .filter(row -> row.get("id") != null) // Filter out rows without job data
                    .map(DBJobTransformer::toJob)
                    .collect(Collectors.toList());
        }

        return JobPaginatedResult.builder()
                .jobs(jobs)
                .total(totalCount)
                .page(page)
                .pageSize(pageSize)
                .build();
    }

}
