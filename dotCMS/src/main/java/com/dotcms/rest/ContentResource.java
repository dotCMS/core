package com.dotcms.rest;

import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.field.LegacyFieldTransformer;
import com.dotcms.rendering.velocity.viewtools.content.util.ContentUtils;
import com.dotcms.repackage.javax.ws.rs.*;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.javax.ws.rs.core.Response.Status;
import com.dotcms.repackage.org.apache.commons.httpclient.HttpStatus;
import com.dotcms.repackage.org.apache.commons.io.FileUtils;
import com.dotcms.repackage.org.apache.commons.io.IOUtils;
import com.dotcms.repackage.org.codehaus.jettison.json.JSONArray;
import com.dotcms.repackage.org.codehaus.jettison.json.JSONException;
import com.dotcms.repackage.org.codehaus.jettison.json.JSONObject;
import com.dotcms.repackage.org.glassfish.jersey.media.multipart.BodyPart;
import com.dotcms.repackage.org.glassfish.jersey.media.multipart.ContentDisposition;
import com.dotcms.repackage.org.glassfish.jersey.media.multipart.FormDataMultiPart;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotcms.uuid.shorty.ShortyId;
import com.dotcms.uuid.shorty.ShortyIdAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.business.DotLockException;
import com.dotmarketing.portlets.contentlet.model.*;
import com.dotmarketing.portlets.contentlet.util.ContentletUtil;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Field.FieldType;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.transform.ContentletRelationshipsTransformer;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.util.*;
import com.liferay.portal.model.User;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.xml.DomDriver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.dotmarketing.util.NumberUtil.toInt;
import static com.dotmarketing.util.NumberUtil.toLong;

@Path("/content")
public class ContentResource {

