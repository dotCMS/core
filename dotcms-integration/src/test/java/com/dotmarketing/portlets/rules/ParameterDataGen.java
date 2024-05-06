package com.dotmarketing.portlets.rules;

import com.dotmarketing.portlets.rules.model.ParameterModel;

/**
 * @author Geoff M. Granum
 */
public class ParameterDataGen {

    private String key = "key";
    private String value = "value";
    private String ownerId;

    public ParameterDataGen() {
    }

    public ParameterModel next() {
        ParameterModel next = new ParameterModel();
        next.setKey(key);
        next.setValue(value);
        next.setOwnerId(ownerId);
        return next;
    }

    public ParameterDataGen key(String key) {
        this.key = key;
        return this;
    }

    public ParameterDataGen value(String value) {
        this.value = value;
        return this;
    }

    public ParameterDataGen ownerId(String id) {
        this.ownerId = id;
        return this;
    }

}
 
