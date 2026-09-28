package com.dotcms.rest.api.v1.asset.bulkduplicate;

import com.dotcms.rest.ResponseEntityFolderBulkDuplicateSubmitView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotmarketing.exception.DotDataException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Duplicates several folders in place from one submission, as a background run (#37062).
 * <p>
 * Answers {@code 202} with a job handle before any folder is duplicated. Each folder's duplicate
 * lands beside it, in the same parent, and holds everything the source holds. A single folder is
 * submitted as a batch of one; there is no synchronous duplicate.
 *
 * @author dotCMS
 */
@Path("/v1/assets")
@Tag(name = "Web Assets")
@ApplicationScoped
public class FolderBulkDuplicateResource {

    private final WebResource webResource;
    private final FolderBulkDuplicateHelper helper;

    /** Required by CDI for proxying, never called by this code. */
    public FolderBulkDuplicateResource() {
        this.webResource = new WebResource();
        this.helper = null;
    }

    /**
     * @param helper validates and enqueues submissions
     */
    @Inject
    public FolderBulkDuplicateResource(final FolderBulkDuplicateHelper helper) {
        this.webResource = new WebResource();
        this.helper = helper;
    }

    /**
     * Accepts a selection of folder paths and answers immediately with a handle.
     *
     * @param request  the HTTP request
     * @param response the HTTP response
     * @param form     the selected folders
     * @return {@code 202} with the run's handle
     * @throws DotDataException the job could not be created
     */
    @POST
    @Path("/folders/_bulkduplicate")
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            operationId = "bulkDuplicateFolders",
            summary = "Duplicate several folders in place from one submission, asynchronously",
            description = "Accepts a list of site-qualified folder paths and answers `202` with a "
                    + "job handle **before any folder is duplicated**. The run is asynchronous, "
                    + "queued to the `folderBulkDuplicate` queue. Follow it with "
                    + "`GET /api/v1/jobs/{jobId}/status`, cancel it with "
                    + "`POST /api/v1/jobs/{jobId}/cancel`, and watch it with "
                    + "`GET /api/v1/jobs/{jobId}/monitor`. The submitting author is notified on "
                    + "any terminal state. The run's outcome stays readable from the job status "
                    + "indefinitely, since finished jobs are not purged, and the author's "
                    + "notification stays until they dismiss it. A cancelled run's outcome names "
                    + "the first folder it never reached, as `stoppedAt`.\n\n"
                    + "Each folder is duplicated in place, beside its original in the same "
                    + "parent, holding everything the original holds in the same state. The "
                    + "duplicate takes the original's name followed by `_copy`, with `_copy` "
                    + "appended again until the name is free. The outcome identifies each folder "
                    + "by the path that was submitted.\n\n"
                    + "One folder failing does not abort the run: every remaining folder is still "
                    + "attempted, and each failure is reported by path with a machine-readable "
                    + "reason.",
            responses = {
                    @ApiResponse(responseCode = "202", description = "Run queued",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityFolderBulkDuplicateSubmitView.class))),
                    @ApiResponse(responseCode = "400", description =
                            "No paths submitted, or more paths than the configured maximum"),
                    @ApiResponse(responseCode = "403", description =
                            "The caller is not entitled to use this operation")
            })
    public Response bulkDuplicate(@Context final HttpServletRequest request,
                                  @Context final HttpServletResponse response,
                                  final FolderBulkDuplicateForm form)
            throws DotDataException {

        final var initData = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();

        final FolderBulkDuplicateSubmitResponse submitted =
                helper.submit(form, initData.getUser());

        return Response.status(Response.Status.ACCEPTED)
                .entity(new ResponseEntityFolderBulkDuplicateSubmitView(submitted))
                .build();
    }
}
