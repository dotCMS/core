import 'zone.js'
import XDebug from 'debug'

import 'reflect-metadata';

import ruleEngineTemplate from './src/rule-engine-view/app/rule-engine.tpl.html!text'
import ruleTemplate from './src/rule-engine-view/app/rule.tpl.html!text'
import ruleActionTemplate from './src/rule-engine-view/app/rule-action.tpl.html!text'
import conditionGroupTemplate from './src/rule-engine-view/app/condition-group.tpl.html!text'
import conditionTemplate from './src/rule-engine-view/app/condition.tpl.html!text'

var templates = {
  ruleEngineTemplate: ruleEngineTemplate,
  ruleTemplate: ruleTemplate,
  ruleActionTemplate: ruleActionTemplate,
  conditionGroupTemplate: conditionGroupTemplate,
  conditionTemplate: conditionTemplate
}

import {Core, Rule, RuleGroup, ConnectionManager, EntityMeta, RestDataStore} from './src/index.js'

Object.assign(window, {
  Core,
  RuleEngine: {
    Rule,
    RuleGroup
  },
  ConnectionManager, EntityMeta, RestDataStore
})

import * as RuleEngineView from 'src/rule-engine-view/index.js';

XDebug.disable() // Clear LocalStorage so changes to log-config files 'take'
XDebug.enable("*, .*") // String of comma separated regex. Not glob patterns.

RuleEngineView.main(templates).then(function () {
  console.log("Loaded rule-engine component.")
});

console.log("Loading rule-engine component.")
