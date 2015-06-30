import XDebug from 'debug';
let log = XDebug('RuleEngineView.RuleEngineComponent');


import {bootstrap, For, If} from 'angular2/angular2';

import {Component, Directive} from 'angular2/src/core/annotations_impl/annotations';
import {View} from 'angular2/src/core/annotations_impl/view';

import {FormBuilder, Validators, FormDirectives, ControlGroup} from 'angular2/forms';

import jsonp from 'jsonp'

import {Core, ServerManager} from '../../coreweb/index.js'
import {RuleEngineAPI, ruleRepo, ruleGroupRepo, Rule, RuleGroup, RuleStore} from '../../rule-engine/index.js';

import "bootstrap/css/bootstrap.css!";
import "./styles/rule-engine.css!";
import "./styles/theme-dark.css!";

import ruleEngineTemplate from './rule-engine.tpl.html!text'
import ruleTemplate from './rule.tpl.html!text'
import ruleActionTemplate from './rule-action.tpl.html!text'
import clauseGroupTemplate from './clause-group.tpl.html!text'
import clauseTemplate from './clause.tpl.html!text'


@Component({
  selector: 'rule-action',
  properties: {
    "rule": "rule",
    "ruleAction": "rule-action",
    "index": "index"
  }
})
@View({
  template: ruleActionTemplate,
  directives: [FormDirectives, If, For],
  injectables: [FormBuilder]
})
class RuleActionComponent {
  _ruleAction:any;
  form:ControlGroup;
  rule:any;
  index:number;
  builder:FormBuilder;

  constructor(b:FormBuilder) {
    this.builder = b;
  }

  get ruleAction():any {
    return this._ruleAction;
  }

  set ruleAction(ruleAction:any) {
    var ruleActionControl = this.builder.group({
      "name": [ruleAction.name, Validators.required]
    });
    ruleActionControl.controls.name.valueChanges.toRx().debounce(500).subscribe(
        (v) => {
          this._ruleAction = this._ruleAction.clone().withName(v).build()
          this.saveChanges()
        })
    this.form = ruleActionControl
    this._ruleAction = ruleAction
  }

  addRuleAction() {
    //RuleActionActionCreators.addRuleAction(RuleActionBuilder.fromJson({owningRule: this.rule.$key.value}).build(), this.rule)
  }

  saveChanges() {
    //RuleActionActionCreators.updateRuleAction(this._ruleAction)
  }

  onChange(action) {
  }

  removeRuleAction() {
    //RuleActionActionCreators.removeRuleAction(this._ruleAction.$key)
  }
}


@Component({
  selector: 'clause',
  properties: {
    "rule": "rule",
    "group": "group",
    "clause": "clause",
    "index": "index"
  }
})
@View({
  template: clauseTemplate,
  directives: [FormDirectives, If, For]
})
class ClauseComponent {
  _clause:any;
  group:any;
  rule:any;
  index:number;
  clauseTypes:Array;
  comparisons:Array;

  constructor() {
    //ClauseStore.addChangeListener(this.onChange.bind(this))
    this.clauseTypes = [{id: 'hello', text: 'foo'}, {id: 'goodbye', text: 'bar'}]
    this.comparisons = [{id: 'compHello', text: 'cFoo'}, {id: 'compGoodbye', text: 'cBar'}];
  }

  get clause() {
    return this._clause;
  }

  set clause(clause) {
    this._clause = clause
  }

  onChange(action) {
  }

  addClause(rule, group) {
    //let clause = ClauseBuilder.fromCfg({owningGroup: group.$key}).build()
    //ClauseActionCreators.addClause(clause, rule.$key)
  }

  removeClause() {
    //ClauseActionCreators.removeClause(this._clause)
  }

  toggleOperator() {
    //ClauseActionCreators.toggleOperator(this._clause)
  }
}


@Component({
  selector: 'clause-group',
  properties: {
    "rule": "rule",
    "group": "group",
    "index": "index"
  }
})
@View({
  template: clauseGroupTemplate,
  directives: [ClauseComponent, FormDirectives, If, For]
})
class ClauseGroupComponent {
  _group;
  rule;
  index:number;
  clauses:Array = null;
  isCollapse:any;

