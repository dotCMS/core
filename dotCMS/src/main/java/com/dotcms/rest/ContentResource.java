package com.dotcms.rest;

import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.field.LegacyFieldTransformer;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.rendering.velocity.viewtools.content.util.ContentUtils;
import com.dotcms.repackage.org.apache.commons.httpclient.HttpStatus;
import com.dotcms.rest.api.v1.authentication.ResponseUtil;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotcms.util.xstream.XStreamHandler;
import com.dotcms.uuid.shorty.ShortyId;
import com.dotcms.uuid.shorty.ShortyIdAPI;
import com.dotcms.workflow.form.FireActionForm;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.RelationshipAPI;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.DotLockException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletDependencies;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.contentlet.model.IndexPolicyProvider;
import com.dotmarketing.portlets.contentlet.util.ContentletUtil;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI.SystemAction;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.FileUtil;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.UUIDUtil;
import com.dotmarketing.util.UtilHTML;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.media.multipart.BodyPart;
import org.glassfish.jersey.media.multipart.ContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
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
import javax.ws.rs.core.Response.Status;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.dotmarketing.util.NumberUtil.toInt;
import static com.dotmarketing.util.NumberUtil.toLong;

/**
 * This REST Endpoint provides access to Contentlet data, fields, and different actions that can be
 * performed on them. It's worth noting that several methods in this Endpoint may belong to legacy
 * code or should not be used in recent dotCMS versions. If you need to add further functionality,
 * make sure you add it to the {@link com.dotcms.rest.api.v1.content.ContentResource} class
 * instead, which represents the most recent versioned REST Endpoint.
 *
 * @author Daniel Silva
 * @since May 25th, 2012
 */
@Path("/content")
@Tag(name = "Content Delivery")
public class ContentResource {

    // set this only from an environmental variable so it cannot be overridden in our Config class
    private final boolean USE_XSTREAM_FOR_DESERIALIZATION = System.getenv("USE_XSTREAM_FOR_DESERIALIZATION")!=null && "true".equals(System.getenv("USE_XSTREAM_FOR_DESERIALIZATION"));

    private static final String RELATIONSHIP_KEY = "__##relationships##__";
    private static final String IP_ADDRESS = "ipAddress";
    private static final String HOST_HEADER = "hostHeader";
    private static final String COOKIES = "cookies";
    private static final String USER_AGENT = "userAgent";
    private static final String REFERER = "referer";
    private static final String REQUEST_METHOD = "requestMethod";
    private static final String ACCEPT_LANGUAGE = "acceptLanguage";

    private final WebResource webResource = new WebResource();
    private final ContentHelper contentHelper = ContentHelper.getInstance();

    /**
     * Performs a content search. Parameters are received via POST and returns a JSON object with
     * the search info and contentlet results. This is an example call using CURL:
     * <pre>
     *     curl --location --request POST 'http://localhost:8080/api/content/_search' \
     *      --header 'Content-Type: application/json' \
     *      --data-raw '{
     *           	 "query": "+structurename:webpagecontent",
     *            	 "sort":"modDate",
     *            	 "limit":20,
     *            	 "offset":0,
     *            	 "userId":"dotcms.org.1"
     *      }'
     * </pre>
     *
     * @param request       {@link HttpServletRequest} object
     * @param response      {@link HttpServletResponse} object
     * @param rememberQuery Indicates if the specified Lucene Query must be stored in the current
     *                      session or not. This usually means that the request is coming from the
     *                      {@code Query Tool} portlet.
     * @param searchForm    {@link SearchForm}
     *
     * @return json array of objects. each object with inode and identifier
     */
    @POST
    @Path("/_search")
    @Produces(MediaType.APPLICATION_JSON)
    public Response search(@Context HttpServletRequest request,
                           @Context final HttpServletResponse response,
                           @QueryParam("rememberQuery") @DefaultValue("false") final boolean rememberQuery,
                           final SearchForm searchForm) throws DotSecurityException, DotDataException {

        final InitDataObject initData = this.webResource.init
                (null, request, response, false, null);

        final User   user    = initData.getUser();
        final String query   = searchForm.getQuery(); // default value = ""
        final String sort    = searchForm.getSort(); // default value = ""
        final int    limit   = searchForm.getLimit();  // default value = 20
        final int    offset  = searchForm.getOffset(); // default value = 0
        final String render  = searchForm.getRender();
        final int depth      = searchForm.getDepth(); // default value = -1
        final long language  = searchForm.getLanguageId();
        final PageMode pageMode         = PageMode.get(request);
        final String userToPullID       = searchForm.getUserId();
        final boolean allCategoriesInfo = searchForm.isAllCategoriesInfo();
        User   userForPull              = user;
        final String tmDate = (String) request.getSession().getAttribute("tm_date");

        if (depth > 3) {
            throw new IllegalArgumentException("Invalid depth: " + depth);
        }

        Logger.debug(this, ()-> "Searching contentlets by: " + searchForm);

        // If the user is an admin, they can send a user to filter the search
        if (null != user && user.isAdmin() && UtilMethods.isSet(userToPullID)) {
            userForPull = APILocator.getUserAPI().loadUserById(userToPullID, APILocator.systemUser(),true);
        }

            if (rememberQuery) {
                request.getSession().setAttribute(WebKeys.EXECUTED_LUCENE_QUERY, query);
            }
        String realQuery = query.contains("variant:") ? query : query + " +variant:default";
        realQuery = processQuery(realQuery);
        final SearchView searchView = this.contentHelper.pullContent(request, response, realQuery,
                userForPull, pageMode, offset, limit, sort, tmDate, render, user, depth,
                language, allCategoriesInfo);
        return Response.ok(new ResponseEntityView<>(searchView)).build();
    }

    /**
     * performs a call to APILocator.getContentletAPI().searchIndex() with the specified parameters.
     * Example call using curl: curl -XGET http://localhost:8080/api/content/indexsearch/+structurename:webpagecontent/sortby/modDate/limit/20/offset/0
     *
     * @param request request object
     * @param query lucene query
     * @param sortBy field to sortby
     * @param limit how many results return
     * @param offset how many results skip
     * @return json array of objects. each object with inode and identifier
     */
    @GET
    @Path("/indexsearch/{query}/sortby/{sortby}/limit/{limit}/offset/{offset}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response indexSearch(@Context HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam("query") String query,
            @PathParam("sortby") String sortBy, @PathParam("limit") int limit,
            @PathParam("offset") int offset,
            @PathParam("type") String type,
            @PathParam("callback") String callback)
            throws DotDataException, JSONException {

        InitDataObject initData = webResource.init(null, request, response, false, null);

        Map<String, String> paramsMap = new HashMap<>();
        paramsMap.put("type", type);
        paramsMap.put("callback", callback);
        //Creating an utility response object
        ResourceResponse responseResource = new ResourceResponse(paramsMap);

        try {
            List<ContentletSearch> searchIndex = APILocator.getContentletAPI()
                    .searchIndex(query, limit, offset, sortBy, initData.getUser(), true);
            JSONArray array = new JSONArray();
            for (ContentletSearch cs : searchIndex) {
                array.put(new JSONObject()
                        .put("inode", cs.getInode())
                        .put("identifier", cs.getIdentifier()));
            }

            return responseResource.response(array.toString());
        } catch (DotSecurityException e) {
            throw new ForbiddenException(e);
        }
    }

    /**
     * Performs a call to APILocator.getContentletAPI().indexCount()
     * using the specified parameters.
     * <p/>
     * Example call using curl:
     * curl -XGET http://localhost:8080/api/content/indexcount/+structurename:webpagecontent
     *
     * @param request request obejct
     * @param query lucene query to count on
     * @return a string with the count
     */
    @GET
    @Path("/indexcount/{query}")
    @Produces(MediaType.TEXT_PLAIN)
    public Response indexCount(@Context HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam("query") String query,
            @PathParam("type") String type,
            @PathParam("callback") String callback) throws DotDataException {

        InitDataObject initData = webResource.init(null, request, response, false, null);

        Map<String, String> paramsMap = new HashMap<>();
        paramsMap.put("type", type);
        paramsMap.put("callback", callback);
        //Creating an utility response object
        ResourceResponse responseResource = new ResourceResponse(paramsMap);

        try {
            return responseResource.response(Long.toString(
                    APILocator.getContentletAPI().indexCount(query, initData.getUser(), true)));
        } catch (DotSecurityException e) {
            throw new ForbiddenException(e);
        }
    }


