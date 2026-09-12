package com.dotcms.jobs.business.processor.impl;

import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.business.BaseTypeToContentTypeStrategy;
import com.dotcms.contenttype.business.BaseTypeToContentTypeStrategyResolver;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotmarketing.beans.Host;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.DotAssetContentType;
import com.dotcms.jobs.business.batch.BatchFailureReason;
import com.dotcms.jobs.business.batch.BatchItemResult;
import com.dotcms.jobs.business.batch.BatchItemStatus;
import com.dotcms.jobs.business.error.JobCancellationException;
import com.dotcms.jobs.business.error.JobProcessingException;
import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.processor.Cancellable;
import com.dotcms.jobs.business.processor.JobProcessor;
import com.dotcms.jobs.business.processor.ProgressTracker;
import com.dotcms.jobs.business.processor.Queue;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletDependencies;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.util.UtilMethods;
import java.io.File;
import java.util.Optional;
import com.dotmarketing.util.Config;
import com.dotcms.util.ConversionUtils;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import com.dotcms.rest.api.v1.asset.bulkupload.BulkUploadReasonResolver;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.enterprise.context.Dependent;

/**
 * Creates the assets of one bulk-upload batch (#37166).
 * <p>
 * <b>Not marked {@code @NoRetryPolicy}, deliberately.</b> The abandoned-job sweep re-queues a
 * stalled run without consulting the retry policy, so marking this no-retry would not prevent a
 * second attempt — it would only leave that attempt unprepared for one.
 * <p>
 * <b>A re-queued run starts over, and that is now the accepted behaviour.</b> An
 * interrupted batch re-attempts every file. On {@code FILEASSET} the unique index on the
 * lower-cased path still rejects the second create, so no duplicate exists and only the report is
 * wrong — files this run created come back as collisions. On {@code DOTASSET} there is no such
 * index (FR-040b), so the files are genuinely created twice.
 *
 * @author dotCMS
 */
@Dependent
@Queue("assetBulkUpload")
public class BulkUploadProcessor implements JobProcessor, Cancellable {

    /**
     * The run's per-item outcome, held in memory for the life of the run.
     * <p>
     * <b>In memory, and therefore not resumable — deliberately.</b>
     * <p>
     * {@code CopyOnWriteArrayList} rather than a plain one, matching
     * {@code BulkRefreshContentletsProcessor}: {@code getResultMetadata} is called by the job
     * framework's thread, not the one running the batch.
     */
    private final List<BatchItemResult> itemResults = new CopyOnWriteArrayList<>();
    private final BulkUploadReasonResolver reasons = new BulkUploadReasonResolver();
    private final AtomicBoolean cancellationRequested = new AtomicBoolean(false);

    @Override
    public void process(final Job job) throws JobProcessingException {

        final Map<String, Object> parameters = job.parameters();
        final User user = user(parameters);
        final List<Map<String, Object>> stagedFiles = stagedFiles(parameters);

        final ProgressTracker progressTracker = job.progressTracker().orElseThrow(
                () -> new JobProcessingException(job.id(), "Progress tracker not found"));

        Logger.info(this, String.format(
                "Bulk upload job [%s]: %d file(s) for user [%s]",
                job.id(), stagedFiles.size(), user.getUserId()));

        final List<String> createdInodes = new ArrayList<>();

        for (int seq = 0; seq < stagedFiles.size(); seq++) {

            if (cancellationRequested.get()) {
                // Cancellation takes effect between files, never mid-file, so nothing is left
                // half-created. What was never reached is SKIPPED, which is a distinct outcome from
                // FAILED: those files were not rejected, they were simply not tried.
                recordRemainderAsSkipped(job, stagedFiles, seq);
                break;
            }

            createOne(job, stagedFiles.get(seq), seq, user, createdInodes);
            progressTracker.updateProgress((seq + 1) / (float) stagedFiles.size());
        }

        // FR-033 — release the staged content, whatever terminal state this run reached, and
        // including the files it never got to. Cheap to overlook because it is the happy path: the
        // reclaim was written for the two failure routes and both were tested, while a run that
        // simply succeeds reaches a terminal state too. Nothing purges staged content on a
        // schedule, so a batch that skipped this leaked its own bytes permanently.
        reclaimStagedContent(job, stagedFiles, user);

        // FR-008a — resolve index visibility ONCE for the batch, and before the completion signal.
        // Per-file WAIT_FOR would not merely block on a refresh; it also flushes the system-wide
        // query cache on every file, charging every other user for this batch. But DEFER alone only
        // enqueues into the reindex journal, so a run reporting "finished" would hand the author
        // files a text search cannot yet find. Content Drive lists folders from the database, so the
        // grid is fine either way — free-text and searchable-field criteria are what need this.
        resolveIndexVisibility(job, createdInodes);

        progressTracker.updateProgress(1.0f);

        // FR-019 — say how the run ended, not only that it began. Without this an operator reading
        // logs sees a batch start and nothing after it, and cannot tell a run that finished from
        // one that stalled: both look identical.
        logTerminalState(job, stagedFiles.size());
    }

