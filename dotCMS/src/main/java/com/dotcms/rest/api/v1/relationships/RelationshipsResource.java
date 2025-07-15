package com.dotcms.rest.api.v1.relationships;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityRelationshipPaginationView;
import com.dotcms.rest.ResponseEntityListMapView;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.v1.authentication.ResponseUtil;
import com.dotcms.util.PaginationUtil;
import com.dotcms.util.pagination.RelationshipPaginator;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import org.glassfish.jersey.server.JSONP;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.dotcms.rest.annotation.SwaggerCompliant;

/**
 * This resource provides all the different end-points associated to information and actions that
 * the front-end can perform on relationships.
 *
 * @author nollymar
 */
@SwaggerCompliant(value = "Rules engine and business logic APIs", batch = 6)
@Path("/v1/relationships")
@Tag(name = "Relationships")
public class RelationshipsResource {

    private final WebResource webResource;

    public RelationshipsResource() {
        this(new WebResource());
    }


    RelationshipsResource(final WebResource webResource) {

        this.webResource = webResource;

    }

    @Operation(
        summary = "Get relationship cardinalities",
        description = "Returns all available relationship cardinality types with their labels and IDs"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Cardinalities retrieved successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityListMapView.class))),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @JSONP
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Path("cardinalities")
    public final Response getCardinality() throws Throwable {
        Logger.debug(this, "Getting relationships cardinality");

        final List<Map<String, Object>> cardinalities = Arrays.stream(WebKeys.Relationship.RELATIONSHIP_CARDINALITY.values())
                      .map(cardinality -> {
                          String label;

                          try {
                              label = LanguageUtil.get(String.format(
                                      "contenttypes.field.properties.relationships.cardinality.%s.label", cardinality.name()));
                          } catch (LanguageException e) {
                              label = cardinality.name();
                          }

                          return Map.<String, Object>of(
                                  "name", cardinality.name(),
                                  "id", cardinality.ordinal(),
                                  "label", label
                                 );
                      })
                      .collect(Collectors.toList());
                      
        return Response.ok(new ResponseEntityListMapView(cardinalities)).build();
    }

    @Operation(
        summary = "Get one-sided relationships",
        description = "Returns orphan relationships (those defined in the parent or children but not in both) for a given content type"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "One-sided relationships retrieved successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityRelationshipPaginationView.class))),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "Content type not found",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @JSONP
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public final Response getOneSidedRelationships(
            @Parameter(description = "Content type identifier", required = true) @QueryParam("contentTypeId") final String contentTypeId,
            @Parameter(description = "Page number for pagination") @QueryParam(PaginationUtil.PAGE) final int page,
            @Parameter(description = "Number of items per page") @QueryParam(PaginationUtil.PER_PAGE) @DefaultValue("0") final int perPage,
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response) throws Throwable {
        Logger.debug(this,
                "Getting the possible relationships for content type " + contentTypeId);

        final InitDataObject initData = this.webResource.init(null, request, response, true, null);
        final User user = initData.getUser();
        final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(user);

        final PaginationUtil paginationUtil = new PaginationUtil(new RelationshipPaginator());

        try {
            final ContentType contentType = contentTypeAPI.find(contentTypeId);

            final Map<String, Object> params = new HashMap<>();
            params.put(RelationshipPaginator.CONTENT_TYPE_PARAM,
                    contentType);
            return paginationUtil.getPage(request, user, null, page, perPage, params);
        } catch (Exception e) {

            return ResponseUtil.mapExceptionResponse(e);
        }
    }
}
