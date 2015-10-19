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
  template: `<div class="container">
  <div class="row">
    <div class="col-sm-12">
      <div class="row page-title-search">
        <div class="col-sm-5">
          <div class="form-group has-feedback">
          <input type="text" class="form-control" placeholder="Start typing to filter rules..." [value]="filterText"
              (keyup)="filterText = $event.target.value">
              <span class="glyphicon glyphicon-search form-control-feedback" style="text-align: left"></span>
          </div>
        </div>
        <div class="col-sm-7">
          <button type="button" class="btn btn-default btn-md pull-right" aria-label="Create a new Rule" (click)="addRule()">
            <span class="glyphicon glyphicon-plus" aria-hidden="true"></span>Add Rule
          </button>
        </div>
      </div>
      <div *ng-for="var r of rules">
        <rule class="row" [rule-snap]="r"
        *ng-if="filterText == '' || r.val().name.toLowerCase().includes(filterText.toLowerCase())"></rule>
      </div>
    </div>
  </div>
</div>`,
  directives: [RuleComponent, NgFor, NgIf]
})
class RuleEngineComponent{
  rules:any[];
  baseUrl:string;
  rulesRef:EntityMeta;
  filterText:string;

  constructor(@Inject(ApiRoot) apiRoot:ApiRoot){
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
      window.location.assign( window.location.protocol + '//' + window.location.host + window.location.pathname + '?baseUrl=' + value )
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
  app.then( (appRef) => {
    console.log("Bootstrapped App: ", appRef)
  }).catch((e) => {
    console.log("Error bootstrapping app: ", e)
    throw e;
  });
  return app
}
