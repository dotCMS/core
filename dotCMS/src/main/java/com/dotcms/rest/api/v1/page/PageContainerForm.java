package com.dotcms.rest.api.v1.page;

import com.dotcms.rest.exception.BadRequestException;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class contains all the Contentlets that make up an HTML Page. It provides the list of Containers in it, and the
 * sub-list of Contentlets in each of them.
 *
 * @author Freddy Rodriguez
 * @since Jan 11th, 2018
 */
@JsonDeserialize(using = PageContainerForm.ContainerDeserialize.class)
public class PageContainerForm {

    private final List<ContainerEntry> entries;
    private final String requestJson;

    public PageContainerForm(final List<ContainerEntry> entries, final String requestJson) {
        this.entries     = ImmutableList.copyOf(entries);
        this.requestJson = requestJson;
    }

    public List<ContainerEntry> getContainerEntries() {
        return entries;
    }

    public String getRequestJson() {
        return requestJson;
    }

    /**
     * This is a custom JSON deserializer for the data stored in the {@link PageContainerForm} class.
     */
    static final class ContainerDeserialize extends JsonDeserializer<PageContainerForm> {

        private static final String CONTAINER_ID_ATTRIBUTE_NAME = "identifier";
        private static final String CONTAINER_UUID_ATTRIBUTE_NAME = "uuid";
        private static final String CONTAINER_PERSONA_TAG_ATTRIBUTE_NAME = "personaTag";
        private static final String CONTAINER_CONTENTLETSID_ATTRIBUTE_NAME = "contentletsId";
        private static final String STYLE_PROPERTIES_ATTRIBUTE_NAME = "styleProperties";

        @Override
        public PageContainerForm deserialize(final JsonParser jsonParser,
                                             final DeserializationContext deserializationContext)
                throws IOException {

            final JsonNode jsonNode = jsonParser.readValueAsTree();
            final List<ContainerEntry> entries = new ArrayList<>();

            for (final JsonNode jsonElement : jsonNode) {
                final JsonNode containerIdNode   = jsonElement.get(CONTAINER_ID_ATTRIBUTE_NAME);
                final JsonNode containerUUIDNode = jsonElement.get(CONTAINER_UUID_ATTRIBUTE_NAME);
                final JsonNode personaTagNode    = jsonElement.get(CONTAINER_PERSONA_TAG_ATTRIBUTE_NAME);

                if (containerIdNode == null || containerUUIDNode == null) {
                    throw new BadRequestException("Container id and uuid are required");
                }

                final String containerId    = containerIdNode.asText();
                final String containerUUID  = containerUUIDNode.asText();
                final String personaTag     = personaTagNode != null? personaTagNode.asText():null;
                final ContainerEntry containerEntry = new ContainerEntry(personaTag, containerId, containerUUID);
                final JsonNode contentletsNode = jsonElement.get(CONTAINER_CONTENTLETSID_ATTRIBUTE_NAME);

                if (contentletsNode == null) {
                    throw new BadRequestException("Contentlets Ids are required");
                }

                contentletsNode.forEach((JsonNode contentId) -> containerEntry.addContentId(contentId.textValue()));

                // Parse styleProperties for each contentlet (optional field)
                final JsonNode stylePropertiesNode = jsonElement.get(STYLE_PROPERTIES_ATTRIBUTE_NAME);
                if (stylePropertiesNode != null && stylePropertiesNode.isObject()) {
                    stylePropertiesNode.fields().forEachRemaining(entry -> {
                        final String contentletId = entry.getKey();
                        final JsonNode styleProps = entry.getValue();
                        if (styleProps != null && styleProps.isObject()) {
                            final Map<String, Object> propsMap = new HashMap<>();
                            styleProps.fields().forEachRemaining(prop -> {
                                final JsonNode propValue = prop.getValue();
                                // Handle different JSON value types
                                if (propValue.isTextual()) {
                                    propsMap.put(prop.getKey(), propValue.asText());
                                } else if (propValue.isNumber()) {
                                    propsMap.put(prop.getKey(), propValue.numberValue());
                                } else if (propValue.isBoolean()) {
                                    propsMap.put(prop.getKey(), propValue.asBoolean());
                                } else {
                                    propsMap.put(prop.getKey(), propValue.toString());
                                }
                            });
                            containerEntry.setStyleProperties(contentletId, propsMap);
                        }
                    });
                }

                entries.add(containerEntry);
            }

            return new PageContainerForm(entries, jsonNode.toString());
        }
    }

    /**
     * Holds the data received from the UI or the REST Endpoint related to the Contentlets that are referenced in a
     * Container.
     */
    static final class ContainerEntry {

        private final String personaTag;
        private final String id;
        private final String uuid;
        private final List<String> contentIds;
        private final Map<String, Map<String, Object>> stylePropertiesMap;

        public ContainerEntry(final String personaTag, final String id, final String uuid) {
            this.id = id;
            this.uuid = uuid;
            this.personaTag  = personaTag;
            this.contentIds = new ArrayList<>();
            this.stylePropertiesMap = new HashMap<>();
        }

        public ContainerEntry(final String personaTag, final String id, final String uuid,
                final List<String> contentIds,
                final Map<String, Map<String, Object>> stylePropertiesMap) {
            this.id = id;
            this.uuid = uuid;
            this.personaTag  = personaTag;

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

        public void setStyleProperties(final String contentletId, final Map<String, Object> styleProperties) {
            if (styleProperties == null) {
                this.stylePropertiesMap.remove(contentletId);
                return;
            }
            this.stylePropertiesMap.put(contentletId, new HashMap<>(styleProperties));
        }

    }

}
