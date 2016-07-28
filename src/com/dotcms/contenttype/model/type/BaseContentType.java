package com.dotcms.contenttype.model.type;


public enum BaseContentTypes {
	ANY(0, ContentType.class),
	CONTENT(1, SimpleContentType.class),
	WIDGET(2, WidgetContentType.class),
	FORM(3,FormContentType.class),
	FILEASSET(4, FileAssetContentType.class),
	HTMLPAGE(5, PageContentType.class),
	PERSONA(6,PersonaContentType.class);

	int type;
	Class implClass;
	
	BaseContentTypes(int type, Class clazz) {
		this.type = type;
		this.implClass=clazz;
	}

	/**
	 * Gets the integer representation of this value.
	 * @return the integer representation
     */
	public int getType() {
		return type;
	}
	public Class implClass() {
		return implClass;
	}
	public static BaseContentTypes getBaseContentType (int value) {
		BaseContentTypes[] types = BaseContentTypes.values();
		for (BaseContentTypes type : types) {
			if (type.type==value){
				return type;
			}
		}
		return ANY;
	}
	
	public static Class getContentTypeClass (int value) {
		BaseContentTypes[] types = BaseContentTypes.values();
		for (BaseContentTypes type : types) {
			if (type.type==value){
				return type.implClass;
			}
		}
		return ANY.implClass;
	}
}
