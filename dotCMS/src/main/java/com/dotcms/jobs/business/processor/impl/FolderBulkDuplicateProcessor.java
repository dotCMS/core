package com.dotcms.jobs.business.processor.impl;

import com.dotcms.jobs.business.batch.BatchFailureReason;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.time.Duration;
import com.dotmarketing.util.Config;
import com.dotcms.jobs.business.processor.ProgressTracker;
import com.dotcms.jobs.business.processor.NoRetryPolicy;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.Set;
import java.util.Optional;
import java.util.LinkedHashMap;
import java.util.Comparator;
import java.util.Collection;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.PermissionAPI;
import com.dotcms.rest.exception.NotFoundException;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.jobs.business.batch.BatchItemResult;
import com.dotcms.jobs.business.batch.BatchItemStatus;
import com.dotcms.jobs.business.error.JobCancellationException;
import com.dotcms.jobs.business.error.JobProcessingException;
import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.processor.Cancellable;
import com.dotcms.jobs.business.processor.JobProcessor;
import com.dotcms.jobs.business.processor.Queue;
import com.dotcms.rest.api.v1.asset.bulkduplicate.FolderBulkDuplicateHelper;
import com.dotcms.rest.api.v1.asset.AssetPathResolver;
import com.dotcms.rest.api.v1.asset.ResolvedAssetAndPath;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.enterprise.context.Dependent;

/**
 * Runs a bulk folder duplication: each submitted folder is duplicated in place, one at a time, in
 * its own transaction (#37062).
 * <p>
 * One folder failing never stops the run: its failure is recorded against its submitted path and
 * the next folder is attempted, so every submitted folder ends with exactly one outcome record.
 * Progress is reported once per completed folder, the finest step that can be observed.
 * <p>
 * Cancellation takes effect between folders: the folder in progress finishes, and every folder
 * not yet reached is recorded SKIPPED with no reason.
 * <p>
 * <b>Liveness.</b> One folder can take a long time, and the queue marks a run abandoned when its
 * {@code updated_at} stops moving. So while each folder is being duplicated a background ticker
 * calls {@link ProgressTracker#heartbeat()} every third of the abandonment threshold, the same way
 * {@code FolderBulkDeleteProcessor} does. The heartbeat only proves the run is alive; progress still
 * moves once per completed folder.
 * <p>
 * <b>Never retried.</b> Duplication is not idempotent: running it again creates a second set of
 * duplicates. Hence {@link NoRetryPolicy}, which does stop an abandoned run from running again:
 * {@code JobQueueManagerAPIImpl.processJobWithRetry} routes a job coming back from ABANDONED
 * through the same retry check as a failed one, so this processor goes to ABANDONED_PERMANENTLY.
 * {@code BulkUploadProcessor}'s Javadoc claims the opposite and is wrong. Bulk delete leaves the
 * annotation off because re-running a delete is harmless; re-running a duplicate is not.
 *
 * @author dotCMS
 */
@Dependent
@Queue(FolderBulkDuplicateHelper.QUEUE_NAME)
@NoRetryPolicy
public class FolderBulkDuplicateProcessor implements JobProcessor, Cancellable {

    /** The property the queue's abandonment detector reads, deliberately not cached here. */
    private static final String ABANDONMENT_THRESHOLD_MINUTES_KEY =
            "JOB_ABANDONMENT_THRESHOLD_MINUTES";
    private static final int DEFAULT_ABANDONMENT_THRESHOLD_MINUTES = 30;
    private static final int HEARTBEAT_INTERVAL_DIVISOR = 3;

    /**
     * Test-only override for the heartbeat interval, so a test can tick well under the one-minute
     * floor of the abandonment threshold. Nothing outside this process needs to know it.
     */
    private static final String HEARTBEAT_INTERVAL_MILLIS_KEY =
            "FOLDER_BULK_DUPLICATE_HEARTBEAT_INTERVAL_MILLIS";

