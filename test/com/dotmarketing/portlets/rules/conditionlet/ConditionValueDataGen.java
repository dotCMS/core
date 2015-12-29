package com.dotmarketing.portlets.rules.conditionlet;

import com.dotmarketing.portlets.rules.model.ParameterModel;

/**
 * @author Geoff M. Granum
 */
public class ConditionValueDataGen {

    private String key = "key";
    private String value = "value";
    private String ownerId;

    public ConditionValueDataGen() {
    }

    public ParameterModel next() {
        ParameterModel next = new ParameterModel();
        next.setKey(key);
        next.setValue(value);
        next.setOwnerId(ownerId);
        return next;
    }

    public ConditionValueDataGen key(String key) {
        this.key = key;
        return this;
    }

    public ConditionValueDataGen value(String value) {
        this.value = value;
        return this;
    }

    public ConditionValueDataGen ownerId(String id) {
        this.ownerId = id;
        return this;
    }

}
 
