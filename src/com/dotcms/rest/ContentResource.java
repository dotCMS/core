package com.dotcms.rest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.lucene.queryParser.ParseException;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Field.FieldType;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.viewtools.content.util.ContentUtils;
import com.liferay.portal.model.User;
import com.sun.jersey.core.header.ContentDisposition;
import com.sun.jersey.multipart.BodyPart;
import com.sun.jersey.multipart.FormDataMultiPart;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.xml.DomDriver;

@Path("/content")
public class ContentResource extends WebResource {
    /**
     * performs a call to APILocator.getContentletAPI().searchIndex() with the 
     * specified parameters.
     * Example call using curl:
     * curl -XGET http://localhost:8080/api/content/indexsearch/+structurename:webpagecontent/sortby/modDate/limit/20/offset/0 
     * 
     * @param request request object
     * @param query lucene query
     * @param sortBy field to sortby
     * @param limit how many results return
     * @param offset how many results skip 
     * @return json array of objects. each object with inode and identifier
     * @throws ParseException
     * @throws DotSecurityException
     * @throws DotDataException
     * @throws JSONException
     */
    @GET
    @Path("/indexsearch/{query}/sortby/{sortby}/limit/{limit}/offset/{offset}")
    @Produces(MediaType.APPLICATION_JSON)
    public String indexSearch(@Context HttpServletRequest request, @PathParam("query") String query, @PathParam("sortby") String sortBy, @PathParam("limit") int limit, @PathParam("offset") int offset) throws ParseException, DotSecurityException, DotDataException, JSONException {
        InitDataObject initData = init(null, true, request, false);

        List<ContentletSearch> searchIndex = APILocator.getContentletAPI().searchIndex(query, limit, offset, sortBy, initData.getUser(), true);
        JSONArray array=new JSONArray();
        for(ContentletSearch cs : searchIndex) {
            array.put(new JSONObject()
            .put("inode", cs.getInode())
            .put("identifier", cs.getIdentifier()));
        }
        return array.toString();
    }

    /**
     * Performs a call to APILocator.getContentletAPI().indexCount()
     * using the specified parameters.
     * 
     * Example call using curl:
     * curl -XGET http://localhost:8080/api/content/indexcount/+structurename:webpagecontent
     * 
     * @param request request obejct
     * @param query lucene query to count on
     * @return a string with the count
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @GET
    @Path("/indexcount/{query}")
    @Produces(MediaType.TEXT_PLAIN)
    public String indexCount(@Context HttpServletRequest request, @PathParam("query") String query) throws DotDataException, DotSecurityException {
        InitDataObject initData = init(null, true, request, false);
        return Long.toString(APILocator.getContentletAPI().indexCount(query, initData.getUser(), true));
    }

    @GET
    @Path("/{params:.*}")
    @Produces(MediaType.TEXT_PLAIN)
    public String getContent(@Context HttpServletRequest request, @Context HttpServletResponse response, @PathParam("params") String params) {
        InitDataObject initData = init(params, true, request, false);

        Map<String, String> paramsMap = initData.getParamsMap();
        User user = initData.getUser();

        String render = paramsMap.get(RESTParams.RENDER.getValue());
        String type = paramsMap.get(RESTParams.TYPE.getValue());
        String query = paramsMap.get(RESTParams.QUERY.getValue());
        String id = paramsMap.get(RESTParams.ID.getValue());
        String orderBy = paramsMap.get(RESTParams.ORDERBY.getValue());
        String limitStr = paramsMap.get(RESTParams.LIMIT.getValue());
        String offsetStr = paramsMap.get(RESTParams.OFFSET.getValue());
        String inode = paramsMap.get(RESTParams.INODE.getValue());
        String result = null;
        type = UtilMethods.isSet(type)?type:"json";
        orderBy = UtilMethods.isSet(orderBy)?orderBy:"modDate desc";
        long language = APILocator.getLanguageAPI().getDefaultLanguage().getId();

        if(paramsMap.get(RESTParams.LANGUAGE.getValue()) != null){
            try{
                language= Long.parseLong(paramsMap.get(RESTParams.LANGUAGE.getValue()))	;
            }
            catch(Exception e){
                Logger.warn(this.getClass(), "Invald language passed in, defaulting to, well, the default");
            }
        }

        /* Limit and Offset Parameters Handling, if not passed, using default */

