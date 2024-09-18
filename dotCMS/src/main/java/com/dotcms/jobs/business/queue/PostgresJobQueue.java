package com.dotcms.jobs.business.queue;

import com.dotcms.jobs.business.error.ErrorDetail;
import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.job.JobResult;
import com.dotcms.jobs.business.job.JobState;
import com.dotcms.jobs.business.queue.error.JobLockingException;
import com.dotcms.jobs.business.queue.error.JobNotFoundException;
import com.dotcms.jobs.business.queue.error.JobQueueDataException;
import com.dotcms.jobs.business.queue.error.JobQueueException;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
 * job_detail_history) as defined in the database schema. Ensure that these tables are properly
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
            + "(id, queue_name, state, parameters, created_at, updated_at) "
            + "VALUES (?, ?, ?, ?::jsonb, ?, ?)";

    private static final String SELECT_JOB_BY_ID_QUERY = "SELECT * FROM job WHERE id = ?";

    private static final String GET_ACTIVE_JOBS_QUERY = "SELECT * FROM job WHERE queue_name = ? "
            + "AND state IN (?, ?) ORDER BY created_at LIMIT ? OFFSET ?";

    private static final String UPDATE_AND_GET_NEXT_JOB_WITH_LOCK_QUERY =
            "UPDATE job_queue SET state = ? "
                    + "WHERE id = (SELECT id FROM job_queue WHERE state = ? "
                    + "ORDER BY priority DESC, created_at ASC LIMIT 1 FOR UPDATE SKIP LOCKED) "
                    + "RETURNING *";

    private static final String GET_COMPLETED_JOBS_QUERY = "SELECT * FROM job "
            + "WHERE queue_name = ? AND state = ? AND completed_at BETWEEN ? AND ? "
            + "ORDER BY completed_at DESC LIMIT ? OFFSET ?";

    private static final String GET_JOBS_QUERY = "SELECT * FROM job ORDER BY created_at "
            + "DESC LIMIT ? OFFSET ?";

    private static final String GET_UPDATED_JOBS_SINCE_QUERY = "SELECT * FROM job "
            + "WHERE id = ANY(?) AND updated_at > ?";

    private static final String GET_FAILED_JOBS_QUERY = "SELECT * FROM job WHERE state = ? "
            + "ORDER BY updated_at DESC LIMIT ? OFFSET ?";

    private static final String UPDATE_JOBS_QUERY = "UPDATE job SET state = ?, progress = ?, "
            + "updated_at = ?, started_at = ?, completed_at = ?, execution_node = ?, retry_count = ?, "
            + "result = ?::jsonb WHERE id = ?";

    private static final String INSERT_INTO_JOB_DETAIL_HISTORY_QUERY =
            "INSERT INTO job_detail_history "
                    + "(id, job_id, state, progress, execution_node, created_at, result) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?::jsonb)";

    private static final String DELETE_JOB_FROM_QUEUE_QUERY = "DELETE FROM job_queue WHERE id = ?";

    private static final String PUT_JOB_BACK_TO_QUEUE_QUERY = "INSERT INTO job_queue "
            + "(id, queue_name, state, priority, created_at) "
            + "VALUES (?, ?, ?, ?, ?) ON CONFLICT (id) DO UPDATE SET state = ?, priority = ?";

    private static final String UPDATE_JOB_PROGRESS = "UPDATE job SET progress = ?, updated_at = ?"
            + " WHERE id = ?";

    @Override
    public String createJob(final String queueName, final Map<String, Object> parameters)
            throws JobQueueException {

        try {

            final ObjectMapper objectMapper = new ObjectMapper();

            final String jobId = UUID.randomUUID().toString();

            final var parametersJson = objectMapper.writeValueAsString(parameters);
            final var now = Timestamp.valueOf(LocalDateTime.now());
            final var jobState = JobState.PENDING.name();

            // Insert into job table
            new DotConnect().setSQL(CREATE_JOB_QUERY)
                    .addParam(jobId)
                    .addParam(queueName)
                    .addParam(jobState)
                    .addParam(parametersJson)
                    .addParam(now)
                    .addParam(now)
                    .loadResult();

            // Creating the jobqueue entry as well
            new DotConnect().setSQL(CREATE_JOB_QUEUE_QUERY)
                    .addParam(jobId)
                    .addParam(queueName)
                    .addParam(jobState)
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
                return mapResultSetToJob(results.get(0));
            }

            Logger.warn(this, "Job with id: " + jobId + " not found");
            throw new JobNotFoundException(jobId);
        } catch (DotDataException e) {
            Logger.error(this, "Database error while fetching job", e);
            throw new JobQueueDataException("Database error while fetching job", e);
        }
    }

    @Override
    public List<Job> getActiveJobs(final String queueName, final int page, final int pageSize)
            throws JobQueueDataException {

        try {
            DotConnect dc = new DotConnect();
            dc.setSQL(GET_ACTIVE_JOBS_QUERY);
            dc.addParam(queueName);
            dc.addParam(JobState.PENDING.name());
            dc.addParam(JobState.RUNNING.name());
            dc.addParam(pageSize);
            dc.addParam((page - 1) * pageSize);

            List<Map<String, Object>> results = dc.loadObjectResults();
            return results.stream().map(this::mapResultSetToJob).collect(Collectors.toList());
        } catch (DotDataException e) {
            Logger.error(this, "Database error while fetching active jobs", e);
            throw new JobQueueDataException("Database error while fetching active jobs", e);
        }
    }

    @Override
    public List<Job> getCompletedJobs(final String queueName, final LocalDateTime startDate,
            final LocalDateTime endDate, final int page, final int pageSize)
            throws JobQueueDataException {

        try {
            DotConnect dc = new DotConnect();
            dc.setSQL(GET_COMPLETED_JOBS_QUERY);
            dc.addParam(queueName);
            dc.addParam(JobState.COMPLETED.name());
            dc.addParam(Timestamp.valueOf(startDate));
            dc.addParam(Timestamp.valueOf(endDate));
            dc.addParam(pageSize);
            dc.addParam((page - 1) * pageSize);

            List<Map<String, Object>> results = dc.loadObjectResults();
            return results.stream().map(this::mapResultSetToJob).collect(Collectors.toList());
        } catch (DotDataException e) {
            Logger.error(this, "Database error while fetching completed jobs", e);
            throw new JobQueueDataException("Database error while fetching completed jobs", e);
        }
    }

    @Override
    public List<Job> getJobs(final int page, final int pageSize) throws JobQueueDataException {

        try {
            DotConnect dc = new DotConnect();
            dc.setSQL(GET_JOBS_QUERY);
            dc.addParam(pageSize);
            dc.addParam((page - 1) * pageSize);

            List<Map<String, Object>> results = dc.loadObjectResults();
            return results.stream().map(this::mapResultSetToJob).collect(Collectors.toList());
        } catch (DotDataException e) {
            Logger.error(this, "Database error while fetching jobs", e);
            throw new JobQueueDataException("Database error while fetching jobs", e);
        }
    }

    @Override
    public List<Job> getFailedJobs(final int page, final int pageSize)
            throws JobQueueDataException {

        try {
            DotConnect dc = new DotConnect();
            dc.setSQL(GET_FAILED_JOBS_QUERY);
            dc.addParam(JobState.FAILED.name());
            dc.addParam(pageSize);
            dc.addParam((page - 1) * pageSize);

            List<Map<String, Object>> results = dc.loadObjectResults();
            return results.stream().map(this::mapResultSetToJob).collect(Collectors.toList());
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
                    return new ObjectMapper().writeValueAsString(r);
                } catch (Exception e) {
                    Logger.error(this, "Failed to serialize job result", e);
                    return null;
                }
            }).orElse(null));
            dc.addParam(job.id());
            dc.loadResult();

            // Update job_detail_history
            DotConnect historyDc = new DotConnect();
            historyDc.setSQL(INSERT_INTO_JOB_DETAIL_HISTORY_QUERY);
            historyDc.addParam(UUID.randomUUID().toString());
            historyDc.addParam(job.id());
            historyDc.addParam(job.state().name());
            historyDc.addParam(job.progress());
            historyDc.addParam(serverId);
            historyDc.addParam(Timestamp.valueOf(LocalDateTime.now()));
            historyDc.addParam(job.result().map(r -> {
                try {
                    return new ObjectMapper().writeValueAsString(r);
                } catch (Exception e) {
                    Logger.error(this, "Failed to serialize job result for history", e);
                    return null;
                }
            }).orElse(null));
            historyDc.loadResult();

            // Remove from job_queue if completed, failed, or cancelled
            if (job.state() == JobState.COMPLETED
                    || job.state() == JobState.FAILED
                    || job.state() == JobState.CANCELLED) {
                removeJob(job.id());
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
            return results.stream().map(this::mapResultSetToJob).collect(Collectors.toList());
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
                final var job = getJob(jobId);

                // Update the job status to RUNNING
                Job updatedJob = Job.builder()
                        .from(job)
                        .state(JobState.RUNNING)
                        .startedAt(Optional.of(LocalDateTime.now()))
                        .build();

                updateJobStatus(updatedJob);
                return updatedJob;
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
            dc.setSQL(UPDATE_JOB_PROGRESS);
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
    public void removeJob(final String jobId) throws JobQueueDataException {

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

    /**
     * Maps a result set to a Job object.
     *
     * @param result The result set to map.
     * @return A Job object mapped from the result set.
     */
    private Job mapResultSetToJob(final Map<String, Object> result) {

        final ObjectMapper objectMapper = new ObjectMapper();

        try {
            return Job.builder()
                    .id((String) result.get("id"))
                    .queueName((String) result.get("queue_name"))
                    .state(JobState.valueOf((String) result.get("state")))
                    .parameters(
                            objectMapper.readValue((String) result.get("parameters"), Map.class))
                    .result(Optional.ofNullable((Map<String, Object>) result.get("result"))
                            .map(r -> {
                                try {
                                    return JobResult.builder()
                                            .errorDetail(Optional.ofNullable(
                                                            (Map<String, Object>) r.get("errorDetail"))
                                                    .map(ed -> ErrorDetail.builder()
                                                            .message((String) ed.get("message"))
                                                            .exceptionClass((String) ed.get(
                                                                    "exceptionClass"))
                                                            .timestamp(toLocalDateTime(
                                                                    ed.get("timestamp")))
                                                            .processingStage((String) ed.get(
                                                                    "processingStage"))
                                                            .build())
                                            )
                                            .metadata(Optional.ofNullable(
                                                    (Map<String, Object>) r.get("metadata")))
                                            .build();
                                } catch (Exception e) {
                                    Logger.error(this, "Failed to map job result", e);
                                    return null;
                                }
                            }))
                    .progress(((Number) result.get("progress")).floatValue())
                    .createdAt(toLocalDateTime(result.get("created_at")))
                    .updatedAt(toLocalDateTime(result.get("updated_at")))
                    .startedAt(Optional.ofNullable(result.get("started_at"))
                            .map(this::toLocalDateTime))
                    .completedAt(Optional.ofNullable(result.get("completed_at"))
                            .map(this::toLocalDateTime))
                    .executionNode(Optional.ofNullable((String) result.get("execution_node")))
                    .retryCount(((Number) result.get("retry_count")).intValue())
                    .build();
        } catch (Exception e) {
            Logger.error(this, "Failed to map result to Job", e);
            throw new DotRuntimeException("Failed to map result to Job", e);
        }
    }

    /**
     * Converts a timestamp object to a LocalDateTime.
     *
     * @param timestamp The timestamp object to convert.
     * @return The converted LocalDateTime.
     */
    private LocalDateTime toLocalDateTime(final Object timestamp) {

        if (timestamp instanceof java.sql.Timestamp) {
            return ((java.sql.Timestamp) timestamp).toLocalDateTime();
        } else if (timestamp instanceof java.util.Date) {
            return ((java.util.Date) timestamp).toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
        }

        throw new IllegalArgumentException("Unsupported timestamp type: " + timestamp.getClass());
    }

}