    /**
     * @Deprecated This method is deprecated and will be removed in future versions. Use {@link com.dotcms.rest.api.v1.content.ContentResource#lockContent(HttpServletRequest, HttpServletResponse, String, String)}
     * @param request
     * @param response
     * @param params
     * @return
     * @throws DotDataException
     * @throws JSONException
     */
    @Deprecated
    @PUT
    @Path("/lock/{params:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response lockContent(@Context HttpServletRequest request,
            @Context HttpServletResponse response, @PathParam("params") String params)
            throws DotDataException, JSONException {

        InitDataObject initData = webResource.init(params, request, response, false, null);
        Map<String, String> paramsMap = initData.getParamsMap();
        String callback = paramsMap.get(RESTParams.CALLBACK.getValue());
        String language = paramsMap.get(RESTParams.LANGUAGE.getValue());

        String id = paramsMap.get(RESTParams.ID.getValue());

        String inode = paramsMap.get(RESTParams.INODE.getValue());

        ResourceResponse responseResource = new ResourceResponse(paramsMap);
        JSONObject jo = new JSONObject();
        User user = initData.getUser();

        long lang = APILocator.getLanguageAPI().getDefaultLanguage().getId();
        boolean live = (paramsMap.get(RESTParams.LIVE.getValue()) == null || !"false"
                .equals(paramsMap.get(RESTParams.LIVE.getValue())));

        if (paramsMap.get(RESTParams.LANGUAGE.getValue()) != null) {
            try {
                lang = Long.parseLong(language);
            } catch (Exception e) {
                Logger.warn(this.getClass(),
                        "Invald language passed in, defaulting to, well, the default");
            }
        }

        try {
            Contentlet contentlet = (inode != null)
                    ? APILocator.getContentletAPI().find(inode, user, live)
                    : APILocator.getContentletAPI()
                            .findContentletByIdentifier(id, live, lang, user, live);
            if (contentlet == null || contentlet.getIdentifier() == null) {
                jo.append("message", "contentlet not found");
                jo.append("return", 404);

                Response.ResponseBuilder responseBuilder = Response.status(HttpStatus.SC_NOT_FOUND);
                return responseBuilder.entity(jo).build();
            } else {
                if (!UtilMethods.isSet(inode)) {
                    inode = contentlet.getInode();
                }
                if (!UtilMethods.isSet(id)) {
                    id = contentlet.getIdentifier();
                }

                APILocator.getContentletAPI().lock(contentlet, user, live);

                if (UtilMethods.isSet(callback)) {
                    jo.put("callback", callback);
                }
                jo.put("inode", inode);
                jo.put("id", id);
                jo.put("message", "locked");
                jo.put("return", 200);
                //Creating an utility response object
            }

            return responseResource.response(jo.toString());
        } catch (DotSecurityException e) {
            throw new ForbiddenException(e);
        }
    }


    /**
     * @Deprecated This method is deprecated and will be removed in future versions. Use {@link com.dotcms.rest.api.v1.content.ContentResource#canLockContent(HttpServletRequest, HttpServletResponse, String, String)}
     * @param request
     * @param response
     * @param params
     * @return
     * @throws DotDataException
     * @throws JSONException
     */
    @Deprecated
    @PUT
    @Path("/canLock/{params:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response canLockContent(@Context HttpServletRequest request, @Context final HttpServletResponse response,
            @PathParam("params") String params)
            throws DotDataException, JSONException {

        InitDataObject initData = webResource.init(params, request, response, false, null);
        Map<String, String> paramsMap = initData.getParamsMap();
        String callback = paramsMap.get(RESTParams.CALLBACK.getValue());
        String language = paramsMap.get(RESTParams.LANGUAGE.getValue());

        String id = paramsMap.get(RESTParams.ID.getValue());

        String inode = paramsMap.get(RESTParams.INODE.getValue());

        ResourceResponse responseResource = new ResourceResponse(paramsMap);
        JSONObject jo = new JSONObject();
        User user = initData.getUser();

        long lang = APILocator.getLanguageAPI().getDefaultLanguage().getId();
        boolean live = (paramsMap.get(RESTParams.LIVE.getValue()) == null || !"false"
                .equals(paramsMap.get(RESTParams.LIVE.getValue())));

        if (paramsMap.get(RESTParams.LANGUAGE.getValue()) != null) {
            try {
                lang = Long.parseLong(language);
            } catch (Exception e) {
                Logger.warn(this.getClass(),
                        "Invald language passed in, defaulting to, well, the default");
            }
        }

        try {
            Contentlet contentlet = (inode != null)
                    ? APILocator.getContentletAPI().find(inode, user, live)
                    : APILocator.getContentletAPI()
                            .findContentletByIdentifier(id, live, lang, user, live);
            if (contentlet == null || contentlet.getIdentifier() == null) {
                jo.append("message", "contentlet not found");
                jo.append("return", 404);

                Response.ResponseBuilder responseBuilder = Response.status(HttpStatus.SC_NOT_FOUND);
                return responseBuilder.entity(jo).build();
            } else {
                if (!UtilMethods.isSet(inode)) {
                    inode = contentlet.getInode();
                }
                if (!UtilMethods.isSet(id)) {
                    id = contentlet.getIdentifier();
                }

                boolean canLock = false;
                try {
                    canLock = APILocator.getContentletAPI().canLock(contentlet, user);
                } catch (DotLockException e) {
                    canLock = false;
                }
                jo.put("canLock", canLock);
                jo.put("locked", contentlet.isLocked());

                Optional<ContentletVersionInfo> cvi = APILocator.getVersionableAPI()
                        .getContentletVersionInfo(id, contentlet.getLanguageId());

                if (contentlet.isLocked() && cvi.isPresent()) {
                    jo.put("lockedBy", cvi.get().getLockedBy());
                    jo.put("lockedOn", cvi.get().getLockedOn());
                    jo.put("lockedByName", APILocator.getUserAPI().loadUserById(cvi.get()
                            .getLockedBy()));


                }

                if (UtilMethods.isSet(callback)) {
                    jo.put("callback", callback);
                }
                jo.put("inode", inode);
                jo.put("id", id);
                jo.put("return", 200);
                //Creating an utility response object
            }

            return responseResource.response(jo.toString());
        } catch (DotSecurityException e) {
            throw new ForbiddenException(e);
        }
    }

    /**
     * @Deprecated This method is deprecated and will be removed in future versions. Use {@link com.dotcms.rest.api.v1.content.ContentResource#unlockContent(HttpServletRequest, HttpServletResponse, String, String)}
     * @param request
     * @param response
     * @param params
     * @return
     * @throws DotDataException
     * @throws JSONException
     */
    @Deprecated
    @PUT
    @Path("/unlock/{params:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response unlockContent(@Context HttpServletRequest request,
            @Context HttpServletResponse response, @PathParam("params") String params)
            throws DotDataException, JSONException {

        InitDataObject initData = webResource.init(params, request, response, false, null);

        Map<String, String> paramsMap = initData.getParamsMap();
        String callback = paramsMap.get(RESTParams.CALLBACK.getValue());
        String language = paramsMap.get(RESTParams.LANGUAGE.getValue());
        String id = paramsMap.get(RESTParams.ID.getValue());
        String inode = paramsMap.get(RESTParams.INODE.getValue());

        ResourceResponse responseResource = new ResourceResponse(paramsMap);
        JSONObject jo = new JSONObject();
        User user = initData.getUser();

        long lang = APILocator.getLanguageAPI().getDefaultLanguage().getId();
        boolean live = (paramsMap.get(RESTParams.LIVE.getValue()) == null || !"false"
                .equals(paramsMap.get(RESTParams.LIVE.getValue())));

        if (paramsMap.get(RESTParams.LANGUAGE.getValue()) != null) {
            try {
                lang = Long.parseLong(language);
            } catch (Exception e) {
                Logger.warn(this.getClass(),
                        "Invald language passed in, defaulting to, well, the default");
            }
        }

        try {
            Contentlet contentlet = (inode != null)
                    ? APILocator.getContentletAPI().find(inode, user, live)
                    : APILocator.getContentletAPI()
                            .findContentletByIdentifier(id, live, lang, user, live);
            if (contentlet == null || contentlet.getIdentifier() == null) {
                jo.append("message", "contentlet not found");
                jo.append("return", 404);


            } else {
                if (!UtilMethods.isSet(inode)) {
                    inode = contentlet.getInode();
                }
                if (!UtilMethods.isSet(id)) {
                    id = contentlet.getIdentifier();
                }

                APILocator.getContentletAPI().unlock(contentlet, user, live);

                if (UtilMethods.isSet(callback)) {
                    jo.put("callback", callback);
                }
                jo.put("inode", inode);
                jo.put("id", id);
                jo.put("message", "unlocked");
                jo.put("return", 200);
                //Creating an utility response object
            }

            return responseResource.response(jo.toString());
        } catch (DotSecurityException e) {
            throw new ForbiddenException(e);
        }
    }


