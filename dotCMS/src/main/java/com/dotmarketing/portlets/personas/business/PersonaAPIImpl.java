package com.dotmarketing.portlets.personas.business;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.Treeable;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.DotContentletValidationException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.personas.model.Persona;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

public class PersonaAPIImpl implements PersonaAPI {

	PersonaFactory pFactory = FactoryLocator.getPersonaFactory();

	@Override
	public List<Field> getBasePersonaFields(Structure structure) {
		List<Field> fields = new ArrayList<>();
		Field field = null;
		int i = 1;

		field = new Field(HOST_FOLDER_FIELD_NAME, Field.FieldType.HOST_OR_FOLDER, Field.DataType.TEXT, structure, true, false, true, i++,
				"", "", "", true, false, true);
		field.setVelocityVarName(HOST_FOLDER_FIELD);
		field.setFieldContentlet("system_field1");
		fields.add(field);

		field = new Field(NAME_FIELD_NAME, Field.FieldType.TEXT, Field.DataType.TEXT, structure, true, true, true, i++, "", "", "", true,
				false, true);
		field.setVelocityVarName(NAME_FIELD);
		field.setFieldContentlet("text1");
		fields.add(field);

		field = new Field(KEY_TAG_FIELD_NAME, Field.FieldType.CUSTOM_FIELD, Field.DataType.TEXT, structure, true, true, true, i++,
				"$velutil.mergeTemplate('/static/personas/keytag_custom_field.vtl')", "", "[a-zA-Z0-9]+", true, false, true);
		field.setVelocityVarName(KEY_TAG_FIELD);
		field.setFieldContentlet("text2");
		fields.add(field);

		field = new Field(PHOTO_FIELD_NAME, Field.FieldType.BINARY, Field.DataType.BINARY, structure, false, false, false, i++, "", "", "",
				true, false, false);
		field.setVelocityVarName(PHOTO_FIELD);
		field.setFieldContentlet("binary1");
		fields.add(field);

		field = new Field(TAGS_FIELD_NAME, Field.FieldType.TAG, Field.DataType.LONG_TEXT, structure, false, true, true, i++, "", "", "",
				true, false, true);
		field.setVelocityVarName(TAGS_FIELD);
		field.setFieldContentlet("text_area1");
		field.setListed(false);
		;
		fields.add(field);

		field = new Field(DESCRIPTION_FIELD_NAME, Field.FieldType.TEXT_AREA, Field.DataType.LONG_TEXT, structure, false, false, false, i++,
				"", "", "", true, false, true);
		field.setVelocityVarName(DESCRIPTION_FIELD);
		field.setFieldContentlet("text_area2");
		fields.add(field);



		return fields;

	}

	@Override
	public void createPersonaBaseFields(Structure structure) throws DotDataException, DotStateException {
		if (structure == null || !InodeUtils.isSet(structure.getInode())) {
			throw new DotStateException("Can not create base Persona fields on a structure that does not exist");
		}
		if (structure.getStructureType() != Structure.STRUCTURE_TYPE_PERSONA) {
			throw new DotStateException("Can not create base Persona fields on a structure that is not Persona type");
		}

		List<Field> fields = getBasePersonaFields(structure);
		for (Field f : fields) {
			FieldFactory.saveField(f);
		}
	}

	@Override
	public Host getParentHost(Persona persona) throws DotDataException, DotStateException, DotSecurityException {
		return APILocator.getHostAPI().find(APILocator.getIdentifierAPI().find(persona).getHostId(),
				APILocator.getUserAPI().getSystemUser(), false);
	}

	@Override
	public List<Persona> getLiveHTMLPages(Host parent, User user, boolean respectFrontEndRoles) throws DotDataException,
			DotSecurityException {
		return getPersonas(parent, true, false, user, respectFrontEndRoles);
	}

	@Override
	public List<Persona> getWorkingHTMLPages(Host parent, User user, boolean respectFrontEndRoles) throws DotDataException,
			DotSecurityException {
		return getPersonas(parent, false, false, user, respectFrontEndRoles);
	}

	@Override
	public List<Persona> getDeletedPersonas(Host parent, User user, boolean respectFrontEndRoles) throws DotDataException,
			DotSecurityException {
		return getPersonas(parent, false, true, user, respectFrontEndRoles);
	}

	@Override
	public List<Persona> getPersonas(Treeable parent, boolean live, boolean deleted, User user, boolean respectFrontEndRoles)
			throws DotDataException, DotSecurityException {
		return getPersonas(parent, live, deleted, -1, 0, "", user, respectFrontEndRoles);
	}

	@Override
	public List<Persona> getPersonas(Treeable parent, boolean live, boolean deleted, int limit, int offset, String sortBy, User user,
			boolean respectFrontEndRoles) throws DotDataException, DotSecurityException {
		List<Persona> personas = new ArrayList<>();
		StringBuilder query = new StringBuilder();
		String liveWorkingDeleted = (live) ? " +live:true " : (deleted) ? " +working:true +deleted:true " : " +working:true -deleted:true ";
		query.append(liveWorkingDeleted);
		if (parent instanceof Host) {
			query.append(" +conFolder:SYSTEM_FOLDER");
			query.append(" +conHost:(").append(parent.getIdentifier()).append(" ").append(Host.SYSTEM_HOST).append(")");
		}
		else if (parent instanceof Folder) {
			query.append(" +conFolder:").append(parent.getIdentifier()).append(" ");
		}
		
		query.append(" +structureType:" + Structure.STRUCTURE_TYPE_PERSONA);
		if (!UtilMethods.isSet(sortBy)) {
			sortBy = "title desc";
		}
		List<Contentlet> contentlets = APILocator.getContentletAPI().search(query.toString(), limit, offset, sortBy, user,
				respectFrontEndRoles);
		for (Contentlet cont : contentlets) {
			personas.add(fromContentlet(cont));
		}
		return personas;
	}

