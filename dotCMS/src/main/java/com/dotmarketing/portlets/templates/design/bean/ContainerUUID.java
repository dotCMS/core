package com.dotmarketing.portlets.templates.design.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.Serializable;

/**
 * Provides the relationship between a Container and its instance ID in a Template's layout in
 * dotCMS. The Container's instance ID is basically the mechanism that allows content authors
 * to add the same Container in a Template more than once.
 *
 * @author Freddy Rodriguez
 * @since Jan 11th, 2018
 */
public class ContainerUUID implements Serializable{

    public static final String UUID_LEGACY_VALUE = "LEGACY_RELATION_TYPE";
    public static final String UUID_START_VALUE = "1";
    public static final String UUID_DEFAULT_VALUE = "-1";

    private final String identifier;
    private String uuid;

    public ContainerUUID(final @JsonProperty("identifier") String containerIdOrPath,
                         final @JsonProperty("uuid") String containerInstanceID) {

        this.identifier = containerIdOrPath;
        this.uuid = containerInstanceID == null ? UUID_DEFAULT_VALUE : containerInstanceID;
    }

    /**
     * Returns the Container's identifier, or its file path in case it's a Container as File.
     *
     * @return The Container's identifier or file path.
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Returns the Container's instance ID in a Template's layout.
     *
     * @return The Container's instance ID.
     */
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

    /**
     * Sets the Container's instance ID for a given Template's layout.
     *
     * @param uuid The Container's instance ID.
     */
    public void setUuid(final String uuid){
        this.uuid = uuid;
    }

}