    /**
     * Reports the run's terminal state and its counts, once, at the end (FR-019).
     * <p>
     * Best-effort like the author-facing notification: a batch that created its files must not be
     * failed by the log line that describes it.
     */
    private void logTerminalState(final Job job, final int submitted) {
        // Cancellation is the author's own choice, so it is reported as a distinct ending rather
        // than folded into "finished" — the two mean different things to whoever is reading the
        // log to find out why a batch is short.
        final String ending = cancellationRequested.get() ? "CANCELED" : "COMPLETED";

        Logger.info(this, String.format(
                "Bulk upload job [%s]: %s - %d submitted, %d created, %d failed, %d skipped",
                job.id(), ending, submitted,
                countOf(itemResults, BatchItemStatus.SUCCESS),
                countOf(itemResults, BatchItemStatus.FAILED),
                countOf(itemResults, BatchItemStatus.SKIPPED)));
    }

    private long countOf(final List<BatchItemResult> results, final BatchItemStatus status) {
        return results.stream().filter(r -> r.status() == status).count();
    }

    /**
     * Creates one asset and records its outcome, whatever that outcome is.
     * <p>
     * <b>Every exit from this method records something.</b> A file that is refused, that fails, or
     * that cannot even be read still leaves a per-item result behind, because the whole feature
     * exists so the author is told what happened to each of their files — a silent gap in the
     * outcome is the one failure mode there is no recovering from.
     * <p>
     * The record is held in memory for the life of the run, so it does not survive an interruption
     * (FR-036a) — see {@link #getResultMetadata(Job)}, where that decision is set out.
     */
    private void createOne(final Job job, final Map<String, Object> file, final int seq,
            final User user, final List<String> createdInodes) {

        final String fileName = String.valueOf(file.get("fileName"));
        final String tempFileId = String.valueOf(file.get("tempFileId"));

        try {
            // A part the submission already refused. Recorded and skipped: there is no temp id to
            // fetch and nothing to create, and re-deciding it here would need a measurement that
            // was never completed. Checked before everything else for that reason.
            final Object refusedAtSubmission = file.get("refusedReason");
            if (refusedAtSubmission != null) {
                record(job, seq, fileName, BatchItemStatus.FAILED,
                        BatchFailureReason.valueOf(String.valueOf(refusedAtSubmission)),
                        "Refused by the staging layer's per-file ceiling while the body was read",
                        null);
                return;
            }

            // The content is resolved FIRST, because the content type is routed from it.
            //
            // This used to come after the size and type pre-check, which forced that check to run
            // against a hardcoded generic type — see resolveContentType. Reordering costs nothing:
            // getTempFile hands back a handle, not a copy.
            final File binary = resolveStagedContent(job, tempFileId, user)
                    .orElseThrow(() -> new StagedContentUnavailableException(tempFileId));

            final boolean isFileAsset = isFileAsset(job.parameters());

            final Contentlet contentlet = new Contentlet();

            // The two base types name their binary field differently, and getting it wrong fails
            // with "Unable to get The Asset From the Given dotAsset Contentlet" — a message that
            // does not mention the field, so it is worth naming here. A dotAsset carries 'asset'
            // and derives its title from the file; a fileAsset carries 'fileAsset' and needs the
            // title and file name set explicitly.
            if (isFileAsset) {
                contentlet.setBinary(FileAssetAPI.BINARY_FIELD, binary);
                contentlet.setStringProperty(FileAssetAPI.TITLE_FIELD, fileName);
                contentlet.setStringProperty(FileAssetAPI.FILE_NAME_FIELD, fileName);
            } else {
                contentlet.setBinary(DotAssetContentType.ASSET_FIELD_VAR, binary);
            }
            applyTarget(contentlet, job.parameters(), user);

            // Routed by the binary's media type, the same way every other creation path does it.
            final ContentType contentType = resolveContentType(job, contentlet, user);
            contentlet.setContentTypeId(contentType.id());

            // Decided from what staging measured, before anything is created (research R4). The
            // validation layer reports an over-size file and a disallowed type through the same
            // exception class, differing only by a translated string, so a reason recovered from
            // it would be a guess. Here both are facts.
            final Optional<BatchFailureReason> refused = reasons.preCheck(
                    sizeOf(file),
                    (String) file.get("mimeType"),
                    effectiveCeiling(contentType),
                    acceptedTypes(contentType));

            if (refused.isPresent()) {
                record(job, seq, fileName, BatchItemStatus.FAILED, refused.get(),
                        "Refused before creation by the file's measured size or resolved type",
                        null);
                return;
            }

            // What the TARGET FOLDER refuses is decided here rather than by letting the create
            // fail — both its filename filter and a name already taken.
            //
            // NOT for correctness: the unique index below is still the authority on a name, and has
            // to be, because two batches racing for one name (FR-042) can both pass this check and
            // only one can win. This is about what the expensive path costs and what it leaves
            // behind. A resubmission is a NORMAL outcome this feature supports (FR-040a), and
            // resolving it by exception meant a full workflow fire and validation per file, each
            // logging its rejection at ERROR through WorkflowAPIImpl.
            //
            // For the folder filter it is not only cheaper, it is the only way to name the reason
            // at all: the product reports a filter mismatch and a name collision through the same
            // exception class AND the same invalid field (hostFolder), differing only by a
            // translated message — so a filter mismatch recovered from the exception was reported
            // to the author as NAME_COLLISION, telling them to rename a file whose name was never
            // the problem.
            final Optional<BatchFailureReason> refusedByFolder =
                    folderRefusal(job.parameters(), user, fileName);
            if (refusedByFolder.isPresent()) {
                record(job, seq, fileName, BatchItemStatus.FAILED, refusedByFolder.get(),
                        "Refused by the target folder before creation", null);
                return;
            }

            // Created, then published — two steps, the way content import does it
            // (ImportUtil#runWorkflowIfCould and #runWorkflowPublishIfCould).
            //
            // An earlier version fired PUBLISH directly, with a gate requiring the mapped action
            // to both save and publish. It produced published files, and it skipped the content
            // type's NEW action entirely — so a customer whose NEW action notifies someone or sets
            // a field lost that, silently, because a different action did the saving. Two steps
            // honour both mappings, which is why import is shaped this way.
            final Contentlet created = publish(job, createWith(contentlet, contentType, user),
                    user);

            recordCreated(job, seq, fileName, created, createdInodes);

        } catch (final StagedContentUnavailableException e) {
            // Not the author's fault: the content expired or could not be read. Named explicitly
            // so the copy never suggests they supplied a bad file (FR-032).
            record(job, seq, fileName, BatchItemStatus.FAILED,
                    BatchFailureReason.STAGED_CONTENT_UNAVAILABLE, e.getMessage(), null);
        } catch (final Exception e) {
            // The exception class travels with the diagnostic message. It is never shown to the
            // author, and it is the first thing anyone needs when an UNCLASSIFIED turns up — which
            // by design means something nobody anticipated.
            record(job, seq, fileName, BatchItemStatus.FAILED, reasons.classify(e),
                    e.getClass().getName() + ": " + e.getMessage(), null);
        }
    }

