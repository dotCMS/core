package com.dotcms.contenttype.model.field;





public enum FieldTypes {
	BUTTON("button"),
	CHECKBOX("checkbox"),
	DATE("date"),
	TIME("time"),
	DATE_TIME("date_time"),
	RADIO("radio"),
	SELECT("select"),
	MULTI_SELECT("multi_select"),
	TEXT("text"),
	TEXT_AREA("textarea"),
	WYSIWYG("wysiwyg"),
	FILE("file"),
	IMAGE("image"),
	TAG("tag"),
	CONSTANT("constant"),
	CATEGORY("category"),
	LINE_DIVIDER("line_divider"),
	TAB_DIVIDER("tab_divider"),
	CATEGORIES_TAB("categories_tab"),
	PERMISSIONS_TAB("permissions_tab"),
	RELATIONSHIPS_TAB("relationships_tab"),
	HIDDEN("hidden"),
	BINARY("binary"), 
	CUSTOM_FIELD("custom_field"),
	HOST_OR_FOLDER("host or folder"),
	KEY_VALUE("key_value");

	private String value;

	FieldTypes (String value) {
		this.value = value;
	}

	public String toString () {
		return value;
	}



	public static FieldTypes getFieldType (String value) {
		FieldTypes[] types = FieldTypes.values();
		for (FieldTypes type : types) {
			if (type.value.equals(value))
				return type;
		}
		return null;
	}

}