package com.dotcms.rest.api.v1.asset.bulkupload;

import com.dotcms.jobs.business.api.JobQueueManagerAPI;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import javax.servlet.http.HttpServletRequest;
import javax.inject.Inject;

/**
 * Validates a bulk-upload submission and enqueues the run (#37166, spec FR-003, FR-004, FR-013c).
 * <p>
 * <b>Order matters here, and it is not arbitrary.</b> Everything decidable without the body is
 * decided first — the form's shape, the target's existence, the author's rights, and a declared
 * total already over the ceiling. Only then is the body read, because reading it writes bytes to
 * shared storage and every check performed afterwards is a check performed too late.
 *
 * @author dotCMS
 */
@ApplicationScoped
public class BulkUploadHelper {

    static final String QUEUE_NAME = "assetBulkUpload";

    static final String MAX_FILES_KEY = "CONTENT_BULK_UPLOAD_MAX_FILES";
    static final String MAX_TOTAL_BYTES_KEY = "CONTENT_BULK_UPLOAD_MAX_TOTAL_BYTES";

    static final int DEFAULT_MAX_FILES = 100;
    static final long DEFAULT_MAX_TOTAL_BYTES = 1073741824L;

    private final JobQueueManagerAPI jobQueueManagerAPI;

    /**
     * Required by CDI, never called by this code.
     * <p>
     * {@code @ApplicationScoped} is a normal scope, so Weld injects a client proxy rather than the
     * bean, and building that proxy needs a no-args constructor. Without one the container fails
     * validation at deployment — {@code WELD-001435, not proxyable} — which does not degrade this
     * endpoint, it stops dotCMS from starting at all. {@code BulkRefreshHelper} keeps the same
     * constructor for the same reason.
     */
    public BulkUploadHelper() {
        this.jobQueueManagerAPI = null;
    }

    @Inject
    public BulkUploadHelper(final JobQueueManagerAPI jobQueueManagerAPI) {
        this.jobQueueManagerAPI = jobQueueManagerAPI;
    }

    /**
     * Reads the submission and enqueues the batch.
     *
     * @param form    the batch parameters, already shape-validated by its own constructor
     * @param parts   the file parts, read lazily so the ceilings can abort mid-body
     * @param staging where the content goes — the temp API in production
     * @param user    the submitting author; the run creates with their permissions and the
     *                completion is addressed to them
     * @return the run's handle. The work has not been done.
     * @throws DoesNotExistException        the target folder or site does not exist — {@code 404}
     * @throws DotSecurityException         the author may not add children there — {@code 403}
     * @throws BulkUploadRefusedException   a ceiling was crossed — {@code 400} or {@code 413}
     */
    public BulkUploadSubmitResponse submit(final BulkUploadForm form,
                                           final Iterable<UploadPart> parts,
                                           final BatchStaging staging,
                                           final User user,
                                           final HttpServletRequest request)
            throws DotDataException, DotSecurityException {

        final int maxFiles = Config.getIntProperty(MAX_FILES_KEY, DEFAULT_MAX_FILES);
        final long maxTotalBytes = Config.getLongProperty(MAX_TOTAL_BYTES_KEY,
                DEFAULT_MAX_TOTAL_BYTES);

        // 1. The target, and the right to write to it. Before the body, so an author who cannot
        //    use the folder is never made to upload into it first.
        final String targetId = resolveAndAuthorizeTarget(form, user);

        // 2. The courtesy refusal (FR-013c.1). Saves an author uploading gigabytes only to be
        //    refused. Never the enforcement point: a caller can under-declare or omit it, which is
        //    what step 3 exists for.
        if (form.getTotalSizeBytes() != null && form.getTotalSizeBytes() > maxTotalBytes) {
            throw new BulkUploadRefusedException(
                    BulkUploadRefusedException.Ceiling.TOTAL_SIZE,
                    String.format("Declared batch size %d exceeds the maximum of %d bytes",
                            form.getTotalSizeBytes(), maxTotalBytes));
        }

        // 3. The authoritative read (FR-010a, FR-013c.2). Aborts at whichever ceiling is crossed
        //    and reclaims what it staged; nothing purges staged content on a schedule.
        final List<StagedPart> staged =
                new BoundedMultipartReader(staging, maxFiles, maxTotalBytes).read(parts);

        if (staged.isEmpty()) {
            throw new BulkUploadRefusedException(
                    BulkUploadRefusedException.Ceiling.FILE_COUNT,
                    "A bulk upload requires at least one file");
        }

        final String jobId = jobQueueManagerAPI.createJob(QUEUE_NAME,
                jobParameters(form, staged, targetId, user, request));

        Logger.info(this, String.format(
                "Bulk upload job [%s] created by user [%s] for %d file(s) into [%s]",
                jobId, user.getUserId(), staged.size(), targetId));

        return BulkUploadSubmitResponse.builder()
                .jobId(jobId)
                .statusUrl("/api/v1/jobs/" + jobId + "/status")
                .submitted(staged.size())
                .build();
    }