    private final AtomicInteger successCount = new AtomicInteger();
    private final AtomicInteger failedCount = new AtomicInteger();
    private final AtomicInteger skippedCount = new AtomicInteger();
    private final AtomicInteger processedCount = new AtomicInteger();
    private final AtomicBoolean cancellationRequested = new AtomicBoolean();
    private final List<BatchItemResult> results = new CopyOnWriteArrayList<>();
    private final PermissionAPI permissionAPI;
    private volatile int total;
    /** The first submitted folder a cancelled run never reached; null when the run was not cut short. */
    private volatile String stoppedAt;
    /** The last progress percentage reported, rounded, so a repeat is never sent. */
    private int lastReportedPercent = -1;

    /** Used by CDI and the job framework's reflection fallback. */
    public FolderBulkDuplicateProcessor() {
        this(APILocator.getPermissionAPI());
    }

    /**
     * @param permissionAPI answers the batch read and add-children checks; a test passes one that
     *                      fails, to prove a failed check is not reported as a refusal
     */
    FolderBulkDuplicateProcessor(final PermissionAPI permissionAPI) {
        this.permissionAPI = permissionAPI;
    }

    /**
     * Duplicates every submitted folder, in submission order.
     * <p>
     * Every refusal that can be known up front is decided before the first copy: a folder inside
     * another selected folder, a protected folder, a path that no longer resolves, and the two
     * permission refusals, each checked for the whole selection in one round-trip. What is left is
     * duplicated one folder at a time.
     *
     * @param job the run, carrying {@code assetPaths} and {@code userId}
     */
    @Override
    public void process(final Job job) {
        final List<String> paths = FolderBulkDuplicateHelper.pathsOf(job.parameters());
        this.total = paths.size();
        final User user = user(job.parameters());
        final FolderDuplicator duplicator = new FolderDuplicator();

        Logger.info(this, String.format(
                "Bulk folder duplicate job [%s]: duplicating %d path(s) for user [%s]",
                job.id(), paths.size(), user.getUserId()));

        final Map<String, Folder> duplicable = decideUpFront(paths, duplicator, user);

        for (int index = 0; index < paths.size(); index++) {
            // Honoured between folders, never inside one, so a folder is always fully duplicated
            // or not duplicated at all.
            if (this.cancellationRequested.get()) {
                recordUnreachedAsSkipped(paths.subList(index, paths.size()), duplicable);
                break;
            }

            final String path = paths.get(index);
            final Folder source = duplicable.get(path);
            if (source != null) {
                // Started just before the folder and stopped the instant it returns, on every exit.
                final ScheduledExecutorService heartbeat = startHeartbeat(job);
                try {
                    duplicateOne(duplicator, source, path, user);
                } finally {
                    heartbeat.shutdownNow();
                }
            }

            reportProgress(job, this.processedCount.incrementAndGet());
        }

        // Records were made as each decision fell, so put them back in submission order.
        this.results.sort(Comparator.comparingInt(result -> paths.indexOf(result.key())));

        Logger.info(this, String.format(
                "Bulk folder duplicate job [%s] finished: %d succeeded, %d failed, %d skipped",
                job.id(), this.successCount.get(), this.failedCount.get(),
                this.skippedCount.get()));
    }

    /**
     * Starts a background ticker calling {@link ProgressTracker#heartbeat()}, at a third of the
     * abandonment threshold so one slow tick cannot itself cause a false abandonment. The caller
     * shuts it down.
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

    /**
     * A third of the abandonment threshold, floored at one minute's worth, unless the test-only
     * override is set. Read fresh on every call, so {@code Config.setProperty} takes effect at once.
     */
    private long heartbeatIntervalMillis() {
        final int thresholdMinutes = Config.getIntProperty(ABANDONMENT_THRESHOLD_MINUTES_KEY,
                DEFAULT_ABANDONMENT_THRESHOLD_MINUTES);
        final long derivedFromThreshold = Duration.ofMinutes(Math.max(thresholdMinutes, 1))
                .dividedBy(HEARTBEAT_INTERVAL_DIVISOR)
                .toMillis();
        return Config.getLongProperty(HEARTBEAT_INTERVAL_MILLIS_KEY, derivedFromThreshold);
    }

