package com.dotcms.rest.api.v1.page;

import static com.dotcms.util.DotPreconditions.checkNotNull;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.content.elasticsearch.business.ESSearchResults;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.ema.EMAConfigurationEntry;
import com.dotcms.ema.EMAConfigurations;
import com.dotcms.ema.EMAWebInterceptor;
import com.dotcms.ema.resolver.EMAConfigStrategy;
import com.dotcms.ema.resolver.EMAConfigStrategyResolver;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityBooleanView;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.v1.page.ImmutablePageRenderParams.Builder;
import com.dotcms.rest.api.v1.personalization.PersonalizationPersonaPageViewPaginator;
import com.dotcms.rest.api.v1.workflow.WorkflowResource;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotcms.util.ConversionUtils;
import com.dotcms.util.HttpRequestDataUtil;
import com.dotcms.util.PaginationUtil;
import com.dotcms.util.TimeMachineUtil;
import com.dotcms.util.pagination.ContentTypesPaginator;
import com.dotcms.util.pagination.OrderDirection;
import com.dotcms.vanityurl.business.VanityUrlAPI;
import com.dotcms.vanityurl.model.CachedVanityUrl;
import com.dotcms.vanityurl.model.VanityUrlResult;
import com.dotcms.variant.VariantAPI;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionLevel;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.cms.urlmap.URLMapInfo;
import com.dotmarketing.cms.urlmap.UrlMapContextBuilder;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.transform.DotTransformerBuilder;
import com.dotmarketing.portlets.contentlet.util.ContentletUtil;
import com.dotmarketing.portlets.htmlpageasset.business.render.HTMLPageAssetNotFoundException;
import com.dotmarketing.portlets.htmlpageasset.business.render.HTMLPageAssetRenderedAPI;
import com.dotmarketing.portlets.htmlpageasset.business.render.PageContextBuilder;
import com.dotmarketing.portlets.htmlpageasset.business.render.PageLivePreviewVersionBean;
import com.dotmarketing.portlets.htmlpageasset.business.render.VanityURLView;
import com.dotmarketing.portlets.htmlpageasset.business.render.page.EmptyPageView;
import com.dotmarketing.portlets.htmlpageasset.business.render.page.PageView;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.util.*;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;
import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.collections.keyvalue.MultiKey;
import org.glassfish.jersey.server.JSONP;

/**
 * Provides different methods to access information about HTML Pages in dotCMS. For example,
 * users of this end-point can get the metadata of an HTML Page (i.e., information about the
 * different data structures that make up a page), the final render of a page, etc.
 *
 * @author Jose Castro
 * @version 4.2
 * @since Oct 6, 2017
 */


@Path("/v1/page")
@Tag(name = "Page", 
        description = "Endpoints that operate on pages",
        externalDocs = @ExternalDocumentation(description = "Additional Page API information", 
                                                url = "https://www.dotcms.com/docs/latest/page-rest-api-layout-as-a-service-laas"))

public class PageResource {

    // publishDate is an alias for the timeMachine query parameter
    public static final String PUBLISH_DATE = "publishDate";
    //Time Machine, Request and session attributes
    public static final String TM_DATE = "tm_date";
    public static final String TM_LANG = "tm_lang";
    public static final String TM_HOST = "tm_host";
    public static final String DOT_CACHE = "dotcache";
    public static final String IS_PAGE_RESOURCE = "pageResource";

    private final PageResourceHelper pageResourceHelper;
    private final WebResource webResource;
    private final HTMLPageAssetRenderedAPI htmlPageAssetRenderedAPI;
    private final ContentletAPI esapi;
    private final HostWebAPI hostWebAPI;
    /**
     * Creates an instance of this REST end-point.
     */
    @SuppressWarnings("unused")
    public PageResource() {
        this(
                PageResourceHelper.getInstance(),
                new WebResource(),
                APILocator.getHTMLPageAssetRenderedAPI(),
                APILocator.getContentletAPI(),
                WebAPILocator.getHostWebAPI()
        );
    }

    @VisibleForTesting
    PageResource(
            final PageResourceHelper pageResourceHelper,
            final WebResource webResource,
            final HTMLPageAssetRenderedAPI htmlPageAssetRenderedAPI,
            final ContentletAPI esapi,
            final HostWebAPI hostWebAPI) {

        this.pageResourceHelper = pageResourceHelper;
        this.webResource = webResource;
        this.htmlPageAssetRenderedAPI = htmlPageAssetRenderedAPI;
        this.esapi = esapi;
        this.hostWebAPI = hostWebAPI;
    }

    /**
     * Returns the metadata -- i.e.; the objects that make up an HTML Page -- in the form of a JSON
     * object based on the specified URI. If such a URI maps to a Vanity URL, the response will
     * change based on its response code:
     * <ul>
     *     <li>If a {@code 200 Forward} states is set, the JSON metadata will include the actual
     *     page that the Vanity URL is referencing.</li>
     *     <li>If a {@code 302 Temporary Redirect} or {@code 301 Permanent Redirect} is set, the
     *     JSON metadata will include an "empty" page JSON, and the Vanity URL properties exposed by
     *     the {@link CachedVanityUrl}.</li>
     * </ul>
     * Here's an example of how this method works:
     * <pre>
     * Format:
     * http://localhost:8080/api/v1/page/json/{page-url-or-vanity-url}
     * <br/>
     * Example:
     * http://localhost:8080/api/v1/page/json/about-us/locations/index
     * </pre>
     *
     * @param originalRequest The {@link HttpServletRequest} object.
     * @param response        The {@link HttpServletResponse} object.
     * @param uri             The path to the HTML Page whose information will be retrieved.
     * @param modeParam       {@link PageMode}
     * @param personaId       {@link com.dotmarketing.portlets.personas.model.Persona}'s identifier
     *                        to render the page
     * @param languageId      {@link com.dotmarketing.portlets.languagesmanager.model.Language}'s Id
     *                        to render the page
     * @param deviceInode     {@link }'s inode to render the page
     *
     * @return All the objects on an associated HTML Page.
     *
     * @throws DotDataException     An error occurred when accessing information in the database.
     * @throws DotSecurityException The currently logged-in user does not have the necessary
     *                              permissions to call this action.
     */
    @NoCache
    @GET
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Path("/json/{uri: .*}")
    public Response loadJson(@Context final HttpServletRequest originalRequest,
            @Context final HttpServletResponse response,
            @PathParam("uri") final String uri,
            @QueryParam(WebKeys.PAGE_MODE_PARAMETER) final String modeParam,
            @QueryParam(WebKeys.CMS_PERSONA_PARAMETER) final String personaId,
            @QueryParam("language_id") final String languageId,
            @QueryParam("device_inode") final String deviceInode,
            @QueryParam(PUBLISH_DATE) final String timeMachineDateAsISO8601
            ) throws DotDataException, DotSecurityException {
        Logger.debug(this, () -> String.format(
                "Rendering page as JSON: uri -> %s , mode -> %s , language -> %s , persona -> %s , device_inode -> %s, timeMachineDate -> %s",
                uri, modeParam, languageId, personaId, deviceInode, timeMachineDateAsISO8601));
        final HttpServletRequest request = this.pageResourceHelper.decorateRequest (originalRequest);
        // Force authentication
        final InitDataObject initData =
                new WebResource.InitBuilder(webResource)
                        .requestAndResponse(request, response)
                        .rejectWhenNoUser(true)
                        .init();
        final User user = initData.getUser();
        final Builder builder = ImmutablePageRenderParams.builder();
        builder
                .originalRequest(originalRequest)
                .request(request)
                .response(response)
                .user(user)
                .uri(uri)
                .asJson(true);

        final PageRenderParams renderParams = optionalRenderParams(modeParam,
                languageId, deviceInode, timeMachineDateAsISO8601, builder);
        return getPageRender(renderParams);
    }

