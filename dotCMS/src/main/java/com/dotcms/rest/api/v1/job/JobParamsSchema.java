package com.dotcms.rest.api.v1.job;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.ws.rs.FormParam;

@Schema(description = "Schema for multipart job queue parameters.")
public class JobParamsSchema {

    @FormParam("file")
    @Schema(
            description = "The file to be processed by the job.",
            type = "string",
            format = "binary"
    )
    private String file;

    @FormParam("params")
    @Schema(
            description = "JSON string with job-specific parameters.",
            type = "string",
            example = "{\n" +
                    "  \"sourceUrl\": \"https://example.com/image.jpeg\",\n" +
                    "  \"width\": 320,\n" +
                    "  \"height\": 240,\n" +
                    "}"
    )
    private String params;

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public String getParams() {
        return params;
    }

    public void setParams(String params) {
        this.params = params;
    }
}