    /**
     * Reports progress only when the rounded percentage changes, not after every folder (FR-027):
     * a report that says nothing new is noise to whoever is following the run.
     */
    private void reportProgress(final Job job, final int completed) {
        final int percent = Math.round(completed * 100f / this.total);
        if (percent != this.lastReportedPercent) {
            this.lastReportedPercent = percent;
            job.progressTracker().ifPresent(
                    tracker -> tracker.updateProgress(completed / (float) this.total));
        }
    }

    /**
     * Records every folder a cancelled run never reached as SKIPPED with no reason, and the first
     * of them as where the run stopped. Folders already decided up front keep the outcome they
     * were given.
     */
    private void recordUnreachedAsSkipped(final List<String> remaining,
            final Map<String, Folder> duplicable) {
        for (final String path : remaining) {
            if (duplicable.containsKey(path)) {
                if (this.stoppedAt == null) {
                    // Where the remainder begins, so it can be resubmitted deliberately (FR-032).
                    this.stoppedAt = path;
                }
                record(path, BatchItemStatus.SKIPPED, null, null);
            }
        }
    }

    /**
     * Records every outcome that can be known before copying anything, and answers the folders
     * that are left to duplicate, by path.
     */
    private Map<String, Folder> decideUpFront(final List<String> paths,
            final FolderDuplicator duplicator, final User user) {

        final AssetPathResolver pathResolver = AssetPathResolver.newInstance();
        final User systemUser = APILocator.systemUser();
        final Map<String, Folder> resolved = new LinkedHashMap<>();

        for (final String path : paths) {
            final String coveringAncestor = findCoveringAncestor(path, paths);
            if (coveringAncestor != null) {
                // Its ancestor's duplicate already carries it; duplicating it too would copy it
                // twice, once inside the ancestor's duplicate and once beside itself.
                record(path, BatchItemStatus.SKIPPED, BatchFailureReason.COVERED_BY_PARENT, null);
            } else if (isSiteRoot(path)) {
                record(path, BatchItemStatus.FAILED, BatchFailureReason.PROTECTED_FOLDER,
                        "a site root is not a folder that can be duplicated");
            } else {
                resolveInto(resolved, pathResolver, path, systemUser);
            }
        }

        // Read on the folder itself, checked as the author for the whole selection at once.
        final Set<String> readable;
        try {
            readable = permitted(resolved.values(), PermissionAPI.PERMISSION_READ, user);
        } catch (final PermissionCheckFailedException e) {
            recordCheckFailure(resolved.keySet(), e);
            return Map.of();
        }
        final Map<String, Permissionable> parents = new LinkedHashMap<>();
        for (final Map.Entry<String, Folder> entry : resolved.entrySet()) {
            if (readable.contains(entry.getValue().getPermissionId())) {
                parentOf(duplicator, entry.getKey(), entry.getValue())
                        .ifPresent(parent -> parents.put(entry.getKey(), parent));
            } else {
                record(entry.getKey(), BatchItemStatus.FAILED,
                        BatchFailureReason.PERMISSION_DENIED, "no permission to read the folder");
            }
        }

        // Add-children where each duplicate lands, again in one round-trip.
        final Set<String> writableParents;
        try {
            writableParents = permitted(parents.values(),
                    PermissionAPI.PERMISSION_CAN_ADD_CHILDREN, user);
        } catch (final PermissionCheckFailedException e) {
            recordCheckFailure(parents.keySet(), e);
            return Map.of();
        }
        final Map<String, Folder> duplicable = new LinkedHashMap<>();
        for (final Map.Entry<String, Permissionable> entry : parents.entrySet()) {
            if (writableParents.contains(entry.getValue().getPermissionId())) {
                duplicable.put(entry.getKey(), resolved.get(entry.getKey()));
            } else {
                record(entry.getKey(), BatchItemStatus.FAILED,
                        BatchFailureReason.PARENT_PERMISSION_DENIED,
                        "no permission to add folders where the duplicate would land");
            }
        }
        return duplicable;
    }

