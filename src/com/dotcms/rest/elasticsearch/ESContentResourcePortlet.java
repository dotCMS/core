package com.dotcms.rest.elasticsearch;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.dotcms.content.elasticsearch.business.ESSearchResults;
import com.dotcms.repackage.javax.ws.rs.GET;
import com.dotcms.repackage.javax.ws.rs.POST;
import com.dotcms.repackage.javax.ws.rs.Path;
import com.dotcms.repackage.javax.ws.rs.Produces;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.apache.commons.io.IOUtils;
import com.dotcms.repackage.org.codehaus.jackson.map.ObjectMapper;
import com.dotcms.repackage.org.codehaus.jettison.json.JSONArray;
import com.dotcms.repackage.org.codehaus.jettison.json.JSONException;
import com.dotcms.repackage.org.codehaus.jettison.json.JSONObject;
import com.dotcms.rest.BaseRestPortlet;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResourceResponse;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

@Path("/es")
public class ESContentResourcePortlet extends BaseRestPortlet {

	ContentletAPI esapi = APILocator.getContentletAPI();

	@POST
	@Path("search")
	@Produces("application/json")
	public Response search(@Context HttpServletRequest request) {

		InitDataObject initData = init(null, true, request, false);

		HttpSession session = request.getSession();

		boolean live = true;
		boolean ADMIN_MODE = (session.getAttribute(com.dotmarketing.util.WebKeys.ADMIN_MODE_SESSION) != null);
		boolean PREVIEW_MODE = ((session.getAttribute(com.dotmarketing.util.WebKeys.PREVIEW_MODE_SESSION) != null) && ADMIN_MODE);
		boolean EDIT_MODE = ((session.getAttribute(com.dotmarketing.util.WebKeys.EDIT_MODE_SESSION) != null) && ADMIN_MODE);
		if (EDIT_MODE || PREVIEW_MODE) {
			live = false;
		}

		ResourceResponse responseResource = new ResourceResponse(initData.getParamsMap());

		User user = initData.getUser();

		ObjectMapper mapper = new ObjectMapper();
		try {
			
			
			
			
			
			String esQuery = IOUtils.toString(request.getInputStream());

			ESSearchResults esresult = esapi.esSearch(esQuery, live, user, live);
			
			JSONObject json = new JSONObject();
			JSONArray jsonCons = new JSONArray();

			for(Object x : esresult){
				Contentlet c = (Contentlet) x;
				try {
					jsonCons.put(contentletToJson(c));
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


			esresult.getContentlets().clear();
			

			

			
			
			json.append("esresponse", new JSONObject(esresult.getResponse().toString()));


			return responseResource.response( json.toString() );

		} catch (Exception e) {
			Logger.error(this.getClass(), "Error processing :" + e.getMessage(), e);
			return responseResource.responseError(e.getMessage());

		}

	}

	@GET
	@Path("search")
	@Produces("application/json")
	public Response searchGet(@Context HttpServletRequest request) {
		return search(request);

	}

	@GET
	@Path("raw")
	@Produces("application/json")
	public Response searchRawGet(@Context HttpServletRequest request) {
		return searchRaw(request);

	}

	@POST
	@Path("raw")
	@Produces("application/json")
	public Response searchRaw(@Context HttpServletRequest request) {

		InitDataObject initData = init(null, true, request, false);

		HttpSession session = request.getSession();

		boolean live = true;
		boolean ADMIN_MODE = (session.getAttribute(com.dotmarketing.util.WebKeys.ADMIN_MODE_SESSION) != null);
		boolean PREVIEW_MODE = ((session.getAttribute(com.dotmarketing.util.WebKeys.PREVIEW_MODE_SESSION) != null) && ADMIN_MODE);
		boolean EDIT_MODE = ((session.getAttribute(com.dotmarketing.util.WebKeys.EDIT_MODE_SESSION) != null) && ADMIN_MODE);
		if (EDIT_MODE || PREVIEW_MODE) {
			live = false;
		}

		ResourceResponse responseResource = new ResourceResponse(initData.getParamsMap());

		User user = initData.getUser();
		try {
			String esQuery = IOUtils.toString(request.getInputStream());

			return responseResource.response(esapi.esSearchRaw(esQuery, live, user, live).toString());

		} catch (Exception e) {
			Logger.error(this.getClass(), "Error processing :" + e.getMessage(), e);
			return responseResource.responseError(e.getMessage());

		}
	}

	final String[] ignoreFields = { "disabledWYSIWYG", "lowIndexPriority" };
	private Set<String> getJSONFields(Structure s) {
		Set<String> jsonFields=new HashSet<String>();
		for(Field f : FieldsCache.getFieldsByStructureInode(s.getInode()))
			if(f.getFieldType().equals(Field.FieldType.KEY_VALUE.toString()))
				jsonFields.add(f.getVelocityVarName());
		return jsonFields;
	}
	private JSONObject contentletToJson(Contentlet con) throws JSONException, IOException {
		JSONObject jo = new JSONObject();
		Structure s = con.getStructure();
		Map<String,Object> map = con.getMap();

		for (String x : ignoreFields) {
			map.remove(x);
		}
		
		
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



		return jo;
	}

}