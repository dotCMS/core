exports.config = {
  framework: 'jasmine2',
  seleniumAddress: 'http://localhost:4444/wd/hub',
  //onPrepare: "./build/e2e/prepare.js",
  onPrepare: function () {
    "use strict";
    require('./node_modules/systemjs/dist/system-polyfills.src.js')
    require('./node_modules/systemjs/dist/system.src.js')

    System.config({
      packages: {
        app: {
          format: 'register',
          defaultExtension: 'js'
        },
        api: {
          format: 'register',
          defaultExtension: 'js'
        },
        view: {
          format: 'register',
          defaultExtension: 'js'
        },
        build: {
          format: 'register',
          defaultExtension: 'js'
        }
      },
      map: {
        "whatwg-fetch": "../thirdparty/whatwg-fetch/fetch.js"
      }
    });


    protractor.__hack = []
    var p = new Promise(function (resolve, reject) {
      System.import('build/view/components/rule-engine/rule-engine.e2e').then(function (fn) {
        protractor.__hack.push(fn)
        resolve(protractor.__hack)
      }, console.error.bind(console));
    });
    module.exports = p
    return p
  },
  specs: [
    './build/e2e/boot.js'
    //'./build/view/components/rule-engine/condition-types/serverside-condition/serverside-condition.e2e.js',
    //'./build/view/components/semantic/elements/input-text/input-text.e2e.js',
    //'./build/view/components/rule-engine/rule-engine.e2e.js'
  ],
  multiCapabilities: [
    //{ browserName: 'firefox' },
    {
      browserName: 'chrome',
      loggingPrefs: {"driver": "INFO", "server": "OFF", "browser": "FINE"}
    }
  ],
  useAllAngular2AppRoots: true,

  jasmineNodeOpts: {
    showColors: true,
    defaultTimeoutInterval: 30000
  }

};