  constructor() {
    //ClauseStore.addChangeListener(this.onChange.bind(this))
    this.isCollapse = true;
  }

  get group() {
    return this._group;
  }

  set group(group) {
    this._group = group
    var idx = 1
    // TODO: remove this :)
    this.clauses = [
      {
        $key: 'rule0' + idx + '-group02-clause01',
        type: 'IsAuthenticated',
        name: 'User is authenticated',
        owningGroup: 'rule0' + idx + '-group02',
        value: true,
        operator: 'AND'
      },
      {
        $key: 'rule0' + idx + '-group02-clause02',
        type: 'VisitorLocation',
        name: 'User is visiting from france',
        owningGroup: 'rule0' + idx + '-group02',
        value: 'CA',
        operator: 'AND'
      }
    ]
    //debugger
    //this.clauses = ClauseStore.getAll(this._group)
  }

  onChange(action) {
    //this.clauses = ClauseStore.getAll(this._group);
  }

  addGroup(rule:any) {
    //RuleActionCreators.addGroup(rule.$key, new ClauseGroupBuilder().build())
  }

  removeGroup() {
    //RuleActionCreators.removeGroup(this.rule.$key, this._group.$key)
  }

  toggleOperator(rule, group) {
    //RuleActionCreators.toggleClauseGroupOperator(this.rule, this._group)
    //log("Toggle group operator", rule, group)
  }

  toggleCollapse() {
    this.isCollapse = !this.isCollapse
  }
}


@Component({
  selector: 'rule',
  properties: {
    "rule": "rule",
    "index": "index"
  },
  injectables: [
    FormBuilder
  ]
})
@View({
  template: ruleTemplate,
  directives: [ClauseGroupComponent, If, For]
})
class RuleComponent {
  _rule:any;
  ruleGroups:Array;
  index:number;
  collapsed:boolean;

  constructor() {
    log('init RuleComponent')
    this.collapsed = true
  }

  set rule(rule:any){
    this._rule = rule
    this.ruleGroups = Object.keys(rule.groups || {}).map((key) => {
      return rule.groups[key]
    })
  }

  get rule():any {
    return this._rule
  }

  updateRule(name:string){
    this._rule.name = name;
    ruleRepo.set(this._rule)
  }

  addGroup() {
    let group = new RuleGroup();
    group.priority = 10
    group.operator = 'OR'
    group.ruleKey = this._rule.$key
    ruleGroupRepo.push(group).then((group) => {
      this._rule.groups[group.$key] = true
    })
  }

  removeRule() {
    ruleRepo.remove(this.rule.$key).then((x) => {
      log("Yay! ", x)
    }).catch((e) => {
      log("Not yay :~(: ", e)
      throw e
    })
  }

}


@Component({
  selector: 'rule-engine'
})
@View({
  template: ruleEngineTemplate,
  directives: [For, RuleComponent, If]
})
class RuleEngine {
  rules:Array;
  baseUrl:string;

  constructor() {
    this.rules = []
    this.baseUrl = ServerManager.baseUrl;
    log("creating rules engine");
    RuleStore.addChangeListener(this.onChange.bind(this))
    RuleEngineAPI.rules.list()
  }

  updateBaseUrl(value){
    let oldUrl = ServerManager.baseUrl
    ServerManager.baseUrl = value;
    this.baseUrl = value;
    this.testBaseUrl(value).catch((e => {
      alert("Error using provided Base Url. Check the development console.");
      log("Error using provided Base Url: ", e)
      this.baseUrl = oldUrl;
      ServerManager.baseUrl = oldUrl
      throw e
    }))

  }

  onChange(event) {
    RuleStore.get().then((rulesAry)=> this.rules = rulesAry)
  }

  addRule() {
    let testRule = new Rule();
    testRule.name = "CoreWeb created this rule" + new Date().toISOString()
    testRule.enabled = true
    testRule.priority = 10
    testRule.fireOn = "EVERY_PAGE"
    testRule.shortCircuit = false
    testRule.groups = {}
    testRule.actions = {}
    ruleRepo.push(testRule)
  }

  testBaseUrl(baseUrl) {
    return new Promise((resolve, reject) => {
      RuleStore.init();
    })
  }
}

export function main() {
  log("Bootstrapping rules engine")
  return bootstrap(RuleEngine);
}
