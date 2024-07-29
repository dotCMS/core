package com.dotcms.ai.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class OpenAIModel implements Serializable {

    private final String id;
    private final String object;
    private final long created;
    private final String ownedBy;

    @JsonCreator
    public OpenAIModel(@JsonProperty("id") final String id,
                       @JsonProperty("object") final String object,
                       @JsonProperty("created") final long created,
                       @JsonProperty("owned_by") final String ownedBy) {
        this.id = id;
        this.object = object;
        this.created = created;
        this.ownedBy = ownedBy;
    }

    public String getId() {
        return id;
    }

    public String getObject() {
        return object;
    }

    public long getCreated() {
        return created;
    }

    @JsonProperty("owned_by")
    public String getOwnedBy() {
        return ownedBy;
    }

}
