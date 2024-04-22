package com.dotcms.rest.api.v1.template;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

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
}
