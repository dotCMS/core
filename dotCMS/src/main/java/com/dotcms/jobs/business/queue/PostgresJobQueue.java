package com.dotcms.jobs.business.queue;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.jobs.business.error.ErrorDetail;
import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.job.JobPaginatedResult;
import com.dotcms.jobs.business.job.JobResult;
import com.dotcms.jobs.business.job.JobState;
import com.dotcms.jobs.business.queue.error.JobLockingException;
import com.dotcms.jobs.business.queue.error.JobNotFoundException;
import com.dotcms.jobs.business.queue.error.JobQueueDataException;
import com.dotcms.jobs.business.queue.error.JobQueueException;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.jonpeterson.jackson.module.versioning.VersioningModule;
import io.vavr.Lazy;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    private static final String DETECT_AND_MARK_ABANDONED_WITH_LOCK_QUERY =
            "WITH active_states AS (" +
                    "    SELECT unnest(ARRAY[$??$]) as state"
                    +
                    "), abandoned_jobs AS (" +
                    "    SELECT j.*, j.result as existing_result" +
                    "    FROM job j" +
                    "    INNER JOIN active_states a ON j.state = a.state::text" +
                    "    WHERE j.updated_at < ?" +
                    "    ORDER BY j.updated_at ASC" +
                    "    LIMIT 1" +
                    "    FOR UPDATE SKIP LOCKED" +
                    ") " +
                    "UPDATE job " +
                    "SET state = ?, updated_at = ? " +
                    "WHERE id IN (SELECT id FROM abandoned_jobs) " +
                    "RETURNING *";

    private static final String GET_JOBS_QUERY_BY_QUEUE_AND_STATE =
            "WITH total AS (SELECT COUNT(*) AS total_count " +
                    "    FROM job WHERE queue_name = ? AND state IN $??$ " +
                    "), " +
                    "paginated_data AS (SELECT * " +
                    "    FROM job WHERE queue_name = ? AND state IN $??$ " +
                    "    ORDER BY $ORDER_BY$ LIMIT ? OFFSET ? " +
                    ") " +
                    "SELECT p.*, t.total_count FROM total t LEFT JOIN paginated_data p ON true";

    private static final String GET_JOBS_QUERY_BY_QUEUE_AND_STATE_IN_DATE_RANGE =
            "WITH total AS (SELECT COUNT(*) AS total_count " +
                    "    FROM job WHERE queue_name = ? AND state IN $??$ AND $DATE_COLUMN$ BETWEEN ? AND ? "
                    +
                    "), " +
                    "paginated_data AS (SELECT * FROM job " +
                    "    WHERE queue_name = ? AND state IN $??$ AND $DATE_COLUMN$ BETWEEN ? AND ? "
                    +
                    "    ORDER BY $ORDER_BY$ DESC LIMIT ? OFFSET ? " +
                    ") " +
                    "SELECT p.*, t.total_count FROM total t LEFT JOIN paginated_data p ON true";

    private static final String GET_JOBS_QUERY_BY_STATE =
            "WITH total AS (" +
                    "    SELECT COUNT(*) AS total_count FROM job " +
                    "    WHERE state IN $??$ " +
                    "), " +
                    "paginated_data AS (" +
                    "    SELECT * " +
                    "    FROM job WHERE state IN $??$ " +
                    "    ORDER BY $ORDER_BY$ DESC " +
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

    private static final String GET_JOBS_FOR_QUEUE_QUERY =
            "WITH total AS (" +
                    "    SELECT COUNT(*) AS total_count " +
                    "    FROM job " +
                    "    WHERE queue_name = ? " +
                    "), " +
                    "paginated_data AS (" +
                    "    SELECT * " +
                    "    FROM job " +
                    "    WHERE queue_name = ? " +
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
            + "EXISTS (SELECT 1 FROM job_history WHERE job_id = ? AND state IN $??$)";

    private static final String COLUMN_TOTAL_COUNT = "total_count";
    private static final String COLUMN_COMPLETED_AT = "completed_at";
    private static final String COLUMN_UPDATED_AT = "updated_at";
    private static final String COLUMN_CREATED_AT = "created_at";

    private static final String REPLACE_TOKEN_PARAMETERS = "$??$";
    private static final String REPLACE_TOKEN_ORDER_BY = "$ORDER_BY$";
    private static final String REPLACE_TOKEN_DATE_COLUMN = "$DATE_COLUMN$";

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

    @WrapInTransaction
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

    @CloseDBIfOpened
    @Override
    public Job getJob(final String jobId) throws JobNotFoundException, JobQueueDataException {

        // Check cache first
        Job job = CacheLocator.getJobCache().get(jobId);
        if (UtilMethods.isSet(job)) {
            return job;
        }

        try {
            DotConnect dc = new DotConnect();
            dc.setSQL(SELECT_JOB_BY_ID_QUERY);
            dc.addParam(jobId);

            List<Map<String, Object>> results = dc.loadObjectResults();
            if (!results.isEmpty()) {

                job = DBJobTransformer.toJob(results.get(0));

                // Cache the job
                CacheLocator.getJobCache().put(job);

                return job;
            }

            Logger.warn(this, "Job with id: " + jobId + " not found");
            throw new JobNotFoundException(jobId);
        } catch (DotDataException e) {
            Logger.error(this, "Database error while fetching job", e);
            throw new JobQueueDataException("Database error while fetching job", e);
        }
    }

    @CloseDBIfOpened
    @Override
    public JobState getJobState(final String jobId)
            throws JobNotFoundException, JobQueueDataException {

        // Check cache first
        JobState jobState = CacheLocator.getJobCache().getState(jobId);
        if (UtilMethods.isSet(jobState)) {
            return jobState;
        }

        final var job = getJob(jobId);

        // Cache the job state
        CacheLocator.getJobCache().putState(job.id(), job.state());

        return job.state();
    }

    @CloseDBIfOpened
    @Override
    public JobPaginatedResult getActiveJobs(final String queueName, final int page, final int pageSize)
            throws JobQueueDataException {

        return getJobsByState(JobStateQueryParameters.builder()
                .queueName(queueName)
                .page(page)
                .pageSize(pageSize)
                .orderByColumn(COLUMN_CREATED_AT)
                .states(JobState.PENDING, JobState.RUNNING,
                        JobState.FAILED, JobState.ABANDONED, JobState.CANCEL_REQUESTED,
                        JobState.CANCELLING).build()
        );
    }


    @CloseDBIfOpened
    @Override
    public JobPaginatedResult getCompletedJobs(final String queueName, final int page, final int pageSize)
            throws JobQueueDataException {

        return getJobsByState(JobStateQueryParameters.builder()
                .queueName(queueName)
                .page(page)
                .pageSize(pageSize)
                .orderByColumn(COLUMN_COMPLETED_AT)
                .states(JobState.SUCCESS, JobState.CANCELED,
                        JobState.ABANDONED_PERMANENTLY, JobState.FAILED_PERMANENTLY).build()
        );
    }

    @CloseDBIfOpened
    @Override
    public JobPaginatedResult getCanceledJobs(final String queueName, final int page, final int pageSize)
            throws JobQueueDataException {

        return getJobsByState(JobStateQueryParameters.builder()
                .queueName(queueName)
                .page(page)
                .pageSize(pageSize)
                .orderByColumn(COLUMN_UPDATED_AT)
                .states(JobState.CANCEL_REQUESTED, JobState.CANCELLING, JobState.CANCELED).build()
        );
    }

    @CloseDBIfOpened
    @Override
    public JobPaginatedResult getFailedJobs(final String queueName, final int page, final int pageSize)
            throws JobQueueDataException {

        return getJobsByState(JobStateQueryParameters.builder()
                .queueName(queueName)
                .page(page)
                .pageSize(pageSize)
                .orderByColumn(COLUMN_UPDATED_AT)
                .states(JobState.FAILED, JobState.FAILED_PERMANENTLY).build()
        );
    }

    @CloseDBIfOpened
    @Override
    public JobPaginatedResult getAbandonedJobs(final String queueName, final int page, final int pageSize)
            throws JobQueueDataException {

        return getJobsByState(JobStateQueryParameters.builder()
                .queueName(queueName)
                .page(page)
                .pageSize(pageSize)
                .orderByColumn(COLUMN_UPDATED_AT)
                .states(JobState.ABANDONED, JobState.ABANDONED_PERMANENTLY).build()
        );
    }

    @CloseDBIfOpened
    @Override
    public JobPaginatedResult getSuccessfulJobs(final String queueName, final int page, final int pageSize)
            throws JobQueueDataException {

        return getJobsByState(JobStateQueryParameters.builder()
                .queueName(queueName)
                .page(page)
                .pageSize(pageSize)
                .orderByColumn(COLUMN_COMPLETED_AT)
                .states(JobState.SUCCESS).build()
        );
    }

    @CloseDBIfOpened
    @Override
    public JobPaginatedResult getCompletedJobs(final String queueName,
            final LocalDateTime startDate,
            final LocalDateTime endDate, final int page, final int pageSize)
            throws JobQueueDataException {

        return getJobsByState(JobStateQueryParameters.builder()
                .queueName(queueName)
                .startDate(startDate)
                .endDate(endDate)
                .filterDateColumn(COLUMN_COMPLETED_AT)
                .page(page)
                .pageSize(pageSize)
                .orderByColumn(COLUMN_COMPLETED_AT)
                .states(JobState.SUCCESS, JobState.CANCELED,
                        JobState.ABANDONED_PERMANENTLY, JobState.FAILED_PERMANENTLY).build()
        );
    }

    @CloseDBIfOpened
    @Override
    public JobPaginatedResult getJobs(final String queueName, final int page, final int pageSize)
            throws JobQueueDataException {

        try {
            DotConnect dc = new DotConnect();
            dc.setSQL(GET_JOBS_FOR_QUEUE_QUERY);
            dc.addParam(queueName);
            dc.addParam(queueName);
            dc.addParam(pageSize);
            dc.addParam((page - 1) * pageSize);

            return jobPaginatedResult(page, pageSize, dc);
        } catch (DotDataException e) {
            Logger.error(this, "Database error while fetching jobs for queue: " + queueName, e);
            throw new JobQueueDataException("Database error while fetching jobs for queue: " + queueName, e);
        }
    }

    @CloseDBIfOpened
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

    @CloseDBIfOpened
    @Override
    public JobPaginatedResult getActiveJobs(final int page, final int pageSize)
            throws JobQueueDataException {

        return getJobsByState(JobStateQueryParameters.builder()
                .page(page)
                .pageSize(pageSize)
                .orderByColumn(COLUMN_CREATED_AT)
                .states(JobState.PENDING, JobState.RUNNING,
                        JobState.FAILED, JobState.ABANDONED, JobState.CANCEL_REQUESTED,
                        JobState.CANCELLING).build()
        );
    }

    @CloseDBIfOpened
    @Override
    public JobPaginatedResult getCompletedJobs(final int page, final int pageSize)
            throws JobQueueDataException {

        return getJobsByState(JobStateQueryParameters.builder()
                .page(page)
                .pageSize(pageSize)
                .orderByColumn(COLUMN_COMPLETED_AT)
                .states(JobState.SUCCESS, JobState.CANCELED,
                        JobState.ABANDONED_PERMANENTLY, JobState.FAILED_PERMANENTLY).build()
        );
    }

    @CloseDBIfOpened
    @Override
    public JobPaginatedResult getSuccessfulJobs(final int page, final int pageSize)
            throws JobQueueDataException {
        return getJobsByState(JobStateQueryParameters.builder()
                .page(page)
                .pageSize(pageSize)
                .orderByColumn(COLUMN_COMPLETED_AT)
                .states(JobState.SUCCESS).build()
        );
    }

    @CloseDBIfOpened
    @Override
    public JobPaginatedResult getCanceledJobs(final int page, final int pageSize)
            throws JobQueueDataException {
        return getJobsByState(JobStateQueryParameters.builder()
                .page(page)
                .pageSize(pageSize)
                .orderByColumn(COLUMN_UPDATED_AT)
                .states(JobState.CANCEL_REQUESTED, JobState.CANCELLING, JobState.CANCELED).build()
        );
    }

    @CloseDBIfOpened
    @Override
    public JobPaginatedResult getFailedJobs(final int page, final int pageSize)
            throws JobQueueDataException {
        return getJobsByState(JobStateQueryParameters.builder()
                .page(page)
                .pageSize(pageSize)
                .orderByColumn(COLUMN_UPDATED_AT)
                .states(JobState.FAILED, JobState.FAILED_PERMANENTLY).build()
        );
    }

    @CloseDBIfOpened
    @Override
    public JobPaginatedResult getAbandonedJobs(final int page, final int pageSize)
            throws JobQueueDataException {
        return getJobsByState(JobStateQueryParameters.builder()
                .page(page)
                .pageSize(pageSize)
                .orderByColumn(COLUMN_UPDATED_AT)
                .states(JobState.ABANDONED, JobState.ABANDONED_PERMANENTLY).build()
        );
    }

    @CloseDBIfOpened
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

            // Remove from job_queue if the job is considered done
            if (job.state() != JobState.PENDING
                    && job.state() != JobState.RUNNING
                    && job.state() != JobState.CANCEL_REQUESTED
                    && job.state() != JobState.CANCELLING) {
                removeJobFromQueue(job.id());
            }

            // Cleanup cache
            CacheLocator.getJobCache().remove(job);
            CacheLocator.getJobCache().removeState(job.id());

        } catch (DotDataException e) {
            Logger.error(this, "Database error while updating job status", e);
            throw new JobQueueDataException("Database error while updating job status", e);
        }
    }

    @CloseDBIfOpened
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

    @CloseDBIfOpened
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

    @CloseDBIfOpened
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

    @CloseDBIfOpened
    @Override
    public Optional<Job> detectAndMarkAbandoned(final Duration threshold, final JobState... inStates)
            throws JobQueueDataException {

        try {

            String parameters = String.join(", ", Collections.nCopies(inStates.length, "?"));

            var query = DETECT_AND_MARK_ABANDONED_WITH_LOCK_QUERY
                    .replace(REPLACE_TOKEN_PARAMETERS, parameters);

            LocalDateTime thresholdTime = LocalDateTime.now().minus(threshold);

            DotConnect dc = new DotConnect();
            dc.setSQL(query);
            for (JobState state : inStates) {
                dc.addParam(state.name());
            }
            dc.addParam(Timestamp.valueOf(thresholdTime));
            dc.addParam(JobState.ABANDONED.name());
            dc.addParam(Timestamp.valueOf(LocalDateTime.now()));

            List<Map<String, Object>> results = dc.loadObjectResults();
            if (!results.isEmpty()) {
                final var foundAbandonedJob = DBJobTransformer.toJob(results.get(0));

                // Create error detail for abandoned job
                final ErrorDetail errorDetail = ErrorDetail.builder()
                        .message("Job abandoned due to no updates within " +
                                threshold.toMinutes() + " minutes")
                        .exceptionClass("com.dotcms.jobs.business.error.JobAbandonedException")
                        .timestamp(LocalDateTime.now())
                        .processingStage("Abandoned Job Detection")
                        .stackTrace("Job exceeded inactivity threshold of " +
                                threshold.toMinutes() + " minutes")
                        .build();
                final JobResult jobResult = JobResult.builder().errorDetail(errorDetail).build();

                final Job abandonedJob = foundAbandonedJob.markAsAbandoned(jobResult);
                updateJobStatus(abandonedJob);

                return Optional.of(abandonedJob);
            }

            return Optional.empty();
        } catch (DotDataException e) {
            final var errorMessage = "Database error while detecting abandoned jobs";
            Logger.error(this, errorMessage, e);
            throw new JobQueueDataException(errorMessage, e);
        }
    }

    @CloseDBIfOpened
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

            // Cleanup cache
            CacheLocator.getJobCache().remove(jobId);

        } catch (DotDataException e) {
            Logger.error(this, "Database error while updating job progress", e);
            throw new JobQueueDataException("Database error while updating job progress", e);
        }
    }

    /**
     * Removes a job from the queue. This method should be used for jobs that have permanently
     * failed and cannot be retried.
     *
     * @param jobId The ID of the job to remove.
     * @throws JobQueueDataException if there's a data storage error while removing the job
     */
    @CloseDBIfOpened
    private void removeJobFromQueue(final String jobId) throws JobQueueDataException {

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

    @CloseDBIfOpened
    @Override
    public boolean hasJobBeenInState(final String jobId, final JobState... states)
            throws JobQueueDataException {

        if (states.length == 0) {
            return false;
        }

        String parameters = String.join(", ", Collections.nCopies(states.length, "?"));

        var query = HAS_JOB_BEEN_IN_STATE_QUERY
                .replace(REPLACE_TOKEN_PARAMETERS, "(" + parameters + ")");

        try {
            DotConnect dc = new DotConnect();
            dc.setSQL(query);
            dc.addParam(jobId);
            for (JobState state : states) {
                dc.addParam(state.name());
            }
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
     * Retrieves a paginated result of jobs filtered by state, queue name, and date range.
     *
     * @param parameters An instance of JobStateQueryParameters containing filter and pagination
     *                   information.
     * @return A JobPaginatedResult containing the jobs that match the specified filters and
     * pagination criteria.
     * @throws JobQueueDataException if there is a data storage error while fetching the jobs.
     */
    @CloseDBIfOpened
    private JobPaginatedResult getJobsByState(final JobStateQueryParameters parameters)
            throws JobQueueDataException {

        if (parameters.queueName().isPresent() && parameters.startDate().isPresent()
                && parameters.endDate().isPresent()) {
            return getJobsFilterByNameDateAndState(parameters);
        } else if (parameters.queueName().isPresent()) {
            return getJobsFilterByNameAndState(parameters);
        }

        return getJobsFilterByState(parameters);
    }

    /**
     * Helper method to fetch jobs by state and return a paginated result.
     *
     * @param parameters An instance of JobStateQueryParameters containing filter and pagination.
     *                   This includes page number, page size, job states, and order by column.
     * @return A JobPaginatedResult instance
     * @throws JobQueueDataException if there's a data storage error while fetching the jobs
     */
    @CloseDBIfOpened
    private JobPaginatedResult getJobsFilterByState(
            final JobStateQueryParameters parameters) throws JobQueueDataException {

        final var states = parameters.states();
        final var page = parameters.page();
        final var pageSize = parameters.pageSize();
        final var orderByColumn = parameters.orderByColumn();

        try {

            String statesParam = String.join(", ", Collections.nCopies(states.length, "?"));

            var query = GET_JOBS_QUERY_BY_STATE
                    .replace(REPLACE_TOKEN_PARAMETERS, "(" + statesParam + ")")
                    .replace(REPLACE_TOKEN_ORDER_BY, orderByColumn);

            DotConnect dc = new DotConnect();
            dc.setSQL(query);
            for (JobState state : states) {
                dc.addParam(state.name());
            }
            for (JobState state : states) {// Repeated for paginated_data CTE
                dc.addParam(state.name());
            }
            dc.addParam(pageSize);
            dc.addParam((page - 1) * pageSize);

            return jobPaginatedResult(page, pageSize, dc);
        } catch (DotDataException e) {
            final var message = "Database error while fetching jobs by state";
            Logger.error(this, message, e);
            throw new JobQueueDataException(message, e);
        }
    }

    /**
     * Retrieves a paginated result of jobs filtered by state and queue name.
     *
     * @param parameters An instance of JobStateQueryParameters containing filter and pagination
     *                   information. This includes queue name, job states, page number, page size
     *                   and order by column.
     * @return A JobPaginatedResult containing the jobs that match the specified filters and
     * pagination criteria.
     * @throws JobQueueDataException if there is a data storage error while fetching the jobs.
     */
    @CloseDBIfOpened
    public JobPaginatedResult getJobsFilterByNameAndState(
            final JobStateQueryParameters parameters) throws JobQueueDataException {

        final var queueName = parameters.queueName().orElseThrow();
        final var states = parameters.states();
        final var page = parameters.page();
        final var pageSize = parameters.pageSize();
        final var orderByColumn = parameters.orderByColumn();

        String statesParam = String.join(", ",
                Collections.nCopies(parameters.states().length, "?"));

        var query = GET_JOBS_QUERY_BY_QUEUE_AND_STATE
                .replace(REPLACE_TOKEN_PARAMETERS, "(" + statesParam + ")")
                .replace(REPLACE_TOKEN_ORDER_BY, orderByColumn);

        try {

            DotConnect dc = new DotConnect();
            dc.setSQL(query);
            dc.addParam(queueName);
            for (JobState state : states) {
                dc.addParam(state.name());
            }
            dc.addParam(queueName);  // Repeated for paginated_data CTE
            for (JobState state : states) {
                dc.addParam(state.name());
            }
            dc.addParam(pageSize);
            dc.addParam((page - 1) * pageSize);

            return jobPaginatedResult(page, pageSize, dc);
        } catch (DotDataException e) {
            Logger.error(this,
                    "Database error while fetching active jobs by queue", e);
            throw new JobQueueDataException(
                    "Database error while fetching active jobs by queue", e);
        }
    }

    /**
     * Retrieves a paginated result of jobs filtered by state, queue name, and date range.
     *
     * @param parameters An instance of JobStateQueryParameters containing filter and pagination
     *                   information. This includes queue name, start and end dates, job states,
     *                   page number, page size, order by column and filter date column.
     * @return A JobPaginatedResult containing the jobs that match the specified filters and
     * pagination criteria.
     * @throws JobQueueDataException if there is a data storage error while fetching the jobs.
     */
    @CloseDBIfOpened
    private JobPaginatedResult getJobsFilterByNameDateAndState(
            final JobStateQueryParameters parameters) throws JobQueueDataException {

        final var queueName = parameters.queueName().orElseThrow();
        final var startDate = parameters.startDate().orElseThrow();
        final var endDate = parameters.endDate().orElseThrow();
        final var states = parameters.states();
        final var page = parameters.page();
        final var pageSize = parameters.pageSize();
        final var orderByColumn = parameters.orderByColumn();
        final var filterDateColumn = parameters.filterDateColumn().orElseThrow();

        String statesParam = String.join(", ",
                Collections.nCopies(parameters.states().length, "?"));

        var query = GET_JOBS_QUERY_BY_QUEUE_AND_STATE_IN_DATE_RANGE
                .replace(REPLACE_TOKEN_PARAMETERS, "(" + statesParam + ")")
                .replace(REPLACE_TOKEN_ORDER_BY, orderByColumn)
                .replace(REPLACE_TOKEN_DATE_COLUMN, filterDateColumn);

        try {
            DotConnect dc = new DotConnect();
            dc.setSQL(query);
            dc.addParam(queueName);
            for (JobState state : states) {
                dc.addParam(state.name());
            }
            dc.addParam(Timestamp.valueOf(startDate));
            dc.addParam(Timestamp.valueOf(endDate));
            dc.addParam(queueName);  // Repeated for paginated_data CTE
            for (JobState state : states) {
                dc.addParam(state.name());
            }
            dc.addParam(Timestamp.valueOf(startDate));
            dc.addParam(Timestamp.valueOf(endDate));
            dc.addParam(pageSize);
            dc.addParam((page - 1) * pageSize);

            return jobPaginatedResult(page, pageSize, dc);
        } catch (DotDataException e) {
            final var message = "Database error while fetching jobs by queue and state";
            Logger.error(this, message, e);
            throw new JobQueueDataException(message, e);
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
    private JobPaginatedResult jobPaginatedResult(
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
