package com.dotcms.rest.api.v1.content.dotimport;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.ws.rs.FormParam;

/**
 * Represents the schema for content import parameters used in Swagger documentation.
 * <p>
 * This class defines the structure and metadata for the parameters required
 * to validate or perform content import operations.
 * </p>
 */
@Schema(description = "Schema for content import parameters.")
public class ContentImportParamsSchema {

    @FormParam("file")
    @Schema(
            description = "The CSV file to import. Required unless `tempFileId` is sent instead.",
            type = "string",
            format = "binary"
    )
    private String file;

    @FormParam("tempFileId")
    @Schema(
            description = "The id of a CSV already uploaded by the same user, such as the "
                    + "`tempFileId` a preview returns in its job parameters, imported instead of "
                    + "uploading `file` again. Send either `file` or `tempFileId`, not both.",
            type = "string"
    )
    private String tempFileId;

    @FormParam("form")
    @Schema(
            description = "JSON string representing import settings.",
            type = "string",
            required = true,
            example = "{\n" +
                    "  \"contentType\": \"activity\",\n" +
                    "  \"language\": \"en-US\",\n" +
                    "  \"workflowActionId\": \"b9d89c80-3d88-4311-8365-187323c96436\",\n" +
                    "  \"fields\": [\"title\"],\n" +
                    "  \"stopOnError\": false,\n" +
                    "  \"commitGranularity\": 100,\n" +
                    "  \"site\": \"demo.dotcms.com\"\n" +
                    "}"
    )
    private String form;

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public String getTempFileId() {
        return tempFileId;
    }

    public void setTempFileId(String tempFileId) {
        this.tempFileId = tempFileId;
    }

    public String getForm() {
        return form;
    }

    public void setForm(String form) {
        this.form = form;
    }
}
