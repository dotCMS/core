package com.dotcms.rest.api.v1.publishing;

import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.bundle.business.BundleAPI;
import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.business.PublishAuditAPI;
import com.dotcms.publisher.business.PublishAuditStatus;
import com.dotcms.publisher.business.PublishAuditStatus.Status;
import com.dotcms.publisher.environment.bean.Environment;
import com.dotcms.publishing.FilterDescriptor;
import com.dotcms.publishing.PublisherConfig.DeliveryStrategy;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.Pagination;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.annotation.SwaggerCompliant;
import com.dotcms.rest.exception.BadRequestException;
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
import java.util.Date;
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
    private final Lazy<com.dotcms.publishing.PublisherAPI> publisherAPI;
    private final Lazy<com.dotcms.publisher.business.PublisherAPI> publisherQueueAPI;
    private final PublishingJobsHelper publishingJobsHelper;

    /**
     * Default constructor for JAX-RS.
     */
    public PublishingResource() {
        this(new WebResource(),
             Lazy.of(PublishAuditAPI::getInstance),
             Lazy.of(APILocator::getBundleAPI),
             Lazy.of(APILocator::getPublisherAPI),
             Lazy.of(com.dotcms.publisher.business.PublisherAPI::getInstance),
             new PublishingJobsHelper());
    }

    /**
     * Constructor for testing with dependency injection.
     *
     * @param webResource          Web resource for authentication
     * @param publishAuditAPI      Audit API for retrieving publishing status
     * @param bundleAPI            Bundle API for bundle operations
     * @param publisherAPI         Publisher API for filter lookup
     * @param publisherQueueAPI    Publisher Queue API for bundle queue operations
     * @param publishingJobsHelper Helper for transforming data to views
     */
    @VisibleForTesting
    public PublishingResource(final WebResource webResource,
                              final Lazy<PublishAuditAPI> publishAuditAPI,
                              final Lazy<BundleAPI> bundleAPI,
                              final Lazy<com.dotcms.publishing.PublisherAPI> publisherAPI,
                              final Lazy<com.dotcms.publisher.business.PublisherAPI> publisherQueueAPI,
                              final PublishingJobsHelper publishingJobsHelper) {
        this.webResource = webResource;
        this.publishAuditAPI = publishAuditAPI;
        this.bundleAPI = bundleAPI;
        this.publisherAPI = publisherAPI;
        this.publisherQueueAPI = publisherQueueAPI;
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
     * Pushes a bundle to specified environments for publishing.
     *
     * <p>This endpoint schedules an existing bundle (with assets already added)
     * for push publishing to one or more environments. Supports three operations:
     * publish, expire, and publishexpire (publish then auto-expire).</p>
     *
     * <h3>Operations:</h3>
     * <ul>
     *   <li><b>publish</b> - Publish content to target environments</li>
     *   <li><b>expire</b> - Unpublish/expire content from target environments</li>
     *   <li><b>publishexpire</b> - Publish now and schedule automatic expiration</li>
     * </ul>
     *
     * <h3>Date Format:</h3>
     * <p>All dates must be in ISO 8601 format with timezone offset:
     * {@code YYYY-MM-DDTHH:mm:ss±HH:MM} (e.g., {@code 2025-03-15T14:30:00-05:00})</p>
     *
     * @param request   The HTTP request
     * @param response  The HTTP response
     * @param bundleId  The existing bundle identifier
     * @param form      Push configuration (operation, dates, environments, filter)
     * @return Push result with confirmation of queued bundle
     */
    @Operation(
            summary = "Push bundle to environments",
            description = "Queues an existing bundle for publishing to specified environments. " +
                    "The bundle must exist and have assets already added. Supports publish, " +
                    "expire, and publishexpire operations with ISO 8601 date/time format."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Bundle successfully queued for publishing",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = ResponseEntityPushBundleResultView.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request (missing required fields, invalid operation, invalid date format)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - no permission to use specified environments",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Bundle or environment not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)
            )
    })
    @POST
    @Path("/push/{bundleId}")
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ResponseEntityPushBundleResultView pushBundle(
            @Parameter(hidden = true) @Context final HttpServletRequest request,
            @Parameter(hidden = true) @Context final HttpServletResponse response,
            @Parameter(
                    description = "Bundle identifier",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathParam("bundleId") final String bundleId,
            final PushBundleForm form) throws DotDataException, DotPublisherException {

        // 1. Authenticate backend user
        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();
        final User user = initData.getUser();

        // 2. Validate bundleId
        if (!UtilMethods.isSet(bundleId)) {
            throw new BadRequestException("Bundle ID is required");
        }

        // 3. Validate form inputs
        publishingJobsHelper.validatePushBundleForm(form);

        // 4. Get and validate bundle exists
        final Bundle bundle = bundleAPI.get().getBundleById(bundleId);
        if (bundle == null) {
            throw new NotFoundException(String.format("Bundle not found: %s", bundleId));
        }

        // 5. Validate environments and permissions
        final List<Environment> validEnvs = publishingJobsHelper.validateEnvironmentPermissions(
                form.getEnvironments(), user);
        if (validEnvs.isEmpty()) {
            throw new NotFoundException("No valid environments found or user lacks permission");
        }

        // 6. Get filter and extract forcePush (falls back to default filter if not found)
        final FilterDescriptor filter = publisherAPI.get().getFilterDescriptorByKey(form.getFilterKey());
        final boolean forcePush = (boolean) filter.getFilters()
                .getOrDefault(FilterDescriptor.FORCE_PUSH_KEY, false);

        // 7. Parse dates
        final Date publishDate = publishingJobsHelper.parseISO8601Date(form.getPublishDate());
        final Date expireDate = publishingJobsHelper.parseISO8601Date(form.getExpireDate());

        // 8. Update bundle with settings
        bundle.setForcePush(forcePush);
        bundle.setFilterKey(form.getFilterKey());
        bundleAPI.get().saveBundleEnvironments(bundle, validEnvs);

        // 9. Execute operation based on type
        final String operation = form.getOperation().toLowerCase();
        switch (operation) {
            case "publish":
                bundle.setPublishDate(publishDate);
                bundleAPI.get().updateBundle(bundle);
                publisherQueueAPI.get().publishBundleAssets(bundleId, publishDate);
                break;
            case "expire":
                bundle.setExpireDate(expireDate);
                bundleAPI.get().updateBundle(bundle);
                publisherQueueAPI.get().unpublishBundleAssets(bundleId, expireDate);
                break;
            case "publishexpire":
                bundle.setPublishDate(publishDate);
                bundle.setExpireDate(expireDate);
                bundleAPI.get().updateBundle(bundle);
                publisherQueueAPI.get().publishAndExpireBundleAssets(bundleId, publishDate, expireDate, user);
                break;
            default:
                throw new BadRequestException(String.format(
                        "Invalid operation: '%s'. Valid values: publish, expire, publishexpire",
                        form.getOperation()));
        }

        // 10. Fire publisher queue immediately (2-second delay for responsive UX)
        publisherQueueAPI.get().firePublisherQueueNow(
                Map.of("deliveryStrategy", DeliveryStrategy.ALL_ENDPOINTS));

        // 11. Build and return result (return actual valid environments, not requested)
        final List<String> validEnvIds = validEnvs.stream()
                .map(Environment::getId)
                .collect(Collectors.toList());
        final PushBundleResultView result = PushBundleResultView.builder()
                .bundleId(bundleId)
                .operation(operation)
                .publishDate(form.getPublishDate())
                .expireDate(form.getExpireDate())
                .environments(validEnvIds)
                .filterKey(form.getFilterKey())
                .build();

        Logger.info(this, String.format("Pushed bundle '%s' to %d environment(s) by user '%s'",
                bundleId, validEnvs.size(), user.getUserId()));

        return new ResponseEntityPushBundleResultView(result);
    }
}
