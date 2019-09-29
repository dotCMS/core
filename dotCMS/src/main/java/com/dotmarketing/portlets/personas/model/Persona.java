package com.dotmarketing.portlets.personas.model;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.personas.business.PersonaAPI;

public class Persona extends Contentlet implements IPersona{
	private static final long serialVersionUID = -4775734788059690797L;

	public static final String DOT_PERSONA_PREFIX_SCHEME = "dot:persona";
	

	public Persona(Contentlet oldCon) {
		Map<String, Object> newMap = new HashMap<>();
		oldCon.getMap().forEach(newMap::put);
		super.map = newMap;
	}

	@Override
	public String getName() {
		return getStringProperty(PersonaAPI.NAME_FIELD);
	}

	@Override
	public void setName(String name) {
		setStringProperty(PersonaAPI.NAME_FIELD, name);
	}

	@Override
	public String getKeyTag() {
		return getStringProperty(PersonaAPI.KEY_TAG_FIELD);
	}

	@Override
	public void setKeyTag(String keyTag) {
		setStringProperty(PersonaAPI.KEY_TAG_FIELD, keyTag);
	}

	@Override
	public String getDescription() {
		return getStringProperty(PersonaAPI.DESCRIPTION_FIELD);
	}

	@Override
	public void setDescription(String description) {
		setStringProperty(PersonaAPI.DESCRIPTION_FIELD, description);
	}

	@Override
	public String getTags() {
		return getStringProperty(PersonaAPI.TAGS_FIELD);
	}

	@Override
	public void setTags(List<String> tags) {
		setStringProperty(PersonaAPI.TAGS_FIELD,tags.toString());
	}
	
	@Override
	public String toString() {
	    return this.getName() + " : " + this.getKeyTag();
	}

}
