package com.dotcms.jobs.business.processor.impl;

import com.dotcms.api.system.event.Payload;
import com.dotcms.api.system.event.SystemEventType;
import com.dotcms.api.system.event.SystemEventsAPI;
import com.dotcms.api.system.event.Visibility;
import com.dotcms.api.system.event.VisibilityRoles;
import com.dotcms.api.system.event.verifier.ExcludeOwnerVerifierBean;
import com.dotcms.jobs.business.batch.BatchFailureReason;
import com.dotcms.jobs.business.batch.BatchItemResult;
import com.dotcms.jobs.business.batch.BatchItemStatus;
import com.dotcms.jobs.business.error.JobCancellationException;
import com.dotcms.jobs.business.error.JobProcessingException;
import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.job.JobState;
import com.dotcms.jobs.business.processor.Cancellable;
import com.dotcms.jobs.business.processor.JobProcessor;
import com.dotcms.jobs.business.processor.ProgressTracker;
import com.dotcms.jobs.business.processor.Queue;
import com.dotcms.rest.api.v1.asset.WebAssetHelper;
import com.dotcms.rest.api.v1.asset.bulkdelete.FolderBulkDeleteHelper;
import com.dotcms.rest.api.v1.asset.view.FolderView;
import com.dotcms.rest.api.v1.asset.view.WebAssetView;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.DotLockException;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.enterprise.context.Dependent;

/**
 * Deletes a selection of folders, one top-level folder at a time (#37063).
 * <p>
 * Each surviving path is deleted by calling the <b>unchanged</b> recursive delete the shipped
 * single-folder endpoint already uses ({@link FolderAPI#delete(Folder, User, boolean)}) — this
 * feature does not bound, chunk or otherwise change that call. A failure on one path never aborts
 * the run (FR-008); the per-path outcome reuses the shared batch contract (D-002, D-012, FR-015).
 * <p>
 * <b>Classification is fact-based, never derived from exception message text</b> (FR-018):
 * {@link BatchFailureReason#PATH_NOT_FOUND} and {@link BatchFailureReason#PROTECTED_FOLDER} are
 * decided from the resolution result itself, before any delete is attempted;
 * {@link BatchFailureReason#PERMISSION_DENIED} and {@link BatchFailureReason#IN_USE} are decided
 * from the exception *type* {@code FolderAPI.delete} throws, never from its message.
 * {@link BatchFailureReason#COVERED_BY_PARENT} is decided up front, from the submitted paths alone,
 * before any resolution happens for the covered path at all (FR-013).
 * <p>
 * Cancellable between top-level folders only, never mid-subtree (FR-026): the cancellation flag is
 * checked once per loop iteration, before a path is either dedup-skipped or handed to
 * {@link #deleteOne}, so whichever folder is already inside {@code FolderAPI.delete} when
 * cancellation lands always finishes normally — that is what makes FR-023's "fully deleted or fully
 * untouched, never partial" guarantee free, rather than something this class has to engineer. Paths
 * not reached are recorded {@code SKIPPED} with no reason (FR-027), and the first of them is kept
 * as {@code stoppedAt} so the outcome says where the run stopped (FR-028).
 * <p>
 * <b>Per-folder announcements</b> (FR-035a, FR-035b): {@code FOLDER_DELETE_STARTED} immediately
 * before, {@code FOLDER_DELETE_FINISHED} immediately after each top-level delete attempt —
 * regardless of whether that attempt succeeded or was caught as a classified failure, since an
 * author working inside the folder needs to know it is no longer busy either way. Distinct from the
 * existing {@code DELETE_FOLDER} event (which only fires on success) and from
 * {@code BULK_FOLDER_DELETE_COMPLETED} (submitter-only, carries the run's outcome, not "is this
 * folder busy") — see research.md R4 for why reusing {@code DELETE_FOLDER} was tried and wrong.
 * Audience ({@code PermissionAPI.getRolesWithPermission(folder, PERMISSION_READ)}) is computed once
 * per folder, before the delete attempt, and reused for both pushes.
 * <p>
 * The liveness heartbeat and the re-queued-run signal are added in later Polish tasks — see the
 * task list for where each lands. A per-folder failure is reported, not retried on the *item* level
 * — the author decides — but see the next paragraph for why that is not the same as
 * {@code @NoRetryPolicy} on the class.
 * <p>
 * <b>Not marked {@code @NoRetryPolicy}, deliberately</b> — the same correction
 * {@code BulkUploadProcessor}'s own javadoc already recorded, and FR-030a's own "Decided" text
 * assumed away before this was checked against the actual framework: the abandonment sweep's
 * re-queue does <b>not</b> bypass the retry policy. {@code JobQueueManagerAPIImpl#canRetry}
 * consults the same {@code @NoRetryPolicy}/retry-count gate for a job coming back from
 * {@code ABANDONED} as it does for one coming back from a thrown exception, so a
 * {@code @NoRetryPolicy} processor can never actually recover from abandonment — it is routed
 * straight to {@code ABANDONED_PERMANENTLY} without {@code process(Job)} ever running again, which
 * would make FR-030 (a re-queued run must not misreport an already-deleted folder) unreachable:
 * there would be no second `process()` call to get it right. Leaving the annotation off costs
 * nothing here either way — every catch block in this class records a {@code FAILED} result and
 * returns rather than re-throwing, so {@code process(Job)} itself never throws an exception for the
 * ordinary exception-triggered retry path to even apply to.
 *
 * @author dotCMS
 */
