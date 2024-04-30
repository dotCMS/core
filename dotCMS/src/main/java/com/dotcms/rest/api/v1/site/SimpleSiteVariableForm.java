package com.dotcms.rest.api.v1.site;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SimpleSiteVariableForm {

    private final String id;
    private final String name;
    private final String key;
    private final String value;

    @JsonCreator
    public SimpleSiteVariableForm(@JsonProperty("id") final String id,
            @JsonProperty("name") final String name,
            @JsonProperty("key") final String key,
            @JsonProperty("value") final String value) {

        this.id = id;
        this.name = name;
        this.key = key;
        this.value = value;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "SiteVariableForm{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", key='" + key + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
