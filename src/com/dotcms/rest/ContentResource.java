package com.dotcms.rest;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.HashSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.xml.DomDriver;

@Path("/content")
public class ContentResource extends WebResource {

	@GET
	@Path("/{path:.*}")
	@Produces(MediaType.TEXT_PLAIN)
	public String getContent(@Context HttpServletRequest request, @Context HttpServletResponse response, @PathParam("path") String path) {

		/* Getting values from the URL  */

		Map<String, String> params = parsePath(path);
		String render = params.get(RENDER);
		String type = params.get(TYPE);
		String query = params.get(QUERY);
		String id = params.get(ID);
		String orderBy = params.get(ORDERBY);
		String limitStr = params.get(LIMIT);
		String offsetStr = params.get(OFFSET);
		String username = params.get(USER);
		String password = params.get(PASSWORD);
		String inode = params.get(INODE);
		String result = null;
		User user = null;
		type = UtilMethods.isSet(type)?type:"json";
		orderBy = UtilMethods.isSet(orderBy)?orderBy:"modDate desc";
		long language = APILocator.getLanguageAPI().getDefaultLanguage().getId();

		if(params.get(LANGUAGE) != null){
			try{
				language= Long.parseLong(params.get(LANGUAGE))	;
			}
			catch(Exception e){
				Logger.warn(this.getClass(), "Invald language passed in, defaulting to, well, the default");
			}
		}

		/* Authenticate the User if passed */

		try {
			user = authenticateUser(username, password);
		} catch (Exception e) {
			Logger.warn(this, "Error authenticating user, username: " + username + ", password: " + password);
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

		boolean live = (params.get(LIVE) == null || ! "false".equals(params.get(LIVE)));

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
					cons = APILocator.getContentletAPI().search(query,new Integer(limit),new Integer(offset),orderBy,user,true);
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
}