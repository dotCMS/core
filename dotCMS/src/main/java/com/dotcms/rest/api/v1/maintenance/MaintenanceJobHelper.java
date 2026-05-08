package com.dotcms.rest.api.v1.maintenance;

import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.concurrent.lock.ClusterLockManager;
import com.dotcms.jobs.business.api.JobQueueManagerAPI;
import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.job.JobPaginatedResult;
import com.dotcms.rest.api.v1.job.JobResponseUtil;
import com.dotcms.rest.api.v1.job.JobStatusResponse;
import com.dotcms.rest.exception.ConflictException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.liferay.portal.model.User;
import java.util.HashMap;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

/**
 * Creates fix-assets and clean-assets jobs on {@link JobQueueManagerAPI}. A cluster lock
 * around the {@code getActiveJobs} check-and-create gives strict 409 semantics: two nodes
 * cannot both pass the active-job check in the same millisecond.
 *
 * @author hassandotcms
 */
@ApplicationScoped
public class MaintenanceJobHelper {

    public static final String FIX_ASSETS_QUEUE = "maintenanceFixAssets";
    public static final String CLEAN_ASSETS_QUEUE = "maintenanceCleanAssets";

    private static final String JOB_STATUS_PATH = "/api/v1/jobs/%s/status";
    private static final String PARAM_USER_ID = "userId";
    private static final String PARAM_REMOTE_ADDR = "remoteAddr";

    private final JobQueueManagerAPI jobQueueManagerAPI;

    @Inject
    public MaintenanceJobHelper(final JobQueueManagerAPI jobQueueManagerAPI) {
        this.jobQueueManagerAPI = jobQueueManagerAPI;
    }

    /**
     * CDI requires a no-arg constructor for proxy creation.
     */
    public MaintenanceJobHelper() {
        this.jobQueueManagerAPI = null;
    }

    /**
     * Creates a fix-assets job, rejecting with 409 Conflict if one is already running.
     */
    public JobStatusResponse createFixAssetsJob(final User user, final HttpServletRequest request) {
        return createSingletonJob(FIX_ASSETS_QUEUE, user, request, "fix-assets");
    }

    /**
     * Creates a clean-orphan-assets job, rejecting with 409 Conflict if one is already running.
     */
    public JobStatusResponse createCleanAssetsJob(final User user, final HttpServletRequest request) {
        return createSingletonJob(CLEAN_ASSETS_QUEUE, user, request, "clean-assets");
    }

    /**
     * Returns the latest job for a given queue — active if one is pending or running, otherwise
     * the most recent completed job. Returns {@code null} if no job has ever been created.
     */
    public Job getLatestJob(final String queueName) {
        try {
            // getJobs() orders by created_at DESC — the first row is the most recent job
            // across all states (pending, running, completed, failed, canceled).
            final JobPaginatedResult result = jobQueueManagerAPI.getJobs(queueName, 1, 1);
            return result.jobs().isEmpty() ? null : result.jobs().get(0);
        } catch (final DotDataException e) {
            throw new DotRuntimeException("Failed to fetch latest job for queue " + queueName, e);
        }
    }

    /**
     * Acquires a short-lived cluster lock keyed on the queue name, re-checks the queue for any
     * active job, and enqueues a new job atomically. If the lock cannot be acquired (another node
     * is starting a job in this exact moment) or an active job exists, throws
     * {@link ConflictException}.
     */
    private JobStatusResponse createSingletonJob(
            final String queueName,
            final User user,
            final HttpServletRequest request,
            final String humanName) {

        final ClusterLockManager<String> lock =
                DotConcurrentFactory.getInstance().getClusterLockManager(queueName);

        final JobStatusResponse result;
        try {
            result = lock.tryClusterLock(() ->
                    checkAndEnqueue(queueName, user, request, humanName));
        } catch (final ConflictException e) {
            throw e;
        } catch (final Throwable t) {
            throw new DotRuntimeException("Failed to create " + humanName + " job", t);
        }

        if (result == null) {
            throw new ConflictException(String.format(
                    "Another node is currently starting a %s job; retry in a moment",
                    humanName));
        }
        return result;
    }

    private JobStatusResponse checkAndEnqueue(
            final String queueName,
            final User user,
            final HttpServletRequest request,
            final String humanName) throws DotDataException {

        final JobPaginatedResult active = jobQueueManagerAPI.getActiveJobs(queueName, 1, 1);
        if (!active.jobs().isEmpty()) {
            throw new ConflictException(String.format(
                    "A %s job is already running (jobId=%s)",
                    humanName, active.jobs().get(0).id()));
        }

        final Map<String, Object> params = new HashMap<>();
        params.put(PARAM_USER_ID, user.getUserId());
        params.put(PARAM_REMOTE_ADDR, request.getRemoteAddr());

        final String jobId = jobQueueManagerAPI.createJob(queueName, params);
        return JobResponseUtil.buildJobStatusResponse(jobId, JOB_STATUS_PATH, request);
    }
}