@Dependent
@Queue(FolderBulkDeleteHelper.QUEUE_NAME)
public class FolderBulkDeleteProcessor implements JobProcessor, Cancellable {

    /**
     * Same property {@code AbandonedJobDetectorConfigProducer} reads, deliberately not cached.
     */
    private static final String ABANDONMENT_THRESHOLD_MINUTES_KEY =
            "JOB_ABANDONMENT_THRESHOLD_MINUTES";
    private static final int DEFAULT_ABANDONMENT_THRESHOLD_MINUTES = 30;
    private static final int HEARTBEAT_INTERVAL_DIVISOR = 3;

    private final AtomicInteger successCount = new AtomicInteger();
    private final AtomicInteger failedCount = new AtomicInteger();
    private final AtomicInteger skippedCount = new AtomicInteger();
    private final AtomicInteger processedCount = new AtomicInteger();
    private final AtomicBoolean cancellationRequested = new AtomicBoolean();
    private final List<BatchItemResult> results = new CopyOnWriteArrayList<>();
    private final SystemEventsAPI systemEventsAPI;
    private volatile String stoppedAt;
    private volatile boolean previouslyAbandoned;
    private int total;

    /**
     * Used by CDI/the job framework's reflection fallback ({@code JobProcessorFactory}) — kept as
     * the sole no-arg constructor so both paths still work, the same reasoning
     * {@code FolderBulkDeleteHelper}'s own no-args constructor javadoc gives.
     */
    public FolderBulkDeleteProcessor() {
        this(APILocator.getSystemEventsAPI());
    }

    /**
     * Visible for testing: lets a test capture what this processor pushes — which announcement, to
     * whom — without depending on the real asynchronous delivery queue, which is a different
     * subject (see {@code FolderBulkDeleteAnnouncementIT}'s own note on why).
     */
    public FolderBulkDeleteProcessor(final SystemEventsAPI systemEventsAPI) {
        this.systemEventsAPI = systemEventsAPI;
    }

