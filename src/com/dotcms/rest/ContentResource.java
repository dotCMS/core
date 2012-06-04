package com.dotcms.rest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

@Path("/content")
public class ContentResource extends WebResource {

	@GET
	@Path("/{path:.*}")
	@Produces(MediaType.TEXT_PLAIN)
	public String getContent(@Context HttpServletRequest request, @Context HttpServletResponse response, @PathParam("path") String path) throws DotDataException, DotSecurityException, IOException {

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
		String result = null;
		User user = null;
		type = UtilMethods.isSet(type)?type:"json";
		orderBy = UtilMethods.isSet(orderBy)?orderBy:"modDate desc";

		/* Authenticate the User if passed */

		user = authenticateUser(username, password);

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

		/* Fetching the content using a query if passed or an id */

		List<Contentlet> cons = new ArrayList<Contentlet>();

		if(UtilMethods.isSet(id)) {
			cons.add(APILocator.getContentletAPI().find(id, user, true));
		} else if(UtilMethods.isSet(query)) {
			cons = APILocator.getContentletAPI().search(query,new Integer(limit),new Integer(offset),orderBy,user,true);
		}

		/* Converting the Contentlet list to XML or JSON */

		if("xml".equals(type)) {
			result = getXML(cons, request, response, render);
		} else {
			result = getJSON(cons, request, response, render);
		}

		return result;
	}


	@SuppressWarnings({ "unchecked", "deprecation", "rawtypes" })
	private String getXML(List<Contentlet> cons, HttpServletRequest request, HttpServletResponse response, String render) throws DotDataException, IOException {
		XStream xstream = new XStream(new DomDriver());
		xstream.alias("content", Map.class);
		StringBuilder sb = new StringBuilder();
		sb.append("<?xml version=\"1.0\" encoding='UTF-8'?>");
		sb.append("<contentlets>");

		for(Contentlet c : cons){
			Map m = c.getMap();
			Structure s = c.getStructure();

			for(Field f : s.getFields()){
				if(f.getFieldType().equals(Field.FieldType.BINARY.toString())){
					m.put(f.getVelocityVarName(), "/contentAsset/raw-data/" +  c.getIdentifier() + "/" + f.getVelocityVarName()	);
					m.put(f.getVelocityVarName() + "ContentAsset", c.getIdentifier() + "/" +f.getVelocityVarName()	);
				}
			}

			if(s.getStructureType() == Structure.STRUCTURE_TYPE_WIDGET && "true".equals(render)) {
				m.put("parsedCode",  WidgetResource.parseWidget(request, response, c));
			}

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
				Logger.error(this.getClass(), "unable JSON contentlet " + c.getIdentifier());
				Logger.debug(this.getClass(), "unable to find contentlet", e);
			}
		}

		try {
			json.put("contentlets", jsonCons);
		} catch (JSONException e) {
			Logger.error(this.getClass(), "unable to create JSONObject");
			Logger.debug(this.getClass(), "unable to create JSONObject", e);
		}

		return json.toString();
	}

	@SuppressWarnings({ "rawtypes", "deprecation" })
	private JSONObject contentletToJSON(Contentlet con, HttpServletRequest request, HttpServletResponse response, String render) throws JSONException, IOException{
		JSONObject jo = new JSONObject();
		Structure s = con.getStructure();
		Map map = con.getMap();

		for (Iterator it = map.keySet().iterator(); it.hasNext(); ) {
			String key = (String) it.next();
			if(Arrays.binarySearch(ignoreFields, key) < 0){
				jo.put(key, map.get(key));
			}
		}

		for(Field f : s.getFields()){
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
}