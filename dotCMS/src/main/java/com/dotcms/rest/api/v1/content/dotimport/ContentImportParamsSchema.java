package com.dotcms.rest.api.v1.content.dotimport;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Schema for content import parameters.")
public class ContentImportParamsSchema {

    @Schema(
            description = "The CSV file to import.",
            type = "string",
            format = "binary",
            required = true
    )
    private String file;

    @Schema(
            description = "JSON string representing import settings.",
            type = "string",
            required = true,
            example = "{\n" +
                    "  \"contentType\": \"activity\",\n" +
                    "  \"language\": \"en-US\",\n" +
                    "  \"workflowActionId\": \"1234\",\n" +
                    "  \"fields\": [\"title\"]\n" +
                    "}"
    )
    private String form;

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public String getForm() {
        return form;
    }

    public void setForm(String form) {
        this.form = form;
    }
}
