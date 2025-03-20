 Feature: Upload FileAsset
   Background:

     * def fileName = __arg.fileName
     * def folderidentifier = __arg.folderidentifier
     * def jsonPayload =
       """
       {
         "contentlet": {
           "contentType": "FileAsset",
           "title": '#(fileName)',
           "fileName": '#(fileName)',
           "hostFolder": '#(folderidentifier)'
         }
       }
       """
     * def filePath = 'classpath:resources/ftm/' + fileName

   Scenario: Fire PUBLISH action with a file upload
     Given url baseUrl + '/api/v1/workflow/actions/default/fire/PUBLISH'
     And headers commonHeaders
     And header Content-Type = 'multipart/form-data'
     And header Accept = '*/*'
     And multipart file file = { read: '#(filePath)', contentType: 'application/json'}
     And multipart file json = { value: '#(jsonPayload)', contentType: 'application/json'}
     When method PUT
     Then status 200
     * def errors = call extractErrors response
     * match errors == []