        int limit = 10;
        int offset = 0;

        try {
            if(UtilMethods.isSet(limitStr)) {
                limit = Integer.parseInt(limitStr);
            }
        } catch(NumberFormatException e) {
        }

        try {
            if(UtilMethods.isSet(offsetStr)) {
                offset = Integer.parseInt(offsetStr);
            }
        } catch(NumberFormatException e) {
        }

        boolean live = (paramsMap.get(RESTParams.LIVE.getValue()) == null || ! "false".equals(paramsMap.get(RESTParams.LIVE.getValue())));

        /* Fetching the content using a query if passed or an id */

        List<Contentlet> cons = new ArrayList<Contentlet>();
        Boolean idPassed = false;
        Boolean inodePassed = false;
        Boolean queryPassed = false;

        try {
            if(idPassed = UtilMethods.isSet(id)) {
                cons.add(APILocator.getContentletAPI().findContentletByIdentifier(id, live, language, user, true));
            } else if(inodePassed = UtilMethods.isSet(inode)) {
                cons.add(APILocator.getContentletAPI().find(inode, user, true));
            } else if(queryPassed = UtilMethods.isSet(query)) {
                String tmDate=(String)request.getSession().getAttribute("tm_date");
                cons = ContentUtils.pull(query, offset, limit,orderBy,user,tmDate);
            }
        } catch (Exception e) {
            if(idPassed) {
                Logger.warn(this, "Can't find Content with Identifier: " + id);
            } else if(queryPassed) {
                Logger.warn(this, "Can't find Content with Inode: " + inode);
            } else if(inodePassed) {
                Logger.warn(this, "Error searching Content : "  + e.getMessage());
            }
        }

        /* Converting the Contentlet list to XML or JSON */

        try {
            if("xml".equals(type)) {
                result = getXML(cons, request, response, render);
            } else {
                result = getJSON(cons, request, response, render);
            }
        } catch (Exception e) {
            Logger.warn(this, "Error converting result to XML/JSON");
        }

