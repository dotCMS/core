package com.dotmarketing.portlets.rules.actionlet;

import com.dotcms.repackage.org.apache.commons.lang.StringUtils;
import java.util.HashMap;
import java.util.Map;

public class ActionParameterDefinition {

    private final String key;
    private final String defaultValue;
    private final DataType dataType;

    public ActionParameterDefinition(String key) {
        this(key, DataType.TEXT, "");
    }

    public ActionParameterDefinition(String key, DataType dataType) {
        this(key, dataType, "");
    }

    /**
     * Creates a parameter with the key, data type and a default value
     */
    public ActionParameterDefinition(String key, DataType datatype, String defaultValue) {
        if(StringUtils.isEmpty(key)){
            throw new IllegalStateException("ActionParameterDefinition requires a valid key.");
        }
        this.key = key;
        this.defaultValue = defaultValue == null ? "" : defaultValue;
        this.dataType = datatype == null ? DataType.TEXT : datatype;
    }


    public String getKey() {
        return key;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public String getDataType() {
        return dataType.toString();
    }

    public Map<String, String> toMappedValues() {
        Map<String, String> mappedValues = new HashMap<>();
        mappedValues.put("key", key);
        mappedValues.put("value", (defaultValue != null) ? defaultValue : "");
        mappedValues.put("dataType", dataType.toString());
        return mappedValues;
    }

    public enum DataType {
        TEXT("text"),
        NUMERIC("numeric");

        private final String type;

        DataType(String s) {
            type = s;
        }

        public boolean equalsName(String otherType) {
            return otherType != null && type.equals(otherType);
        }

        @Override
        public String toString() {
            return this.type;
        }
    }
}

