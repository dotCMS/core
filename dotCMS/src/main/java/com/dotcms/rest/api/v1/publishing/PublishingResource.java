package com.dotcms.rest.api.v1.publishing;

import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.bundle.business.BundleAPI;
import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.business.PublishAuditAPI;
import com.dotcms.publisher.business.PublishAuditStatus;
import com.dotcms.publisher.business.PublishAuditStatus.Status;
import com.dotcms.publishing.PublisherConfig.DeliveryStrategy;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.Pagination;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.annotation.SwaggerCompliant;
import com.dotcms.rest.exception.ConflictException;
import com.dotcms.rest.exception.NotFoundException;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.vavr.Lazy;
import org.glassfish.jersey.server.JSONP;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST Resource for viewing and managing publishing jobs.
 * Provides endpoints for listing publishing jobs with filtering, pagination,
 * and asset preview capabilities.
 *
 * <p>This resource unifies data from the publishing audit table and bundle
 * metadata to provide a comprehensive view of publishing operations.</p>
 *
 * @author hassandotcms
 * @since Jan 2026
 */
@Path("/v1/publishing")
@SwaggerCompliant(value = "Content and asset management APIs", batch = 2)
@Tag(name = "Publishing", description = "Push publishing job management endpoints")
public class PublishingResource {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PER_PAGE = 50;
    private static final int MAX_PER_PAGE = 500;

    private final WebResource webResource;
    private final Lazy<PublishAuditAPI> publishAuditAPI;
    private final Lazy<BundleAPI> bundleAPI;
    private final PublishingJobsHelper publishingJobsHelper;
    private final PublishingRetryHelper publishingRetryHelper;

    /**
     * Default constructor for JAX-RS.
     */
    public PublishingResource() {
        this(new WebResource(),
             Lazy.of(PublishAuditAPI::getInstance),
             Lazy.of(APILocator::getBundleAPI),
             new PublishingJobsHelper(),
             new PublishingRetryHelper());
    }

    /**
     * Constructor for testing with dependency injection.
     *
     * @param webResource            Web resource for authentication
     * @param publishAuditAPI        Audit API for retrieving publishing status
     * @param bundleAPI              Bundle API for bundle operations
     * @param publishingJobsHelper   Helper for transforming data to views
     * @param publishingRetryHelper  Helper for retry operations
     */
    @VisibleForTesting
    public PublishingResource(final WebResource webResource,
                              final Lazy<PublishAuditAPI> publishAuditAPI,
                              final Lazy<BundleAPI> bundleAPI,
                              final PublishingJobsHelper publishingJobsHelper,
                              final PublishingRetryHelper publishingRetryHelper) {
        this.webResource = webResource;
        this.publishAuditAPI = publishAuditAPI;
        this.bundleAPI = bundleAPI;
        this.publishingJobsHelper = publishingJobsHelper;
        this.publishingRetryHelper = publishingRetryHelper;
    }

