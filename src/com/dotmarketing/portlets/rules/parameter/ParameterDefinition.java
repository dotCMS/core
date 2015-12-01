package com.dotmarketing.portlets.rules.parameter;

import com.dotcms.repackage.org.apache.commons.lang.StringUtils;
import com.dotmarketing.portlets.rules.parameter.display.DropdownInput;
import com.dotmarketing.portlets.rules.parameter.type.DataType;
import com.dotmarketing.portlets.rules.parameter.type.TextType;

public class ParameterDefinition {

    private final String key;
    private final String defaultValue;
    private final DataType dataType;

    public ParameterDefinition(String key) {
        this(key, new TextType(), "");
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
        this.dataType = datatype;
    }

    public ParameterDefinition(String headerNameKey, DataType dataType, DropdownInput input) {

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