    /**
     * Creates the contentlet through the content type's own {@code NEW} mapping.
     * <p>
     * Mirrors {@code ImportUtil#runWorkflowIfCould}: fire the mapped action when it saves,
     * otherwise check in directly. Firing it is what runs the content type's own actionlets, which
     * is the whole reason to ask the mapping rather than always checking in.
     * <p>
     * {@code DISABLE_WORKFLOW} on the fallback because {@code checkin} looks {@code NEW} up itself
     * and would fire the very action just rejected — import's own comment on the same line calls it
     * "needed to avoid recursive call".
     */
    private Contentlet createWith(final Contentlet contentlet,
            final ContentType contentType, final User user)
            throws DotDataException, DotSecurityException {

        final Optional<WorkflowAction> saveAction = APILocator.getWorkflowAPI()
                .findActionMappedBySystemActionContentlet(
                        contentlet, WorkflowAPI.SystemAction.NEW, user)
                .filter(WorkflowAction::hasSaveActionlet);

        if (saveAction.isEmpty()) {
            Logger.debug(this, String.format(
                    "No NEW action saves for [%s]; checking in directly", contentType.variable()));
            contentlet.setBoolProperty(Contentlet.DISABLE_WORKFLOW, true);
            return APILocator.getContentletAPI().checkin(contentlet, user, false);
        }

        return APILocator.getWorkflowAPI().fireContentWorkflow(contentlet,
                new ContentletDependencies.Builder()
                        .modUser(user)
                        .workflowActionId(saveAction.get().getId())
                        .respectAnonymousPermissions(false)
                        // DEFER, never WAIT_FOR: the per-file wait also flushes the system-wide
                        // query cache, so a full batch would charge every other user one flush per
                        // file. The batch resolves visibility once, at the end, before the
                        // completion signal (FR-008a).
                        .indexPolicy(IndexPolicy.DEFER)
                        .indexPolicyDependencies(IndexPolicy.DEFER)
                        .build());
    }

