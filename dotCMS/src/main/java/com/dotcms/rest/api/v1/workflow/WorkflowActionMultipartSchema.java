package com.dotcms.rest.api.v1.workflow;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.ws.rs.FormParam;
import java.util.List;

/**
 * OpenAPI schema for workflow multipart requests.
 * Runtime binding remains FormDataMultiPart; this class is only for documentation.
 */
@Schema(description = "Multipart form for workflow actions. Include a JSON 'contentlet' and one or more 'file' parts that map to binary field variables.")
public class WorkflowActionMultipartSchema {

    @FormParam("contentlet")
    @Schema(description = "JSON object describing the contentlet values.", example = "{\\n  \"contentType\": \"News\",\\n  \"title\": \"My News\"\\n}")
    public String contentlet;

    @FormParam("binaryFields")
    @Schema(description = "List of binary field variables. The uploaded files in 'file' should correspond by index to these field variables.", example = "[\"binaryImage\", \"binaryDocument\"]")
    public List<String> binaryFields;

    @FormParam("file")
    @ArraySchema(
            arraySchema = @Schema(description = "Files to upload. Repeat the 'file' part for multiple files; order should match 'binaryFields'."),
            schema = @Schema(type = "string", format = "binary")
    )
    public List<Object> file;
}