    /**
     * Resolves a path to its folder as the system user, so whether the author may read it is
     * decided by the batch check rather than an exception, recording the path's failure when it
     * does not name a folder that can be duplicated. A path that names a file is not found.
     *
     * @param resolved     collects each path that resolved to a folder, keyed by the path
     * @param pathResolver turns a site-qualified path into its site and folder
     * @param path         the submitted path
     * @param systemUser   the user the lookup runs as
     */
    private void resolveInto(final Map<String, Folder> resolved,
            final AssetPathResolver pathResolver, final String path, final User systemUser) {
        try {
            // The resolver alone: it finds the site and the folder by path and nothing more.
            // WebAssetHelper.getAssetInfo would also list the folder's whole contents, which is
            // costly for a large folder and is thrown away here before the heartbeat starts.
            final ResolvedAssetAndPath resolvedPath = pathResolver.resolve(path, systemUser);
            final Folder folder = UtilMethods.isSet(resolvedPath.asset())
                    ? null
                    : resolvedPath.resolvedFolder();
            if (folder == null || !UtilMethods.isSet(folder.getInode())) {
                record(path, BatchItemStatus.FAILED, BatchFailureReason.PATH_NOT_FOUND,
                        "the path does not resolve to a folder");
            } else if (FolderAPI.SYSTEM_FOLDER.equals(folder.getInode())) {
                record(path, BatchItemStatus.FAILED, BatchFailureReason.PROTECTED_FOLDER,
                        "the system folder is never duplicated");
            } else {
                resolved.put(path, folder);
            }
        } catch (final NotFoundInDbException | NotFoundException | IllegalArgumentException e) {
            // Gone, not a folder, or not a path the server can parse: all PATH_NOT_FOUND by the
            // contract (FR-013). IllegalArgumentException is how the path resolver reports a
            // path it cannot parse.
            record(path, BatchItemStatus.FAILED, BatchFailureReason.PATH_NOT_FOUND,
                    e.getMessage());
        } catch (final Exception e) {
            Logger.warn(this, String.format("Unable to resolve path [%s]: %s",
                    path, e.getMessage()), e);
            record(path, BatchItemStatus.FAILED, BatchFailureReason.UNCLASSIFIED, e.getMessage());
        }
    }

