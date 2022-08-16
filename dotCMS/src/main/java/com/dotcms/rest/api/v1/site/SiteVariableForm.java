package com.dotcms.rest.api.v1.site;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Form to create a site variable
 * @author jsanca
 */
public class SiteVariableForm {

    private final String id;
    private final String siteId;
    private final String name;
    private final String key;
    private final String value;

    @JsonCreator
    public SiteVariableForm(@JsonProperty("id")     final String id,
                            @JsonProperty("siteId") final String siteId,
                            @JsonProperty("name")   final String name,
                            @JsonProperty("key")    final String key,
                            @JsonProperty("value")  final String value) {

        this.id = id;
        this.siteId = siteId;
        this.name = name;
        this.key = key;
        this.value = value;
    }

    public String getId() {
        return id;
    }

    public String getSiteId() {
        return siteId;
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
                ", siteId='" + siteId + '\'' +
                ", name='" + name + '\'' +
                ", key='" + key + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
