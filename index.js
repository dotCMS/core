import 'zone.js'
import 'reflect-metadata';
import 'es6-shim';

import {ConnectionManager, EntityMeta, RestDataStore} from 'coreweb/api/index'
import * as RuleEngineView from 'rule-engine/index';

Object.assign(window, {
  ConnectionManager, EntityMeta, RestDataStore
})

window.RuleEngine = window.RuleEngine || {}


RuleEngineView.main(ConnectionManager, RestDataStore).then(function () {
  console.log("Loaded rule-engine component.")
});

console.log("Loading rule-engine component.")
