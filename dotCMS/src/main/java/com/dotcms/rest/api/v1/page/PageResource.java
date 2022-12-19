package com.dotcms.rest.api.v1.page;

import com.dotcms.content.elasticsearch.business.ESSearchResults;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.ema.EMAWebInterceptor;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.v1.authentication.ResponseUtil;
import com.dotcms.rest.api.v1.personalization.PersonalizationPersonaPageViewPaginator;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotcms.security.apps.AppSecrets;
import com.dotcms.security.apps.AppsAPI;
import com.dotcms.util.ConversionUtils;
import com.dotcms.util.HttpRequestDataUtil;
import com.dotcms.util.PaginationUtil;
import com.dotcms.util.pagination.OrderDirection;
import com.dotcms.variant.VariantAPI;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionLevel;
import com.dotmarketing.business.web.WebAPILocator;
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
import com.dotmarketing.portlets.htmlpageasset.business.render.page.PageView;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.collections.keyvalue.MultiKey;
import org.glassfish.jersey.server.JSONP;

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
import java.io.IOException;
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
@Tag(name = "Page")
public class PageResource {

    private final PageResourceHelper pageResourceHelper;
    private final WebResource webResource;
    private final HTMLPageAssetRenderedAPI htmlPageAssetRenderedAPI;
    private final ContentletAPI esapi;

    /**
     * Creates an instance of this REST end-point.
     */
    public PageResource() {
        this(
                PageResourceHelper.getInstance(),
                new WebResource(),
                APILocator.getHTMLPageAssetRenderedAPI(),
                APILocator.getContentletAPI()
        );
    }

    @VisibleForTesting
    PageResource(
            final PageResourceHelper pageResourceHelper,
            final WebResource webResource,
            final HTMLPageAssetRenderedAPI htmlPageAssetRenderedAPI,
            final ContentletAPI esapi) {

        this.pageResourceHelper = pageResourceHelper;
        this.webResource = webResource;
        this.htmlPageAssetRenderedAPI = htmlPageAssetRenderedAPI;
        this.esapi = esapi;
    }

