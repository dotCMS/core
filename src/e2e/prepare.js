"use strict";
console.log("prepare.js")
//require('../../node_modules/es6-shim/es6-shim.js')
require('../../node_modules/systemjs/dist/system-polyfills.src.js')
require('../../node_modules/systemjs/dist/system.src.js')
//require('../../node_modules/rxjs/bundles/Rx.js')

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
var p = new Promise(function(resolve, reject) {
  console.log("prepare.js", "promise")

  System.import('build/view/components/rule-engine/rule-engine.e2e').then(function (fn) {
    console.log("ASdfasfasd")
    protractor.__hack.push(fn)
    resolve(protractor.__hack)
  }, console.error.bind(console));
});

module.exports = p
return p