    /**
     * Returns the metadata -- i.e.; the objects that make up an HTML Page, including the rendered
     * HTML code from the page and its containers -- in the form of a JSON object based on the
     * specified URI. If such a URI maps to a Vanity URL, the response will change based on its
     * response code:
     * <ul>
     *     <li>If a {@code 200 Forward} states is set, the JSON metadata will include the actual
     *     page that the Vanity URL is referencing.</li>
     *     <li>If a {@code 302 Temporary Redirect} or {@code 301 Permanent Redirect} is set, the
     *     JSON metadata will include an "empty" page JSON, and the Vanity URL properties exposed by
     *     the {@link CachedVanityUrl}.</li>
     * </ul>
     * Here's an example of how this method works:
     * <pre>
     * Format:
     * http://localhost:8080/api/v1/page/render/{page-url-or-vanity-url}?mode={mode}&com.dotmarketing.persona.id={personaId}&language_id={languageId}&device_inode={deviceInode}
     * <br/>
     * Example:
     * http://localhost:8080/api/v1/page/render/about-us/locations/index?language_id=1
     * </pre>
     *
     * @param originalRequest The {@link HttpServletRequest} object.
     * @param response        The {@link HttpServletResponse} object.
     * @param uri             The path to the HTML Page whose information will be retrieved, or a
     *                        Vanity URL.
     * @param modeParam       The current {@link PageMode} used to render the page.
     * @param personaId       The {@link com.dotmarketing.portlets.personas.model.Persona}'s
     *                        identifier used to render the page.
     * @param languageId      The
     *                        {@link com.dotmarketing.portlets.languagesmanager.model.Language}'s ID
     *                        to render the page.
     * @param deviceInode     The {@link java.lang.String}'s inode to render the page. This is used
     *                        to render the page with specific width and height dimensions.
     *
     * @return The HTML Page's metadata -- or the associated Vanity URL data -- in JSON format.
     *
     * @throws DotDataException     An error occurred when accessing information in the database.
     * @throws DotSecurityException The currently logged-in user does not have the necessary
     *                              permissions to call this action.
     */
    @NoCache
    @GET
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Path("/render/{uri: .*}")
    public Response render(@Context final HttpServletRequest originalRequest,
            @Context final HttpServletResponse response,
            @PathParam("uri") final String uri,
            @QueryParam(WebKeys.PAGE_MODE_PARAMETER) final String modeParam,
            @QueryParam(WebKeys.CMS_PERSONA_PARAMETER) final String personaId,
            @QueryParam(WebKeys.LANGUAGE_ID_PARAMETER) final String languageId,
            @QueryParam("device_inode") final String deviceInode,
            @QueryParam(PUBLISH_DATE) final String timeMachineDateAsISO8601
    ) throws DotSecurityException, DotDataException {
        if (Boolean.TRUE.equals(HttpRequestDataUtil.getAttribute(originalRequest, EMAWebInterceptor.EMA_REQUEST_ATTR, false))
                && !this.includeRenderedAttrFromEMA(originalRequest, uri)) {
            final String depth = HttpRequestDataUtil.getAttribute(originalRequest, EMAWebInterceptor.DEPTH_PARAM, null);
            if (UtilMethods.isSet(depth)) {
                HttpServletRequestThreadLocal.INSTANCE.getRequest().setAttribute(WebKeys.HTMLPAGE_DEPTH, depth);
            }
            return this.loadJson(originalRequest, response, uri, modeParam, personaId, languageId
                    , deviceInode, timeMachineDateAsISO8601);
        }
        Logger.debug(this, () -> String.format(
                "Rendering page: uri -> %s , mode -> %s , language -> %s , persona -> %s , device_inode -> %s , timeMachineDate -> %s",
                uri, modeParam, languageId, personaId, deviceInode, timeMachineDateAsISO8601));
        final HttpServletRequest request = this.pageResourceHelper.decorateRequest(originalRequest);
        // Force authentication
        final InitDataObject initData =
                new WebResource.InitBuilder(webResource)
                        .requestAndResponse(request, response)
                        .rejectWhenNoUser(true)
                        .init();
        final User user = initData.getUser();


        final Builder builder = ImmutablePageRenderParams.builder();
        builder.originalRequest(originalRequest)
                .request(request)
                .response(response)
                .user(user)
                .uri(uri);
        final PageRenderParams renderParams = optionalRenderParams(modeParam,
                languageId, deviceInode, timeMachineDateAsISO8601, builder);
        return getPageRender(renderParams);
    }

    /**
     * Returns the metadata -- i.e.; the objects that make up an HTML Page -- in the form of a JSON
     * @param modeParam      The current {@link PageMode} used to render the page.
     * @param languageId    The {@link com.dotmarketing.portlets.languagesmanager.model.Language}'s
     * @param deviceInode  The {@link java.lang.String}'s inode to render the page.
     * @param timeMachineDateAsISO8601 {@link String} with the date to set the Time Machine to in ISO8601 format.
     * @param builder The builder to use to create the {@link PageRenderParams}.
     * @return The {@link PageRenderParams} object.
     */
    private PageRenderParams optionalRenderParams(final String modeParam,
            final String languageId, final String deviceInode,
            final String timeMachineDateAsISO8601,
            Builder builder) {
        if (null != languageId){
            builder.languageId(languageId);
        }
        if (null != modeParam){
            builder.modeParam(modeParam);
        }
        if (null != deviceInode){
            builder.deviceInode(deviceInode);
        }
        TimeMachineUtil.parseTimeMachineDate(timeMachineDateAsISO8601).ifPresentOrElse(
                builder::timeMachineDate,
                () -> Logger.debug(this, () -> String.format(
                        "Date %s is not older than the grace window. Skipping Time Machine setup.",
                        timeMachineDateAsISO8601))
        );
        return builder.build();
    }

