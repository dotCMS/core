package com.dotmarketing.portlets.rules.parameter;

import com.dotcms.repackage.com.google.common.base.Preconditions;
import com.dotcms.repackage.org.apache.commons.lang.StringUtils;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.dotmarketing.portlets.rules.parameter.display.Input;
import com.dotmarketing.portlets.rules.parameter.type.DataType;

public class ParameterDefinition<T extends DataType> {

    private final String key;
    private final String defaultValue;
    private final Input<T> inputType;
    private final int priority;

    /**
     * Creates a parameter with the key, data type and a default value
     */
    public ParameterDefinition(String key, Input<T> inputType, int priority) {
        this(key, inputType, priority, "");
    }
    public ParameterDefinition(String key, Input<T> inputType, int priority, String defaultValue) {
        Preconditions.checkState(StringUtils.isNotBlank(key), "ParameterDefinition requires a valid key.");
        this.key = key;
        this.defaultValue = defaultValue == null ? "" : defaultValue;
        this.inputType = inputType;
        this.priority = priority;
    }

    public String getKey() {
        return key;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public int getPriority() {
        return priority;
    }

    public Input<T> getInputType() {
        return inputType;
    }

    public void checkValid(ParameterModel model) {
        this.inputType.getDataType().checkValid(model.getValue());
        model.getValue();
    }
}

