package com.dotmarketing.portlets.templates.design.bean;

import java.io.Serializable;

import com.dotmarketing.portlets.containers.business.FileAssetContainerUtil;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Container link with a {@link TemplateLayout}, this have the UUID
 */
public class ContainerUUID implements Serializable{

    public static final String UUID_LEGACY_VALUE = "LEGACY_RELATION_TYPE";
    public static final String UUID_START_VALUE = "1";
    public static final String UUID_DEFAULT_VALUE = "-1";

    private final String identifier;
    private final String uuid;


    public ContainerUUID(final @JsonProperty("identifier") String containerIdentifier,
                         final @JsonProperty("uuid") String containerIdOrPath) {

        this.identifier = containerIdentifier;
        this.uuid = containerIdOrPath == null ? UUID_DEFAULT_VALUE : containerIdOrPath;
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