    /**
     *
     * @param request
     * @param response
     * @param params
     * @return Contentlets that match the search criteria in json or xml format.
     *         When `depth` param is set to:
     *         0 --> The contentlet object will contain the identifiers of the related contentlets
     *         1 --> The contentlet object will contain the related contentlets
     *         2 --> The contentlet object will contain the related contentlets, which in turn will contain the identifiers of their related contentlets
     *         3 --> The contentlet object will contain the related contentlets, which in turn will contain a list of their related contentlets
     *         null --> Relationships will not be sent in the response
     */
    @GET
    @Path("/{params:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getContent(@Context HttpServletRequest request, @Context final HttpServletResponse response,
            @PathParam("params") String params) {
        final InitDataObject initData = this.webResource.init
                (params, request, response, false, null);
        // Creating a utility response object
        final ResourceResponse responseResource = new ResourceResponse(initData.getParamsMap());
        final Map<String, String> paramsMap = initData.getParamsMap();
        final User user = initData.getUser();
        //Try the render url parameter first, then the query parameter
        final String render = UtilMethods.isSet(paramsMap.get(RESTParams.RENDER.getValue())) ?
                paramsMap.get(RESTParams.RENDER.getValue()) :
                request.getParameter(RESTParams.RENDER.getValue());
        final String query = paramsMap.get(RESTParams.QUERY.getValue());
        final String related = paramsMap.get(RESTParams.RELATED.getValue());
        final String id = paramsMap.get(RESTParams.ID.getValue());
        final String limitStr = paramsMap.get(RESTParams.LIMIT.getValue());
        final String offsetStr = paramsMap.get(RESTParams.OFFSET.getValue());
        final String inode = paramsMap.get(RESTParams.INODE.getValue());
        final String respectFrontEndRolesKey = RESTParams.RESPECT_FRONT_END_ROLES.getValue().toLowerCase();
        final boolean respectFrontendRoles = !UtilMethods.isSet(paramsMap.get(respectFrontEndRolesKey)) || Boolean.parseBoolean(paramsMap.get(respectFrontEndRolesKey));
        final long language = toLong(paramsMap.get(RESTParams.LANGUAGE.getValue()),
                () -> APILocator.getLanguageAPI().getDefaultLanguage().getId());
        /* Limit and Offset Parameters Handling, if not passed, using default */
        final int limit = toInt(limitStr, () -> 10);
        final int offset = toInt(offsetStr, () -> 0);
        final boolean live = (paramsMap.get(RESTParams.LIVE.getValue()) == null ||
                !"false".equals(paramsMap.get(RESTParams.LIVE.getValue())));

        final String depthParam = paramsMap.get(RESTParams.DEPTH.getValue());
        final int depth = toInt(depthParam, () -> -1);

        request.setAttribute(RESTParams.DEPTH.toString(), String.valueOf(depth));

        if ((depth < 0 || depth > 3) && depthParam != null){
            final String errorMsg =
                    "Error searching content " + id + ". Reason: Invalid depth: " + depthParam;
            Logger.error(this, errorMsg);
            return ExceptionMapperUtil.createResponse(null, errorMsg);
        }

        final String allCategoriesInfoKey = RESTParams.ALL_CATEGORIES_INFO.getValue().toLowerCase();
        final boolean allCategoriesInfo =
                UtilMethods.isSet(paramsMap.get(allCategoriesInfoKey)) &&
                !("false".equals(paramsMap.get(allCategoriesInfoKey)));

        /* Fetching the content using a query if passed or an id */
        List<Contentlet> contentlets = new ArrayList<>();
        boolean idPassed = UtilMethods.isSet(id);
        boolean inodePassed  = UtilMethods.isSet(inode);
        boolean queryPassed = UtilMethods.isSet(query);
        String result = null;
        Optional<Status> status = Optional.empty();
        String type = paramsMap.get(RESTParams.TYPE.getValue());
        String orderBy = paramsMap.get(RESTParams.ORDERBY.getValue());
        final String tmDate = (request.getSession(false) !=null && request.getSession().getAttribute("tm_date") !=null)
                ? (String)  request.getSession().getAttribute("tm_date")
                : null;
        type = UtilMethods.isSet(type) ? type : "json";
        final String relatedOrder = UtilMethods.isSet(orderBy) ? orderBy: null;
        orderBy = UtilMethods.isSet(orderBy) ? orderBy : "modDate desc";

        try {
            if (idPassed) {
                final Contentlet contentlet = APILocator.getContentletAPI()
                        .findContentletByIdentifier(id, live, language, user, respectFrontendRoles);
                if (contentlet != null){
                    contentlets.add(contentlet);
                }
            } else if (inodePassed) {
                final Contentlet contentlet = APILocator.getContentletAPI()
                        .find(inode, user, respectFrontendRoles);
                if (contentlet != null){
                    contentlets.add(contentlet);
                }
            } else if (UtilMethods.isSet(related)){
                //Related identifier are expected this way: "ContentTypeVarName.FieldVarName:contentletIdentifier"
                //In case of multiple relationships, they must be sent as a comma separated list
                //i.e.: ContentTypeVarName1.FieldVarName1:contentletIdentifier1,ContentTypeVarName2.FieldVarName2:contentletIdentifier2
                int i = 0;
                for (final String relationshipValue : related.split(StringPool.COMMA)) {
                    if (i == 0) {
                        contentlets.addAll(getPullRelated(user, limit, offset, relatedOrder, tmDate,
                                processQuery(query), relationshipValue, language, live));
                    } else {
                        //filter the intersection in case multiple relationship
                        contentlets = contentlets.stream()
                                .filter(getPullRelated(user, limit, offset, relatedOrder, tmDate,
                                        processQuery(query), relationshipValue, language, live)::contains).collect(
                                        Collectors.toList());
                    }

                    i++;
                }
            } else if (queryPassed){
                contentlets = ContentUtils
                        .pull(processQuery(query), offset, limit, orderBy, user, tmDate);
            }
        } catch (final DotSecurityException e) {
            Logger.debug(this, "Permission error: " + e.getMessage(), e);
            return ExceptionMapperUtil.createResponse(new DotStateException("No Permissions"), Response.Status.FORBIDDEN);
        } catch (final Exception e) {
            if (idPassed) {
                Logger.warnAndDebug(this.getClass(), "Can't find Content with Identifier: " + id + " " + e.getMessage(), e);
            } else if (queryPassed || UtilMethods.isSet(related)) {
                Logger.warn(this, "Error searching Content : " + e.getMessage());
            } else if (inodePassed) {
                Logger.warnAndDebug(this.getClass(), "Can't find Content with Inode: " + inode, e);
            }
            status = Optional.of(Status.INTERNAL_SERVER_ERROR);
        }

        /* Converting the Contentlet list to XML or JSON */
        try {
            if ("xml".equals(type)) {
                result = getXML(contentlets, request, response, render, user, depth,
                        respectFrontendRoles, language, live, allCategoriesInfo);
            } else {
                result = getJSON(contentlets, request, response, render, user, depth,
                        respectFrontendRoles, language, live, allCategoriesInfo);
            }
        } catch (final Exception e) {
            Logger.warn(this, String.format("Error converting result to %s for request [ %s ]: " +
                    "%s", type, params, ExceptionUtil.getErrorMessage(e)));
        }
        return responseResource.response(result, null, status);
    }

