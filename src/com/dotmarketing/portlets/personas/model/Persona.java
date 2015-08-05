package com.dotmarketing.portlets.personas.model;


import java.util.ArrayList;
import java.util.List;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.personas.business.PersonaAPI;

public class Persona extends Contentlet implements IPersona{
	private static final long serialVersionUID = -4775734788059690797L;

	@Override
	public String getName() {
		return getStringProperty(PersonaAPI.NAME_FIELD);
	}

	@Override
	public void setName(String name) {
		setStringProperty(PersonaAPI.NAME_FIELD, name);
	}

	@Override
	public String KeyTag() {
		return getStringProperty(PersonaAPI.KEY_TAG_FIELD);
	}

	@Override
	public void setKeyTag(String keyTag) {
		setStringProperty(PersonaAPI.KEY_TAG_FIELD, keyTag);
	}

	@Override
	public String Description() {
		return getStringProperty(PersonaAPI.DESCRIPTION_FIELD);
	}

	@Override
	public void setDescription(String description) {
		setStringProperty(PersonaAPI.DESCRIPTION_FIELD, description);
	}

	@Override
	public boolean isActive() {
		String value = getStringProperty(PersonaAPI.ACTIVE_FIELD);
		return value!=null && value.contains("true");
	}

	@Override
	public void setActive(boolean active) {
		setStringProperty(PersonaAPI.ACTIVE_FIELD, active ? "true" : "");
	}

	@Override
	public List<String> getTags() {
		//TODO
		return null;
	}

	@Override
	public void setTags(List<String> tags) {
		//TODO
	}

}
