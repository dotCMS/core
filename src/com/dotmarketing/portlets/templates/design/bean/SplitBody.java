package com.dotmarketing.portlets.templates.design.bean;

/**
 * Bean that represent a single HTML select/option when we try to edit a drawed template.
 * 
 * @author	Graziano Aliberti - Engineering Ingegneria Informatica
 * @date	Apr 23, 2012
 */
public class SplitBody {
	
	private int identifier;
	
	private String value;
	
	private String id;

	public int getIdentifier() {
		return identifier;
	}

	public void setIdentifier(int identifier) {
		this.identifier = identifier;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	
}
