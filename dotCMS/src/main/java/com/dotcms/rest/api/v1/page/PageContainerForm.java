package com.dotcms.rest.api.v1.page;

import com.dotcms.repackage.com.fasterxml.jackson.core.JsonParser;
import com.dotcms.repackage.com.fasterxml.jackson.databind.DeserializationContext;
import com.dotcms.repackage.com.fasterxml.jackson.databind.JsonDeserializer;
import com.dotcms.repackage.com.fasterxml.jackson.databind.JsonNode;
import com.dotcms.repackage.com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.dotcms.repackage.jersey.repackaged.com.google.common.collect.ImmutableList;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * {@link PageResource#addContent(HttpServletRequest, HttpServletResponse, String)}'s form
 */
@JsonDeserialize(using = PageContainerForm.ContainerDeserialize.class)
public class PageContainerForm {

    private final List<ContainerEntry> entries;
    private final String requestJson;

    public PageContainerForm(final List<ContainerEntry> entries, final String requestJson) {
        this.entries = ImmutableList.copyOf(entries);
        this.requestJson = requestJson;
    }

    public List<ContainerEntry> getContainerEntries() {
        return entries;
    }

    public String getRequestJson() {
        return requestJson;
    }

    static final class ContainerDeserialize extends JsonDeserializer<PageContainerForm> {

        private static final String CONTAINER_ID_ATTRIBUTE_NAME = "id";
        private static final String CONTAINER_UUID_ATTRIBUTE_NAME = "uuid";
        private static final String CONTAINER_CONTENTLETSID_ATTRIBUTE_NAME = "contentlets";

        @Override
        public PageContainerForm deserialize(final JsonParser jsonParser,
                                             final DeserializationContext deserializationContext)
                throws IOException {

            final JsonNode jsonNode = jsonParser.readValueAsTree();
            final List<ContainerEntry> entries = new ArrayList<>();

            for (final JsonNode jsonElement : jsonNode) {
                final String containerId = jsonElement.get(CONTAINER_ID_ATTRIBUTE_NAME).asText();
                final String containerUUID = jsonElement.get(CONTAINER_UUID_ATTRIBUTE_NAME).asText();
                final ContainerEntry containerEntry = new ContainerEntry(containerId, containerUUID);

                final JsonNode containerNode = jsonElement.get(CONTAINER_CONTENTLETSID_ATTRIBUTE_NAME);

                containerNode.forEach((JsonNode contentId) -> containerEntry.addContentId(contentId.textValue()));
                entries.add(containerEntry);
            }

            return new PageContainerForm(entries, jsonNode.toString());
        }
    }

    static final class ContainerEntry {
        private final String id;
        private final String uuid;
        private final List<String> contentIds;

        public ContainerEntry(final String id, final String uuid) {
            this.id = id;
            this.uuid = uuid;
            contentIds = new ArrayList<>();
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