    /**
     * Method used to obtain related content that matches a given criteria (lucene query and additional params)
     * @param user
     * @param limit
     * @param offset
     * @param orderBy
     * @param tmDate
     * @param luceneQuery
     * @param relationshipValue
     * @param language
     * @param live
     * @return
     * @throws DotSecurityException
     * @throws DotDataException
     */
    private List<Contentlet> getPullRelated(User user, int limit, int offset,
            String orderBy, String tmDate, String luceneQuery, String relationshipValue, long language, boolean live)
            throws DotSecurityException, DotDataException {
        final String contentTypeVar = relationshipValue.split(":")[0].split("\\.")[0];
        final String fieldVar = relationshipValue.split(":")[0].split("\\.")[1];
        final String relatedIdentifier = relationshipValue.split(":")[1];

        ContentType relatedContentType = APILocator.getContentTypeAPI(user).find(contentTypeVar);

        final Relationship relationship = APILocator.getRelationshipAPI()
                .getRelationshipFromField(relatedContentType.fieldMap().get(fieldVar),
                        user);

        return ContentUtils
                .pullRelated(relationship.getRelationTypeValue(), relatedIdentifier,
                        luceneQuery, relationship.hasParents(), limit, offset, orderBy, user,
                        tmDate, language, live);
    }

    /**
     * This methods receives a Lucene query.
     * It processes the query looking for special scenarios like structure fields (i.e: stInode, stName) and replace them with valid Content fields
     * @param luceneQuery
     * @return luceneQuery
     */
    private String processQuery(String luceneQuery) throws DotDataException, DotSecurityException {
        if (luceneQuery == null) {
            return null;
        }

        //Look for stName
        if (luceneQuery.contains(Contentlet.STRUCTURE_NAME_KEY + ":")) {
            //Parameter is in the FORMAT  stName:variableName
            //Replace to FORMAT  ContentType:variableName
            luceneQuery = luceneQuery.replaceAll(Contentlet.STRUCTURE_NAME_KEY + ":", "ContentType:");
        }

        //Look for stInode
        String stInodeKey = Contentlet.STRUCTURE_INODE_KEY + ":";
        if (luceneQuery.contains(stInodeKey)) {
            //Parameter is in the FORMAT  stInode:inode

            //Lucene parameters are separated by blankSpace
            int startIndex = luceneQuery.indexOf(stInodeKey) + stInodeKey.length();
            int endIndex = luceneQuery.indexOf(' ', startIndex);
            String inode = (endIndex < 0) ? luceneQuery.substring(startIndex) : luceneQuery.substring(startIndex, endIndex);

            ContentType type = APILocator.getContentTypeAPI(APILocator.systemUser()).find(inode);
            if (type != null && InodeUtils.isSet(type.inode())) {
                //Replace to FORMAT   ContentType:variableName
                luceneQuery = luceneQuery.replace(Contentlet.STRUCTURE_INODE_KEY + ":", "ContentType:");
                luceneQuery = luceneQuery.replace(inode, type.variable());
            }
        }

        return luceneQuery;
    }

    /**
     * Creates an xml (as string) from a list of contentlets
     * @param cons
     * @param request
     * @param response
     * @param render
     * @param user
     * @param depth
     * @param respectFrontendRoles
     * @param language
     * @param live
     * @param allCategoriesInfo
     * @return
     * @throws DotDataException
     * @throws IOException
     * @throws DotSecurityException
     */
    private String getXML(final List<Contentlet> cons, final HttpServletRequest request,
            final HttpServletResponse response, final String render, final User user,
            final int depth, final boolean respectFrontendRoles, long language, boolean live,
            final boolean allCategoriesInfo){

        final StringBuilder sb = new StringBuilder();
        final XStream xstream = XStreamHandler.newXStreamInstance();
        xstream.alias("content", Map.class);
        xstream.registerConverter(new MapEntryConverter());
        sb.append("<?xml version=\"1.0\" encoding='UTF-8'?>");
        sb.append("<contentlets>");

        cons.forEach(contentlet -> {
            try {
                //we need to add relationships
                if (depth != -1){
                    sb.append(xstream.toXML(
                            addRelationshipsToXML(request, response, render, user, depth, respectFrontendRoles, contentlet,
                                    getContentXML(contentlet, request, response, render, user, allCategoriesInfo), null, language, live,
                                    allCategoriesInfo)));
                } else{
                    sb.append(xstream.toXML(
                                    getContentXML(contentlet, request, response, render, user, allCategoriesInfo)));
                }
            } catch (Exception e) {
                Logger.error(this, "Error generating content xml: " + e.getMessage(), e);
            }
        });

        sb.append("</contentlets>");
        return sb.toString();
    }

    /**
     *
     * @param contentlet
     * @param request
     * @param response
     * @param render
     * @param user
     * @param allCategoriesInfo
     * @return
     * @throws DotDataException
     * @throws IOException
     * @throws DotSecurityException
     */
    private Map<String, Object> getContentXML(final Contentlet contentlet,
            final HttpServletRequest request,
            final HttpServletResponse response, final String render, final User user,
            final boolean allCategoriesInfo)
            throws DotDataException, IOException, DotSecurityException {

        final Map<String, Object> m = new HashMap<>();
        final ContentType type = contentlet.getContentType();

        final boolean doRender = (BaseContentType.WIDGET.equals(type.baseType()) && "true".equalsIgnoreCase(render));
        //Render code
        m.putAll(ContentletUtil.getContentPrintableMap(user, contentlet, allCategoriesInfo, doRender));
        if (BaseContentType.WIDGET.equals(type.baseType()) && Boolean.toString(true)
                .equalsIgnoreCase(render)) {
            m.put("parsedCode", WidgetResource.parseWidget(request, response, contentlet));
        }

        if (BaseContentType.HTMLPAGE.equals(type.baseType())) {
            m.put(HTMLPageAssetAPI.URL_FIELD, this.contentHelper.getUrl(contentlet));
        }

        final Set<String> jsonFields = this.contentHelper.getJSONFields(type);
        for (String key : m.keySet()) {
            if (jsonFields.contains(key)) {
                m.put(key, contentlet.getKeyValueProperty(key));
            }
        }

        m.put("__icon__", UtilHTML.getIconClass(contentlet));
        m.put("contentTypeIcon", type.icon());

        return m;
    }

    /**
     * Add relationships records to a contentlet xml
     * @param request
     * @param response
     * @param render
     * @param user
     * @param depth
     * @param contentlet
     * @param objectMap
     * @param addedRelationships
     * @param language
     * @param live
     * @param allCategoriesInfo
     * @return
     * @throws DotDataException
     * @throws JSONException
     * @throws IOException
     * @throws DotSecurityException
     */
    private Map<String, Object> addRelationshipsToXML(final HttpServletRequest request,
            final HttpServletResponse response,
            final String render, final User user, final int depth,
            final boolean respectFrontendRoles,
            final Contentlet contentlet, final Map<String, Object> objectMap,
            Set<Relationship> addedRelationships, long language, boolean live,
            final boolean allCategoriesInfo)
            throws DotDataException, IOException, DotSecurityException {

        Relationship relationship;
        final RelationshipAPI relationshipAPI = APILocator.getRelationshipAPI();

        //filter relationships fields
        final Map<String, com.dotcms.contenttype.model.field.Field> fields = contentlet.getContentType().fields()
                .stream().filter(field -> field instanceof RelationshipField).collect(
                        Collectors.toMap(field -> field.variable(), field -> field));

        final ContentletRelationships contentletRelationships = new ContentletRelationships(
                contentlet);

        if (addedRelationships == null) {
            addedRelationships = new HashSet<>();
        }

        for (com.dotcms.contenttype.model.field.Field field : fields.values()) {

            try{
                relationship = relationshipAPI.getRelationshipFromField(field, user);
            }catch(DotDataException | DotSecurityException e){
                Logger.warn("Error getting relationship for field " + field, e.getMessage(), e);
                continue;
            }

            if (addedRelationships.contains(relationship)) {
                continue;
            }
            if (!relationship.getParentStructureInode().equals(relationship.getChildStructureInode())) {
                addedRelationships.add(relationship);
            }

            final boolean isChildField = relationshipAPI.isChildField(relationship, field);

            ContentletRelationships.ContentletRelationshipRecords relationshipRecords = contentletRelationships.new ContentletRelationshipRecords(
                    relationship, isChildField);

            List records = addRelatedContentToXMLMap(request, response,
                    render, user, depth, respectFrontendRoles, contentlet,
                    addedRelationships, language, live, field, isChildField, allCategoriesInfo);

            objectMap.put(field.variable(),
                    relationshipRecords.doesAllowOnlyOne() && records.size() > 0 ? records.get(0)
                            : records);

            //For self-related fields, the other side of the relationship should be added if the other-side field exists
            if (relationshipAPI.sameParentAndChild(relationship)){
                com.dotcms.contenttype.model.field.Field otherSideField = null;
                if (relationship.getParentRelationName() != null
                        && relationship.getChildRelationName() != null) {
                    if (isChildField) {
                        if (fields.containsKey(relationship.getParentRelationName())) {
                            otherSideField = fields.get(relationship.getParentRelationName());
                        }
                    } else {
                        if (fields.containsKey(relationship.getChildRelationName())) {
                            otherSideField = fields.get(relationship.getChildRelationName());
                        }
                    }
                }
                if (otherSideField != null){

                    relationshipRecords = contentletRelationships.new ContentletRelationshipRecords(
                            relationship, !isChildField);

                    records = addRelatedContentToXMLMap(request, response,
                            render, user, depth, respectFrontendRoles, contentlet,
                            addedRelationships, language, live, otherSideField, !isChildField,
                            allCategoriesInfo);

                    objectMap.put(otherSideField.variable(),
                            relationshipRecords.doesAllowOnlyOne() && records.size() > 0 ? records.get(0)
                                    : records);
                }
            }
        }
        return objectMap;
    }

