/// <reference path="../../../typings/es6/lib.es6.d.ts" />

/// <reference path="../../../typings/angular2/angular2.d.ts" />
/// <reference path="../../../typings/dotcms/dotcms-core-web.d.ts" />
/// <reference path="../../../typings/entity-forge/entity-forge.d.ts" />

/// <reference path="./rule-action-component.ts" />
/// <reference path="./rule-condition-component.ts" />
/// <reference path="./rule-component.ts" />

import {bootstrap, NgFor, NgIf, Component, Directive, View} from 'angular2/angular2';

import {initActionlets} from './rule-action-component';
import {initConditionlets} from './rule-condition-component';
import {RuleComponent} from './rule-component';


@Component({
  selector: 'rule-engine'
})
@View({
  template: RuleEngine.templates.ruleEngineTemplate,
  directives: [RuleComponent, NgFor, NgIf]
})
class RuleEngineComponent {
  rules:Array<any>;
  baseUrl:string;
  rulesRef:EntityMeta;

  constructor() {
    console.log('Creating RuleEngine component.')
    this.rules = []
    this.baseUrl = ConnectionManager.baseUrl;
    this.rulesRef = new EntityMeta('/api/v1/sites/48190c8c-42c4-46af-8d1a-0cd5db894797/rules')
    this.onChange()
  }

  updateBaseUrl(value) {
    let oldUrl = ConnectionManager.baseUrl
    ConnectionManager.baseUrl = value;
    this.baseUrl = value;
    this.testBaseUrl(value).catch((e => {
      alert("Error using provided Base Url. Check the development console.");
      console.log("Error using provided Base Url: ", e)
      this.baseUrl = oldUrl;
      ConnectionManager.baseUrl = oldUrl
      throw e
    }))

  }

  onChange(event = null) {
    console.log("RuleEngine change event: ", event)
    this.rulesRef.once('value', (rulesSnap) => {
      this.rules = []
      if (rulesSnap && rulesSnap.forEach) {
        rulesSnap.forEach((ruleSnap) => {
          this.rules.push(ruleSnap)
        })
      }
      else {
        throw rulesSnap
      }
    })
  }

  addRule() {
    console.log("Adding Rule, yo")
    let testRule = new RuleEngine.Rule();
    testRule.name = "CoreWeb created this rule. " + new Date().toISOString()
    testRule.enabled = true
    testRule.priority = 10
    testRule.fireOn = "EVERY_PAGE"
    testRule.shortCircuit = false
    testRule.conditionGroups = {}
    testRule.actions = {}
    this.rulesRef.push(testRule).then((ruleRef) => this.onChange())
  }

  testBaseUrl(baseUrl) {
    return new Promise((resolve, reject) => {
      // get rules.
    })
  }
}


export function main() {
  console.log("Bootstrapping rules engine")
  ConnectionManager.persistenceHandler = RestDataStore
  initConditionlets()
  initActionlets()
  return bootstrap(RuleEngineComponent);
}
