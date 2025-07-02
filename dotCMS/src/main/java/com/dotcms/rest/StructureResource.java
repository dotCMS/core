package com.dotcms.rest;

import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Path("/structure")
@Tag(name = "Content Type", description = "Content type definitions and schema management")
public class StructureResource {

	@GET
	@Path("/{path:.*}")
	@Produces("application/json")
	public Response getStructuresWithWYSIWYGFields(@Context HttpServletRequest request, @Context HttpServletResponse response,
                                                   @PathParam("path") String path, @QueryParam("name") String name,
                                                   @PathParam ("type") String type,
                                                   @PathParam ("callback") String callback)
											throws DotDataException, JSONException {

        Map<String, String> paramsMap = new HashMap<>();
        paramsMap.put( "type", type );
        paramsMap.put( "callback", callback );
        //Creating an utility response object
        ResourceResponse responseResource = new ResourceResponse( paramsMap );

		List<Structure> structures=new ArrayList<>();
		
		String inodeFilter = "";
		if(path!= null && path.length() > 1) {
			inodeFilter = path.substring(1);
		}
		
		String nameFilter = "";
		if(name != null && name.length() > 1) {
			nameFilter = name.toLowerCase();
			if(nameFilter.contains("*"))
				nameFilter = nameFilter.substring(0, nameFilter.indexOf("*"));
		}
		
		String range = "";
		int beginItem = -1;
		int endItem = -1;
		String rangeHeader = request.getHeader("Range");
		if(rangeHeader != null && rangeHeader.length() >= 6 && rangeHeader.indexOf("=") > 0) {
			range = rangeHeader.substring(rangeHeader.indexOf("=") + 1);
			beginItem = Integer.valueOf(range.substring(0, range.indexOf("-")));
			endItem = Integer.valueOf(range.substring(range.indexOf("-") + 1));
		}

		try {
			if (inodeFilter.isEmpty()) {
				List<Structure> allStructures = StructureFactory
						.getStructures("structuretype,upper(name)", -1);
				for (Structure st : allStructures) {
					if (!st.isArchived() && (nameFilter.isEmpty() || (st.getName()
							.toLowerCase().startsWith(nameFilter)))) {
						for (Field field : FieldsCache.getFieldsByStructureInode(st.getInode())) {
							if (field.getFieldType().equals(Field.FieldType.WYSIWYG.toString())) {
								structures.add(st);
								break;
							}
						}
					}
				}
			} else {
				Structure specificStructure = CacheLocator.getContentTypeCache()
						.getStructureByInode(inodeFilter);
				if (specificStructure != null)
					structures.add(specificStructure);
			}
		} catch (DotSecurityException e) {
			throw new ForbiddenException(e);
		}
		
		JSONArray jsonStructures = new JSONArray();
		JSONObject jsonStructureObject = new JSONObject();
		
		int structCount = 0;
		for(Structure st: structures)
		{
			if(!inodeFilter.isEmpty() || (!range.isEmpty() && structCount >= beginItem && structCount <= endItem)){
				jsonStructureObject = new JSONObject();
				jsonStructureObject.put("id", st.getInode());
				jsonStructureObject.put("name", st.getName());
			}
			if(inodeFilter.isEmpty()){
				jsonStructures.add(jsonStructureObject);
			}else{
				return responseResource.response(jsonStructureObject.toString());
			}
		}
		if(inodeFilter.isEmpty() && !range.isEmpty()) {
			response.addHeader("Content-Range", "items " + beginItem + "-" + Math.min(endItem, structCount -1) + "/" + structCount);
		}

        return responseResource.response(jsonStructures.toString());
    }
}
