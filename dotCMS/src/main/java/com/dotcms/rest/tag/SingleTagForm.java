package com.dotcms.rest.tag;

import com.dotcms.rest.api.Validated;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * Form for creating a single tag via v2 API
 */
public class SingleTagForm extends Validated {

    @NotNull(message = "Tag name is required")
    @Size(min = 1, max = 255, message = "Tag name must be between 1 and 255 characters")
    private final String name;

    @Nullable
    private final String siteId;

    @Nullable
    private final String ownerId;

    @JsonCreator
    public SingleTagForm(
            @JsonProperty("name") final String name,
            @JsonProperty("siteId") final String siteId,
            @JsonProperty("ownerId") final String ownerId) {
        this.name = name;
        this.siteId = siteId;
        this.ownerId = ownerId;
        checkValid();
    }

    public String getName() {
        return name;
    }

    public String getSiteId() {
        return siteId;
    }

    public String getOwnerId() {
        return ownerId;
    }
}