    /**
     * Returns the building blocks of an HTML Page including either its rendered version, or just
     * its metadata in the form of a JSON object. Vanity URLs are taken into account, in which case
     * Temporary or Permanent Redirects will include an "empty" page object; whereas a Forward will
     * return the metadata of the page that the Vanity URL points to.
     * @param renderParams The parameters used to render the page.
     * @return The HTML Page's metadata -- or the associated Vanity URL data -- in JSON format.
     *
     * @throws DotDataException     An error occurred when accessing information in the database.
     * @throws DotSecurityException The currently logged-in user does not have the necessary
     *                              permissions to call this action.
     */
    private Response getPageRender(final PageRenderParams renderParams) throws DotDataException,
            DotSecurityException {

        final HttpServletRequest request = renderParams.request();
        final HttpServletResponse response = renderParams.response();

        //Let's set up the Time Machine if needed
        setUpTimeMachineIfPresent(renderParams);

            String resolvedUri = renderParams.uri();
            final Optional<CachedVanityUrl> cachedVanityUrlOpt =
                    this.pageResourceHelper.resolveVanityUrlIfPresent(
                            renderParams.originalRequest(), renderParams.uri(),
                            renderParams.languageId());
            if (cachedVanityUrlOpt.isPresent()) {
                response.setHeader(VanityUrlAPI.VANITY_URL_RESPONSE_HEADER,
                        cachedVanityUrlOpt.get().vanityUrlId);
                if (cachedVanityUrlOpt.get().isTemporaryRedirect() || cachedVanityUrlOpt.get()
                        .isPermanentRedirect()) {
                    Logger.debug(this, () -> String.format("Incoming Vanity URL is a %d Redirect",
                            cachedVanityUrlOpt.get().response));

                    final CachedVanityUrl cachedVanityUrl = cachedVanityUrlOpt.get();
                    final String finalForwardTo = cachedVanityUrlOpt.get().handle(renderParams.uri()).getRewrite();
                    final CachedVanityUrl finalCachedVanityUrl = new CachedVanityUrl( cachedVanityUrl.vanityUrlId,
                    cachedVanityUrl.url, cachedVanityUrl.languageId, cachedVanityUrl.siteId, finalForwardTo, cachedVanityUrl.response, cachedVanityUrl.order);


                    final EmptyPageView emptyPageView =
                            new EmptyPageView.Builder().vanityUrl(finalCachedVanityUrl).build();

                    return Response.ok(new ResponseEntityView<>(emptyPageView)).build();
                } else {
                    final VanityUrlResult vanityUrlResult = cachedVanityUrlOpt.get()
                            .handle(renderParams.uri());
                    resolvedUri = vanityUrlResult.getRewrite();
                    Logger.debug(this,
                            () -> String.format("Incoming Vanity URL resolved to URI: %s",
                                    vanityUrlResult.getRewrite()));
                }
            }
            final Optional<String> modeParam = renderParams.modeParam();
            final PageMode mode = modeParam.isPresent()
                    ? PageMode.get(modeParam.get())
                    : this.htmlPageAssetRenderedAPI.getDefaultEditPageMode(renderParams.user(),
                            request, resolvedUri);
            PageMode.setPageMode(renderParams.request(), mode);
            final Optional<String> deviceInode = renderParams.deviceInode();
            if (deviceInode.isPresent() && StringUtils.isSet(deviceInode.get())) {
                request.getSession()
                        .setAttribute(WebKeys.CURRENT_DEVICE,deviceInode.get());
            } else {
                final HttpSession session = request.getSession(false);
                if (null != session) {
                    session.removeAttribute(WebKeys.CURRENT_DEVICE);
                }
            }
            final PageView pageRendered;
            final PageContextBuilder pageContextBuilder = PageContextBuilder.builder()
                    .setUser(renderParams.user())
                    .setPageUri(resolvedUri)
                    .setPageMode(mode);
            cachedVanityUrlOpt.ifPresent(cachedVanityUrl
                    -> pageContextBuilder.setVanityUrl(
                    new VanityURLView.Builder().vanityUrl(cachedVanityUrl).build()));
            if (renderParams.asJson()) {
                pageRendered = this.htmlPageAssetRenderedAPI.getPageMetadata(
                        pageContextBuilder
                                .setParseJSON(true)
                                .build(),
                        request, response
                );
            } else {
                pageRendered = this.htmlPageAssetRenderedAPI.getPageRendered(
                        pageContextBuilder.build(), request, response
                );
            }
            final Host site = APILocator.getHostAPI()
                    .find(pageRendered.getPage().getHost(), renderParams.user(),
                            PageMode.get(request).respectAnonPerms);
            request.setAttribute(WebKeys.CURRENT_HOST, site);
            request.getSession().setAttribute(WebKeys.CURRENT_HOST, site);
            return Response.ok(new ResponseEntityView<>(pageRendered)).build();

    }

    /**
     * Sets up the Time Machine if the request includes a Time Machine date.
     * @param renderParams The parameters used to render the page.
     */
    private void setUpTimeMachineIfPresent(final PageRenderParams renderParams) {
        final Optional<Instant> timeMachineDate = renderParams.timeMachineDate();
        if(timeMachineDate.isPresent()){
            final String timeMachineEpochMillis = String.valueOf(timeMachineDate.get().toEpochMilli());
            final Optional<Host> host = currentHost(renderParams);
            if(host.isEmpty()){
                throw new IllegalArgumentException("Unable to set a host for the Time Machine");
            }
            final HttpServletRequest request = renderParams.request();
            final HttpSession session = request.getSession(false);
            if (null != session) {
               session.setAttribute(TM_DATE, timeMachineEpochMillis);
               session.setAttribute(TM_LANG, renderParams.languageId());
               session.setAttribute(DOT_CACHE, "refresh");
               session.setAttribute(TM_HOST, host.get());
               session.setAttribute(IS_PAGE_RESOURCE, true);
            }
               request.setAttribute(TM_DATE, timeMachineEpochMillis);
               request.setAttribute(TM_LANG, renderParams.languageId());
               request.setAttribute(DOT_CACHE, "refresh");
               request.setAttribute(TM_HOST, host.get());
               request.setAttribute(IS_PAGE_RESOURCE, true);
        } else {
            resetTimeMachine(renderParams.request());
        }
    }

    /**
     * Removes the Time Machine attributes from the session.
     * @param request The current instance of the {@link HttpServletRequest}.
     */
    private void resetTimeMachine(final HttpServletRequest request) {
        final HttpSession session = request.getSession(false);
        if (null != session) {
            session.removeAttribute(TM_DATE);
            session.removeAttribute(TM_LANG);
            session.removeAttribute(TM_HOST);
            session.removeAttribute(DOT_CACHE);
            // we do not remove the IS_PAGE_RESOURCE attribute
            //It'll get removed from the old from the time machine portal
        }
    }

    /**
     * Returns the metadata of an HTML Page based on the specified URI. If such a URI maps to a
     * @param renderParams The parameters used to render the page.
     * @return current host if any, otherwise the default host.
     */
    private Optional<Host> currentHost(final PageRenderParams renderParams) {
         Host currentHost = hostWebAPI.getCurrentHostNoThrow(renderParams.request());
        if (null == currentHost) {
            try {
                currentHost = hostWebAPI.findDefaultHost(renderParams.user(), false);
            } catch ( DotSecurityException | DotDataException e) {
                Logger.error(this, "Error getting default host", e);
            }
        }
        return Optional.ofNullable(currentHost);
    }

