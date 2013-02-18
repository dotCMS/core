package com.dotcms.rest;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;

@Path("/structure")
public class StructureResource extends WebResource {

	@GET
	@Path("/{path:.*}")
	@Produces(MediaType.TEXT_PLAIN)
	public String getStructuresWithWYSIWYGFields(@Context HttpServletRequest request, @Context HttpServletResponse response, @PathParam("path") String path) throws DotStateException, DotDataException, DotSecurityException {
		List<Structure> structures=new ArrayList<Structure>();
		List<Structure> allStructures = StructureFactory.getStructures("structuretype,upper(name)", -1);
		for(Structure st : allStructures) {
			if(st.isArchived() == false) {
				for(Field field : FieldsCache.getFieldsByStructureInode(st.getInode())) {
					if(field.getFieldType().equals(Field.FieldType.WYSIWYG.toString())) {
						structures.add(st);
						break;
					}
				}
			}
		}

		boolean bInitialStruct = true;
		String EOL = System.getProperty("line.separator");
		StringBuilder structureDataStore = new StringBuilder("{ \"identifier\": \"iNode\", \"label\": \"name\", \"items\": [");
		structureDataStore.append(EOL);
		for(Structure st: structures)
		{
			if(bInitialStruct)
				bInitialStruct = false;
			else
			{
				structureDataStore.append(",");
				structureDataStore.append(EOL);
			}
			structureDataStore.append("{\"iNode\": \"");
			structureDataStore.append(st.getInode());
			structureDataStore.append("\", \"name\": \"");
			structureDataStore.append(st.getName());
			structureDataStore.append("\"}");
		}
		structureDataStore.append("]}");
		
/*		
		for(Structure st : structures) {
			retValue+=st.getInode()+ "|" + st.getName() +"||";	
		}
*/
		return structureDataStore.toString();
	}
}
