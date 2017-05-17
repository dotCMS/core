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

	DataTypes (String value) {
		this.value = value;
	}

	public String toString () {
		return value;
	}

	public static DataTypes getDataType (String value) {
		if (value.isEmpty() || value.contains("_divider") || value.contains("binary") || value.contains("_tab") || value
			.contains("constant")) {
			return SYSTEM;
	    }
		DataTypes[] types = DataTypes.values();
		for (DataTypes type : types) {
			if (type.value.equals(value))
				return type;
		}
		return SYSTEM;
	}

}