    /**
     * Saves a Template and links it with an HTML Page. If the page already has an Anonymous
     * Template linked to it, it will be updated in these new changes. Otherwise, a new Anonymous
     * Template is created and the previously linked Template will remain unchanged.
     *
     * @param request          The current instance of the {@link HttpServletRequest}.
     * @param response         The current instance of the {@link HttpServletResponse}.
     * @param pageId           The ID of the page that the Template will be linked to.
     * @param variantNameParam The name of the Variant associated to the page.
     * @param form             The {@link PageForm} containing the information of the Template.
     *
     * @return The {@link Response} entity containing the updated {@link PageView} object for the
     * specified page.
     *
     * @throws DotSecurityException The currently logged-in user does not have the necessary
     *                              permissions to perform this action.
     */
    @NoCache
    @POST
    @Path("/{pageId}/layout")
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Consumes({MediaType.APPLICATION_JSON})
    @Operation(operationId = "postPageLayoutHTMLLink",
        summary = "Links template and page",
        description = "Takes a saved template and links it to an HTML page.\n\n" +
                    "Any pages with a template already linked will update with the new link.\n\n" +
                    "Otherwise a new template will be created without making any changes to previous templates.\n\n" +
                    "Returns the rendered page.\n\n",
        tags = {"Page"},
        responses = {
                @ApiResponse(responseCode = "200", description = "Page template linked to HTML and saved successfully",
                        content = @Content(mediaType = "application/json", 
                                schema = @Schema(implementation = ResponseEntityView.class)
                                )
                        ),
                @ApiResponse(responseCode = "400", description = "Bad request or data exception"),
                @ApiResponse(responseCode = "404", description = "Page not found")
                })
    public Response saveLayout(
                @Context final HttpServletRequest request,
                @Context final HttpServletResponse response,
                @PathParam("pageId") @Parameter(description = "ID for the page will link to") final String pageId,
                @QueryParam("variantName") final String variantNameParam,
                @RequestBody(description = "POST body consists of a JSON object containing " + 
                                        "one property called 'PageForm', which contains information " +
                                        "about the layout of a page's template ",
                                required = true,
                                content = @Content(
                                        schema = @Schema(implementation = PageForm.class)
                                        )
                                )
                final PageForm form) 
                throws DotSecurityException {                

        final String variantName = UtilMethods.isSet(variantNameParam) ? variantNameParam :
                VariantAPI.DEFAULT_VARIANT.name();

        Logger.debug(this, () -> String.format("Saving layout: pageId -> %s , layout -> %s , variantName -> %s", pageId,
                form != null ? form.getLayout() : null, variantName));
        checkNotNull(form, BadRequestException.class, "The 'PageForm' JSON data is required");

        final InitDataObject auth = webResource.init(request, response, true);
        final User user = auth.getUser();
        try {
            HTMLPageAsset page = (HTMLPageAsset) this.pageResourceHelper.getPage(user, pageId, request);

            page = this.pageResourceHelper.saveTemplate(user, page, form);

            final PageView renderedPage = this.htmlPageAssetRenderedAPI.getPageRendered(
                    PageContextBuilder.builder()
                            .setUser(user)
                            .setPage(page)
                            .setPageMode(PageMode.PREVIEW_MODE)
                            .build(),
                    request,
                    response
            );

            return Response.ok(new ResponseEntityView<>(renderedPage)).build();
        } catch (final DoesNotExistException e) {
            final String errorMsg = String.format("DoesNotExistException on PageResource.saveLayout. Parameters: [ %s ], [ %s ], [ %s ]: ",
                    request, pageId, form);
            Logger.error(this, errorMsg, e);
            return ExceptionMapperUtil.createResponse("", "Unable to find page with Identifier: " + pageId, Response.Status.NOT_FOUND);
        } catch (final BadRequestException | DotDataException e) {
            final String errorMsg = String.format("%s on PageResource.saveLayout. Parameters: [ %s ], [ %s ], [ %s ]: ",
                    e.getClass().getCanonicalName(), request, pageId, form);
            Logger.error(this, errorMsg, e);
            return ExceptionMapperUtil.createResponse(e, Response.Status.BAD_REQUEST);
        }
    }

    /**
     * Save a template.
     *
     * @see Template#isAnonymous()
     *
     * @param request The {@link HttpServletRequest} object.
     * @param form The {@link PageForm}
     * @return
     */
    @NoCache
    @POST
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Consumes({MediaType.APPLICATION_JSON})
    @Path("/layout")
    @Operation(operationId = "postPageLayout",
                summary = "Saves a page template",
                description = "Handles saving of a page template using provided data. " +
                                "Method processes the request and returns HTTP response indicating a complete save operation.",
                tags = {"Page"},
                responses = {
                        @ApiResponse(responseCode = "200", description = "Page template saved successfully",
                                content = @Content(mediaType = "application/json", 
                                        schema = @Schema(implementation = ResponseEntityView.class)
                                        )
                                ),
                        @ApiResponse(responseCode = "400", description = "Bad request or data exception"),
                        @ApiResponse(responseCode = "404", description = "Page not found")
                })
    public Response saveLayout(
                @Context final HttpServletRequest request, 
                @Context final HttpServletResponse response, 
                @RequestBody(description = "POST body consists of a JSON object containing " + 
                                        "one property called 'PageForm', which contains a " +
                                        "template layout for the page",
                                required = true,
                                content = @Content(
                                        schema = @Schema(implementation = PageForm.class)
                                        )
                                )
                final PageForm form) 
                throws DotDataException {

        final InitDataObject auth = webResource.init(request, response, true);
        final User user = auth.getUser();

        Response res = null;

        try {

            final Template templateSaved = this.pageResourceHelper.saveTemplate(user, form);
            res = Response.ok(new ResponseEntityView<>(templateSaved)).build();

        } catch (DotSecurityException e) {
            final String errorMsg = String.format("DotSecurityException on PageResource.saveLayout, parameters:  %s, %s: ",
                    request, form);
            Logger.error(this, errorMsg, e);
            throw new ForbiddenException(e);
        } catch (BadRequestException e) {
            final String errorMsg = String.format("%s on PageResource.saveLayout, parameters:  %s, %s: ",
                    e.getClass().getCanonicalName(), request, form);
            Logger.error(this, errorMsg, e);
            res = ExceptionMapperUtil.createResponse(e, Response.Status.BAD_REQUEST);
        }

        return res;
    }

