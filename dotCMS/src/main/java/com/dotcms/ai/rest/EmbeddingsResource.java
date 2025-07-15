package com.dotcms.ai.rest;

import com.dotcms.ai.AiKeys;
import com.dotcms.ai.api.EmbeddingsCallStrategy;
import com.dotcms.ai.db.EmbeddingsDTO;
import com.dotcms.ai.rest.forms.CompletionsForm;
import com.dotcms.ai.rest.forms.EmbeddingsForm;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.SwaggerCompliant;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.ws.rs.Consumes;
import org.glassfish.jersey.server.JSONP;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The EmbeddingsResource class provides RESTful endpoints for managing embeddings in the system.
 * It includes endpoints for creating, deleting, and retrieving embeddings, as well as additional utility operations.
 * This class requires user authentication and certain operations require specific user roles.
 */
@SwaggerCompliant(value = "Modern APIs and specialized services", batch = 7)
@Path("/v1/ai/embeddings")
@Tag(name = "AI")
public class EmbeddingsResource {

    private final ContentletAPI contentletAPI;

    public EmbeddingsResource() {
        this.contentletAPI = APILocator.getContentletAPI();
    }

    /**
     * Test endpoint for the EmbeddingsResource.
     *
     * @param request the HttpServletRequest object.
     * @param response the HttpServletResponse object.
     * @return a Response object containing a map with "type" as key and "embeddings" as value.
     */
    @Operation(
        summary = "Test AI embeddings service",
        description = "Returns a test response to verify the AI embeddings service is operational"
    )
    @ApiResponse(responseCode = "200", 
                description = "Test response returned successfully",
                content = @Content(mediaType = "application/json",
                                  schema = @Schema(type = "object", description = "Simple key-value map indicating embeddings service type")))
    @GET
    @JSONP
    @Path("/test")
    @Produces(MediaType.APPLICATION_JSON)
    public final Response textResource(@Context final HttpServletRequest request,
                                       @Context final HttpServletResponse response) {

        return Response.ok(Map.of(AiKeys.TYPE, AiKeys.EMBEDDINGS), MediaType.APPLICATION_JSON).build();
    }

    /**
     * Endpoint to create embeddings.
     *
     * @param request the HttpServletRequest object.
     * @param response the HttpServletResponse object.
     * @param embeddingsForm the form data for creating embeddings.
     * @return a Response object containing the result of the embeddings creation.
     */
    @Operation(
        summary = "Create AI embeddings",
        description = "Creates embeddings for content based on the provided form data, processing up to 10,000 content items"
    )
    @ApiResponse(responseCode = "200", 
                description = "Embeddings created successfully",
                content = @Content(mediaType = "application/json",
                                  schema = @Schema(type = "object", description = "Embeddings operation result containing timing and count information")))
    @ApiResponse(responseCode = "401", 
                description = "Unauthorized - backend user authentication required",
                content = @Content(mediaType = "application/json"))
    @ApiResponse(responseCode = "500", 
                description = "Internal server error during embeddings creation",
                content = @Content(mediaType = "application/json"))
    @POST
    @JSONP
    @Path("/")
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes(MediaType.APPLICATION_JSON)
    public final Response embed(@Context final HttpServletRequest request,
                                @Context final HttpServletResponse response,
                                @RequestBody(description = "Embeddings form containing query, limit, offset, indexName, model, velocityTemplate, and fields",
                                           required = true,
                                           content = @Content(schema = @Schema(implementation = EmbeddingsForm.class)))
                                final EmbeddingsForm embeddingsForm) {

        // force authentication
        final User user = new WebResource.InitBuilder(request, response).requiredBackendUser(true).init().getUser();
        long startTime = System.currentTimeMillis();

        try {
            int added = 0;
            int newOffset = embeddingsForm.offset;
            for (int i = 0; i < 10000; i++) {
                // searchIndex(String luceneQuery, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles)
                final List<ContentletSearch> searchResults = contentletAPI
                        .searchIndex(
                                embeddingsForm.query + " +live:true",
                                embeddingsForm.limit,
                                newOffset,
                                AiKeys.MODDATE,
                                user,
                                false);
                if (searchResults.isEmpty()) {
                    break;
                }
                newOffset += embeddingsForm.limit;

                final List<String> inodes = searchResults
                        .stream()
                        .map(ContentletSearch::getInode)
                        .collect(Collectors.toUnmodifiableList());
                added += inodes.size();

                EmbeddingsCallStrategy.resolveStrategy().bulkEmbed(inodes, embeddingsForm);
            }

            final long totalTime = System.currentTimeMillis() - startTime;
            final Map<String, Object> map = Map.of(
                    AiKeys.TIME_TO_EMBEDDINGS, totalTime + "ms",
                    AiKeys.TOTAL_TO_EMBED, added,
                    AiKeys.INDEX_NAME, embeddingsForm.indexName);
            final ResponseBuilder builder = Response.ok(map, MediaType.APPLICATION_JSON);

            return builder.build();
        } catch (Exception e) {
            Logger.error(this.getClass(), e.getMessage(), e);
            return Response.status(500).entity(Map.of(AiKeys.ERROR, e.getMessage())).build();
        }
    }

