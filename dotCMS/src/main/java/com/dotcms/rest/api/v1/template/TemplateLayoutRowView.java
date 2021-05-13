package com.dotcms.rest.api.v1.template;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class TemplateLayoutRowView {

    private final String styleClass;
    private final List<TemplateLayoutColumnView> columns;

    @JsonCreator
    public TemplateLayoutRowView(@JsonProperty("styleClass") final String styleClass,
                                 @JsonProperty("columns")    final List<TemplateLayoutColumnView> columns) {
        this.styleClass = styleClass;
        this.columns    = columns;
    }

    public String getStyleClass() {
        return styleClass;
    }

    public List<TemplateLayoutColumnView> getColumns() {
        return columns;
    }
}
