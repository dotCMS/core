import XDebug from 'debug'

import 'zone.js'
import 'reflect-metadata';
import 'es6-shim';

import {Core} from 'coreweb-util/index.js'
import {ConnectionManager, EntityMeta, RestDataStore} from 'entity-forge/index.js'
import {Rule, RuleGroup} from 'rule-engine-api/index.js'
import * as RuleEngineView from 'rule-engine-view/index.js';

Object.assign(window, {
  Core,
  ConnectionManager, EntityMeta, RestDataStore
})

window.RuleEngine = window.RuleEngine || {}
window.RuleEngine.Rule = Rule;
window.RuleEngine.RuleGroup = RuleGroup;


XDebug.disable() // Clear LocalStorage so changes to log-config files 'take'
XDebug.enable("*, .*") // String of comma separated regex. Not glob patterns.

RuleEngineView.main(ConnectionManager, RestDataStore).then(function () {
  console.log("Loaded rule-engine component.")
});

console.log("Loading rule-engine component.")
