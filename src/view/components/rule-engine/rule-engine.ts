import {Http, HTTP_PROVIDERS} from 'angular2/http'
import {bootstrap} from 'angular2/angular2';
import {Provider, EventEmitter, Component, Directive, View, Injector} from 'angular2/angular2'
import {CORE_DIRECTIVES} from 'angular2/angular2'
import {Observable} from 'rxjs/Rx.KitchenSink'

import {ApiRoot} from '../../../api/persistence/ApiRoot'
import {EntityMeta, EntitySnapshot} from '../../../api/persistence/EntityBase'
import {UserModel} from "../../../api/auth/UserModel"


import {RuleComponent} from './rule-component'
import {RuleService, RuleModel} from "../../../api/rule-engine/Rule"
import {ActionService} from "../../../api/rule-engine/Action"
import {CwChangeEvent} from "../../../api/util/CwEvent"
import {RestDataStore} from "../../../api/persistence/RestDataStore"
import {DataStore} from "../../../api/persistence/DataStore"
import {ConditionGroupService} from "../../../api/rule-engine/ConditionGroup"
import {ConditionTypeService} from "../../../api/rule-engine/ConditionType"
import {ConditionService} from "../../../api/rule-engine/Condition"
import {I18nService, TreeNode} from "../../../api/system/locale/I18n"
import {ComparisonService} from "../../../api/system/ruleengine/conditionlets/Comparisons"
import {InputService} from "../../../api/system/ruleengine/conditionlets/Inputs"
import {ActionTypeService} from "../../../api/rule-engine/ActionType"
import {CwFilter} from "../../../api/util/CwFilter"


const I8N_BASE:string = 'api.sites.ruleengine'

/**
 *
 */
@Component({
  selector: 'cw-rule-engine'
})
@View({
  template: `<div flex layout="column" class="cw-rule-engine">
  <div flex layout="column" layout-align="start start" class="cw-header">
    <div flex layout="row" layout-align="space-between center">
      <div flex layout="row" layout-align="space-between center" class="ui icon input">
        <i class="filter icon"></i>
        <input class="cw-rule-filter" type="text" placeholder="{{rsrc('inputs.filter.placeholder') | async}}" [value]="filterText" (keyup)="filterText = $event.target.value">
      </div>
      <div flex="2"></div>
      <button class="ui button cw-button-add" aria-label="Create a new rule" (click)="addRule()">
        <i class="plus icon" aria-hidden="true"></i>{{rsrc('inputs.addRule.label') | async}}
      </button>
    </div>
    <div class="cw-filter-links">
      <span>{{rsrc('inputs.filter.status.show.label') | async}}:</span>
      <a href="javascript:void(0)" [ngClass]="{'active': !isFilteringField('enabled'),'cw-filter-link': true}" (click)="setFieldFilter('enabled',null)">{{rsrc('inputs.filter.status.all.label') | async}}</a>
      <span>&#124;</span>
      <a href="javascript:void(0)" [ngClass]="{'active': isFilteringField('enabled',true),'cw-filter-link': true}" (click)="setFieldFilter('enabled',true)">{{rsrc('inputs.filter.status.active.label') | async}}</a>
      <span>&#124;</span>
      <a href="javascript:void(0)" [ngClass]="{'active': isFilteringField('enabled',false),'cw-filter-link': true}" (click)="setFieldFilter('enabled',false)">{{rsrc('inputs.filter.status.inactive.label') | async}}</a>
    </div>
  </div>

  <rule flex layout="row" *ngFor="var r of rules" [rule]="r" [hidden]="isFiltered(r) == true"
        (change)="onRuleChange($event)"
        (remove)="onRuleRemove($event)"></rule>
</div>

`,
  directives: [CORE_DIRECTIVES, RuleComponent]
})
export class RuleEngineComponent {
  rules:Array<RuleModel>
  filterText:string
  status:string
  activeRules:number
  private ruleService:RuleService;
  private resources:I18nService;
  private _rsrcCache:{[key:string]:Observable<string>}

  constructor(ruleService:RuleService, resources:I18nService) {
    this.resources = resources
    resources.get(I8N_BASE).subscribe((rsrc)=> {
    })
    this.ruleService = ruleService
    this.filterText = ""
    this.rules = []
    this._rsrcCache = {}
    this.ruleService.list().subscribe(rules => {
      this.rules = rules
      this.sort()
    })
    this.status = null

    this.getFilteredRulesStatus()
  }

  rsrc(subkey:string) {
    let x = this._rsrcCache[subkey]
    if(!x){
      x = this.resources.get(I8N_BASE + '.rules.' + subkey)
      this._rsrcCache[subkey] = x
    }
    return x

  }

  sort() {
    this.rules.sort(function (a, b) {
      return b.priority - a.priority;
    });
  }

  onRuleChange(rule:RuleModel) {
    if (rule.isValid()) {
      if (rule.isPersisted()) {
        this.ruleService.save(rule)
      }
      else {
        this.ruleService.add(rule);
      }
    }
    this.getFilteredRulesStatus()
  }

  onRuleRemove(rule:RuleModel) {
    if (rule.isPersisted()) {
      this.ruleService.remove(rule)
    }
    this.rules = this.rules.filter((arrayRule) => {
      return arrayRule.key !== rule.key
    })
    this.getFilteredRulesStatus()
  }

  addRule() {
    let rule = new RuleModel(null)
    rule.priority = this.rules.length ? this.rules[0].priority + 1 : 1;
    this.rules.unshift(rule)
    this.sort()
  }

  getFilteredRulesStatus() {
    this.activeRules = 0;
    for (var i = 0; i < this.rules.length; i++) {
      if (this.rules[i].enabled) {
        this.activeRules++;
      }
    }
  }

  setFieldFilter(field:string, value:string = null) {
    // remove old status
    var re = new RegExp(field + ':[\\w]*')
    this.filterText = this.filterText.replace(re, '') // whitespace issues: "blah:foo enabled:false mahRule"
    if (value !== null) {
      this.filterText = field + ':' + value + ' ' + this.filterText
    }
  }

  isFilteringField(field:string, value:any = null):boolean {
    let isFiltering
    if (value === null) {
      var re = new RegExp(field + ':[\\w]*')
      isFiltering = this.filterText.match(re) != null
    } else {
      isFiltering = this.filterText.indexOf(field + ':' + value) >= 0
    }
    return isFiltering
  }

  isFiltered(rule:RuleModel) {
    return CwFilter.isFiltered(rule, this.filterText)
  }
}


export class RuleEngineApp {
  static main() {

    let app = bootstrap(RuleEngineComponent, [ApiRoot,
      I18nService,
      ComparisonService,
      InputService,
      ActionTypeService,
      UserModel,
      RuleService,
      ActionService,
      ConditionGroupService,
      ConditionService,
      ConditionTypeService,
      HTTP_PROVIDERS,
      new Provider(DataStore, {useClass: RestDataStore})
    ])


    app.then((appRef) => {
      console.log("Bootstrapped App: ", appRef)
    }).catch((e) => {
      console.log("Error bootstrapping app: ", e)
      throw e;
    });
    return app
  }
}