	/**
	 * This method checks to insure that we are not persisting a persona
	 * contentlet that duplicates the keytag of any other persona on the same
	 * host (across structures), Key tag fields are intended to be unique on
	 * Hosts).
	 */
	@Override
	public void validatePersona(Contentlet c) throws DotContentletValidationException {
		Persona persona = fromContentlet(c);
		User user = null;
		try {
			user = APILocator.getUserAPI().loadUserById(c.getModUser());
		} catch (Exception e) {
			try {
				user = APILocator.getUserAPI().getSystemUser();
			} catch (DotDataException e1) {
				throw new DotContentletValidationException("User Not Found");
			}
		}

		if(c.getLanguageId()!=APILocator.getLanguageAPI().getDefaultLanguage().getId()) {
			throw new DotContentletValidationException("Can't create Persona in a Language different than default language");
		}

		String keyTag = persona.getKeyTag();

		// we need to make sure no other persona has the same keyfield
		List<Structure> personaStructs = StructureFactory.getStructures("structuretype=6", "mod_date", -1, 0, "asc");

		StringWriter sw = new StringWriter();
		if (UtilMethods.isSet(persona.getIdentifier())) {
			sw.append(" -identifier:").append(persona.getIdentifier());
		}
		if (UtilMethods.isSet(persona.getInode())) {
			sw.append(" -inode:").append(persona.getInode());
		}

		sw.append(" +conhost:").append(persona.getHost());
		sw.append(" +basetype:6 +languageid:* +(");

		for (Structure s : personaStructs) {
			sw.append(s.getVelocityVarName()).append(".").append(KEY_TAG_FIELD).append(":").append(keyTag).append(" ");

		}
		sw.append(") ");

		try {
			

			if (APILocator.getContentletAPI().indexCount(sw.toString(), APILocator.getUserAPI().getSystemUser(), false) > 0) {
				Language l = APILocator.getLanguageAPI().getLanguage(user.getLanguageId());
				String message = APILocator.getLanguageAPI().getStringKey(l, "message.persona.error.invalidKeyTagField");
				message = message.replace("{0}", persona.getKeyTag());

				throw new DotContentletValidationException(message);
			}

		} catch (DotDataException | DotSecurityException e) {
			throw new DotContentletValidationException(e.getMessage(), e);
		}

	}

	/**
	 * A Persona key tag should be persist as a Tag. When the
	 * <code>enable</code> parameter is <code>true</code>, the tag will be
	 * created if does not already exist. Saving a Persona tag in the <i>Tag</i>
	 * table will set the value of the "persona" column to <code>true</code>.
	 *
	 * @param personaContentlet
	 *            - The Persona contentlet that is being saved.
	 * @param enable
	 *            - If <code>true</code> the tag to be saved will be handled as
	 *            a Persona tag. Otherwise, the tag will be handled as a regular
	 *            tag.
	 * @throws DotDataException
	 *             An error occurred when saving the data.
	 * @throws DotSecurityException
	 *             The current user does not have permissions to perform the
	 *             requested action.
	 */
	public void enableDisablePersonaTag ( Contentlet personaContentlet, boolean enable ) throws DotDataException, DotSecurityException {

		Persona persona = fromContentlet(personaContentlet);
		String keyTag = persona.getKeyTag();

		// Search for the tag related to this key tag, either in current host or
		// system host
		Tag foundPersonaKeyTag = APILocator.getTagAPI().getTagByNameAndHost(keyTag, persona.getHost());
		if (foundPersonaKeyTag == null || !UtilMethods.isSet(foundPersonaKeyTag.getTagId())) {
			foundPersonaKeyTag = APILocator.getTagAPI().getTagByNameAndHost(keyTag, Host.SYSTEM_HOST);
		}

		//Make sure the tag exist for this key tag, if not we need to create it
		if ( enable && (foundPersonaKeyTag == null || !UtilMethods.isSet(foundPersonaKeyTag.getTagId())) ) {

			//Persist the key tag as a Tag
			APILocator.getTagAPI().getTagAndCreate(keyTag, persona.getHost(), true);
			return;
		}

		if ( foundPersonaKeyTag != null && UtilMethods.isSet(foundPersonaKeyTag.getTagId()) ) {
			//Disable/enable this tag as Persona tag
			APILocator.getTagAPI().enableDisablePersonaTag(foundPersonaKeyTag.getTagId(), enable);
		}
	}

	@Override
	public Persona find(String id, User user, boolean respectFrontEndRoles) throws DotDataException, DotSecurityException {

		Contentlet con = APILocator.getContentletAPI().findContentletByIdentifier(id, false,
				APILocator.getLanguageAPI().getDefaultLanguage().getId(), user, respectFrontEndRoles);

		return UtilMethods.isSet(con) ? fromContentlet(con) : null;
	}

	@Override
	public Persona fromContentlet(final Contentlet con) throws DotStateException {

		Persona p = new Persona(con);

		return p;
	}

	@Override
	public void createDefaultPersonaStructure() throws DotDataException {

		pFactory.createDefualtPersonaStructure();
	}

	@Override
	public Structure getDefaultPersonaStructure() throws DotSecurityException, DotDataException {

		Structure defaultStr = APILocator.getStructureAPI().findByVarName(PersonaAPI.DEFAULT_PERSONAS_STRUCTURE_VARNAME,
				APILocator.getUserAPI().getSystemUser());
		return defaultStr;
	}

}