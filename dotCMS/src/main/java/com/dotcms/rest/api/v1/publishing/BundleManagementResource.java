package com.dotcms.rest.api.v1.publishing;

import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.bundle.business.BundleAPI;
import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.business.PublishAuditAPI;
import com.dotcms.publisher.business.PublishAuditStatus;
import com.dotcms.publisher.business.PublisherAPI;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.annotation.SwaggerCompliant;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.rest.exception.ConflictException;
import com.dotcms.rest.exception.NotFoundException;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * REST Resource for managing bundle assets.
 * Provides endpoints for adding assets to bundles and removing assets
 * from unpushed bundles.
 *
 * <p>Replaces the legacy {@code RemotePublishAjaxAction.addToBundle()} AJAX action
 * and the JSP-based asset deletion in {@code view_unpushed_bundles.jsp}.</p>
 *
 * @author hassandotcms
 * @since Mar 2026
 */
@Path("/v1/bundles")
@SwaggerCompliant(value = "Content and asset management APIs", batch = 2)
@Tag(name = "Publishing", description = "Push publishing job management endpoints")
public class BundleManagementResource {

    private final WebResource webResource;
    private final Lazy<BundleAPI> bundleAPI;
    private final Lazy<PublisherAPI> publisherQueueAPI;
    private final Lazy<PublishAuditAPI> publishAuditAPI;
    private final PublishingJobsHelper publishingJobsHelper;

    /**
     * Default constructor for JAX-RS.
     */
    public BundleManagementResource() {
        this(new WebResource(),
             Lazy.of(APILocator::getBundleAPI),
             Lazy.of(PublisherAPI::getInstance),
             Lazy.of(PublishAuditAPI::getInstance),
             new PublishingJobsHelper());
    }

    /**
     * Constructor for testing with dependency injection.
     *
     * @param webResource          Web resource for authentication
     * @param bundleAPI            Bundle API for bundle operations
     * @param publisherQueueAPI    Publisher Queue API for bundle asset operations
     * @param publishAuditAPI      Publish Audit API for status checks
     * @param publishingJobsHelper Helper for status checks
     */
    @VisibleForTesting
    public BundleManagementResource(final WebResource webResource,
                                     final Lazy<BundleAPI> bundleAPI,
                                     final Lazy<PublisherAPI> publisherQueueAPI,
                                     final Lazy<PublishAuditAPI> publishAuditAPI,
                                     final PublishingJobsHelper publishingJobsHelper) {
        this.webResource = webResource;
        this.bundleAPI = bundleAPI;
        this.publisherQueueAPI = publisherQueueAPI;
        this.publishAuditAPI = publishAuditAPI;
        this.publishingJobsHelper = publishingJobsHelper;
    }

