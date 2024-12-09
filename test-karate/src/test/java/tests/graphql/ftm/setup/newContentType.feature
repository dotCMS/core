Feature: Create a Content Type
  Background:
    * def contentTypeVariable = 'MyContentType' + Math.floor(Math.random() * 1000)

  Scenario Outline: Create a content type and expect 200 OK
    Given url baseUrl + '/api/v1/contenttype'
    And headers commonHeaders
    And request
      """
      {
        "baseType":"CONTENT",
        "clazz":"com.dotcms.contenttype.model.type.ImmutableSimpleContentType",
        "defaultType":false,
        "expireDateVar":"expiresOn",
        "fields":[
          {
            "clazz":"com.dotcms.contenttype.model.field.ImmutableTextField",
            "dataType":"TEXT",
            "fieldType":"Text",
            "fieldTypeLabel":"Text",
            "fieldVariables":[

            ],
            "fixed":false,
            "forceIncludeInApi":false,
            "indexed":true,
            "listed":false,
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
            "dataType":"DATE",
            "fieldType":"Date-and-Time",
            "fieldTypeLabel":"Date and Time",
            "fieldVariables":[

            ],
            "fixed":false,
            "forceIncludeInApi":false,
            "indexed":true,
            "listed":false,
            "name":"publishDate",
            "readOnly":false,
            "required":false,
            "searchable":true,
            "sortOrder":3,
            "unique":false,
            "variable":"publishDate"
          },
          {
            "clazz":"com.dotcms.contenttype.model.field.ImmutableDateTimeField",
            "dataType":"DATE",
            "fieldType":"Date-and-Time",
            "fieldTypeLabel":"Date and Time",
            "fieldVariables":[

            ],
            "fixed":false,
            "forceIncludeInApi":false,
            "indexed":true,
            "listed":false,
            "name":"expiresOn",
            "readOnly":false,
            "required":false,
            "searchable":true,
            "sortOrder":4,
            "unique":false,
            "variable":"expiresOn"
          },
          {
            "clazz":"com.dotcms.contenttype.model.field.ImmutableTagField",
            "dataType":"SYSTEM",
            "fieldType":"Tag",
            "fieldTypeLabel":"Tag",
            "fieldVariables":[

            ],
            "fixed":false,
            "forceIncludeInApi":false,
            "indexed":true,
            "listed":false,
            "name":"tags",
            "readOnly":false,
            "required":false,
            "searchable":false,
            "sortOrder":5,
            "unique":false,
            "variable":"tags"
          }
        ],
        "fixed":false,
        "folder":"SYSTEM_FOLDER",
        "folderPath":"/",
        "host":"48190c8c-42c4-46af-8d1a-0cd5db894797",
        "icon":"adjust",
        "multilingualable":false,
        "name":"#(contentTypeVariable)",
        "publishDateVar":"publishDate",
        "sortOrder":0,
        "system":false,
        "variable":"#(contentTypeVariable)",
        "versionable":true
      }
      """
    When method POST
    Then status 200
    And match response.entity[0].id != null
    And match response.entity[0].variable == contentTypeVariable
    Examples:
      | name           | description          |
      | TestImportJob1 | THE DESCRIPTION 1    |