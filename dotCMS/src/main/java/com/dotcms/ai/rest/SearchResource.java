package com.dotcms.ai.rest;

import com.dotcms.ai.AiKeys;
import com.dotcms.ai.api.SearchForContentRequest;
import com.dotcms.ai.config.AiModelConfig;
import com.dotcms.ai.config.AiModelConfigFactory;
import com.dotcms.ai.db.EmbeddingsDTO;
import com.dotcms.ai.rest.forms.CompletionsForm;
import com.dotcms.ai.util.ContentToStringUtil;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.rest.WebResource;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.StringUtils;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.model.User;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.glassfish.jersey.server.JSONP;

import javax.inject.Inject;
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
@Path("/v1/ai/search")
@Tag(name = "AI", description = "AI-powered content generation and analysis endpoints")
public class SearchResource {

    private final AiModelConfigFactory modelConfigFactory;

    @Inject
    public SearchResource(AiModelConfigFactory modelConfigFactory) {
        this.modelConfigFactory = modelConfigFactory;
    }
    /**
     * Handles GET requests to test the response of the search service.
     *
     * @param request the HTTP request
     * @param response the HTTP response
     * @return a Response object containing the test response
     */
    @GET
    @JSONP
    @Path("/test")
    @Produces(MediaType.APPLICATION_JSON)
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
    @GET
    @JSONP
    @Produces({MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_JSON})
    public final Response searchByGet(@Context final HttpServletRequest request,
                                      @Context final HttpServletResponse response,
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
    @POST
    @JSONP
    @Produces(MediaType.APPLICATION_JSON)
    public final Response searchByPost(@Context final HttpServletRequest request,
                                       @Context final HttpServletResponse response,
                                       final CompletionsForm form) {

        final User user = new WebResource.InitBuilder(request, response)
                .requiredBackendUser(true)
                .requiredFrontendUser(true)
                .init()
                .getUser();

        final Host site = WebAPILocator.getHostWebAPI().getHost(request);
        final Optional<AiModelConfig> modelConfigOpt = this.modelConfigFactory.getAiModelConfigOrDefaultEmbedding(site, form.embeddingModel);

        if(modelConfigOpt.isPresent()) {

            CompletionsForm finalForm = form;
            if (StringUtils.isNotSet(form.embeddingModel)) {
                // probably get the default one
                finalForm = CompletionsForm.copy(form).model(modelConfigOpt.get().getName()).build();
            }

            final EmbeddingsDTO searcher = EmbeddingsDTO.from(form).withUser(user).build();

            Logger.debug(this, "Using new AI api for the search rag resource with the form: " + finalForm);
            final JSONObject searchContentResponse = APILocator.getDotAIAPI()
                    .getEmbeddingsAPI().searchForContent(toSearchForContentRequest(form, searcher, modelConfigOpt.get()));
            Logger.debug(this, ()-> "Search content response: " + searchContentResponse);
            return Response.ok(searchContentResponse).build();
        }

        final EmbeddingsDTO searcher = EmbeddingsDTO.from(form).withUser(user).build();

        return Response.ok(
                APILocator.getDotAIAPI().getEmbeddingsAPI().searchForContent(searcher).toString(),
                MediaType.APPLICATION_JSON).build();
    }

    private SearchForContentRequest toSearchForContentRequest(final CompletionsForm form,
                                                    final EmbeddingsDTO searcher,
                                                    final AiModelConfig aiModelConfig) {

        final SearchForContentRequest.Builder builder = SearchForContentRequest.builder()
                .vendorModelPath(form.embeddingModel)
                .prompt(form.prompt)
                .embeddingModelConfig(aiModelConfig)
                .temperature(form.temperature)
                .searcher(searcher);

        return builder.build();
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
    @GET
    @JSONP
    @Path("/related")
    @Produces(MediaType.APPLICATION_JSON)
    public final Response relatedByGet(@Context final HttpServletRequest request,
                                       @Context final HttpServletResponse response,
                                       @QueryParam("language") final long language,
                                       @QueryParam("identifier") final String identifier,
                                       @QueryParam("inode") final String inode,
                                       @QueryParam("indexName") final String indexName,
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
    @POST
    @JSONP
    @Path("/related")
    @Produces(MediaType.APPLICATION_JSON)
    public final Response relatedByPost(@Context final HttpServletRequest request,
                                        @Context final HttpServletResponse response,
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
