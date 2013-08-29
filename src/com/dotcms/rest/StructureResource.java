package com.dotcms.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;

import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;

@Path("/structure")
public class StructureResource extends WebResource {

	@GET
	@Path("/{path:.*}")
	@Produces("application/json")
	public Response getStructuresWithWYSIWYGFields(@Context HttpServletRequest request, @Context HttpServletResponse response,
                                                   @PathParam("path") String path, @QueryParam("name") String name,
                                                   @PathParam ("type") String type,
                                                   @PathParam ("callback") String callback) throws DotStateException, DotDataException, DotSecurityException {

        Map<String, String> paramsMap = new HashMap<String, String>();
        paramsMap.put( "type", type );
        paramsMap.put( "callback", callback );
        //Creating an utility response object
        ResourceResponse responseResource = new ResourceResponse( paramsMap );

		List<Structure> structures=new ArrayList<Structure>();
		
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
		
		if(inodeFilter.isEmpty()) {
			List<Structure> allStructures = StructureFactory.getStructures("structuretype,upper(name)", -1);
			for(Structure st : allStructures) {
				if(st.isArchived() == false && (nameFilter.isEmpty() || (st.getName().toLowerCase().startsWith(nameFilter)))) {
					for(Field field : FieldsCache.getFieldsByStructureInode(st.getInode())) {
						if(field.getFieldType().equals(Field.FieldType.WYSIWYG.toString())) {
							structures.add(st);
							break;
						}
					}
				}
			}
		}
		else {
			Structure specificStructure = StructureCache.getStructureByInode(inodeFilter);
			if(specificStructure != null)
				structures.add(specificStructure);
		}

		boolean bInitialStruct = true;
		StringBuilder structureDataStore = new StringBuilder("");
		String EOL = System.getProperty("line.separator");
		if(inodeFilter.isEmpty()) {
			structureDataStore.append("[");
			structureDataStore.append(EOL);
		}
		int structCount = 0;
		for(Structure st: structures)
		{
			if(!inodeFilter.isEmpty() || (!range.isEmpty() && structCount >= beginItem && structCount <= endItem)){
				if(bInitialStruct)
					bInitialStruct = false;
				else
				{
					structureDataStore.append(",");
					structureDataStore.append(EOL);
				}
				structureDataStore.append("{id: \"");
				structureDataStore.append(st.getInode());
				structureDataStore.append("\", name: \"");
				structureDataStore.append(st.getName());
				structureDataStore.append("\"}");
			}
			structCount++;
		}
		if(inodeFilter.isEmpty()) {
			structureDataStore.append(EOL);
			structureDataStore.append("]");
			if(!range.isEmpty()){
				response.addHeader("Content-Range", "items " + beginItem + "-" + Math.min(endItem, structCount -1) + "/" + structCount);
			}
		}

        return responseResource.response( structureDataStore.toString() );
    }
}
