package com.dotcms.jobs.business.processor.impl;

import com.dotcms.content.elasticsearch.business.ContentletIndexAPI;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.jobs.business.error.JobCancellationException;
import com.dotcms.jobs.business.error.JobProcessingException;
import com.dotcms.jobs.business.error.JobValidationException;
import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.processor.Cancellable;
import com.dotcms.jobs.business.processor.JobProcessor;
import com.dotcms.jobs.business.processor.NoRetryPolicy;
import com.dotcms.jobs.business.processor.ProgressTracker;
import com.dotcms.jobs.business.processor.Queue;
import com.dotcms.jobs.business.processor.Validator;
import com.dotcms.rest.api.v1.content.bulkrefresh.BulkRefreshItemResult;
import com.dotcms.rest.api.v1.content.bulkrefresh.BulkRefreshItemStatus;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.ContentletCache;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.enterprise.context.Dependent;

/**
 * Reindexes a selection of contentlets, one identifier at a time.
 * <p>
 * Backs {@code POST /api/v1/content/_bulkrefresh}. For each identifier it clears the contentlet cache
 * and writes every version to the index <b>synchronously</b>: the default {@link IndexPolicy#DEFER}
 * only enqueues into {@code dist_reindex_journal} and returns, which is exactly why the single-item
 * {@code _refresh} can answer {@code true} with nothing actually reindexed. Synchronous writes are
 * what make "done" honest.
 * <p>
 * Reindexing is not retried — {@link NoRetryPolicy}. A failed item is a per-item record, and replaying
 * a whole batch to re-attempt one identifier costs far more than it recovers.
 *
 * @author dotCMS
 */
@Dependent
@Queue("bulkRefreshContentlets")
@NoRetryPolicy
public class BulkRefreshContentletsProcessor implements JobProcessor, Validator, Cancellable {

    /** Job parameter: the submitted contentlet inodes. */
    public static final String PARAM_CONTENTLET_IDS = "contentletIds";

    /** Job parameter: whether related content is reindexed alongside each item. */
    public static final String PARAM_INCLUDE_DEPENDENCIES = "includeDependencies";

    /** Job parameter: whether per-item records are kept and reported. */
    public static final String PARAM_INCLUDE_ITEM_RESULTS = "includeItemResults";

    /** Job parameter: the submitting user, so the run reindexes with their permissions. */
    public static final String PARAM_USER_ID = "userId";

    private static final String SKIP_REASON =
            "The reindex was cancelled before these items were attempted";

    private final ContentletAPI contentletAPI;
    private final IdentifierAPI identifierAPI;
    private final ContentletIndexAPI contentletIndexAPI;
    private final ContentletCache contentletCache;
    private final UserAPI userAPI;

    private final AtomicBoolean cancellationRequested = new AtomicBoolean(false);

    /**
     * Per-item records, in submission order, populated only when the job asked for them. Read back
     * through {@link #getResultMetadata(Job)} once the run is terminal, which is what persists them
     * with the job for the drill-down.
     */
    private final List<BulkRefreshItemResult> itemResults = new CopyOnWriteArrayList<>();

    private final AtomicInteger total = new AtomicInteger();
    private final AtomicInteger successCount = new AtomicInteger();
    private final AtomicInteger failedCount = new AtomicInteger();
    private final AtomicInteger skippedCount = new AtomicInteger();
    private final AtomicInteger versionsIndexed = new AtomicInteger();

    /**
     * Required by {@link com.dotcms.jobs.business.api.JobProcessorFactory}, which instantiates
     * processors through their no-arg constructor.
     */
    public BulkRefreshContentletsProcessor() {
        this(APILocator.getContentletAPI(), APILocator.getIdentifierAPI(),
                APILocator.getContentletIndexAPI(), CacheLocator.getContentletCache(),
                APILocator.getUserAPI());
    }

    @VisibleForTesting
    BulkRefreshContentletsProcessor(final ContentletAPI contentletAPI,
            final IdentifierAPI identifierAPI, final ContentletIndexAPI contentletIndexAPI,
            final ContentletCache contentletCache, final UserAPI userAPI) {
        this.contentletAPI = contentletAPI;
        this.identifierAPI = identifierAPI;
        this.contentletIndexAPI = contentletIndexAPI;
        this.contentletCache = contentletCache;
        this.userAPI = userAPI;
    }

