package com.dotcms.rest.api.v2.tags;

import com.dotcms.rest.api.Validated;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

/**
 * Form for creating a tag(s) via v2 API
 */
public class TagForm extends Validated {

    @NotNull(message = "Tag name is required")
    private final String name;

    @Nullable
    private final String siteId;

    @Nullable
    private final String ownerId;

    @Nullable
    private final Boolean persona;

    @JsonCreator
    public TagForm(
            @JsonProperty("name") final String name,
            @JsonProperty("siteId") final String siteId,
            @JsonProperty("ownerId") final String ownerId,
            @JsonProperty("persona") final Boolean persona) {
        this.name = name;
        this.siteId = siteId;
        this.ownerId = ownerId;
        this.persona = persona;
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

    public Boolean getPersona() {
        return persona;
    }
    
    @Override
    public void checkValid() {
        super.checkValid();

        TagValidationHelper.validateTagName(name, "name");
    }
}