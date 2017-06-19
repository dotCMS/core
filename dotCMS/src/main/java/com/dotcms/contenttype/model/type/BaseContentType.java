package com.dotcms.contenttype.model.type;

import com.fasterxml.jackson.annotation.JsonValue;

public enum BaseContentType {
	ANY(0, ContentType.class),
	CONTENT(1, SimpleContentType.class),
	WIDGET(2, WidgetContentType.class),
	FORM(3,FormContentType.class),
	FILEASSET(4, FileAssetContentType.class),
	HTMLPAGE(5, PageContentType.class),
	PERSONA(6,PersonaContentType.class),
	KEY_VALUE(7, KeyValueContentType.class);

	final int type;
	Class immutableClass;
	
	BaseContentType(int type, Class clazz) {
		this.type = type;
		this.immutableClass=clazz;
	}

	/**
	 * Gets the integer representation of this value.
	 * @return the integer representation
     */
	@JsonValue
	public int getType() {
		return type;
	}

	public Class immutableClass() {
		return immutableClass;
	}
	public static BaseContentType getBaseContentType (int value) {
		BaseContentType[] types = BaseContentType.values();
		for (BaseContentType type : types) {
			if (type.type==value){
				return type;
			}
		}
		return ANY;
	}
	
	public static Class getContentTypeClass (int value) {
		BaseContentType[] types = BaseContentType.values();
		for (BaseContentType type : types) {
			if (type.type==value){
				return type.immutableClass;
			}
		}
		return ANY.immutableClass;
	}
}
