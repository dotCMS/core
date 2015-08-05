package com.dotmarketing.portlets.personas.business;

import java.util.List;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.personas.model.Persona;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.InodeUtils;
import com.liferay.portal.model.User;

public class PersonaAPIImpl implements PersonaAPI{
	public static final String DEFAULT_PERSONA_STRUCTURE_HOST_FIELD = "defaultPersonaStructure";
	private static PermissionAPI permissionAPI = APILocator.getPermissionAPI();
	

	@Override
	public void createPersonaBaseFields(Structure structure) throws DotDataException, DotStateException {
		if(structure == null || !InodeUtils.isSet(structure.getInode())){
			throw new DotStateException("Can not create base Persona fields on a structure that does not exist");
		}
		if(structure.getStructureType() != Structure.STRUCTURE_TYPE_PERSONA){
			throw new DotStateException("Can not create base Persona fields on a structure that is not Persona type");
		}
		
		Field field = new Field(NAME_FIELD_NAME, Field.FieldType.TEXT,Field.DataType.TEXT,structure,true,true,true,1,"","","",true,false,true);
		field.setVelocityVarName(NAME_FIELD);
		FieldFactory.saveField(field);
		
		field = new Field(KEY_TAG_FIELD_NAME, Field.FieldType.TEXT,Field.DataType.TEXT,structure,true,true,true,2,"","","",true,false,true);
		field.setVelocityVarName(KEY_TAG_FIELD);
		FieldFactory.saveField(field);
		
		field = new Field(DESCRIPTION_FIELD_NAME, Field.FieldType.TEXT_AREA,Field.DataType.LONG_TEXT,structure,false,false,false,3,"","","",true,false,true);
		field.setVelocityVarName(DESCRIPTION_FIELD);
		FieldFactory.saveField(field);
		
		field = new Field(TAGS_FIELD_NAME, Field.FieldType.TAG,Field.DataType.LONG_TEXT,structure,true,true,true,4,"","","",true,false,true);
		field.setVelocityVarName(TAGS_FIELD);
		FieldFactory.saveField(field);
		
		field = new Field(ACTIVE_FIELD_NAME, Field.FieldType.CHECKBOX,Field.DataType.TEXT,structure,false,false,true,5,"|true","false","",true,false,true);
		field.setVelocityVarName(ACTIVE_FIELD);
		FieldFactory.saveField(field);
		
		field = new Field(PHOTO_FIELD_NAME, Field.FieldType.BINARY,Field.DataType.BINARY,structure,false,false,false,6,"","","",true,false,false);
		field.setVelocityVarName(PHOTO_FIELD);
		FieldFactory.saveField(field);
		
	}

	@Override
	public Host getParentHost(Persona persona) throws DotDataException, DotStateException, DotSecurityException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Persona> getActivePersonas(Host parent, User user, boolean respectFrontEndRoles)
			throws DotDataException, DotSecurityException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Persona> getInactivePersonas(Host parent, User user, boolean respectFrontEndRoles)
			throws DotDataException, DotSecurityException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Persona> getDeletedPersonas(Host parent, User user, boolean respectFrontEndRoles)
			throws DotDataException, DotSecurityException {
		// TODO Auto-generated method stub
		return null;
	}

}