        return result;
    }


    private String getXML(List<Contentlet> cons, HttpServletRequest request, HttpServletResponse response, String render) throws DotDataException, IOException {
        XStream xstream = new XStream(new DomDriver());
        xstream.alias("content", Map.class);
        xstream.registerConverter(new MapEntryConverter());
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding='UTF-8'?>");
        sb.append("<contentlets>");

        for(Contentlet c : cons){
            Map<String, Object> m = c.getMap();
            Structure s = c.getStructure();

            for(Field f : FieldsCache.getFieldsByStructureInode(s.getInode())){
                if(f.getFieldType().equals(Field.FieldType.BINARY.toString())){
                    m.put(f.getVelocityVarName(), "/contentAsset/raw-data/" +  c.getIdentifier() + "/" + f.getVelocityVarName()	);
                    m.put(f.getVelocityVarName() + "ContentAsset", c.getIdentifier() + "/" +f.getVelocityVarName()	);
                }
            }

            if(s.getStructureType() == Structure.STRUCTURE_TYPE_WIDGET && "true".equals(render)) {
                m.put("parsedCode",  WidgetResource.parseWidget(request, response, c));
            }

            Set<String> jsonFields=getJSONFields(s);
            for(String key : m.keySet())
                if(jsonFields.contains(key))
                    m.put(key, c.getKeyValueProperty(key));

            sb.append(xstream.toXML(m));
        }

        sb.append("</contentlets>");
        return sb.toString();
    }

    private String getJSON(List<Contentlet> cons, HttpServletRequest request, HttpServletResponse response, String render) throws IOException{
        JSONObject json = new JSONObject();
        JSONArray jsonCons = new JSONArray();

        for(Contentlet c : cons){
            try {
                jsonCons.put(contentletToJSON(c, request, response, render));
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

    private Set<String> getJSONFields(Structure s) {
        Set<String> jsonFields=new HashSet<String>();
        for(Field f : FieldsCache.getFieldsByStructureInode(s.getInode()))
            if(f.getFieldType().equals(Field.FieldType.KEY_VALUE.toString()))
                jsonFields.add(f.getVelocityVarName());
        return jsonFields;
    }

    private JSONObject contentletToJSON(Contentlet con, HttpServletRequest request, HttpServletResponse response, String render) throws JSONException, IOException{
        JSONObject jo = new JSONObject();
        Structure s = con.getStructure();
        Map<String,Object> map = con.getMap();

        Set<String> jsonFields=getJSONFields(s);

        for(String key : map.keySet()) {
            if(Arrays.binarySearch(ignoreFields, key) < 0)
                if(jsonFields.contains(key)) {
                    Logger.info(this, key+" is a json field: "+map.get(key).toString());
                    jo.put(key, new JSONObject(con.getKeyValueProperty(key)));
                }
                else
                    jo.put(key, map.get(key));
        }

        for(Field f : FieldsCache.getFieldsByStructureInode(s.getInode())){
            if(f.getFieldType().equals(Field.FieldType.BINARY.toString())){
                jo.put(f.getVelocityVarName(), "/contentAsset/raw-data/" +  con.getIdentifier() + "/" + f.getVelocityVarName()	);
                jo.put(f.getVelocityVarName() + "ContentAsset", con.getIdentifier() + "/" +f.getVelocityVarName()	);
            }
        }

        if(s.getStructureType() == Structure.STRUCTURE_TYPE_WIDGET && "true".equals(render)) {
            jo.put("parsedCode",  WidgetResource.parseWidget(request, response, con));
        }

        return jo;
    }

    final String[] ignoreFields = {"disabledWYSIWYG", "lowIndexPriority"};

    public class MapEntryConverter implements Converter{
        public boolean canConvert(@SuppressWarnings("rawtypes") Class clazz) {
            return AbstractMap.class.isAssignableFrom(clazz);
        }

        public void marshal(Object value, HierarchicalStreamWriter writer, MarshallingContext context) {
            @SuppressWarnings("unchecked")
            Map<String,Object> map = (Map<String,Object>) value;
            for (Entry<String,Object> entry : map.entrySet()) {
                writer.startNode(entry.getKey().toString());
                writer.setValue(entry.getValue()!=null?entry.getValue().toString():"");
                writer.endNode();
            }
        }

        public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
            Map<String, String> map = new HashMap<String, String>();

            while(reader.hasMoreChildren()) {
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
    public Response multipartPUT(@Context HttpServletRequest request, @Context HttpServletResponse response, 
            FormDataMultiPart multipart,@PathParam("params") String params) throws URISyntaxException {
        InitDataObject init=init(params,true,request,true);
        User user=init.getUser();

        Contentlet contentlet=new Contentlet();
        
        for(BodyPart part : multipart.getBodyParts()) {
            ContentDisposition cd=part.getContentDisposition();
            String name=cd!=null && cd.getParameters().containsKey("name") ? cd.getParameters().get("name") : "";
            
            if(part.getMediaType().equals(MediaType.APPLICATION_JSON_TYPE) || name.equals("json")) {
                try {
                    processJSON(contentlet,part.getEntityAs(InputStream.class));
                } catch (JSONException e) {
                    return Response.status(Status.BAD_REQUEST).build();
                } catch (IOException e) {
                    return Response.status(Status.INTERNAL_SERVER_ERROR).build();
                }
            }
            else if(part.getMediaType().equals(MediaType.APPLICATION_XML_TYPE) || name.equals("xml")) {
                try {
                    processXML(contentlet, part.getEntityAs(InputStream.class));
                } catch (Exception e) {
                    return Response.status(Status.INTERNAL_SERVER_ERROR).build();
                }
            }
            else if(part.getMediaType().equals(MediaType.APPLICATION_FORM_URLENCODED_TYPE) || name.equals("urlencoded")) {
                try {
                    processForm(contentlet, part.getEntityAs(InputStream.class));
                } catch (Exception e) {
                    return Response.status(Status.INTERNAL_SERVER_ERROR).build();
                }
            }
            else if(part.getContentDisposition()!=null) {
                InputStream input=part.getEntityAs(InputStream.class);
                String filename=part.getContentDisposition().getFileName();
                java.io.File tmp=new java.io.File(APILocator.getFileAPI().getRealAssetPathTmpBinary()
                        + java.io.File.separator + user.getUserId() 
                        + java.io.File.separator + System.currentTimeMillis()  
                        + java.io.File.separator + filename);
                if(tmp.exists())
                    tmp.delete();
                try {
                    FileUtils.copyInputStreamToFile(input, tmp);
                    
                    for(Field ff : FieldsCache.getFieldsByStructureInode(contentlet.getStructureInode())) {
                        // filling binarys in order. as they come / as field order says
                        if(ff.getFieldContentlet().startsWith("binary") && contentlet.getBinary(ff.getVelocityVarName())==null) {
                            contentlet.setBinary(ff.getVelocityVarName(), tmp);
                            break;
                        }   
                    }
                } catch (IOException e) {
                    return Response.status(Status.INTERNAL_SERVER_ERROR).build();
                }
            }
        }
        
        return saveContent(contentlet,init);
    }
    
    @PUT
    @Path("/{params:.*}")
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes({MediaType.APPLICATION_JSON,MediaType.APPLICATION_FORM_URLENCODED,MediaType.APPLICATION_XML})
    public Response singlePUT(@Context HttpServletRequest request, @Context HttpServletResponse response, @PathParam("params") String params) throws URISyntaxException {
        InitDataObject init=init(params,true,request,true);
        
        Contentlet contentlet=new Contentlet();
        try {
            if(request.getContentType().equals(MediaType.APPLICATION_JSON)) {
                processJSON(contentlet, request.getInputStream());
            }
            else if(request.getContentType().equals(MediaType.APPLICATION_XML)) {
                processXML(contentlet, request.getInputStream());
            }
            else if(request.getContentType().equals(MediaType.APPLICATION_FORM_URLENCODED)) {
                processForm(contentlet, request.getInputStream());
            }
        } catch(JSONException e) {
            return Response.status(Status.BAD_REQUEST).build();
        } catch (Exception e) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
        
        return saveContent(contentlet,init);
    }
    
    /*
    @PATCH
    @Path("/{params:.*}")
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML,MediaType.APPLICATION_FORM_URLENCODED})
    public Response singlePatch(@Context HttpServletRequest request, @Context HttpServletResponse response, @PathParam("params") String params) throws URISyntaxException {
        return Response.ok().build();
    }*/
    
    protected Response saveContent(Contentlet contentlet, InitDataObject init) throws URISyntaxException {
        boolean live = init.getParamsMap().containsKey("publish");
        boolean clean=false;
        
        try {
            // preparing categories
            List<Category> cats=new ArrayList<Category>();
            for(Field field : FieldsCache.getFieldsByStructureInode(contentlet.getStructureInode())) {
                if(field.getFieldType().equals(FieldType.CATEGORY.toString())) {
                    String catValue=contentlet.getStringProperty(field.getVelocityVarName());
                    if(UtilMethods.isSet(catValue)) {
                        for(String cat : catValue.split("\\s*,\\s*")) {
                            // take it as catId
                            Category category=APILocator.getCategoryAPI().find(cat, init.getUser(), false);
                            if(category!=null && InodeUtils.isSet(category.getCategoryId())) {
                                cats.add(category);
                            }
                            else {
                                // try it as catKey
                                category=APILocator.getCategoryAPI().findByKey(cat, init.getUser(), false);
                                if(category!=null && InodeUtils.isSet(category.getCategoryId())) {
                                    cats.add(category);
                                }
                                else {
                                    // try it as variable
                                    // FIXME: https://github.com/dotCMS/dotCMS/issues/2847
                                    HibernateUtil hu=new HibernateUtil(Category.class);
                                    hu.setQuery("from "+Category.class.getCanonicalName()+" WHERE category_velocity_var_name=?");
                                    hu.setParam(cat);
                                    category=(Category)hu.load();
                                    if(category!=null && InodeUtils.isSet(category.getCategoryId())) {
                                        cats.add(category);
                                    }                                    
                                }
                            }
                            
                        }
                    }
                }
            }            
            
            // running a workflow action?
            for(WorkflowAction action : APILocator.getWorkflowAPI().findAvailableActions(contentlet, init.getUser())) {
                if(init.getParamsMap().containsKey(action.getName().toLowerCase())) {
                    
                    contentlet.setStringProperty(Contentlet.WORKFLOW_ACTION_KEY, action.getId());
                    
                    if(action.isCommentable()) {
                        String comment=init.getParamsMap().get(Contentlet.WORKFLOW_COMMENTS_KEY.toLowerCase());
                        if(UtilMethods.isSet(comment)) {
                            contentlet.setStringProperty(Contentlet.WORKFLOW_COMMENTS_KEY, comment);
                        }
                    }
                    
                    if(action.isAssignable()) {
                        String assignTo=init.getParamsMap().get(Contentlet.WORKFLOW_ASSIGN_KEY.toLowerCase());
                        if(UtilMethods.isSet(assignTo)) {
                            contentlet.setStringProperty(Contentlet.WORKFLOW_ASSIGN_KEY, assignTo);
                        }
                    }
                    
                    live=false; // avoid manually publishing 
                    break;
                }
            }
            
            HibernateUtil.startTransaction();
            
            contentlet = APILocator.getContentletAPI().checkin(contentlet,new HashMap<Relationship, List<Contentlet>>(),cats,new ArrayList<Permission>(),init.getUser(),false);
            
            if(live)
                APILocator.getContentletAPI().publish(contentlet, init.getUser(), false);
            
            HibernateUtil.commitTransaction();
            clean = true;
        } catch (DotContentletStateException e) {
            return Response.status(Status.CONFLICT).build();
        } catch(IllegalArgumentException e) {
            return Response.status(Status.CONFLICT).build();
        } catch (DotSecurityException e) {
            return Response.status(Status.FORBIDDEN).build();
        } catch (Exception e) {
            Logger.warn(this, e.getMessage(), e);
            return Response.serverError().build();
        }
        finally {
            try {
                if(!clean)
                    HibernateUtil.rollbackTransaction();
                HibernateUtil.closeSession();
            }catch(Exception e) {
                Logger.warn(this, e.getMessage(), e);
            }
        }
        
        // waiting for the index
        try {
            APILocator.getContentletAPI().isInodeIndexed(contentlet.getInode(),contentlet.isLive());
        } catch (Exception ex) {
            return Response.serverError().build();
        }
        
        return Response.seeOther(new URI("/content/inode/"+contentlet.getInode()))
                       .header("inode", contentlet.getInode())
                       .header("identifier", contentlet.getIdentifier())
                       .status(Status.OK).build();
    }

    @SuppressWarnings("unchecked")
    protected void processXML(Contentlet contentlet, InputStream input) {
        XStream xstream=new XStream(new DomDriver());
        xstream.alias("content", Map.class);
        xstream.registerConverter(new MapEntryConverter());
        Map<String,Object> root=(Map<String,Object>) xstream.fromXML(input);
        processMap(contentlet,root);
    }
    
    protected void processForm(Contentlet contentlet, InputStream input) throws Exception {
        Map<String,Object> map=new HashMap<String,Object>();
        for(String param : IOUtils.toString(input).split("&")) {
            String[] parts=param.split("=");
            String key=URLDecoder.decode(parts[0],"UTF-8");
            String value=URLDecoder.decode(parts[1],"UTF-8");
            map.put(key, value);
        }
        processMap(contentlet, map);
    }
    
    protected void processMap(Contentlet contentlet, Map<String,Object> map) {
        String stInode=(String)map.get(Contentlet.STRUCTURE_INODE_KEY);
        if(UtilMethods.isSet(stInode)) {
            Structure st=StructureCache.getStructureByInode(stInode);
            if(st!=null && InodeUtils.isSet(st.getInode())) {
                // basic data
                contentlet.setStructureInode(st.getInode());
                if(map.containsKey("languageId"))
                    contentlet.setLanguageId(Long.parseLong((String)map.get("languageId")));
                
                // build a field map for easy lookup
                Map<String,Field> fieldMap=new HashMap<String,Field>();
                for(Field ff : FieldsCache.getFieldsByStructureInode(stInode))
                    fieldMap.put(ff.getVelocityVarName(), ff);
                
                for(Map.Entry<String,Object> entry : map.entrySet()) {
                    String key=entry.getKey();
                    Object value=entry.getValue();
                    Field ff=fieldMap.get(key);
                    if(ff!=null) {
                        if(ff.getFieldType().equals(FieldType.HOST_OR_FOLDER.toString())) {
                            // it can be hostId, folderId, hostname, hostname:/folder/path
                            try {
                                User sysuser=APILocator.getUserAPI().getSystemUser();
                                Host hh=APILocator.getHostAPI().find(value.toString(), sysuser, false);
                                if(hh!=null && InodeUtils.isSet(hh.getIdentifier())) {
                                    contentlet.setHost(hh.getIdentifier());
                                }
                                else {
                                    Folder folder=null;
                                    try {
                                        folder=APILocator.getFolderAPI().find(value.toString(), sysuser, false);
                                    } catch(Exception ex) {}
                                    if(folder!=null && InodeUtils.isSet(folder.getInode())) {
                                        contentlet.setFolder(folder.getInode());
                                        contentlet.setHost(folder.getHostId());
                                    }
                                    else {
                                        if(value.toString().contains(":")) {
                                            String[] split=value.toString().split(":");
                                            hh=APILocator.getHostAPI().findByName(split[0],sysuser, false);
                                            if(hh!=null && InodeUtils.isSet(hh.getIdentifier())) {
                                                folder=APILocator.getFolderAPI().findFolderByPath(split[1], hh, sysuser, false);
                                                if(folder!=null && InodeUtils.isSet(folder.getInode())) {
                                                    contentlet.setHost(hh.getIdentifier());
                                                    contentlet.setFolder(folder.getInode());
                                                }
                                            }
                                        }
                                        else {
                                            hh=APILocator.getHostAPI().findByName(value.toString(), sysuser, false);
                                            if(hh!=null && InodeUtils.isSet(hh.getIdentifier())) {
                                                contentlet.setHost(hh.getIdentifier());
                                            }
                                        }
                                    }
                                }
                            }
                            catch(Exception ex) {
                                // just pass
                            }
                        }
                        else if(ff.getFieldType().equals(FieldType.CATEGORY.toString())) {
                            contentlet.setStringProperty(ff.getVelocityVarName(), value.toString());
                        }
                        else {
                            APILocator.getContentletAPI().setContentletProperty(contentlet, ff, value);
                        }
                    }
                }
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    protected void processJSON(Contentlet contentlet, InputStream input) throws JSONException, IOException {
        HashMap<String,Object> map=new HashMap<String,Object>();
        JSONObject obj=new JSONObject(IOUtils.toString(input));
        Iterator<String> keys = obj.keys();
        while(keys.hasNext()) {
            String key=keys.next();
            Object value=obj.get(key);
            map.put(key, value.toString());
        }
        processMap(contentlet,map);
    }   
}