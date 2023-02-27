package com.dotcms.rest.api.v1.experiments;

import com.dotcms.repackage.javax.validation.constraints.Size;
import com.dotcms.rest.api.Validated;

public class AddVariantForm extends Validated {
    @Size(min=1, max = 255)
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