    /**
     *
     * @param request
     * @param response
     * @param render
     * @param user
     * @param depth
     * @param respectFrontendRoles
     * @param contentlet
     * @param addedRelationships
     * @param language
     * @param live
     * @param field
     * @param isParent
     * @param allCategoriesInfo
     * @throws DotDataException
     * @throws IOException
     * @throws DotSecurityException
     */
    private List addRelatedContentToXMLMap(final HttpServletRequest request, final HttpServletResponse response,
            final String render, final User user, final int depth, final boolean respectFrontendRoles,
            final Contentlet contentlet, final Set<Relationship> addedRelationships, final long language,
            final boolean live, final com.dotcms.contenttype.model.field.Field field,
            final boolean isParent, final boolean allCategoriesInfo)
            throws DotDataException, IOException, DotSecurityException {

        final List records = new ArrayList();

        for (Contentlet relatedContent : contentlet.getRelated(field.variable(), user, respectFrontendRoles, isParent, language, live)) {
            switch (depth) {
                //returns a list of identifiers
                case 0:
                    records.add(relatedContent.getIdentifier());
                    break;

                //returns a list of related content objects
                case 1:
                    records.add(
                            getContentXML(relatedContent, request, response, render, user, allCategoriesInfo));
                    break;

                //returns a list of related content identifiers for each of the related content
                case 2:
                    records.add(addRelationshipsToXML(request, response, render, user, 0,
                            respectFrontendRoles, relatedContent,
                            getContentXML(relatedContent, request, response, render, user, allCategoriesInfo),
                            new HashSet<>(addedRelationships), language, live, allCategoriesInfo));
                    break;

                //returns a list of hydrated related content for each of the related content
                case 3:
                    records.add(addRelationshipsToXML(request, response, render, user, 1,
                            respectFrontendRoles, relatedContent,
                            getContentXML(relatedContent, request, response, render, user, allCategoriesInfo),
                            new HashSet<>(addedRelationships), language, live, allCategoriesInfo));
                    break;
            }
        }

        return records;
    }


    private String getXMLContentIds(Contentlet con) {
        XStream xstream = XStreamHandler.newXStreamInstance();
        xstream.alias("content", Map.class);
        xstream.registerConverter(new MapEntryConverter());
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding='UTF-8'?>");
        sb.append("<contentlet>");
        Map<String, Object> m = new HashMap<>();
        m.put("inode", con.getInode());
        m.put("identifier", con.getIdentifier());
        sb.append(xstream.toXML(m));
        sb.append("</contentlet>");
        return sb.toString();
    }

    /**
     *
     * @param con
     * @return
     */
    private String getJSONContentIds(final Contentlet con){
        JSONObject json = new JSONObject();
        try {
            json.put("inode", con.getInode());
            json.put("identifier", con.getIdentifier());
        } catch (JSONException e) {
            Logger.warn(this.getClass(), "unable JSON contentlet " + con.getIdentifier());
            Logger.debug(this.getClass(), "unable to find contentlet", e);
        }
        return json.toString();
    }

    /**
     * Creates a json object (as string) of a list of contentlets
     * @param cons
     * @param request
     * @param response
     * @param render
     * @param user
     * @param depth
     * @param respectFrontendRoles
     * @param language
     * @param live
     * @param allCategoriesInfo
     * @return
     * @throws IOException
     * @throws DotDataException
     */
    private String getJSON(final List<Contentlet> cons, final HttpServletRequest request,
            final HttpServletResponse response, final String render, final User user,
            final int depth, final boolean respectFrontendRoles, final long language,
            final boolean live, final boolean allCategoriesInfo){
        final JSONObject json = this.contentHelper.getJSONObject(cons, request, response, render, user,
                depth, respectFrontendRoles, language, live, allCategoriesInfo);
        return json.toString();
    }

    public class MapEntryConverter implements Converter {

        public boolean canConvert(@SuppressWarnings("rawtypes") Class clazz) {
            return AbstractMap.class.isAssignableFrom(clazz);
        }

        public void marshal(final Object value, final HierarchicalStreamWriter writer,
                final MarshallingContext context) {
            @SuppressWarnings("unchecked")
            final Map<String, Object> map = (Map<String, Object>) value;
            for (Entry<String, Object> entry : map.entrySet()) {
                writer.startNode(entry.getKey().toString());
                if (entry.getValue() instanceof List){
                    final List itemsList = ((List)entry.getValue());
                    if (!((List)entry.getValue()).isEmpty()){
                        //list of more than one object
                        marshalInternalNode(writer, context, itemsList);
                    }
                } else if(entry.getValue() instanceof Map) {
                    //one object saved as a map
                    marshal(entry.getValue(), writer, context);
                }else{
                    //simple string
                    writer.setValue(entry.getValue() != null ? entry.getValue().toString() : "");
                }
                writer.endNode();
            }
        }

        /**
         * Build xml node for all elements in a list. Each element is saved as a map
         * @param writer
         * @param context
         * @param itemsList
         */
        private void marshalInternalNode(final HierarchicalStreamWriter writer,
                final MarshallingContext context,
                final List itemsList) {
            itemsList.forEach(item -> {
                writer.startNode("item");
                if (item instanceof Map){
                    marshal(item, writer, context);
                } else {
                    //simple string
                    writer.setValue(item!= null ? item.toString() : "");
                }

                writer.endNode();
            });
        }

        public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
            Map<String, String> map = new HashMap<>();