    @Override
    public void process(final Job job) {

        final Map<String, Object> parameters = job.parameters();
        final List<String> submittedPaths = pathsOf(parameters);
        this.total = submittedPaths.size();
        final User user = user(parameters);
        this.previouslyAbandoned = wasPreviouslyAbandoned(job);

        Logger.info(this, String.format(
                "Bulk folder delete job [%s]: deleting %d path(s) for user [%s]",
                job.id(), submittedPaths.size(), user.getUserId()));

        // FR-013: decided up front, from the submitted paths alone — a descendant of another
        // selected path is never resolved, never attempted, and never charged to the folder that
        // covers it. Order-independent by construction: every path is checked against every
        // other, not against what has been processed so far.
        final WebAssetHelper webAssetHelper = WebAssetHelper.newInstance();
        for (int i = 0; i < submittedPaths.size(); i++) {

            if (this.cancellationRequested.get()) {
                recordRemainderAsSkipped(job, submittedPaths, i);
                break;
            }

            final String path = submittedPaths.get(i);
            final String coveringAncestor = findCoveringAncestor(path, submittedPaths);
            if (coveringAncestor != null) {
                Logger.info(this, String.format(
                        "Bulk folder delete job [%s]: [%s] is covered by selected ancestor [%s], "
                                + "skipping", job.id(), path, coveringAncestor));
                record(path, BatchItemStatus.SKIPPED, BatchFailureReason.COVERED_BY_PARENT, null);
            } else {
                deleteOne(webAssetHelper, path, job, user);
            }

            // FR-024/FR-025: the only honest denominator is completed top-level folders — nothing
            // inside FolderAPI.delete is observable, so this is the finest granularity that
            // exists, and it is reported exactly once per completed path, regardless of outcome.
            final int completed = this.processedCount.incrementAndGet();
            job.progressTracker().ifPresent(
                    tracker -> tracker.updateProgress(completed / (float) this.total));
        }

        Logger.info(this, String.format(
                "Bulk folder delete job [%s] finished: %d succeeded, %d failed, %d skipped",
                job.id(), this.successCount.get(), this.failedCount.get(),
                this.skippedCount.get()));
    }

    /**
     * Records every path from {@code fromIndex} onward as {@code SKIPPED} with no reason — they
     * were never attempted because cancellation landed first, a distinct meaning from
     * {@code COVERED_BY_PARENT} (FR-027) — and keeps the first of them as {@code stoppedAt} so the
     * outcome tells the author exactly where the run stopped (FR-028).
     */
    private void recordRemainderAsSkipped(final Job job, final List<String> allPaths,
            final int fromIndex) {

        this.stoppedAt = allPaths.get(fromIndex);
        for (int i = fromIndex; i < allPaths.size(); i++) {
            final String path = allPaths.get(i);
            Logger.info(this, String.format(
                    "Bulk folder delete job [%s]: cancelled, [%s] was never attempted",
                    job.id(), path));
            record(path, BatchItemStatus.SKIPPED, null, null);
        }
    }

    /**
     * Whether this job has ever been marked {@code ABANDONED} before this call — read once, at the
     * start of {@code process(Job)}, per plan.md PO-7. Checked via
     * {@code JobQueueManagerAPI.getJobQueue().hasJobBeenInState(...)}, already public — no
     * framework change was needed here (T009's own finding, see plan.md PO-7 and research.md R7).
     * <p>
     * Best-effort: a failure to read this history defaults to {@code false} (a fresh-run
     * assumption), which is the safe direction — it costs an author a spurious
     * {@code PATH_NOT_FOUND} on a folder a dead run already deleted, never the reverse (claiming
     * {@code SUCCESS} for a folder that was never touched).
     */
    private boolean wasPreviouslyAbandoned(final Job job) {
        try {
            return APILocator.getJobQueueManagerAPI().getJobQueue()
                    .hasJobBeenInState(job.id(), JobState.ABANDONED);
        } catch (final Exception e) {
            Logger.warn(this, String.format(
                    "Unable to check whether job [%s] was previously abandoned: %s",
                    job.id(), e.getMessage()), e);
            return false;
        }
    }

    @Override
    public void cancel(final Job job) throws JobCancellationException {
        Logger.info(this, "Cancellation requested for bulk folder delete job " + job.id());
        this.cancellationRequested.set(true);
    }

    /**
     * The first other submitted path that is a proper ancestor of {@code path}, or {@code null} if
     * none is. Comparison is a normalized string-prefix check (both sides forced to end with
     * {@code /}), which is exactly what "ancestor" means for a site-qualified path — no folder
     * lookup involved, so a descendant is skipped without ever being resolved (FR-013).
     * <p>
     * <b>A site root is never a covering ancestor</b>, even though every path in that site is a
     * string-prefix match against it. FR-013's premise is that deleting the ancestor removes the
     * descendant as a side effect — but a site root is always refused outright (FR-011,
     * {@code PROTECTED_FOLDER}), never actually deleted, so it never has that side effect. Treating
     * it as covering would silently skip every other folder in the same site instead of attempting
     * them, which is exactly what FR-011 requires *not* to happen ("the rest of the selection still
     * runs") — caught by
     * {@code test_process_siteRootPath_recordedAsProtectedFolder_restOfSelectionStillRuns}.
     */
    private static String findCoveringAncestor(final String path, final List<String> allPaths) {
        final String normalizedPath = normalize(path);
        for (final String other : allPaths) {
            if (other.equals(path)) {
                continue;
            }
            final String normalizedOther = normalize(other);
            if (!normalizedOther.equals(normalizedPath)
                    && !isSiteRoot(normalizedOther)
                    && normalizedPath.startsWith(normalizedOther)) {
                return other;
            }
        }
        return null;
    }

