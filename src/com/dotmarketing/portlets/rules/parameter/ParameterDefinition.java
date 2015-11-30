package com.dotmarketing.portlets.rules.parameter;

import com.dotcms.repackage.org.apache.commons.lang.StringUtils;
import com.dotmarketing.portlets.rules.parameter.type.DataType;

public class ParameterDefinition {

    private final String key;
    private final String defaultValue;
    private final DataType dataType;

    public ParameterDefinition(String key) {
        this(key, DataType.TEXT, "");
    }

    public ParameterDefinition(String key, DataType dataType) {
        this(key, dataType, "");
    }

    /**
     * Creates a parameter with the key, data type and a default value
     */
    public ParameterDefinition(String key, DataType datatype, String defaultValue) {
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

    public DataType getDataType() {
        return dataType;
    }


}

