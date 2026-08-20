package com.dotcms.rest.api.v1.content.bulkrefresh;

import com.dotcms.jobs.business.api.JobQueueManagerAPI;
import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.job.JobView;
import com.dotcms.jobs.business.processor.impl.BulkRefreshContentletsProcessor;
import com.dotcms.rest.api.v1.job.JobResponseUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import java.util.HashMap;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

/**
 * Job creation, authorization and validation for bulk refresh.
 * <p>
 * Kept out of {@link BulkRefreshResource} so the resource stays a thin HTTP layer, following the
 * {@code ContentImportResource} / {@code ContentImportHelper} split.
 *
 * @author dotCMS
 */
@ApplicationScoped
public class BulkRefreshHelper {

    /** Queue name; must match {@code @Queue} on the processor. */
    public static final String BULK_REFRESH_QUEUE_NAME = "bulkRefreshContentlets";

    /** Ceiling on inodes per submission. Synchronous indexing makes an unbounded batch expensive. */
    public static final String MAX_ITEMS_CONFIG_PROPERTY = "CONTENT_BULK_REFRESH_MAX_ITEMS";

    public static final int MAX_ITEMS_DEFAULT = 500;

    private static final String STATUS_ENDPOINT = "/api/v1/content/_bulkrefresh/%s";

    private final JobQueueManagerAPI jobQueueManagerAPI;

    public BulkRefreshHelper() {
        // Default constructor required for CDI
        this.jobQueueManagerAPI = null;
    }

    @Inject
    public BulkRefreshHelper(final JobQueueManagerAPI jobQueueManagerAPI) {
        this.jobQueueManagerAPI = jobQueueManagerAPI;
    }

    /**
     * Authorizes the user, validates the form and enqueues the job.
     *
     * @param form    the submitted selection and flags
     * @param user    the submitting backend user
     * @param request used to build the absolute status URL
     * @return the job handle plus the URLs a client needs to follow it
     * @throws DotSecurityException if the user is neither a CMS Power User nor a CMS Administrator
     * @throws IllegalArgumentException if the selection is empty or over the configured cap
     */
    public BulkRefreshSubmitResponse submit(final BulkRefreshForm form, final User user,
            final HttpServletRequest request) throws DotDataException, DotSecurityException {

        if (!canRefresh(user)) {
            throw new DotSecurityException(String.format(
                    "User [%s] must be a CMS Power User or a CMS Administrator to reindex content",
                    user.getUserId()));
        }

        final int submitted = form.getContentletIds().size();
        final int maxItems = maxItems();
        if (submitted > maxItems) {
            throw new IllegalArgumentException(String.format(
                    "A bulk refresh accepts at most %d items; %d were submitted",
                    maxItems, submitted));
        }

        final Map<String, Object> jobParameters = new HashMap<>();
        jobParameters.put(BulkRefreshContentletsProcessor.PARAM_CONTENTLET_IDS,
                form.getContentletIds());
        jobParameters.put(BulkRefreshContentletsProcessor.PARAM_INCLUDE_DEPENDENCIES,
                form.isIncludeDependencies());
        jobParameters.put(BulkRefreshContentletsProcessor.PARAM_INCLUDE_ITEM_RESULTS,
                form.isIncludeItemResults());
        jobParameters.put(BulkRefreshContentletsProcessor.PARAM_USER_ID, user.getUserId());

        final String jobId = this.jobQueueManagerAPI.createJob(
                BULK_REFRESH_QUEUE_NAME, jobParameters);

        Logger.info(this, String.format(
                "Bulk refresh job [%s] created by user [%s] for %d inode(s)",
                jobId, user.getUserId(), submitted));

        // Reuses the job status URL builder so the base URL is derived the same way everywhere.
        final String statusUrl = JobResponseUtil
                .buildJobStatusResponse(jobId, STATUS_ENDPOINT, request).statusUrl();

        return BulkRefreshSubmitResponse.builder()
                .jobId(jobId)
                .statusUrl(statusUrl)
                .submitted(submitted)
                .build();
    }

    /**
     * Whether the user may reindex content.
     * <p>
     * Matches the legacy gate in {@code view_contentlets.jsp} so nobody who could press the old
     * Refresh button loses access, and nobody who could not gains it. "Always available" in the
     * ticket means <i>not gated by content state</i> — that still holds; this is a role gate, and
     * reindexing is expensive enough to want one.
     *
     * @param user the user to check
     * @return true for a CMS Power User or a CMS Administrator
     */
    public boolean canRefresh(final User user) throws DotDataException {

        final RoleAPI roleAPI = APILocator.getRoleAPI();
        return roleAPI.doesUserHaveRole(user, roleAPI.loadRoleByKey(Role.CMS_POWER_USER))
                || roleAPI.doesUserHaveRole(user, roleAPI.loadCMSAdminRole());
    }

    /**
     * The effective per-submission cap.
     */
    public int maxItems() {
        return Config.getIntProperty(MAX_ITEMS_CONFIG_PROPERTY, MAX_ITEMS_DEFAULT);
    }

    /**
     * The job, for the status snapshot and the polling fallback.
     *
     * @throws com.dotmarketing.exception.DoesNotExistException if no such job exists
     */
    public Job getJob(final String jobId) throws DotDataException {
        return this.jobQueueManagerAPI.getJob(jobId);
    }

    /**
     * Requests cancellation. Stops at the next item boundary: already-indexed identifiers stay
     * indexed and the remainder are counted skipped, so no item is left reported as pending.
     */
    public void cancelJob(final String jobId) throws DotDataException {
        this.jobQueueManagerAPI.cancelJob(jobId);
    }

    /**
     * Renders a job for the REST layer, carrying the processor's result metadata in
     * {@code result.metadata} once the run is terminal.
     */
    public JobView view(final Job job) {
        return JobView.builder().from(job).build();
    }
}