    private static String normalize(final String path) {
        return path.endsWith("/") ? path : path + "/";
    }

    /**
     * Whether a normalized path names a site root — {@code //hostname/} — rather than a folder
     * within it. A site-qualified path has the shape {@code //host/segment/segment/.../}; a site
     * root has none of the trailing segments.
     */
    private static boolean isSiteRoot(final String normalizedPath) {
        final String withoutLeadingSlashes = normalizedPath.replaceFirst("^/+", "");
        final String[] segments = withoutLeadingSlashes.split("/");
        return segments.length <= 1;
    }

    /**
     * Resolves and deletes one top-level folder. Any failure is caught here rather than propagated:
     * a bad path or a permission refusal on one folder is that folder's own outcome, and failing
     * the whole run over it would discard every folder already deleted (FR-008).
     */
    private void deleteOne(final WebAssetHelper webAssetHelper, final String path, final Job job,
            final User user) {

        final Folder folder;
        try {
            folder = resolve(webAssetHelper, path, user);
        } catch (final NotAFolderException e) {
            recordUnresolved(path, e.getMessage());
            return;
        } catch (final Exception e) {
            // The resolution step itself failed for a reason other than "not a folder" — an
            // unresolvable/malformed path behaves the same way from the caller's point of view
            // (FR-010: gone, a file, or malformed are all this path's own PATH_NOT_FOUND failure).
            Logger.warn(this, String.format("Unable to resolve path [%s]: %s",
                    path, e.getMessage()), e);
            recordUnresolved(path, e.getMessage());
            return;
        }

        if (FolderAPI.SYSTEM_FOLDER.equals(folder.getInode())) {
            // Decided before attempting the delete: FolderAPIImpl.delete() also refuses this, but
            // with the *same* DotSecurityException type it uses for an ordinary permission
            // refusal (FR-011 is a fact we already have, not one worth re-deriving from a message).
            // Never announced either — it is never actually attempted, so there is nothing for
            // "entering a delete" to honestly describe.
            record(path, BatchItemStatus.FAILED, BatchFailureReason.PROTECTED_FOLDER,
                    "the system folder is never deleted");
            return;
        }

        announceAndDelete(folder, path, job, user);
    }

    /**
     * Records a path that failed to resolve to a folder — {@code SUCCESS} if this job has been
     * {@code ABANDONED} before ("already gone, which is the intended end state", FR-030), or the
     * normal {@code PATH_NOT_FOUND} failure for a genuinely fresh run (FR-010). A path resolving to
     * a file rather than gone entirely hits this same branch on a re-queued run too — that is still
     * the honest answer: this feature cannot tell "a prior attempt deleted the folder" apart from
     * "the path never named a folder at all" once the folder is gone, and FR-030 only asks that the
     * former not be misreported as a failure.
     */
    private void recordUnresolved(final String path, final String message) {
        if (this.previouslyAbandoned) {
            record(path, BatchItemStatus.SUCCESS, null, null);
        } else {
            record(path, BatchItemStatus.FAILED, BatchFailureReason.PATH_NOT_FOUND, message);
        }
    }

