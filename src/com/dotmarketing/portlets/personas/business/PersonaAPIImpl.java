package com.dotmarketing.portlets.personas.business;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.DotContentletValidationException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.personas.model.Persona;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

public class PersonaAPIImpl implements PersonaAPI {

	PersonaFactory pFactory = FactoryLocator.getPersonaFactory();

	@Override
	public List<Field> getBasePersonaFields(Structure structure) {
		List<Field> fields = new ArrayList<Field>();
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
	public List<Persona> getPersonas(Object parent, boolean live, boolean deleted, User user, boolean respectFrontEndRoles)
			throws DotDataException, DotSecurityException {
		return getPersonas(parent, live, deleted, -1, 0, "", user, respectFrontEndRoles);
	}

	@Override
	public List<Persona> getPersonas(Object parent, boolean live, boolean deleted, int limit, int offset, String sortBy, User user,
			boolean respectFrontEndRoles) throws DotDataException, DotSecurityException {
		List<Persona> personas = new ArrayList<Persona>();
		StringBuffer query = new StringBuffer();
		String liveWorkingDeleted = (live) ? " +live:true " : (deleted) ? " +working:true +deleted:true " : " +working:true -deleted:true ";
		query.append(liveWorkingDeleted);
		if (parent instanceof Host) {
			query.append(" +conFolder:SYSTEM_FOLDER +conHost:" + ((Host) parent).getIdentifier() + " ");
		}
		query.append(" +structureType:" + Structure.STRUCTURE_TYPE_PERSONA);
		if (!UtilMethods.isSet(sortBy)) {
			sortBy = "modDate desc";
		}
		List<Contentlet> contentlets = APILocator.getContentletAPI().search(query.toString(), limit, offset, sortBy, user,
				respectFrontEndRoles);
		for (Contentlet cont : contentlets) {
			personas.add((Persona) cont);
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

		String keyTag = persona.getKeyTag();

		// we need to make sure no other persona has the same keyfield
		List<Structure> personaStructs = StructureFactory.getStructures("structuretype=6", "mod_date", -1, 0, "asc");

		StringWriter sw = new StringWriter();
		if (UtilMethods.isSet(persona.getIdentifier())) {
			sw.append(" -identifier:" + persona.getIdentifier());
		}
		if (UtilMethods.isSet(persona.getInode())) {
			sw.append(" -inode:" + persona.getInode());
		}

		sw.append(" +conhost:" + persona.getHost());
		sw.append(" +basetype:6 +languageid:* +(");

		for (Structure s : personaStructs) {
			sw.append( s.getVelocityVarName() + "." + KEY_TAG_FIELD + ":" + keyTag + " ");

		}
		sw.append(") ");

		try {
			

			if (APILocator.getContentletAPI().indexCount(sw.toString(), APILocator.getUserAPI().getSystemUser(), false) > 0) {
				Language l = APILocator.getLanguageAPI().getLanguage(user.getLanguageId());
				String message = APILocator.getLanguageAPI().getStringKey(l, "message.persona.error.invalidKeyTagField");
				message = message.replace("{0}", persona.getKeyTag());

				throw new DotContentletValidationException(message);
			}

			try {
				// TODO
				// make the key tag persist as a key tag
				// Tag tag = APILocator.getTagAPI().getTag(keyTag,
				// UserAPI.SYSTEM_USER_ID, persona.getHost());

			} catch (Exception e) {
				Logger.error(this.getClass(), "tag failed to save:" + e.getMessage());
				throw new DotContentletValidationException(e.getMessage(), e);
			}

		} catch (DotDataException | DotSecurityException e) {
			throw new DotContentletValidationException(e.getMessage(), e);
		}

	}

	@Override
	public Persona find(String id, User user, boolean respectFrontEndRoles) throws DotDataException, DotSecurityException {

		Contentlet con = APILocator.getContentletAPI().findContentletByIdentifier(id, false,
				APILocator.getLanguageAPI().getDefaultLanguage().getId(), user, respectFrontEndRoles);

		return fromContentlet(con);
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