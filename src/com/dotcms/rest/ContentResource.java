package com.dotcms.rest;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
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
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.lucene.queryParser.ParseException;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
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
	public Response saveContent(@Context HttpServletRequest request, @Context HttpServletResponse response, 
			FormDataMultiPart multipart,@PathParam("params") String params) {
		InitDataObject init=init(params,true,request,true);
		User user=init.getUser();
		
		Contentlet contentlet=new Contentlet();
		for(BodyPart part : multipart.getBodyParts()) {
			if(part.getMediaType().equals(MediaType.APPLICATION_JSON)) {
				
			}
			else if(part.getMediaType().equals(MediaType.APPLICATION_XML)) {
				
			}
			else if(part.getMediaType().equals(MediaType.APPLICATION_FORM_URLENCODED)) {
				
			}
			else if(part.getContentDisposition().getType().equals("attachment")) {
				
			}
		}
		return null;
	}
}