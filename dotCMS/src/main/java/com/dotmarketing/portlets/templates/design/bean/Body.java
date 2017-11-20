package com.dotmarketing.portlets.templates.design.bean;

import java.util.List;

/**
 * It's a {@link com.dotmarketing.portlets.templates.model.Template}'s Body
 */
public class Body {
    private List<TemplateLayoutRow> rows;

    public List<TemplateLayoutRow> getRows() {
        return rows;
    }

    public void setRows(List<TemplateLayoutRow> rows) {
        this.rows = rows;
    }
}
