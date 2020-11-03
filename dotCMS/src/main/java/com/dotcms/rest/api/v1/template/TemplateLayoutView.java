package com.dotcms.rest.api.v1.template;

public class TemplateLayoutView {

    private final String      width;
    private final String      title;
    private final boolean     header;
    private final boolean     footer;
    private final BodyView    body;
    private final SidebarView sidebar;

    public TemplateLayoutView(final String width,
                              final String title,
                              final boolean header,
                              final boolean footer,
                              final BodyView body,
                              final SidebarView sidebar) {
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
