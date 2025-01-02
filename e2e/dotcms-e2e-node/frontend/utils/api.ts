import { APIRequestContext, expect } from "@playwright/test";

export async function createBasicContentType(request: APIRequestContext) {
  const data = {
    defaultType: false,
    icon: "new_releases",
    fixed: false,
    system: false,
    clazz: "com.dotcms.contenttype.model.type.ImmutableSimpleContentType",
    description: "Include all fields",
    host: "48190c8c-42c4-46af-8d1a-0cd5db894797",
    folder: "SYSTEM_FOLDER",
    name: "BasicContentType",
    systemActionMappings: {
      NEW: "",
    },
    metadata: {
      CONTENT_EDITOR2_ENABLED: true,
    },
    workflow: ["d61a59e1-a49c-46f2-a929-db2b4bfa88b2"],
  };

  const endpoint = `/api/v1/contenttype`;

  const contentTypeResponse = await request.post(endpoint, {
    data,
  });

  expect(contentTypeResponse.status()).toBe(201);

  const contentTypeResponseBody = await contentTypeResponse.json();
  expect(contentTypeResponseBody.id).toBeDefined();

  return contentTypeResponseBody;
}

export async function addTextFieldToContentType(
  request: APIRequestContext,
  contentTypeId: string,
) {
  const data = {
    layout: [
      {
        divider: {
          clazz: "com.dotcms.contenttype.model.field.ImmutableRowField",
          contentTypeId,
          dataType: "SYSTEM",
          fieldContentTypeProperties: [],
          fieldType: "Row",
          fieldTypeLabel: "Row",
          fieldVariables: [],
          fixed: false,
          forceIncludeInApi: false,
          iDate: 1735844752000,
          indexed: false,
          listed: false,
          modDate: 1735844752000,
          name: "Row Field",
          readOnly: false,
          required: false,
          searchable: false,
          sortOrder: -1,
          unique: false,
        },
        columns: [
          {
            columnDivider: {
              clazz: "com.dotcms.contenttype.model.field.ImmutableColumnField",
              contentTypeId,
              dataType: "SYSTEM",
              fieldContentTypeProperties: [],
              fieldType: "Column",
              fieldTypeLabel: "Column",
              fieldVariables: [],
              fixed: false,
              forceIncludeInApi: false,
              iDate: 1735844752000,
              indexed: false,
              listed: false,
              modDate: 1735844752000,
              name: "Column Field",
              readOnly: false,
              required: false,
              searchable: false,
              sortOrder: -1,
              unique: false,
            },
            fields: [
              {
                clazz: "com.dotcms.contenttype.model.field.ImmutableTextField",
                name: "Text Field",
                dataType: "TEXT",
                regexCheck: "",
                defaultValue: "",
                hint: "Text hint",
                required: true,
                searchable: false,
                indexed: false,
                listed: false,
                unique: false,
                id: null,
              },
            ],
          },
        ],
      },
    ],
  };

  const endpoint = `/api/v3/contenttype/${contentTypeId}/fields/move`;

  const response = await request.put(endpoint, {
    data,
  });

  expect(response.status()).toBe(200);
}