    /**
     * Adds assets to a bundle, resolving or creating the bundle as needed.
     *
     * <p>Bundle resolution follows this order:</p>
     * <ol>
     *   <li>If {@code bundleId} is provided, look up by ID</li>
     *   <li>If not found, look up unsent bundles by {@code bundleName}</li>
     *   <li>If still not found, create a new bundle</li>
     * </ol>
     *
     * <p>Assets that cannot be added (e.g., no publish permission) are reported
     * in the {@code errors} list — they do not fail the entire request.</p>
     *
     * @param request  The HTTP request
     * @param response The HTTP response
     * @param form     Request body containing bundleId/bundleName and assetIds
     * @return Result with bundle details and per-asset error information
     */
    @Operation(
            summary = "Add assets to a bundle",
            description = "Adds assets to an existing bundle or creates a new bundle. " +
                    "Bundle resolution: (1) look up by bundleId, (2) if not found, " +
                    "look up unsent bundles by bundleName, (3) if still not found, " +
                    "auto-create a new bundle. A non-matching bundleId does NOT return " +
                    "404 — it falls through to name lookup and then auto-creation. " +
                    "Individual asset failures (e.g., no publish permission) are reported " +
                    "in the errors list as human-readable strings, not as HTTP errors."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Assets processed (check errors list for per-asset failures)",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = ResponseEntityAddAssetsToBundleView.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request (e.g., empty assetIds) or publisher error " +
                            "(e.g., asset already linked to the bundle)",
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
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)
            )
    })
    @POST
    @Path("/assets")
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ResponseEntityAddAssetsToBundleView addAssetsToBundle(
            @Parameter(hidden = true) @Context final HttpServletRequest request,
            @Parameter(hidden = true) @Context final HttpServletResponse response,
            @RequestBody(
                    description = "Assets to add and target bundle information",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = AddAssetsToBundleForm.class)
                    )
            )
            final AddAssetsToBundleForm form) {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();

        final User user = initData.getUser();

        if (form == null) {
            throw new BadRequestException("Request body is required");
        }
        form.checkValid();

        try {
            boolean created = false;
            Bundle bundle = null;

            // Step 2a: Try by ID
            if (UtilMethods.isSet(form.getBundleId())) {
                bundle = bundleAPI.get().getBundleById(form.getBundleId());
            }

            // Step 2b: Try by name (unsent bundles only)
            if (bundle == null && UtilMethods.isSet(form.getBundleName())) {
                bundle = bundleAPI.get()
                        .getUnsendBundlesByName(
                                user.getUserId(), form.getBundleName(), 1000, 0)
                        .stream()
                        .filter(b -> b.getName().equalsIgnoreCase(form.getBundleName()))
                        .findFirst()
                        .orElse(null);
            }

            // Step 2c: Create new bundle
            if (bundle == null) {
                final String name = UtilMethods.isSet(form.getBundleName())
                        ? form.getBundleName()
                        : "Bundle:" + new Date();
                bundle = new Bundle(name, null, null, user.getUserId());
                bundleAPI.get().saveBundle(bundle);
                created = true;
            }

            // Step 3: Save bundle assets
            final Map<String, Object> resultMap = publisherQueueAPI.get()
                    .saveBundleAssets(form.getAssetIds(), bundle.getId(), user);

            // Step 4: Build response
            @SuppressWarnings("unchecked")
            final List<String> errorMessages =
                    (List<String>) resultMap.get("errorMessages");

            final int total = (int) resultMap.get("total");
            final List<String> errors = errorMessages != null ? errorMessages : List.of();

            Logger.info(this, String.format(
                    "Added %d assets to bundle '%s' (created=%s) by user '%s'. Errors: %d",
                    total, bundle.getId(), created, user.getUserId(), errors.size()));

            return new ResponseEntityAddAssetsToBundleView(
                    AddAssetsToBundleView.builder()
                            .bundleId(bundle.getId())
                            .bundleName(bundle.getName())
                            .created(created)
                            .total(total)
                            .errors(errors)
                            .build());

        } catch (DotPublisherException e) {
            throw new BadRequestException(e.getMessage());
        } catch (DotDataException e) {
            Logger.error(this, "Error adding assets to bundle: " + e.getMessage(), e);
            throw new DotRuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Removes one or more assets from an unpushed bundle.
     *
     * <p>Each asset is processed independently — failures for individual assets
     * do not prevent other assets from being removed. Results are returned
     * per-asset with success/failure status.</p>
     *
     * <p>The bundle must not be in an active publishing state (BUNDLING,
     * SENDING_TO_ENDPOINTS, or PUBLISHING_BUNDLE).</p>
     *
     * @param request   The HTTP request
     * @param response  The HTTP response
     * @param bundleId  The bundle identifier
     * @param form      Request body containing assetIds to remove
     * @return Per-asset removal results
     */
    @Operation(
            summary = "Remove assets from an unpushed bundle",
            description = "Removes one or more assets from an unpushed bundle. " +
                    "Each asset is processed independently with per-asset results. " +
                    "Assets not found in the bundle return success=false."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Removal completed (check per-asset success flags)",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = ResponseEntityRemoveAssetsFromBundleView.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request (e.g., blank bundleId, empty assetIds)",
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
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Cannot remove assets while bundle is in " +
                            "BUNDLING, SENDING_TO_ENDPOINTS, or PUBLISHING_BUNDLE status",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)
            )
    })
    @DELETE
    @Path("/{bundleId}/assets")
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ResponseEntityRemoveAssetsFromBundleView removeAssetsFromBundle(
            @Parameter(hidden = true) @Context final HttpServletRequest request,
            @Parameter(hidden = true) @Context final HttpServletResponse response,
            @Parameter(
                    description = "Bundle identifier",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathParam("bundleId") final String bundleId,
            @RequestBody(
                    description = "Assets to remove from the bundle",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = RemoveAssetsFromBundleForm.class)
                    )
            )
            final RemoveAssetsFromBundleForm form) {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();

        final User user = initData.getUser();

        // Validate bundleId
        if (bundleId == null || bundleId.trim().isEmpty()) {
            throw new BadRequestException("bundleId is required");
        }

        // Validate form
        if (form == null || form.assetIds() == null || form.assetIds().isEmpty()) {
            throw new BadRequestException("assetIds must not be null or empty");
        }

        try {
            // Resolve bundle
            final Bundle bundle = bundleAPI.get().getBundleById(bundleId);
            if (bundle == null) {
                throw new NotFoundException(
                        String.format("Bundle not found: %s", bundleId));
            }

            // Guard: bundle must NOT be in-progress
            final PublishAuditStatus auditStatus =
                    publishAuditAPI.get().getPublishAuditStatus(bundleId);
            if (auditStatus != null
                    && publishingJobsHelper.isInProgressStatus(auditStatus.getStatus())) {
                throw new ConflictException(
                        "Cannot remove assets from bundle while publishing is in progress");
            }

            // Pre-fetch assets in the bundle queue for existence checks
            final Set<String> assetsInBundle = publisherQueueAPI.get()
                    .getQueueElementsByBundleId(bundleId)
                    .stream()
                    .map(e -> e.getAsset())
                    .collect(Collectors.toSet());

            // Process each asset independently
            final List<RemoveAssetResultView> results = new ArrayList<>();

            for (final String assetId : form.assetIds()) {

                if (!assetsInBundle.contains(assetId)) {
                    results.add(RemoveAssetResultView.builder()
                            .assetId(assetId)
                            .success(false)
                            .message("Asset not found in bundle")
                            .build());
                    continue;
                }

                try {
                    bundleAPI.get().deleteAssetFromBundleAndAuditStatus(assetId, bundleId);
                    results.add(RemoveAssetResultView.builder()
                            .assetId(assetId)
                            .success(true)
                            .message("Asset removed from bundle")
                            .build());
                } catch (DotDataException e) {
                    Logger.error(this, String.format(
                            "Error removing asset '%s' from bundle '%s': %s",
                            assetId, bundleId, e.getMessage()), e);
                    results.add(RemoveAssetResultView.builder()
                            .assetId(assetId)
                            .success(false)
                            .message(e.getMessage())
                            .build());
                }
            }

            final long successCount = results.stream()
                    .filter(RemoveAssetResultView::success).count();

            Logger.info(this, String.format(
                    "Removed assets from bundle '%s' by user '%s': %d succeeded, %d failed",
                    bundleId, user.getUserId(), successCount,
                    results.size() - successCount));

            return new ResponseEntityRemoveAssetsFromBundleView(results);

        } catch (DotDataException | DotPublisherException e) {
            Logger.error(this, "Error removing assets from bundle: " + e.getMessage(), e);
            throw new DotRuntimeException(e.getMessage(), e);
        }
    }

}
