package com.dotcms.rest;

import javax.validation.constraints.NotNull;
import com.dotcms.rest.api.Validated;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Encapsulates the data of an environment
 * @author jsanca
 */
public class EnvironmentForm  extends Validated implements java.io.Serializable {

    @NotNull
    @JsonProperty("name")
    private final String name;

    @NotNull
    @JsonProperty("pushMode")
    private final PushMode pushMode;

    @JsonProperty("whoCanSend")
    private final List<String> whoCanSend;
    @JsonCreator
    public EnvironmentForm(@JsonProperty("name")      final String name,
                           @JsonProperty("pushMode")  final PushMode pushType,
                           @JsonProperty("whoCanSend") final List<String> whoCanSend
                           ) {
        this.name = name;
        this.whoCanSend = whoCanSend;
        this.pushMode = pushType;
        checkValid();
    }

    public String getName() {
        return name;
    }

    public List<String> getWhoCanSend() {
        return whoCanSend;
    }

    public PushMode getPushMode() {

        return pushMode;
    }
}
