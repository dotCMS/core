package com.dotcms.ai.rest;

import com.dotcms.ai.api.BulkEmbeddingsRunner;
import com.dotcms.ai.api.EmbeddingsAPI;
import com.dotcms.ai.db.EmbeddingsDB;
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
 * Call
 */
@Path("/v1/ai/embeddings")
public class EmbeddingsResource {


    @GET
    @JSONP
    @Path("/test")
    @Produces(MediaType.APPLICATION_JSON)
    public final Response textResource(@Context final HttpServletRequest request, @Context final HttpServletResponse response) {

        return Response.ok(Map.of("type", "embeddings"), MediaType.APPLICATION_JSON).build();

    }

    @POST
    @JSONP
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public final Response embed(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                                EmbeddingsForm embeddingsForm

    ) {
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
                List<ContentletSearch> searchResults = APILocator.getContentletAPI().searchIndex(embeddingsForm.query + " +live:true", embeddingsForm.limit, newOffset, "moddate", user, false);
                if (searchResults.isEmpty()) {
                    break;
                }
                newOffset += embeddingsForm.limit;

                List<String> inodes = searchResults
                        .stream()
                        .map(ContentletSearch::getInode)
                        .collect(Collectors.toList());
                added+=inodes.size();
                OpenAIThreadPool.submit(new BulkEmbeddingsRunner(inodes,embeddingsForm));

            }

            long totalTime = System.currentTimeMillis() - startTime;


            Map<String, Object> map = Map.of("timeToEmbeddings", totalTime + "ms", "totalToEmbed", added, "indexName", embeddingsForm.indexName);
            ResponseBuilder builder = Response.ok(map, MediaType.APPLICATION_JSON);

            return builder.build();
        } catch (Exception e) {
            Logger.error(this.getClass(), e.getMessage(), e);
            return Response.status(500).entity(Map.of("error", e.getMessage())).build();
        }

    }


    @DELETE
    @JSONP
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public final Response delete(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                                 JSONObject json) {
        final User user = new WebResource.InitBuilder(request, response).requiredBackendUser(true).init().getUser();



        if(UtilMethods.isSet(()->json.optString("deleteQuery"))){

            int numberDeleted =
                    EmbeddingsAPI.impl().deleteByQuery(json.optString("deleteQuery"),
                            Optional.ofNullable(json.optString("indexName")), user);

            return Response.ok(Map.of("deleted", numberDeleted)).build();

        }


        EmbeddingsDTO dto = new EmbeddingsDTO.Builder()
                .withIndexName(json.optString("indexName"))
                .withIdentifier(json.optString("identifier"))
                .withLanguage(json.optLong("language", 0))
                .withInode(json.optString("inode"))
                .withContentType(json.optString("contentType"))
                .withHost(json.optString("site"))
                .build();
        int deleted = EmbeddingsAPI.impl().deleteEmbedding(dto);
        return Response.ok(Map.of("deleted", deleted)).build();

    }

    @DELETE
    @JSONP
    @Path("/db")
    @Produces(MediaType.APPLICATION_JSON)
    public final Response dropAndRecreateTables(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                                                JSONObject json) {
        new WebResource.InitBuilder(request, response)
                .requiredBackendUser(true)
                .requiredRoles(Role.CMS_ADMINISTRATOR_ROLE)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();


        EmbeddingsAPI.impl().dropEmbeddingsTable();
        EmbeddingsAPI.impl().initEmbeddingsTable();
        return Response.ok(Map.of("created", true)).build();

    }


    @GET
    @JSONP
    @Path("/count")
    @Produces(MediaType.APPLICATION_JSON)
    public final Response count(@Context final HttpServletRequest request, @Context final HttpServletResponse response,

                                @QueryParam("site") String site,
                                @QueryParam("contentType") String contentType,
                                @QueryParam("indexName") String indexName,
                                @QueryParam("language") String language,
                                @QueryParam("identifier") String identifier,
                                @QueryParam("inode") String inode,
                                @QueryParam("fieldVar") String fieldVar) {
        new WebResource.InitBuilder(request, response).requiredBackendUser(true).init().getUser();

        CompletionsForm form = new CompletionsForm.Builder().contentType(contentType).site(site).language(language).fieldVar(fieldVar).indexName(indexName).prompt("NOT USED").build();
        return count(request, response, form);


    }

    @POST
    @JSONP
    @Path("/count")
    @Produces(MediaType.APPLICATION_JSON)
    public final Response count(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                                CompletionsForm form) {
        new WebResource.InitBuilder(request, response).requiredBackendUser(true).init().getUser();

        form = (form == null) ? new CompletionsForm.Builder().prompt("NOT USED").build() : form;
        EmbeddingsDTO dto = EmbeddingsDTO.from(form).build();


        return Response.ok(Map.of("embeddingsCount", EmbeddingsDB.impl.get().countEmbeddings(dto))).build();

    }

    @GET
    @JSONP
    @Path("/indexCount")
    @Produces(MediaType.APPLICATION_JSON)
    public final Response indexCount(@Context final HttpServletRequest request, @Context final HttpServletResponse response) {
        new WebResource.InitBuilder(request, response)
                .requiredBackendUser(true)
                .requiredRoles(Role.CMS_ADMINISTRATOR_ROLE)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();
        new WebResource.InitBuilder(request, response).requiredBackendUser(true).init().getUser();

        return Response.ok(Map.of("indexCount", EmbeddingsDB.impl.get().countEmbeddingsByIndex())).build();

    }
}