    /**
     * Endpoint to delete embeddings.
     *
     * @param request the HttpServletRequest object.
     * @param response the HttpServletResponse object.
     * @param json the JSON object containing the data for the embeddings to be deleted.
     * @return a Response object containing the result of the embeddings' deletion.
     */
    @Operation(
        summary = "Delete AI embeddings",
        description = "Deletes embeddings based on provided criteria such as query, identifier, inode, or content type"
    )
    @ApiResponse(responseCode = "200", 
                description = "Embeddings deleted successfully",
                content = @Content(mediaType = "application/json",
                                  schema = @Schema(type = "object", description = "Deletion result containing count of deleted items")))
    @ApiResponse(responseCode = "401", 
                description = "Unauthorized - backend user authentication required",
                content = @Content(mediaType = "application/json"))
    @DELETE
    @JSONP
    @Path("/")
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes(MediaType.APPLICATION_JSON)
    public final Response delete(@Context final HttpServletRequest request,
                                 @Context final HttpServletResponse response,
                                 @RequestBody(description = "JSON object containing deletion criteria such as query, identifier, inode, or content type",
                                            required = true,
                                            content = @Content(schema = @Schema(type = "object", description = "Deletion criteria including query, identifier, inode, or content type")))
                                 final JSONObject json) {

        final User user = new WebResource.InitBuilder(request, response).requiredBackendUser(true).init().getUser();

        if (UtilMethods.isSet(() -> json.optString(AiKeys.DELETE_QUERY))){
            final int numberDeleted =
                    APILocator.getDotAIAPI().getEmbeddingsAPI().deleteByQuery(
                            json.optString(AiKeys.DELETE_QUERY),
                            Optional.ofNullable(json.optString(AiKeys.INDEX_NAME)),
                            user);
            return Response.ok(Map.of(AiKeys.DELETED, numberDeleted)).build();
        }

        final EmbeddingsDTO dto = new EmbeddingsDTO.Builder()
                .withIndexName(json.optString(AiKeys.INDEX_NAME))
                .withIdentifier(json.optString(AiKeys.IDENTIFIER))
                .withLanguage(json.optLong(AiKeys.LANGUAGE, 0))
                .withInode(json.optString(AiKeys.INODE))
                .withContentType(json.optString(AiKeys.CONTENT_TYPE))
                .withHost(json.optString(AiKeys.SITE))
                .build();
        int deleted = APILocator.getDotAIAPI().getEmbeddingsAPI().deleteEmbedding(dto);

        return Response.ok(Map.of(AiKeys.DELETED, deleted)).build();
    }

    /**
     * Endpoint to drop and recreate the embeddings table.
     *
     * @param request the HttpServletRequest object.
     * @param response the HttpServletResponse object.
     * @param json the JSON object containing the data for the operation.
     * @return a Response object containing the result of the operation.
     */
    @Operation(
        summary = "Drop and recreate embeddings tables",
        description = "Drops and recreates the embeddings database tables. Requires CMS Administrator role."
    )
    @ApiResponse(responseCode = "200", 
                description = "Tables dropped and recreated successfully",
                content = @Content(mediaType = "application/json",
                                  schema = @Schema(type = "object", description = "Table creation result status")))
    @ApiResponse(responseCode = "401", 
                description = "Unauthorized - backend user authentication required",
                content = @Content(mediaType = "application/json"))
    @ApiResponse(responseCode = "403", 
                description = "Forbidden - CMS Administrator role required",
                content = @Content(mediaType = "application/json"))
    @DELETE
    @JSONP
    @Path("/db")
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes(MediaType.APPLICATION_JSON)
    public final Response dropAndRecreateTables(@Context final HttpServletRequest request,
                                                @Context final HttpServletResponse response,
                                                @RequestBody(description = "Empty JSON object to trigger table recreation",
                                                           required = true,
                                                           content = @Content(schema = @Schema(type = "object", description = "Empty JSON object for triggering table recreation")))
                                                final JSONObject json) {

        new WebResource.InitBuilder(request, response)
                .requiredBackendUser(true)
                .requiredRoles(Role.CMS_ADMINISTRATOR_ROLE)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();

        APILocator.getDotAIAPI().getEmbeddingsAPI().dropEmbeddingsTable();
        APILocator.getDotAIAPI().getEmbeddingsAPI().initEmbeddingsTable();
        return Response.ok(Map.of(AiKeys.CREATED, true)).build();
    }

