package com.dotcms.rest.api.v1.asset.bulkdelete;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.jobs.business.api.JobQueueManagerAPI;
import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.job.JobPaginatedResult;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;

/**
 * Validates a bulk-delete submission and enqueues the run #37063.
 * <p>
 * <b>What is decided here versus in the run.</b> Only what makes a submission unusable —
 * empty, over the configured maximum — is refused here. Per-path permission and resolution failures
 * are the run's own business: a submission is never refused because one of its paths turns out to
 * be bad, only because the submission as a whole cannot be acted on.
 *
 * @author dotCMS
 */
@ApplicationScoped
public class FolderBulkDeleteHelper {

    /**
     * The queue this feature's runs are submitted to.
     */
    public static final String QUEUE_NAME = "folderBulkDelete";

    public static final String MAX_PATHS_KEY = "FOLDER_BULK_DELETE_MAX_PATHS";
    public static final int DEFAULT_MAX_PATHS = 50;

    /**
     * How many active jobs {@link #checkForOverlap} reads per page.
     */
    private static final int ACTIVE_JOBS_PAGE_SIZE = 50;

    private final JobQueueManagerAPI jobQueueManagerAPI;

    /**
     * Required by CDI, never called by this code.
     * <p>
     * {@code @ApplicationScoped} needs a no-args constructor for Weld to build a client proxy —
     * without one the container fails validation at deployment (WELD-001435) and dotCMS does not
     * start at all. {@code BulkUploadHelper} carries the same constructor for the same reason.
     */
    public FolderBulkDeleteHelper() {
        this.jobQueueManagerAPI = null;
    }

    @Inject
    public FolderBulkDeleteHelper(final JobQueueManagerAPI jobQueueManagerAPI) {
        this.jobQueueManagerAPI = jobQueueManagerAPI;
    }

    /**
     * Validates the submission and enqueues the run.
     *
     * @param form the submitted paths
     * @param user the submitting author; the run executes with their permissions and the completion
     *             is addressed to them
     * @return the run's handle. No folder has been deleted yet.
     * @throws FolderBulkDeleteRefusedException the submission itself is unusable — {@code 400} — or
     *                                          it overlaps an in-flight run's paths — {@code 409}
     *                                          (FR-029)
     */
    @WrapInTransaction
    public FolderBulkDeleteSubmitResponse submit(final FolderBulkDeleteForm form, final User user)
            throws DotDataException {

        if (form.assetPaths().isEmpty()) {
            throw new FolderBulkDeleteRefusedException(
                    "EMPTY_SELECTION", "assetPaths", Response.Status.BAD_REQUEST,
                    "no folder paths were submitted");
        }

        // Collapsed before the run, so the outcome reports each distinct path once. Keyed by
        // comparisonKey: "//host/a", "//host/a/" and "//host/A/" are the same folder, since folder
        // resolution ignores case. The first spelling is kept as sent (trailing slash added): it is
        // what the result key and the folder events carry back to the author. LinkedHashMap keeps
        // submission order.
        final Map<String, String> firstSpellingByKey = new LinkedHashMap<>();
        for (final String path : form.assetPaths()) {
            firstSpellingByKey.putIfAbsent(comparisonKey(path), normalize(path));
        }
        final Set<String> distinctPaths = new LinkedHashSet<>(firstSpellingByKey.values());

        final int maxPaths = Config.getIntProperty(MAX_PATHS_KEY, DEFAULT_MAX_PATHS);
        if (distinctPaths.size() > maxPaths) {
            throw new FolderBulkDeleteRefusedException(
                    "OVER_MAX_PATHS", "assetPaths", Response.Status.BAD_REQUEST,
                    String.format("selection exceeds the maximum of %d paths", maxPaths));
        }

        // FR-029b: a transaction-scoped advisory lock per site this submission touches, held for
        // the rest of this method — including the createJob call below, which joins this same
        // transaction (@WrapInTransaction on PostgresJobQueue#createJob reuses the connection
        // already open here rather than starting a second one). Two submissions for the same site
        // fully serialize through the overlap check; a submission for an unrelated site is
        // unaffected. A database lock rather than an in-memory one, so it also holds across
        // cluster nodes.
        acquireSiteLocks(distinctPaths);
        checkForOverlap(distinctPaths);

        final String jobId = jobQueueManagerAPI.createJob(QUEUE_NAME,
                jobParameters(distinctPaths, user));

        Logger.info(this, String.format(
                "Bulk folder delete job [%s] created by user [%s] for %d path(s)",
                jobId, user.getUserId(), distinctPaths.size()));

        return FolderBulkDeleteSubmitResponse.builder()
                .jobId(jobId)
                .statusUrl("/api/v1/jobs/" + jobId + "/status")
                .submitted(distinctPaths.size())
                .build();
    }

