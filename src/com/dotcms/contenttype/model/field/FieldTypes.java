package com.dotcms.contenttype.model.field;




public enum FieldTypes {

	BUTTON,
	CHECKBOX,
	DATE,
	TIME,
	DATE_TIME,
	RADIO,
	SELECT,
	MULTI_SELECT,
	TEXT,
	TEXT_AREA,
	WYSIWYG,
	FILE,
	IMAGE,
	TAG,
	CONSTANT,
	CATEGORY,
	LINE_DIVIDER,
	TAB_DIVIDER,
	CATEGORIES_TAB,
	PERMISSIONS_TAB,
	RELATIONSHIPS_TAB,
	HIDDEN,
	BINARY, 
	CUSTOM_FIELD, 
	HOST_OR_FOLDER,
	KEY_VALUE;



	public String toString () {
		return this.name();
	}

	public FieldTypes getFieldType (String value) {
		return FieldTypes.valueOf(value);
	}


}