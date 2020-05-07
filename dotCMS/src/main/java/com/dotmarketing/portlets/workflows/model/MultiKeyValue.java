package com.dotmarketing.portlets.workflows.model;

public class MultiKeyValue {

    private final String key;
    private final String value;

    public MultiKeyValue(final String key, final String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }
}
