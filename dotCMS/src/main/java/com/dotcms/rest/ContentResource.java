package com.dotcms.rest;

import com.dotcms.repackage.com.thoughtworks.xstream.XStream;
import com.dotcms.repackage.com.thoughtworks.xstream.converters.Converter;
import com.dotcms.repackage.com.thoughtworks.xstream.converters.MarshallingContext;
import com.dotcms.repackage.com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.dotcms.repackage.com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.dotcms.repackage.com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.dotcms.repackage.com.thoughtworks.xstream.io.xml.DomDriver;
import com.dotcms.repackage.javax.ws.rs.Consumes;
import com.dotcms.repackage.javax.ws.rs.GET;
import com.dotcms.repackage.javax.ws.rs.POST;
import com.dotcms.repackage.javax.ws.rs.PUT;
import com.dotcms.repackage.javax.ws.rs.Path;
import com.dotcms.repackage.javax.ws.rs.PathParam;
import com.dotcms.repackage.javax.ws.rs.Produces;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.javax.ws.rs.core.Response.Status;
import com.dotcms.repackage.org.apache.commons.httpclient.HttpStatus;
import com.dotcms.repackage.org.apache.commons.io.FileUtils;
import com.dotcms.repackage.org.apache.commons.io.IOUtils;
import com.dotcms.repackage.org.apache.commons.net.io.Util;
import com.dotcms.repackage.org.codehaus.jettison.json.JSONArray;
import com.dotcms.repackage.org.codehaus.jettison.json.JSONException;
import com.dotcms.repackage.org.codehaus.jettison.json.JSONObject;
import com.dotcms.repackage.org.glassfish.jersey.media.multipart.BodyPart;
import com.dotcms.repackage.org.glassfish.jersey.media.multipart.ContentDisposition;
import com.dotcms.repackage.org.glassfish.jersey.media.multipart.FormDataMultiPart;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.business.DotLockException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.contentlet.util.ContentletUtil;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.structure.factories.RelationshipFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Field.FieldType;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.viewtools.content.util.ContentUtils;
import com.liferay.portal.model.User;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

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
	 * @throws DotSecurityException
	 * @throws DotDataException
	 * @throws JSONException
	 */
	@GET
	@Path("/indexsearch/{query}/sortby/{sortby}/limit/{limit}/offset/{offset}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response indexSearch ( @Context HttpServletRequest request, @PathParam ("query") String query,
			@PathParam ("sortby") String sortBy, @PathParam ("limit") int limit,
			@PathParam ("offset") int offset,
			@PathParam ("type") String type,
			@PathParam ("callback") String callback) throws DotSecurityException, DotDataException, JSONException {

        InitDataObject initData = webResource.init(null, true, request, false, null);

		Map<String, String> paramsMap = new HashMap<String, String>();
		paramsMap.put( "type", type );
		paramsMap.put( "callback", callback );
		//Creating an utility response object
		ResourceResponse responseResource = new ResourceResponse( paramsMap );

		List<ContentletSearch> searchIndex = APILocator.getContentletAPI().searchIndex(query, limit, offset, sortBy, initData.getUser(), true);
		JSONArray array=new JSONArray();
		for(ContentletSearch cs : searchIndex) {
			array.put(new JSONObject()
			.put("inode", cs.getInode())
			.put("identifier", cs.getIdentifier()));
		}

		return responseResource.response( array.toString() );
	}

	/**
	 * Performs a call to APILocator.getContentletAPI().indexCount()
	 * using the specified parameters.
	 * <p/>
	 * Example call using curl:
	 * curl -XGET http://localhost:8080/api/content/indexcount/+structurename:webpagecontent
	 *
	 * @param request request obejct
	 * @param query   lucene query to count on
	 * @return a string with the count
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	@GET
	@Path ("/indexcount/{query}")
	@Produces (MediaType.TEXT_PLAIN)
	public Response indexCount ( @Context HttpServletRequest request, @PathParam ("query") String query,
			@PathParam ("type") String type,
			@PathParam ("callback") String callback ) throws DotDataException, DotSecurityException {

        InitDataObject initData = webResource.init(null, true, request, false, null);

		Map<String, String> paramsMap = new HashMap<String, String>();
		paramsMap.put( "type", type );
		paramsMap.put( "callback", callback );
		//Creating an utility response object
		ResourceResponse responseResource = new ResourceResponse( paramsMap );

		return responseResource.response( Long.toString( APILocator.getContentletAPI().indexCount( query, initData.getUser(), true ) ) );
	}
	

	@PUT
	@Path ("/lock/{params:.*}")
	@Produces (MediaType.APPLICATION_JSON)

	public Response lockContent(@Context HttpServletRequest request, @Context HttpServletResponse response, @PathParam("params") String params) throws DotDataException, DotSecurityException, JSONException {


        InitDataObject initData = webResource.init(params, true, request, false, null);
		Map<String, String> paramsMap = initData.getParamsMap();
		String callback = paramsMap.get(RESTParams.CALLBACK.getValue());
		String language = paramsMap.get(RESTParams.LANGUAGE.getValue());

		String id = paramsMap.get(RESTParams.ID.getValue());

		String inode = paramsMap.get(RESTParams.INODE.getValue());
		
		
		ResourceResponse responseResource = new ResourceResponse( paramsMap );
		JSONObject jo = new JSONObject();
		User user = initData.getUser();
		

		long lang = APILocator.getLanguageAPI().getDefaultLanguage().getId();
		boolean live = (paramsMap.get(RESTParams.LIVE.getValue()) == null || ! "false".equals(paramsMap.get(RESTParams.LIVE.getValue())));

		if(paramsMap.get(RESTParams.LANGUAGE.getValue()) != null){
			try{
				lang= Long.parseLong(language)	;
			}
			catch(Exception e){
				Logger.warn(this.getClass(), "Invald language passed in, defaulting to, well, the default");
			}
		}
		
		
		Contentlet contentlet = (inode!=null) 
				? APILocator.getContentletAPI().find(inode, user, live) 
						:APILocator.getContentletAPI().findContentletByIdentifier(id,live, lang,  user, live);
		if(contentlet==null || contentlet.getIdentifier()==null){
			jo.append("message", "contentlet not found");
			jo.append("return", 404);
			
	        Response.ResponseBuilder responseBuilder = Response.status( HttpStatus.SC_NOT_FOUND);
	        return  responseBuilder.entity(jo).build();
		}else{
			if(!UtilMethods.isSet(inode)){
				inode = contentlet.getInode();
			}
			if(!UtilMethods.isSet(id)){
				id = contentlet.getIdentifier();
			}	
					
			APILocator.getContentletAPI().lock(contentlet, user, live);
	
	
	
	
			if(UtilMethods.isSet(callback)){
				jo.put("callback", callback);
			}
			jo.put("inode", inode);
			jo.put("id", id);
			jo.put("message", "locked");
			jo.put("return", 200);
			//Creating an utility response object
		}
		
		return responseResource.response( jo.toString() );
	}
	

	@PUT
	@Path ("/canLock/{params:.*}")
	@Produces (MediaType.APPLICATION_JSON)
	public Response canLockContent(@Context HttpServletRequest request,  @PathParam("params") String params) throws DotDataException, DotSecurityException, JSONException {

        InitDataObject initData = webResource.init(params, true, request, false, null);
		Map<String, String> paramsMap = initData.getParamsMap();
		String callback = paramsMap.get(RESTParams.CALLBACK.getValue());
		String language = paramsMap.get(RESTParams.LANGUAGE.getValue());

		String id = paramsMap.get(RESTParams.ID.getValue());

		String inode = paramsMap.get(RESTParams.INODE.getValue());
		
		
		ResourceResponse responseResource = new ResourceResponse( paramsMap );
		JSONObject jo = new JSONObject();
		User user = initData.getUser();
		

		long lang = APILocator.getLanguageAPI().getDefaultLanguage().getId();
		boolean live = (paramsMap.get(RESTParams.LIVE.getValue()) == null || ! "false".equals(paramsMap.get(RESTParams.LIVE.getValue())));

		if(paramsMap.get(RESTParams.LANGUAGE.getValue()) != null){
			try{
				lang= Long.parseLong(language)	;
			}
			catch(Exception e){
				Logger.warn(this.getClass(), "Invald language passed in, defaulting to, well, the default");
			}
		}
		
		
		Contentlet contentlet = (inode!=null) 
				? APILocator.getContentletAPI().find(inode, user, live) 
						:APILocator.getContentletAPI().findContentletByIdentifier(id,live, lang,  user, live);
		if(contentlet==null || contentlet.getIdentifier()==null){
			jo.append("message", "contentlet not found");
			jo.append("return", 404);
			
	        Response.ResponseBuilder responseBuilder = Response.status( HttpStatus.SC_NOT_FOUND);
	        return  responseBuilder.entity(jo).build();
		}else{
			if(!UtilMethods.isSet(inode)){
				inode = contentlet.getInode();
			}
			if(!UtilMethods.isSet(id)){
				id = contentlet.getIdentifier();
			}	
					
			boolean canLock = false;
            try{
            	canLock = APILocator.getContentletAPI().canLock(contentlet, user);
            }
            catch(DotLockException e){
            	canLock=false;
            }
            jo.put("canLock", canLock);
            jo.put("locked", contentlet.isLocked());
            ContentletVersionInfo cvi = APILocator.getVersionableAPI().getContentletVersionInfo(id, contentlet.getLanguageId());
            if(contentlet.isLocked()){
            	jo.put("lockedBy", cvi.getLockedBy());
            	jo.put("lockedOn", cvi.getLockedOn());
            	jo.put("lockedByName", APILocator.getUserAPI().loadUserById(cvi.getLockedBy()));
            	
            	
            }
	
			if(UtilMethods.isSet(callback)){
				jo.put("callback", callback);
			}
			jo.put("inode", inode);
			jo.put("id", id);
			jo.put("return", 200);
			//Creating an utility response object
		}
		
		return responseResource.response( jo.toString() );
	}
	
	@PUT
	@Path ("/unlock/{params:.*}")
	@Produces (MediaType.APPLICATION_JSON)

	public Response unlockContent(@Context HttpServletRequest request, @Context HttpServletResponse response, @PathParam("params") String params) throws DotDataException, DotSecurityException, JSONException {

        InitDataObject initData = webResource.init(params, true, request, false, null);
		Map<String, String> paramsMap = initData.getParamsMap();
		String callback = paramsMap.get(RESTParams.CALLBACK.getValue());
		String language = paramsMap.get(RESTParams.LANGUAGE.getValue());
		String id = paramsMap.get(RESTParams.ID.getValue());
		String inode = paramsMap.get(RESTParams.INODE.getValue());
		
		
		ResourceResponse responseResource = new ResourceResponse( paramsMap );
		JSONObject jo = new JSONObject();
		User user = initData.getUser();
		

		long lang = APILocator.getLanguageAPI().getDefaultLanguage().getId();
		boolean live = (paramsMap.get(RESTParams.LIVE.getValue()) == null || ! "false".equals(paramsMap.get(RESTParams.LIVE.getValue())));

		if(paramsMap.get(RESTParams.LANGUAGE.getValue()) != null){
			try{
				lang= Long.parseLong(language)	;
			}
			catch(Exception e){
				Logger.warn(this.getClass(), "Invald language passed in, defaulting to, well, the default");
			}
		}
		
		
		Contentlet contentlet = (inode!=null) 
				? APILocator.getContentletAPI().find(inode, user, live) 
						:APILocator.getContentletAPI().findContentletByIdentifier(id,live, lang,  user, live);
		if(contentlet==null || contentlet.getIdentifier()==null){
			jo.append("message", "contentlet not found");
			jo.append("return", 404);
			
			
		}else{
			if(!UtilMethods.isSet(inode)){
				inode = contentlet.getInode();
			}
			if(!UtilMethods.isSet(id)){
				id = contentlet.getIdentifier();
			}	
					
			APILocator.getContentletAPI().unlock(contentlet, user, live);
	
	
	
			
	
			if(UtilMethods.isSet(callback)){
				jo.put("callback", callback);
			}
			jo.put("inode", inode);
			jo.put("id", id);
			jo.put("message", "unlocked");
			jo.put("return", 200);
			//Creating an utility response object
		}
		
		return responseResource.response( jo.toString() );
	}
	
	
	
	
	@GET
	@Path("/{params:.*}")
	@Produces(MediaType.TEXT_PLAIN)
	public Response getContent(@Context HttpServletRequest request, @Context HttpServletResponse response, @PathParam("params") String params) {

        InitDataObject initData = webResource.init(params, true, request, false, null);
		//Creating an utility response object
		ResourceResponse responseResource = new ResourceResponse( initData.getParamsMap() );

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
				result = getXML(cons, request, response, render, user);
			} else {
				result = getJSON(cons, request, response, render, user);
			}
		} catch (Exception e) {
			Logger.warn(this, "Error converting result to XML/JSON");
		}

		return responseResource.response( result );
	}


	private String getXML(List<Contentlet> cons, HttpServletRequest request, HttpServletResponse response, String render, User user) throws DotDataException, IOException {
		XStream xstream = new XStream(new DomDriver());
		xstream.alias("content", Map.class);
		xstream.registerConverter(new MapEntryConverter());
		StringBuilder sb = new StringBuilder();
		sb.append("<?xml version=\"1.0\" encoding='UTF-8'?>");
		sb.append("<contentlets>");

		for(Contentlet c : cons){
			Map<String, Object> m = new HashMap<>();
			Structure s = c.getStructure();

			m.putAll(ContentletUtil.getContentPrintableMap(user, c));

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



	private String getXMLContentIds(Contentlet con) throws DotDataException, IOException {
		XStream xstream = new XStream(new DomDriver());
		xstream.alias("content", Map.class);
		xstream.registerConverter(new MapEntryConverter());
		StringBuilder sb = new StringBuilder();
		sb.append("<?xml version=\"1.0\" encoding='UTF-8'?>");
		sb.append("<contentlet>");
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("inode",con.getInode());
		m.put("identifier",con.getIdentifier());
		sb.append(xstream.toXML(m));
		sb.append("</contentlet>");
		return sb.toString();
	}
	
	private String getJSONContentIds(Contentlet con) throws IOException{
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

	private String getJSON(List<Contentlet> cons, HttpServletRequest request, HttpServletResponse response, String render, User user) throws IOException, DotDataException{
		JSONObject json = new JSONObject();
		JSONArray jsonCons = new JSONArray();

		for(Contentlet c : cons){
			try {
				jsonCons.put(contentletToJSON(c, request, response, render, user));
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

	public static Set<String> getJSONFields(Structure s) {
		Set<String> jsonFields=new HashSet<String>();
		for(Field f : FieldsCache.getFieldsByStructureInode(s.getInode()))
			if(f.getFieldType().equals(Field.FieldType.KEY_VALUE.toString()))
				jsonFields.add(f.getVelocityVarName());
		return jsonFields;
	}

	public static JSONObject contentletToJSON(Contentlet con, HttpServletRequest request, HttpServletResponse response, String render, User user) throws JSONException, IOException, DotDataException{
		JSONObject jo = new JSONObject();
		Structure s = con.getStructure();
		Map<String,Object> map = ContentletUtil.getContentPrintableMap(user, con);

		Set<String> jsonFields=getJSONFields(s);

		for(String key : map.keySet()) {
			if(Arrays.binarySearch(ignoreFields, key) < 0)
				if(jsonFields.contains(key)) {
					Logger.info(ContentResource.class, key+" is a json field: "+map.get(key).toString());
					jo.put(key, new JSONObject(con.getKeyValueProperty(key)));
				}
				else {
					jo.put(key, map.get(key));
				}
		}

		if(s.getStructureType() == Structure.STRUCTURE_TYPE_WIDGET && "true".equals(render)) {
			jo.put("parsedCode",  WidgetResource.parseWidget(request, response, con));
		}

		return jo;
	}

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
		return multipartPUTandPOST(request, response, multipart, params, "PUT");
	}
	
	@POST
	@Path("/{params:.*}")
	@Produces(MediaType.TEXT_PLAIN)
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response multipartPOST(@Context HttpServletRequest request, @Context HttpServletResponse response,
			FormDataMultiPart multipart,@PathParam("params") String params) throws URISyntaxException {
		return multipartPUTandPOST(request, response, multipart, params, "POST");
	}
	
	private Response multipartPUTandPOST(HttpServletRequest request, HttpServletResponse response,
			FormDataMultiPart multipart, String params, String method) throws URISyntaxException{

        InitDataObject init= webResource.init(params, true, request, false, null);
		User user=init.getUser();
		Contentlet contentlet=new Contentlet();
		setRequestMetadata(contentlet, request);
		
		Map<String, Object> map = new HashMap<String, Object>();
		List<String> usedBinaryFields = new ArrayList<String>();
		for(BodyPart part : multipart.getBodyParts()) {
			ContentDisposition cd=part.getContentDisposition();
			String name=cd!=null && cd.getParameters().containsKey("name") ? cd.getParameters().get("name") : "";

			if(part.getMediaType().equals(MediaType.APPLICATION_JSON_TYPE) || name.equals("json")) {
				try {
					processJSON(contentlet,part.getEntityAs(InputStream.class));
				} catch (JSONException e) {

					Logger.error( this.getClass(), "Error processing JSON for Stream", e );

					Response.ResponseBuilder responseBuilder = Response.status( HttpStatus.SC_BAD_REQUEST );
					responseBuilder.entity( e.getMessage() );
					return responseBuilder.build();
				} catch (IOException e) {

					Logger.error( this.getClass(), "Error processing Stream", e );

					Response.ResponseBuilder responseBuilder = Response.status( HttpStatus.SC_INTERNAL_SERVER_ERROR );
					responseBuilder.entity( e.getMessage() );
					return responseBuilder.build();
				}
			}
			else if(part.getMediaType().equals(MediaType.APPLICATION_XML_TYPE) || name.equals("xml")) {
				try {
					processXML(contentlet, part.getEntityAs(InputStream.class));
				} catch (Exception e) {
					if(e instanceof DotSecurityException){
						SecurityLogger.logInfo(this.getClass(), "Invalid XML POSTED to ContentTypeResource from " + request.getRemoteAddr());
					}
					Logger.error( this.getClass(), "Error processing Stream", e );

					Response.ResponseBuilder responseBuilder = Response.status( HttpStatus.SC_INTERNAL_SERVER_ERROR );
					responseBuilder.entity( e.getMessage() );
					return responseBuilder.build();
				}
			}
			else if(part.getMediaType().equals(MediaType.APPLICATION_FORM_URLENCODED_TYPE) || name.equals("urlencoded")) {
				try {
					processForm(contentlet, part.getEntityAs(InputStream.class));
				} catch (Exception e) {
					Logger.error( this.getClass(), "Error processing Stream", e );

					Response.ResponseBuilder responseBuilder = Response.status( HttpStatus.SC_INTERNAL_SERVER_ERROR );
					responseBuilder.entity( e.getMessage() );
					return responseBuilder.build();
				}
			}
			else if(part.getMediaType().equals(MediaType.TEXT_PLAIN_TYPE)) {
				try {
					map.put(name, part.getEntityAs(String.class));
					processMap( contentlet, map );
				} catch (Exception e) {
					Logger.error( this.getClass(), "Error processing Plain Tex", e );

					Response.ResponseBuilder responseBuilder = Response.status( HttpStatus.SC_INTERNAL_SERVER_ERROR );
					responseBuilder.entity( e.getMessage() );
					return responseBuilder.build();
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
						String fieldName = ff.getFieldContentlet();
						if (fieldName.startsWith("binary")
								&& !usedBinaryFields.contains(fieldName)) {
							contentlet.setBinary(ff.getVelocityVarName(), tmp);
							usedBinaryFields.add(fieldName);
							break;
						}
					}
				} catch (IOException e) {

					Logger.error( this.getClass(), "Error processing Stream", e );

					Response.ResponseBuilder responseBuilder = Response.status( HttpStatus.SC_INTERNAL_SERVER_ERROR );
					responseBuilder.entity( e.getMessage() );
					return responseBuilder.build();
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
		return singlePUTandPOST(request, response, params, "PUT");
	}
	
	@POST
	@Path("/{params:.*}")
	@Produces(MediaType.TEXT_PLAIN)
	@Consumes({MediaType.APPLICATION_JSON,MediaType.APPLICATION_FORM_URLENCODED,MediaType.APPLICATION_XML})
	public Response singlePOST(@Context HttpServletRequest request, @Context HttpServletResponse response, @PathParam("params") String params) throws URISyntaxException {
		return singlePUTandPOST(request, response, params, "POST");
	}
	
	private Response singlePUTandPOST(HttpServletRequest request, HttpServletResponse response, String params, String method) throws URISyntaxException {
        InitDataObject init= webResource.init(params, true, request, false, null);

		Contentlet contentlet=new Contentlet();
		setRequestMetadata(contentlet, request);
		
		try {
			if(request.getContentType().startsWith(MediaType.APPLICATION_JSON)) {
				processJSON(contentlet, request.getInputStream());
			}
			else if(request.getContentType().startsWith(MediaType.APPLICATION_XML)) {
				try{
					processXML(contentlet, request.getInputStream());
				}
				catch(DotSecurityException se){
					SecurityLogger.logInfo(this.getClass(), "Invalid XML POSTED to ContentTypeResource from " + request.getRemoteAddr());
					throw new DotSecurityException("");
				}
			}
			else if(request.getContentType().startsWith(MediaType.APPLICATION_FORM_URLENCODED)) {
				if(method.equals("PUT")){
					processForm(contentlet, request.getInputStream());
				}
				else if(method.equals("POST")){
					processFormPost(contentlet, request, false);
				}
				
			}
		} catch ( JSONException e ) {

			Logger.error( this.getClass(), "Error processing JSON for Stream", e );

			Response.ResponseBuilder responseBuilder = Response.status( HttpStatus.SC_BAD_REQUEST );
			responseBuilder.entity( e.getMessage() );
			return responseBuilder.build();
		} catch ( Exception e ) {

			Logger.error( this.getClass(), "Error processing Stream", e );

			Response.ResponseBuilder responseBuilder = Response.status( HttpStatus.SC_INTERNAL_SERVER_ERROR );
			responseBuilder.entity( e.getMessage() );
			return responseBuilder.build();
		}

		return saveContent(contentlet,init);
	}

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

			Map<Relationship,List<Contentlet>> relationships=(Map<Relationship,List<Contentlet>>)contentlet.get(RELATIONSHIP_KEY);

			HibernateUtil.startTransaction();

			boolean allowFrontEndSaving = Config.getBooleanProperty("REST_API_CONTENT_ALLOW_FRONT_END_SAVING", false);

			contentlet = APILocator.getContentletAPI().checkin(contentlet,relationships,cats,new ArrayList<Permission>(),init.getUser(),allowFrontEndSaving);

			if(live)
				APILocator.getContentletAPI().publish(contentlet, init.getUser(), allowFrontEndSaving);

			HibernateUtil.commitTransaction();
			clean = true;
		} catch ( DotContentletStateException e ) {

			Logger.error( this.getClass(), "Error saving Contentlet" + e );

			Response.ResponseBuilder responseBuilder = Response.status( HttpStatus.SC_CONFLICT );
			responseBuilder.entity( e.getMessage() );
			return responseBuilder.build();
		} catch ( IllegalArgumentException e ) {

			Logger.error( this.getClass(), "Error saving Contentlet" + e );

			Response.ResponseBuilder responseBuilder = Response.status( HttpStatus.SC_CONFLICT );
			responseBuilder.entity( e.getMessage() );
			return responseBuilder.build();
		} catch ( DotSecurityException e ) {

			Logger.error( this.getClass(), "Error saving Contentlet" + e );

			Response.ResponseBuilder responseBuilder = Response.status( HttpStatus.SC_FORBIDDEN );
			responseBuilder.entity( e.getMessage() );
			return responseBuilder.build();
		} catch ( Exception e ) {
			Logger.warn( this, e.getMessage(), e );
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

		if(init.getParamsMap().containsKey("type") || init.getParamsMap().containsKey("callback")){
			if(init.getParamsMap().containsKey("callback") && !init.getParamsMap().containsKey("type")){
				Map<String,String> map = init.getParamsMap();
				map.put("type", "jsonp");
				init.setParamsMap(map);
			}

			String type = init.getParamsMap().get(RESTParams.TYPE.getValue());
			String result ="";
			try {
				if("xml".equals(type)) {
					
					result = getXMLContentIds(contentlet);
					return Response.ok(result,MediaType.APPLICATION_XML)
							.location(new URI("content/inode/"+contentlet.getInode()+"/type/xml"))
							.header("inode", contentlet.getInode())
							.header("identifier", contentlet.getIdentifier())
							.status(Status.OK).build();
				} else if("text".equals(type)){
					
					return Response.ok("inode:"+contentlet.getInode()+",identifier:"+contentlet.getIdentifier(),MediaType.TEXT_PLAIN)
							.location(new URI("content/inode/"+contentlet.getInode()+"/type/text"))
							.header("inode", contentlet.getInode())
							.header("identifier", contentlet.getIdentifier())
							.status(Status.OK).build();
				}else {

					result = getJSONContentIds(contentlet);

					if(type.equals("jsonp")){

						String callback = init.getParamsMap().get(RESTParams.CALLBACK.getValue());
						return Response.ok(callback+"("+result+")","application/javascript")
								.location(new URI("content/inode/"+contentlet.getInode()+"/type/jsonp/callback/"+callback))
								.header("inode", contentlet.getInode())
								.header("identifier", contentlet.getIdentifier())
								.status(Status.OK).build();
					}else{

						return Response.ok(result,MediaType.APPLICATION_JSON)
								.location(new URI("content/inode/"+contentlet.getInode()+"/type/json"))
								.header("inode", contentlet.getInode())
								.header("identifier", contentlet.getIdentifier())
								.status(Status.OK).build();
					}
				}
			} catch (Exception e) {
				Logger.warn(this, "Error converting result to XML/JSON");
				return Response.serverError().build();
			}
		}else {
			return Response.seeOther(new URI("content/inode/"+contentlet.getInode()))
					.header("inode", contentlet.getInode())
					.header("identifier", contentlet.getIdentifier())
					.status(Status.OK).build();
		}
	}

	@SuppressWarnings("unchecked")
	protected void processXML(Contentlet contentlet, InputStream inputStream) throws IOException, DotSecurityException {
		
		
		String input = IOUtils.toString(inputStream, "UTF-8");
		// deal with XXE or SSRF security vunerabilities in XML docs
		// besides, we do not expect a fully formed xml doc - only an xml doc that can be transformed into a java.util.Map
		// Mingle Card 512
		String upper = input.trim().toUpperCase();
		if(upper.contains("<!DOCTYPE") || upper.contains("<!ENTITY") || upper.startsWith("<?XML")){
			throw new DotSecurityException("Invalid XML");
		}
		XStream xstream=new XStream(new DomDriver());
		xstream.alias("content", Map.class);
		xstream.registerConverter(new MapEntryConverter());
		Map<String,Object> root=(Map<String,Object>) xstream.fromXML(input);
		processMap(contentlet,root);
	}

	protected void processForm ( Contentlet contentlet, InputStream input ) throws Exception {

		Map<String, Object> map = new HashMap<String, Object>();
		for ( String param : IOUtils.toString( input ).split( "&" ) ) {

			int index = param.indexOf( "=" );

			//Verify if we have a value
			if ( index != -1 ) {
				String key = URLDecoder.decode( param.substring( 0, index ), "UTF-8" );
				String value = URLDecoder.decode( param.substring( index + 1, param.length() ), "UTF-8" );
				map.put( key, value );
			}
		}
		processMap( contentlet, map );
	}
	
	protected void processFormPost ( Contentlet contentlet, HttpServletRequest request, boolean multiPart) throws Exception {

		Map<String, Object> map = new HashMap<String, Object>();
		
		if(multiPart){
			ArrayList<Part> partList = new ArrayList<Part>(request.getParts());
			
			for(Part part : partList){
				String partName = part.getName();
				String partValue = part.getHeader(partName);
				map.put( partName, partValue );
			}
			
		}else{
			Enumeration<String> parameterNames = request.getParameterNames();

			while (parameterNames.hasMoreElements()) {
				String paramName = parameterNames.nextElement();
				String paramValue = request.getParameter(paramName);
				map.put( paramName, paramValue );
			}	
		}
		
		processMap( contentlet, map );
	}

	protected void processMap(Contentlet contentlet, Map<String,Object> map) {
		String stInode=(String)map.get(Contentlet.STRUCTURE_INODE_KEY);
		if(!UtilMethods.isSet(stInode)) {
			String stName=(String)map.get("stName");
			if(UtilMethods.isSet(stName)) {
				stInode = CacheLocator.getContentTypeCache().getStructureByVelocityVarName(stName).getInode();
			}
		}
		if(UtilMethods.isSet(stInode)) {
			Structure st=CacheLocator.getContentTypeCache().getStructureByInode(stInode);
			if(st!=null && InodeUtils.isSet(st.getInode())) {
				// basic data
				contentlet.setStructureInode(st.getInode());
				if(map.containsKey("languageId")) {
					contentlet.setLanguageId(Long.parseLong((String)map.get("languageId")));
				}
				else {
					contentlet.setLanguageId(APILocator.getLanguageAPI().getDefaultLanguage().getId());
				}

				// check for existing identifier
				if(map.containsKey("identifier")) {
					contentlet.setIdentifier(String.valueOf(map.get("identifier")));
					try {
						Contentlet existing=APILocator.getContentletAPI().findContentletByIdentifier((String)map.get("identifier"), false,
								contentlet.getLanguageId(), APILocator.getUserAPI().getSystemUser(), false);
						APILocator.getContentletAPI().copyProperties(contentlet, existing.getMap());
						contentlet.setInode("");
					} catch (Exception e) {
						Logger.debug(this.getClass(), "can't get existing content for ident "+map.get("identifier")+" lang "+contentlet.getLanguageId() + " - creating new one");
					}
				}

				// build a field map for easy lookup
				Map<String,Field> fieldMap=new HashMap<String,Field>();
				for(Field ff : FieldsCache.getFieldsByStructureInode(stInode))
					fieldMap.put(ff.getVelocityVarName(), ff);

				// look for relationships
				Map<Relationship,List<Contentlet>> relationships=new HashMap<Relationship,List<Contentlet>>();
				for(Relationship rel : RelationshipFactory.getAllRelationshipsByStructure(st)) {
					String relname=rel.getRelationTypeValue();
					String query=(String)map.get(relname);
					if(UtilMethods.isSet(query)) {
						try {
							List<Contentlet> cons=APILocator.getContentletAPI().search(
									query, 0, 0, null, APILocator.getUserAPI().getSystemUser(), false);
							if(cons.size()>0) {
								relationships.put(rel, cons);
							}
							Logger.info(this, "got "+cons.size()+" related contents");
						} catch (Exception e) {
							Logger.warn(this, e.getMessage(), e);
						}
					}
				}
				contentlet.setProperty(RELATIONSHIP_KEY, relationships);


				// fill fields
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
													if(st.getStructureType()==Structure.Type.FILEASSET.getType()){
														Identifier existingIdent = APILocator.getIdentifierAPI().find(hh,split[1]);
														if(existingIdent != null && UtilMethods.isSet(existingIdent.getId()) && UtilMethods.isSet(contentlet.getIdentifier())){
															contentlet.setIdentifier(existingIdent.getId());
														}
													}
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
						else if((ff.getFieldType().equals(FieldType.FILE.toString()) || ff.getFieldType().equals(FieldType.IMAGE.toString())) &&
								value.toString().startsWith("//")) {
							boolean found=false;
							try {
								String str=value.toString().substring(2);
								String hostname=str.substring(0,str.indexOf('/'));
								String uri=str.substring(str.indexOf('/'));
								Host host=APILocator.getHostAPI().findByName(hostname, APILocator.getUserAPI().getSystemUser(), false);
								if(host!=null && InodeUtils.isSet(host.getIdentifier())) {
									Identifier ident=APILocator.getIdentifierAPI().find(host, uri);
									if(ident!=null && InodeUtils.isSet(ident.getId())) {
										contentlet.setStringProperty(ff.getVelocityVarName(), ident.getId());
										found=true;
									}
								}
								if(!found) {
									throw new Exception("asset "+value+" not found");
								}

							}
							catch(Exception ex) {
								throw new RuntimeException(ex);
							}
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
		processMap(contentlet, webResource.processJSON(input));
	}
	
	private void setRequestMetadata(Contentlet contentlet, HttpServletRequest request) {
		try{
			contentlet.setStringProperty(HOST_HEADER, request.getHeader("Host"));
		}
		catch(Exception e){
			Logger.error(this.getClass(), "Cannot set HOST_HEADER" + HOST_HEADER + e);
			
		}
		
		try{
            //request.getCookies() could return null.
            if(UtilMethods.isSet(request.getCookies())){
                contentlet.setStringProperty(COOKIES, request.getCookies().toString());
            } else {
                //There are some cases where the cookies are not in the request, for example using curl without -b or --cookie.
                Logger.warn(this.getClass(), "COOKIES are not in the REQUEST");
            }
		}
		catch(Exception e){
			Logger.error(this.getClass(), "Cannot set COOKIES " + e);
			
		}
		try{
			contentlet.setStringProperty(REQUEST_METHOD, request.getMethod());
		}
		catch(Exception e){
			Logger.error(this.getClass(), "Cannot set REQUEST_METHOD" + e);
			
		}
		try{
			contentlet.setStringProperty(REFERER, request.getHeader("Referer"));
		}
		catch(Exception e){
			Logger.error(this.getClass(), "Cannot set REFERER" + e);
			
		}
		try{
			contentlet.setStringProperty(USER_AGENT, request.getHeader("User-Agent"));
		}
		catch(Exception e){
			Logger.error(this.getClass(), "Cannot set USER_AGENT" + e);
			
		}
		try{
			contentlet.setStringProperty(IP_ADDRESS, request.getRemoteHost());
		}
		catch(Exception e){
			Logger.error(this.getClass(), "Cannot set IP_ADDRESS" + e);
			
		}
		
		try{
			contentlet.setStringProperty(ACCEPT_LANGUAGE, request.getHeader("Accept-Language"));
		}
		catch(Exception e){
			Logger.error(this.getClass(), "Cannot set ACCEPT_LANGUAGE" + e);
			
		}
	}
}