    public static final String[] ignoreFields = {"disabledWYSIWYG", "lowIndexPriority"};

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
            @PathParam("query") String query,
            @PathParam("sortby") String sortBy, @PathParam("limit") int limit,
            @PathParam("offset") int offset,
            @PathParam("type") String type,
            @PathParam("callback") String callback)
            throws DotDataException, JSONException {

        InitDataObject initData = webResource.init(null, true, request, false, null);

        Map<String, String> paramsMap = new HashMap<String, String>();
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
            @PathParam("query") String query,
            @PathParam("type") String type,
            @PathParam("callback") String callback) throws DotDataException {

        InitDataObject initData = webResource.init(null, true, request, false, null);

        Map<String, String> paramsMap = new HashMap<String, String>();
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


    @PUT
    @Path("/lock/{params:.*}")
    @Produces(MediaType.APPLICATION_JSON)

    public Response lockContent(@Context HttpServletRequest request,
            @Context HttpServletResponse response, @PathParam("params") String params)
            throws DotDataException, JSONException {

        InitDataObject initData = webResource.init(params, true, request, false, null);
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


    @PUT
    @Path("/canLock/{params:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response canLockContent(@Context HttpServletRequest request,
            @PathParam("params") String params)
            throws DotDataException, JSONException {

        InitDataObject initData = webResource.init(params, true, request, false, null);
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
                ContentletVersionInfo cvi = APILocator.getVersionableAPI()
                        .getContentletVersionInfo(id, contentlet.getLanguageId());
                if (contentlet.isLocked()) {
                    jo.put("lockedBy", cvi.getLockedBy());
                    jo.put("lockedOn", cvi.getLockedOn());
                    jo.put("lockedByName", APILocator.getUserAPI().loadUserById(cvi.getLockedBy()));


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

    @PUT
    @Path("/unlock/{params:.*}")
    @Produces(MediaType.APPLICATION_JSON)

    public Response unlockContent(@Context HttpServletRequest request,
            @Context HttpServletResponse response, @PathParam("params") String params)
            throws DotDataException, JSONException {

        InitDataObject initData = webResource.init(params, true, request, false, null);

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


    @GET
    @Path("/{params:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getContent(@Context HttpServletRequest request,
            @Context HttpServletResponse response, @PathParam("params") String params) {

        final InitDataObject initData = this.webResource.init
                (params, true, request, false, null);
        //Creating an utility response object
        final ResourceResponse responseResource = new ResourceResponse(initData.getParamsMap());
        final Map<String, String> paramsMap = initData.getParamsMap();
        final User user = initData.getUser();
        final String render = paramsMap.get(RESTParams.RENDER.getValue());
        final String query = paramsMap.get(RESTParams.QUERY.getValue());
        final String id = paramsMap.get(RESTParams.ID.getValue());
        final String limitStr = paramsMap.get(RESTParams.LIMIT.getValue());
        final String offsetStr = paramsMap.get(RESTParams.OFFSET.getValue());
        final String inode = paramsMap.get(RESTParams.INODE.getValue());
        final long language = toLong(paramsMap.get(RESTParams.LANGUAGE.getValue()),
                () -> APILocator.getLanguageAPI().getDefaultLanguage().getId());
        /* Limit and Offset Parameters Handling, if not passed, using default */
        final int limit = toInt(limitStr, () -> 10);
        final int offset = toInt(offsetStr, () -> 0);
        final boolean live = (paramsMap.get(RESTParams.LIVE.getValue()) == null ||
                !"false".equals(paramsMap.get(RESTParams.LIVE.getValue())));

        /* Fetching the content using a query if passed or an id */
        List<Contentlet> contentlets = new ArrayList<>();
        Boolean idPassed = false;
        Boolean inodePassed = false;
        Boolean queryPassed = false;
        String result = null;
        Optional<Status> status = Optional.empty();
        String type = paramsMap.get(RESTParams.TYPE.getValue());
        String orderBy = paramsMap.get(RESTParams.ORDERBY.getValue());

        type = UtilMethods.isSet(type) ? type : "json";
        orderBy = UtilMethods.isSet(orderBy) ? orderBy : "modDate desc";

        try {

            if (idPassed = UtilMethods.isSet(id)) {
                Optional.ofNullable(
                        this.contentHelper.hydrateContentLet(APILocator.getContentletAPI()
                                .findContentletByIdentifier(id, live, language, user, true)))
                        .ifPresent(contentlets::add);
            } else if (inodePassed = UtilMethods.isSet(inode)) {
                Optional.ofNullable(
                        this.contentHelper.hydrateContentLet(APILocator.getContentletAPI()
                                .find(inode, user, true)))
                        .ifPresent(contentlets::add);
            } else if (queryPassed = UtilMethods.isSet(query)) {
                String tmDate = (String) request.getSession().getAttribute("tm_date");
                String luceneQuery = processQuery(query);
                contentlets = ContentUtils.pull(luceneQuery, offset, limit, orderBy, user, tmDate);
            }

            status = (null == contentlets || contentlets.isEmpty()) ?
                    Optional.of(Status.NOT_FOUND) : status;
        } catch (DotSecurityException e) {

            Logger.debug(this, "Permission error: " + e.getMessage(), e);
            throw new ForbiddenException(e);
        } catch (Exception e) {
            if (idPassed) {
                Logger.warn(this, "Can't find Content with Identifier: " + id);
            } else if (queryPassed) {
                Logger.warn(this, "Can't find Content with Inode: " + inode);
            } else if (inodePassed) {
                Logger.warn(this, "Error searching Content : " + e.getMessage());
            }
            status = Optional.of(Status.INTERNAL_SERVER_ERROR);
        }

        /* Converting the Contentlet list to XML or JSON */
        try {
            if ("xml".equals(type)) {
                result = getXML(contentlets, request, response, render, user);
            } else {
                result = getJSON(contentlets, request, response, render, user);
            }
        } catch (Exception e) {
            Logger.warn(this, "Error converting result to XML/JSON");
        }

        return responseResource.response(result, null, status);
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

    private String getXML(final List<Contentlet> cons, HttpServletRequest request,
            HttpServletResponse response, String render, User user)
            throws DotDataException, IOException, DotSecurityException {
        XStream xstream = new XStream(new DomDriver());
        xstream.alias("content", Map.class);
        xstream.registerConverter(new MapEntryConverter());
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding='UTF-8'?>");
        sb.append("<contentlets>");

        for (Contentlet c : cons) {
            Map<String, Object> m = new HashMap<>();
            final ContentType type = c.getContentType();

            m.putAll(ContentletUtil.getContentPrintableMap(user, c));

            if (BaseContentType.WIDGET.equals(type.baseType()) && Boolean.toString(true)
                    .equalsIgnoreCase(render)) {
                m.put("parsedCode", WidgetResource.parseWidget(request, response, c));
            }

            if (BaseContentType.HTMLPAGE.equals(type.baseType())) {
                m.put(HTMLPageAssetAPI.URL_FIELD, this.contentHelper.getUrl(c));
            }

            final Set<String> jsonFields = getJSONFields(type);
            for (String key : m.keySet()) {
                if (jsonFields.contains(key)) {
                    m.put(key, c.getKeyValueProperty(key));
                }
            }

            sb.append(xstream.toXML(m));
        }

        sb.append("</contentlets>");
        return sb.toString();
    }


    private String getXMLContentIds(Contentlet con) throws DotDataException, IOException {
        XStream xstream = new XStream(new DomDriver());
        xstream.alias("content", Map.class);
        xstream.registerConverter(new MapEntryConverter());
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding='UTF-8'?>");
        sb.append("<contentlet>");
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("inode", con.getInode());
        m.put("identifier", con.getIdentifier());
        sb.append(xstream.toXML(m));
        sb.append("</contentlet>");
        return sb.toString();
    }

    private String getJSONContentIds(Contentlet con) throws IOException {
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

    private String getJSON(List<Contentlet> cons, HttpServletRequest request,
            HttpServletResponse response, String render, User user)
            throws IOException, DotDataException {
        JSONObject json = new JSONObject();
        JSONArray jsonCons = new JSONArray();

        for (Contentlet c : cons) {
            try {
                JSONObject jo = contentletToJSON(c, request, response, render, user);
                if (BaseContentType.HTMLPAGE.equals(c.getContentType().baseType())) {
                    jo.put(HTMLPageAssetAPI.URL_FIELD, this.contentHelper.getUrl(c));
                }
                jsonCons.put(jo);
            } catch (Exception e) {
                Logger.warn(this.getClass(), "unable JSON contentlet " + c.getIdentifier());
                Logger.debug(this.getClass(), "unable to find contentlet", e);
            }
        }

        try {
            json.put("contentlets", jsonCons);
        } catch (JSONException e) {
            Logger.warn(this.getClass(), "unable to create JSONObject");
            Logger.debug(this.getClass(), "unable to create JSONObject", e);
        }

        return json.toString();
    }

    public static Set<String> getJSONFields(ContentType type)
            throws DotDataException, DotSecurityException {
        Set<String> jsonFields = new HashSet<String>();
        List<Field> fields = new LegacyFieldTransformer(
                APILocator.getContentTypeAPI(APILocator.systemUser()).
                        find(type.inode()).fields()).asOldFieldList();
        for (Field f : fields) {
            if (f.getFieldType().equals(Field.FieldType.KEY_VALUE.toString())) {
                jsonFields.add(f.getVelocityVarName());
            }
        }

        return jsonFields;
    }

    public static JSONObject contentletToJSON(Contentlet con, HttpServletRequest request,
            HttpServletResponse response, String render, User user)
            throws JSONException, IOException, DotDataException, DotSecurityException {
        JSONObject jo = new JSONObject();
        ContentType type = con.getContentType();
        Map<String, Object> map = ContentletUtil.getContentPrintableMap(user, con);

        Set<String> jsonFields = getJSONFields(type);

        for (String key : map.keySet()) {
            if (Arrays.binarySearch(ignoreFields, key) < 0) {
                if (jsonFields.contains(key)) {
                    Logger.info(ContentResource.class,
                            key + " is a json field: " + map.get(key).toString());
                    jo.put(key, new JSONObject(con.getKeyValueProperty(key)));
                } else {
                    jo.put(key, map.get(key));
                }
            }
        }

        if (BaseContentType.WIDGET.equals(type.baseType()) && Boolean.toString(true)
                .equalsIgnoreCase(render)) {
            jo.put("parsedCode", WidgetResource.parseWidget(request, response, con));
        }

        return jo;
    }

    public class MapEntryConverter implements Converter {

        public boolean canConvert(@SuppressWarnings("rawtypes") Class clazz) {
            return AbstractMap.class.isAssignableFrom(clazz);
        }

        public void marshal(Object value, HierarchicalStreamWriter writer,
                MarshallingContext context) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) value;
            for (Entry<String, Object> entry : map.entrySet()) {
                writer.startNode(entry.getKey().toString());
                writer.setValue(entry.getValue() != null ? entry.getValue().toString() : "");
                writer.endNode();
            }
        }

        public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
            Map<String, String> map = new HashMap<String, String>();

            while (reader.hasMoreChildren()) {
                reader.moveDown();
                map.put(reader.getNodeName(), reader.getValue());
                reader.moveUp();
            }
            return map;
        }



    }

    @PUT
    @Path("/{params:.*}")
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response multipartPUT(@Context HttpServletRequest request,
            @Context HttpServletResponse response,
            FormDataMultiPart multipart, @PathParam("params") String params)
            throws URISyntaxException, DotDataException {
        return multipartPUTandPOST(request, response, multipart, params, "PUT");
    }

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

    private Response multipartPUTandPOST(HttpServletRequest request, HttpServletResponse response,
            FormDataMultiPart multipart, String params, String method)
            throws URISyntaxException, DotDataException {

        InitDataObject init = webResource.init(params, true, request, false, null);
        Contentlet contentlet = new Contentlet();
        setRequestMetadata(contentlet, request);

        Map<String, Object> map = new HashMap<String, Object>();
        List<String> usedBinaryFields = new ArrayList<String>();
        String binaryFieldsInput = null;
        List<String> binaryFields = new ArrayList<>();

        for (BodyPart part : multipart.getBodyParts()) {
            ContentDisposition cd = part.getContentDisposition();
            String name = cd != null && cd.getParameters().containsKey("name") ? cd.getParameters()
                    .get("name") : "";

            if (part.getMediaType().equals(MediaType.APPLICATION_JSON_TYPE) || name
                    .equals("json")) {
                try {
                    processJSON(contentlet, part.getEntityAs(InputStream.class));
                    try {
                        binaryFieldsInput =
                            webResource.processJSON(part.getEntityAs(InputStream.class)).get("binary_fields")
                                .toString();
                    } catch (NullPointerException npe) {
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

                    Response.ResponseBuilder responseBuilder = Response
                            .status(HttpStatus.SC_BAD_REQUEST);
                    responseBuilder.entity(e.getMessage());
                    return responseBuilder.build();
                } catch (IOException e) {

                    Logger.error(this.getClass(), "Error processing Stream", e);

                    Response.ResponseBuilder responseBuilder = Response
                            .status(HttpStatus.SC_INTERNAL_SERVER_ERROR);
                    responseBuilder.entity(e.getMessage());
                    return responseBuilder.build();
                } catch (DotSecurityException e) {
                    throw new ForbiddenException(e);
                }
            } else if (part.getMediaType().equals(MediaType.APPLICATION_XML_TYPE) || name
                    .equals("xml")) {
                try {
                    processXML(contentlet, part.getEntityAs(InputStream.class));
                } catch (Exception e) {
                    if (e instanceof DotSecurityException) {
                        SecurityLogger.logInfo(this.getClass(),
                                "Invalid XML POSTED to ContentTypeResource from " + request
                                        .getRemoteAddr());
                    }
                    Logger.error(this.getClass(), "Error processing Stream", e);

                    Response.ResponseBuilder responseBuilder = Response
                            .status(HttpStatus.SC_INTERNAL_SERVER_ERROR);
                    responseBuilder.entity(e.getMessage());
                    return responseBuilder.build();
                }
            } else if (part.getMediaType().equals(MediaType.APPLICATION_FORM_URLENCODED_TYPE)
                    || name.equals("urlencoded")) {
                try {
                    processForm(contentlet, part.getEntityAs(InputStream.class));
                } catch (Exception e) {
                    Logger.error(this.getClass(), "Error processing Stream", e);

                    Response.ResponseBuilder responseBuilder = Response
                            .status(HttpStatus.SC_INTERNAL_SERVER_ERROR);
                    responseBuilder.entity(e.getMessage());
                    return responseBuilder.build();
                }
            } else if (part.getMediaType().equals(MediaType.TEXT_PLAIN_TYPE)) {
                try {
                    map.put(name, part.getEntityAs(String.class));
                    processMap(contentlet, map);
                } catch (Exception e) {
                    Logger.error(this.getClass(), "Error processing Plain Tex", e);

                    Response.ResponseBuilder responseBuilder = Response
                            .status(HttpStatus.SC_INTERNAL_SERVER_ERROR);
                    responseBuilder.entity(e.getMessage());
                    return responseBuilder.build();
                }
            } else if (part.getContentDisposition() != null) {
                InputStream input = part.getEntityAs(InputStream.class);
                String filename = part.getContentDisposition().getFileName();
                java.io.File tmpFolder = new File(
                        APILocator.getFileAssetAPI().getRealAssetPathTmpBinary() + UUIDUtil.uuid());
                tmpFolder.mkdirs();
                java.io.File tmp = new File(
                        tmpFolder.getAbsolutePath() + File.separator + filename);
                if (tmp.exists()) {
                    tmp.delete();
                }
                try {
                    FileUtils.copyInputStreamToFile(input, tmp);
                    List<Field> fields = new LegacyFieldTransformer(
                            APILocator.getContentTypeAPI(APILocator.systemUser()).
                                    find(contentlet.getContentType().inode()).fields())
                            .asOldFieldList();
                    for (Field ff : fields) {
                        // filling binariess in order. as they come / as field order says
                        String fieldName = ff.getFieldContentlet();
                        if (fieldName.startsWith("binary")
                                && !usedBinaryFields.contains(fieldName)) {
                            String fieldVarName = ff.getVelocityVarName();
                            if (binaryFields.size() > 0) {
                                fieldVarName = binaryFields.remove(0);
                            }
                            contentlet.setBinary(fieldVarName, tmp);
                            usedBinaryFields.add(fieldName);
                            break;
                        }
                    }
                } catch (IOException e) {

                    Logger.error(this.getClass(), "Error processing Stream", e);

                    Response.ResponseBuilder responseBuilder = Response
                            .status(HttpStatus.SC_INTERNAL_SERVER_ERROR);
                    responseBuilder.entity(e.getMessage());
                    return responseBuilder.build();
                } catch (DotSecurityException e) {
                    throw new ForbiddenException(e);
                }
            }
        }

        return saveContent(contentlet, init);
    }

    @PUT
    @Path("/{params:.*}")
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED,
            MediaType.APPLICATION_XML})
    public Response singlePUT(@Context HttpServletRequest request,
            @Context HttpServletResponse response, @PathParam("params") String params)
            throws URISyntaxException {
        return singlePUTandPOST(request, response, params, "PUT");
    }

    @POST
    @Path("/{params:.*}")
    @Produces(MediaType.TEXT_PLAIN)
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
        InitDataObject init = webResource.init(params, true, request, false, null);

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

            Response.ResponseBuilder responseBuilder = Response
                    .status(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            responseBuilder.entity(e.getMessage());
            return responseBuilder.build();
        }

        return saveContent(contentlet, init);
    }

    protected Response saveContent(Contentlet contentlet, InitDataObject init)
            throws URISyntaxException {
        boolean live = init.getParamsMap().containsKey("publish");
        boolean clean = false;
        final boolean ALLOW_FRONT_END_SAVING = Config
            .getBooleanProperty("REST_API_CONTENT_ALLOW_FRONT_END_SAVING", false);

        try {

            // preparing categories
            List<Category> cats = new ArrayList<>();
            List<Field> fields = new LegacyFieldTransformer(
                    APILocator.getContentTypeAPI(APILocator.systemUser()).
                            find(contentlet.getContentType().inode()).fields()).asOldFieldList();
            for (Field field : fields) {
                if (field.getFieldType().equals(FieldType.CATEGORY.toString())) {
                    String catValue = contentlet.getStringProperty(field.getVelocityVarName());
                    if (UtilMethods.isSet(catValue)) {
                        for (String cat : catValue.split("\\s*,\\s*")) {
                            // take it as catId
                            Category category = APILocator.getCategoryAPI()
                                    .find(cat, init.getUser(), ALLOW_FRONT_END_SAVING);
                            if (category != null && InodeUtils.isSet(category.getCategoryId())) {
                                cats.add(category);
                            } else {
                                // try it as catKey
                                category = APILocator.getCategoryAPI()
                                        .findByKey(cat, init.getUser(), ALLOW_FRONT_END_SAVING);
                                if (category != null && InodeUtils
                                        .isSet(category.getCategoryId())) {
                                    cats.add(category);
                                } else {
                                    // try it as variable
                                    // FIXME: https://github.com/dotCMS/dotCMS/issues/2847
                                    HibernateUtil hu = new HibernateUtil(Category.class);
                                    hu.setQuery("from " + Category.class.getCanonicalName()
                                            + " WHERE category_velocity_var_name=?");
                                    hu.setParam(cat);
                                    category = (Category) hu.load();
                                    if (category != null && InodeUtils
                                            .isSet(category.getCategoryId())) {
                                        cats.add(category);
                                    }
                                }
                            }

                        }
                    }
                }
            }

            // running a workflow action?
            final ContentWorkflowResult contentWorkflowResult = processWorkflowAction(contentlet, init, live);

            live = contentWorkflowResult.publish;
            Map<Relationship, List<Contentlet>> relationships = (Map<Relationship, List<Contentlet>>) contentlet
                    .get(RELATIONSHIP_KEY);

            HibernateUtil.startTransaction();

            cats = UtilMethods.isSet(cats)?cats:null;

            // if one of the actions does not have save, so call the checkin
            if (!contentWorkflowResult.save) {

                contentlet.setIndexPolicy(IndexPolicyProvider.getInstance().forSingleContent());
                contentlet = APILocator.getContentletAPI()
                        .checkin(contentlet, relationships, cats, init.getUser(), ALLOW_FRONT_END_SAVING);
                if (live) {
                    APILocator.getContentletAPI()
                            .publish(contentlet, init.getUser(), ALLOW_FRONT_END_SAVING);
                }
            } else {

                contentlet = APILocator.getWorkflowAPI().fireContentWorkflow(contentlet,
                        new ContentletDependencies.Builder()
                        .respectAnonymousPermissions(ALLOW_FRONT_END_SAVING)
                        .modUser(init.getUser()).categories(cats)
                        .relationships(this.getContentletRelationshipsFromMap(contentlet, relationships))
                        .indexPolicy(IndexPolicyProvider.getInstance().forSingleContent())
                        .build());
            }

            HibernateUtil.closeAndCommitTransaction();
            clean = true;
        } catch (DotContentletStateException e) {

            Logger.error(this.getClass(), "Error saving Contentlet" + e);

            Response.ResponseBuilder responseBuilder = Response.status(HttpStatus.SC_CONFLICT);
            responseBuilder.entity(e.getMessage());
            return responseBuilder.build();
        } catch (IllegalArgumentException e) {

            Logger.error(this.getClass(), "Error saving Contentlet" + e);

            Response.ResponseBuilder responseBuilder = Response.status(HttpStatus.SC_CONFLICT);
            responseBuilder.entity(e.getMessage());
            return responseBuilder.build();
        } catch (DotSecurityException e) {

            Logger.error(this.getClass(), "Error saving Contentlet" + e);
            throw new ForbiddenException(e);
        } catch (Exception e) {
            Logger.warn(this, e.getMessage(), e);
            return Response.serverError().build();
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

    private ContentletRelationships getContentletRelationshipsFromMap(final Contentlet contentlet,
                                                                      final Map<Relationship, List<Contentlet>> contentRelationships) {

        return new ContentletRelationshipsTransformer(contentlet, contentRelationships).findFirst();
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

        String input = IOUtils.toString(inputStream, "UTF-8");
        // deal with XXE or SSRF security vunerabilities in XML docs
        // besides, we do not expect a fully formed xml doc - only an xml doc that can be transformed into a java.util.Map
        // Mingle Card 512
        String upper = input.trim().toUpperCase();
        if (upper.contains("<!DOCTYPE") || upper.contains("<!ENTITY") || upper
                .startsWith("<?XML")) {
            throw new DotSecurityException("Invalid XML");
        }
        XStream xstream = new XStream(new DomDriver());
        xstream.alias("content", Map.class);
        xstream.registerConverter(new MapEntryConverter());
        Map<String, Object> root = (Map<String, Object>) xstream.fromXML(input);
        processMap(contentlet, root);
    }

    protected void processForm(Contentlet contentlet, InputStream input) throws Exception {

        Map<String, Object> map = new HashMap<String, Object>();
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

        Map<String, Object> map = new HashMap<String, Object>();

        if (multiPart) {
            ArrayList<Part> partList = new ArrayList<Part>(request.getParts());

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

        processMap(contentlet, webResource.processJSON(input));
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