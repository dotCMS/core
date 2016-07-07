package com.dotcms.contenttype.model.type;


public enum BaseContentTypes {
	NONE(0),
	CONTENT(1),
	WIDGET(2),
	FORM(3),
	FILEASSET(4),
	HTMLPAGE(5),
	PERSONA(6),
	HOST(7),
	EVENT(8);

	private int type;

	BaseContentTypes(int type) {
		this.type = type;
	}

	/**
	 * Gets the integer representation of this value.
	 * @return the integer representation
     */
	public int getType() {
		return type;
	}
	
	public static BaseContentTypes getBaseContentType (int value) {
		BaseContentTypes[] types = BaseContentTypes.values();
		for (BaseContentTypes type : types) {
			if (type.type==value){
				return type;
			}
		}
		return NONE;
	}
}
