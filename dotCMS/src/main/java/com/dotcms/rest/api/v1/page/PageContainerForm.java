package com.dotcms.rest.api.v1.page;

import com.dotcms.rest.exception.BadRequestException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
                entries.add(containerEntry);
            }

            return new PageContainerForm(entries, jsonNode.toString());
        }
    }
}