            while (reader.hasMoreChildren()) {
                reader.moveDown();
                map.put(reader.getNodeName(), reader.getValue());
                reader.moveUp();
            }
            return map;
        }



    }

    /**
     * This method has been deprecated in favor of the {@link com.dotcms.rest.api.v1.workflow.WorkflowResource#fireActionDefaultMultipart(HttpServletRequest, HttpServletResponse, String, String, long, SystemAction, FormDataMultiPart)}
     * @param request
     * @param response
     * @param multipart
     * @param params
     * @deprecated 
     * @see com.dotcms.rest.api.v1.workflow.WorkflowResource#fireActionDefaultMultipart(HttpServletRequest, HttpServletResponse, String, String, long, SystemAction, FormDataMultiPart)
     * @return
     * @throws URISyntaxException
     * @throws DotDataException
     */
    @Deprecated
    @PUT
    @Path("/{params:.*}")
    @Produces({MediaType.APPLICATION_JSON, "application/javascript", MediaType.TEXT_PLAIN})
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response multipartPUT(@Context HttpServletRequest request,
            @Context HttpServletResponse response,
            FormDataMultiPart multipart, @PathParam("params") String params)
            throws URISyntaxException, DotDataException {
        return multipartPUTandPOST(request, response, multipart, params, "PUT");
    }

    /**
     * This method has been deprecated in favor of the {@link com.dotcms.rest.api.v1.workflow.WorkflowResource#fireActionDefaultMultipart(HttpServletRequest, HttpServletResponse, String, String, long, SystemAction, FormDataMultiPart)}
     * @param request
     * @param response
     * @param multipart
     * @param params
     * @deprecated
     * @see com.dotcms.rest.api.v1.workflow.WorkflowResource#fireActionDefaultMultipart(HttpServletRequest, HttpServletResponse, String, String, long, SystemAction, FormDataMultiPart)
     * @return
     * @throws URISyntaxException
     * @throws DotDataException
     */
    @Deprecated
    @POST
    @Path("/{params:.*}")
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response multipartPOST(@Context HttpServletRequest request,
            @Context HttpServletResponse response,
            FormDataMultiPart multipart, @PathParam("params") String params)
            throws URISyntaxException, DotDataException {
        return multipartPUTandPOST(request, response, multipart, params, "POST");
    }

    private Response multipartPUTandPOST(final HttpServletRequest request,final HttpServletResponse response,
            final FormDataMultiPart multipart, final String params, final String method)
            throws URISyntaxException, DotDataException {

        final InitDataObject init = new WebResource.InitBuilder(request, response)
                .requiredAnonAccess(AnonymousAccess.WRITE)
                .params(params)
                .init();
        final Contentlet contentlet = new Contentlet();
        setRequestMetadata(contentlet, request);

        final Map<String, Object> map = new HashMap<>();
        final List<String> usedBinaryFields = new ArrayList<>();
        final List<String> binaryFields = new ArrayList<>();
        String binaryFieldsInput = null;

        for (final BodyPart part : multipart.getBodyParts()) {

            final ContentDisposition contentDisposition = part.getContentDisposition();
            final String unsanitizedName = contentDisposition != null && contentDisposition.getParameters().containsKey("name") ? contentDisposition.getParameters().get("name") : "";

            final String name = FileUtil.sanitizeFileName(unsanitizedName);
            if(!unsanitizedName.equals(name)) {
                SecurityLogger.logInfo(getClass(), "Invalid filename uploaded, possible RCE.  Supplied filename: '" + unsanitizedName + "'");
            }
            
            
            
            final MediaType mediaType = part.getMediaType();

            if (mediaType.equals(MediaType.APPLICATION_JSON_TYPE) || name.equals("json")) {
                try {
                    processJSON(contentlet, part.getEntityAs(InputStream.class));
                    try {
                        binaryFieldsInput = WebResource.processJSON(part.getEntityAs(InputStream.class)).get("binary_fields").toString();
                    } catch (NullPointerException npe) {
                      //empty on purpose
                    }
                    if (UtilMethods.isSet(binaryFieldsInput)) {
                        if (!binaryFieldsInput.contains(",")) {
                            binaryFields.add(binaryFieldsInput);
                        } else {
                            for (String binaryFieldSplit : binaryFieldsInput.split(",")) {
                                binaryFields.add(binaryFieldSplit.trim());
                            }
                        }
                    }
                } catch (JSONException e) {

                    Logger.error(this.getClass(), "Error processing JSON for Stream", e);

                    Response.ResponseBuilder responseBuilder = Response.status(HttpStatus.SC_BAD_REQUEST);
                    responseBuilder.entity(e.getMessage());
                    return responseBuilder.build();
                } catch (IOException e) {

                    Logger.error(this.getClass(), "Error processing Stream", e);

                    Response.ResponseBuilder responseBuilder = Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR);
                    responseBuilder.entity(e.getMessage());
                    return responseBuilder.build();
                } catch (DotSecurityException e) {
                    throw new ForbiddenException(e);
                }
            } else if (mediaType.equals(MediaType.APPLICATION_XML_TYPE) || name.equals("xml")) {
                try {

                    processXML(contentlet, part.getEntityAs(InputStream.class));
                } catch (Exception e) {
                    if (e instanceof DotSecurityException) {
                        SecurityLogger.logInfo(this.getClass(),
                                "Invalid XML POSTED to ContentTypeResource from " + request
                                        .getRemoteAddr());
                    }
                    Logger.error(this.getClass(), "Error processing Stream", e);

                    Response.ResponseBuilder responseBuilder = Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR);
                    responseBuilder.entity(e.getMessage());
                    return responseBuilder.build();
                }
            } else if (mediaType.equals(MediaType.APPLICATION_FORM_URLENCODED_TYPE) || name.equals("urlencoded")) {
                try {
                    processForm(contentlet, part.getEntityAs(InputStream.class));
                } catch (Exception e) {
                    Logger.error(this.getClass(), "Error processing Stream", e);

                    Response.ResponseBuilder responseBuilder = Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR);
                    responseBuilder.entity(e.getMessage());
                    return responseBuilder.build();
                }
            } else if(mediaType.equals(MediaType.TEXT_PLAIN_TYPE)) {
                try {
                    map.put(name, part.getEntityAs(String.class));
                    processMap( contentlet, map );

                    if(null != contentDisposition && UtilMethods.isSet(contentDisposition.getFileName())){
                        processFile(contentlet, usedBinaryFields, binaryFields, part);
                    }

                } catch (Exception e) {
                    Logger.error( this.getClass(), "Error processing Plain Tex", e );

                    Response.ResponseBuilder responseBuilder = Response.status( HttpStatus.SC_INTERNAL_SERVER_ERROR );
                    responseBuilder.entity( e.getMessage() );
                    return responseBuilder.build();
                }
            } else if (null != contentDisposition) {
                try {
                    this.processFile(contentlet, usedBinaryFields, binaryFields, part);
                } catch (IOException e) {

                    Logger.error(this.getClass(), "Error processing Stream", e);

                    Response.ResponseBuilder responseBuilder = Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR);
                    responseBuilder.entity(e.getMessage());
                    return responseBuilder.build();
                } catch (DotSecurityException e) {
                    throw new ForbiddenException(e);
                }
            }
        }

        return saveContent(contentlet, init);
    }

    private void processFile(final Contentlet contentlet,
                             final List<String> usedBinaryFields,
                             final List<String> binaryFields,
                             final BodyPart part) throws IOException, DotSecurityException, DotDataException {

        try(final InputStream input = part.getEntityAs(InputStream.class)){
            final String badFileName = part.getContentDisposition().getFileName();
            final String filename = FileUtil.sanitizeFileName(badFileName);
            if(!badFileName.equals(filename)) {
                SecurityLogger.logInfo(getClass(), "Invalid filename uploaded, possible exploit attempt: " + badFileName);
                if(Config.getBooleanProperty("THROW_ON_BAD_FILENAMES", true)) {
                    throw new IllegalArgumentException("Invalid filename uploaded : " + badFileName);
                }
            }
            
            final File tmpFolder = new File(APILocator.getFileAssetAPI().getRealAssetPathTmpBinary() + UUIDUtil.uuid());
    
            if(!tmpFolder.mkdirs()) {
                throw new IOException("Unable to create temp folder to save binaries");
            }
    
            final File tempFile = new File(
                    tmpFolder.getAbsolutePath() + File.separator + filename);
            Files.deleteIfExists(tempFile.toPath());
    
            FileUtils.copyInputStreamToFile(input, tempFile);
            final List<Field> fields = new LegacyFieldTransformer(
                    APILocator.getContentTypeAPI(APILocator.systemUser()).
                            find(contentlet.getContentType().inode()).fields())
                    .asOldFieldList();
            for (final Field field : fields) {
                // filling binaries in order. as they come / as field order says
                final String fieldName = field.getFieldContentlet();
                if (fieldName.startsWith("binary") && !usedBinaryFields.contains(fieldName)) {
    
                    String fieldVarName = field.getVelocityVarName();
                    if (binaryFields.size() > 0) {
                        fieldVarName = binaryFields.remove(0);
                    }
                    contentlet.setBinary(fieldVarName, tempFile);
                    usedBinaryFields.add(fieldName);
                    break;
                }
            }
        }
    }

    /**
     * This method has been deprecated in favor of {@link com.dotcms.rest.api.v1.workflow.WorkflowResource#fireActionDefault(HttpServletRequest, HttpServletResponse, String, String, long, SystemAction, FireActionForm)}
     * @param request
     * @param response
     * @param params
     * @deprecated
     * @see  {@link com.dotcms.rest.api.v1.workflow.WorkflowResource#fireActionDefault(HttpServletRequest, HttpServletResponse, String, String, long, SystemAction, FireActionForm)}
     * @return
     * @throws URISyntaxException
     */
    @PUT
    @Path("/{params:.*}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED,
            MediaType.APPLICATION_XML})
    @Deprecated
    public Response singlePUT(@Context HttpServletRequest request,
            @Context HttpServletResponse response, @PathParam("params") String params)
            throws URISyntaxException {
        return singlePUTandPOST(request, response, params, "PUT");
    }

    /**
     * This method has been deprecated in favor of {@link com.dotcms.rest.api.v1.workflow.WorkflowResource#fireActionDefault(HttpServletRequest, HttpServletResponse, String, String, long, SystemAction, FireActionForm)}
     * @param request
     * @param response
     * @param params
     * @deprecated
     * @see  {@link com.dotcms.rest.api.v1.workflow.WorkflowResource#fireActionDefault(HttpServletRequest, HttpServletResponse, String, String, long, SystemAction, FireActionForm)}
     *
     * @return
     * @throws URISyntaxException
     */
    @Deprecated
    @POST
    @Path("/{params:.*}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED,
            MediaType.APPLICATION_XML})
    public Response singlePOST(@Context HttpServletRequest request,
            @Context HttpServletResponse response, @PathParam("params") String params)
            throws URISyntaxException {
        return singlePUTandPOST(request, response, params, "POST");
    }

    private Response singlePUTandPOST(HttpServletRequest request, HttpServletResponse response,
            String params, String method)
            throws URISyntaxException {
        final InitDataObject init = new WebResource.InitBuilder(request, response)
                .requiredAnonAccess(AnonymousAccess.WRITE)
                .params(params)
                .init();

        Contentlet contentlet = new Contentlet();
        setRequestMetadata(contentlet, request);

        try {
            if (request.getContentType().startsWith(MediaType.APPLICATION_JSON)) {
                processJSON(contentlet, request.getInputStream());
            } else if (request.getContentType().startsWith(MediaType.APPLICATION_XML)) {
                try {

                    processXML(contentlet, request.getInputStream());
                } catch (DotSecurityException se) {
                    SecurityLogger.logInfo(this.getClass(),
                            "Invalid XML POSTED to ContentTypeResource from " + request
                                    .getRemoteAddr());
                    throw new ForbiddenException(se);
                }
            } else if (request.getContentType().startsWith(MediaType.APPLICATION_FORM_URLENCODED)) {
                if (method.equals("PUT")) {
                    processForm(contentlet, request.getInputStream());
                } else if (method.equals("POST")) {
                    processFormPost(contentlet, request, false);
                }

            }
        } catch (JSONException e) {

            Logger.error(this.getClass(), "Error processing JSON for Stream", e);

            Response.ResponseBuilder responseBuilder = Response.status(HttpStatus.SC_BAD_REQUEST);
            responseBuilder.entity(e.getMessage());
            return responseBuilder.build();
        } catch (Exception e) {
            Logger.error(this.getClass(), "Error processing Stream", e);
            return ResponseUtil.mapExceptionResponse(e);
        }

        return saveContent(contentlet, init);
    }

    protected Response saveContent(Contentlet contentlet, InitDataObject init)
            throws URISyntaxException {
        boolean live = init.getParamsMap().containsKey("publish");
        boolean clean = false;
        PageMode mode = PageMode.get();

        try {

            HibernateUtil.startTransaction();

            // preparing categories
            final Optional<List<Category>> categories = MapToContentletPopulator.INSTANCE.fetchCategories
                    (contentlet, init.getUser(), mode.respectAnonPerms);
            // running a workflow action?
            final ContentWorkflowResult contentWorkflowResult = processWorkflowAction(contentlet, init, live);
            final ContentletRelationships relationships = (ContentletRelationships) contentlet
                    .get(RELATIONSHIP_KEY);
            live = contentWorkflowResult.publish;

            // if one of the actions does not have save, so call the checkin
            if (!contentWorkflowResult.save) {

                contentlet.setIndexPolicy(IndexPolicyProvider.getInstance().forSingleContent());
                contentlet = APILocator.getContentletAPI()
                        .checkin(contentlet, relationships, categories.orElse(null), null, init.getUser(), mode.respectAnonPerms);
                if (live) {
                    APILocator.getContentletAPI()
                            .publish(contentlet, init.getUser(), mode.respectAnonPerms);
                }
            } else {

                contentlet = APILocator.getWorkflowAPI().fireContentWorkflow(contentlet,
                        new ContentletDependencies.Builder()
                        .respectAnonymousPermissions(mode.respectAnonPerms)
                        .modUser(init.getUser()).categories(categories.orElse(null))
                        .relationships(relationships)
                        .indexPolicy(IndexPolicyProvider.getInstance().forSingleContent())
                        .build());
            }

            HibernateUtil.closeAndCommitTransaction();
            clean = true;
        } catch (Exception e) {
            return ResponseUtil.mapExceptionResponse(e);
        } finally {
            try {
                if (!clean) {
                    HibernateUtil.rollbackTransaction();
                }
                HibernateUtil.closeSession();
            } catch (Exception e) {
                Logger.warn(this, e.getMessage(), e);
            }
        }

        // waiting for the index
        try {
            APILocator.getContentletAPI()
                    .isInodeIndexed(contentlet.getInode(), contentlet.isLive()); // not sure about this one.
        } catch (Exception ex) {
            return Response.serverError().build();
        }

        if (init.getParamsMap().containsKey("type") || init.getParamsMap()
                .containsKey("callback")) {
            if (init.getParamsMap().containsKey("callback") && !init.getParamsMap()
                    .containsKey("type")) {
                Map<String, String> map = init.getParamsMap();
                map.put("type", "jsonp");
                init.setParamsMap(map);
            }

            String type = init.getParamsMap().get(RESTParams.TYPE.getValue());
            String result = "";
            try {
                if ("xml".equals(type)) {

                    result = getXMLContentIds(contentlet);
                    return Response.ok(result, MediaType.APPLICATION_XML)
                            .location(
                                    new URI("content/inode/" + contentlet.getInode() + "/type/xml"))
                            .header("inode", contentlet.getInode())
                            .header("identifier", contentlet.getIdentifier())
                            .status(Status.OK).build();
                } else if ("text".equals(type)) {

                    return Response
                            .ok("inode:" + contentlet.getInode() + ",identifier:" + contentlet
                                    .getIdentifier(), MediaType.TEXT_PLAIN)
                            .location(new URI("content/inode/" + contentlet.getInode()
                                    + "/type/text"))
                            .header("inode", contentlet.getInode())
                            .header("identifier", contentlet.getIdentifier())
                            .status(Status.OK).build();
                } else {

                    result = getJSONContentIds(contentlet);

                    if (type.equals("jsonp")) {

                        String callback = init.getParamsMap().get(RESTParams.CALLBACK.getValue());
                        return Response.ok(callback + "(" + result + ")", "application/javascript")
                                .location(new URI("content/inode/" + contentlet.getInode()
                                        + "/type/jsonp/callback/" + callback))
                                .header("inode", contentlet.getInode())
                                .header("identifier", contentlet.getIdentifier())
                                .status(Status.OK).build();
                    } else {

                        return Response.ok(result, MediaType.APPLICATION_JSON)
                                .location(new URI("content/inode/" + contentlet.getInode()
                                        + "/type/json"))
                                .header("inode", contentlet.getInode())
                                .header("identifier", contentlet.getIdentifier())
                                .status(Status.OK).build();
                    }
                }
            } catch (Exception e) {
                Logger.warn(this, "Error converting result to XML/JSON");
                return Response.serverError().build();
            }
        } else {
            return Response.seeOther(new URI("content/inode/" + contentlet.getInode()))
                    .header("inode", contentlet.getInode())
                    .header("identifier", contentlet.getIdentifier())
                    .status(Status.OK).build();
        }
    }



    private ContentWorkflowResult processWorkflowAction(final Contentlet contentlet,
                                          final InitDataObject init,
                                          boolean live) throws DotDataException, DotSecurityException {

        boolean save = false;
        final Optional<WorkflowAction> foundWorkflowAction =
                init.getParamsMap().containsKey(Contentlet.WORKFLOW_ACTION_KEY.toLowerCase())?
                    this.findWorkflowActionById  (contentlet, init, this.getLongActionId(init.getParamsMap().get(Contentlet.WORKFLOW_ACTION_KEY.toLowerCase()))):
                    this.findWorkflowActionByName(contentlet, init);

        if (foundWorkflowAction.isPresent()) {

            contentlet.setActionId(foundWorkflowAction.get().getId());

            if (foundWorkflowAction.get().isCommentable()) {
                final String comment = init.getParamsMap()
                        .get(Contentlet.WORKFLOW_COMMENTS_KEY.toLowerCase());
                if (UtilMethods.isSet(comment)) {
                    contentlet.setStringProperty(Contentlet.WORKFLOW_COMMENTS_KEY, comment);
                }
            }

            if (foundWorkflowAction.get().isAssignable()) {
                final String assignTo = init.getParamsMap()
                        .get(Contentlet.WORKFLOW_ASSIGN_KEY.toLowerCase());
                if (UtilMethods.isSet(assignTo)) {
                    contentlet.setStringProperty(Contentlet.WORKFLOW_ASSIGN_KEY, assignTo);
                }
            }

            save = foundWorkflowAction.get().hasSaveActionlet(); // avoid manually saving
            live = false; // avoid manually publishing
        }

        return new ContentWorkflowResult(save, live);
    } // processWorkflowAction.

    private Optional<WorkflowAction> findWorkflowActionByName(final Contentlet contentlet,
                                                              final InitDataObject init) throws DotSecurityException, DotDataException {

        final List<WorkflowAction> availableActionsOnListing =
                APILocator.getWorkflowAPI().findAvailableActionsListing(contentlet, init.getUser());

        final List<WorkflowAction> availableActionsOnEditing =
                APILocator.getWorkflowAPI().findAvailableActionsEditing(contentlet, init.getUser());

        final Stream<WorkflowAction> combinedStream = Stream.concat(
                availableActionsOnListing.stream(),
                availableActionsOnEditing.stream()
        );

        final Set<WorkflowAction> availableActions = combinedStream.collect(Collectors.toCollection(LinkedHashSet::new));

        for (final WorkflowAction action : availableActions) {

            if (init.getParamsMap().containsKey(action.getName().toLowerCase())) {

                return Optional.of(action);
            }
        }

        return Optional.empty();
    }

    private Optional<WorkflowAction> findWorkflowActionById(final Contentlet contentlet,
                                                            final InitDataObject init,
                                                            final String workflowActionId) throws DotSecurityException, DotDataException {

        final List<WorkflowAction> availableActionsOnListing =
                APILocator.getWorkflowAPI().findAvailableActionsListing(contentlet, init.getUser());

        final List<WorkflowAction> availableActionsOnEditing =
                APILocator.getWorkflowAPI().findAvailableActionsEditing(contentlet, init.getUser());

        final Stream<WorkflowAction> combinedStream = Stream.concat(
                availableActionsOnListing.stream(),
                availableActionsOnEditing.stream()
        );

        final Set<WorkflowAction> availableActions = combinedStream.collect(Collectors.toCollection(LinkedHashSet::new));

        for (final WorkflowAction action : availableActions) {

            if (action.getId().equals(workflowActionId)) {

                return Optional.of(action);
            }
        }

        return Optional.empty();
    }

    /**
     * Converts the shortyId to long id
     * @param shortyId String
     * @return String id
     */
    private String getLongActionId (final String shortyId) {

        final Optional<ShortyId> shortyIdOptional =
                APILocator.getShortyAPI().getShorty(shortyId, ShortyIdAPI.ShortyInputType.WORKFLOW_ACTION);

        return shortyIdOptional.isPresent()?
                shortyIdOptional.get().longId:shortyId;
    } // getLongId.

    @SuppressWarnings("unchecked")
    protected void processXML(Contentlet contentlet, InputStream inputStream)
            throws IOException, DotSecurityException, DotDataException {

        // github issue #20364
        if(!USE_XSTREAM_FOR_DESERIALIZATION) {
            SecurityLogger.logInfo(ContentResource.class, "Insecure XML PUT or Post Detected - possible vunerability probing");
            throw new DotStateException("Unable to deserialize XML");
        }
        
        String input = IOUtils.toString(inputStream, "UTF-8");
        // deal with XXE or SSRF security vunerabilities in XML docs
        // besides, we do not expect a fully formed xml doc - only an xml doc that can be transformed into a java.util.Map
        // Mingle Card 512
        String upper = input.trim().toUpperCase();
        if (upper.contains("<!DOCTYPE") || upper.contains("<!ENTITY") || upper
                .startsWith("<?XML")) {
            throw new DotSecurityException("Invalid XML");
        }
        XStream xstream = XStreamHandler.newXStreamInstance();
        xstream.alias("content", Map.class);
        xstream.registerConverter(new MapEntryConverter());
        Map<String, Object> root = (Map<String, Object>) xstream.fromXML(input);
        processMap(contentlet, root);
    }

    protected void processForm(Contentlet contentlet, InputStream input) throws Exception {

        Map<String, Object> map = new HashMap<>();
        for (String param : IOUtils.toString(input).split("&")) {

            int index = param.indexOf("=");

            //Verify if we have a value
            if (index != -1) {
                String key = URLDecoder.decode(param.substring(0, index), "UTF-8");
                String value = URLDecoder
                        .decode(param.substring(index + 1, param.length()), "UTF-8");
                map.put(key, value);
            }
        }
        processMap(contentlet, map);
    }

    protected void processFormPost(Contentlet contentlet, HttpServletRequest request,
            boolean multiPart) throws Exception {

        Map<String, Object> map = new HashMap<>();

        if (multiPart) {
            ArrayList<Part> partList = new ArrayList<>(request.getParts());

            for (Part part : partList) {
                String partName = part.getName();
                String partValue = part.getHeader(partName);
                map.put(partName, partValue);
            }

        } else {
            Enumeration<String> parameterNames = request.getParameterNames();

            while (parameterNames.hasMoreElements()) {
                String paramName = parameterNames.nextElement();
                String paramValue = request.getParameter(paramName);
                map.put(paramName, paramValue);
            }
        }

        processMap(contentlet, map);
    }

    protected void processMap(final Contentlet contentlet, final Map<String, Object> map)
            throws DotDataException, DotSecurityException {

        this.contentHelper.populateContentletFromMap(contentlet, map);
    }

    @SuppressWarnings("unchecked")
    protected void processJSON(final Contentlet contentlet, final InputStream input)
            throws JSONException, IOException, DotDataException, DotSecurityException {

        processMap(contentlet, WebResource.processJSON(input));
    }

    private void setRequestMetadata(Contentlet contentlet, HttpServletRequest request) {
        try {
            contentlet.setStringProperty(HOST_HEADER, request.getHeader("Host"));
        } catch (Exception e) {
            Logger.error(this.getClass(), "Cannot set HOST_HEADER" + HOST_HEADER + e);

        }

        try {
            //request.getCookies() could return null.
            if (UtilMethods.isSet(request.getCookies())) {
                contentlet.setStringProperty(COOKIES, request.getCookies().toString());
            } else {
                //There are some cases where the cookies are not in the request, for example using curl without -b or --cookie.
                Logger.warn(this.getClass(), "COOKIES are not in the REQUEST");
            }
        } catch (Exception e) {
            Logger.error(this.getClass(), "Cannot set COOKIES " + e);

        }
        try {
            contentlet.setStringProperty(REQUEST_METHOD, request.getMethod());
        } catch (Exception e) {
            Logger.error(this.getClass(), "Cannot set REQUEST_METHOD" + e);

        }
        try {
            contentlet.setStringProperty(REFERER, request.getHeader("Referer"));
        } catch (Exception e) {
            Logger.error(this.getClass(), "Cannot set REFERER" + e);

        }
        try {
            contentlet.setStringProperty(USER_AGENT, request.getHeader("User-Agent"));
        } catch (Exception e) {
            Logger.error(this.getClass(), "Cannot set USER_AGENT" + e);

        }
        try {
            contentlet.setStringProperty(IP_ADDRESS, request.getRemoteHost());
        } catch (Exception e) {
            Logger.error(this.getClass(), "Cannot set IP_ADDRESS" + e);

        }

        try {
            contentlet.setStringProperty(ACCEPT_LANGUAGE, request.getHeader("Accept-Language"));
        } catch (Exception e) {
            Logger.error(this.getClass(), "Cannot set ACCEPT_LANGUAGE" + e);

        }
    }

    private class ContentWorkflowResult {

        private final boolean save;
        private final boolean publish;

        public ContentWorkflowResult(boolean save, boolean publish) {
            this.save = save;
            this.publish = publish;
        }


    }

}
