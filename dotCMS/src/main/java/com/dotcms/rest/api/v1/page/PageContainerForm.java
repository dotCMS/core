package com.dotcms.rest.api.v1.page;

import com.dotcms.repackage.com.fasterxml.jackson.core.JsonParser;
import com.dotcms.repackage.com.fasterxml.jackson.databind.DeserializationContext;
import com.dotcms.repackage.com.fasterxml.jackson.databind.JsonDeserializer;
import com.dotcms.repackage.com.fasterxml.jackson.databind.JsonNode;
import com.dotcms.repackage.com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * {@link PageResource#addContent(HttpServletRequest, HttpServletResponse, String)}'s form
 */
@JsonDeserialize(using = PageContainerForm.ContainerDeserialize.class)
public class PageContainerForm {

    private final List<ContainerEntry> entries;
    private final String requestJson;

    public PageContainerForm(final List<ContainerEntry> entries, final String requestJson) {
        this.entries = entries;
        this.requestJson = requestJson;
    }

    public List<ContainerEntry> getContainerEntries() {
        return entries;
    }

    public String getRequestJson() {
        return requestJson;
    }

    static final class ContainerDeserialize extends JsonDeserializer<PageContainerForm> {

        @Override
        public PageContainerForm deserialize(final JsonParser jsonParser,
                                             final DeserializationContext deserializationContext)
                throws IOException {

            final JsonNode jsonNode = jsonParser.readValueAsTree();
            final List<ContainerEntry> entries = new ArrayList<>();

            final Iterator<String> containerIds = jsonNode.fieldNames();

            while (containerIds.hasNext()) {
                final String containerId = containerIds.next();
                final ContainerEntry containerEntry = new ContainerEntry(containerId);

                final JsonNode containerNode = jsonNode.get(containerId);

                containerNode.forEach(contentId -> containerEntry.addContentId(contentId.textValue()));
                entries.add(containerEntry);
            }

            return new PageContainerForm(entries, jsonParser.readValueAsTree().toString());
        }
    }

    static final class ContainerEntry {
        private final String containerId;
        private final List<String> contentIds = new ArrayList<>();

        public ContainerEntry(final String containerId) {
            this.containerId = containerId;
        }

        public String getContainerId() {
            return containerId;
        }

        public List<String> getContentIds() {
            return contentIds;
        }

        public void addContentId(final String contentId) {
            this.contentIds.add(contentId);
        }
    }
}