    /**
     * Brackets the actual delete attempt with the two per-folder announcements (FR-035a): the
     * audience — everyone with {@code PERMISSION_READ} on the folder, excluding whoever is doing
     * the deleting — is computed once, before the attempt, since a successful delete leaves nothing
     * to compute rights against afterward. {@code FOLDER_DELETE_FINISHED} fires in every case
     * below, success or a classified failure alike (data-model.md §5) — this is the one method in
     * the class where that finally-equivalent shape matters.
     */
    private void announceAndDelete(final Folder folder, final String path, final Job job,
            final User user) {

        final Set<Role> readers = readerRolesOf(folder, path);
        announce(SystemEventType.FOLDER_DELETE_STARTED, job, path, user, readers);

        // FR-024a: started immediately before the blocking delete, stopped the instant it
        // returns — on any exit path, including cancellation and every catch below.
        final ScheduledExecutorService heartbeat = startHeartbeat(job);
        try {
            APILocator.getFolderAPI().delete(folder, user, false);
            record(path, BatchItemStatus.SUCCESS, null, null);
        } catch (final DotSecurityException e) {
            record(path, BatchItemStatus.FAILED, BatchFailureReason.PERMISSION_DENIED,
                    e.getMessage());
        } catch (final DotStateException e) {
            if (e.getCause() instanceof DotLockException) {
                record(path, BatchItemStatus.FAILED, BatchFailureReason.IN_USE, e.getMessage());
            } else {
                Logger.warn(this, String.format("Unable to delete folder [%s]: %s",
                        path, e.getMessage()), e);
                record(path, BatchItemStatus.FAILED, BatchFailureReason.UNCLASSIFIED,
                        e.getMessage());
            }
        } catch (final Exception e) {
            Logger.warn(this, String.format("Unable to delete folder [%s]: %s",
                    path, e.getMessage()), e);
            record(path, BatchItemStatus.FAILED, BatchFailureReason.UNCLASSIFIED, e.getMessage());
        } finally {
            heartbeat.shutdownNow();
            announce(SystemEventType.FOLDER_DELETE_FINISHED, job, path, user, readers);
        }
    }

    /**
     * Starts a background ticker calling {@link ProgressTracker#heartbeat()} at an interval derived
     * from the configured abandonment threshold — read fresh via {@code Config.getIntProperty} on
     * every call, deliberately not cached the way {@code AbandonedJobDetectorConfigProducer}'s own
     * {@code static final} fields are (that caching is exactly what made a runtime
     * {@code Config.setProperty} override unreliable for T065's abandonment test; this read has no
     * such caching to avoid the same trap). Set to a third of the threshold (plan.md PO-4) so a
     * single slow tick does not itself risk a false abandonment. The caller owns shutting this down
     * — see {@link #announceAndDelete}.
     */
    private ScheduledExecutorService startHeartbeat(final Job job) {

        final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        job.progressTracker().ifPresent(tracker -> {
            final long intervalMillis = heartbeatIntervalMillis();
            executor.scheduleAtFixedRate(tracker::heartbeat, intervalMillis, intervalMillis,
                    TimeUnit.MILLISECONDS);
        });
        return executor;
    }

    private long heartbeatIntervalMillis() {
        final int thresholdMinutes = Config.getIntProperty(
                ABANDONMENT_THRESHOLD_MINUTES_KEY, DEFAULT_ABANDONMENT_THRESHOLD_MINUTES);
        return Duration.ofMinutes(Math.max(thresholdMinutes, 1))
                .dividedBy(HEARTBEAT_INTERVAL_DIVISOR)
                .toMillis();
    }

    /**
     * The audience for both of this folder's announcements — never lets computing it get in the way
     * of the delete attempt itself: a failure here (logged) falls back to an empty audience rather
     * than aborting the folder, since the announcement is best-effort but the delete is not.
     */
    private Set<Role> readerRolesOf(final Folder folder, final String path) {
        try {
            return APILocator.getPermissionAPI()
                    .getRolesWithPermission(folder, PermissionAPI.PERMISSION_READ);
        } catch (final Exception e) {
            Logger.warn(this, String.format(
                    "Unable to compute the announcement audience for [%s]: %s",
                    path, e.getMessage()), e);
            return Set.of();
        }
    }

