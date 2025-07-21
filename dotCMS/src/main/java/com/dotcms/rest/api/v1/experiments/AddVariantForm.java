package com.dotcms.rest.api.v1.experiments;

import org.hibernate.validator.constraints.Length;
import com.dotcms.rest.api.Validated;

public class AddVariantForm extends Validated {
    @Length(min=1, max = 255)
    private String description ="";

    public AddVariantForm() {
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String name) {
        this.description = name;
        checkValid();
    }
}
