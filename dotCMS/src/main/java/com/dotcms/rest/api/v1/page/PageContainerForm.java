package com.dotcms.rest.api.v1.page;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableList;
import com.dotcms.rest.exception.BadRequestException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * {@link PageResource#addContent(HttpServletRequest, String, PageContainerForm)}'s form
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

    static final class ContainerEntry {

        private final String personaTag;
        private final String id;
        private final String uuid;
        private final List<String> contentIds;

        public ContainerEntry(final String personaTag, final String id, final String uuid) {
            this.id = id;
            this.uuid = uuid;
            this.personaTag  = personaTag;
            contentIds = new ArrayList<>();
        }

        public String getPersonaTag() {
            return personaTag;
        }

        public String getContainerId() {
            return id;
        }

        public List<String> getContentIds() {
            return contentIds;
        }

        public void addContentId(final String contentId) {
            this.contentIds.add(contentId);
        }

        public String getContainerUUID() {
            return uuid;
        }
    }
}