    /**
     * Pushes one per-folder announcement — best-effort, like every other notification this feature
     * sends: a delivery failure is logged and never turns into this folder's own outcome (the
     * announcement is fire-and-forget by design, data-model.md §5; the client's own
     * in-flight-listing establishment on load is the documented recovery path, C-012).
     * <p>
     * Payload is deliberately the frontend's minimal {@code {jobId, path}}
     * (`DotFolderDeleteAnnouncementEvent`, PR dotCMS/core#37612) — not {@code folder.getMap()},
     * which is what the existing {@code DELETE_FOLDER} event carries. This announcement says a
     * folder is busy or no longer busy, nothing about how its own delete went.
     */
    private void announce(final SystemEventType type, final Job job, final String path,
            final User user, final Set<Role> readers) {

        final Map<String, Object> payload = new HashMap<>();
        payload.put("jobId", job.id());
        payload.put("path", path);

        try {
            this.systemEventsAPI.pushAsync(type, new Payload(payload,
                    Visibility.EXCLUDE_OWNER, new ExcludeOwnerVerifierBean(user.getUserId(),
                    new VisibilityRoles(VisibilityRoles.Operator.OR, readers),
                    Visibility.ROLES)));
        } catch (final Exception e) {
            Logger.warn(this, String.format("Unable to push %s for [%s]: %s",
                    type, path, e.getMessage()), e);
        }
    }

    /**
     * Resolves a path to the {@link Folder} it names, exactly the way the shipped single-folder
     * delete does ({@code WebAssetHelper#deleteFolder}) — reused here rather than called directly
     * because classification needs the intermediate facts (is this even a folder? which one?) that
     * {@code deleteFolder}'s all-in-one method does not expose.
     *
     * @throws NotAFolderException the path resolves, but not to a folder (a file) — FR-010
     */
    private Folder resolve(final WebAssetHelper webAssetHelper, final String path, final User user)
            throws Exception {

        final WebAssetView assetInfo = webAssetHelper.getAssetInfo(path, user);
        if (!(assetInfo instanceof FolderView)) {
            throw new NotAFolderException(path);
        }

        final FolderView folderView = (FolderView) assetInfo;
        final Folder folder = APILocator.getFolderAPI().find(folderView.inode(), user, false);
        if (folder == null || !UtilMethods.isSet(folder.getInode())) {
            throw new NotAFolderException(path);
        }
        return folder;
    }

    private void record(final String path, final BatchItemStatus status,
            final BatchFailureReason reason, final String message) {

        final BatchItemResult.Builder builder = BatchItemResult.builder()
                .key(path)
                .status(status);
        if (reason != null) {
            builder.reason(reason);
        }
        if (message != null) {
            builder.message(message);
        }
        this.results.add(builder.build());

        if (status == BatchItemStatus.SUCCESS) {
            this.successCount.incrementAndGet();
        } else if (status == BatchItemStatus.FAILED) {
            this.failedCount.incrementAndGet();
        } else if (status == BatchItemStatus.SKIPPED) {
            this.skippedCount.incrementAndGet();
        }
    }

    /**
     * A path resolved to something, but not to a folder — FR-010. Classification-only signal.
     */
    private static final class NotAFolderException extends Exception {

        NotAFolderException(final String path) {
            super("The path [" + path + "] does not resolve to a folder");
        }
    }

    @Override
    public Map<String, Object> getResultMetadata(final Job job) {

        final Map<String, Object> metadata = new HashMap<>();
        metadata.put("total", this.total);
        // Excludes paths a cancellation left unreached; includes ancestor-removed ones, since
        // those *were* processed as a dedup decision (data-model.md §4).
        metadata.put("processed", this.processedCount.get());
        metadata.put("successCount", this.successCount.get());
        metadata.put("failedCount", this.failedCount.get());
        metadata.put("skippedCount", this.skippedCount.get());
        metadata.put("results", List.copyOf(this.results));
        if (this.stoppedAt != null) {
            metadata.put("stoppedAt", this.stoppedAt);
        }
        return metadata;
    }

    private static List<String> pathsOf(final Map<String, Object> parameters) {
        return FolderBulkDeleteHelper.pathsOf(parameters);
    }

    private User user(final Map<String, Object> parameters) {
        final String userId = String.valueOf(parameters.get("userId"));
        try {
            return APILocator.getUserAPI().loadUserById(userId);
        } catch (final Exception e) {
            throw new JobProcessingException("Unable to load the submitting user " + userId, e);
        }
    }
}
