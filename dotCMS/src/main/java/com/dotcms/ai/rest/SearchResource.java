package com.dotcms.ai.rest;

import com.dotcms.ai.AiKeys;
import com.dotcms.ai.db.EmbeddingsDTO;
import com.dotcms.ai.rest.forms.CompletionsForm;
import com.dotcms.ai.util.ContentToStringUtil;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.SwaggerCompliant;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
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
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.ws.rs.Consumes;
import org.glassfish.jersey.server.JSONP;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The SearchResource class provides REST endpoints for interacting with the AI search service.
 * It includes methods for searching content based on a given query and finding related content.
 */
@SwaggerCompliant(value = "Modern APIs and specialized services", batch = 7)
@Path("/v1/ai/search")
@Tag(name = "AI")
public class SearchResource {

    /**
     * Handles GET requests to test the response of the search service.
     *
     * @param request the HTTP request
     * @param response the HTTP response
     * @return a Response object containing the test response
     */
    @Operation(
        summary = "Test AI search service",
        description = "Returns a test response to verify the AI search service is operational"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Test response returned successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(type = "object", description = "Simple key-value map indicating search service type")))
    })
    @GET
    @JSONP
    @Path("/test")
    @Produces({MediaType.APPLICATION_JSON})
    public final Response testResponse(@Context final HttpServletRequest request,
                                       @Context final HttpServletResponse response) {

        Response.ResponseBuilder builder = Response.ok(Map.of(AiKeys.TYPE, AiKeys.SEARCH), MediaType.APPLICATION_JSON);
        return builder.build();
    }

    /**
     * Handles GET requests to search content based on a given query.
     *
     * @param request the HTTP request
     * @param response the HTTP response
     * @param query the query to search content from
     * @return a Response object containing the search results
     * @throws IOException if an I/O error occurs
     */
    @Operation(
        summary = "Search content using AI",
        description = "Searches content using AI-powered semantic search with various filtering and configuration options"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Search results returned successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(type = "object", description = "AI search results containing matching contentlets and text fragments"))),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @JSONP
    @Produces({MediaType.APPLICATION_JSON})
    public final Response searchByGet(@Context final HttpServletRequest request,
                                      @Context final HttpServletResponse response,
                                      @Parameter(description = "Search query text", required = true)
                                      @QueryParam("query") String query,
                                      @Parameter(description = "Maximum number of search results to return", example = "1000")
                                      @DefaultValue("1000") @QueryParam("searchLimit") int searchLimit,
                                      @Parameter(description = "Number of results to skip for pagination", example = "0")
                                      @DefaultValue("0") @QueryParam("searchOffset") int searchOffset,
                                      @Parameter(description = "Site identifier to limit search scope")
                                      @QueryParam("site") String site,
                                      @Parameter(description = "Content type to filter search results")
                                      @QueryParam("contentType") String contentType,
                                      @Parameter(description = "Name of the search index to use", example = "default")
                                      @DefaultValue("default") @QueryParam("indexName") String indexName,
                                      @Parameter(description = "Similarity threshold for search results", example = "0.5")
                                      @DefaultValue(".5") @QueryParam("threshold") float threshold,
                                      @Parameter(description = "Whether to stream the response", example = "false")
                                      @DefaultValue("false") @QueryParam("stream") boolean stream,
                                      @Parameter(description = "Maximum length of response in tokens", example = "1024")
                                      @DefaultValue("1024") @QueryParam("responseLength") int responseLength,
                                      @Parameter(description = "Search operator to use", example = "<=>")
                                      @DefaultValue("<=>") @QueryParam("operator") String operator,
                                      @Parameter(description = "Language identifier for search")
                                      @QueryParam("language") String language) {

        final CompletionsForm form = new CompletionsForm.Builder()
                .prompt(query)
                .searchLimit(searchLimit)
                .site(site)
                .language(language)
                .contentType(contentType)
                .searchOffset(searchOffset)
                .threshold(threshold)
                .indexName(indexName)
                .operator(operator)
                .stream(stream)
                .responseLengthTokens(responseLength)
                .build();

        return searchByPost(request, response, form);
    }

    /**
     * Handles POST requests to search content based on a given query.
     *
     * @param request the HTTP request
     * @param response the HTTP response
     * @param form the form data containing the query
     * @return a Response object containing the search results
     */
    @Operation(
        summary = "Search content using AI (POST)",
        description = "Searches content using AI-powered semantic search with form data containing search parameters"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200",
                    description = "Search results returned successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(type = "object", description = "AI search results containing matching contentlets and text fragments"))),
        @ApiResponse(responseCode = "401",
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @POST
    @JSONP
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes(MediaType.APPLICATION_JSON)
    public final Response searchByPost(@Context final HttpServletRequest request,
                                       @Context final HttpServletResponse response,
                                       @RequestBody(description = "Form data containing search query and configuration options",
                                                  required = true,
                                                  content = @Content(schema = @Schema(implementation = CompletionsForm.class)))
                                       final CompletionsForm form) {

        final User user = new WebResource.InitBuilder(request, response)
                .requiredBackendUser(true)
                .requiredFrontendUser(true)
                .init()
                .getUser();

        final EmbeddingsDTO searcher = EmbeddingsDTO.from(form).withUser(user).build();

        return Response.ok(
                APILocator.getDotAIAPI().getEmbeddingsAPI().searchForContent(searcher).toString(),
                MediaType.APPLICATION_JSON).build();
    }

    /**
     * Handles GET requests to find related content based on a given identifier or inode.
     *
     * @param request the HTTP request
     * @param response the HTTP response
     * @param language the language id
     * @param identifier the identifier of the content
     * @param inode the inode of the content
     * @return a Response object containing the related content
     * @throws IOException if an I/O error occurs
     */
    @Operation(
        summary = "Find related content using AI",
        description = "Finds content related to a specific content item using AI-powered semantic similarity"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Related content found successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(type = "object", description = "Related content results containing array of content items with similarity scores and matching text fragments"))),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "Content not found",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @JSONP
    @Path("/related")
    @Produces({MediaType.APPLICATION_JSON})
    public final Response relatedByGet(@Context final HttpServletRequest request,
                                       @Context final HttpServletResponse response,
                                       @Parameter(description = "Language ID for content lookup", required = true)
                                       @QueryParam("language") final long language,
                                       @Parameter(description = "Content identifier")
                                       @QueryParam("identifier") final String identifier,
                                       @Parameter(description = "Content inode")
                                       @QueryParam("inode") final String inode,
                                       @Parameter(description = "Index name to search in")
                                       @QueryParam("indexName") final String indexName,
                                       @Parameter(description = "Field variable name to use for content extraction")
                                       @QueryParam("fieldVar") final String fieldVar)
            throws DotDataException, DotSecurityException {

        return relatedByPost(
                request,
                response,
                new JSONObject(
                        Map.of(
                                AiKeys.LANGUAGE, language,
                                AiKeys.IDENTIFIER, identifier,
                                AiKeys.INODE, inode,
                                AiKeys.INDEX_NAME, indexName,
                                AiKeys.FIELD_VAR, fieldVar)));
    }

    /**
     * Handles POST requests to find related content based on a given identifier or inode.
     *
     * @param request the HTTP request
     * @param response the HTTP response
     * @param json the JSON object containing the identifier or inode
     * @return a Response object containing the related content
     * @throws IOException if an I/O error occurs
     */
    @Operation(
        summary = "Find related content using AI (POST)",
        description = "Finds content related to a specific content item using AI-powered semantic similarity with JSON data"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Related content found successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(type = "object", description = "Simple key-value map indicating search service type"))),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "Content not found",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @POST
    @JSONP
    @Path("/related")
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes(MediaType.APPLICATION_JSON)
    public final Response relatedByPost(@Context final HttpServletRequest request,
                                        @Context final HttpServletResponse response,
                                        @RequestBody(description = "JSON object containing content identifier, inode, language, indexName, and fieldVar", 
                                                   required = true,
                                                   content = @Content(schema = @Schema(type = "object", description = "JSON object containing content identifier, inode, language, indexName, and fieldVar for finding related content")))
                                        final JSONObject json)
            throws DotDataException, DotSecurityException {

        final String fieldVar = json.optString(AiKeys.FIELD_VAR);
        final String indexName = json.optString(AiKeys.INDEX_NAME, AiKeys.DEFAULT);
        final String inode = json.optString(AiKeys.INODE);
        final String identifier = json.optString(AiKeys.IDENTIFIER);
        final long language = json.optLong(AiKeys.LANGUAGE, APILocator.getLanguageAPI().getDefaultLanguage().getId());

        final User user = new WebResource.InitBuilder(request, response)
                .requiredBackendUser(true)
                .requiredFrontendUser(true)
                .init()
                .getUser();
        final Host host = WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(request);
        final Contentlet contentlet =
                (UtilMethods.isSet(inode))
                        ? APILocator.getContentletAPI().find(inode, user, true)
                        : APILocator
                            .getContentletAPI()
                            .findContentletByIdentifier(
                                    identifier,
                                    !user.isBackendUser(),
                                    language,
                                    user,
                                    true);

        if (!UtilMethods.isSet(contentlet) || UtilMethods.isEmpty(contentlet::getIdentifier)) {
            Logger.warn(this.getClass(), getFailMessage(identifier, inode, language));
            throw new NotFoundInDbException("contentlet not found");
        }

        final Field fieldToTry = contentlet.getContentType().fieldMap().get(fieldVar);
        final List<Field> fields = fieldToTry == null
                ? ContentToStringUtil.impl.get().guessWhatFieldsToIndex(contentlet)
                : List.of(fieldToTry);

        final Optional<String> contentToRelate = ContentToStringUtil.impl.get().parseFields(contentlet, fields);
        if (contentToRelate.isEmpty()) {
            Logger.warn(
                    this.getClass(),
                    "unable to find matching content for id:" + identifier + " inode:" + inode + " language:" + language);
            throw new NotFoundInDbException("content not found");
        }

        final EmbeddingsDTO searcher = new EmbeddingsDTO.Builder()
                .withQuery(contentToRelate.get())
                .withIndexName(indexName)
                .withExcludeIndentifiers(new String[]{contentlet.getIdentifier()})
                .withUser(user)
                .withLimit(50)
                .build();

        return Response
                .ok(APILocator.getDotAIAPI().getEmbeddingsAPI(host).searchForContent(searcher).toString(), MediaType.APPLICATION_JSON)
                .build();
    }

    private static String getFailMessage(final String identifier, final String inode, final long language) {
        return "unable to find matching contentlet for id:" + identifier + " inode:" + inode + " language:" + language;
    }

}