    /**
     * Resolves the folder or site the batch targets and checks the author may add children to it.
     * <p>
     * <b>Permission is checked once, on the target, not per file.</b> Every file in the batch lands
     * in the same place, so a per-item loop would ask the same question N times — the O(N) pattern
     * ADR-0020 records as having caused multi-second responses on a comparable endpoint. The
     * narrower per-file checks the creation path performs are a different question and stay where
     * they are.
     */
    private String resolveAndAuthorizeTarget(final BulkUploadForm form, final User user)
            throws DotDataException, DotSecurityException {

        final PermissionAPI permissionAPI = APILocator.getPermissionAPI();

        if (UtilMethods.isSet(form.getFolderId())) {
            final Folder folder = APILocator.getFolderAPI()
                    .find(form.getFolderId(), user, false);
            if (folder == null || !UtilMethods.isSet(folder.getInode())) {
                throw new DoesNotExistException(
                        "Target folder does not exist: " + form.getFolderId());
            }
            if (!permissionAPI.doesUserHavePermission(folder,
                    PermissionAPI.PERMISSION_CAN_ADD_CHILDREN, user, false)) {
                throw new DotSecurityException(String.format(
                        "User [%s] may not add children to folder [%s]",
                        user.getUserId(), form.getFolderId()));
            }
            return folder.getIdentifier();
        }

        final Host site = APILocator.getHostAPI().find(form.getSiteId(), user, false);
        if (site == null || !UtilMethods.isSet(site.getIdentifier())) {
            throw new DoesNotExistException("Target site does not exist: " + form.getSiteId());
        }
        if (!permissionAPI.doesUserHavePermission(site,
                PermissionAPI.PERMISSION_CAN_ADD_CHILDREN, user, false)) {
            throw new DotSecurityException(String.format(
                    "User [%s] may not add children to site [%s]",
                    user.getUserId(), form.getSiteId()));
        }
        return site.getIdentifier();
    }

    /**
     * Builds the job's parameters.
     * <p>
     * The measured size and resolved media type are <b>copied in</b> rather than re-read when the
     * run starts. The batch total was accumulated from them before the batch existed, and if the
     * content later expires the run still knows what it was meant to be processing — so
     * {@code STAGED_CONTENT_UNAVAILABLE} can name the file instead of reporting an anonymous gap.
     */
    private Map<String, Object> jobParameters(final BulkUploadForm form,
                                              final List<StagedPart> staged,
                                              final String targetId,
                                              final User user,
                                              final HttpServletRequest request) {

        final List<Map<String, Object>> files = new ArrayList<>(staged.size());
        for (final StagedPart part : staged) {
            final Map<String, Object> file = new HashMap<>();
            file.put("tempFileId", part.tempFileId());
            file.put("fileName", part.fileName());
            file.put("sizeBytes", part.sizeBytes());
            file.put("mimeType", part.mimeType());
            files.add(file);
        }

        final Map<String, Object> parameters = new HashMap<>();
        parameters.put("baseType", form.getBaseType());
        parameters.put("targetId", targetId);

        // Only the target that was actually given. Exactly one of the two is set by definition, so
        // putting both would always carry one null — and the job framework stores parameters in an
        // ImmutableMap, which rejects null values. That does not fail this submission alone: the
        // insert succeeds and the failure surfaces later inside PostgresJobQueue.nextJob, which is
        // the *shared* processing loop, so one such job stops every queue from advancing.
        if (UtilMethods.isSet(form.getFolderId())) {
            parameters.put("folderId", form.getFolderId());
        } else {
            parameters.put("siteId", form.getSiteId());
        }
        parameters.put("userId", user.getUserId());
        parameters.put("stagedFiles", files);

        // Captured here because the run cannot obtain it later. The product's binary-field
        // strategy resolves a temp id from a thread-local HTTP request, which is null on a worker
        // thread, so the run retrieves its content by this fingerprint instead. Without it every
        // file in every batch would fail as unavailable.
        parameters.put("requestFingerprint",
                APILocator.getTempFileAPI().getRequestFingerprint(request));
        return parameters;
    }
}
