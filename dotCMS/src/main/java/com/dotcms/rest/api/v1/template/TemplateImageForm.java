package com.dotcms.rest.api.v1.template;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 * TemplateImageForm
 */
public class TemplateImageForm implements Serializable {

    @JsonProperty("templateId")
    private String templateId;

    @JsonCreator
    public TemplateImageForm(@JsonProperty("templateId") final String templateId) {
        this.templateId = templateId;
    }

    public String getTemplateId() {
        return templateId;
    }
}
