package com.dotmarketing.portlets.rules.conditionlet;

import com.dotmarketing.portlets.rules.model.ConditionValue;
import com.dotmarketing.portlets.rules.model.RuleActionParameter;

/**
 * @author Geoff M. Granum
 */
public class ConditionValueDataGen {

    private String key = "key";
    private String value = "value";
    private String ownerId;

    public ConditionValueDataGen() {
    }

    public ConditionValue next() {
        ConditionValue next = new ConditionValue();
        next.setKey(key);
        next.setValue(value);
        next.setConditionId(ownerId);
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
 
