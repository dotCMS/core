package com.dotcms.rest.api.v1.template;

import java.util.List;

public class BodyView {

    private final List<TemplateLayoutRowView> rows;

    public BodyView(final List<TemplateLayoutRowView> rows) {
        this.rows = rows;
    }

    public List<TemplateLayoutRowView> getRows() {
        return rows;
    }
}
