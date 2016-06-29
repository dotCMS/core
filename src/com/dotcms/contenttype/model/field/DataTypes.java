package com.dotcms.contenttype.model.field;


public enum DataTypes {


		BOOL("bool"),
		DATE("date"),
		FLOAT("float"),
		INTEGER("integer"),
		TEXT("text"),
		LONG_TEXT("text_area"),
		SECTION_DIVIDER("section_divider"),
		BINARY("binary");

		private final String value;

		DataTypes (String value) {
			this.value = value;
		}

		public String toString () {
			return value;
		}

		public static DataTypes getDataType (String value) {
			return DataTypes.valueOf(value);
		}

	}

