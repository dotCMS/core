package com.dotcms.rest.api.v1.content.bulkrefresh;

import com.dotcms.jobs.business.api.JobQueueManagerAPI;
import com.dotcms.jobs.business.processor.impl.BulkRefreshContentletsProcessor;
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


    private final JobQueueManagerAPI jobQueueManagerAPI;

    /**
     * Required for CDI proxying of this normal-scoped bean, and identical to the one on
     * {@code ContentImportHelper} (:93) that this class follows.
     * <p>
     * It does hand out an instance whose API reference is null, which would NPE if anything called it
     * directly — nothing does, and Weld builds client proxies without invoking a constructor. Kept
     * rather than removed because dropping it risks an unproxyable-bean deployment failure for a
     * cosmetic gain.
     */
    public BulkRefreshHelper() {
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
     * @return the accepted job's handle. Completion is pushed, not fetched.
     * @throws DotSecurityException if the user is neither a CMS Power User nor a CMS Administrator
     * @throws IllegalArgumentException if the selection is empty or over the configured cap
     */
    public BulkRefreshSubmitResponse submit(final BulkRefreshForm form, final User user)
            throws DotDataException, DotSecurityException {

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

        return BulkRefreshSubmitResponse.builder()
                .jobId(jobId)
                .submitted(submitted)
                .build();
    }

    /**
     * Whether the user may reindex content.
     * <p>
     * Matches the legacy gate in {@code view_contentlets.jsp:209} so nobody who could press the old
     * Refresh button loses access, and nobody who could not gains it. "Always available" in the
     * ticket means <i>not gated by content state</i> — that still holds; this is a role gate, and
     * reindexing is expensive enough to want one.
     * <p>
     * <b>In practice this resolves to CMS Administrator alone.</b> {@code Role.CMS_POWER_USER} is the
     * role <i>key</i> {@code "CMS Power User"}, and no such key ships in the starter data, so
     * {@code loadRoleByKey} answers null and {@code doesUserHaveRole(user, null)} is silently false.
     * Legacy makes the identical call, so this is faithful rather than a regression — but it was
     * failing silently, which is why the miss is now logged. Granting real Power Users access means
     * resolving the role by name, and that widens the gate beyond what legacy allowed: a product
     * decision, not something to change while matching legacy.
     * <p>
     * This gates <i>submission</i> only. It does not protect a submitted job's contents: the
     * generic {@code GET /api/v1/jobs/{jobId}/status} requires only a backend user and returns the
     * whole job, parameters included — a pre-existing exposure this feature neither creates nor
     * closes.
     *
     * @param user the user to check
     * @return true for a CMS Power User or a CMS Administrator
     */
    public boolean canRefresh(final User user) throws DotDataException {

        final RoleAPI roleAPI = APILocator.getRoleAPI();

        final Role powerUser = roleAPI.loadRoleByKey(Role.CMS_POWER_USER);
        if (null == powerUser) {
            Logger.warn(this, String.format(
                    "No role found for key [%s], so the reindex gate is CMS Administrator only. "
                            + "This matches the legacy Refresh button, which resolves the same key.",
                    Role.CMS_POWER_USER));
        }

        return roleAPI.doesUserHaveRole(user, powerUser)
                || roleAPI.doesUserHaveRole(user, roleAPI.loadCMSAdminRole());
    }

    /**
     * The effective per-submission cap.
     */
    public int maxItems() {
        return Config.getIntProperty(MAX_ITEMS_CONFIG_PROPERTY, MAX_ITEMS_DEFAULT);
    }

}
