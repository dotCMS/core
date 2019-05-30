package com.dotcms.workflow.form;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.dotcms.rest.api.Validated;

import java.util.List;

/**
 * Encapsulates the input for the BulkAction Form.
 * @author jsanca
 */
public class BulkActionForm extends Validated {

    private final List<String> contentletIds;
    private final String       query;

    @JsonCreator
    public BulkActionForm(@JsonProperty("contentletIds") final List<String> contentletIds,
                          @JsonProperty("query") final String query) {

        this.contentletIds      = contentletIds;
        this.query              = query;
    }

    public List<String> getContentletIds() {
        return contentletIds;
    }

    public String getQuery() {
        return query;
    }

    @Override
    public String toString() {
        return "BulkActionForm{" +
                "contentletIds=" + contentletIds +
                ", query='" + query + '\'' +
                '}';
    }
}