    /**
     * Returns the metadata in JSON format of the objects that make up an HTML Page in the system.
     *
     * <pre>
     * Format:
     * http://localhost:8080/api/v1/page/json/{page-url}
     * <br/>
     * Example:
     * http://localhost:8080/api/v1/page/json/about-us/locations/index
     * </pre>
     *
     * @param originalRequest The {@link HttpServletRequest} object.
     * @param response The {@link HttpServletResponse} object.
     * @param uri The path to the HTML Page whose information will be retrieved.
     * @param modeParam {@link PageMode}
     * @param personaId {@link com.dotmarketing.portlets.personas.model.Persona}'s identifier to render the page
     * @param languageId {@link com.dotmarketing.portlets.languagesmanager.model.Language}'s Id to render the page
     * @param deviceInode {@link }'s inode to render the page
     * @return All the objects on an associated HTML Page.
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
            @QueryParam("device_inode") final String deviceInode) throws DotSecurityException, DotDataException {

        Logger.debug(this, String.format("Rendering page: uri -> %s mode-> %s language -> persona -> %s device_inode -> %s live -> %b", uri,
                modeParam, languageId, personaId, deviceInode));

        final HttpServletRequest request = this.pageResourceHelper.decorateRequest (originalRequest);
        // Force authentication
        final InitDataObject auth = webResource.init(request, response, true);
        final User user = auth.getUser();
        Response res;

        final PageMode mode = modeParam != null ? PageMode.get(modeParam) :
                this.htmlPageAssetRenderedAPI.getDefaultEditPageMode(user, request, uri);
        PageMode.setPageMode(request, mode);

        try {

            if (deviceInode != null) {
                request.getSession().setAttribute(WebKeys.CURRENT_DEVICE, deviceInode);
            }

            final PageView pageRendered = this.htmlPageAssetRenderedAPI.getPageMetadata(
                    PageContextBuilder.builder()
                            .setUser(user)
                            .setPageUri(uri)
                            .setPageMode(mode)
                            .setParseJSON(true)
                            .build(),
                    request,
                    response
            );

            final Response.ResponseBuilder responseBuilder = Response.ok(new ResponseEntityView(pageRendered));


            final Host host = APILocator.getHostAPI().find(pageRendered.getPage().getHost(), user,
                    PageMode.get(request).respectAnonPerms);
            request.setAttribute(WebKeys.CURRENT_HOST, host);
            request.getSession().setAttribute(WebKeys.CURRENT_HOST, host);

            res = responseBuilder.build();
        } catch (HTMLPageAssetNotFoundException e) {
            final String errorMsg = String.format("HTMLPageAssetNotFoundException on PageResource.render, parameters:  %s, %s %s: ",
                    request, uri, modeParam);
            Logger.error(this, errorMsg, e);
            res = ExceptionMapperUtil.createResponse(e, Response.Status.NOT_FOUND);
        } catch (Exception e) {

            final String errorMsg = String.format("HTMLPageAssetNotFoundException on PageResource.render, parameters:  %s, %s %s: ",
                    request, uri, modeParam);
            Logger.error(this, errorMsg, e);
            res = ResponseUtil.mapExceptionResponse(e);
        }
        return res;
    }


    /**
     * Returns the metadata in JSON format of the objects that make up an HTML Page in the system including page's and
     * containers html code rendered
     *
     * <pre>
     * Format:
     * http://localhost:8080/api/v1/page/render/{page-url}
     * <br/>
     * Example:
     * http://localhost:8080/api/v1/page/render/about-us/locations/index
     * </pre>
     *
     * @param originalRequest The {@link HttpServletRequest} object.
     * @param response The {@link HttpServletResponse} object.
     * @param uri The path to the HTML Page whose information will be retrieved.
     * @param modeParam {@link PageMode}
     * @param personaId {@link com.dotmarketing.portlets.personas.model.Persona}'s identifier to render the page
     * @param languageId {@link com.dotmarketing.portlets.languagesmanager.model.Language}'s Id to render the page
     * @param deviceInode {@link java.lang.String}'s inode to render the page
     * @return
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
            @QueryParam("device_inode") final String deviceInode) throws DotSecurityException, DotDataException, SystemException, PortalException {
        if (HttpRequestDataUtil.getAttribute(originalRequest, EMAWebInterceptor.EMA_REQUEST_ATTR, Boolean.FALSE)) {
            if (!includeRenderedAttrFromEMA(originalRequest, uri)) {
                return loadJson(originalRequest, response, uri, modeParam, personaId, languageId, deviceInode);
            }
        }
        Logger.debug(this, ()->String.format(
                "Rendering page: uri -> %s mode-> %s language -> persona -> %s device_inode -> %s live -> %b",
                uri, modeParam, languageId, personaId, deviceInode));

        final HttpServletRequest request = this.pageResourceHelper.decorateRequest (originalRequest);
        // Force authentication
        final InitDataObject auth = webResource.init(request, response, true);
        final User user = auth.getUser();
        Response res;


        final PageMode mode = modeParam != null
                ? PageMode.get(modeParam)
                : this.htmlPageAssetRenderedAPI.getDefaultEditPageMode(user, request,uri);

        PageMode.setPageMode(request, mode);

        if (deviceInode != null) {
            request.getSession().setAttribute(WebKeys.CURRENT_DEVICE, deviceInode);
        }

        final HttpSession session = request.getSession(false);
        if(null != session){
            // Time Machine-Date affects the logic on the vtls that conform parts of the rendered pages.
            // so.. we better get rid of it.
            session.removeAttribute("tm_date");
        }

        final PageView pageRendered = this.htmlPageAssetRenderedAPI.getPageRendered(
                PageContextBuilder.builder()
                        .setUser(user)
                        .setPageUri(uri)
                        .setPageMode(mode)
                        .build(),
                request,
                response
        );

        final Host host = APILocator.getHostAPI().find(pageRendered.getPage().getHost(), user,
                PageMode.get(request).respectAnonPerms);
        request.setAttribute(WebKeys.CURRENT_HOST, host);
        request.getSession().setAttribute(WebKeys.CURRENT_HOST, host);

        res = Response.ok(new ResponseEntityView(pageRendered)).build();

        return res;
    }


    /**
     * Save a template and link it with a page, If the page already has a anonymous template linked then it is updated,
     * otherwise a new template is created and the old link template remains unchanged
     *
     * @see Template#isAnonymous()
     *
     * @param request The {@link HttpServletRequest} object.
     * @param pageId page's Id to link the template
     * @param form The {@link PageForm}
     * @return
     */
    @NoCache
    @POST
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Path("/{pageId}/layout")
    public Response saveLayout(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam("pageId") final String pageId,
            final PageForm form) throws DotSecurityException {

        Logger.debug(this, String.format("Saving layout: pageId -> %s layout-> %s", pageId,
                form != null ? form.getLayout() : null));

        if (form == null) {
            throw new BadRequestException("Layout is required");
        }

        final InitDataObject auth = webResource.init(request, response, true);
        final User user = auth.getUser();

        Response res;

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

            res = Response.ok(new ResponseEntityView(renderedPage)).build();

        } catch(DoesNotExistException e) {
            final String errorMsg = String.format("DoesNotExistException on PageResource.saveLayout, parameters:  %s, %s %s: ",
                    request, pageId, form);
            Logger.error(this, errorMsg, e);
            res = ExceptionMapperUtil.createResponse("", "Unable to find page with Identifier: " + pageId, Response.Status.NOT_FOUND);
        } catch (BadRequestException | DotDataException e) {
            final String errorMsg = String.format("%s on PageResource.saveLayout, parameters:  %s, %s %s: ",
                    e.getClass().getCanonicalName(), request, pageId, form);
            Logger.error(this, errorMsg, e);
            res = ExceptionMapperUtil.createResponse(e, Response.Status.BAD_REQUEST);
        }

