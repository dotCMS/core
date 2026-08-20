package com.dotcms.rest.api.v1.content.bulkrefresh;

import com.dotcms.jobs.business.job.JobView;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityBulkRefreshSubmitView;
import com.dotcms.rest.ResponseEntityStringView;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Reindexes a selection of contentlets — the bulk counterpart of
 * {@code PUT /api/v1/content/_refresh/{identifierOrInode}}, which is unchanged.
 * <p>
 * <b>This is not a full index rebuild.</b> {@code POST /api/v1/esindex/reindex} rebuilds the entire
 * index and is a different operation; nothing here touches it.
 * <p>
 * Job-backed: the {@code POST} returns a {@code jobId} and a status URL, and the client polls that URL
 * until the job reaches a terminal state. A reload can reattach to a run in flight, since the job id is
 * all that is needed to follow it.
 * <p>
 * Reindexing is <i>not</i> modelled as a workflow action or a {@code SystemAction}: bulk fire resolves
 * its target set by searching the index, which is circular for an operation whose whole purpose is to
 * fix the index — content missing from the index cannot be found by an index search, so the items that
 * most need this would be exactly the ones such a path could never reach.
 *
 * @author dotCMS
 */
@Path("/v1/content/_bulkrefresh")
@Tag(name = "Content", description = "Bulk reindex of selected contentlets")
public class BulkRefreshResource {

    private final WebResource webResource;
    private final BulkRefreshHelper bulkRefreshHelper;

    @Inject
    public BulkRefreshResource(final BulkRefreshHelper bulkRefreshHelper) {
        this(new WebResource(), bulkRefreshHelper);
    }

    public BulkRefreshResource(final WebResource webResource,
            final BulkRefreshHelper bulkRefreshHelper) {
        this.webResource = webResource;
        this.bulkRefreshHelper = bulkRefreshHelper;
    }

    /**
     * Submits a selection of contentlets to be reindexed.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "bulkRefreshContent", summary = "Reindex a selection of contentlets",
            description = "Clears the contentlet cache and reindexes every selected contentlet, all "
                    + "versions of each. Returns a job id plus the URLs to follow the run: this is "
                    + "accepted work, not finished work. Not a full index rebuild.",
            tags = {"Content"},
            responses = {
                    @ApiResponse(responseCode = "202", description = "Accepted - reindex job enqueued",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityBulkRefreshSubmitView.class))),
                    @ApiResponse(responseCode = "400", description = "Bad request - empty selection or over the configured item cap"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized - no backend user session"),
                    @ApiResponse(responseCode = "403", description = "Forbidden - not a CMS Power User or CMS Administrator"),
                    @ApiResponse(responseCode = "415", description = "Unsupported Media Type"),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error - the job could not be created")
            })
    public Response bulkRefresh(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            final BulkRefreshForm form) throws DotDataException, DotSecurityException {

        final User user = init(request, response).getUser();

        Logger.debug(this, () -> String.format("User %s is submitting %d inode(s) to be reindexed",
                user.getUserId(), form.getContentletIds().size()));

        final BulkRefreshSubmitResponse submitted =
                this.bulkRefreshHelper.submit(form, user, request);

        // 202, not 200: the work is accepted, not done. A client must not be able to read this as
        // "reindexed" - telling the user otherwise is exactly the misleading success this endpoint
        // exists to avoid.
        return Response.status(Response.Status.ACCEPTED)
                .entity(new ResponseEntityBulkRefreshSubmitView(submitted))
                .build();
    }

    /**
     * Status snapshot of a reindex job. Poll this to follow a run and to read its final result.
     */
    @GET
    @Path("/{jobId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "getBulkRefreshStatus", summary = "Get the status of a reindex job",
            description = "Returns the job's current state and progress while it runs, and once terminal "
                    + "the full result including counters and — when requested at submit — the "
                    + "per-item records.",
            tags = {"Content"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Job status retrieved"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized - no backend user session"),
                    @ApiResponse(responseCode = "404", description = "Not Found - unknown job id"),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error")
            })
    public ResponseEntityView<JobView> getJobStatus(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam("jobId")
            @Parameter(name = "jobId", in = ParameterIn.PATH, required = true,
                    description = "The reindex job's unique identifier.",
                    schema = @Schema(type = "string", format = "uuid"))
            final String jobId) throws DotDataException {

        final User user = init(request, response).getUser();

        Logger.debug(this, () -> String.format("User %s is retrieving reindex job %s",
                user.getUserId(), jobId));

        return new ResponseEntityView<>(
                this.bulkRefreshHelper.view(this.bulkRefreshHelper.getJob(jobId)));
    }

    /**
     * Requests cancellation of a reindex job.
     */
    @POST
    @Path("/{jobId}/cancel")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "cancelBulkRefresh", summary = "Cancel a reindex job",
            description = "Stops the run at the next item boundary. Identifiers already reindexed "
                    + "stay reindexed; the remainder are counted as skipped.",
            tags = {"Content"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Cancellation requested"),
                    @ApiResponse(responseCode = "400", description = "Bad request - the job is already in a terminal state"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized - no backend user session"),
                    @ApiResponse(responseCode = "404", description = "Not Found - unknown job id"),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error")
            })
    public ResponseEntityStringView cancelJob(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam("jobId")
            @Parameter(name = "jobId", in = ParameterIn.PATH, required = true,
                    description = "The reindex job's unique identifier.",
                    schema = @Schema(type = "string", format = "uuid"))
            final String jobId) throws DotDataException {

        final User user = init(request, response).getUser();

        Logger.debug(this, () -> String.format("User %s is cancelling reindex job %s",
                user.getUserId(), jobId));

        this.bulkRefreshHelper.cancelJob(jobId);
        return new ResponseEntityStringView(
                "Cancellation request successfully sent to job " + jobId);
    }

    /**
     * Requires a backend user; anonymous callers are rejected before any content is touched.
     */
    private InitDataObject init(final HttpServletRequest request,
            final HttpServletResponse response) {
        return new WebResource.InitBuilder(this.webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();
    }
}
