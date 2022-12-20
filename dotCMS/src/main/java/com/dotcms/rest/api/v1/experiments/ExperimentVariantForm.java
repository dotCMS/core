package com.dotcms.rest.api.v1.experiments;

/**
 * Form to update {@link com.dotcms.experiments.model.ExperimentVariant} description.
 */
public class ExperimentVariantForm {

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    private String description;

}