    /**
     * Publishes what was just created, through the content type's own {@code PUBLISH} mapping
     * (FR-006a).
     * <p>
     * Mirrors {@code ImportUtil#runWorkflowPublishIfCould}: fire the mapped action when it
     * publishes, otherwise publish directly.
     * <p>
     * <b>{@code DISABLE_WORKFLOW} on the direct call is not belt-and-braces.</b> {@code publish()}
     * is not the direct operation it reads as — {@code checkAndRunPublishAsWorkflow}
     * ({@code ESContentletAPIImpl:5686}) resolves {@code PUBLISH} and runs it as a workflow
     * instead. This branch is entered exactly when that mapping does not publish, so without the
     * flag the call fires the same non-publishing action and leaves the file saved but not live: a
     * run reporting success having done half the job.
     */
    private Contentlet publish(final Job job, final Contentlet created, final User user)
            throws DotDataException, DotSecurityException {

        if (created == null || !UtilMethods.isSet(created.getIdentifier())) {
            // Nothing to publish, and recordCreated will reject it in a moment with a message that
            // names the file. Returned as-is rather than guessed at.
            return created;
        }

        final Optional<WorkflowAction> publishAction = APILocator.getWorkflowAPI()
                .findActionMappedBySystemActionContentlet(
                        created, WorkflowAPI.SystemAction.PUBLISH, user)
                .filter(WorkflowAction::hasPublishActionlet);

        if (publishAction.isPresent()) {
            return APILocator.getWorkflowAPI().fireContentWorkflow(created,
                    new ContentletDependencies.Builder()
                            .modUser(user)
                            .workflowActionId(publishAction.get().getId())
                            .respectAnonymousPermissions(false)
                            .indexPolicy(IndexPolicy.DEFER)
                            .indexPolicyDependencies(IndexPolicy.DEFER)
                            .build());
        }

        Logger.debug(this, String.format(
                "No PUBLISH action publishes for job [%s]; publishing directly", job.id()));

        created.setBoolProperty(Contentlet.DISABLE_WORKFLOW, true);
        APILocator.getContentletAPI().publish(created, user, false);
        created.getMap().remove(Contentlet.DISABLE_WORKFLOW);
        return created;
    }

    /**
     * Records one file as created, having first confirmed that it was.
     * <p>
     * <b>Never report a success we cannot point at.</b> The whole feature exists so the author is
     * told what actually happened, so "created" has to mean a contentlet that exists — and this is
     * not hypothetical: {@code fireContentWorkflow} with an unresolved action logs an error,
     * creates nothing and returns normally, which once had every file in a batch recorded SUCCESS
     * against an empty folder.
     * <p>
     * Shared by both creation routes — the workflow fire and the direct checkin — so neither can
     * grow its own idea of what counts as created.
     */
    private void recordCreated(final Job job, final int seq, final String fileName,
            final Contentlet created, final List<String> createdInodes) {

        if (created == null || !UtilMethods.isSet(created.getIdentifier())) {
            throw new IllegalStateException("No persisted contentlet was returned for " + fileName);
        }

        createdInodes.add(created.getInode());
        record(job, seq, fileName, BatchItemStatus.SUCCESS, null, null, created.getIdentifier());
    }

