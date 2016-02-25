package com.dotmarketing.portlets.rules.parameter.display;

import com.dotcms.rest.exception.InvalidRuleParameterException;
import com.dotmarketing.portlets.rules.parameter.type.DataType;

/**
 * @author Geoff M. Granum
 */
public class Input<T extends DataType> {

    private final String id;
    private final T dataType;

    public Input(String id, T dataType) {
        this.id = id;
        this.dataType = dataType;
    }

    public String getId() {
        return id;
    }

    public T getDataType() {
        return dataType;
    }

    /**
     * Validates the parameter context for the conditionlet. Each input will implement this validation if required.
     * @param value parameter value
     * @throws InvalidRuleParameterException
     */
    public void checkValid(String value)  throws InvalidRuleParameterException{
    	return;
    }
}

