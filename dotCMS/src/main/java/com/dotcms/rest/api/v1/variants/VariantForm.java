package com.dotcms.rest.api.v1.variants;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Form to create a new Variant
 * @author jsanca
 */
public class VariantForm {

    @JsonProperty("name")
    private final String name;
    @JsonProperty("description")
    private final String description;

    @JsonCreator
    public VariantForm(@JsonProperty("name")final String name,
                       @JsonProperty("description")final String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }


    public String getDescription() {
        return description;
    }

}
