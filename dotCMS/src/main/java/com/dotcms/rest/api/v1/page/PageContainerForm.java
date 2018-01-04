package com.dotcms.rest.api.v1.page;

import com.dotcms.repackage.com.fasterxml.jackson.core.JsonParser;
import com.dotcms.repackage.com.fasterxml.jackson.core.JsonProcessingException;
import com.dotcms.repackage.com.fasterxml.jackson.databind.DeserializationContext;
import com.dotcms.repackage.com.fasterxml.jackson.databind.JsonDeserializer;
import com.dotcms.repackage.com.fasterxml.jackson.databind.JsonNode;
import com.dotcms.repackage.com.fasterxml.jackson.databind.ObjectMapper;
import com.dotcms.repackage.com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * {@link PageResource#addContent(HttpServletRequest, HttpServletResponse, String)}'s form
 */
@JsonDeserialize(using = PageContainerForm.ContainerDeserialize.class)
public class PageContainerForm {

    private List<ContainerEntry> entries;
    private String requestJson;

    public PageContainerForm(List<ContainerEntry> entries, String s) {
        this.entries = entries;
    }

    public List<ContainerEntry> getContainerEntries() {
        return entries;
    }

    public String getRequestJson() {
        return requestJson;
    }

    static final class ContainerDeserialize extends JsonDeserializer<PageContainerForm> {

        @Override
        public PageContainerForm deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
                throws IOException {

            JsonNode jsonNode = jsonParser.readValueAsTree();
            List<ContainerEntry> entries = new ArrayList<>();

            Iterator<String> containerIds = jsonNode.fieldNames();

            while (containerIds.hasNext()) {
                String containerId = containerIds.next();
                ContainerEntry containerEntry = new ContainerEntry(containerId);

                JsonNode containerNode = jsonNode.get(containerId);

                containerNode.forEach(contentId -> containerEntry.addContentId(contentId.textValue()));
                entries.add(containerEntry);
            }

            return new PageContainerForm(entries, jsonParser.readValueAsTree().toString());
        }
    }

    static final class ContainerEntry {
        private String containerId;
        private List<String> contentIds = new ArrayList<>();

        public ContainerEntry(String containerId) {
            this.containerId = containerId;
        }

        public String getContainerId() {
            return containerId;
        }

        public List<String> getContentIds() {
            return contentIds;
        }

        public void addContentId(String contentId) {
            this.contentIds.add(contentId);
        }
    }
}