    @Override
    public void process(final Job job) throws JobProcessingException {

        final Map<String, Object> parameters = job.parameters();
        final boolean includeDependencies = flag(parameters, PARAM_INCLUDE_DEPENDENCIES);
        final boolean recordItemResults = flag(parameters, PARAM_INCLUDE_ITEM_RESULTS);
        final User user = user(parameters);

        final List<WorkItem> workItems = resolve(contentletIds(parameters), user);
        this.total.set(workItems.size());

        final ProgressTracker progressTracker = job.progressTracker().orElseThrow(
                () -> new JobProcessingException(job.id(), "Progress tracker not found"));

        Logger.info(this, String.format(
                "Bulk refresh job [%s]: reindexing %d identifier(s) for user [%s], "
                        + "includeDependencies=%s", job.id(), workItems.size(), user.getUserId(),
                includeDependencies));

        for (final WorkItem workItem : workItems) {

            if (this.cancellationRequested.get()) {
                skip(workItem, recordItemResults);
            } else {
                refresh(workItem, user, includeDependencies, recordItemResults);
            }

            progressTracker.updateProgress(processed() / (float) workItems.size());
        }

        Logger.info(this, String.format(
                "Bulk refresh job [%s] finished: %d succeeded, %d failed, %d skipped, "
                        + "%d version(s) indexed", job.id(), this.successCount.get(),
                this.failedCount.get(), this.skippedCount.get(), this.versionsIndexed.get()));
    }

    /**
     * Reindexes every version of one identifier.
     * <p>
     * Any failure is caught here rather than propagated: a permission problem or a vanished row on one
     * identifier is that item's outcome, and failing the job over it would discard the results of
     * every item already reindexed.
     */
    private void refresh(final WorkItem workItem, final User user,
            final boolean includeDependencies, final boolean recordItemResults) {

        if (workItem.unresolved()) {
            fail(workItem, workItem.resolutionError, recordItemResults);
            return;
        }

        try {
            // Cache first: a stale cache entry would otherwise be re-indexed as-is.
            this.contentletCache.remove(workItem.identifier);

            final Identifier identifier = this.identifierAPI.find(workItem.identifier);
            final List<Contentlet> versions =
                    this.contentletAPI.findAllVersions(identifier, false, user, false);

            for (final Contentlet version : versions) {
                // Without this the write is only enqueued, and "done" would be a lie.
                version.setIndexPolicy(IndexPolicy.WAIT_FOR);
                this.contentletIndexAPI.addContentToIndex(version, includeDependencies);
            }

            final int indexed = null == versions ? 0 : versions.size();
            this.versionsIndexed.addAndGet(indexed);
            this.successCount.incrementAndGet();

            if (recordItemResults) {
                this.itemResults.add(BulkRefreshItemResult.builder()
                        .identifier(workItem.identifier)
                        .inodes(workItem.inodes)
                        .status(BulkRefreshItemStatus.SUCCESS)
                        .versionsIndexed(indexed)
                        .build());
            }
        } catch (final Exception e) {
            Logger.warn(this, String.format("Unable to reindex identifier [%s]: %s",
                    workItem.identifier, e.getMessage()), e);
            fail(workItem, message(e), recordItemResults);
        }
    }

    private void fail(final WorkItem workItem, final String errorMessage,
            final boolean recordItemResults) {

        this.failedCount.incrementAndGet();
        if (recordItemResults) {
            this.itemResults.add(BulkRefreshItemResult.builder()
                    .identifier(Optional.ofNullable(workItem.identifier))
                    .inodes(workItem.inodes)
                    .status(BulkRefreshItemStatus.FAILED)
                    .errorMessage(errorMessage)
                    .build());
        }
    }

    private void skip(final WorkItem workItem, final boolean recordItemResults) {

        this.skippedCount.incrementAndGet();
        if (recordItemResults) {
            this.itemResults.add(BulkRefreshItemResult.builder()
                    .identifier(Optional.ofNullable(workItem.identifier))
                    .inodes(workItem.inodes)
                    .status(BulkRefreshItemStatus.SKIPPED)
                    .build());
        }
    }

