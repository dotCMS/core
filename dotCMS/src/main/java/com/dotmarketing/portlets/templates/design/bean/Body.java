package com.dotmarketing.portlets.templates.design.bean;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * It's a {@link com.dotmarketing.portlets.templates.model.Template}'s Body
 */
public class Body {
    private List<TemplateLayoutRow> rows;


    @JsonCreator
    public Body(@JsonProperty("rows") List<TemplateLayoutRow> rows) {
        this.rows = rows;
    }

    public List<TemplateLayoutRow> getRows() {
        return rows;
    }
}
