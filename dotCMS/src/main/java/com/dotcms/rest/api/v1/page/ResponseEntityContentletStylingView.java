package com.dotcms.rest.api.v1.page;

import com.dotcms.rest.ResponseEntityView;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

// Extend ResponseEntityView with String to keep "entity": "ok"
public class ResponseEntityContentletStylingView extends ResponseEntityView<String> {

    private final List<ContentletStylingView> items;

    public ResponseEntityContentletStylingView(List<ContentletStylingView> items) {
        // Pass "OK" to the parent constructor so 'entity' is populated
        super("ok");
        this.items = items;
    }

    @JsonProperty("items")
    public List<ContentletStylingView> getItems() {
        return items;
    }
}