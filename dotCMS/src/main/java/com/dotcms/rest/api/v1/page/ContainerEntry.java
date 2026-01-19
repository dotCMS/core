package com.dotcms.rest.api.v1.page;

import com.dotmarketing.util.UtilMethods;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Holds the data received from the UI or the REST Endpoint related to the Contentlets that are
 * referenced in a Container.
 */
public class ContainerEntry {

    private final String personaTag;
    private final String id;
    private final String uuid;
    private final List<String> contentIds;
    private final Map<String, Map<String, Object>> stylePropertiesMap;

    public ContainerEntry(final String personaTag, final String id, final String uuid) {
        this.id = id;
        this.uuid = uuid;
        this.personaTag = personaTag;
        this.contentIds = new ArrayList<>();
        this.stylePropertiesMap = new HashMap<>();
    }

    public ContainerEntry(final String personaTag, final String id, final String uuid,
            final List<String> contentIds) {
        this.id = id;
        this.uuid = uuid;
        this.personaTag = personaTag;
        this.contentIds = UtilMethods.isSet(contentIds) ? new ArrayList<>(contentIds) : new ArrayList<>();
        this.stylePropertiesMap = new HashMap<>();
    }

    public ContainerEntry(final String personaTag, final String id, final String uuid,
            final List<String> contentIds,
            final Map<String, Map<String, Object>> stylePropertiesMap) {
        this.id = id;
        this.uuid = uuid;
        this.personaTag = personaTag;

        // Defensive copy of contentIds
        this.contentIds = UtilMethods.isSet(contentIds) ? new ArrayList<>(contentIds) : new ArrayList<>();

        // Defensive deep copy of style properties
        this.stylePropertiesMap = new HashMap<>();
        if (UtilMethods.isSet(stylePropertiesMap)) {
            stylePropertiesMap.forEach((key, value) -> {
                this.stylePropertiesMap.put(key, new HashMap<>(value));
            });
        }
    }

    public String getPersonaTag() {
        return personaTag;
    }

    public String getContainerId() {
        return id;
    }

    public List<String> getContentIds() {
        return Collections.unmodifiableList(contentIds);
    }

    public void addContentId(final String contentId) {
        this.contentIds.add(contentId);
    }

    public String getContainerUUID() {
        return uuid;
    }

    public Map<String, Map<String, Object>> getStylePropertiesMap() {
        return Collections.unmodifiableMap(stylePropertiesMap);
    }

    public void setStyleProperties(final String contentletId,
            final Map<String, Object> styleProperties) {
        if (styleProperties == null) {
            this.stylePropertiesMap.remove(contentletId);
            return;
        }
        this.stylePropertiesMap.put(contentletId, new HashMap<>(styleProperties));
    }
}
