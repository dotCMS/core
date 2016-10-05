package com.dotcms.contenttype.model.field;



public enum DataTypes {
	NONE("none"),
	BOOL("bool"),
	DATE("date"),
	FLOAT("float"),
	INTEGER("integer"),
	TEXT("text"),
	LONG_TEXT("text_area"),
	SECTION_DIVIDER("section_divider"),
	CONSTANT("constant"),
	SYSTEM("system_field"),
	BINARY("binary");

	final String value;

	DataTypes (String value) {
		this.value = value;
	}

	public String toString () {
		return value;
	}

	public static DataTypes getDataType (String value) {
		DataTypes[] types = DataTypes.values();
		for (DataTypes type : types) {
			if (type.value.equals(value))
				return type;
		}
		return null;
	}

}
