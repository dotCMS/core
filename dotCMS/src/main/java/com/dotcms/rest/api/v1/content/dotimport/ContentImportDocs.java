package com.dotcms.rest.api.v1.content.dotimport;

public class ContentImportDocs {

    public static final String FORM_FIELD_DOC =
            "This endpoint accepts a `multipart/form-data` request with two fields:\n\n" +
                    "| **Field** | **Type** | **Required** | **Description** |\n" +
                    "|-----------|----------|--------------|-----------------|\n" +
                    "| `file`    | File     | ✅ Yes        | The CSV file to import. Must contain content rows and match the expected structure for the content type. |\n" +
                    "| `form`    | String   | ✅ Yes        | A JSON string containing the import parameters. See structure below. |\n\n" +
                    "**`form` field structure:**\n\n" +
                    "| **Property**         | **Type**   | **Required** | **Default** | **Description** |\n" +
                    "|----------------------|------------|--------------|-------------|-----------------|\n" +
                    "| `contentType`        | String     | ✅ Yes        | –           | Content Type variable or ID to import data into. |\n" +
                    "| `language`           | String     | ❌ No         | Default language | Language code (e.g., `en-US`) or language ID. |\n" +
                    "| `workflowActionId`   | String     | ✅ Yes        | –           | Workflow Action UUID to apply to imported content. |\n" +
                    "| `fields`             | String[]   | ❌ No         | –           | List of field variables or IDs used as keys for content updates. |\n" +
                    "| `stopOnError`        | Boolean    | ❌ No         | `false`     | Whether to stop import on first validation error. |\n" +
                    "| `commitGranularity`  | Integer    | ❌ No         | `100`       | Number of rows to commit in each transaction batch. |\n\n" +
                    "**Example `form` value:**\n\n" +
                    "```json\n" +
                    "{\n" +
                    "  \"contentType\": \"webPageContent\",\n" +
                    "  \"language\": \"en-US\",\n" +
                    "  \"workflowActionId\": \"b9d89c80-3d88-4311-8365-187323c96436\",\n" +
                    "  \"fields\": [\"title\"],\n" +
                    "  \"stopOnError\": false,\n" +
                    "  \"commitGranularity\": 100\n" +
                    "}\n" +
                    "```";
}
