package com.dotmarketing.portlets.templates.design.bean;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Container link with a {@link TemplateLayout}, this have the UUID
 */
public class ContainerUUID implements Serializable{

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

    @Override
    public String toString() {
       try {
           return new ObjectMapper().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            return super.toString();
        }
    }
}
