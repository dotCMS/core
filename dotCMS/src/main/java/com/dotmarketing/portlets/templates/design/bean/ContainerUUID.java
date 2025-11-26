package com.dotmarketing.portlets.templates.design.bean;


import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.dotcms.util.CollectionsUtils.list;

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

    /**x
     * History of Changes: This section lists all the UUIDs that have been assigned to this Container. For example,
     * if the history is ["3", "1", "4"], it means the Container was initially assigned UUID 3,
     * then changed to UUID 1 after a layout modification, and is currently assigned UUID 4.
     *
     * If the layout changes but the Container is not moved, the same UUID will be repeated in the history.
     * For instance, consider the following layout:
     *
     * Row 1: Container A, UUID 1, history: ["1"]
     * Row 2: Container A, UUID 2, history: ["2"]
     * If a new row is added after the first one, the UUIDs and histories will be as follows:
     *
     * Row 1: Container A, UUID 1, history: ["1", "1"]
     * Row 2: Container A, UUID 2, history: ["2"]
     * Row 3: Container A, UUID 3, history: ["2", "3"]
     * The ["1", "1"] indicates that the layout changed, but this Container was not moved.
     */
    private List<String> historyUUIDs;

    public ContainerUUID(final String containerIdOrPath,
                         final String containerInstanceID) {

        this.identifier = containerIdOrPath;
        this.uuid = containerInstanceID == null ? UUID_DEFAULT_VALUE : containerInstanceID;
        this.historyUUIDs = isNew(containerInstanceID) ? new ArrayList<>() : list(this.uuid);
    }

    public ContainerUUID(final @JsonProperty("identifier") String containerIdOrPath,
                         final @JsonProperty("uuid") String containerInstanceID,
                         final @JsonProperty("historyUUIDs") List<String> historyUUIDs) {

        this.identifier = containerIdOrPath;
        this.uuid = containerInstanceID == null ? UUID_DEFAULT_VALUE : containerInstanceID;

        if (isNew(containerInstanceID)) {
            this.historyUUIDs = new ArrayList<>();
        } else {
            this.historyUUIDs = UtilMethods.isSet(historyUUIDs) ? new ArrayList<>(historyUUIDs) : list(this.uuid);
        }
    }

    private boolean isNew(String containerInstanceID) {
        return UUID_DEFAULT_VALUE.equals(containerInstanceID);
    }

    public void addUUIDTOHistory(final String uuid){
        if (uuid.equals(UUID_DEFAULT_VALUE)) {
            return;
        }

        historyUUIDs.add(uuid);
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

    public List<String> getHistoryUUIDs() {
        return Collections.unmodifiableList(this.historyUUIDs);
    }
}
