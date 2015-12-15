exports.config = {
  framework: 'jasmine2',
  seleniumAddress: 'http://localhost:4444/wd/hub',
  specs: [
    //'./build/view/components/rule-engine/condition-types/serverside-condition/serverside-condition.e2e.js',
    //'./build/view/components/semantic/elements/input-text/input-text.e2e.js',
    './build/view/components/rule-engine/rule-engine.e2e.js'
  ],
  multiCapabilities: [
    //{ browserName: 'firefox' },
    { browserName: 'chrome'}
  ],
  useAllAngular2AppRoots: true,

  jasmineNodeOpts: {
    showColors: true,
    defaultTimeoutInterval: 30000
  }

};