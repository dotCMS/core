/// <reference path="../../../../jspm_packages/npm/angular2@2.0.0-alpha.44/angular2.d.ts" />
/// <reference path="../../../../jspm_packages/npm/@reactivex/rxjs@5.0.0-alpha.4/dist/cjs/Rx.d.ts" />

import {bootstrap, bind, NgFor, NgIf, Component, Directive, View, Inject} from 'angular2/angular2';
//noinspection TypeScriptCheckImport
import * as Rx from 'rxjs/dist/cjs/Rx'


import {ApiRoot} from 'api/persistence/ApiRoot';
import {EntityMeta, EntitySnapshot} from 'api/persistence/EntityBase';
import {ActionTypesProvider} from 'api/rule-engine/ActionType';
import {ConditionTypesProvider} from 'api/rule-engine/ConditionTypes';
import {UserModel} from "api/auth/UserModel";
import {I18NCountryProvider} from 'api/system/locale/I18NCountryProvider'


import {RuleComponent} from './rule-component';
import {RuleService, RuleModel} from "api/rule-engine/Rule";
import {ActionService} from "api/rule-engine/Action";
import {CwEvent} from "api/util/CwEvent";
import {RestDataStore} from "api/persistence/RestDataStore";
import {DataStore} from "api/persistence/DataStore";

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
  <rule flex layout="row" *ng-for="var r of rules" [model]="r" [hidden]="!(filterText == '' || r.name.toLowerCase().includes(filterText.toLowerCase()))"></rule>
</div>

`,
  directives: [RuleComponent, NgFor, NgIf]
})
class RuleEngineComponent {
  rules:RuleModel[];
  filterText:string;
  ruleService:RuleService;

  constructor(@Inject(RuleService) ruleService:RuleService) {
    this.ruleService = ruleService;
    this.filterText = ""
    this.rules = []

    this.ruleService.get().subscribe( (rule:RuleModel) => {
          this.rules.push(rule)
        },
        (err) => {
          console.log('Something went wrong: ' + err.message);
        })
  }

  addRule() {
    this.ruleService.add()
  }

}


export function main() {


  let app = bootstrap(RuleEngineComponent, [ApiRoot,
    ActionTypesProvider,
    ConditionTypesProvider,
    UserModel,
    I18NCountryProvider,
    RuleService,
    ActionService,
    bind(DataStore).toClass(<ng.Type>RestDataStore)

  ])
  app.then((appRef) => {
    console.log("Bootstrapped App: ", appRef)
  }).catch((e) => {
    console.log("Error bootstrapping app: ", e)
    throw e;
  });
  return app
}
