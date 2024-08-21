package com.dotcms.ai.rest;

import com.dotcms.ai.AiKeys;
import com.dotcms.ai.api.BulkEmbeddingsRunner;
import com.dotcms.ai.api.EmbeddingsAPI;
import com.dotcms.ai.db.EmbeddingsDTO;
import com.dotcms.ai.rest.forms.CompletionsForm;
import com.dotcms.ai.rest.forms.EmbeddingsForm;
import com.dotcms.ai.util.OpenAIThreadPool;
import com.dotcms.rest.WebResource;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.model.User;
import org.glassfish.jersey.server.JSONP;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
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
@Path("/v1/ai/embeddings")
public class EmbeddingsResource {

    /**
     * Test endpoint for the EmbeddingsResource.
     *
     * @param request the HttpServletRequest object.
     * @param response the HttpServletResponse object.
     * @return a Response object containing a map with "type" as key and "embeddings" as value.
     */
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
    @POST
    @JSONP
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public final Response embed(@Context final HttpServletRequest request,
                                @Context final HttpServletResponse response,
                                final EmbeddingsForm embeddingsForm) {

        // force authentication
        final User user = new WebResource.InitBuilder(request, response).requiredBackendUser(true).init().getUser();
        long startTime = System.currentTimeMillis();

        if (UtilMethods.isEmpty(embeddingsForm.query)) {
            return Response.ok("query is required").build();
        }

        try {
            int added = 0;
            int newOffset = embeddingsForm.offset;
            for (int i = 0; i < 10000; i++) {
                // searchIndex(String luceneQuery, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles)
                final List<ContentletSearch> searchResults = APILocator
                        .getContentletAPI()
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
                        .collect(Collectors.toList());
                added += inodes.size();
                OpenAIThreadPool.submit(new BulkEmbeddingsRunner(inodes,embeddingsForm));
            }

            final long totalTime = System.currentTimeMillis() - startTime;
            final Map<String, Object> map = Map.of(
                    AiKeys.TIME_TO_EMBEDDINGS, totalTime + "ms",
                    AiKeys.TOTAL_TO_EMBED, added, AiKeys.INDEX_NAME, embeddingsForm.indexName);
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
     * @return a Response object containing the result of the embeddings deletion.
     */
    @DELETE
    @JSONP
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public final Response delete(@Context final HttpServletRequest request,
                                 @Context final HttpServletResponse response,
                                 final JSONObject json) {

        final User user = new WebResource.InitBuilder(request, response).requiredBackendUser(true).init().getUser();

        if (UtilMethods.isSet(() -> json.optString(AiKeys.DELETE_QUERY))){
            final int numberDeleted =
                    EmbeddingsAPI.impl().deleteByQuery(json.optString(AiKeys.DELETE_QUERY),
                            Optional.ofNullable(json.optString(AiKeys.INDEX_NAME)), user);
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
        int deleted = EmbeddingsAPI.impl().deleteEmbedding(dto);
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
    @DELETE
    @JSONP
    @Path("/db")
    @Produces(MediaType.APPLICATION_JSON)
    public final Response dropAndRecreateTables(@Context final HttpServletRequest request,
                                                @Context final HttpServletResponse response,
                                                final JSONObject json) {

        new WebResource.InitBuilder(request, response)
                .requiredBackendUser(true)
                .requiredRoles(Role.CMS_ADMINISTRATOR_ROLE)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();

        EmbeddingsAPI.impl().dropEmbeddingsTable();
        EmbeddingsAPI.impl().initEmbeddingsTable();
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
    @GET
    @JSONP
    @Path("/count")
    @Produces(MediaType.APPLICATION_JSON)
    public final Response count(@Context final HttpServletRequest request,
                                @Context final HttpServletResponse response,
                                @QueryParam("site") final String site,
                                @QueryParam("contentType") final String contentType,
                                @QueryParam("indexName") final String indexName,
                                @QueryParam("language") final String language,
                                @QueryParam("identifier") final String identifier,
                                @QueryParam("inode") final String inode,
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
    @POST
    @JSONP
    @Path("/count")
    @Produces(MediaType.APPLICATION_JSON)
    public final Response count(@Context final HttpServletRequest request,
                                @Context final HttpServletResponse response,
                                final CompletionsForm form) {

        new WebResource.InitBuilder(request, response).requiredBackendUser(true).init().getUser();
        final EmbeddingsDTO dto = EmbeddingsDTO
                .from(Optional
                        .ofNullable(form)
                        .orElse(new CompletionsForm.Builder().prompt("NOT USED").build()))
                .build();
        return Response.ok(Map.of(AiKeys.EMBEDDINGS_COUNT, EmbeddingsAPI.impl().countEmbeddings(dto))).build();
    }

    /**
     * Endpoint to count embeddings by index.
     *
     * @param request the HttpServletRequest object.
     * @param response the HttpServletResponse response.
     * @return a Response object containing the count of embeddings by index.
     */
    @GET
    @JSONP
    @Path("/indexCount")
    @Produces(MediaType.APPLICATION_JSON)
    public final Response indexCount(@Context final HttpServletRequest request,
                                     @Context final HttpServletResponse response) {
        new WebResource.InitBuilder(request, response)
                .requiredBackendUser(true)
                .requiredRoles(Role.CMS_ADMINISTRATOR_ROLE)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();
        new WebResource.InitBuilder(request, response).requiredBackendUser(true).init().getUser();
        return Response.ok(Map.of(AiKeys.INDEX_COUNT, EmbeddingsAPI.impl().countEmbeddingsByIndex())).build();
    }

}