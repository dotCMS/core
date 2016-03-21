exports.config = {
  framework: 'jasmine2',
  seleniumAddress: 'http://localhost:4444/wd/hub',
  /**
   * This is a hack which allows us to use SystemJS within our Protractor tests. Why do this? So we can share
   * common code. Some otherwise easy workarounds are not possible for various reason.
   *  Q: Why not just have a separate compile?
   *  A: Because tsc defaults to 'tsconfig.json' and we can't override it, so no way to create two config files.
   *
   *
   * @returns {Promise}
   */
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
        },
        'whatwg-fetch': {
          format: 'register'
        }
      },
      map: {
        "whatwg-fetch": "../../thirdparty/whatwg-fetch/fetch.js"
      }
    });

    var specFiles = [
      'build/view/components/rule-engine/rule-engine.e2e'
    ]

    protractor.__hack = []
    var p = Promise.all(specFiles.map(function(specFile) {
      return System.import(specFile).then(function (fn) {
        protractor.__hack.push(fn)
      })
    }))

    /* The following lets us use the browsername in our tests */
    browser.getCapabilities().then(function (cap) {
      browser.browserName = cap.caps_.browserName;
    });

    /* The following lets us grab a hostname for our target test server from a single location. */
    browser.testLoc = {
      core: 'http://localhost:8080',
      coreWeb: 'http://localhost:9000'
    },

    module.exports = p
    return p
  },
  specs: [
    './build/e2e/boot.js'
  ],
  multiCapabilities: [
    {
      browserName: 'chrome',
      loggingPrefs: {"driver": "INFO", "server": "OFF", "browser": "FINE"}
    },
    {
      browserName: 'safari',
      loggingPrefs: {"driver": "INFO", "server": "OFF", "browser": "FINE"}
    }
  ],
  useAllAngular2AppRoots: true,
  allScriptsTimeout: 60000,
  jasmineNodeOpts: {
    showColors: true,
    defaultTimeoutInterval: 60000
  }

};