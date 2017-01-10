package com.dotmarketing.portlets.personas.business;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import com.dotmarketing.portlets.personas.model.Persona;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.Treeable;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.DotContentletValidationException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.liferay.portal.model.User;

public interface PersonaAPI {
	
	static final String HOST_FOLDER_FIELD_NAME = "Site/Folder";
	static final String HOST_FOLDER_FIELD = "hostFolder";
	
	static final String NAME_FIELD = "name";
	static final String NAME_FIELD_NAME = "Name";
	
	static final String KEY_TAG_FIELD = "keyTag";
	static final String KEY_TAG_FIELD_NAME = "Key Tag";
	
	static final String DESCRIPTION_FIELD = "description";
	static final String DESCRIPTION_FIELD_NAME = "Description";
	
	static final String TAGS_FIELD = "tags";
	static final String TAGS_FIELD_NAME = "Other Tags";
	
	static final String PHOTO_FIELD = "photo";
	static final String PHOTO_FIELD_NAME = "Photo";
	
	static final String DEFAULT_PERSONAS_STRUCTURE_NAME = "Persona";
	static final String DEFAULT_PERSONAS_STRUCTURE_DESCRIPTION = "Default Structure for Personas";
	static final String DEFAULT_PERSONAS_STRUCTURE_VARNAME = "persona";
	static final String DEFAULT_PERSONAS_STRUCTURE_INODE = "c938b15f-bcb6-49ef-8651-14d455a97045";
	
	void createPersonaBaseFields(Structure structure) throws DotDataException, DotStateException;
	
	Host getParentHost(Persona persona) throws DotDataException, DotStateException, DotSecurityException;
	
	List<Persona> getLiveHTMLPages ( Host parent, User user, boolean respectFrontEndRoles ) throws DotDataException, DotSecurityException;
	
	List<Persona> getWorkingHTMLPages ( Host parent, User user, boolean respectFrontEndRoles ) throws DotDataException, DotSecurityException;
	
	List<Persona> getDeletedPersonas(Host parent, User user, boolean respectFrontEndRoles) throws DotDataException, DotSecurityException;
	
	List<Persona> getPersonas(Treeable parent, boolean live, boolean deleted, User user, boolean respectFrontEndRoles) throws DotDataException, DotSecurityException;
	
	List<Persona> getPersonas(Treeable parent, boolean live, boolean deleted, int limit, int offset, String sortBy, User user,boolean respectFrontEndRoles) throws DotDataException,DotSecurityException;

	/**
	 * @param id The Persona identifier.
	 * @param user The User with Permissions to find the Persona.
	 * @param respectFrontEndRoles true if call comes from FrontEnd, otherwise false.
	 * @return Persona object from Contentlet, null if not present.
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	Persona find(String id,  User user, boolean respectFrontEndRoles)throws DotDataException, DotSecurityException;

	Persona fromContentlet(Contentlet con) throws DotDataException, DotSecurityException, IllegalAccessException, InvocationTargetException;

	Structure getDefaultPersonaStructure() throws DotSecurityException, DotDataException;

	List<Field> getBasePersonaFields(Structure structure);

	void validatePersona(Contentlet c) throws DotContentletValidationException;

	/**
	 * A Persona key tag should be persist as a Tag, when the @enable param is true the tag will be created if does not
	 * already exist.
	 *
	 * @param personaContentlet
	 * @param enable            When false the tag created based on the Persona key tag will be handle as a regular tag
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public void enableDisablePersonaTag ( Contentlet personaContentlet, boolean enable ) throws DotDataException, DotSecurityException;

	void createDefaultPersonaStructure() throws DotDataException;

}