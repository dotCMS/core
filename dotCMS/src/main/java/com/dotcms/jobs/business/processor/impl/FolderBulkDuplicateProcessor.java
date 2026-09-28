package com.dotcms.jobs.business.processor.impl;

import com.dotcms.jobs.business.batch.BatchFailureReason;
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
import com.dotcms.rest.api.v1.asset.WebAssetHelper;
import com.dotcms.rest.api.v1.asset.view.FolderView;
import com.dotcms.rest.api.v1.asset.view.WebAssetView;
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
 * not yet reached is recorded SKIPPED with no reason. Retry and heartbeat arrive in their own
 * phase.
 *
 * @author dotCMS
 */
@Dependent
@Queue(FolderBulkDuplicateHelper.QUEUE_NAME)
public class FolderBulkDuplicateProcessor implements JobProcessor, Cancellable {

    private final AtomicInteger successCount = new AtomicInteger();
    private final AtomicInteger failedCount = new AtomicInteger();
    private final AtomicInteger skippedCount = new AtomicInteger();
    private final AtomicInteger processedCount = new AtomicInteger();
    private final AtomicBoolean cancellationRequested = new AtomicBoolean();
    private final List<BatchItemResult> results = new CopyOnWriteArrayList<>();
    private volatile int total;

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
                duplicateOne(duplicator, source, path, user);
            }

            final int completed = this.processedCount.incrementAndGet();
            job.progressTracker().ifPresent(
                    tracker -> tracker.updateProgress(completed / (float) this.total));
        }

        // Records were made as each decision fell, so put them back in submission order.
        this.results.sort(Comparator.comparingInt(result -> paths.indexOf(result.key())));

        Logger.info(this, String.format(
                "Bulk folder duplicate job [%s] finished: %d succeeded, %d failed, %d skipped",
                job.id(), this.successCount.get(), this.failedCount.get(),
                this.skippedCount.get()));
    }

    /**
     * Records every folder a cancelled run never reached as SKIPPED with no reason. Folders
     * already decided up front keep the outcome they were given.
     */
    private void recordUnreachedAsSkipped(final List<String> remaining,
            final Map<String, Folder> duplicable) {
        for (final String path : remaining) {
            if (duplicable.containsKey(path)) {
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

        final WebAssetHelper webAssetHelper = WebAssetHelper.newInstance();
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
                resolveInto(resolved, webAssetHelper, path, systemUser);
            }
        }

        // Read on the folder itself, checked as the author for the whole selection at once.
        final Set<String> readable = permitted(resolved.values(), PermissionAPI.PERMISSION_READ,
                user);
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
        final Set<String> writableParents = permitted(parents.values(),
                PermissionAPI.PERMISSION_CAN_ADD_CHILDREN, user);
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
     * does not name a folder that can be duplicated.
     */
    private void resolveInto(final Map<String, Folder> resolved,
            final WebAssetHelper webAssetHelper, final String path, final User systemUser) {
        try {
            final WebAssetView assetInfo = webAssetHelper.getAssetInfo(path, systemUser);
            final Folder folder = assetInfo instanceof FolderView
                    ? APILocator.getFolderAPI()
                            .find(((FolderView) assetInfo).inode(), systemUser, false)
                    : null;
            if (folder == null || !UtilMethods.isSet(folder.getInode())) {
                record(path, BatchItemStatus.FAILED, BatchFailureReason.PATH_NOT_FOUND,
                        "the path does not resolve to a folder");
            } else if (FolderAPI.SYSTEM_FOLDER.equals(folder.getInode())) {
                record(path, BatchItemStatus.FAILED, BatchFailureReason.PROTECTED_FOLDER,
                        "the system folder is never duplicated");
            } else {
                resolved.put(path, folder);
            }
        } catch (final NotFoundInDbException | NotFoundException e) {
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
     * round-trip. A failure to check reads as holding none, so nothing is duplicated on a guess.
     */
    private Set<String> permitted(final Collection<? extends Permissionable> items,
            final int permission, final User user) {
        if (items.isEmpty()) {
            return Set.of();
        }
        try {
            return APILocator.getPermissionAPI()
                    .filterCollection(List.<Permissionable>copyOf(items), permission, user, false)
                    .stream()
                    .map(Permissionable::getPermissionId)
                    .collect(Collectors.toSet());
        } catch (final Exception e) {
            Logger.warn(this, "Unable to check permissions for the selection: " + e.getMessage(),
                    e);
            return Set.of();
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
     * Compared lowercased, since folder resolution ignores case. A site root never covers
     * anything: it is refused, never duplicated, so it has no duplicate to carry the folder.
     */
    private static String findCoveringAncestor(final String path, final List<String> allPaths) {
        final String normalizedPath = normalize(path).toLowerCase();
        for (final String other : allPaths) {
            final String normalizedOther = normalize(other).toLowerCase();
            if (!normalizedOther.equals(normalizedPath) && !isSiteRoot(normalizedOther)
                    && normalizedPath.startsWith(normalizedOther)) {
                return other;
            }
        }
        return null;
    }

    private static String normalize(final String path) {
        return path.endsWith("/") ? path : path + "/";
    }

    /** Whether a site-qualified path names a site root, {@code //hostname/}, not a folder in it. */
    private static boolean isSiteRoot(final String path) {
        return normalize(path).replaceFirst("^/+", "").split("/").length <= 1;
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
     * @return {@code total}, {@code processed}, the three counts, and {@code results}
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
