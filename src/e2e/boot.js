"use strict";



var TestUtil = {


// A Protracterized httpGet() promise [curtesy of Leo Galluci ,
// http://stackoverflow.com/questions/25137881/how-to-use-protractor-to-get-the-response-status-code-and-response-text

// // Example:
//it('should return 200 and contain proper body', function() {
//  httpGet("http://localhost:80").then(function(result) {
//    expect(result.statusCode).toBe(200);
//    expect(result.bodyString).toContain('Apache');
//  });
//});
  httpGet: function(siteUrl) {
    //noinspection TypeScriptUnresolvedFunction
    var http = require('http');
    var defer = protractor.promise.defer();

    http.get(siteUrl, function (response) {

      var bodyString = '';

      response.setEncoding('utf8');

      response.on("data", function (chunk) {
        bodyString += chunk;
      });

      response.on('end', function () {
        defer.fulfill({
          response: response,
          statusCode: response.statusCode,
          bodyString: bodyString
        });
      });

    }).on('error', function (e) {
      defer.reject("Got http.get error: " + e.message);
    });

    return defer.promise;
  }

}



console.log("boot.js")
var initSpecModules = protractor.__hack ? protractor.__hack : "dang"

let b = browser
if (initSpecModules && initSpecModules.length) {
  console.log("Loading " + initSpecModules.length + " specs")
  for(var i = 0, L = initSpecModules.length; i < L; i++){
    try{
      initSpecModules[i].initSpec(TestUtil)
    }catch(e){
      console.log("Error loading spec.", e)
    }
  }
}