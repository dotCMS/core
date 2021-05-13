package com.dotcms.rest.api.v1.template;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.Map;

public class TemplateLayoutView {

    private final String      width;
    private final String      title;
    private final boolean     header;
    private final boolean     footer;
    private final BodyView    body;
    private final SidebarView sidebar;

    @JsonCreator
    public TemplateLayoutView(@JsonProperty("width")   final String width,
                              @JsonProperty("title")   final String title,
                              @JsonProperty("header")  final boolean header,
                              @JsonProperty("footer")  final boolean footer,
                              @JsonProperty("body")    final BodyView body,
                              @JsonProperty("sidebar") final SidebarView sidebar) {
        this.width   = width;
        this.title   = title;
        this.header  = header;
        this.footer  = footer;
        this.body    = body;
        this.sidebar = sidebar;
    }

    public String getWidth() {
        return width;
    }

    public String getTitle() {
        return title;
    }

    public boolean isHeader() {
        return header;
    }

    public boolean isFooter() {
        return footer;
    }

    public BodyView getBody() {
        return body;
    }

    public SidebarView getSidebar() {
        return sidebar;
    }

    /**
     * This method iterates over each container in the template and assign a UUID according the
     * amount of times the container is present in the template.
     * This is for the users can have 2 diff templates with diff design but the same containers
     * so they can change the template without losing the content.
     *
     * TemplateLayoutView with the updated UUID for each container.
     */
    protected void updateUUIDOfContainers(){
        final Map<String,Integer> UUIDByContainermap = new HashMap<>();
        this.getBody().getRows().forEach(
                row -> row.getColumns().stream().forEach(
                        column -> column.getContainers().stream().forEach(
                                containerUUID -> {
                                    final String containerID = containerUUID.getIdentifier();
                                    int value = UUIDByContainermap.getOrDefault(containerID,0);
                                    UUIDByContainermap.put(containerID,++value);
                                    containerUUID.setUuid(UUIDByContainermap.get(containerID).toString());
                                }
                        )));

        if(this.getSidebar()!=null) {
            this.getSidebar().getContainers().stream().forEach(
                    containerUUID -> {
                        final String containerID = containerUUID.getIdentifier();
                        int value = UUIDByContainermap.getOrDefault(containerID, 0);
                        UUIDByContainermap.put(containerID, ++value);
                        containerUUID.setUuid(UUIDByContainermap.get(containerID).toString());
                    }
            );
        }
    }
}
