package com.dotcms.ai.rest;

import com.dotcms.ai.api.EmbeddingsAPI;
import com.dotcms.ai.db.EmbeddingsDTO;
import com.dotcms.ai.rest.forms.CompletionsForm;
import com.dotcms.ai.util.ContentToStringUtil;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.rest.WebResource;
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
import org.glassfish.jersey.server.JSONP;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Call
 */
@Path("/v1/ai/search")
public class SearchResource {

    @GET
    @JSONP
    @Path("/test")
    @Produces(MediaType.APPLICATION_JSON)
    public final Response testResponse(@Context final HttpServletRequest request, @Context final HttpServletResponse response) {

        Response.ResponseBuilder builder = Response.ok(Map.of("type", "search"), MediaType.APPLICATION_JSON);
        return builder.build();
    }

    @GET
    @JSONP
    @Produces({MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_JSON})
    public final Response searchByGet(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                                      @QueryParam("query") String query,
                                      @DefaultValue("1000") @QueryParam("searchLimit") int searchLimit,
                                      @DefaultValue("0") @QueryParam("searchOffset") int searchOffset,
                                      @QueryParam("site") String site,
                                      @QueryParam("contentType") String contentType,
                                      @DefaultValue("default") @QueryParam("indexName") String indexName,
                                      @DefaultValue(".5") @QueryParam("threshold") float threshold,
                                      @DefaultValue("false") @QueryParam("stream") boolean stream,
                                      @DefaultValue("1024") @QueryParam("responseLength") int responseLength,
                                      @DefaultValue("<=>") @QueryParam("operator") String operator,
                                      @QueryParam("language") String language) throws DotDataException, DotSecurityException, IOException {


        CompletionsForm form = new CompletionsForm.Builder()
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

    @POST
    @JSONP
    @Produces(MediaType.APPLICATION_JSON)
    public final Response searchByPost(@Context final HttpServletRequest request,
                                       @Context final HttpServletResponse response,
                                       CompletionsForm form

    ) throws DotDataException, DotSecurityException, IOException {

        User user = new WebResource.InitBuilder(request, response).requiredBackendUser(true).requiredFrontendUser(true).init().getUser();

        EmbeddingsDTO searcher = EmbeddingsDTO.from(form).withUser(user).build();


        return Response.ok(EmbeddingsAPI.impl().searchForContent(searcher).toString(), MediaType.APPLICATION_JSON).build();


    }

    @GET
    @JSONP
    @Path("/related")
    @Produces(MediaType.APPLICATION_JSON)
    public final Response relatedByGet(@Context final HttpServletRequest request,
                                           @Context final HttpServletResponse response,
                                           @QueryParam("language") long language,
                                           @QueryParam("identifier") String identifier,
                                           @QueryParam("inode") String inode,
                                           @QueryParam("indexName") String indexName,
                                           @QueryParam("fieldVar") String fieldVar) throws DotDataException, DotSecurityException, IOException {



        return relatedByPost(request, response, new JSONObject(Map.of("language", language, "identifier", identifier, "inode", inode, "indexName", indexName, "fieldVar", fieldVar)));

    }

    @POST
    @JSONP
    @Path("/related")
    @Produces(MediaType.APPLICATION_JSON)
    public final Response relatedByPost(@Context final HttpServletRequest request,
                                            @Context final HttpServletResponse response,
                                            JSONObject json



    ) throws DotDataException, DotSecurityException, IOException {
        final String fieldVar = json.optString("fieldVar");
        final String indexName = json.optString("indexName", "default");
        final String inode = json.optString("inode");
        final String identifier = json.optString("identifier");
        final long language = json.optLong("language", APILocator.getLanguageAPI().getDefaultLanguage().getId());

        User user = new WebResource.InitBuilder(request, response).requiredBackendUser(true).requiredFrontendUser(true).init().getUser();


        Host host = WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(request);


        Contentlet contentlet = (UtilMethods.isSet(inode)) ? APILocator.getContentletAPI().find(inode, user, true)
                : APILocator.getContentletAPI().findContentletByIdentifier(identifier, !user.isBackendUser(), language, user, true);

        if (UtilMethods.isEmpty(() -> contentlet.getIdentifier())) {
            Logger.warn(this.getClass(), "unable to find matching contentlet for id:" + identifier + " inode:" + inode + " language:" + language);
            return Response.status(404).build();
        }

        Field fieldToTry = contentlet.getContentType().fieldMap().get(fieldVar);
        List<Field> fields = fieldToTry == null ? ContentToStringUtil.impl.get().guessWhatFieldsToIndex(contentlet) : List.of(fieldToTry);


        Optional<String> contentToRelate = ContentToStringUtil.impl.get().parseFields(contentlet, fields);
        if (contentToRelate.isEmpty()) {
            Logger.warn(this.getClass(), "unable to find matching content for id:" + identifier + " inode:" + inode + " language:" + language);
            return Response.status(404).build();
        }
        EmbeddingsDTO searcher = new EmbeddingsDTO.Builder().withQuery(contentToRelate.get())
                .withIndexName(indexName)
                .withExcludeIndentifiers(new String[]{contentlet.getIdentifier()})
                .withUser(user)
                .withLimit(50)
                .build();

        return Response.ok(EmbeddingsAPI.impl(host).searchForContent(searcher).toString(), MediaType.APPLICATION_JSON).build();


    }


}
