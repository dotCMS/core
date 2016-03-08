import {Component} from 'angular2/core'
import {CORE_DIRECTIVES} from 'angular2/common'
import {Observable} from 'rxjs/Rx'

import {RuleComponent} from './rule-component'
import {RuleService, RuleModel} from "../../../api/rule-engine/Rule"
import {I18nService} from "../../../api/system/locale/I18n"
import {CwFilter} from "../../../api/util/CwFilter"
import {CwChangeEvent} from "../../../api/util/CwEvent";
import {ServerSideFieldModel} from "../../../api/rule-engine/ServerSideFieldModel";


const I8N_BASE:string = 'api.sites.ruleengine'

export interface ParameterChangeEvent extends CwChangeEvent {
  rule?:RuleModel
  source?:ServerSideFieldModel
  name:string
  value:string
}


export interface TypeChangeEvent extends CwChangeEvent {
  rule?:RuleModel
  source:ServerSideFieldModel
  value:any
  index:number
}

/**
 *
 */
@Component({
  selector: 'cw-rule-engine',
  template: `<div class="cw-rule-engine">
  <div class="cw-header">
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
      <a href="javascript:void(0)" class="cw-filter-link" [class.active]="!isFilteringField('enabled')" (click)="setFieldFilter('enabled',null)">{{rsrc('inputs.filter.status.all.label') | async}}</a>
      <span>&#124;</span>
      <a href="javascript:void(0)" class="cw-filter-link" [class.active]="isFilteringField('enabled',true)" (click)="setFieldFilter('enabled',true)">{{rsrc('inputs.filter.status.active.label') | async}}</a>
      <span>&#124;</span>
      <a href="javascript:void(0)" class="cw-filter-link" [class.active]="isFilteringField('enabled',false)" (click)="setFieldFilter('enabled',false)">{{rsrc('inputs.filter.status.inactive.label') | async}}</a>
    </div>
  </div>

  <rule *ngFor="var r of rules" [rule]="r" [hidden]="isFiltered(r) == true"
        (change)="onRuleChange($event)"
        (enableStateChange)="onEnableStateChange($event.rule, $event.enabled)"
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

  onEnableStateChange(rule, enabled){
    rule.enabled = enabled
    this.onRuleChange(rule)
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