    /**
     * Retrieves the staged content <b>by the fingerprint captured at submission</b>, not through
     * the request.
     * <p>
     * <b>This is why the fingerprint is a job parameter.</b> The product's own binary-field
     * strategy resolves a temp id via {@code HttpServletRequestThreadLocal.INSTANCE.getRequest()},
     * which is null on a worker thread — a run has no HTTP request, and by the time it executes the
     * submitting one is long closed. Setting the temp id as a field value and letting that strategy
     * resolve it would therefore fail for every file in every batch. The {@code accessingList}
     * overload exists for exactly this, so the run resolves the file itself and hands the creation
     * path a real {@link File}.
     */
    private Optional<File> resolveStagedContent(final Job job, final String tempFileId,
            final User user) {
        final List<String> accessingList = accessingList(job, user);
        return APILocator.getTempFileAPI().getTempFile(accessingList, tempFileId)
                .map(tempFile -> tempFile.file);
    }

    /**
     * Raised when staged content cannot be retrieved, so it is reported as its own reason.
     */
    private static class StagedContentUnavailableException extends RuntimeException {

        StagedContentUnavailableException(final String tempFileId) {
            super("Staged content is no longer available: " + tempFileId);
        }
    }

    /**
     * The ceiling that applies to one file, in FR-011's order.
     * <p>
     * The content type's own {@code maxFileLength} wins wherever an operator declared one, so a
     * file is accepted or rejected identically whether it arrives alone or in a batch. The
     * configured fallback applies only where none is declared — which is the default, and without
     * it the batch would have no per-file bound at all. That fallback is the one place a batch is
     * deliberately stricter than a single upload (FR-011a), recorded rather than discovered.
     */
    private long effectiveCeiling(final ContentType contentType) {

        final long declared = binaryField(contentType)
                .flatMap(field -> field.fieldVariableValue(BinaryField.MAX_FILE_LENGTH))
                .map(value -> ConversionUtils.toLongFromByteCountHumanDisplaySize(value, -1L))
                .orElse(-1L);

        return declared > 0 ? declared : Config.getLongProperty(
                "CONTENT_BULK_UPLOAD_FALLBACK_MAX_FILE_BYTES", 209715200L);
    }

    /**
     * The content type's allow list, empty when it declares none — which means "everything".
     */
    private List<String> acceptedTypes(final ContentType contentType) {
        return binaryField(contentType)
                .flatMap(field -> field.fieldVariableValue(BinaryField.ALLOWED_FILE_TYPES))
                .filter(UtilMethods::isSet)
                .map(value -> Arrays.asList(value.split(",")))
                .orElse(List.of());
    }

    /**
     * The binary field the batch writes into — 'asset' for a dotAsset, 'fileAsset' otherwise.
     */
    private Optional<Field> binaryField(final ContentType contentType) {
        return contentType.fields().stream()
                .filter(field -> field instanceof BinaryField)
                .findFirst();
    }

    private long sizeOf(final Map<String, Object> file) {
        final Object size = file.get("sizeBytes");
        return size instanceof Number ? ((Number) size).longValue() : 0L;
    }

