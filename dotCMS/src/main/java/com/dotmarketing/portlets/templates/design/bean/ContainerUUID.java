package com.dotmarketing.portlets.templates.design.bean;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Container link with a {@link TemplateLayout}, this have the UUID
 */
public class ContainerUUID {

    private final String identifier;
    private final String uuid;


    public ContainerUUID(final @JsonProperty("identifier") String containerIdentifier,
                         final @JsonProperty("uuid") String containerUUID) {

        this.identifier = containerIdentifier;
        this.uuid = containerUUID;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getUUID() {
        return uuid;
    }
}
