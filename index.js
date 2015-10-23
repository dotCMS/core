import 'babel/polyfill'
import 'es6-shim'
import 'zone.js'
import 'reflect-metadata'

import * as RuleEngineView from 'rule-engine/index';

RuleEngineView.main().then(function () {
  console.log("Loaded rule-engine component.")
});

console.log("Loading rule-engine component.")
