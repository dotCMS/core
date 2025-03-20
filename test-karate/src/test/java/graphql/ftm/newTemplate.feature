Feature: Create a new Template for later use during Time Machine testing
  Background:
    * def templateName = 'MyTemplate' + Math.floor(Math.random() * 1000)
  Scenario: Create a new Template
    Given url baseUrl + '/api/v1/templates'
    And headers commonHeaders
    And request
      """
      {
        "title":"#(templateName)",
        "theme":"13f88067-1e25-4e30-bc64-7e8f42ad542f",
        "friendlyName":"Test Template.",
        "layout":{
          "body":{
            "rows":[
              {
                "styleClass":"",
                "columns":[
                  {
                    "styleClass":"",
                    "leftOffset":1,
                    "width":100,
                    "containers":[
                      {
                        "identifier":"#(containerId)",
                      }
                    ]
                  }
                ]
              },{
                "styleClass":"",
                "columns":[
                  {
                    "styleClass":"",
                    "leftOffset":1,
                    "width":100,
                    "containers":[
                      {
                        "identifier":"#(containerId)",
                      }
                    ]
                  }
                ]
              }
            ]
          },
          "header":true,
          "footer":true,
          "sidebar":{
            "location":"",
            "containers":[

            ],
            "width":"small"
          }
        }
      }
      """
    When method post
    Then status 200