package com.dotcms.rest.api.v1.variants;

import com.dotcms.experiments.model.Experiment;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.v1.experiments.AddVariantForm;
import com.dotcms.rest.api.v1.experiments.ResponseEntitySingleExperimentView;
import com.dotcms.rest.exception.NotFoundException;
import com.dotcms.util.DotPreconditions;
import com.dotcms.variant.model.Variant;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import  javax.ws.rs.core.Response;
import org.glassfish.jersey.server.JSONP;

/**
 * REST API for {@link com.dotcms.variant.model.Variant}
 */
@Path("/v1/variants")
@Tag(name = "Variants", description = "Endpoints for managing content variants")
public class VariantResource {

    private final WebResource webResource;

    public VariantResource() {
        webResource =  new WebResource();
    }

    @PUT
    @Path("/{variantName}/_promote")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(
            operationId = "promoteVariant",
            summary = "Promotes a variant to become the default",
            description = "Promotes a content variant to replace the default content version. " +
                    "This action makes the variant content live for all users.",
            tags = {"Variants"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Variant promoted successfully"),
                    @ApiResponse(responseCode = "400", description = "Bad request - Invalid variant name"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated"),
                    @ApiResponse(responseCode = "403", description = "Forbidden - User lacks required permissions"),
                    @ApiResponse(responseCode = "404", description = "Variant not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public Response promote(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @Parameter(description = "Name of the variant to promote") @PathParam("variantName") final String variantName) throws DotDataException {

        final InitDataObject initData = getInitData(request, response);
        final User user = initData.getUser();

        final Variant variant = APILocator.getVariantAPI().get(variantName)
                .orElseThrow(() -> new DoesNotExistException("Variant not found: " + variantName));

        APILocator.getVariantAPI().promote(variant, user);

        return Response.ok().build();
    }

    /**
     * Adds a new {@link com.dotcms.variant.model.Variant} to the system
     *
     */
    @POST
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(
            operationId = "addVariant",
            summary = "Creates a new content variant",
            description = "Creates a new content variant with the specified name and description. " +
                    "Variants allow for A/B testing and personalization of content.",
            tags = {"Variants"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Variant created successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityVariantView.class))),
                    @ApiResponse(responseCode = "400", description = "Bad request - Invalid variant data or variant already exists"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated"),
                    @ApiResponse(responseCode = "403", description = "Forbidden - User lacks required permissions"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntityVariantView addVariant(@Context final HttpServletRequest request,
                                                         @Context final HttpServletResponse response,
                                                         @RequestBody(description = "Variant configuration including name and description",
                                                                 required = true,
                                                                 content = @Content(schema = @Schema(implementation = VariantForm.class)))
                                                         final VariantForm variantForm) throws DotDataException, DotSecurityException {

        DotPreconditions.isTrue(variantForm!=null, ()->"Missing Variant Form",
                IllegalArgumentException.class);

        final InitDataObject initData = getInitData(request, response);
        final User user = initData.getUser();

        Logger.debug(this, ()-> "Adding Variant: " + variantForm.getName()
                + " by user: " + user.getUserId());

        APILocator.getVariantAPI().get(variantForm.getName())
                .ifPresent(variant -> {
                    throw new IllegalArgumentException("Variant already exists: " + variantForm.getName());
                });

        final Variant variant = APILocator.getVariantAPI().save(
                Variant.builder()
                        .name(variantForm.getName())
                        .description(variantForm.getDescription())
                        .archived(false)
                        .build());
        return new ResponseEntityVariantView(variant);
    }

    private InitDataObject getInitData(final HttpServletRequest request,
            final HttpServletResponse response) {
        return new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response)
                .requiredBackendUser(true)
                .rejectWhenNoUser(true)
                .init();
    }
}
