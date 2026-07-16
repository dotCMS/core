package com.dotcms.rest.api.v1.template;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public class TemplateLayoutRowView {

    private final String styleClass;
    private final List<TemplateLayoutColumnView> columns;
    private final Map<String, Object> metadata;

    @JsonCreator
    public TemplateLayoutRowView(@JsonProperty("styleClass") final String styleClass,
                                 @JsonProperty("columns")    final List<TemplateLayoutColumnView> columns,
                                 @JsonProperty("metadata")   final Map<String, Object> metadata) {
        this.styleClass = styleClass;
        this.columns    = columns;
        this.metadata   = metadata;
    }

    public String getStyleClass() {
        return styleClass;
    }

    public List<TemplateLayoutColumnView> getColumns() {
        return columns;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }
}
