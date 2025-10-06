package com.dotcms.rest.api.v1.job;

public class JobQueueDocs {

    public static final String FORM_FIELD_DOC =
            "This endpoint accepts a `multipart/form-data` request with two fields:\n\n" +
                    "| **Field** | **Type** | **Required** | **Description** |\n" +
                    "|-----------|----------|--------------|-----------------|\n" +
                    "| `file`    | File     | ❌ No        | The file to be processed by the queue.|\n" +
                    "| `form`    | String   | ❌ No        | A JSON string containing job-specific parameters.|\n\n" +
                    "**Example `form` value:**\n\n" +
                    "```json\n" +
                    "{\n" +
                    " \"contentType\":\"CustomContentType\", \"workflowActionId\":\"Workflow-UUID\", \"language\":\"en-us\", \"stopOnError\":true,\n" +
                    "}\n" +
                    "```";
}