    /**
     * Endpoint to count embeddings.
     *
     * @param request the HttpServletRequest object.
     * @param response the HttpServletResponse response.
     * @param site the site parameter.
     * @param contentType the contentType parameter.
     * @param indexName the indexName parameter.
     * @param language the language parameter.
     * @param identifier the identifier parameter.
     * @param inode the inode parameter.
     * @param fieldVar the fieldVar parameter.
     * @return a Response object containing the count of embeddings.
     */
    @Operation(
        summary = "Count embeddings (GET)",
        description = "Counts embeddings based on provided query parameters such as site, content type, index name, etc."
    )
    @ApiResponse(responseCode = "200", 
                description = "JSON object containing embeddingsCount key",
                content = @Content(mediaType = "application/json",
                                  schema = @Schema(type = "object", description = "Count results containing embedding statistics")))
    @ApiResponse(responseCode = "401", 
                description = "Unauthorized - backend user authentication required",
                content = @Content(mediaType = "application/json"))
    @GET
    @JSONP
    @Path("/count")
    @Produces({MediaType.APPLICATION_JSON})
    public final Response count(@Context final HttpServletRequest request,
                                @Context final HttpServletResponse response,
                                @Parameter(description = "Site identifier")
                                @QueryParam("site") final String site,
                                @Parameter(description = "Content type")
                                @QueryParam("contentType") final String contentType,
                                @Parameter(description = "Index name")
                                @QueryParam("indexName") final String indexName,
                                @Parameter(description = "Language identifier")
                                @QueryParam("language") final String language,
                                @Parameter(description = "Content identifier")
                                @QueryParam("identifier") final String identifier,
                                @Parameter(description = "Content inode")
                                @QueryParam("inode") final String inode,
                                @Parameter(description = "Field variable name")
                                @QueryParam("fieldVar") final String fieldVar) {

        new WebResource.InitBuilder(request, response).requiredBackendUser(true).init().getUser();
        final CompletionsForm form = new CompletionsForm.Builder()
                .contentType(contentType)
                .site(site)
                .language(language)
                .fieldVar(fieldVar)
                .indexName(indexName)
                .prompt("NOT USED")
                .build();
        return count(request, response, form);
    }

    /**
     * Endpoint to count embeddings.
     *
     * @param request the HttpServletRequest object.
     * @param response the HttpServletResponse response.
     * @param form the form data for counting embeddings.
     * @return a Response object containing the count of embeddings.
     */
    @Operation(
        summary = "Count embeddings (POST)",
        description = "Counts embeddings based on provided form data containing search criteria"
    )
    @ApiResponse(responseCode = "200", 
                description = "JSON object containing embeddingsCount key",
                content = @Content(mediaType = "application/json",
                                  schema = @Schema(type = "object", description = "Count results containing embedding statistics")))
    @ApiResponse(responseCode = "401", 
                description = "Unauthorized - backend user authentication required",
                content = @Content(mediaType = "application/json"))
    @POST
    @JSONP
    @Path("/count")
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes(MediaType.APPLICATION_JSON)
    public final Response count(@Context final HttpServletRequest request,
                                @Context final HttpServletResponse response,
                                @RequestBody(description = "Completion form containing search criteria for counting embeddings",
                                           required = false,
                                           content = @Content(schema = @Schema(implementation = CompletionsForm.class)))
                                final CompletionsForm form) {

        new WebResource.InitBuilder(request, response).requiredBackendUser(true).init().getUser();
        final EmbeddingsDTO dto = EmbeddingsDTO
                .from(Optional
                        .ofNullable(form)
                        .orElse(new CompletionsForm.Builder().prompt("NOT USED").build()))
                .build();
        return Response.ok(Map.of(AiKeys.EMBEDDINGS_COUNT, APILocator.getDotAIAPI().getEmbeddingsAPI().countEmbeddings(dto))).build();
    }

    /**
     * Endpoint to count embeddings by index.
     *
     * @param request the HttpServletRequest object.
     * @param response the HttpServletResponse response.
     * @return a Response object containing the count of embeddings by index.
     */
    @Operation(
        summary = "Count embeddings by index",
        description = "Returns count of embeddings grouped by index name. Requires CMS Administrator role."
    )
    @ApiResponse(responseCode = "200", 
                description = "JSON Object containing indexCount key",
                content = @Content(mediaType = "application/json",
                                  schema = @Schema(type = "object", description = "Count results containing index statistics grouped by index name")))
    @ApiResponse(responseCode = "401", 
                description = "Unauthorized - backend user authentication required",
                content = @Content(mediaType = "application/json"))
    @ApiResponse(responseCode = "403", 
                description = "Forbidden - CMS Administrator role required",
                content = @Content(mediaType = "application/json"))
    @GET
    @JSONP
    @Path("/indexCount")
    @Produces({MediaType.APPLICATION_JSON})
    public final Response indexCount(@Context final HttpServletRequest request,
                                     @Context final HttpServletResponse response) {
        new WebResource.InitBuilder(request, response)
                .requiredBackendUser(true)
                .requiredRoles(Role.CMS_ADMINISTRATOR_ROLE)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();
        new WebResource.InitBuilder(request, response).requiredBackendUser(true).init().getUser();
        return Response.ok(Map.of(AiKeys.INDEX_COUNT, APILocator.getDotAIAPI().getEmbeddingsAPI().countEmbeddingsByIndex())).build();
    }

}
