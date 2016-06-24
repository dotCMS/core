package com.dotcms.contenttype.model;

public enum BaseContentType {
	NONE(0),
	CONTENT(1),
	WIDGET(2),
	FORM(3),
	FILEASSET(4),
	HTMLPAGE(5),
	PERSONA(6);

	private int type;

	BaseContentType(int type) {
		this.type = type;
	}

	/**
	 * Gets the integer representation of this value.
	 * @return the integer representation
     */
	public int getType() {
		return type;
	}
}
