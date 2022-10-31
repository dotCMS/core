package com.dotcms.contenttype.model.field;

public enum DataTypes {
	NONE("none"),
	BOOL("bool"),
	DATE("date"),
	FLOAT("float"),
	INTEGER("integer"),
	TEXT("text"),
	LONG_TEXT("text_area"),
	SYSTEM("system_field");


	public final String value;

	DataTypes(String value) {
		this.value = value;
	}

	public String toString () {
		return value;
	}

}
