package com.dotcms.rest.api.v1.template;

import java.util.List;

public class TemplateLayoutRowView {

    private final String styleClass;
    private final List<TemplateLayoutColumnView> columns;

    public TemplateLayoutRowView(final String styleClass, final List<TemplateLayoutColumnView> columns) {
        this.styleClass = styleClass;
        this.columns = columns;
    }

    public String getStyleClass() {
        return styleClass;
    }

    public List<TemplateLayoutColumnView> getColumns() {
        return columns;
    }
}
