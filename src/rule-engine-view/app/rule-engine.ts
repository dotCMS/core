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

import {ruleEngineTemplate} from './templates/index'


@Component({
  selector: 'rule-engine'
})
@View({
  template: ruleEngineTemplate,
  directives: [RuleComponent, NgFor, NgIf]
})
class RuleEngineComponent {
  rules:any[];
  baseUrl:string;
  rulesRef:EntityMeta;
  filterText:string;

  constructor() {
    console.log('Creating RuleEngine component.')
    this.rules = []
    this.baseUrl = ConnectionManager.baseUrl;
    this.rulesRef = new EntityMeta('/api/v1/sites/48190c8c-42c4-46af-8d1a-0cd5db894797/rules')
    this.filterText = ""
    this.readSnapshots(this.rulesRef).catch((e) => console.log(e));
    this.rulesRef.on('child_added', (snap) => {
      this.rules = this.rules.concat(snap)
    })
    this.rulesRef.on('child_removed', (snap) => {
      this.rules = this.rules.filter((rule)=> {
        return rule.key() !== snap.key()
      })
    })
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

  readSnapshots(rulesRef:EntityMeta) {
    return new Promise((resolve, reject)=> {
      let snaps = []
      rulesRef.once('value', (rulesSnap) => {
        if (rulesSnap && rulesSnap.forEach) {
          rulesSnap.forEach((ruleSnap) => {
            console.log('Rule read: ', ruleSnap)
            snaps.push(ruleSnap)
          })
        }
        else {
          reject(rulesSnap)
        }
        resolve(snaps)
      })
    })
  }

  addRule() {
    let testRule = new RuleEngine.Rule();
    testRule.name = "CoreWeb created this rule. " + new Date().toISOString()
    testRule.enabled = true
    testRule.priority = 10
    testRule.fireOn = "EVERY_PAGE"
    testRule.shortCircuit = false
    testRule.conditionGroups = {}
    testRule.actions = {}
    this.rulesRef.push(testRule).catch((e)=> {
      console.log("Error pushing new rule: ", e)
      throw e
    })
  }

  testBaseUrl(baseUrl) {
    return new Promise((resolve, reject) => {
      // get rules.
    })
  }
}


export function main() {
  ConnectionManager.persistenceHandler = RestDataStore
  initConditionlets()
  initActionlets()
  return bootstrap(RuleEngineComponent, null, function (ex, stack) {
    console.log("Error bootstrapping app: ", ex, stack)
  }).catch((e) => {
    console.log("Error bootstrapping app: ", e)
    throw e;
  });
}
