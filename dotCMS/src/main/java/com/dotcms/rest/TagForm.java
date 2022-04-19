package com.dotcms.rest;

import com.dotcms.rest.api.Validated;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Map;
import javax.validation.constraints.NotNull;
import org.apache.commons.lang.builder.ToStringBuilder;

public class TagForm extends Validated {

    @NotNull
    private final String userId;

    @NotNull
    private final Map<String,RestTag> tags;

    @JsonCreator
    public TagForm(final Map<String,RestTag> tags, final String userId) {
        this.tags = tags;
        this.userId = userId;
    }

    public Map<String, RestTag> getTags() {
        return tags;
    }

    public String getUserId() {
        return userId;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString( this );
    }
}
