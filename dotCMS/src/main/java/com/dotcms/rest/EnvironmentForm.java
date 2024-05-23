package com.dotcms.rest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Encapsulates the data of an environment
 * @author jsanca
 */
public class EnvironmentForm implements java.io.Serializable {

    @JsonProperty("name")
    private final String name;
    @JsonProperty("pushType")
    private final String pushType;

    @JsonProperty("whoCanUse")
    private final List<String> whoCanUse;
    @JsonCreator
    public EnvironmentForm(@JsonProperty("name")      final String name,
                           @JsonProperty("pushType")  final String pushType,
                           @JsonProperty("whoCanUse") final List<String> whoCanUse
                           ) {
        this.name = name;
        this.whoCanUse = whoCanUse;
        this.pushType = pushType;
    }

    public String getName() {
        return name;
    }

    public List<String> getWhoCanUse() {
        return whoCanUse;
    }

    public String getPushType() {

        return pushType;
    }
}
