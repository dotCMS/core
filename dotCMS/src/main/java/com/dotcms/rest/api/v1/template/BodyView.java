package com.dotcms.rest.api.v1.template;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class BodyView {

    private final List<TemplateLayoutRowView> rows;

    @JsonCreator
    public BodyView(@JsonProperty("rows") final List<TemplateLayoutRowView> rows) {
        this.rows = rows;
    }

    public List<TemplateLayoutRowView> getRows() {
        return rows;
    }
}
