/// <reference path="../../../../typings/angular2/angular2.d.ts" />
/// <reference path="../../../../typings/coreweb/coreweb-api.d.ts" />

/// <reference path="./rule-action-component.ts" />
/// <reference path="./rule-condition-component.ts" />
/// <reference path="./rule-component.ts" />

import {bootstrap, NgFor, NgIf, Component, Directive, View, Inject} from 'angular2/angular2';



import {ApiRoot} from 'api/persistence/ApiRoot';
import {ActionTypesProvider} from 'api/rule-engine/ActionTypes';
import {ConditionTypesProvider} from 'api/rule-engine/ConditionTypes';
import {UserModel} from "api/auth/UserModel";
import {I18NCountryProvider} from 'api/system/locale/I18NCountryProvider'


import {RuleComponent} from './rule-component';

@Component({
  selector: 'rule-engine'
})
@View({
  template: `<div flex layout="column" class="cw-rule-engine">
  <div flex layout="row" layout-align="space-between center" class="cw-header">
    <div flex layout="row" layout-align="space-between center" class="ui icon input">
      <i class="filter icon"></i>
      <input type="text" placeholder="Start typing to filter rules..." [value]="filterText" (keyup)="filterText = $event.target.value">
    </div>
    <div flex="2"></div>
    <button  class="ui button cw-button-add" aria-label="Create a new Rule" (click)="addRule()">
      <i class="plus icon" aria-hidden="true"></i>Add Rule
    </button>
  </div>
  <rule flex layout="row" *ng-for="var r of rules" [rule-snap]="r" [hidden]="!(filterText == '' || r.val().name.toLowerCase().includes(filterText.toLowerCase()))"></rule>
</div>

`,
  directives: [RuleComponent, NgFor, NgIf]
})
class RuleEngineComponent {
  rules:any[];
  baseUrl:string;
  rulesRef:EntityMeta;
  filterText:string;

  constructor(@Inject(ApiRoot) apiRoot:ApiRoot) {
    this.rules = []
    this.baseUrl = ConnectionManager.baseUrl;
    this.rulesRef = apiRoot.defaultSite.child('ruleengine/rules')
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
    try {
      ConnectionManager.setBaseUrl(value)
      window.location.assign(window.location.protocol + '//' + window.location.host + window.location.pathname + '?baseUrl=' + value)
    } catch (e) {
      alert("Error using provided Base Url. Check the development console.");
      console.log("Error using provided Base Url: ", e)
      this.baseUrl = oldUrl;
      ConnectionManager.baseUrl = oldUrl
    }
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
    let testRule = {
      name: "CoreWeb created this rule.",
      enabled: true,
      priority: 10,
      fireOn: "EVERY_PAGE",
      shortCircuit: false,
      conditionGroups: {},
      actions: {}
    }
    this.rulesRef.push(testRule).catch((e)=> {
      console.log("Error pushing new rule: ", e)
      throw e
    })
  }

}


export function main() {
  ConnectionManager.persistenceHandler = RestDataStore

  let app = bootstrap(RuleEngineComponent, [ApiRoot, ActionTypesProvider, ConditionTypesProvider, UserModel, I18NCountryProvider])
  app.then((appRef) => {
    console.log("Bootstrapped App: ", appRef)
  }).catch((e) => {
    console.log("Error bootstrapping app: ", e)
    throw e;
  });
  return app
}
