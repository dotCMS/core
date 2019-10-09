package com.dotmarketing.portlets.personas.business;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Optional;

import com.dotmarketing.portlets.contentlet.model.ContentletListener;
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

import io.vavr.Tuple2;

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
	
	
	static final String DEFAULT_PERSONA_NAME_KEY="modes.persona.no.persona";
	
	
	
	void createPersonaBaseFields(Structure structure) throws DotDataException, DotStateException;
	
	Host getParentHost(Persona persona) throws DotDataException, DotStateException, DotSecurityException;
	
	List<Persona> getLiveHTMLPages ( Host parent, User user, boolean respectFrontEndRoles ) throws DotDataException, DotSecurityException;
	
	List<Persona> getWorkingHTMLPages ( Host parent, User user, boolean respectFrontEndRoles ) throws DotDataException, DotSecurityException;
	
	List<Persona> getDeletedPersonas(Host parent, User user, boolean respectFrontEndRoles) throws DotDataException, DotSecurityException;
	
	List<Persona> getPersonas(Treeable parent, boolean live, boolean deleted, User user, boolean respectFrontEndRoles) throws DotDataException, DotSecurityException;
	
	List<Persona> getPersonas(Treeable parent, boolean live, boolean deleted, int limit, int offset, String sortBy, User user,boolean respectFrontEndRoles) throws DotDataException,DotSecurityException;

	/**
	 * Find a persona even a working version
	 * @param identifier The Persona identifier.
	 * @param user The User with Permissions to find the Persona.
	 * @param respectFrontEndRoles true if call comes from FrontEnd, otherwise false.
	 * @return Persona object from Contentlet, null if not present.
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	Persona find(String identifier,  User user, boolean respectFrontEndRoles)throws DotDataException, DotSecurityException;

	/**
	 * Find a live persona version
	 * @param id The Persona identifier.
	 * @param user The User with Permissions to find the Persona.
	 * @param respectFrontEndRoles true if call comes from FrontEnd, otherwise false.
	 * @return Persona object from Contentlet, null if not present.
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	Persona findLive(String id,  User user, boolean respectFrontEndRoles)throws DotDataException, DotSecurityException;

	Persona fromContentlet(Contentlet con) throws DotDataException, DotSecurityException;

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

	/**
	 * If exists a persona indexed by the personaTag, will return an Optional with the persona, otherwise Optional will be empty.
	 * @param personaTag {@link String}
     * @param user {@link User}
     * @param respectFrontEndRoles {@link Boolean}
	 * @return Optional of Persona
	 */
    Optional<Persona> findPersonaByTag (final String personaTag, final User user, final boolean respectFrontEndRoles) throws DotSecurityException, DotDataException;

	/**
	 * Adds a persona listener
	 * @param personaListener
	 */
	void addPersonaListener (final ContentletListener<Persona> personaListener);

  /**
   * Returns two things: first, a pageable list of personas that will include the default persona;
   * second, the total number of matches, needed for paging
   * 
   * @param parent
   * @param filter
   * @param live
   * @param limit
   * @param offset
   * @param sortBy
   * @param user
   * @param respectFrontEndRoles
   * @return
   * @throws DotDataException
   * @throws DotSecurityException
   */
  Tuple2<List<Persona>, Integer> getPersonasIncludingDefaultPersona(Treeable parent, String filter, boolean live, int limit, int offset,
      String sortBy, User user, boolean respectFrontEndRoles) throws DotDataException, DotSecurityException;

  /**
   * Returns the Default Persona, which is just a mock object
   * @return
   */
  Persona getDefaultPersona();

}