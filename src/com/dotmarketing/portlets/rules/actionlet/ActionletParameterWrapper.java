package com.dotmarketing.portlets.rules.actionlet;

import java.util.HashMap;
import java.util.Map;

public class ActionletParameterWrapper{

	private String key;
	private String value;
	private DataType datatType;

	/**
	 * Creates a simple parameter constructor using only the parameter key, the default type is text and no default value
	 * @param key
	 */
	public ActionletParameterWrapper(String key){
		this.key = key;
		this.datatType = DataType.TEXT;
	}

	/**
	 * Creates a parameter with the proper key and data type, no default value
	 * @param key
	 * @param dataType
	 */
	public ActionletParameterWrapper(String key, DataType dataType){
		this.key = key;
		this.datatType = dataType;
	}

	/**
	 * Creates a parameter with the key, data type and a default value
	 * @param key
	 * @param datatype
	 * @param value
	 */
	public ActionletParameterWrapper(String key, DataType datatype, String value){
		this.key = key;
		this.value = value;
		this.datatType = datatype;
	}

	public String getKey(){
		return key;
	}

	public String getValue(){
		return value;
	}

	public String getDataType(){
		return datatType.toString();
	}

	public Map<String,String> toMappedValues(){
		Map<String,String> mappedValues = new HashMap<String,String>();
		mappedValues.put("key", key);
		mappedValues.put("value", (value!=null)?value:"");
		mappedValues.put("dataType", datatType.toString());
		return mappedValues;
	}

    public enum DataType{
    	TEXT("text"),NUMERIC("numeric");

    	private final String type;

        private DataType(String s) {
            type = s;
        }

        public boolean equalsName(String otherType) {
            return (otherType == null) ? false : type.equals(otherType);
        }

        @Override
        public String toString() {
           return this.type;
        }
    }
}