    /**
     * Resolves the <b>specific</b> content type this file should become, from its media type.
     * <p>
     * Routed through the product's own {@code BaseTypeToContentTypeStrategyResolver} — the same one
     * {@code ESContentletAPIImpl#checkOrSetContentType} uses — rather than a matcher of our own, so
     * a batch and a single upload of the same file land on the same type by construction rather
     * than by two implementations agreeing.
     * <p>
     * <b>The context map is built by hand because the run has no HTTP request.</b> The product's
     * caller reads the request thread-local for the session id and the temp fingerprint; a worker
     * thread has neither, so the accessing list is assembled from the job's own parameters, the
     * same way {@link #resolveStagedContent} does. The binary is already set on the contentlet as a
     * real {@link File}, which the strategies accept directly, so nothing here depends on
     * re-resolving a temp id.
     * <p>
     * Falls back to the base type's default when nothing matches — which is what the strategies
     * themselves do, and what the previous behaviour was for every file.
     */
    private ContentType resolveContentType(final Job job, final Contentlet contentlet,
            final User user)
            throws DotDataException, DotSecurityException {

        final boolean isFileAsset = isFileAsset(job.parameters());
        final BaseContentType baseType =
                isFileAsset ? BaseContentType.FILEASSET : BaseContentType.DOTASSET;

        final Optional<ContentType> routed = routeByMediaType(job, contentlet, user, baseType);
        if (routed.isPresent()) {
            return routed.get();
        }

        final String fallback = isFileAsset
                ? FileAssetAPI.DEFAULT_FILE_ASSET_STRUCTURE_VELOCITY_VAR_NAME
                : "dotAsset";
        return APILocator.getContentTypeAPI(user).find(fallback);
    }

    /**
     * Asks the product's strategy for the specific type, and never lets that question fail a file.
     * <p>
     * A routing failure degrades to the base type's default — which is where every file landed
     * before this existed, so the worst case is the previous behaviour rather than a lost file.
     */
    private Optional<ContentType> routeByMediaType(final Job job, final Contentlet contentlet,
            final User user,
            final BaseContentType baseType) {
        try {
            final Optional<BaseTypeToContentTypeStrategy> strategy =
                    BaseTypeToContentTypeStrategyResolver.getInstance().get(baseType);
            if (strategy.isEmpty()) {
                return Optional.empty();
            }

            final Host host = APILocator.getHostAPI().find(contentlet.getHost(), user, false);
            if (null == host) {
                return Optional.empty();
            }

            final List<String> accessingList = accessingList(job, user);

            return strategy.get().apply(baseType, Map.of(
                    "user", user,
                    "host", host,
                    "contentletMap", contentlet.getMap(),
                    "accessingList", accessingList));

        } catch (final Exception e) {
            Logger.warn(this, String.format(
                    "Bulk upload job [%s]: could not route '%s' by media type, falling back to the "
                            + "base type's default: %s",
                    job.id(), contentlet.getTitle(), e.getMessage()));
            return Optional.empty();
        }
    }

    /**
     * Places the asset in the folder, or at the site root when the batch targets a site.
     */
    private void applyTarget(final Contentlet contentlet, final Map<String, Object> parameters,
            final User user) throws DotDataException, DotSecurityException {
        final Object folderId = parameters.get("folderId");
        if (folderId != null) {
            final Folder folder = APILocator.getFolderAPI()
                    .find(String.valueOf(folderId), user, false);
            contentlet.setHost(folder.getHostId());
            contentlet.setFolder(folder.getInode());
            return;
        }
        contentlet.setHost(String.valueOf(parameters.get("siteId")));
        contentlet.setFolder(Folder.SYSTEM_FOLDER);
    }

