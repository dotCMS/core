Feature: Create a Content Type with URL Map
  Scenario: Create a content type and expect 200 OK
    Given url baseUrl + '/api/v1/contenttype'
    And headers commonHeaders
    And request
      """
      {
        "baseType":"CONTENT",
        "clazz":"com.dotcms.contenttype.model.type.ImmutableSimpleContentType",
        "defaultType":false,
        "detailPage":"#(detailPageId)",
        "expireDateVar":"expiredDate",
        "fields":[
          {
            "clazz":"com.dotcms.contenttype.model.field.ImmutableRowField",
            "contentTypeId":"d90434114b901620af330653e4d33f25",
            "dataType":"SYSTEM",
            "fieldContentTypeProperties":[

            ],
            "fieldType":"Row",
            "fieldTypeLabel":"Row",
            "fieldVariables":[

            ],
            "fixed":false,
            "forceIncludeInApi":false,
            "iDate":1740685995000,
            "id":"5124055bd1ff0f842dacce4d354ee776",
            "indexed":false,
            "listed":false,
            "modDate":1740706488000,
            "name":"fields-0",
            "readOnly":false,
            "required":false,
            "searchable":false,
            "sortOrder":0,
            "unique":false,
            "variable":"fields0"
          },
          {
            "clazz":"com.dotcms.contenttype.model.field.ImmutableColumnField",
            "contentTypeId":"d90434114b901620af330653e4d33f25",
            "dataType":"SYSTEM",
            "fieldContentTypeProperties":[

            ],
            "fieldType":"Column",
            "fieldTypeLabel":"Column",
            "fieldVariables":[

            ],
            "fixed":false,
            "forceIncludeInApi":false,
            "iDate":1740685995000,
            "id":"c18c215e85ea54ad66b6bbf2ad3a7571",
            "indexed":false,
            "listed":false,
            "modDate":1740706488000,
            "name":"fields-1",
            "readOnly":false,
            "required":false,
            "searchable":false,
            "sortOrder":1,
            "unique":false,
            "variable":"fields1"
          },
          {
            "clazz":"com.dotcms.contenttype.model.field.ImmutableTextField",
            "contentTypeId":"d90434114b901620af330653e4d33f25",
            "dataType":"TEXT",
            "fieldType":"Text",
            "fieldTypeLabel":"Text",
            "fieldVariables":[

            ],
            "fixed":false,
            "forceIncludeInApi":false,
            "iDate":1740686023000,
            "id":"d6c455b45a84abbb28256e857c70fc81",
            "indexed":true,
            "listed":false,
            "modDate":1740706488000,
            "name":"title",
            "readOnly":false,
            "required":true,
            "searchable":true,
            "sortOrder":2,
            "unique":false,
            "variable":"title"
          },
          {
            "clazz":"com.dotcms.contenttype.model.field.ImmutableTextField",
            "contentTypeId":"d90434114b901620af330653e4d33f25",
            "dataType":"TEXT",
            "fieldType":"Text",
            "fieldTypeLabel":"Text",
            "fieldVariables":[

            ],
            "fixed":false,
            "forceIncludeInApi":false,
            "iDate":1740686023000,
            "id":"53e9ed7ffd1dd44dc1407f84c20270f8",
            "indexed":true,
            "listed":false,
            "modDate":1740706488000,
            "name":"URL Title",
            "readOnly":false,
            "required":true,
            "searchable":true,
            "sortOrder":2,
            "unique":false,
            "variable":"urlTitle"
          },
          {
            "clazz":"com.dotcms.contenttype.model.field.ImmutableDateTimeField",
            "contentTypeId":"d90434114b901620af330653e4d33f25",
            "dataType":"DATE",
            "fieldType":"Date-and-Time",
            "fieldTypeLabel":"Date and Time",
            "fieldVariables":[

            ],
            "fixed":false,
            "forceIncludeInApi":false,
            "iDate":1740686455000,
            "id":"2e148b4f786738c51bec7c7b1038d29a",
            "indexed":true,
            "listed":false,
            "modDate":1740706749000,
            "name":"publish date",
            "readOnly":false,
            "required":false,
            "searchable":false,
            "sortOrder":4,
            "unique":false,
            "variable":"publishDate"
          },
          {
            "clazz":"com.dotcms.contenttype.model.field.ImmutableDateTimeField",
            "contentTypeId":"d90434114b901620af330653e4d33f25",
            "dataType":"DATE",
            "fieldType":"Date-and-Time",
            "fieldTypeLabel":"Date and Time",
            "fieldVariables":[

            ],
            "fixed":false,
            "forceIncludeInApi":false,
            "iDate":1740686461000,
            "id":"dbafefc489beef1d118b561f54b7521a",
            "indexed":true,
            "listed":false,
            "modDate":1740706749000,
            "name":"expired date",
            "readOnly":false,
            "required":false,
            "searchable":false,
            "sortOrder":5,
            "unique":false,
            "variable":"expiredDate"
          }
        ],
        "fixed":false,
        "folder":"SYSTEM_FOLDER",
        "folderPath":"/",
        "host":"48190c8c-42c4-46af-8d1a-0cd5db894797",
        "iDate":1740685943000,
        "icon":"event_note",
        "id":"d90434114b901620af330653e4d33f25",
        "layout":[
          {
            "divider":{
              "clazz":"com.dotcms.contenttype.model.field.ImmutableRowField",
              "contentTypeId":"d90434114b901620af330653e4d33f25",
              "dataType":"SYSTEM",
              "fieldContentTypeProperties":[

              ],
              "fieldType":"Row",
              "fieldTypeLabel":"Row",
              "fieldVariables":[

              ],
              "fixed":false,
              "forceIncludeInApi":false,
              "iDate":1740685995000,
              "id":"5124055bd1ff0f842dacce4d354ee776",
              "indexed":false,
              "listed":false,
              "modDate":1740706488000,
              "name":"fields-0",
              "readOnly":false,
              "required":false,
              "searchable":false,
              "sortOrder":0,
              "unique":false,
              "variable":"fields0"
            },
            "columns":[
              {
                "columnDivider":{
                  "clazz":"com.dotcms.contenttype.model.field.ImmutableColumnField",
                  "contentTypeId":"d90434114b901620af330653e4d33f25",
                  "dataType":"SYSTEM",
                  "fieldContentTypeProperties":[

                  ],
                  "fieldType":"Column",
                  "fieldTypeLabel":"Column",
                  "fieldVariables":[

                  ],
                  "fixed":false,
                  "forceIncludeInApi":false,
                  "iDate":1740685995000,
                  "id":"c18c215e85ea54ad66b6bbf2ad3a7571",
                  "indexed":false,
                  "listed":false,
                  "modDate":1740706488000,
                  "name":"fields-1",
                  "readOnly":false,
                  "required":false,
                  "searchable":false,
                  "sortOrder":1,
                  "unique":false,
                  "variable":"fields1"
                },
                "fields":[
                  {
                    "clazz":"com.dotcms.contenttype.model.field.ImmutableTextField",
                    "contentTypeId":"d90434114b901620af330653e4d33f25",
                    "dataType":"TEXT",
                    "fieldType":"Text",
                    "fieldTypeLabel":"Text",
                    "fieldVariables":[

                    ],
                    "fixed":false,
                    "forceIncludeInApi":false,
                    "iDate":1740686023000,
                    "id":"d6c455b45a84abbb28256e857c70fc81",
                    "indexed":true,
                    "listed":false,
                    "modDate":1740706488000,
                    "name":"title",
                    "readOnly":false,
                    "required":true,
                    "searchable":true,
                    "sortOrder":2,
                    "unique":false,
                    "variable":"title"
                  },
                  {
                    "clazz":"com.dotcms.contenttype.model.field.ImmutableDateTimeField",
                    "contentTypeId":"d90434114b901620af330653e4d33f25",
                    "dataType":"DATE",
                    "fieldType":"Date-and-Time",
                    "fieldTypeLabel":"Date and Time",
                    "fieldVariables":[

                    ],
                    "fixed":false,
                    "forceIncludeInApi":false,
                    "iDate":1740686455000,
                    "id":"2e148b4f786738c51bec7c7b1038d29a",
                    "indexed":true,
                    "listed":false,
                    "modDate":1740706749000,
                    "name":"publish date",
                    "readOnly":false,
                    "required":false,
                    "searchable":false,
                    "sortOrder":4,
                    "unique":false,
                    "variable":"publishDate"
                  },
                  {
                    "clazz":"com.dotcms.contenttype.model.field.ImmutableDateTimeField",
                    "contentTypeId":"d90434114b901620af330653e4d33f25",
                    "dataType":"DATE",
                    "fieldType":"Date-and-Time",
                    "fieldTypeLabel":"Date and Time",
                    "fieldVariables":[

                    ],
                    "fixed":false,
                    "forceIncludeInApi":false,
                    "iDate":1740686461000,
                    "id":"dbafefc489beef1d118b561f54b7521a",
                    "indexed":true,
                    "listed":false,
                    "modDate":1740706749000,
                    "name":"expired date",
                    "readOnly":false,
                    "required":false,
                    "searchable":false,
                    "sortOrder":5,
                    "unique":false,
                    "variable":"expiredDate"
                  }
                ]
              }
            ]
          }
        ],
        "metadata":{
          "CONTENT_EDITOR2_ENABLED":false
        },
        "modDate":1740706769000,
        "multilingualable":false,
        "name":"test-url-map",
        "publishDateVar":"publishDate",
        "siteName":"systemHost",
        "sortOrder":0,
        "system":false,
        "systemActionMappings":{

        },
        "urlMapPattern":"/articles/{urlTitle}",
        "variable":"TestUrlMap",
        "versionable":true,
        "workflow":[
          "d61a59e1-a49c-46f2-a929-db2b4bfa88b2"
        ]
      }
      """
    When method POST
    Then status 200
    And match response.entity[0].id != null