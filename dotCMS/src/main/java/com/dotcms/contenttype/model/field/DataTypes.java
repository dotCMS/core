package com.dotcms.contenttype.model.field;

import com.dotmarketing.util.UtilMethods;

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
		if (!UtilMethods.isSet(value) || value.contains("_divider") || value.contains("binary") || value.contains("_tab") || value
			.contains("constant")) {
			return SYSTEM;
	    }
		value = value.replaceAll("[0-9]", "");
		DataTypes[] types = DataTypes.values();
		for (DataTypes type : types) {
			if (type.value.equals(value.toLowerCase()))
				return type;
		}
		return SYSTEM;
	}

}