    /**
     * What the target folder refuses about this file, asked before the create is attempted.
     * <p>
     * Two rules, resolved together because they need the same folder and the same lookup order:
     * <ul>
     *   <li><b>The folder's filename filter</b> ({@code filesMasks}). Applies to both base types —
     *       {@code validateFileAsset} and {@code validateDotAsset} both call
     *       {@code FolderAPI.matchFilter}.</li>
     *   <li><b>A name already taken</b>, <b>case-insensitively</b> (FR-042a): {@code Report.pdf}
     *       and {@code report.pdf} are one contended name, and {@code fileNameExists} resolves
     *       through the same lower-cased identifier the unique index is built on.</li>
     * </ul>
     * <b>Filter first</b>, deliberately. A file can break both, carries only one reason, and the
     * folder filter is the one the author can act on without knowing what else is in the folder —
     * "this folder only takes .jpg" is actionable; "that name is taken" sends them to rename a file
     * the folder would have refused anyway.
     * <p>
     * <b>Only the name check is FILEASSET-only.</b> A dotAsset does not carry a file name the way a
     * fileAsset does — its title is derived from the binary — so there is no equivalent lookup and
     * the create remains the only answer for that base type. Answering "no refusal" here is
     * therefore not a claim that the name is free; it means "not decided yet", which is safe
     * precisely because this check never had authority.
     * <p>
     * <b>Never allowed to fail the file.</b> If a lookup itself errors, this yields to the create
     * rather than inventing a refusal: a diagnostic query must not be able to reject an author's
     * file.
     */
    private Optional<BatchFailureReason> folderRefusal(final Map<String, Object> parameters,
            final User user, final String fileName) {
        try {
            final Object folderId = parameters.get("folderId");
            if (folderId == null) {
                // A site-rooted batch targets SYSTEM_FOLDER, which carries no filter and which
                // fileNameExists does not resolve the way it resolves a real folder. Left to the
                // creation.
                return Optional.empty();
            }

            final Folder folder = APILocator.getFolderAPI()
                    .find(String.valueOf(folderId), user, false);

            if (!APILocator.getFolderAPI().matchFilter(folder, fileName)) {
                return Optional.of(BatchFailureReason.FOLDER_FILTER_MISMATCH);
            }

            if (!isFileAsset(parameters)) {
                return Optional.empty();
            }

            return APILocator.getFileAssetAPI().fileNameExists(
                    APILocator.getHostAPI().find(folder.getHostId(), user, false),
                    folder, fileName)
                    ? Optional.of(BatchFailureReason.NAME_COLLISION)
                    : Optional.empty();

        } catch (final Exception e) {
            Logger.debug(this, String.format(
                    "Could not pre-check the target folder for '%s'; leaving it to the create: %s",
                    fileName, e.getMessage()));
            return Optional.empty();
        }
    }

    /**
     * Releases every file's staged content once the run is done with it.
     * <p>
     * <b>Every file, not only the ones that succeeded.</b> A cancelled run leaves items it never
     * reached, and those are the likeliest to be forgotten precisely because no per-item outcome
     * was written for them — there is no row pointing at what to clean up. Driven from the job's
     * own parameters instead, which list every file the batch was given.
     * <p>
     * Best-effort per file: one failure must not stop the rest, and none of it may fail a run whose
     * work is already done and recorded. A file that cannot be released is logged loudly, because
     * nothing else will ever collect it.
     */
    private void reclaimStagedContent(final Job job, final List<Map<String, Object>> stagedFiles,
            final User user) {
        for (final Map<String, Object> file : stagedFiles) {
            if (file.get("tempFileId") == null) {
                // Refused before staging — nothing of it reached the staging layer, so there is
                // nothing to release and asking would log a spurious failure.
                continue;
            }
            final String tempFileId = String.valueOf(file.get("tempFileId"));
            try {
                resolveStagedContent(job, tempFileId, user).ifPresent(binary -> {
                    if (binary.exists() && !binary.delete()) {
                        Logger.warn(this, String.format(
                                "Bulk upload job [%s]: could not delete staged content '%s'; "
                                        + "nothing purges it on a schedule, so it will remain",
                                job.id(), tempFileId));
                    }
                });
            } catch (final Exception e) {
                Logger.warn(this, String.format(
                        "Bulk upload job [%s]: could not reclaim staged content '%s': %s",
                        job.id(), tempFileId, e.getMessage()), e);
            }
        }
    }

    /**
     * Makes every asset this run created visible to search, in one pass.
     */
    private void resolveIndexVisibility(final Job job, final List<String> createdInodes) {
        if (createdInodes.isEmpty()) {
            return;
        }
        try {
            Logger.info(this, String.format(
                    "Bulk upload job [%s]: resolving index visibility for %d asset(s)",
                    job.id(), createdInodes.size()));

            final List<Contentlet> created = APILocator.getContentletAPI()
                    .findContentlets(createdInodes);

            // One WAIT_FOR for the whole batch instead of N. This is the half that DEFER alone
            // cannot provide: DEFER only enqueues into the reindex journal, so without this a run
            // reporting "finished" would hand the author files a text search cannot yet find.
            created.forEach(contentlet -> contentlet.setIndexPolicy(IndexPolicy.WAIT_FOR));
            APILocator.getContentletIndexAPI().addContentToIndex(created);
        } catch (final Exception e) {
            Logger.error(this, String.format(
                    "Bulk upload job [%s]: could not resolve index visibility: %s",
                    job.id(), e.getMessage()), e);
        }
    }

