package com.dotcms.rest.api.v1.system.ruleengine.actionlets;

/**
 * @author Geoff M. Granum
 */
public class RestActionParamDefinition {

    private final String defaultValue;
    private final String dataType;

    public RestActionParamDefinition(String defaultValue, String dataType) {
        this.defaultValue = defaultValue;
        this.dataType = dataType;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public String getDataType() {
        return dataType;
    }
}
 