    /** Where a resolved folder's duplicate lands, or a recorded failure when that is unknown. */
    private Optional<Permissionable> parentOf(final FolderDuplicator duplicator, final String path,
            final Folder folder) {
        try {
            return Optional.of(duplicator.parentOf(folder));
        } catch (final Exception e) {
            Logger.warn(this, String.format("Unable to find where [%s] would be duplicated: %s",
                    path, e.getMessage()), e);
            record(path, BatchItemStatus.FAILED, BatchFailureReason.UNCLASSIFIED, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * The permission ids, among the given items, the author holds a permission on, resolved in one
     * round-trip.
     *
     * @throws PermissionCheckFailedException the check itself could not run. That is not a
     *                                        refusal, and the caller must not report it as one.
     */
    private Set<String> permitted(final Collection<? extends Permissionable> items,
            final int permission, final User user) {
        if (items.isEmpty()) {
            return Set.of();
        }
        try {
            return this.permissionAPI
                    .filterCollection(List.<Permissionable>copyOf(items), permission, user, false)
                    .stream()
                    .map(Permissionable::getPermissionId)
                    .collect(Collectors.toSet());
        } catch (final Exception e) {
            throw new PermissionCheckFailedException(e);
        }
    }

    /**
     * Records every folder a failed permission check covered as FAILED / UNCLASSIFIED, with the
     * error as its diagnostic. Reporting them as denied would hide the real failure behind a reason
     * that is not true.
     */
    private void recordCheckFailure(final Collection<String> paths,
            final PermissionCheckFailedException e) {
        Logger.warn(this, "Unable to check permissions for the selection: "
                + e.getCause().getMessage(), e.getCause());
        for (final String path : paths) {
            record(path, BatchItemStatus.FAILED, BatchFailureReason.UNCLASSIFIED,
                    "Unable to check permissions: " + e.getCause().getMessage());
        }
    }

    /** A batch permission check that could not run, as opposed to one that said no. */
    private static final class PermissionCheckFailedException extends RuntimeException {

        PermissionCheckFailedException(final Throwable cause) {
            super(cause);
        }
    }

    /**
     * Duplicates one folder the up-front checks cleared, recording its outcome. A failure here is
     * this folder's alone: failing the run over it would discard every folder already duplicated.
     */
    private void duplicateOne(final FolderDuplicator duplicator, final Folder source,
            final String path, final User user) {
        try {
            duplicator.duplicate(source, user);
            record(path, BatchItemStatus.SUCCESS, null, null);
        } catch (final Exception e) {
            Logger.warn(this, String.format("Unable to duplicate folder [%s]: %s",
                    path, e.getMessage()), e);
            record(path, BatchItemStatus.FAILED, BatchFailureReason.UNCLASSIFIED, e.getMessage());
        }
    }

    /**
     * The first other submitted path that is a proper ancestor of this one, or {@code null}.
     * Decided from the paths alone, so it does not depend on the order they were submitted in.
     * Compared lowercased, since folder resolution ignores case. Only a site-qualified folder
     * path can cover another. A site root is refused, and a path with no site in front of it is
     * never resolved, so neither is duplicated and neither has a duplicate to carry the folder.
     *
     * @param path     the submitted path being decided
     * @param allPaths every path in the submission
     * @return the covering ancestor as it was submitted, or {@code null} when there is none
     */
    static String findCoveringAncestor(final String path, final List<String> allPaths) {
        final String normalizedPath = normalize(path).toLowerCase();
        for (final String other : allPaths) {
            final String normalizedOther = normalize(other).toLowerCase();
            if (!normalizedOther.equals(normalizedPath) && canCover(normalizedOther)
                    && normalizedPath.startsWith(normalizedOther)) {
                return other;
            }
        }
        return null;
    }

    /** Whether a submitted path can be duplicated at all: site-qualified and not a site root. */
    private static boolean canCover(final String path) {
        return path.startsWith("//") && !isSiteRoot(path);
    }

    private static String normalize(final String path) {
        return path.endsWith("/") ? path : path + "/";
    }

    /** A site-qualified path naming only its site: two slashes, a hostname, an optional slash. */
    private static final Pattern SITE_ROOT = Pattern.compile("^//[^/]+/?$");

    /**
     * Whether a path names a site root, {@code //hostname/}, rather than a folder in it.
     * <p>
     * Only a site-qualified path can be a site root. A path with no site in front of it, such as
     * {@code /blogs/} or a bare word, is malformed rather than protected, so it is left for the
     * resolver to report as not found.
     *
     * @param path a submitted path, with or without its trailing slash
     * @return true when the path is {@code //hostname} or {@code //hostname/}
     */
    static boolean isSiteRoot(final String path) {
        return SITE_ROOT.matcher(path).matches();
    }

    private void record(final String path, final BatchItemStatus status,
            final BatchFailureReason reason, final String message) {
        final BatchItemResult.Builder builder = BatchItemResult.builder().key(path).status(status);
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
        } else {
            this.skippedCount.incrementAndGet();
        }
    }

    /**
     * Asks the run to stop before its next folder.
     *
     * @param job the run
     * @throws JobCancellationException never
     */
    @Override
    public void cancel(final Job job) throws JobCancellationException {
        Logger.info(this, "Cancellation requested for bulk folder duplicate job " + job.id());
        this.cancellationRequested.set(true);
    }

    /**
     * The run's outcome: its counts and one record per submitted folder.
     *
     * @param job the run
     * @return {@code total}, {@code processed}, the three counts and {@code results}, plus
     *     {@code stoppedAt} when a cancellation cut the run short
     */
    @Override
    public Map<String, Object> getResultMetadata(final Job job) {
        final Map<String, Object> metadata = new HashMap<>();
        metadata.put("total", this.total);
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

    private User user(final Map<String, Object> parameters) {
        final String userId = String.valueOf(parameters.get("userId"));
        try {
            return APILocator.getUserAPI().loadUserById(userId);
        } catch (final Exception e) {
            throw new JobProcessingException("Unable to load the submitting user " + userId, e);
        }
    }
}