    private void recordRemainderAsSkipped(final Job job, final List<Map<String, Object>> files,
            final int from) {
        for (int seq = from; seq < files.size(); seq++) {
            record(job, seq, String.valueOf(files.get(seq).get("fileName")),
                    BatchItemStatus.SKIPPED, null, null, null);
        }
    }

    /**
     * Records one item's outcome.
     * <p>
     * {@code seq} is kept in the signature and unused for storage: the list is appended in
     * submission order, which is the order FR-015 reports in. It stays because every caller already
     * knows it and a future durable store would need it back.
     */
    private void record(final Job job, final int seq, final String key,
            final BatchItemStatus status, final BatchFailureReason reason,
            final String message, final String refId) {

        final BatchItemResult.Builder builder = BatchItemResult.builder()
                .key(key)
                .status(status);

        // Never set to null: the shared type models both as Optional, and an explicit null would
        // be a different thing from absent to anything reading it back.
        if (reason != null) {
            builder.reason(reason);
        }
        if (message != null) {
            builder.message(message);
        }

        itemResults.add(builder.build());
    }

    @Override
    public void cancel(final Job job) throws JobCancellationException {
        Logger.info(this, "Cancellation requested for bulk upload job " + job.id());
        cancellationRequested.set(true);
    }

    /**
     * The batch outcome, built from what this run recorded as it went.
     * <p>
     * <b>Held in memory, which is why it does not survive an interruption.</b> This previously
     * read
     * a durable per-item table, and that table is what made a re-queued run able to skip files it
     * had already created. It was removed (#37166): the decision was that one feature should not
     * carry a private store for state the job framework does not offer. FR-036 … FR-038 and SC-009
     * were withdrawn from the spec with it, and a run that is interrupted now restarts from the
     * first file — for a FILEASSET batch the unique index still prevents a second copy and only the
     * report is wrong, but a DOTASSET batch has no such index (FR-040b) and genuinely duplicates.
     * <p>
     * Same shape as {@code BulkRefreshContentletsProcessor}, which is the precedent this now
     * follows exactly.
     */
    @Override
    public Map<String, Object> getResultMetadata(final Job job) {

        final Map<String, Object> metadata = new HashMap<>();
        final List<BatchItemResult> results = new ArrayList<>(itemResults);

        final long success = countOf(results, BatchItemStatus.SUCCESS);
        final long failed = countOf(results, BatchItemStatus.FAILED);
        final long skipped = countOf(results, BatchItemStatus.SKIPPED);

        metadata.put("total", results.size());
        metadata.put("processed", success + failed);
        metadata.put("successCount", success);
        metadata.put("failedCount", failed);
        metadata.put("skippedCount", skipped);
        metadata.put("results", results);

        // Contract §3. Lets the client report "already uploaded" instead of "everything failed" —
        // the two look identical in the counts, because a duplicate collides on every file, and
        // only this tells them apart (FR-040a).
        metadata.put("duplicateSubmission", job.parameters().containsKey("duplicateOfJobId"));

        return metadata;
    }

    /**
     * Whether this batch creates fileAssets. The two base types differ in enough places to name.
     */
    private boolean isFileAsset(final Map<String, Object> parameters) {
        return "FILEASSET".equals(parameters.get("baseType"));
    }

    /**
     * Who the staging layer will accept as the owner of this run's content.
     * <p>
     * <b>Built from the job, never from a request.</b> The run has none — that is why the
     * submission captures a fingerprint into the job's parameters in the first place. Shared by
     * every caller so the two cannot drift: content resolved under one list and routed under a
     * different one would fail in ways that look like the file being missing.
     */
    private List<String> accessingList(final Job job, final User user) {
        final List<String> accessingList = new ArrayList<>();
        accessingList.add(user.getUserId());
        final Object fingerprint = job.parameters().get("requestFingerprint");
        if (null != fingerprint) {
            accessingList.add(String.valueOf(fingerprint));
        }
        return accessingList;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> stagedFiles(final Map<String, Object> parameters) {
        final Object files = parameters.get("stagedFiles");
        return files instanceof List ? (List<Map<String, Object>>) files : List.of();
    }

    private User user(final Map<String, Object> parameters) {
        try {
            return APILocator.getUserAPI()
                    .loadUserById(String.valueOf(parameters.get("userId")));
        } catch (final Exception e) {
            throw new IllegalStateException("Could not resolve the submitting user", e);
        }
    }
}
