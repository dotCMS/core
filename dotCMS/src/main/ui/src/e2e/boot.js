"use strict";


/**
 * For whatever reason we cannot use 'require' inside of a Systemjs imported class. So
 * we'll prep this here and pass it into the test.
 *
 * Note that this is being run on the server - Node executes this particular magic, not the browser.
 * @type {{httpGet: TestUtil.httpGet}}
 */
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
// Load the specs that we prepared via the onPrepare method in protractor.conf.js
var initSpecModules = protractor.__hack ? protractor.__hack : "dang"
if (initSpecModules && initSpecModules.length) {
  for(var i = 0, L = initSpecModules.length; i < L; i++){
    try{
      initSpecModules[i].initSpec(TestUtil)
    }catch(e){
      console.log("Error loading spec.", e)
    }
  }
}