import {Http, HTTP_PROVIDERS} from 'angular2/http'
import {bootstrap} from 'angular2/angular2';
import {Provider, Observable, Component, Directive, View, Injector} from 'angular2/angular2';
import {CORE_DIRECTIVES} from 'angular2/angular2';
import * as Rx from 'rxjs/Rx.KitchenSink'



import {ApiRoot} from '../../../api/persistence/ApiRoot';
import {EntityMeta, EntitySnapshot} from '../../../api/persistence/EntityBase';
import {UserModel} from "../../../api/auth/UserModel";
import {I18NCountryProvider} from '../../../api/system/locale/I18NCountryProvider'


import {RuleComponent} from './rule-component';
import {RuleService, RuleModel} from "../../../api/rule-engine/Rule";
import {ActionService} from "../../../api/rule-engine/Action";
import {CwChangeEvent} from "../../../api/util/CwEvent";
import {RestDataStore} from "../../../api/persistence/RestDataStore";
import {DataStore} from "../../../api/persistence/DataStore";
import {ConditionGroupService} from "../../../api/rule-engine/ConditionGroup";
import {ConditionTypeService} from "../../../api/rule-engine/ConditionType";
import {ConditionService} from "../../../api/rule-engine/Condition";
import {I18nService} from "../../../api/system/locale/I18n";
import {ComparisonService} from "../../../api/system/ruleengine/conditionlets/Comparisons";
import {InputService} from "../../../api/system/ruleengine/conditionlets/Inputs";
import {ActionTypeService} from "../../../api/rule-engine/ActionType";
import {CwFilter} from "../../../api/util/CwFilter"

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
        <input class="cw-rule-filter" type="text" placeholder="{{rsrc.inputs.filter.placeholder}}" [value]="filterText" (keyup)="filterText = $event.target.value">
      </div>
      <div flex="2"></div>
      <button class="ui button cw-button-add" aria-label="Create a new rule" (click)="addRule()">
        <i class="plus icon" aria-hidden="true"></i>{{rsrc.inputs.addRule.label}}
      </button>
    </div>
    <div class="cw-filter-links">
      <button class="cw-button-link ui black basic button" (click)="setFieldFilter('enabled')" [disabled]="!isFilteringField('enabled')">
        {{rsrc.inputs.filter.status?.all}} ({{rules.length}})
      </button>
      <button class="cw-button-link ui black basic button" (click)="setFieldFilter('enabled', true)" [disabled]="isFilteringField('enabled', true)">
        {{rsrc.inputs.filter.status?.active}} ({{activeRules}}/{{rules.length}})
      </button>
      <button class="cw-button-link ui black basic button" (click)="setFieldFilter('enabled', false)" [disabled]="isFilteringField('enabled', false)">
        {{rsrc.inputs.filter.status?.inactive}} ({{rules.length-activeRules}}/{{rules.length}})
      </button>
    </div>
  </div>

  <rule flex layout="row" *ngFor="var r of rules" [rule]="r" [hidden]="isFiltered(r) == true"
        (change)="onRuleChange($event)"
        (remove)="onRuleRemove($event)"
  ></rule>
</div>

`,
    directives: [CORE_DIRECTIVES, RuleComponent]
  })
  export class RuleEngineComponent {
    rules:Array<RuleModel>
    filterText:string
    status:string
    activeRules:number
    rsrc:any
    private ruleService:RuleService;
    private ruleStub:RuleModel

    constructor(ruleService:RuleService) {
      this.rsrc = ruleService.rsrc
      this.ruleService = ruleService
      this.filterText = ""
      this.rules = []
      this.ruleService.list().subscribe(rules => {
        this.rules = rules
        this.sort()
      })
      this.status = null

      this.getFilteredRulesStatus()
    }

    sort() {
      this.rules.sort(function (a, b) {
        return b.priority - a.priority;
      });
    }

    onRuleChange(rule:RuleModel){
      if(rule.isValid()){
        if(rule.isPersisted()){
          this.ruleService.save(rule)
        }
        else {
          this.ruleService.add(rule);
        }
      }
      this.getFilteredRulesStatus()
    }

    onRuleRemove(rule:RuleModel){
      if(rule.isPersisted()){
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
    	for (var i = 0;  i < this.rules.length ; i++){
    		if (this.rules[i].enabled){
    			this.activeRules++;
    		}
    	}
    }

    setFieldFilter(field:string, value:string=null){
      // remove old status
      var re = new RegExp(field + ':[\\w]*')
      this.filterText = this.filterText.replace(re, '' ) // whitespace issues: "blah:foo enabled:false mahRule"
      if(value !== null) {
        this.filterText = field + ':' + value + ' ' + this.filterText
      }
    }

    isFilteringField(field:string, value:any=null):boolean{
      let isFiltering
      if(value === null){
        var re = new RegExp(field + ':[\\w]*')
        isFiltering =  this.filterText.match(re) != null
      } else{
        isFiltering = this.filterText.indexOf(field + ':' + value) >= 0
      }
      return isFiltering
    }

    isFiltered(rule:RuleModel){
        return CwFilter.isFiltered(rule,this.filterText)
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
      I18NCountryProvider,
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

