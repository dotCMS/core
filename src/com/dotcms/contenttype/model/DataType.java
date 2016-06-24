package com.dotcms.contenttype.model;


public enum DataType {


		BOOL("bool"),
		DATE("date"),
		FLOAT("float"),
		INTEGER("integer"),
		TEXT("text"),
		LONG_TEXT("text_area"),
		SECTION_DIVIDER("section_divider"),
		BINARY("binary");

		private final String value;

		DataType (String value) {
			this.value = value;
		}

		public String toString () {
			return value;
		}

		public static DataType getDataType (String value) {
			return DataType.valueOf(value);
		}

	}