        return res;
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
    @Path("/layout")
    public Response saveLayout(@Context final HttpServletRequest request, @Context final HttpServletResponse response, final PageForm form) throws DotDataException {

        final InitDataObject auth = webResource.init(request, response, true);
        final User user = auth.getUser();

        Response res = null;

        try {

            final Template templateSaved = this.pageResourceHelper.saveTemplate(user, form);
            res = Response.ok(new ResponseEntityView(templateSaved)).build();

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
        } catch (IOException e) {
            final String errorMsg = String.format("IOException on PageResource.saveLayout, parameters:  %s, %s: ",
                    request, form);
            Logger.error(this, errorMsg, e);
            res = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }

        return res;
    }

    /**
     * Updates all the contents in an HTML Page. This method is used to update changes when both adding or removing
     * Contentlets from Containers. It takes a JSON object -- serialized as a {@link PageContainerForm} object -- in
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
     *              ]
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
     *              ]
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

            pageResourceHelper.saveContent(pageId, this.reduce(pageContainerForm.getContainerEntries()), language, variantName);

            return Response.ok(new ResponseEntityView("ok")).build();
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
                    contentlet = APILocator.getContentletAPI().findContentletByIdentifierAnyLanguage(contentletId);
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
     * If a container is being sent dupe, the entries will be reduce to one and the non repeated contentlets will be combined.
     * @param containerEntries List
     * @return List
     */
    private List<PageContainerForm.ContainerEntry> reduce(final List<PageContainerForm.ContainerEntry> containerEntries) {
        final Map<MultiKey, Set<String>> containerEntryMap = new HashMap<>();

        for (final PageContainerForm.ContainerEntry containerEntry : containerEntries) {

            final Set<String> contentletIdList = containerEntryMap.computeIfAbsent(new MultiKey(containerEntry.getPersonaTag(),
                    containerEntry.getContainerId(), containerEntry.getContainerUUID()), k -> new LinkedHashSet<>());

            contentletIdList.addAll(containerEntry.getContentIds());
        }

        return containerEntryMap.entrySet().stream()
                .map(entry -> new PageContainerForm.ContainerEntry((String)entry.getKey().getKeys()[0],
                        (String)entry.getKey().getKeys()[1], (String)entry.getKey().getKeys()[2], new ArrayList<>(entry.getValue())))
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
     * re-added to the JSON data via UI in the {@code Include 'rendered' attribute?} box in the EMA APP configuration
     * portlet.
     * </p>
     *
     * @param request The current instance of the {@link HttpServletRequest} object.
     * @param uri     The URI to the HTML Page that will be sent to the EMA Service.
     * @return If the {@code rendered} attribute must be added to the JSON data, return {@code true}. Otherwise, return {@code false}.
     */
    private boolean includeRenderedAttrFromEMA(final HttpServletRequest request, final String uri) {
        final AppsAPI appsAPI = APILocator.getAppsAPI();
        boolean includeRenderedAttr = Boolean.FALSE;
        Host currentSite = null;
        try {
            currentSite = WebAPILocator.getHostWebAPI().getCurrentHost(request);
            final Optional<AppSecrets> secretsOpt = appsAPI.getSecrets(EMAWebInterceptor.EMA_APP_CONFIG_KEY, true, currentSite, APILocator.systemUser());
            if (secretsOpt.isPresent()) {
                AppSecrets secrets = secretsOpt.get();
                Optional<Boolean> renderedOpt = secrets.getSecrets().containsKey(EMAWebInterceptor.INCLUDE_RENDERED_VAR) ?
                        Optional.ofNullable(secrets.getSecrets().get(EMAWebInterceptor.INCLUDE_RENDERED_VAR).getBoolean()) : Optional.empty();
                if (renderedOpt.isPresent() && renderedOpt.get()) {
                    includeRenderedAttr = Boolean.TRUE;
                }
            }
        } catch (final DotDataException | DotSecurityException | SystemException | PortalException e) {
            final String siteName = null != currentSite ? currentSite.getHostname() : "N/A";
            Logger.debug(this, String.format("An error occurred when checking the 'rendered' attribute in site '%s' for URI '%s': %s", siteName, uri, e.getMessage()));
        }
        return includeRenderedAttr;
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
} // E:O:F:PageResource