    /**
     * Updates all the contents in an HTML Page. This method is used to update changes when both adding or removing
     * Contentlets from Containers. Also, it allows to update the style properties of the Contentlets.
     * It takes a JSON object -- serialized as a {@link PageContainerForm} object -- in
     * the following format:
     * <pre>
     *     {@code
     *     [
     *          {
     *              "identifier": "{CONTAINER-1-ID}",
     *              "uuid": "dotParser_{UNIQUE-ID}",
     *              "modified": "{ADDED-OR-REMOVED-CONTENTLET-ID}", // Optional
     *              "contentletsId": [
     *                  "CONTENTLET-IDENTIFIER-1",
     *                  "CONTENTLET-IDENTIFIER-2",
     *                  "CONTENTLET-IDENTIFIER-3",
     *                  "..."
     *              ],
     *              "styleProperties": {
     *                  "CONTENTLET-IDENTIFIER-1": {
     *                      "width": "100px",
     *                      "color": "red",
     *                  },
     *                  "CONTENTLET-IDENTIFIER-3": {
     *                      "fontSize": "16px"
     *                  }
     *              }
     *          },
     *          {
     *              "identifier": "{CONTAINER-2-ID}",
     *              "uuid": "dotParser_{UNIQUE-ID}",
     *              "modified": "{ADDED-OR-REMOVED-CONTENTLET-ID}", // Optional
     *              "contentletsId": [
     *                  "CONTENTLET-IDENTIFIER-4",
     *                  "CONTENTLET-IDENTIFIER-5",
     *                  "CONTENTLET-IDENTIFIER-6",
     *                  "..."
     *              ],
     *              "styleProperties": {
     *                  "CONTENTLET-IDENTIFIER-4": {
     *                      "backgroundColor": "blue"
     *                  },
     *                  "..."
     *              }
     *          }
     *      ]
     *     }
     * </pre>
     *
     * @param request The current instance of the {@link HttpServletRequest}.
     * @param pageId The ID of the HTML Page whose contents are being updated.
     * @param pageContainerForm The {@link PageContainerForm} containing the basic information of every Container and
     *                          their respective Contentlets.
     * @return The {@link Response} entity.
     */
    @POST
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{pageId}/content")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Contentlets saved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples =  @ExampleObject(value = "{\n"
                                    + "    \"entity\": [\n"
                                    + "        {\n"
                                    + "            \"containerId\": \"some-container-id\",\n"
                                    + "            \"contentletId\": \"some-contentlet-id\",\n"
                                    + "            \"styleProperties\": {\n"
                                    + "                \"color\": \"#FF0000\",\n"
                                    + "                \"margin\": \"10px\",\n"
                                    + "                \"width\": \"100px\"\n"
                                    + "            },\n"
                                    + "            \"uuid\": \"some-uuid\"\n"
                                    + "        },\n"
                                    + "        {\n"
                                    + "            \"containerId\": \"other-container-id\",\n"
                                    + "            \"contentletId\": \"other-contentlet-id\",\n"
                                    + "            \"styleProperties\": null,\n"
                                    + "            \"uuid\": \"other-uuid\"\n"
                                    + "        }\n"
                                    + "    ],\n"
                                    + "    \"errors\": [],\n"
                                    + "    \"i18nMessagesMap\": {},\n"
                                    + "    \"messages\": [],\n"
                                    + "    \"pagination\": null,\n"
                                    + "    \"permissions\": []\n"
                                    + "}"
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Bad request or data exception"),
    })
    public final Response addContent(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam("pageId") final String pageId,
            @QueryParam("variantName") final String variantNameParam,
            final PageContainerForm pageContainerForm)
            throws DotSecurityException, DotDataException {

        final String variantName = UtilMethods.isSet(variantNameParam) ? variantNameParam :
                VariantAPI.DEFAULT_VARIANT.name();

        Logger.debug(this, ()->String.format("Saving page's content: %s",
                pageContainerForm != null ? pageContainerForm.getRequestJson() : null));

        final InitDataObject initData = webResource.init(request, response,true);

        if (pageContainerForm == null) {
            throw new BadRequestException("Layout is required");
        }

        try {
            final User user = initData.getUser();

            final IHTMLPage page = pageResourceHelper.getPage(user, pageId, request);

            APILocator.getPermissionAPI().checkPermission(page, PermissionLevel.EDIT, user);

            final Language language = WebAPILocator.getLanguageWebAPI().getLanguage(request);
            this.validateContainerEntries(pageContainerForm.getContainerEntries());

            // Save content and Get the saved contentlets
            final List<ContentView> savedContent = pageResourceHelper.saveContent(
                    pageId, this.reduce(pageContainerForm.getContainerEntries()), language, variantName);

            return Response.ok(new ResponseEntityContentView(savedContent)).build();
        } catch(HTMLPageAssetNotFoundException e) {
            final String errorMsg = String.format("HTMLPageAssetNotFoundException on PageResource.addContent, pageId: %s: ",
                    pageId);
            Logger.error(this, errorMsg, e);
            return ExceptionMapperUtil.createResponse(e, Response.Status.NOT_FOUND);
        }
    }

    protected void validateContainerEntries(final List<PageContainerForm.ContainerEntry> containerEntries) {

        final Map<String, Set<String>> containerContentTypesMap = new HashMap<>();
        for (final PageContainerForm.ContainerEntry containerEntry : containerEntries) {

            final String containerId = containerEntry.getContainerId();
            final Set<String> contentTypeSet    = containerContentTypesMap.computeIfAbsent(containerId,  key -> this.getContainerContentTypes(containerId));
            final List<String> contentletIdList = containerEntry.getContentIds();
            for (final String contentletId : contentletIdList) {
                final Contentlet contentlet;
                try {
                    contentlet = APILocator.getContentletAPI().findContentletByIdentifierAnyLanguageAnyVariant(contentletId);
                    if (null == contentlet) {

                        throw new BadRequestException("The contentlet: " + contentletId + " does not exists!");
                    }

                    if (contentlet.getBaseType().get().equals(BaseContentType.CONTENT) && !contentTypeSet.contains(contentlet.getContentType().variable())) {

                        throw new BadRequestException("The content type: " + contentlet.getContentType().variable() + " is not valid for the container");
                    }
                } catch (DotDataException e) {

                    throw new BadRequestException(e, e.getMessage());
                }
            }
        }
    }

    private Set<String> getContainerContentTypes (final String containerId) {

        try {
            final Container container = APILocator.getContainerAPI().findContainer(containerId,APILocator.systemUser(),false,false)
                    .orElseThrow(() -> new DoesNotExistException("Container with ID :" + containerId + " not found"));
            final List<ContentType> contentTypes = APILocator.getContainerAPI().getContentTypesInContainer(container);
            return null != contentTypes? contentTypes.stream().map(ContentType::variable).collect(Collectors.toSet()) : Collections.emptySet();
        } catch (DotDataException | DotSecurityException e) {

            throw new BadRequestException(e, e.getMessage());
        }
    }

    /**
     * If a container is being sent dupe, the entries will be reduced to one and the non-repeated contentlets will be combined.
     * @param containerEntries List
     * @return List
     */
    private List<PageContainerForm.ContainerEntry> reduce(final List<PageContainerForm.ContainerEntry> containerEntries) {
        // Helper class to hold both contentIds and styleProperties during reduction
        class ContainerData {
            final Set<String> contentIds = new LinkedHashSet<>();
            final Map<String, Map<String, Object>> stylePropertiesMap = new HashMap<>();
        }

        final Map<MultiKey, ContainerData> containerEntryMap = new HashMap<>();

        for (final PageContainerForm.ContainerEntry containerEntry : containerEntries) {
            // containerEntryMap key: personaTag + containerId + containerUUID
            final MultiKey key = new MultiKey(containerEntry.getPersonaTag(),
                    containerEntry.getContainerId(), containerEntry.getContainerUUID());

            final ContainerData data = containerEntryMap.computeIfAbsent(key, k -> new ContainerData());

            data.contentIds.addAll(containerEntry.getContentIds());

            // Merge styles. Duplicated keys overwrite previous ones (last one wins)
            final Map<String, Map<String, Object>> incomingStyles = Optional.of(
                    containerEntry.getStylePropertiesMap()).orElse(Collections.emptyMap());

            data.stylePropertiesMap.putAll(incomingStyles);
        }

        return containerEntryMap.entrySet().stream()
                .map(entry -> new PageContainerForm.ContainerEntry(
                        (String) entry.getKey().getKeys()[0],
                        (String) entry.getKey().getKeys()[1],
                        (String) entry.getKey().getKeys()[2],
                        new ArrayList<>(entry.getValue().contentIds),
                        entry.getValue().stylePropertiesMap))
                .collect(Collectors.toList());
    }
    /**
     *
     * @param request
     * @param response
     * @param uri
     * @param modeStr
     * @return
     */
    @NoCache
    @GET
    @Produces({"application/html", "application/javascript"})
    @Path("/renderHTML/{uri: .*}")
    public Response renderHTMLOnly(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam("uri") final String uri,
            @QueryParam("mode") @DefaultValue("LIVE_ADMIN") final String modeStr)
            throws DotDataException, DotSecurityException {

        Logger.debug(this, String.format("Rendering page: uri -> %s mode-> %s", uri, modeStr));

        // Force authentication
        final InitDataObject auth = webResource.init(request,  response,true);
        final User user = auth.getUser();
        Response res = null;

        final PageMode mode = PageMode.get(modeStr);
        PageMode.setPageMode(request, mode);
        try {

            final String html = this.htmlPageAssetRenderedAPI.getPageHtml(
                    PageContextBuilder.builder()
                            .setUser(user)
                            .setPageUri(uri)
                            .setPageMode(mode)
                            .build(),
                    request,
                    response
            );
            final Response.ResponseBuilder responseBuilder = Response.ok(html);

            res = responseBuilder.build();
        } catch (HTMLPageAssetNotFoundException e) {
            final String messageFormat =
                    "HTMLPageAssetNotFoundException on PageResource.renderHTMLOnly, parameters: request -> %s, uri -> %s mode -> %s: ";
            final String errorMsg = String.format(messageFormat, request, uri, mode);
            Logger.error(this, errorMsg, e);
            res = ExceptionMapperUtil.createResponse(e, Response.Status.NOT_FOUND);
        }

        return res;
    }

    /**
     * Return all the page that contains the path parameter
     *
     * @param request
     * @param path path to filter, if it start with '//' then the path contains the site name
     * @param liveQueryParam if it is true then return page only with live version, if it is false return both live and working version
     * @param onlyLiveSites if it is true then filter page only from live sites
     * @return
     * @throws DotDataException throw if a DotDataException occur
     * @throws DotSecurityException throw if the user don't have the right permissions
     */
    @NoCache
    @GET
    @Produces({"application/html", "application/javascript"})
    @Path("search")
    public Response searchPage(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @QueryParam("path") final String path,
            @QueryParam("live") final Boolean liveQueryParam,
            @QueryParam("onlyLiveSites") final boolean onlyLiveSites)
            throws DotDataException, DotSecurityException {

        final InitDataObject initData = webResource.init(null,  request, response,
                true, null);
        final User user = initData.getUser();

        final boolean live = (liveQueryParam == null) ? PageMode.get(request).showLive : liveQueryParam;

        final String esQuery = getPageByPathESQuery(path);

        final ESSearchResults esresult = esapi.esSearch(esQuery, live, user, live);
        final Set<Map<String, Object>> contentletMaps = applyFilters(onlyLiveSites, esresult)
                .stream()
                .map(contentlet -> {
                    try {
                        return ContentletUtil.getContentPrintableMap(user, contentlet);
                    } catch (DotDataException | IOException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        return Response.ok(new ResponseEntityView(contentletMaps)).build();
    }

    /**
     * Returns the page render version for live and preview working version
     * In addition returns a boolean flag "diff" if the live and preview are different or not (this is just a simple equals)
     * @param request
     * @param response
     * @param pageId
     * @param languageId
     * @return Response
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @GET
    @Path("/{pageId}/render/versions")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response getHtmlVersionsPage (@Context final HttpServletRequest  request,
                                   @Context final HttpServletResponse response,
                                   @PathParam("pageId")  final String  pageId,
                                   @QueryParam("langId") final String languageId) throws DotSecurityException, DotDataException, ExecutionException, InterruptedException {

        final User user = this.webResource.init(request, response, true).getUser();
        final long finalLanguageId = ConversionUtils.toLong(languageId, ()->APILocator.getLanguageAPI().getDefaultLanguage().getId());

        final PageLivePreviewVersionBean pageLivePreviewVersionBean =
                this.htmlPageAssetRenderedAPI.getPageRenderedLivePreviewVersion(pageId, user, finalLanguageId, request, response);

        return Response.ok(new ResponseEntityView(pageLivePreviewVersionBean)).build();
    }

    /**
     * Returns the tree associated to the page
     * @param request
     * @param response
     * @param pageId
     * @return Response, pair with
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @GET
    @Path("/{pageId}/content/tree")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ResponseEntityView<List<MulitreeView>> getContentTree (@Context final HttpServletRequest  request,
                                                   @Context final HttpServletResponse response,
                                                   @PathParam("pageId") final String  pageId) throws SystemException, PortalException, DotDataException, DotSecurityException {

        final User user = this.webResource.init(request, response, true).getUser();

        Logger.debug(this, ()-> "Getting multitree per page: " + pageId);

        final List<MultiTree> multiTrees = APILocator.getMultiTreeAPI().getMultiTrees(pageId);

        final List<MulitreeView> mulitreeViews = null != multiTrees? multiTrees.stream().map(multiTree ->
                new MulitreeView(multiTree.getHtmlPage(), multiTree.getContainer(),
                        multiTree.getContentlet(), multiTree.getRelationType(), multiTree.getTreeOrder(),
                        multiTree.getPersonalization(), multiTree.getVariantId())).collect(Collectors.toList()):
                Collections.emptyList();

        return new ResponseEntityView<>(mulitreeViews);
    } // getPersonalizedPersonasOnPage

    /**
     * Returns the list of personas with a flag that determine if the persona has been customized on a page or not.
     * { persona:Persona, personalized:boolean, pageId:String  }
     * @param request
     * @param response
     * @param pageId
     * @return Response, pair with
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @GET
    @Path("/{pageId}/personas")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response getPersonalizedPersonasOnPage (@Context final HttpServletRequest  request,
            @Context final HttpServletResponse response,
            @QueryParam(PaginationUtil.FILTER)   final String filter,
            @QueryParam(PaginationUtil.PAGE)     final int page,
            @QueryParam(PaginationUtil.PER_PAGE) final int perPage,
            @DefaultValue("title") @QueryParam(PaginationUtil.ORDER_BY) final String orderbyParam,
            @DefaultValue("ASC") @QueryParam(PaginationUtil.DIRECTION)  final String direction,
            @QueryParam("hostId") final String  hostId,
            @PathParam("pageId")  final String  pageId,
            @QueryParam("respectFrontEndRoles") Boolean respectFrontEndRolesParams) throws SystemException, PortalException, DotDataException, DotSecurityException {

        final User user = this.webResource.init(request, response, true).getUser();
        final boolean respectFrontEndRoles = respectFrontEndRolesParams != null ? respectFrontEndRolesParams : PageMode.get(request).respectAnonPerms;

        Logger.debug(this, ()-> "Getting page personas per page: " + pageId);

        final Map<String, Object> extraParams =
                ImmutableMap.<String, Object>builder()
                        .put(PersonalizationPersonaPageViewPaginator.PAGE_ID, pageId)
                        .put("hostId", UtilMethods.isSet(hostId)?hostId: WebAPILocator.getHostWebAPI().getCurrentHost(request).getIdentifier())
                        .put("respectFrontEndRoles",respectFrontEndRoles).build();


        final PaginationUtil paginationUtil = new PaginationUtil(new PersonalizationPersonaPageViewPaginator());

        return paginationUtil.getPage(request, user, filter, page, perPage, orderbyParam,
                OrderDirection.valueOf(direction), extraParams);
    } // getPersonalizedPersonasOnPage

    private String getPageByPathESQuery(final String pathParam) {
        String hostFilter = "";
        String path;

        if (pathParam.startsWith("//")) {
            final String[] pathSplit = pathParam.split("/");
            final String hostName = pathSplit[2];
            hostFilter = String.format("+conhostName:%s", hostName);
            path = String.join("/", Arrays.copyOfRange(pathSplit, 3, pathSplit.length));
        } else {
            path = pathParam;
        }

        path = path.replace("/", "\\\\/");

        return String.format("{"
                + "query: {"
                + "query_string: {"
                + "query: \"+basetype:5 +path:*%s* %s languageid:1^10\""
                + "}"
                + "}"
                + "}", path, hostFilter);
    }

    private Collection<Contentlet> applyFilters(
            final boolean workingSite,
            final ESSearchResults esresult) throws DotDataException {

        final Collection<Contentlet> contentlets = this.removeMultiLangVersion(esresult);
        return workingSite ? filterByWorkingSite(contentlets) : contentlets;
    }

    private Collection<Contentlet> filterByWorkingSite(final Collection<Contentlet> contentlets)
            throws DotDataException {

        final User systemUser = APILocator.getUserAPI().getSystemUser();

        final Map<String, Host> hosts = contentlets.stream()
                .map(Contentlet::getHost)
                .distinct()
                .map(hostId -> {
                    try {
                        return APILocator.getHostAPI().find(hostId, systemUser, false);
                    } catch (DotDataException | DotSecurityException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Host::getIdentifier, host -> host));

        return contentlets.stream()
                .filter(contentlet -> {
                    try {
                        return hosts.get(contentlet.getHost()).isLive();
                    } catch (DotDataException | DotSecurityException e) {
                        return false;
                    }
                })
                .collect(Collectors.toSet());
    }

    private Collection<Contentlet> removeMultiLangVersion(final Collection<Contentlet> contentlets) {
        final Map<String, Contentlet> result = new HashMap<>();

        for (final Contentlet contentlet : contentlets) {
            final String contenletId = contentlet.getIdentifier();

            if (!result.containsKey(contenletId)) {
                result.put(contenletId, contentlet);
            }
        }

        return result.values();
    }

    /**
     * Checks whether the {@code rendered} attribute in the JSON data must be posted to the EMA service or not.
     * <p>
     * When dotCMS executes a POST to the EMA Service, it is sending the {@code page.rendered} property as part of the
     * JSON payload. This is not required, and dotCMS should not do it. However, if the user needs it anyway, it can be
     * re-added to the response via UI in the {@code Configuration Data} JSON field in the EMA APP configuration
     * portlet.
     * </p>
     *
     * @param request The current instance of the {@link HttpServletRequest} object.
     * @param uri     The URI to the HTML Page that will be sent to the EMA Service.
     * @return If the {@code rendered} attribute must be added to the JSON data, return {@code true}. Otherwise, return {@code false}.
     */
    private boolean includeRenderedAttrFromEMA(final HttpServletRequest request, final String uri) {
        Host currentSite = null;
        try {
            currentSite = WebAPILocator.getHostWebAPI().getCurrentHost(request);
            final Optional<EMAConfigStrategy> configStrategy = new EMAConfigStrategyResolver().get(currentSite);
            final Optional<EMAConfigurations> emaConfig = configStrategy.flatMap(EMAConfigStrategy::resolveConfig);
            final Optional<EMAConfigurationEntry> config = emaConfig.isPresent()
                                                                   ? emaConfig.get().byUrl(uri)
                                                                   : Optional.empty();
            return config.isPresent() && config.get().isIncludeRendered();
        } catch (final DotDataException | DotSecurityException | SystemException | PortalException e) {
            final String siteName = null != currentSite ? currentSite.getHostname() : "N/A";
            Logger.debug(this, String.format("An error occurred when checking the 'rendered' attribute in site '%s' " +
                                                     "for URI '%s': %s", siteName, uri, e.getMessage()));
        }
        return false;
    }

    /**
     * Copy the contentlet sent over the CopyContentletForm
     * The contentlet should be part of the multitree on the page sent in the form, also the content should exists to be copied.
     * @param request {@link HttpServletRequest}
     * @param response {@link HttpServletResponse}
     * @param copyContentletForm {@link CopyContentletForm}
     * @return Contentlet Map
     */
    @PUT
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/copyContent")
    public final  ResponseEntityView<Map<String, Object>> copyContent(
                                      @Context final HttpServletRequest request,
                                      @Context final HttpServletResponse response,
                                      final CopyContentletForm copyContentletForm)
            throws DotSecurityException, DotDataException {

        Logger.debug(this, ()-> "Copying the contentlet: " + copyContentletForm);

        if (copyContentletForm == null) {

            throw new BadRequestException("Form is required");
        }

        final InitDataObject initData = webResource.init(request, response,true);
        final PageMode pageMode       = PageMode.get(request);
        final User user               = initData.getUser();
        final Language language       = WebAPILocator.getLanguageWebAPI().getLanguage(request); // todo: not sure if this should be received on the form.
        final Contentlet copiedContentlet = this.pageResourceHelper.copyContentlet(copyContentletForm, user, pageMode, language);
        final Map<String, Object> entity = (Map<String, Object>)new DotTransformerBuilder().defaultOptions().content(copiedContentlet).build()
                .toMaps().stream().findFirst().orElse(Collections.emptyMap());
        return new ResponseEntityView<>(entity);
    }

    /**
     * Do a deep copy of a page:
     * 1. Creates a page copy
     * 2. For each contentlet on the multitree related on the page, creates a copy.
     * @param request {@link HttpServletRequest}
     * @param response {@link HttpServletResponse}
     * @param copyContentletForm {@link CopyContentletForm}
     * @return Contentlet Map
     */
    @PUT
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{pageId}/_deepcopy")
    public final  ResponseEntityView<Map<String, Object>> deepCopyPage(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam("pageId") final String pageId)
            throws DotSecurityException, DotDataException {

        Logger.debug(this, ()-> "Copying the page: " + pageId);

        final InitDataObject initData = webResource.init(request, response,true);
        final PageMode pageMode       = PageMode.get(request);
        final User user               = initData.getUser();
        final IHTMLPage  page         = this.pageResourceHelper.getPage(user, pageId, request);
        final Language language       = WebAPILocator.getLanguageWebAPI().getLanguage(request); // todo: not sure if this should be received on the form.

        if (null == page) {

            throw new DoesNotExistException("The page: " +  pageId + " does not exists.");
        }

        final Contentlet copiedContentlet = this.pageResourceHelper.copyPage(page, user, pageMode, language);

        return new ResponseEntityView<>(new DotTransformerBuilder().defaultOptions().content(copiedContentlet).build()
                .toMaps().stream().findFirst().orElse(Collections.emptyMap()));
    } // deepCopyPage.

    /**
     * Returns all content types associated to page (base type page + all content type with url map)
     *
     * @param originalRequest The {@link HttpServletRequest} object.
     * @param response The {@link HttpServletResponse} object.
     * @param page {@link Integer} number of pages
     * @param perPage @{@link Integer} how many pages
     * @param orderbyParam {@link String} order by (default title)
     * @param direction {@link String} ASC
     * @return All the content types that match
     */
    @NoCache
    @GET
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Path("/types")
    public Response getPageTypes(@Context final HttpServletRequest originalRequest,
                             @Context final HttpServletResponse response,
                             @DefaultValue("") @QueryParam(PaginationUtil.FILTER)   final String filter,
                             @QueryParam(PaginationUtil.PAGE)     final int page,
                             @QueryParam(PaginationUtil.PER_PAGE) final int perPage,
                             @DefaultValue("UPPER(name)") @QueryParam(PaginationUtil.ORDER_BY) final String orderbyParam,
                             @DefaultValue("ASC") @QueryParam(PaginationUtil.DIRECTION)  final String direction) throws DotSecurityException, DotDataException {

        final InitDataObject auth = webResource.init(originalRequest, response, true);
        final User user = auth.getUser();
        final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(user);

        Logger.debug(this, ()-> "Getting page types, page: " +page + ",per page: " + perPage +
                ",order by" + orderbyParam + ", direction: " + direction);


        final List<ContentType> pageTypes    = contentTypeAPI.findByBaseType(BaseContentType.HTMLPAGE, "mod_date", 100, 0);
        final List<String> typeVarNames = new ImmutableList.Builder<String>()
                .addAll(pageTypes.stream().map(ContentType::variable).collect(Collectors.toList()))
                .addAll(contentTypeAPI.findUrlMapped().stream().map(ContentType::variable).collect(Collectors.toList())).build();
        final Map<String, Object> extraParams = new HashMap<>();
        extraParams.put(ContentTypesPaginator.TYPES_PARAMETER_NAME, typeVarNames);
            final PaginationUtil paginationUtil =
                    new PaginationUtil(new ContentTypesPaginator(contentTypeAPI));
        return paginationUtil.getPage(originalRequest, user, filter, page, perPage, orderbyParam,
                    OrderDirection.valueOf(direction), extraParams);
    }

    /**
     * Returns true if the page exist and the current user has 'type' permission over it
     * Parameters:
     * - type: is optional, by default is READ
     * - path: page path
     *
     * @param originalRequest The {@link HttpServletRequest} object.
     * @param response The {@link HttpServletResponse} object.
     * @param type {@link String} type: READ by default @see {@link PermissionLevel}
     * @param path {@link String} page path
     * @return All the content types that match
     */
    @NoCache
    @POST
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Path("/_check-permission")
    public ResponseEntityBooleanView checkPagePermission(@Context final HttpServletRequest request,
                                                         @Context final HttpServletResponse response,
                                                         final PageCheckPermissionForm pageCheckPermissionForm) throws DotSecurityException, DotDataException {

        final User user = new WebResource.InitBuilder(webResource).requestAndResponse(request, response)
                .rejectWhenNoUser(true).requiredBackendUser(true).init().getUser();

        final PageMode mode = PageMode.get(request);
        Logger.debug(this, ()-> "Checking Page Permission type" + pageCheckPermissionForm.getType()
                +" for the page path: " + pageCheckPermissionForm.getPath() + ", host id: " + pageCheckPermissionForm.getHostId()
                + ", lang: " + pageCheckPermissionForm.getLanguageId());

        final long languageId  = -1 != pageCheckPermissionForm.getLanguageId()? pageCheckPermissionForm.getLanguageId():
                WebAPILocator.getLanguageWebAPI().getLanguage(request).getId();
        final Host currentHost = UtilMethods.isSet(pageCheckPermissionForm.getHostId())?
                WebAPILocator.getHostWebAPI().find(pageCheckPermissionForm.getHostId(), user, PageMode.get(request).respectAnonPerms):
                WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(request);

        final Optional<URLMapInfo> urlMapInfoOptional = APILocator.getURLMapAPI().processURLMap(
                UrlMapContextBuilder.builder()
                        .setHost(currentHost)
                        .setLanguageId(languageId)
                        .setMode(mode)
                        .setUri(pageCheckPermissionForm.getPath())
                        .setUser(user)
                        .setGraphQL(false)
                        .build());

        final Permissionable page = urlMapInfoOptional.isPresent()? urlMapInfoOptional.get().getContentlet():
                 APILocator.getHTMLPageAssetAPI().getPageByPath(
                    pageCheckPermissionForm.getPath(), currentHost, languageId, mode.showLive);

        if (null != page) {

            return new ResponseEntityBooleanView(APILocator.getPermissionAPI().
                    doesUserHavePermission(page, pageCheckPermissionForm.getType().getType(),
                            user, false));
        }

        throw new DoesNotExistException("The page: " + pageCheckPermissionForm.getPath() + " do not exist");
    } // checkPagePermission.

    /**
     * Returns true if the page exist and the current user has 'type' permission over it
     * Parameters:
     * - type: is optional, by default is READ
     * - path: page path
     *
     * @param originalRequest The {@link HttpServletRequest} object.
     * @param response The {@link HttpServletResponse} object.
     * @param type {@link String} type: READ by default @see {@link PermissionLevel}
     * @param path {@link String} page path
     * @return All the content types that match
     */
    @NoCache
    @POST
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Path("/actions")
    public ResponseEntityPageWorkflowActionsView findAvailableActions(@Context final HttpServletRequest request,
                                                         @Context final HttpServletResponse response,
                                                         final FindAvailableActionsForm findAvailableActionsForm) throws DotSecurityException, DotDataException {

        final User user = new WebResource.InitBuilder(webResource).requestAndResponse(request, response)
                .rejectWhenNoUser(true).requiredBackendUser(true).init().getUser();

        final PageMode mode = PageMode.get(request);
        Logger.debug(this, () -> String.format("Finding available actions for page path: '%s' / Site ID: '%s' / Lang ID: %s",
                findAvailableActionsForm.getPath(), findAvailableActionsForm.getHostId(), findAvailableActionsForm.getLanguageId()));

        final long languageId  = -1 != findAvailableActionsForm.getLanguageId()? findAvailableActionsForm.getLanguageId():
                WebAPILocator.getLanguageWebAPI().getLanguage(request).getId();
        final Host currentHost = UtilMethods.isSet(findAvailableActionsForm.getHostId())?
                WebAPILocator.getHostWebAPI().find(findAvailableActionsForm.getHostId(), user, PageMode.get(request).respectAnonPerms):
                WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(request);

        final Optional<URLMapInfo> urlMapInfoOptional = APILocator.getURLMapAPI().processURLMap(
                UrlMapContextBuilder.builder()
                        .setHost(currentHost)
                        .setLanguageId(languageId)
                        .setMode(mode)
                        .setUri(findAvailableActionsForm.getPath())
                        .setUser(user)
                        .setGraphQL(false)
                        .build());

        final Contentlet page = urlMapInfoOptional.isPresent()? urlMapInfoOptional.get().getContentlet():
                (Contentlet)APILocator.getHTMLPageAssetAPI().getPageByPath(
                        findAvailableActionsForm.getPath(), currentHost, languageId, mode.showLive);

        if  (null != page) {

            final List<WorkflowAction> actions = APILocator.getWorkflowAPI()
                    .findAvailableActions(page, user, findAvailableActionsForm.getRenderMode());

            return new ResponseEntityPageWorkflowActionsView(new PageWorkflowActionsView(
                    new DotTransformerBuilder().defaultOptions().content(page).build()
                            .toMaps().stream().findFirst().orElse(Collections.emptyMap()),

                    actions.stream().map(WorkflowResource::convertToWorkflowActionView).collect(Collectors.toList())
            ));
        }
        throw new DoesNotExistException(String.format("HTML Page path '%s' with language ID '%s' in Site " +
                        "'%s' does not exist", findAvailableActionsForm.getPath(),
                findAvailableActionsForm.getLanguageId(), findAvailableActionsForm.getHostId()));
    } // findAvailableActions.

    /**
     * Receives the Identifier of an HTML Page, returns all the available languages in dotCMS and,
     * for each of them, adds a flag indicating whether the page is available in that language or
     * not. This may be particularly useful when requiring the system to provide a specific action
     * when a page is NOT available in a given language. Here's an example of how you can use it:
     * <pre>
     *     GET <a href="http://localhost:8080/api/v1/page/${PAGE_ID}/languages">http://localhost:8080/api/v1/page/${PAGE_ID}/languages</a>
     * </pre>
     *
     * @param request  The current instance of the {@link HttpServletRequest}.
     * @param response The current instance of the {@link HttpServletResponse}.
     * @param pageId   The Identifier of the HTML Page whose available languages will be checked.
     *
     * @return A {@link Response} object containing the list of languages and the flag indicating
     * whether the page is available in such a language or not.
     *
     * @throws DotDataException An error occurred when interacting with the database.
     * @deprecated This method is deprecated and will be removed in future versions. Please use the
     * more generic REST Endpoint
     * {@link com.dotcms.rest.api.v1.content.ContentResource#getExistingLanguagesForContent(String, User)} instead.
     */
    @GET
    @Path("/{pageId}/languages")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Deprecated(since = "Nov 7th, 24", forRemoval = true)
    public Response checkPageLanguageVersions(@Context final HttpServletRequest request,
                                              @Context final HttpServletResponse response,
                                              @PathParam("pageId") final String pageId) throws DotDataException {
        final User user = new WebResource.InitBuilder(webResource).requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .requiredBackendUser(true)
                .init().getUser();
        Logger.debug(this, () -> String.format("Check the languages that page '%s' is available on", pageId));
        final List<ExistingLanguagesForPageView> languagesForPage =
                this.pageResourceHelper.getExistingLanguagesForPage(pageId, user);
        return Response.ok(new ResponseEntityView<>(languagesForPage)).build();
    }

} // E:O:F:PageResource
