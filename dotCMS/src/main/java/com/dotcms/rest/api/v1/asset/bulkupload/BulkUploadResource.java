package com.dotcms.rest.api.v1.asset.bulkupload;

import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.util.SecurityUtils;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.media.multipart.BodyPart;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;

/**
 * Creates several assets from one multipart submission, as a background run (#37166).
 * <p>
 * <b>Why this endpoint exists</b>, since it does not upload anything the temp API could not:
 * content search already drops files through a {@code for} loop in a browser tab firing one
 * workflow action per file. Close the tab half way and the remaining files never happen and nobody
 * is told; when one fails, the author gets a count and not the names. This endpoint does the same N
 * ordinary single creates, tracked as <b>one job</b> — authoritative counts, per-file outcomes that
 * name the file and carry a machine-readable reason, progress, a result that is still there
 * tomorrow, and a run that can be cancelled or resumed.
 * <p>
 * Content import is the precedent for the shape: multipart in, the temp API called underneath, the
 * work handed to a job.
 *
 * @author dotCMS
 */
@Path("/v1/assets")
@Tag(name = "Bulk Upload", description = "Creating several assets in one background run")
@ApplicationScoped
public class BulkUploadResource {

    private static final String TEMP_RESOURCE_ENABLED = "TEMP_RESOURCE_ENABLED";

    private final WebResource webResource;
    private final BulkUploadHelper helper;

    /** Required by CDI for proxying, never called by this code — see {@link BulkUploadHelper}. */
    public BulkUploadResource() {
        this.webResource = new WebResource();
        this.helper = null;
    }

    @Inject
    public BulkUploadResource(final BulkUploadHelper helper) {
        this.webResource = new WebResource();
        this.helper = helper;
    }

    /**
     * Accepts a batch of files and answers immediately with a handle.
     */
    @POST
    @Path("/_bulkupload")
    @NoCache
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            operationId = "bulkUploadAssets",
            summary = "Create several assets from one upload, asynchronously",
            description = "Accepts several files plus a JSON `form` part naming the target and the "
                    + "base type, and answers `202` with a job handle **before the assets are "
                    + "created**. The work runs in the background: follow it with "
                    + "`GET /api/v1/jobs/{jobId}/status`, watch it with "
                    + "`GET /api/v1/jobs/{jobId}/monitor`, cancel it with "
                    + "`POST /api/v1/jobs/{jobId}/cancel`, and read the per-file outcome from the "
                    + "job's result. The submitting author is notified on any terminal state, so "
                    + "the outcome survives them navigating away.\n\n"
                    + "One file failing does not abort the batch: every remaining file is still "
                    + "attempted, and each failure is reported by name with a machine-readable "
                    + "reason.",
            responses = {
                    @ApiResponse(responseCode = "202", description = "Batch queued",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = BulkUploadSubmitResponse.class))),
                    @ApiResponse(responseCode = "400", description =
                            "No files; malformed or missing `form`; unsupported base type; neither "
                                    + "or both of folderId/siteId; more files than the configured "
                                    + "maximum; request not same-origin; staging disabled"),
                    @ApiResponse(responseCode = "403", description =
                            "The author may not add children to the target"),
                    @ApiResponse(responseCode = "404", description =
                            "The target folder or site does not exist"),
                    @ApiResponse(responseCode = "413", description =
                            "The batch is over the configured total size")
            })
    public Response bulkUpload(@Context final HttpServletRequest request,
                               @Context final HttpServletResponse response,
                               final FormDataMultiPart body)
            throws DotDataException, DotSecurityException, IOException {

        // Two controls that live on the staging layer's own REST resource and are therefore NOT
        // inherited by calling TempFileAPI directly. No global filter supplies them either: the
        // product's referer interceptor protects a fixed list of paths that does not include
        // /api/. Content import omits both, which is a gap to close here rather than copy.
        verifyStagingEnabled();
        if (!new SecurityUtils().validateReferer(request)) {
            throw new BadRequestException("Invalid Origin or referer");
        }

        final var initData = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();

        final BulkUploadForm form = readForm(body);
        final BulkUploadSubmitResponse submitted = helper.submit(
                form, filesOf(body), new TempFileBatchStaging(request), initData.getUser(),
                request);

        return Response.status(Response.Status.ACCEPTED)
                .entity(new ResponseEntityView<>(submitted))
                .build();
    }

    /**
     * Refuses when an operator has switched the staging layer off, rather than writing staged
     * content behind a switch they deliberately turned.
     */
    private void verifyStagingEnabled() {
        if (!Config.getBooleanProperty(TEMP_RESOURCE_ENABLED, true)) {
            Logger.error(this, "Bulk upload refused: the temp file resource is disabled");
            throw new BadRequestException(
                    "Temp Files Resource is not enabled; bulk upload requires it");
        }
    }

    /**
     * Reads the JSON {@code form} part. Its constructor validates the shape and throws, so a
     * malformed batch is refused before a single file part is touched.
     */
    private BulkUploadForm readForm(final FormDataMultiPart body) throws IOException {
        final FormDataBodyPart formPart = body.getField("form");
        if (formPart == null) {
            throw new BadRequestException("A JSON 'form' part is required");
        }
        return new ObjectMapper().readValue(formPart.getValue(), BulkUploadForm.class);
    }

    /**
     * The file parts, in the order they arrived — which is the order the author chose them, and
     * therefore the order the outcome reports.
     */
    private List<UploadPart> filesOf(final FormDataMultiPart body) {
        final List<BodyPart> fileParts = body.getFields("files") == null
                ? List.of() : new ArrayList<>(body.getFields("files"));

        final List<UploadPart> parts = new ArrayList<>(fileParts.size());
        for (final BodyPart part : fileParts) {
            final FormDataBodyPart formPart = (FormDataBodyPart) part;
            parts.add(new UploadPart(
                    formPart.getFormDataContentDisposition().getFileName(),
                    formPart.getValueAs(InputStream.class)));
        }
        return parts;
    }
}