    /**
     * Refuses the submission if any of its paths is the same as, an ancestor of, or a descendant of
     * a path an in-flight run in this queue is already covering (FR-029, FR-029a).
     * <p>
     * <b>"Active" is deliberately conservative</b>: every non-terminal state
     * {@link JobQueueManagerAPI#getActiveJobs} returns, including a run that has failed or been
     * abandoned but not yet reached its permanent state. A spurious refusal here is far cheaper
     * than a missed race.
     * <p>
     * <b>Unlike the processor's own dedup logic, a site root is not special-cased here.</b>
     * {@code FolderBulkDeleteProcessor} excludes a site root from being a "covering ancestor"
     * because deleting it never actually happens (it is always refused as
     * {@code PROTECTED_FOLDER}), so it never removes a descendant as a side effect. That reasoning
     * does not apply to this guard: refusing a submission that overlaps a site root some other run
     * is working under is an over-cautious refusal, not a correctness bug — exactly the
     * conservative bias this check is built on.
     * <p>
     * Every page is iterated, not only the first — a run sitting past page one must still block.
     */
    private void checkForOverlap(final Set<String> distinctPaths) throws DotDataException {

        int page = 1;
        while (true) {
            final JobPaginatedResult activeJobs =
                    jobQueueManagerAPI.getActiveJobs(QUEUE_NAME, page, ACTIVE_JOBS_PAGE_SIZE);

            for (final Job activeJob : activeJobs.jobs()) {
                for (final String activePath : pathsOf(activeJob.parameters())) {
                    for (final String submittedPath : distinctPaths) {
                        if (overlaps(submittedPath, activePath)) {
                            throw new FolderBulkDeleteRefusedException(
                                    "OVERLAPPING_RUN", "assetPaths", Response.Status.CONFLICT,
                                    String.format(
                                            "another deletion is already running for %s",
                                            submittedPath));
                        }
                    }
                }
            }

            if (activeJobs.jobs().size() < ACTIVE_JOBS_PAGE_SIZE) {
                break;
            }
            page++;
        }
    }

    /**
     * Same path, an ancestor, or a descendant — a bidirectional prefix check on
     * {@link #comparisonKey}, so case differences never hide an overlap.
     */
    private static boolean overlaps(final String pathA, final String pathB) {
        final String keyA = comparisonKey(pathA);
        final String keyB = comparisonKey(pathB);
        return keyA.startsWith(keyB) || keyB.startsWith(keyA);
    }

    private static String normalize(final String path) {
        return path.endsWith("/") ? path : path + "/";
    }

    /**
     * The form two paths are compared in: normalized and lowercased, the same way
     * {@code FolderFactoryImpl} and {@code AssetPathResolver} lowercase a path before resolving
     * it. Only for comparing — what is stored and reported stays as the author sent it.
     */
    private static String comparisonKey(final String path) {
        return normalize(path).toLowerCase();
    }

    /**
     * Reads each submitted path's original {@code paths[].path} entries — the same shape
     * {@link com.dotcms.jobs.business.processor.impl.FolderBulkDeleteProcessor} reads back from a
     * job's own parameters — so the overlap check compares against exactly what an active run is
     * covering, not a re-derived guess.
     */
    public static List<String> pathsOf(final Map<String, Object> parameters) {
        final Object raw = parameters.get("paths");
        if (!(raw instanceof Collection)) {
            return List.of();
        }
        final List<String> paths = new ArrayList<>();
        for (final Object entry : (Collection<?>) raw) {
            if (entry instanceof Map) {
                paths.add(String.valueOf(((Map<?, ?>) entry).get("path")));
            }
        }
        return paths;
    }

    /**
     * Acquires a transaction-scoped Postgres advisory lock for every distinct site this submission
     * touches, in a fixed (sorted) order — so two submissions naming the same two sites in
     * different order cannot deadlock against each other.
     */
    private void acquireSiteLocks(final Set<String> distinctPaths) throws DotDataException {

        final SortedSet<String> siteIdentifiers = new TreeSet<>();
        for (final String path : distinctPaths) {
            siteIdentifiers.add(hostnameOf(path));
        }

        for (final String siteIdentifier : siteIdentifiers) {
            // nosemgrep: gitlab.find_sec_bugs.CUSTOM_INJECTION-2 -- static SQL, the only runtime
            // value is bound via addParam
            new DotConnect().setSQL("SELECT pg_advisory_xact_lock(hashtext(?))")
                    .addParam(siteIdentifier)
                    .loadObjectResults();
        }
    }

    /**
     * The hostname segment of a site-qualified path — {@code //hostname/segment/.../} —
     * lowercased, since site resolution ignores case: {@code Demo.dotcms.com} and
     * {@code demo.dotcms.com} must take the same advisory lock.
     */
    private static String hostnameOf(final String path) {
        final String withoutLeadingSlashes = path.replaceFirst("^/+", "").toLowerCase();
        final int nextSlash = withoutLeadingSlashes.indexOf('/');
        return nextSlash == -1 ? withoutLeadingSlashes
                : withoutLeadingSlashes.substring(0, nextSlash);
    }

    private Map<String, Object> jobParameters(final Set<String> distinctPaths, final User user) {

        final List<Map<String, Object>> paths = new ArrayList<>(distinctPaths.size());
        for (final String path : distinctPaths) {
            final Map<String, Object> p = new HashMap<>();
            p.put("path", path);
            paths.add(p);
        }

        final Map<String, Object> parameters = new HashMap<>();
        parameters.put("userId", user.getUserId());
        parameters.put("paths", paths);
        return parameters;
    }
}