    /**
     * Returns a paginated list of publishing jobs with optional filtering.
     *
     * <p>This endpoint provides a unified view of publishing operations including
     * both completed (audit) and pending (queue) bundles. Results can be filtered
     * by status and/or bundle name/ID.</p>
     *
     * <h3>Usage Examples:</h3>
     * <ul>
     *   <li>Audit view: {@code ?status=SUCCESS,FAILED_TO_PUBLISH,SUCCESS_WITH_WARNINGS}</li>
     *   <li>Queue view: {@code ?status=WAITING_FOR_PUBLISHING}</li>
     *   <li>In-progress: {@code ?status=BUNDLING,SENDING_TO_ENDPOINTS,PUBLISHING_BUNDLE}</li>
     *   <li>All bundles: (omit status parameter)</li>
     *   <li>Search by name: {@code ?filter=staging}</li>
     * </ul>
     *
     * @param request   The HTTP request
     * @param response  The HTTP response
     * @param page      Page number (starts from 1, default: 1)
     * @param perPage   Results per page (1-500, default: 50)
     * @param filter    Case-insensitive partial match on bundle_id and bundle name
     * @param status    Comma-separated status values to filter by
     * @return Paginated list of publishing jobs with metadata
     */
    @Operation(
            summary = "List publishing jobs",
            description = "Returns a paginated list of publishing jobs combining audit status " +
                    "and bundle metadata. Supports filtering by status and bundle name/ID. " +
                    "Each job includes asset preview (first 3 assets), environment count, and timing information."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Publishing jobs retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = ResponseEntityPublishingJobsView.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request parameters (e.g., invalid status value, pagination out of range)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - insufficient permissions",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)
            )
    })
    @GET
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ResponseEntityPublishingJobsView listPublishingJobs(
            @Parameter(hidden = true) @Context final HttpServletRequest request,
            @Parameter(hidden = true) @Context final HttpServletResponse response,
            @Parameter(
                    description = "Page number (starts from 1)",
                    example = "1"
            )
            @QueryParam("page") @DefaultValue("1") final int page,
            @Parameter(
                    description = "Results per page (1-500)",
                    example = "50"
            )
            @QueryParam("per_page") @DefaultValue("50") final int perPage,
            @Parameter(
                    description = "Case-insensitive partial match filter on bundle_id and bundle name",
                    example = "staging"
            )
            @QueryParam("filter") final String filter,
            @Parameter(
                    description = "Comma-separated status values to filter (e.g., SUCCESS,FAILED_TO_PUBLISH). " +
                            "Valid values: BUNDLE_REQUESTED, WAITING_FOR_PUBLISHING, BUNDLING, SENDING_TO_ENDPOINTS, " +
                            "PUBLISHING_BUNDLE, BUNDLE_SENT_SUCCESSFULLY, RECEIVED_BUNDLE, BUNDLE_SAVED_SUCCESSFULLY, " +
                            "SUCCESS, SUCCESS_WITH_WARNINGS, FAILED_TO_BUNDLE, FAILED_TO_SENT, " +
                            "FAILED_TO_SEND_TO_ALL_GROUPS, FAILED_TO_SEND_TO_SOME_GROUPS, FAILED_TO_PUBLISH, " +
                            "FAILED_INTEGRITY_CHECK, INVALID_TOKEN, LICENSE_REQUIRED",
                    example = "SUCCESS,FAILED_TO_PUBLISH"
            )
            @QueryParam("status") final String status) throws DotPublisherException {

        // Initialize request context and authenticate user
        new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();

        // Validate status parameter if provided
        if (UtilMethods.isSet(status)) {
            final List<String> invalidStatuses = publishingJobsHelper.getInvalidStatuses(status);
            if (!invalidStatuses.isEmpty()) {
                throw new BadRequestException(
                        String.format("Invalid status value(s): %s. Valid values: %s",
                                String.join(", ", invalidStatuses),
                                String.join(", ", publishingJobsHelper.getValidStatusNames())));
            }
        }

        // Validate and normalize pagination parameters
        final int validPage = Math.max(DEFAULT_PAGE, page);
        final int validPerPage = Math.min(Math.max(1, perPage), MAX_PER_PAGE);
        final int offset = (validPage - 1) * validPerPage;

        // Parse status filter
        final List<Status> statusList = publishingJobsHelper.parseStatuses(status);

        // Retrieve paginated audit statuses with combined filtering
        final List<PublishAuditStatus> auditStatuses =
                publishAuditAPI.get().getPublishAuditStatus(
                        validPerPage, offset, PublishingJobsHelper.ASSET_PREVIEW_LIMIT, filter, statusList);

        // Get total count for pagination
        final int totalCount = publishAuditAPI.get()
                .countPublishAuditStatus(filter, statusList);

        // Transform to view objects with enriched data
        final List<PublishingJobView> jobs = auditStatuses.stream()
                .map(publishingJobsHelper::toPublishingJobView)
                .collect(Collectors.toList());

        // Build pagination metadata
        final Pagination pagination = new Pagination.Builder()
                .currentPage(validPage)
                .perPage(validPerPage)
                .totalEntries(totalCount)
                .build();

        Logger.debug(this, () -> String.format(
                "Listed %d publishing jobs (page %d, total %d) with filter='%s', status='%s'",
                jobs.size(), validPage, totalCount, filter, status));

        return new ResponseEntityPublishingJobsView(jobs, pagination);
    }

    /**
     * Returns detailed status, endpoints, and timestamps for a specific publishing bundle.
     *
     * <p>This endpoint provides complete environment/endpoint breakdown with individual
     * success/failure status, error messages, and stack traces for failed endpoints.</p>
     *
     * @param request   The HTTP request
     * @param response  The HTTP response
     * @param bundleId  The bundle identifier
     * @return Detailed publishing job view with environment/endpoint breakdown
     */
    @Operation(
            summary = "Get publishing job details",
            description = "Returns detailed status, environments, endpoints, and timestamps " +
                    "for a specific publishing bundle. Includes per-endpoint success/failure " +
                    "status with error messages and stack traces for failed endpoints."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Publishing job details retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = ResponseEntityPublishingJobDetailView.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid bundle ID format",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - insufficient permissions",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Bundle not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)
            )
    })
    @GET
    @Path("/{bundleId}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ResponseEntityPublishingJobDetailView getPublishingJobDetails(
            @Parameter(hidden = true) @Context final HttpServletRequest request,
            @Parameter(hidden = true) @Context final HttpServletResponse response,
            @Parameter(
                    description = "Bundle identifier",
                    required = true,
                    example = "f3d9a4b7-staging-bundle-2026-01-15"
            )
            @PathParam("bundleId") final String bundleId) throws DotPublisherException {

        // Initialize request context and authenticate user
        new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();

        // Validate bundleId
        if (!UtilMethods.isSet(bundleId)) {
            throw new BadRequestException("Bundle ID is required");
        }

        // Retrieve audit status
        final PublishAuditStatus auditStatus = publishAuditAPI.get()
                .getPublishAuditStatus(bundleId);

        if (auditStatus == null) {
            throw new NotFoundException(String.format("Bundle not found: %s", bundleId));
        }

        // Transform to detailed view
        final PublishingJobDetailView detailView = publishingJobsHelper.toPublishingJobDetailView(auditStatus);

        Logger.debug(this, () -> String.format(
                "Retrieved publishing job details for bundle '%s' with status '%s'",
                bundleId, auditStatus.getStatus()));

        return new ResponseEntityPublishingJobDetailView(detailView);
    }

    /**
     * Deletes a specific publishing job/bundle.
     *
     * <p>This endpoint removes a bundle from the publishing queue. It is designed for
     * canceling queued/scheduled publishes or cleaning up terminal state bundles.
     * Bundles that are actively publishing cannot be deleted to prevent data inconsistency.</p>
     *
     * <h3>Deletable Statuses:</h3>
     * <ul>
     *   <li>Terminal: SUCCESS, FAILED_TO_PUBLISH, FAILED_TO_BUNDLE, etc.</li>
     *   <li>Queued: WAITING_FOR_PUBLISHING</li>
     * </ul>
     *
     * <h3>Non-Deletable Statuses (409 Conflict):</h3>
     * <ul>
     *   <li>BUNDLING - Creating bundle archive</li>
     *   <li>SENDING_TO_ENDPOINTS - Transmitting to targets</li>
     *   <li>PUBLISHING_BUNDLE - Applying at receiver</li>
     * </ul>
     *
     * @param request   The HTTP request
     * @param response  The HTTP response
     * @param bundleId  The bundle identifier to delete
     * @return Success message on deletion
     */
    @Operation(
            summary = "Delete a publishing job",
            description = "Removes a specific bundle from the publishing queue. " +
                    "Cannot delete bundles that are actively publishing (BUNDLING, SENDING_TO_ENDPOINTS, PUBLISHING_BUNDLE)."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Bundle deleted successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(type = "object",
                                    description = "Success response with message",
                                    example = "{\"message\": \"Bundle deleted successfully\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - insufficient permissions",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Bundle not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Cannot delete bundle while publishing is in progress",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)
            )
    })
    @DELETE
    @Path("/{bundleId}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response deletePublishingJob(
            @Parameter(hidden = true) @Context final HttpServletRequest request,
            @Parameter(hidden = true) @Context final HttpServletResponse response,
            @Parameter(
                    description = "Bundle identifier",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathParam("bundleId") final String bundleId) throws DotDataException, DotPublisherException {

        // Initialize request context and authenticate user (requires backend user)
        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();

        final User user = initData.getUser();

        // Validate bundleId is provided
        if (!UtilMethods.isSet(bundleId)) {
            throw new BadRequestException("Bundle ID is required");
        }

        // Check if bundle exists (either in audit or bundle table)
        final PublishAuditStatus auditStatus = publishAuditAPI.get().getPublishAuditStatus(bundleId);
        final Bundle bundle = bundleAPI.get().getBundleById(bundleId);

        if (auditStatus == null && bundle == null) {
            throw new NotFoundException(String.format("Bundle not found: %s", bundleId));
        }

        // Check if bundle is in-progress (cannot delete)
        if (auditStatus != null && publishingJobsHelper.isInProgressStatus(auditStatus.getStatus())) {
            throw new ConflictException(String.format(
                    "Cannot delete bundle while publishing is in progress (status: %s). " +
                    "Wait for publish to complete or fail.",
                    auditStatus.getStatus().name()));
        }

        // Delete bundle and all dependencies
        bundleAPI.get().deleteBundleAndDependencies(bundleId, user);

        Logger.info(this, String.format("Deleted publishing job '%s' by user '%s'",
                bundleId, user.getUserId()));

        return Response.ok(Map.of("message", "Bundle deleted successfully")).build();
    }

    /**
     * Retries failed or successful bundles by re-queueing them for publishing.
     *
     * <p>This endpoint supports bulk operations, allowing multiple bundles to be retried
     * in a single request. Each bundle is processed independently, with per-bundle
     * success/failure results returned in the response.</p>
     *
     * <h3>Retryable Statuses:</h3>
     * <ul>
     *   <li>SUCCESS - Re-publish successful bundles (force push auto-enabled)</li>
     *   <li>SUCCESS_WITH_WARNINGS - Re-publish with warnings (force push auto-enabled)</li>
     *   <li>FAILED_TO_PUBLISH - Retry failed publishing</li>
     *   <li>FAILED_TO_SEND_TO_ALL_GROUPS - Retry when all endpoints failed</li>
     *   <li>FAILED_TO_SEND_TO_SOME_GROUPS - Retry when some endpoints failed</li>
     *   <li>FAILED_TO_SENT - Retry send failures</li>
     * </ul>
     *
     * <h3>Non-Retryable Statuses:</h3>
     * <ul>
     *   <li>BUNDLING - Bundle creation in progress</li>
     *   <li>SENDING_TO_ENDPOINTS - Transfer in progress</li>
     *   <li>WAITING_FOR_PUBLISHING - Already queued</li>
     * </ul>
     *
     * @param request   The HTTP request
     * @param response  The HTTP response
     * @param form      Request body containing bundleIds, forcePush, and deliveryStrategy
     * @return Per-bundle retry results
     */
    @Operation(
            summary = "Retry failed or successful bundles",
            description = "Re-attempts sending bundles that were previously pushed but failed, " +
                    "partially failed, or succeeded but need re-synchronization. " +
                    "Supports bulk operations with per-bundle results."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Retry operation completed (check individual results for success/failure)",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = ResponseEntityRetryBundlesView.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request parameters (e.g., empty bundleIds, invalid deliveryStrategy)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - insufficient permissions",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)
            )
    })
    @POST
    @Path("/retry")
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ResponseEntityRetryBundlesView retryBundles(
            @Parameter(hidden = true) @Context final HttpServletRequest request,
            @Parameter(hidden = true) @Context final HttpServletResponse response,
            @RequestBody(
                    description = "Retry request containing bundle IDs and options",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = RetryBundlesForm.class)
                    )
            )
            final RetryBundlesForm form) {

        // Initialize request context and authenticate user
        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();

        final User user = initData.getUser();

        // Validate form
        if (form == null || form.bundleIds() == null || form.bundleIds().isEmpty()) {
            throw new BadRequestException("At least one bundle ID is required");
        }

        // Validate delivery strategy
        final DeliveryStrategy deliveryStrategy = form.deliveryStrategy();
        if (deliveryStrategy == null) {
            throw new BadRequestException(
                    "Invalid deliveryStrategy. Valid values: ALL_ENDPOINTS, FAILED_ENDPOINTS");
        }

        // Process each bundle - catch exceptions per item
        final List<RetryBundleResultView> results = new ArrayList<>();

        for (final String bundleId : form.bundleIds()) {
            try {
                final RetryResultDTO dto = publishingRetryHelper.retryBundle(
                        bundleId,
                        form.forcePush(),
                        deliveryStrategy,
                        user,
                        request
                );

                // Build success result from DTO
                results.add(RetryBundleResultView.builder()
                        .bundleId(dto.getBundleId())
                        .success(true)
                        .message("Bundle successfully re-queued for publishing")
                        .forcePush(dto.isForcePush())
                        .operation(dto.getOperation())
                        .deliveryStrategy(dto.getDeliveryStrategy())
                        .assetCount(dto.getAssetCount())
                        .build());

            } catch (final Exception e) {
                Logger.debug(this, "Error retrying bundle " + bundleId + ": " + e.getMessage(), e);

                // Build failure result
                results.add(RetryBundleResultView.builder()
                        .bundleId(bundleId != null ? bundleId.trim() : "unknown")
                        .success(false)
                        .message(e.getMessage())
                        .forcePush(null)
                        .operation(null)
                        .deliveryStrategy(deliveryStrategy.name())
                        .assetCount(null)
                        .build());
            }
        }

        // Log summary
        final long successCount = results.stream().filter(RetryBundleResultView::success).count();
        final long failureCount = results.size() - successCount;

        Logger.info(this, String.format(
                "Retry operation completed by user '%s': %d succeeded, %d failed out of %d bundles",
                user.getUserId(), successCount, failureCount, results.size()));

        return new ResponseEntityRetryBundlesView(results);
    }
}
