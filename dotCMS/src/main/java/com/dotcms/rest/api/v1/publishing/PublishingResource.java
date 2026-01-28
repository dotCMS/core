package com.dotcms.rest.api.v1.publishing;

import com.dotcms.api.system.event.message.MessageSeverity;
import com.dotcms.api.system.event.message.SystemMessageEventUtil;
import com.dotcms.api.system.event.message.builder.SystemMessageBuilder;
import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.concurrent.DotSubmitter;
import com.dotcms.publisher.bundle.business.BundleAPI;
import com.dotcms.publisher.bundle.business.BundleDeleteResult;
import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.business.PublishAuditAPI;
import com.dotcms.publisher.business.PublishAuditStatus;
import com.dotcms.publisher.business.PublishAuditStatus.Status;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.Pagination;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.annotation.SwaggerCompliant;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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

    /**
     * Default constructor for JAX-RS.
     */
    public PublishingResource() {
        this(new WebResource(),
             Lazy.of(PublishAuditAPI::getInstance),
             Lazy.of(APILocator::getBundleAPI),
             new PublishingJobsHelper());
    }

    /**
     * Constructor for testing with dependency injection.
     *
     * @param webResource          Web resource for authentication
     * @param publishAuditAPI      Audit API for retrieving publishing status
     * @param bundleAPI            Bundle API for bundle operations
     * @param publishingJobsHelper Helper for transforming data to views
     */
    @VisibleForTesting
    public PublishingResource(final WebResource webResource,
                              final Lazy<PublishAuditAPI> publishAuditAPI,
                              final Lazy<BundleAPI> bundleAPI,
                              final PublishingJobsHelper publishingJobsHelper) {
        this.webResource = webResource;
        this.publishAuditAPI = publishAuditAPI;
        this.bundleAPI = bundleAPI;
        this.publishingJobsHelper = publishingJobsHelper;
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
     * Bulk deletes publishing jobs by status.
     *
     * <p>This endpoint is designed for cleaning up terminal state bundles (completed/failed)
     * or canceling queued bundles. It CANNOT purge in-progress bundles to prevent data
     * corruption.</p>
     *
     * <h3>Safe to Purge:</h3>
     * <ul>
     *   <li>Terminal: SUCCESS, FAILED_TO_PUBLISH, FAILED_TO_BUNDLE, etc.</li>
     *   <li>Queued: WAITING_FOR_PUBLISHING (cancels scheduled publishes)</li>
     * </ul>
     *
     * <h3>Cannot Purge (400 Bad Request):</h3>
     * <ul>
     *   <li>BUNDLING - Creating bundle archive</li>
     *   <li>SENDING_TO_ENDPOINTS - Transmitting to targets</li>
     *   <li>PUBLISHING_BUNDLE - Applying at receiver</li>
     * </ul>
     *
     * @param request  The HTTP request
     * @param response The HTTP response
     * @param status   Comma-separated status values to purge (optional)
     * @return Acknowledgment message with purge details
     */
    @Operation(
            summary = "Bulk delete publishing jobs by status",
            description = "Removes all bundles matching the specified status filter. " +
                    "Cannot purge in-progress bundles (BUNDLING, SENDING_TO_ENDPOINTS, PUBLISHING_BUNDLE). " +
                    "If no status specified, uses safe defaults (all terminal + queued statuses)."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Purge operation initiated (processes in background)",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = ResponseEntityPurgeView.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid status value or attempted to purge in-progress statuses",
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
    @DELETE
    @Path("/purge")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ResponseEntityPurgeView purgePublishingJobs(
            @Parameter(hidden = true) @Context final HttpServletRequest request,
            @Parameter(hidden = true) @Context final HttpServletResponse response,
            @Parameter(
                    description = "Comma-separated status values to purge. If omitted, uses safe defaults " +
                            "(all terminal + queued, excludes in-progress). " +
                            "Cannot include: BUNDLING, SENDING_TO_ENDPOINTS, PUBLISHING_BUNDLE",
                    example = "SUCCESS,FAILED_TO_PUBLISH"
            )
            @QueryParam("status") final String status) throws DotDataException {

        // Initialize request context and authenticate user (requires backend user)
        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();

        final User user = initData.getUser();

        // Determine statuses to purge
        final List<Status> statusList;
        if (UtilMethods.isSet(status)) {
            // Validate provided statuses
            final List<String> invalidStatuses = publishingJobsHelper.getInvalidStatuses(status);
            if (!invalidStatuses.isEmpty()) {
                throw new BadRequestException(
                        String.format("Invalid status value(s): %s. Valid values: %s",
                                String.join(", ", invalidStatuses),
                                String.join(", ", publishingJobsHelper.getValidStatusNames())));
            }
            statusList = publishingJobsHelper.parseStatuses(status);
        } else {
            // Use safe defaults
            statusList = new ArrayList<>(PublishingJobsHelper.SAFE_PURGE_STATUSES);
        }

        // Check for in-progress statuses (400 Bad Request)
        final List<Status> inProgressFound = publishingJobsHelper.getInProgressStatuses(statusList);
        if (!inProgressFound.isEmpty()) {
            throw new BadRequestException(
                    String.format("Cannot purge bundles with in-progress statuses: %s. " +
                                    "These statuses are excluded to prevent data corruption.",
                            inProgressFound.stream()
                                    .map(Status::name)
                                    .collect(Collectors.joining(", "))));
        }

        Logger.info(this, String.format("Purging publishing jobs with statuses: %s by user: %s",
                statusList.stream().map(Status::name).collect(Collectors.joining(", ")),
                user.getUserId()));

        // Execute purge asynchronously (consistent with legacy bulk delete pattern)
        final DotSubmitter dotSubmitter = DotConcurrentFactory
                .getInstance().getSubmitter(DotConcurrentFactory.DOT_SYSTEM_THREAD_POOL);

        dotSubmitter.execute(() -> {
            try {
                final BundleDeleteResult result = bundleAPI.get()
                        .deleteAllBundles(user, statusList.toArray(new Status[0]));

                sendPurgeResultMessage(initData, result);

            } catch (DotDataException e) {
                Logger.error(this, "Error purging publishing jobs", e);
                sendPurgeErrorMessage(initData, e);
            }
        });

        // Return immediate acknowledgment
        final List<String> statusNames = statusList.stream()
                .map(Status::name)
                .collect(Collectors.toList());

        return new ResponseEntityPurgeView(Map.of(
                "message", "Purge operation started. Results will be notified when complete.",
                "statusesRequested", statusNames
        ));
    }

    /**
     * Sends success/warning message after purge completes.
     */
    private void sendPurgeResultMessage(final InitDataObject initData,
                                         final BundleDeleteResult result) {
        try {
            final int deletedCount = result.getDeleteBundleSet().size();
            final int failedCount = result.getFailedBundleSet().size();
            final String userId = initData.getUser().getUserId();

            final String message;
            final MessageSeverity severity;

            if (failedCount == 0) {
                message = String.format("%d bundles purged successfully", deletedCount);
                severity = MessageSeverity.INFO;
            } else {
                message = String.format("%d bundles purged successfully, %d failed", deletedCount, failedCount);
                severity = MessageSeverity.WARNING;
            }

            final SystemMessageEventUtil systemMessageEventUtil = SystemMessageEventUtil.getInstance();
            systemMessageEventUtil.pushMessage(
                    new SystemMessageBuilder()
                            .setMessage(message)
                            .setLife(DateUtil.SEVEN_SECOND_MILLIS)
                            .setSeverity(severity)
                            .create(),
                    List.of(userId));

            Logger.info(this, String.format("Purge completed: %s (user: %s)", message, userId));

        } catch (Exception e) {
            Logger.error(this, "Error sending purge result message", e);
        }
    }

    /**
     * Sends error message if purge fails.
     */
    private void sendPurgeErrorMessage(final InitDataObject initData,
                                        final Exception e) {
        try {
            final String userId = initData.getUser().getUserId();
            final String message = String.format("Purge operation failed: %s", e.getMessage());

            final SystemMessageEventUtil systemMessageEventUtil = SystemMessageEventUtil.getInstance();
            systemMessageEventUtil.pushMessage(
                    new SystemMessageBuilder()
                            .setMessage(message)
                            .setLife(DateUtil.TEN_SECOND_MILLIS)
                            .setSeverity(MessageSeverity.ERROR)
                            .create(),
                    List.of(userId));

        } catch (Exception ex) {
            Logger.error(this, "Error sending purge error message", ex);
        }
    }
}
