Feature: Create a Content Type
  Background:
    * def contentTypeVariable = 'Banner'

  Scenario: Create a content type and expect 200 OK
    Given url baseUrl + '/api/v1/contenttype'
    And headers commonHeaders
    And request
      """
      {
        "baseType":"CONTENT",
        "clazz":"com.dotcms.contenttype.model.type.ImmutableSimpleContentType",
        "defaultType":false,
        "description":"Hero image used on homepage and landing pages",
        "expireDateVar":"expiredDate",
        "fields":[
          {
            "clazz":"com.dotcms.contenttype.model.field.ImmutableRowField",
            "contentTypeId":"4c441ada-944a-43af-a653-9bb4f3f0cb2b",
            "dataType":"SYSTEM",
            "fieldContentTypeProperties":[

            ],
            "fieldType":"Row",
            "fieldTypeLabel":"Row",
            "fieldVariables":[

            ],
            "fixed":false,
            "forceIncludeInApi":false,
            "iDate":1529965036000,
            "id":"5dfdad46-49f4-450f-b66d-33460f645aaf",
            "indexed":false,
            "listed":false,
            "modDate":1740007777000,
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
            "contentTypeId":"4c441ada-944a-43af-a653-9bb4f3f0cb2b",
            "dataType":"SYSTEM",
            "fieldContentTypeProperties":[

            ],
            "fieldType":"Column",
            "fieldTypeLabel":"Column",
            "fieldVariables":[

            ],
            "fixed":false,
            "forceIncludeInApi":false,
            "iDate":1529965036000,
            "id":"6ab3fec7-c46b-4c95-b6dd-6f9ea5e2826e",
            "indexed":false,
            "listed":false,
            "modDate":1740007777000,
            "name":"fields-1",
            "readOnly":false,
            "required":false,
            "searchable":false,
            "sortOrder":1,
            "unique":false,
            "variable":"fields1"
          },
          {
            "clazz":"com.dotcms.contenttype.model.field.ImmutableHostFolderField",
            "contentTypeId":"4c441ada-944a-43af-a653-9bb4f3f0cb2b",
            "dataType":"SYSTEM",
            "fieldType":"Host-Folder",
            "fieldTypeLabel":"Site or Folder",
            "fieldVariables":[

            ],
            "fixed":false,
            "forceIncludeInApi":false,
            "iDate":1566840730000,
            "id":"3e089676-778a-4f17-905f-7e6b707415b9",
            "indexed":true,
            "listed":false,
            "modDate":1740007777000,
            "name":"Host",
            "readOnly":false,
            "required":true,
            "searchable":false,
            "sortOrder":2,
            "unique":false,
            "variable":"host"
          },
          {
            "clazz":"com.dotcms.contenttype.model.field.ImmutableTextField",
            "contentTypeId":"4c441ada-944a-43af-a653-9bb4f3f0cb2b",
            "dataType":"TEXT",
            "fieldType":"Text",
            "fieldTypeLabel":"Text",
            "fieldVariables":[

            ],
            "fixed":false,
            "forceIncludeInApi":false,
            "iDate":1308941149000,
            "id":"5c21daa4-1482-4bc9-9d61-04b42d35a6ee",
            "indexed":true,
            "listed":true,
            "modDate":1740007777000,
            "name":"Title",
            "readOnly":false,
            "regexCheck":"[^(<[.\\n]+>)]*",
            "required":true,
            "searchable":true,
            "sortOrder":3,
            "unique":false,
            "variable":"title"
          },
          {
            "clazz":"com.dotcms.contenttype.model.field.ImmutableCustomField",
            "contentTypeId":"4c441ada-944a-43af-a653-9bb4f3f0cb2b",
            "dataType":"LONG_TEXT",
            "fieldType":"Custom-Field",
            "fieldTypeLabel":"Custom Field",
            "fieldVariables":[

            ],
            "fixed":false,
            "forceIncludeInApi":false,
            "iDate":1555426352000,
            "id":"31eb3833-53e6-47cc-af45-221a42a3ebc5",
            "indexed":false,
            "listed":false,
            "modDate":1740007777000,
            "name":"styles",
            "readOnly":false,
            "required":false,
            "searchable":false,
            "sortOrder":4,
            "unique":false,
            "values":"<style>\r\n#styles_tag {\r\n    display: none;\r\n}\r\n.field__radio {\r\n    display: flex;\r\n}\r\n.radio {\r\n    margin-right: 20px;\r\n}\r\n.thumbnailDiv {\r\n    border: solid 1px #C5C5C5;\r\n    margin-bottom: 10px;\r\n    width: 100% !important;\r\n}\r\n\r\n.thumbnailDivHover {\r\n    border: solid 1px #161616;\r\n    margin-bottom: 10px;\r\n    width: 100% !important;\r\n}\r\n</style>",
            "variable":"styles"
          },
          {
            "clazz":"com.dotcms.contenttype.model.field.ImmutableRowField",
            "contentTypeId":"4c441ada-944a-43af-a653-9bb4f3f0cb2b",
            "dataType":"SYSTEM",
            "fieldContentTypeProperties":[

            ],
            "fieldType":"Row",
            "fieldTypeLabel":"Row",
            "fieldVariables":[

            ],
            "fixed":false,
            "forceIncludeInApi":false,
            "iDate":1555000294000,
            "id":"b885d71b-2d6c-40ae-9351-b6d58f3c6704",
            "indexed":false,
            "listed":false,
            "modDate":1740007777000,
            "name":"fields-2",
            "readOnly":false,
            "required":false,
            "searchable":false,
            "sortOrder":5,
            "unique":false,
            "variable":"fields9"
          },
          {
            "clazz":"com.dotcms.contenttype.model.field.ImmutableColumnField",
            "contentTypeId":"4c441ada-944a-43af-a653-9bb4f3f0cb2b",
            "dataType":"SYSTEM",
            "fieldContentTypeProperties":[

            ],
            "fieldType":"Column",
            "fieldTypeLabel":"Column",
            "fieldVariables":[

            ],
            "fixed":false,
            "forceIncludeInApi":false,
            "iDate":1555000294000,
            "id":"92777b44-e53c-4635-b42a-2ce745d10d6c",
            "indexed":false,
            "listed":false,
            "modDate":1740007777000,
            "name":"fields-3",
            "readOnly":false,
            "required":false,
            "searchable":false,
            "sortOrder":6,
            "unique":false,
            "variable":"fields10"
          },
          {
            "clazz":"com.dotcms.contenttype.model.field.ImmutableCustomField",
            "contentTypeId":"4c441ada-944a-43af-a653-9bb4f3f0cb2b",
            "dataType":"LONG_TEXT",
            "defaultValue":"1",
            "fieldType":"Custom-Field",
            "fieldTypeLabel":"Custom Field",
            "fieldVariables":[

            ],
            "fixed":false,
            "forceIncludeInApi":false,
            "iDate":1412016703000,
            "id":"c4712e56-ec64-4795-8613-63dff910b34e",
            "indexed":true,
            "listed":true,
            "modDate":1740007777000,
            "name":"Layout",
            "readOnly":false,
            "required":false,
            "searchable":false,
            "sortOrder":7,
            "unique":false,
            "values":"##banner-layout.vtl\r\n#dotParse(\"/dA/54c1cb5e96ca7f179a9d4ce0056c1ee2\")",
            "variable":"layout"
          },
          {
            "clazz":"com.dotcms.contenttype.model.field.ImmutableTextField",
            "contentTypeId":"4c441ada-944a-43af-a653-9bb4f3f0cb2b",
            "dataType":"TEXT",
            "fieldType":"Text",
            "fieldTypeLabel":"Text",
            "fieldVariables":[

            ],
            "fixed":false,
            "forceIncludeInApi":false,
            "iDate":1582137456000,
            "id":"0eeef3b4-7fc2-45d3-9fda-c99c5b016374",
            "indexed":true,
            "listed":true,
            "modDate":1740007777000,
            "name":"Caption",
            "readOnly":false,
            "required":false,
            "searchable":false,
            "sortOrder":8,
            "unique":false,
            "variable":"caption"
          },
          {
            "clazz":"com.dotcms.contenttype.model.field.ImmutableTextField",
            "contentTypeId":"4c441ada-944a-43af-a653-9bb4f3f0cb2b",
            "dataType":"TEXT",
            "fieldType":"Text",
            "fieldTypeLabel":"Text",
            "fieldVariables":[

            ],
            "fixed":false,
            "forceIncludeInApi":false,
            "iDate":1562795494000,
            "id":"814af473-c03d-446d-8609-f82b0d67b7c3",
            "indexed":false,
            "listed":false,
            "modDate":1740007777000,
            "name":"Button Text",
            "readOnly":false,
            "required":false,
            "searchable":false,
            "sortOrder":9,
            "unique":false,
            "variable":"buttonText"
          },
          {
            "clazz":"com.dotcms.contenttype.model.field.ImmutableTextField",
            "contentTypeId":"4c441ada-944a-43af-a653-9bb4f3f0cb2b",
            "dataType":"TEXT",
            "fieldType":"Text",
            "fieldTypeLabel":"Text",
            "fieldVariables":[

            ],
            "fixed":false,
            "forceIncludeInApi":false,
            "iDate":1562795510000,
            "id":"f959784b-b2e7-4a09-b004-fe3e67e5fbd6",
            "indexed":false,
            "listed":false,
            "modDate":1740007777000,
            "name":"Link",
            "readOnly":false,
            "required":false,
            "searchable":false,
            "sortOrder":10,
            "unique":false,
            "variable":"link"
          },
          {
            "clazz":"com.dotcms.contenttype.model.field.ImmutableCustomField",
            "contentTypeId":"4c441ada-944a-43af-a653-9bb4f3f0cb2b",
            "dataType":"LONG_TEXT",
            "defaultValue":"#FFFFFF",
            "fieldType":"Custom-Field",
            "fieldTypeLabel":"Custom Field",
            "fieldVariables":[

            ],
            "fixed":false,
            "forceIncludeInApi":false,
            "iDate":1555001286000,
            "id":"9d859b44-18c5-4b3c-b077-e812a945f255",
            "indexed":false,
            "listed":false,
            "modDate":1740007777000,
            "name":"Text Color",
            "readOnly":false,
            "required":false,
            "searchable":false,
            "sortOrder":11,
            "unique":false,
            "values":"##SET FIELD VARIABLE NAME TO APPLY PICKER TO\r\n\r\n#set($fieldName = \"textColor\")\r\n\r\n##color-picker.vtl\r\n#dotParse(\"/dA/7f494f7ea2cb44717baac7004d974c52\")",
            "variable":"textColor"
          },
          {
            "clazz":"com.dotcms.contenttype.model.field.ImmutableTagField",
            "contentTypeId":"4c441ada-944a-43af-a653-9bb4f3f0cb2b",
            "dataType":"SYSTEM",
            "fieldType":"Tag",
            "fieldTypeLabel":"Tag",
            "fieldVariables":[

            ],
            "fixed":false,
            "forceIncludeInApi":false,
            "iDate":1567533022000,
            "id":"27499ccb-1bbb-4c02-ab91-c11e01f58ac7",
            "indexed":true,
            "listed":false,
            "modDate":1740007777000,
            "name":"Tags",
            "readOnly":false,
            "required":false,
            "searchable":false,
            "sortOrder":12,
            "unique":false,
            "variable":"tags"
          },
          {
            "clazz":"com.dotcms.contenttype.model.field.ImmutableColumnField",
            "contentTypeId":"4c441ada-944a-43af-a653-9bb4f3f0cb2b",
            "dataType":"SYSTEM",
            "fieldContentTypeProperties":[

            ],
            "fieldType":"Column",
            "fieldTypeLabel":"Column",
            "fieldVariables":[

            ],
            "fixed":false,
            "forceIncludeInApi":false,
            "iDate":1555000294000,
            "id":"f735c683-f05e-4138-8056-73d76e80b1aa",
            "indexed":false,
            "listed":false,
            "modDate":1740007777000,
            "name":"fields-4",
            "readOnly":false,
            "required":false,
            "searchable":false,
            "sortOrder":13,
            "unique":false,
            "variable":"fields11"
          },
          {
            "clazz":"com.dotcms.contenttype.model.field.ImmutableImageField",
            "contentTypeId":"4c441ada-944a-43af-a653-9bb4f3f0cb2b",
            "dataType":"TEXT",
            "fieldType":"Image",
            "fieldTypeLabel":"Image",
            "fieldVariables":[

            ],
            "fixed":false,
            "forceIncludeInApi":false,
            "iDate":1717082944000,
            "id":"13bf66791d690a89ca05171ecdd5f71c",
            "indexed":false,
            "listed":false,
            "modDate":1740007777000,
            "name":"Image",
            "readOnly":false,
            "required":false,
            "searchable":false,
            "sortOrder":14,
            "unique":false,
            "variable":"image"
          },
          {
            "clazz":"com.dotcms.contenttype.model.field.ImmutableDateTimeField",
            "contentTypeId":"4c441ada-944a-43af-a653-9bb4f3f0cb2b",
            "dataType":"DATE",
            "fieldType":"Date-and-Time",
            "fieldTypeLabel":"Date and Time",
            "fieldVariables":[

            ],
            "fixed":false,
            "forceIncludeInApi":false,
            "iDate":1739980935000,
            "id":"492fea0d7e7f1dd4a34af18fca2ed9ab",
            "indexed":true,
            "listed":false,
            "modDate":1740007777000,
            "name":"publish date",
            "readOnly":false,
            "required":false,
            "searchable":false,
            "sortOrder":15,
            "unique":false,
            "variable":"publishDate"
          },
          {
            "clazz":"com.dotcms.contenttype.model.field.ImmutableDateTimeField",
            "contentTypeId":"4c441ada-944a-43af-a653-9bb4f3f0cb2b",
            "dataType":"DATE",
            "fieldType":"Date-and-Time",
            "fieldTypeLabel":"Date and Time",
            "fieldVariables":[

            ],
            "fixed":false,
            "forceIncludeInApi":false,
            "iDate":1739980959000,
            "id":"487af50492627a9b95a49f35253a0dff",
            "indexed":true,
            "listed":false,
            "modDate":1740007777000,
            "name":"expired date",
            "readOnly":false,
            "required":false,
            "searchable":false,
            "sortOrder":16,
            "unique":false,
            "variable":"expiredDate"
          }
        ],
        "fixed":false,
        "folder":"SYSTEM_FOLDER",
        "folderPath":"/",
        "host":"SYSTEM_HOST",
        "iDate":1489086945734,
        "icon":"image_aspect_ratio",
        "id":"4c441ada-944a-43af-a653-9bb4f3f0cb2b",
        "metadata":{
          "CONTENT_EDITOR2_ENABLED":false
        },
        "modDate":1740007777000,
        "multilingualable":false,
        "name":"Banner",
        "publishDateVar":"publishDate",
        "siteName":"systemHost",
        "sortOrder":0,
        "system":false,
        "systemActionMappings":{

        },
        "variable":"#(contentTypeVariable)",
        "versionable":true,
        "workflow":[
          "d61a59e1-a49c-46f2-a929-db2b4bfa88b2"
        ]
      }
      """
    When method POST
    Then status 200
    And match response.entity[0].id != null
    And match response.entity[0].variable == contentTypeVariable