    /**
     * Turns the submitted inodes into the units of work, collapsing several inodes of the same
     * identifier into one.
     * <p>
     * "Missing from search" is rarely confined to one language, so reindexing is done per identifier
     * across all its versions — three language rows of the same content are one reindex, not three.
     * Submission order is preserved so a client sees its rows settle roughly in the order it sent them.
     * An inode that no longer resolves becomes its own item and is reported as a failure, because a
     * selection can go stale between the click and the submit and that must not cost the caller the
     * rest of the batch.
     */
    private List<WorkItem> resolve(final List<String> inodes, final User user) {

        final Map<String, List<String>> inodesByIdentifier = new LinkedHashMap<>();
        final List<WorkItem> workItems = new ArrayList<>();

        for (final String inode : inodes) {

            String identifier = null;
            String error = null;
            try {
                final Contentlet contentlet = this.contentletAPI.find(inode, user, false);
                if (null == contentlet || !UtilMethods.isSet(contentlet.getIdentifier())) {
                    error = String.format("No contentlet found for inode %s", inode);
                } else {
                    identifier = contentlet.getIdentifier();
                }
            } catch (final Exception e) {
                error = String.format("Unable to resolve inode %s: %s", inode, message(e));
            }

            if (null == identifier) {
                workItems.add(new WorkItem(null, List.of(inode), error));
                continue;
            }

            final List<String> existing = inodesByIdentifier.get(identifier);
            if (null != existing) {
                // Same content, another language or version — one reindex covers all of them, but the
                // client still needs every inode named back so it can settle each selected row.
                existing.add(inode);
            } else {
                final List<String> collected = new ArrayList<>();
                collected.add(inode);
                inodesByIdentifier.put(identifier, collected);
                workItems.add(new WorkItem(identifier, collected, null));
            }
        }

        return workItems;
    }

    @Override
    public void validate(final Map<String, Object> parameters) throws JobValidationException {

        final Object contentletIds = null == parameters ? null : parameters.get(PARAM_CONTENTLET_IDS);
        if (!(contentletIds instanceof Collection) || ((Collection<?>) contentletIds).isEmpty()) {
            final String errorMessage = "A non-empty list of contentlet inodes is required";
            Logger.error(this.getClass(), errorMessage);
            throw new JobValidationException(errorMessage);
        }
    }

    @Override
    public void cancel(final Job job) throws JobCancellationException {

        Logger.info(this.getClass(), "Bulk refresh cancellation requested: " + job.id());
        this.cancellationRequested.set(true);
    }

    @Override
    public Map<String, Object> getResultMetadata(final Job job) {

        final Map<String, Object> metadata = new HashMap<>();
        metadata.put("total", this.total.get());
        metadata.put("processed", processed());
        metadata.put("successCount", this.successCount.get());
        metadata.put("failedCount", this.failedCount.get());
        metadata.put("skippedCount", this.skippedCount.get());
        metadata.put("versionsIndexed", this.versionsIndexed.get());
        metadata.put("includeDependencies", flag(job.parameters(), PARAM_INCLUDE_DEPENDENCIES));

        if (this.skippedCount.get() > 0) {
            metadata.put("skipReason", SKIP_REASON);
        }

        // Only when asked for: this map is persisted with the job, so a 500-entry array nobody
        // requested is storage spent on nothing. The terminal SSE event is built from here, though,
        // so when it is requested it has to be complete.
        if (flag(job.parameters(), PARAM_INCLUDE_ITEM_RESULTS)) {
            metadata.put("results", List.copyOf(this.itemResults));
        }

        return metadata;
    }

    private int processed() {
        return this.successCount.get() + this.failedCount.get() + this.skippedCount.get();
    }

    @SuppressWarnings("unchecked")
    private static List<String> contentletIds(final Map<String, Object> parameters) {
        final Object contentletIds = parameters.get(PARAM_CONTENTLET_IDS);
        return contentletIds instanceof Collection
                ? List.copyOf((Collection<String>) contentletIds)
                : List.of();
    }

    private static boolean flag(final Map<String, Object> parameters, final String name) {
        final Object value = null == parameters ? null : parameters.get(name);
        return value instanceof Boolean
                ? (Boolean) value
                : Boolean.parseBoolean(String.valueOf(value));
    }

    private User user(final Map<String, Object> parameters) {
        final String userId = String.valueOf(parameters.get(PARAM_USER_ID));
        try {
            return this.userAPI.loadUserById(userId);
        } catch (final Exception e) {
            throw new JobProcessingException("Unable to load the submitting user " + userId, e);
        }
    }

    /**
     * The root cause's message, localized the same way {@code ActionFail} does, so a per-item failure
     * names the actual problem rather than whatever wrapper it arrived in.
     */
    private static String message(final Exception e) {
        final Throwable rootCause = ExceptionUtil.getRootCause(e);
        return UtilMethods.isSet(rootCause.getMessage())
                ? rootCause.getMessage()
                : rootCause.toString();
    }

    /**
     * One unit of work: an identifier and every submitted inode that resolved to it, or — when nothing
     * resolved — the lone inode and why it failed.
     */
    private static final class WorkItem {

        private final String identifier;
        private final List<String> inodes;
        private final String resolutionError;

        WorkItem(final String identifier, final List<String> inodes,
                final String resolutionError) {
            this.identifier = identifier;
            this.inodes = inodes;
            this.resolutionError = resolutionError;
        }

        boolean unresolved() {
            return null == this.identifier;
        }
    }
}
