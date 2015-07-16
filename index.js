import XDebug from 'debug'

import 'reflect-metadata';

import {Core, Rule, RuleGroup, ConnectionManager, EntityMeta, RestDataStore} from './src/index.js'

Object.assign(window, {
  Core,
  ConnectionManager, EntityMeta, RestDataStore
})

window.RuleEngine = window.RuleEngine || {}
window.RuleEngine.Rule = Rule;
window.RuleEngine.RuleGroup = RuleGroup;

import RuleEngineView from 'src/rule-engine-view/index.js';

XDebug.disable() // Clear LocalStorage so changes to log-config files 'take'
XDebug.enable("*, .*") // String of comma separated regex. Not glob patterns.

RuleEngineView.main().then(function () {
  console.log("Loaded rule-engine component.")
});

console.log("Loading rule-engine component.")
