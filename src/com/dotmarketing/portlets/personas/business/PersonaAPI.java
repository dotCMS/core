package com.dotmarketing.portlets.personas.business;

import java.util.ArrayList;
import java.util.List;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.personas.model.Persona;
import com.dotmarketing.portlets.structure.model.Structure;
import com.liferay.portal.model.User;

public interface PersonaAPI {
	
	static final String NAME_FIELD = "name";
	static final String NAME_FIELD_NAME = "Name";
	
	static final String KEY_TAG_FIELD = "keyTag";
	static final String KEY_TAG_FIELD_NAME = "Key Tag";
	
	static final String DESCRIPTION_FIELD = "description";
	static final String DESCRIPTION_FIELD_NAME = "Description";
	
	static final String ACTIVE_FIELD = "active";
	static final String ACTIVE_FIELD_NAME = "Active";
	
	static final String TAGS_FIELD = "tags";
	static final String TAGS_FIELD_NAME = "Tags";
	
	static final String PHOTO_FIELD = "photo";
	static final String PHOTO_FIELD_NAME = "Photo";
	
	static final String DEFAULT_PERSONAS_STRUCTURE_NAME = "Personas";
	static final String DEFAULT_PERSONAS_STRUCTURE_DESCRIPTION = "Default Structure for Personas";
	static final String DEFAULT_PERSONAS_STRUCTURE_VARNAME = "personas";
	static final String DEFAULT_PERSONAS_STRUCTURE_INODE = "353e4804-a595-427a-ad75-114d88729833";
	
	void createPersonaBaseFields(Structure structure) throws DotDataException, DotStateException;
	
	Host getParentHost(Persona persona) throws DotDataException, DotStateException, DotSecurityException;
	
	List<Persona> getActivePersonas (Host parent, User user, boolean respectFrontEndRoles) throws DotDataException, DotSecurityException;
	
	List<Persona> getInactivePersonas(Host parent, User user, boolean respectFrontEndRoles) throws DotDataException, DotSecurityException;
	
	List<Persona> getDeletedPersonas(Host parent, User user, boolean respectFrontEndRoles) throws DotDataException, DotSecurityException;
	
}
